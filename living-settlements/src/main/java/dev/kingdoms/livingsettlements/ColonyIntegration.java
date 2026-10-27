package dev.kingdoms.livingsettlements;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/** Calls verified v0.6.0 port APIs, without duplicating colony lifecycle or permissions. */
public final class ColonyIntegration {
    private ColonyIntegration() {}

    public static IColony at(ServerLevel level, BlockPos position) {
        if (!loaded(level, position)) { return null; }
        return IColonyManager.getInstance().getColonyByPosFromWorld(level, position);
    }
    public static boolean loaded(ServerLevel level, BlockPos position) {
        return level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) != null;
    }
    public static boolean mayManage(IColony colony, ServerPlayer player) {
        return player != null && colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS);
    }
    public static boolean buildingAreaLoaded(ServerLevel level, IBuilding building) {
        if (!loaded(level, building.getPosition())) { return false; }
        var corners = building.getCorners();
        return footprintLoaded(corners.getA().getX(), corners.getB().getX(),
            corners.getA().getZ(), corners.getB().getZ(),
            (x, z) -> level.getChunkSource().getChunkNow(x, z) != null);
    }
    static boolean footprintLoaded(int cornerAX, int cornerBX, int cornerAZ, int cornerBZ,
            BiPredicate<Integer, Integer> isChunkLoaded) {
        // The port immediately probes/places construction tape one block beyond each edge.
        int minX = (Math.min(cornerAX, cornerBX) - 1) >> 4;
        int maxX = (Math.max(cornerAX, cornerBX) + 1) >> 4;
        int minZ = (Math.min(cornerAZ, cornerBZ) - 1) >> 4;
        int maxZ = (Math.max(cornerAZ, cornerBZ) + 1) >> 4;
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > 256) { return false; }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!isChunkLoaded.test(x, z)) { return false; }
            }
        }
        return true;
    }
    public static List<GovernorPolicy.Candidate> candidates(ServerLevel level, IColony colony) {
        List<GovernorPolicy.Candidate> candidates = new ArrayList<>();
        for (IBuilding building : colony.getServerBuildingManager().getBuildings().values()) {
            candidates.add(new GovernorPolicy.Candidate(building.getPosition().asLong(),
                BuiltInRegistries.BLOCK.getKey(building.getBuildingType().getBuildingBlock()).getPath(),
                building.getBuildingLevel(), building.getMaxBuildingLevel(), buildingAreaLoaded(level, building)));
        }
        return candidates;
    }
    public static boolean requestUpgrade(ServerLevel level, IColony colony, ServerPlayer player, long position) {
        // requestUpgrade itself does NOT enforce MANAGE_HUTS in this port.
        if (!mayManage(colony, player) || player.level() != level || colony.isColonyUnderAttack()
                || !colony.getWorkManager().getWorkOrders().isEmpty()) { return false; }
        IBuilding building = colony.getServerBuildingManager().getBuilding(BlockPos.of(position));
        if (building == null || !buildingAreaLoaded(level, building)) { return false; }
        building.requestUpgrade(player, BlockPos.ZERO); // Zero selects the port's automatic builder assignment.
        return !colony.getWorkManager().getWorkOrders().isEmpty();
    }
}
