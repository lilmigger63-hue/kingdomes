package dev.kingdoms.livingsettlements;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class GovernorService {
    private final Map<String, Integer> priorities;
    private int cursor;

    public GovernorService() {
        try (var stream = GovernorService.class.getResourceAsStream("/data/living_settlements/governor_priorities.json")) {
            if (stream == null) { throw new IllegalStateException("Missing governor priorities"); }
            priorities = Map.copyOf(new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8),
                new TypeToken<Map<String, Integer>>() {}.getType()));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cannot load governor priorities", ex);
        }
    }

    public void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 200 != 0) { return; }
        GovernorSavedData data = GovernorSavedData.get(level);
        data.retryAfter.entrySet().removeIf(entry -> entry.getValue() <= level.getGameTime());
        var assignments = new java.util.ArrayList<>(data.assignments());
        java.util.Collections.rotate(assignments, cursor++);
        for (var assignment : assignments) {
            int id = assignment.colonyId();
            var colony = IColonyManager.getInstance().getColonyByWorld(id, level);
            if (colony == null) { data.reports.put(id, "Paused: colony no longer exists"); continue; }
            boolean npcOwned = NaturalSettlementsData.get(level).ownsNpcColony(id, assignment.authorizer());
            if(npcOwned&&NaturalSettlementConfig.MAYOR.get()&&MayorSavedData.get(level).at(id)!=null)continue;
            if (npcOwned && !NaturalSettlementConfig.AUTONOMOUS.get()) { data.reports.put(id,"NPC autonomous development disabled"); continue; }
            int credit = 0;
            if (npcOwned) for (var s : NaturalSettlementsData.get(level).settlements()) if (s.colonyId==id) { credit=s.developmentCredit; break; }
            // Persisted offline intent brings native work forward when players return, one order at a time.
            if (level.getGameTime()%1200!=0 && !(npcOwned && credit>0 && level.getGameTime()%200==0)) continue;
            if (npcOwned && !OfflineDevelopment.takeBudget(level)) continue;
            var player = level.getServer().getPlayerList().getPlayer(assignment.authorizer());
            if (player == null) { player = NaturalSettlementService.npcActor(level, colony, assignment.authorizer()); }
            if (!ColonyIntegration.mayManage(colony, player) || player.level() != level) {
                data.reports.put(id, "Paused: authorizer must be online in this dimension with MANAGE_HUTS permission");
                continue;
            }
            if (!ColonyIntegration.loaded(level, colony.getCenter())) {
                data.reports.put(id, npcOwned ? "Unloaded: bounded development planning; physical construction waits for loaded chunks" : "Paused: colony center is unloaded"); continue;
            }
            if (NaturalSettlementsData.get(level).ownsNpcColony(id, assignment.authorizer())) {
                NpcSettlementBootstrap.addRealVisitors(level, colony);
            }
            if (colony.isColonyUnderAttack() || !colony.getWorkManager().getWorkOrders().isEmpty()) {
                data.reports.put(id, "Paused: raid or existing work order"); continue;
            }
            var candidates = ColonyIntegration.candidates(level, colony).stream()
                .filter(candidate->NaturalSettlementsData.get(level).settlements().stream().noneMatch(s->s.colonyId==id&&s.owner.equals(colony.getPermissions().getOwner())&&s.blockedPlots.contains(candidate.position())))
                .filter(candidate -> !data.retryAfter.containsKey(candidate.position())).toList();
            var choice = GovernorPolicy.choose(candidates,
                new GovernorPolicy.Gate(true, true, false, false), assignment.maximumLevel(), priorities);
            if (choice.isEmpty()) {
                data.reports.put(id, "Idle: no loaded eligible building below the level cap (or retry cooldown)");
                continue;
            }
            var selected = choice.orElseThrow();
            if (ColonyIntegration.requestUpgrade(level, colony, player, selected.position())) {
                data.reports.put(id, "Queued " + selected.type() + " to level " + (selected.level() + 1));
                if (npcOwned) for (var settlement:NaturalSettlementsData.get(level).settlements()) {
                    if (settlement.colonyId==id && settlement.developmentCredit>0) {
                        settlement.developmentCredit--; NaturalSettlementsData.get(level).setDirty(); break;
                    }
                }
            } else {
                // A locked research tier or unavailable builder must not starve the other huts.
                data.retryAfter.put(selected.position(), level.getGameTime() + 12000);
                data.reports.put(id, "Port declined " + selected.type() + "; retry in 10 minutes");
            }
        }
    }
}
