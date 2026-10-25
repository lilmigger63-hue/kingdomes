package com.kingdomsofminecraft;

import com.kingdomsofminecraft.campaign.CampaignState;
import com.kingdomsofminecraft.integration.MineColoniesAdapter;
import com.kingdomsofminecraft.integration.LoadedTerrainSampler;
import com.kingdomsofminecraft.integration.BlueprintPreflightCommands;
import com.kingdomsofminecraft.geography.SiteRole;
import java.util.Locale;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import com.kingdomsofminecraft.persistence.CampaignStore;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-thread-owned campaign lifecycle, shared by integrated and dedicated servers. */
@Mod(KingdomsOfMinecraft.MOD_ID)
public final class KingdomsOfMinecraft {
    public static final String MOD_ID = "kingdomsofminecraft";
    private final LoadedTerrainSampler terrain=new LoadedTerrainSampler();
    private final BlueprintPreflightCommands blueprintPlans=new BlueprintPreflightCommands();
    private CampaignStore store;
    private CampaignState state;

    public KingdomsOfMinecraft(IEventBus modEventBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::starting);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stopping);
        NeoForge.EVENT_BUS.addListener(this::commands);
    }

    private void starting(ServerStartingEvent event) {
        terrain.clear();
        blueprintPlans.clear();
        store = new CampaignStore(event.getServer().getWorldPath(LevelResource.ROOT)
                .resolve(MOD_ID).resolve("campaign.dat"));
        try {
            state = store.loadOrCreate(event.getServer().overworld().getSeed());
            LogUtils.getLogger().info("Kingdoms campaign loaded at step {}", state.simulationStep());
        } catch (IOException error) {
            store = null;
            state = null;
            throw new IllegalStateException("Cannot load Kingdoms campaign; preserve the save and investigate", error);
        }
    }

    private void tick(ServerTickEvent.Post event) {
        if (state == null || event.getServer().getTickCount() % 20 != 0) return;
        state = state.advance();
        if (state.simulationStep() % 30 == 0) save();
    }

    private void stopping(ServerStoppingEvent event) {
        blueprintPlans.clear();
        if (state == null) return;
        try { save(); } finally { state = null; store = null; terrain.clear(); }
    }

    private void save() {
        try {
            store.save(state);
            LogUtils.getLogger().info("Kingdoms campaign saved at step {}", state.simulationStep());
        } catch (IOException error) {
            // Fail explicitly; never replace a failed save with a fresh campaign.
            throw new IllegalStateException("Cannot save Kingdoms campaign", error);
        }
    }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kingdoms")
                .then(blueprintPlans.command())
                .then(Commands.literal("status").executes(context -> {
                    if (state == null) {
                        context.getSource().sendFailure(Component.literal("Campaign is not loaded"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Kingdoms campaign step: " + state.simulationStep() + "; kingdoms: " + state.kingdoms().size()), false);
                    return 1;
                }))
                .then(Commands.literal("list").executes(context -> {
                    if (state == null) {
                        context.getSource().sendFailure(Component.literal("Campaign is not loaded"));
                        return 0;
                    }
                    for (var kingdom : state.kingdoms()) {
                        var clan = kingdom.founder();
                        context.getSource().sendSuccess(() -> Component.literal("[" + kingdom.id() + "] " + kingdom.name()
                                + " — " + clan.name() + ", ruled by " + clan.ruler()
                                + " (" + clan.culture().tradition() + ", " + clan.culture().race() + ")"), false);
                    }
                    return state.kingdoms().size();
                }))
                .then(Commands.literal("politics").executes(context -> {
                    if (state == null) {
                        context.getSource().sendFailure(Component.literal("Campaign is not loaded"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Clans: " + state.allegiances().size() + "; recorded treaties: " + state.treaties().size()), false);
                    for (var allegiance : state.allegiances()) {
                        String kingdom = state.kingdoms().stream().filter(k -> k.id() == allegiance.kingdomId()).findFirst().orElseThrow().name();
                        context.getSource().sendSuccess(() -> Component.literal(allegiance.clan().name() + " serves " + kingdom), false);
                    }
                    for (var treaty : state.treaties()) {
                        context.getSource().sendSuccess(() -> Component.literal("Kingdoms " + treaty.first() + " / " + treaty.second()
                                + ": " + (treaty.war() ? "war" : "peace") + " since step " + treaty.changedAt()), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("site").then(Commands.argument("role",StringArgumentType.word()).executes(context -> {
                    var source=context.getSource();
                    SiteRole role;
                    try { role=SiteRole.valueOf(StringArgumentType.getString(context,"role").toUpperCase(Locale.ROOT)); }
                    catch(IllegalArgumentException error) {source.sendFailure(Component.literal("Roles: village, castle, trade, dwarven, elven, fort, coastal"));return 0;}
                    var position=BlockPos.containing(source.getPosition());
                    var cell=terrain.sample(source.getLevel(),Math.floorDiv(position.getX(),16),Math.floorDiv(position.getZ(),16));
                    if(cell.isEmpty()){source.sendFailure(Component.literal("No usable loaded terrain sample here"));return 0;}
                    var assessment=role.assess(cell.get());
                    source.sendSuccess(() -> Component.literal("Terrain screen for " + role.name().toLowerCase(Locale.ROOT) + ": "
                            + (assessment.suitable()?"candidate":"unsuitable") + "; score " + assessment.score()
                            + "; dry samples " + cell.get().dry() + "/25; height " + cell.get().minY() + "–" + cell.get().maxY()
                            + "; sampled " + (source.getLevel().getGameTime()-cell.get().sampledAt()) + " ticks ago"
                            + "; water " + cell.get().water() + "; forest biome " + cell.get().forest()
                            + "; river biome " + cell.get().river() + "; ocean biome " + cell.get().ocean()
                            + (assessment.reasons().isEmpty()?"":"; " + String.join(", ",assessment.reasons()))
                            + ". Advisory only: MineColonies blueprint fit, protection, resources and access still need checking."),false);
                    return 1;
                })))
                .then(Commands.literal("territory").executes(context -> {
                    if (state == null) return 0;
                    var source=context.getSource();
                    var position=BlockPos.containing(source.getPosition());
                    if (!source.getLevel().hasChunk(Math.floorDiv(position.getX(),16),Math.floorDiv(position.getZ(),16))) {
                        source.sendSuccess(() -> Component.literal("Area unloaded; positional colony lookup skipped. Use /kingdoms colonies."),false);
                        return 1;
                    }
                    var colony=MineColoniesAdapter.at(source.getLevel(),position);
                    source.sendSuccess(() -> Component.literal(colony.map(c -> "Colony " + c.key().colonyId() + ": " + c.name())
                            .orElse("No MineColonies colony at this position")), false);
                    return 1;
                }))
                .then(Commands.literal("settlements").executes(context -> showColonies(context.getSource())))
                .then(Commands.literal("colonies").executes(context -> showColonies(context.getSource()))
                        .then(Commands.literal("bind").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("colony",IntegerArgumentType.integer(1))
                                        .then(Commands.argument("kingdom",IntegerArgumentType.integer(0)).executes(context -> {
                                            if (state == null) return 0;
                                            var source=context.getSource();
                                            var colony=MineColoniesAdapter.find(source.getLevel(),IntegerArgumentType.getInteger(context,"colony"));
                                            if (colony.isEmpty()) { source.sendFailure(Component.literal("Unknown colony in this dimension")); return 0; }
                                            try {
                                                var next=state.bindColony(colony.get().key(),IntegerArgumentType.getInteger(context,"kingdom"));
                                                store.save(next);
                                                state=next;
                                                source.sendSuccess(() -> Component.literal("Colony political allegiance recorded; MineColonies player permissions are unchanged"), true);
                                                return 1;
                                            } catch (IllegalArgumentException | IOException error) {
                                                source.sendFailure(Component.literal(error.getMessage())); return 0;
                                            }
                                        })))))
                .then(Commands.literal("settlement").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("kingdom", IntegerArgumentType.integer(0))
                                .then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                                    context.getSource().sendFailure(Component.literal("Standalone sites are legacy data. Create a MineColonies colony, then use /kingdoms colonies bind."));
                                    return 0;
                                }))))
                .then(Commands.literal("capital").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("settlement",IntegerArgumentType.integer(0)).executes(context -> {
                            context.getSource().sendFailure(Component.literal("Legacy capital setup is disabled; MineColonies Town Halls administer colonies. Political capital mapping is pending."));
                            return 0;
                        }))));
    }

    private int showColonies(net.minecraft.commands.CommandSourceStack source) {
        if (state == null) return 0;
        var colonies=MineColoniesAdapter.discover(source.getLevel());
        source.sendSuccess(() -> Component.literal("MineColonies colonies: " + colonies.size()),false);
        for (var colony:colonies) {
            var allegiance=state.colonies().stream().filter(b -> b.colony().equals(colony.key())).findFirst();
            source.sendSuccess(() -> Component.literal("[" + colony.key().colonyId() + "] " + colony.name()
                    + "; player owner " + colony.playerOwnerName() + "; citizens " + colony.population() + "; buildings " + colony.buildings() + "; Town Hall level " + colony.townHallLevel()
                    + allegiance.map(b -> "; kingdom " + b.kingdomId()).orElse("; politically unassigned")),false);
        }
        return colonies.size();
    }

}
