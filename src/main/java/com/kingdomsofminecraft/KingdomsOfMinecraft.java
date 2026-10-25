package com.kingdomsofminecraft;

import com.kingdomsofminecraft.campaign.CampaignState;
import com.kingdomsofminecraft.campaign.Region;
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
                .then(Commands.literal("territory").executes(context -> {
                    if (state == null) return 0;
                    var region = currentRegion(context.getSource());
                    var owner = state.ownerOf(region);
                    context.getSource().sendSuccess(() -> Component.literal(region.dimension() + " region " + region.x() + ", " + region.z()
                            + (owner.isPresent() ? ": kingdom " + owner.getAsInt() : ": unclaimed")), false);
                    return 1;
                }))
                .then(Commands.literal("settlements").executes(context -> {
                    if (state == null) return 0;
                    context.getSource().sendSuccess(() -> Component.literal("Registered sites: " + state.settlements().size()), false);
                    for (var site : state.settlements()) {
                        context.getSource().sendSuccess(() -> Component.literal("[" + site.id() + "] " + site.name() + ": kingdom " + site.kingdomId()
                                + (site.capital() ? " (capital)" : "") + ", " + site.region().dimension() + " region " + site.region().x() + ", " + site.region().z()), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("settlement").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("kingdom", IntegerArgumentType.integer(0))
                                .then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                                    if (state == null) return 0;
                                    try {
                                        state = state.registerSettlement(StringArgumentType.getString(context,"name"),
                                                IntegerArgumentType.getInteger(context,"kingdom"), currentRegion(context.getSource()));
                                        save();
                                        context.getSource().sendSuccess(() -> Component.literal("Registered strategic settlement site. No buildings or blocks were changed."), true);
                                        return 1;
                                    } catch (IllegalArgumentException error) {
                                        context.getSource().sendFailure(Component.literal(error.getMessage()));
                                        return 0;
                                    }
                                }))))
                .then(Commands.literal("capital").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("settlement", IntegerArgumentType.integer(0)).executes(context -> {
                            if (state == null) return 0;
                            try {
                                state = state.designateCapital(IntegerArgumentType.getInteger(context,"settlement"));
                                save();
                                context.getSource().sendSuccess(() -> Component.literal("Capital designation updated"), true);
                                return 1;
                            } catch (IllegalArgumentException error) {
                                context.getSource().sendFailure(Component.literal(error.getMessage()));
                                return 0;
                            }
                        }))));
    }

    private static Region currentRegion(net.minecraft.commands.CommandSourceStack source) {
        var position = BlockPos.containing(source.getPosition());
        return Region.atBlock(source.getLevel().dimension().identifier().toString(), position.getX(), position.getZ());
    }
}
