package com.kingdomsofminecraft.testing;

import com.kingdomsofminecraft.persistence.CampaignStore;
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
                helper.succeed();
            } catch (IOException error) {
                throw helper.assertionException("Campaign autosave could not be read: " + error.getMessage());
            }
        });
    }
}
