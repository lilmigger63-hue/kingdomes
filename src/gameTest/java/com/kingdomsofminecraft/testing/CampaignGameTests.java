package com.kingdomsofminecraft.testing;

import com.kingdomsofminecraft.persistence.CampaignStore;
import com.kingdomsofminecraft.integration.MineColoniesAdapter;
import com.minecolonies.api.colony.IColonyManager;
import com.ldtteam.structurize.storage.StructurePacks;
import com.kingdomsofminecraft.campaign.*;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Isolated integration test mod, never bundled in the release artifact. */
@Mod(CampaignGameTests.ID)
public final class CampaignGameTests {
    public static final String ID = "kingdomsofminecraft_tests";
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, ID);
    static {
        FUNCTIONS.register("campaign_lifecycle", () -> CampaignGameTests::campaignLifecycle);
        FUNCTIONS.register("terrain_sampling", () -> TerrainGameTests::terrain);
    }
    public CampaignGameTests(IEventBus bus) {
        FUNCTIONS.register(bus);
        NeoForge.EVENT_BUS.addListener(this::restoreCampaignFixture);
    }

    private void restoreCampaignFixture(ServerAboutToStartEvent event) {
        String source = System.getProperty("kingdomsofminecraft.reloadFixture");
        if (source == null) return;
        // GameTestMain resets its universe. Restore only a previous campaign file
        // before the production Starting event; never seed an ordinary play world.
        Path target = event.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("kingdomsofminecraft/campaign.dat");
        try {
            Files.createDirectories(target.getParent());
            Files.copy(Path.of(source), target, StandardCopyOption.REPLACE_EXISTING);
            // Make re-generating initial records on production startup observable.
            var store = new CampaignStore(target);
            var previous = store.loadOrCreate(Long.MIN_VALUE);
            var edited = new ArrayList<>(previous.kingdoms());
            var first = edited.getFirst();
            edited.set(0, new Kingdom(first.id(), "Kept Across Restart", first.founder(), new Banner(0x102030, 0x405060, 7)));
            var sites = new ArrayList<>(previous.settlements());
            var bindings=new ArrayList<>(previous.colonies());
            bindings.set(0,new ColonyBinding(bindings.getFirst().colony(),1));
            var political = new CampaignState(previous.worldSeed(), previous.simulationStep(), edited, previous.allegiances(), previous.treaties(), sites, bindings)
                    .joinClan(new Clan(100, "Restart Vassals", "Rela", Culture.FOREST), edited.getFirst().id())
                    .setWar(edited.get(0).id(), edited.get(1).id(), true);
            store.save(political);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot restore integration campaign fixture", error);
        }
    }

    private static void campaignLifecycle(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        try {
            var source = server.createCommandSourceStack().withPosition(new net.minecraft.world.phys.Vec3(5000000,80,5000000));
            var unprivileged = source.withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS);
            var dispatcher = server.getCommands().getDispatcher();
            helper.assertTrue(!server.overworld().hasChunk(312500,312500), "Remote test region starts unloaded");
            try {
                dispatcher.execute("kingdoms settlement 0 Unauthorized", unprivileged);
                throw helper.assertionException("Non-operator settlement registration was allowed");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
            try {
                dispatcher.execute("kingdoms capital 0", unprivileged);
                throw helper.assertionException("Non-operator capital designation was allowed");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
            helper.assertTrue(net.neoforged.fml.ModList.get().isLoaded("minecolonies"), "Real MineColonies mod must load");
            var packs=StructurePacks.getPackMetas();
            helper.assertTrue(!packs.isEmpty(), "Real blueprint packs must be provisioned");
            // NeoForge fake player avoids a vanilla mock login without negotiated mod channels.
            // This is test ownership only, not an implemented AI governor.
            var basePack=packs.stream().filter(p -> p.getName().equals("Minecolonies Original")).findFirst()
                    .orElseThrow(() -> helper.assertionException("Required upstream MineColonies base blueprint pack is missing"));
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.fromString("473ae721-8927-499c-aae5-badfd0540d65"),"IntegrationOwner"));
            var colony=IColonyManager.getInstance().createColony(helper.getLevel(),helper.absolutePos(new net.minecraft.core.BlockPos(0,0,0)),
                    player,"Integration Colony",basePack.getName());
            helper.assertTrue(colony != null, "Public MineColonies API must create a real colony");
            var owner=colony.getPermissions().getOwner();
            var snapshot=MineColoniesAdapter.find(helper.getLevel(),colony.getID()).orElseThrow();
            helper.assertTrue(snapshot.name().equals("Integration Colony") && snapshot.playerOwner().equals(player.getUUID()) && snapshot.playerOwnerName().equals("IntegrationOwner"), "Read authoritative colony name and player owner");
            helper.assertTrue(snapshot.population()==0 && snapshot.buildings()==0 && snapshot.townHallLevel()==0, "Read initial population/buildings without synthesizing development");
            String bind="kingdoms colonies bind " + colony.getID() + " ";
            try {
                dispatcher.execute(bind+"0",unprivileged);
                throw helper.assertionException("Non-operator colony binding allowed");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
            int expectedKingdom=System.getProperty("kingdomsofminecraft.reloadFixture")==null ? 0 : 1;
            helper.assertTrue(dispatcher.execute(bind+expectedKingdom,source)==1, "Operator binds a real colony or repeats the restored mapping");
            helper.assertTrue(dispatcher.execute(bind+(expectedKingdom==0?1:0),source)==0, "Rebinding cannot bypass conquest");
            helper.assertTrue(colony.getPermissions().getOwner().equals(owner), "Political mapping must not alter MineColonies owner permissions");
            helper.assertTrue(dispatcher.execute("kingdoms settlement 0 Legacy",source)==0, "Standalone settlement registration is disabled");
            helper.assertTrue(dispatcher.execute("kingdoms capital 0",source)==0, "Standalone capital administration is disabled");
            helper.assertTrue(dispatcher.execute("kingdoms territory",unprivileged)==1, "Public colony territory inspection");
            helper.assertTrue(dispatcher.execute("kingdoms settlements",unprivileged)==1, "Public colony discovery");
            helper.assertTrue(!server.overworld().hasChunk(312500,312500), "Discovery and mapping must not load the remote query region");
            int result = server.getCommands().getDispatcher().execute(
                    "kingdoms status", server.createCommandSourceStack());
            helper.assertTrue(result == 1, "Campaign status must be registered and loaded");
            int count = server.getCommands().getDispatcher().execute("kingdoms list", server.createCommandSourceStack());
            helper.assertTrue(count >= 11 && count <= 20, "World must contain 11–20 NPC-founded kingdoms");
            helper.assertTrue(server.getCommands().getDispatcher().execute("kingdoms politics", server.createCommandSourceStack()) == 1,
                    "Politics inspection must be available");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw helper.assertionException("Status command failed: " + error.getMessage());
        }
        // Wait beyond one autosave interval, proving real server events advance and persist state.
        helper.runAfterDelay(650, () -> {
            var file = server.getWorldPath(LevelResource.ROOT)
                    .resolve("kingdomsofminecraft/campaign.dat");
            try {
                helper.assertTrue(java.nio.file.Files.isRegularFile(file), "Autosave must create world campaign file");
                var saved = new CampaignStore(file).loadOrCreate(Long.MIN_VALUE);
                helper.assertTrue(saved.worldSeed() == server.overworld().getSeed(), "Campaign must use owning world seed");
                helper.assertTrue(saved.simulationStep() >= 30, "Campaign must advance and autosave");
                helper.assertTrue(saved.kingdoms().size() >= 11 && saved.kingdoms().size() <= 20, "Kingdom records must persist");
                helper.assertTrue(saved.colonies().size()==1, "One physical colony has one persisted political binding");
                helper.assertTrue(saved.colonies().getFirst().colony().dimension().equals("minecraft:overworld"), "Binding dimension persists");
                String fixture = System.getProperty("kingdomsofminecraft.reloadFixture");
                if (fixture != null) {
                    helper.assertTrue(saved.colonies().getFirst().kingdomId()==1, "Custom colony political mapping survives startup");
                    var previous = new CampaignStore(Path.of(fixture)).loadOrCreate(Long.MIN_VALUE);
                    helper.assertTrue(saved.simulationStep() > previous.simulationStep(), "Restart must advance the persisted clock");
                    helper.assertTrue(saved.allegiances().stream().anyMatch(a -> a.clan().id() == 100
                            && a.clan().name().equals("Restart Vassals") && a.kingdomId() == saved.kingdoms().getFirst().id()), "Vassal allegiance must survive restart");
                    helper.assertTrue(saved.atWar(saved.kingdoms().get(1).id(), saved.kingdoms().get(0).id()), "War treaty must survive restart symmetrically");
                    helper.assertTrue(saved.treaties().getFirst().changedAt() == previous.simulationStep(), "Restart must preserve treaty time");
                    var first = saved.kingdoms().getFirst();
                    helper.assertTrue(first.name().equals("Kept Across Restart"), "Restart must preserve custom kingdom name");
                    helper.assertTrue(first.banner().equals(new Banner(0x102030, 0x405060, 7)), "Restart must preserve custom banner");
                    helper.assertTrue(first.id() == previous.kingdoms().getFirst().id()
                            && first.founder().equals(previous.kingdoms().getFirst().founder()), "Restart must preserve founding clan");
                    helper.assertTrue(saved.kingdoms().subList(1, saved.kingdoms().size())
                            .equals(previous.kingdoms().subList(1, previous.kingdoms().size())), "Restart must preserve other kingdoms");
                }
                helper.succeed();
            } catch (IOException error) {
                throw helper.assertionException("Campaign autosave could not be read: " + error.getMessage());
            }
        });
    }
}
