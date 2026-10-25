package com.kingdomsofminecraft.testing;

import com.kingdomsofminecraft.persistence.CampaignStore;
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
            var political = new CampaignState(previous.worldSeed(), previous.simulationStep(), edited, previous.allegiances(), previous.treaties())
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
                String fixture = System.getProperty("kingdomsofminecraft.reloadFixture");
                if (fixture != null) {
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
