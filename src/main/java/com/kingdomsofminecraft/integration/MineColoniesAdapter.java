package com.kingdomsofminecraft.integration;

import com.kingdomsofminecraft.campaign.ColonyKey;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server-thread read-only facade. Never changes permissions, inventories, workers or chunks. */
public final class MineColoniesAdapter {
    private MineColoniesAdapter() {}
    public record ColonySnapshot(ColonyKey key, String name, UUID playerOwner, String playerOwnerName, int population, int buildings, int townHallLevel) {}

    public static List<ColonySnapshot> discover(ServerLevel level) {
        return IColonyManager.getInstance().getColonies(level).stream().map(MineColoniesAdapter::snapshot).toList();
    }
    public static Optional<ColonySnapshot> find(ServerLevel level, int id) {
        return Optional.ofNullable(IColonyManager.getInstance().getColonyByWorld(id,level)).map(MineColoniesAdapter::snapshot);
    }
    public static Optional<ColonySnapshot> at(ServerLevel level, BlockPos position) {
        // The upstream positional API calls getChunk; guard before entering it.
        if (!level.hasChunk(Math.floorDiv(position.getX(),16),Math.floorDiv(position.getZ(),16))) return Optional.empty();
        return Optional.ofNullable(IColonyManager.getInstance().getColonyByPosFromWorld(level,position)).map(MineColoniesAdapter::snapshot);
    }
    private static ColonySnapshot snapshot(IColony colony) {
        var manager=colony.getServerBuildingManager();
        var hall=manager.getTownHall();
        return new ColonySnapshot(new ColonyKey(colony.getDimension().identifier().toString(),colony.getID()),
                colony.getName(),colony.getPermissions().getOwner(),colony.getPermissions().getOwnerName(),colony.getCitizenManager().getCurrentCitizenCount(),
                manager.getBuildings().size(),hall==null ? 0 : hall.getBuildingLevel());
    }
}
