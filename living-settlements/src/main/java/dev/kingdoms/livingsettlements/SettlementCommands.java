package dev.kingdoms.livingsettlements;

import com.minecolonies.api.colony.IColony;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SettlementCommands {
    private SettlementCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("settlements")
            .executes(context -> help(context.getSource()))
            .then(Commands.literal("survey").executes(context -> survey(context.getSource())))
            .then(Commands.literal("colony").executes(context -> status(context.getSource())))
            .then(Commands.literal("mayor").executes(context -> mayor(context.getSource())))
            .then(Commands.literal("governor")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("enable")
                    .executes(context -> enable(context.getSource(), 1))
                    .then(Commands.argument("maximumLevel", IntegerArgumentType.integer(1, 5))
                        .executes(context -> enable(context.getSource(), IntegerArgumentType.getInteger(context, "maximumLevel")))))
                .then(Commands.literal("disable").executes(context -> disable(context.getSource())))));
    }
    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Living Settlements: /settlements survey | colony | mayor | governor enable [1-5] | governor status | governor disable"), false);
        return 1;
    }
    private static int survey(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var result = TerrainSurvey.survey(source.getLevel(), player.blockPosition());
        source.sendSuccess(() -> Component.literal("64x64 settlement survey: " + result.reason()
            + "; samples " + result.samples() + "/81; relief " + result.relief()
            + " blocks. Terrain screening only; colony spacing and structure fit still need checking."), false);
        return result.suitable() ? 1 : 0;
    }
    private static int mayor(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException();var colony=ColonyIntegration.at(source.getLevel(),player.blockPosition());
        if(colony==null){source.sendFailure(Component.literal("Stand inside a MineColonies colony."));return 0;}
        var mayor=MayorSavedData.get(source.getLevel()).at(colony.getID());
        if(mayor==null||!mayor.owner.equals(colony.getPermissions().getOwner())){source.sendFailure(Component.literal("This colony has no autonomous mayor."));return 0;}
        source.sendSuccess(()->Component.literal(colony.getName()+": mayor #"+mayor.citizenId()+", development stage "+mayor.stage()+", goal "+mayor.goal()+". "+mayor.report()+". Founding stock remaining: "+mayor.stockUnits()+" items; planned road waypoints: "+mayor.roadWaypoints()+"."),false);return 1;
    }
    private static IColony managedColony(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var colony = ColonyIntegration.at(source.getLevel(), player.blockPosition());
        if (colony == null) {
            source.sendFailure(Component.literal("Stand inside an existing MineColonies colony."));
            return null;
        }
        if (!ColonyIntegration.mayManage(colony, player)) {
            source.sendFailure(Component.literal("Your MineColonies rank needs MANAGE_HUTS permission for this colony."));
            return null;
        }
        return colony;
    }
    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var colony = managedColony(source);
        if (colony == null) { return 0; }
        var data = GovernorSavedData.get(source.getLevel());
        var assignment = data.assignment(colony.getID());
        source.sendSuccess(() -> Component.literal(colony.getName() + " (#" + colony.getID() + "): "
            + colony.getCitizenManager().getCurrentCitizenCount() + " citizens, "
            + colony.getServerBuildingManager().getBuildings().size() + " buildings, "
            + colony.getWorkManager().getWorkOrders().size() + " work orders. Governor: "
            + (assignment == null ? "disabled" : "enabled, maximum level " + assignment.maximumLevel()
                + ". " + data.reports.getOrDefault(colony.getID(), "Waiting for next cycle"))), false);
        return 1;
    }
    private static int enable(CommandSourceStack source, int maximumLevel) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var colony = managedColony(source);
        if (colony == null) { return 0; }
        GovernorSavedData.get(source.getLevel()).enable(colony.getID(), source.getPlayerOrException().getUUID(), maximumLevel);
        source.sendSuccess(() -> Component.literal("Governor enabled for " + colony.getName() + ", maximum level " + maximumLevel
            + ". Checks every 60 seconds while you are online in this dimension; uses normal builders and materials."), false);
        return 1;
    }
    private static int disable(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var colony = managedColony(source);
        if (colony == null) { return 0; }
        GovernorSavedData.get(source.getLevel()).disable(colony.getID());
        source.sendSuccess(() -> Component.literal("Governor disabled. Existing MineColonies work orders are retained."), false);
        return 1;
    }
}
