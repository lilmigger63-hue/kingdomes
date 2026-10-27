package dev.kingdoms.livingsettlements;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;

@Mod(LivingSettlements.MOD_ID)
public final class LivingSettlements {
    public static final String MOD_ID = "living_settlements";
    public LivingSettlements(ModContainer container, IEventBus bus) {
        VillageMarkerPiece.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, NaturalSettlementConfig.SPEC, "living-settlements-server.toml");
        var natural = new NaturalSettlementService();
        NeoForge.EVENT_BUS.addListener(natural::started);
        NeoForge.EVENT_BUS.addListener(natural::stopped);
        NeoForge.EVENT_BUS.addListener(natural::chunkLoaded);
        NeoForge.EVENT_BUS.addListener(natural::playerPlaced);
        NeoForge.EVENT_BUS.addListener(natural::playerBroke);
        NeoForge.EVENT_BUS.addListener(natural::tick);
        NeoForge.EVENT_BUS.addListener(SettlementCommands::register);
        var mayors=new MayorService();
        NeoForge.EVENT_BUS.addListener(mayors::tick);
        NeoForge.EVENT_BUS.addListener(mayors::stopped);
        NeoForge.EVENT_BUS.addListener(new GovernorService()::tick);
        NeoForge.EVENT_BUS.addListener(OfflineDevelopment::tick);
    }
}
