package com.kingdomsofminecraft;

import com.kingdomsofminecraft.campaign.CampaignState;
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
    private CampaignStore store;
    private CampaignState state;

    public KingdomsOfMinecraft(IEventBus modEventBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::starting);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stopping);
        NeoForge.EVENT_BUS.addListener(this::commands);
    }

    private void starting(ServerStartingEvent event) {
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
        if (state == null) return;
        try { save(); } finally { state = null; store = null; }
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
                        context.getSource().sendSuccess(() -> Component.literal(kingdom.name()
                                + " — " + clan.name() + ", ruled by " + clan.ruler()
                                + " (" + clan.culture().tradition() + ", " + clan.culture().race() + ")"), false);
                    }
                    return state.kingdoms().size();
                })));
    }
}
