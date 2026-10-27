package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.operations.PlaceStructureOperation;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.CreativeStructureHandler;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.savedata.IServerColonySaveData;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.util.ColonyUtils;
import com.minecolonies.api.util.CreativeBuildingStructureHandler;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.modules.BuildingExtensionsModule;
import com.minecolonies.core.util.ChunkDataHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.List;
import java.util.UUID;

/** Exact port-specific lifecycle; callers must hold a persisted reservation and loaded area. */
@SuppressWarnings("removal")
final class NpcSettlementBootstrap {
    private NpcSettlementBootstrap() {}
    static ServerPlayer actor(ServerLevel level, UUID owner) {
        return FakePlayerFactory.get(level, new GameProfile(owner, "NPC_" + owner.toString().substring(0, 8)));
    }
    static boolean areaLoaded(ServerLevel level, NaturalSettlementsData.Settlement s) {
        if(s.villageChunk!=Long.MIN_VALUE){var origin=ChunkPos.unpack(s.villageChunk);if(level.getChunkSource().getChunkNow(origin.x(),origin.z())==null)return false;}
        if(s.adaptive) {
            if(s.plans.isEmpty())return ColonyIntegration.loaded(level,BlockPos.of(s.center));
            var hall=BlockPos.of(s.plans.getFirst().anchor());
            int radius=(MineColonies.getConfig().getServer().initialColonySize.get()+1)*16;
            if(radius>512)return false;
            for(int x=(hall.getX()-radius)>>4;x<=(hall.getX()+radius)>>4;x++)for(int z=(hall.getZ()-radius)>>4;z<=(hall.getZ()+radius)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
            for(var p:s.plans)for(int x=(p.minX()-16)>>4;x<=(p.minX()+p.sizeX()+16)>>4;x++)for(int z=(p.minZ()-16)>>4;z<=(p.minZ()+p.sizeZ()+16)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
            return true;
        }
        BlockPos center = BlockPos.of(s.center);
        int margin = Math.max(s.radius + 32, (MineColonies.getConfig().getServer().initialColonySize.get() + 1) * 16);
        if (margin > 512) { return false; } // Do not expand unbounded work for unusual upstream settings.
        for (int x = (center.getX() - margin) >> 4; x <= (center.getX() + margin) >> 4; x++) {
            for (int z = (center.getZ() - margin) >> 4; z <= (center.getZ() + margin) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) { return false; }
            }
        }
        return true;
    }
    static boolean claimsClear(ServerLevel level, NaturalSettlementsData.Settlement s) {
        if(s.adaptive) {
            if(s.plans.isEmpty())return true;
            var hall=BlockPos.of(s.plans.getFirst().anchor());int claimRadius=MineColonies.getConfig().getServer().initialColonySize.get()*16;
            if(claimRadius>512)return false;
            for(int x=(hall.getX()-claimRadius)>>4;x<=(hall.getX()+claimRadius)>>4;x++)for(int z=(hall.getZ()-claimRadius)>>4;z<=(hall.getZ()+claimRadius)>>4;z++) {
                var claim=IColonyManager.getInstance().getClaimData(level.dimension(),new ChunkPos(x,z));
                if(claim!=null&&claim.getOwningColony()!=0&&claim.getOwningColony()!=s.colonyId)return false;
            }
            for(var p:s.plans)for(int x=p.minX()>>4;x<=(p.minX()+p.sizeX()-1)>>4;x++)for(int z=p.minZ()>>4;z<=(p.minZ()+p.sizeZ()-1)>>4;z++) {
                var chunk=level.getChunkSource().getChunkNow(x,z);
                if(chunk==null||!VillageReplacement.structuresClear(level,chunk,s))return false;
            }
            return true;
        }
        BlockPos center = BlockPos.of(s.center);
        int margin = Math.max(s.radius + 16, MineColonies.getConfig().getServer().initialColonySize.get() * 16);
        for (int x = (center.getX() - margin) >> 4; x <= (center.getX() + margin) >> 4; x++) {
            for (int z = (center.getZ() - margin) >> 4; z <= (center.getZ() + margin) >> 4; z++) {
                var claim = IColonyManager.getInstance().getClaimData(level.dimension(), new ChunkPos(x, z));
                if (claim != null && claim.getOwningColony() != 0 && claim.getOwningColony() != s.colonyId) { return false; }
                // Preserve vanilla villages and other generated structures, including their references.
                if (s.colonyId == 0) {
                    var chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk == null || !VillageReplacement.structuresClear(level, chunk, s)) { return false; }
                }
            }
        }
        return true;
    }
    static IColony recoverOrFound(ServerLevel level, NaturalSettlementsData.Settlement s) {
        BlockPos anchor = BlockPos.of(s.plans.getFirst().anchor());
        var manager = IColonyManager.getInstance();
        for (var colony : manager.getColonies(level)) {
            if (colony.getPermissions().getOwner().equals(s.owner) && colony.getCenter().equals(anchor)) {
                s.colonyId = colony.getID(); return colony;
            }
        }
        if (s.colonyId != 0) { throw new IllegalStateException("Saved colony is missing or no longer NPC-owned; not recreating it"); }
        // Native saved-data factory registers the colony without the manager's rejected fake-player subscriptions.
        var colony = IServerColonySaveData.getOrComputeSaveData(level).createColony(level, "Settlement " + Integer.toUnsignedString(s.owner.hashCode(), 36), anchor);
        if (colony == null) { throw new IllegalStateException("Port refused colony creation"); }
        s.colonyId = colony.getID();
        colony.setStructurePack(s.style);
        colony.getPermissions().setOwner(actor(level, s.owner));
        colony.setCanBeAutoDeleted(false);
        ChunkDataHelper.claimColonyChunks(level, true, (Colony) colony, anchor);
        colony.markDirty(); return colony;
    }
    static boolean stillOwned(IColony colony, NaturalSettlementsData.Settlement s) {
        return colony != null && colony.getID() == s.colonyId && colony.getPermissions().getOwner().equals(s.owner);
    }
    static PlaceStructureOperation startBuilding(ServerLevel level, IColony colony,
            NaturalSettlementsData.Settlement s, NaturalSettlementsData.BuildingPlan p, Blueprint blueprint) {
        if (blueprint.getSizeX() != p.sizeX() || blueprint.getSizeY() != p.sizeY() || blueprint.getSizeZ() != p.sizeZ()
                || !BlockPos.of(p.anchor()).subtract(blueprint.getPrimaryBlockOffset()).equals(new BlockPos(p.minX(), p.minY(), p.minZ()))) {
            throw new IllegalStateException("Blueprint changed since reservation; not overwriting terrain");
        }
        if (!p.role().equals("field")) {
            BlockPos anchor = BlockPos.of(p.anchor());
            var building = colony.getServerBuildingManager().getBuilding(anchor);
            if (building == null) {
                if (level.getBlockEntity(anchor) != null) { throw new IllegalStateException("Unexpected block entity at planned hut"); }
                level.setBlock(anchor, blueprint.getBlockState(blueprint.getPrimaryBlockOffset()), 3);
                if (!(level.getBlockEntity(anchor) instanceof AbstractTileEntityColonyBuilding tile)) { throw new IllegalStateException("Blueprint anchor did not create a hut"); }
                var nbt = blueprint.getTileEntityData(anchor, blueprint.getPrimaryBlockOffset());
                if (nbt != null) { tile.readSchematicDataFromNBT(nbt); }
                tile.setPackName(s.style); tile.setBlueprintPath(p.path()); tile.setRotationMirror(RotationMirror.NOT_MIRRORED[p.rotation()]);
                building = colony.getServerBuildingManager().addNewBuilding(tile, level);
                if (building == null) { throw new IllegalStateException("Port refused building registration"); }
            }
            building.setCorners(new BlockPos(p.minX(), p.minY(), p.minZ()), new BlockPos(p.minX() + p.sizeX() - 1, p.minY() + p.sizeY() - 1, p.minZ() + p.sizeZ() - 1));
        }
        return new PlaceStructureOperation(new BudgetedStructurePlacer(new BoundedHandler(level, BlockPos.of(p.anchor()), blueprint)), null);
    }
    static void finishBuilding(ServerLevel level, IColony colony, NaturalSettlementsData.Settlement settlement, NaturalSettlementsData.BuildingPlan p) {
        if (p.role().equals("field")) {
            BlockPos position = BlockPos.of(p.anchor());
            var state = level.getBlockState(position);
            level.setBlock(position.above(), state.setValue(BlockScarecrow.HALF, DoubleBlockHalf.UPPER), 3);
            ChunkDataHelper.tryClaim(level, ChunkPos.containing(position).getWorldPosition(), true, (Colony) colony, false);
            var manager = colony.getServerBuildingManager();
            var field = manager.getMatchingBuildingExtension(extension -> extension instanceof FarmField && extension.getPosition().equals(position))
                .map(extension -> (FarmField) extension).orElse(null);
            if (field == null) { field = FarmField.create(position, level); manager.addBuildingExtension(field); }
            field.setSeed(new ItemStack(Items.CARROT));
            for (Direction direction : Direction.Plane.HORIZONTAL) { field.setRadius(direction, 4); }
            var farmerPlan = settlement.plans.stream().filter(plan -> plan.role().equals("farmer")&&!settlement.blockedPlots.contains(plan.anchor()))
                .filter(plan->{var b=manager.getBuilding(BlockPos.of(plan.anchor()));return b!=null&&b.getBuildingLevel()>0;})
                .min(java.util.Comparator.comparingDouble(plan->BlockPos.of(plan.anchor()).distSqr(position))).orElseThrow();
            var farmer = manager.getBuilding(BlockPos.of(farmerPlan.anchor()));
            if (!field.isTaken()) {
                for (var module : farmer.getModulesByType(BuildingExtensionsModule.class)) {
                    if (module.getExpectedExtensionType().isInstance(field) && module.assignExtension(field)) { break; }
                }
            }
            if (!field.isTaken() || !farmer.getID().equals(field.getBuildingId())) { throw new IllegalStateException("Native farmer module refused the starter field"); }
            manager.markBuildingExtensionsDirty();
        } else {
            var building = colony.getServerBuildingManager().getBuilding(BlockPos.of(p.anchor()));
            if (building == null) { throw new IllegalStateException("Placed hut lost registration"); }
            building.setBuildingLevel(p.level());
            building.onUpgradeComplete(null, p.level()); // Native modules/claims, without an expensive whole-blueprint prestige analysis.
            building.markDirty();
        }
    }
    static void addRealVisitors(ServerLevel level, IColony colony) {
        for (var player : level.players()) {
            var chunk = level.getChunkSource().getChunkNow(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
            if (chunk != null && ColonyUtils.getOwningColony(chunk) == colony.getID()) { colony.getPackageManager().addCloseSubscriber(player); }
        }
    }
    static boolean populateOne(ServerLevel level, IColony colony, int population) {
        var manager = colony.getCitizenManager();
        manager.calculateMaxCitizens();
        if (manager.getCurrentCitizenCount() >= population) { return true; }
        ICitizenData citizen = manager.spawnOrCreateCivilian((ICitizenData) null, level, List.of(colony.getCenter()), true);
        if (citizen == null) { return false; }
        citizen.setIsChild(false); citizen.setSaturation(12);
        return false;
    }
    static void assignAndSupply(ServerLevel level, IColony colony, NaturalSettlementsData.Settlement s, boolean grantSupplies) {
        var citizens = colony.getCitizenManager().getCitizens();
        var homes = s.plans.stream().filter(p -> p.role().equals("home")).map(p -> colony.getServerBuildingManager().getBuilding(BlockPos.of(p.anchor()))).toList();
        for (var citizen : citizens) {
            for (var home : homes) {
                if(home==null)continue;
                if (citizen.getHomeBuilding() != null) { break; }
                for (var module : home.getModulesByType(LivingBuildingModule.class)) { module.assignCitizen(citizen); }
            }
        }
        // Stable order gives the essentials a worker before less essential buildings.
        for (String role : List.of("builder", "lumberjack", "farmer", "courier", "miner", "cook")) {
            var plan = s.plans.stream().filter(p -> p.role().equals(role)).findFirst().orElse(null);
            if(plan==null)continue;
            var building = colony.getServerBuildingManager().getBuilding(BlockPos.of(plan.anchor()));
            if(building==null)continue;
            var citizen = colony.getCitizenManager().getJoblessCitizen();
            if (citizen != null) {
                for (var module : building.getModulesByType(WorkerBuildingModule.class)) { if (module.assignCitizen(citizen)) { break; } }
            }
        }
        if (!grantSupplies || (s.townHallOnly&&!NaturalSettlementConfig.START_RESOURCES.get())) { return; }
        for (var citizen : citizens) {
            insert(citizen.getInventory(), new ItemStack(Items.BREAD, 16)); insert(citizen.getInventory(), new ItemStack(Items.CARROT, 16));
            for (var tool : List.of(Items.STONE_AXE, Items.STONE_PICKAXE, Items.STONE_SHOVEL, Items.STONE_HOE)) { insert(citizen.getInventory(), new ItemStack(tool)); }
        }
        for (var p : s.plans) {
            if (p.role().equals("field")) { continue; }
            var building = colony.getServerBuildingManager().getBuilding(BlockPos.of(p.anchor()));
            if(building==null)continue;
            var inventory = building.getItemHandlerCap();
            if (inventory == null) { continue; }
            insert(inventory, new ItemStack(Items.BREAD, 32)); insert(inventory, new ItemStack(Items.CARROT, 32));
            if (p.role().equals("lumberjack")) { insert(inventory, new ItemStack(Items.OAK_SAPLING, 32)); }
            if (p.role().equals("warehouse") || p.role().equals("builder") || (s.townHallOnly&&p.role().equals("townhall"))) {
                for (var item : List.of(Items.OAK_LOG, Items.OAK_PLANKS, Items.COBBLESTONE, Items.DIRT, Items.GLASS, Items.TORCH, Items.COAL)) {
                    insert(inventory, new ItemStack(item, 64));
                }
            }
        }
    }
    private static void insert(IItemHandler inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.getSlots() && !stack.isEmpty(); slot++) { stack = inventory.insertItem(slot, stack, false); }
    }
    /** Native handlers and phases, with addon-owned budgets; no custom block-placement engine. */
    private static final class BoundedHandler extends CreativeStructureHandler {
        private final CreativeBuildingStructureHandler nativeHandler;
        BoundedHandler(ServerLevel level, BlockPos pos, Blueprint bp) {
            super(level, pos, bp, RotationMirror.NONE, false);
            nativeHandler = new CreativeBuildingStructureHandler(level, pos, bp, RotationMirror.NONE, false);
        }
        @Override public int getStepsPerCall() { return NaturalSettlementConfig.PLACE_BUDGET.get(); }
        @Override public int getMaxBlocksCheckedPerCall() { return NaturalSettlementConfig.PLACE_BUDGET.get(); }
        @Override public void triggerSuccess(BlockPos pos, List<ItemStack> items, boolean placement) { nativeHandler.triggerSuccess(pos, items, placement); }
        @Override public void onCompletion() { nativeHandler.onCompletion(); }
    }
}
