package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.CreativeStructureHandler;
import com.ldtteam.structurize.api.RotationMirror;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;

/** Finite founding stock plus real inventory redistribution; no recurring item grants. */
@SuppressWarnings("removal")
final class MayorResources {
    static final int MAX_KIT_UNITS=32768;
    static CompoundTag itemTag(ServerLevel level,ItemStack stack) {
        return (CompoundTag)ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),stack.copyWithCount(1)).getOrThrow();
    }
    static ItemStack item(ServerLevel level,CompoundTag entry) {
        return ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),entry.getCompoundOrEmpty("item")).getOrThrow();
    }
    static void addStock(ServerLevel level,MayorSavedData.Mayor m,ItemStack stack) {
        addStock(level,m,stack,Long.MIN_VALUE);
    }
    static void addStock(ServerLevel level,MayorSavedData.Mayor m,ItemStack stack,long target) {
        if(stack.isEmpty())return;
        var tag=itemTag(level,stack);int amount=stack.getCount();if(amount>MAX_KIT_UNITS-m.kitUnits)throw new IllegalStateException("Founding blueprints exceed finite 32768-item kit");
        for(var entry:m.stock)if(entry.getLongOr("for",Long.MIN_VALUE)==target&&entry.getCompoundOrEmpty("item").equals(tag)) {
            entry.putInt("remaining",entry.getIntOr("remaining",0)+amount);m.kitUnits+=amount;return;
        }
        if(m.stock.size()>=1024)throw new IllegalStateException("Founding blueprints exceed 1024 stock entries");
        var entry=new CompoundTag();entry.put("item",tag);entry.putLong("for",target);entry.putInt("remaining",amount);m.stock.add(entry);m.kitUnits+=amount;
    }
    static ItemStack debitStock(ServerLevel level,MayorSavedData.Mayor m,ItemStack wanted,int count) {
        for(var entry:m.stock) {
            var actual=item(level,entry);if(!ItemStack.isSameItemSameComponents(actual,wanted))continue;
            int amount=Math.min(Math.min(count,actual.getMaxStackSize()),entry.getIntOr("remaining",0));
            if(amount>0){entry.putInt("remaining",entry.getIntOr("remaining",0)-amount);return actual.copyWithCount(amount);}
        }
        return ItemStack.EMPTY;
    }
    static int stockIndex(ServerLevel level,MayorSavedData.Mayor m,ItemStack wanted){for(int i=0;i<m.stock.size();i++)if(m.stock.get(i).getIntOr("remaining",0)>0&&ItemStack.isSameItemSameComponents(item(level,m.stock.get(i)),wanted))return i;return -1;}
    static int insert(IItemHandler target,ItemStack stack,boolean simulate) {
        var remaining=stack.copy();for(int i=0;i<target.getSlots()&&!remaining.isEmpty();i++)remaining=target.insertItem(i,remaining,simulate);
        return stack.getCount()-remaining.getCount();
    }
    static ItemStack extract(IItemHandler source,ItemStack wanted,int maximum) {
        for(int i=0;i<source.getSlots();i++)if(ItemStack.isSameItemSameComponents(source.getStackInSlot(i),wanted))return source.extractItem(i,maximum,false);
        return ItemStack.EMPTY;
    }
    static List<IItemHandler> inventories(ServerLevel level,IColony colony) {
        var result=new ArrayList<IItemHandler>();
        for(var b:colony.getServerBuildingManager().getBuildings().values())if(ColonyIntegration.buildingAreaLoaded(level,b)) {
            var inv=b.getItemHandlerCap();if(inv!=null)result.add(inv);
        }
        return result;
    }
    static boolean moveOne(ServerLevel level,IColony colony) {
        for(var target:colony.getServerBuildingManager().getBuildings().values()) {
            if(!(target instanceof BuildingBuilder builder)||!ColonyIntegration.buildingAreaLoaded(level,target))continue;
            var inventory=target.getItemHandlerCap();if(inventory==null)continue;
            for(var resource:builder.getNeededResources().values()) {
                var wanted=resource.getItemStack();int existing=0;
                for(int i=0;i<inventory.getSlots();i++)if(ItemStack.isSameItemSameComponents(wanted,inventory.getStackInSlot(i)))existing+=inventory.getStackInSlot(i).getCount();
                int missing=Math.min(64,resource.getAmount()-existing);if(missing<=0)continue;
                for(var donor:colony.getServerBuildingManager().getBuildings().values()) {
                    if(donor.getPosition().equals(target.getPosition())||!ColonyIntegration.buildingAreaLoaded(level,donor))continue;
                    var source=donor.getItemHandlerCap();if(source==null)continue;
                    for(int i=0;i<source.getSlots();i++) {
                        var held=source.getStackInSlot(i);if(!ItemStack.isSameItemSameComponents(held,wanted))continue;
                        int accepted=insert(inventory,held.copyWithCount(Math.min(missing,held.getCount())),true);if(accepted==0)continue;
                        var actual=source.extractItem(i,accepted,false);int inserted=insert(inventory,actual,false);
                        if(inserted<actual.getCount())insert(source,actual.copyWithCount(actual.getCount()-inserted),false);
                        if(inserted>0) {target.overruleNextOpenRequestWithStack(actual.copyWithCount(inserted));target.markDirty();return true;}
                    }
                }
            }
        }
        return false;
    }
    static void feedOne(ServerLevel level,IColony colony) {
        for(var citizen:colony.getCitizenManager().getCitizens()) {
            boolean food=false;for(int i=0;i<citizen.getInventory().getSlots();i++)if(citizen.getInventory().getStackInSlot(i).has(DataComponents.FOOD)){food=true;break;}
            if(food)continue;
            for(var source:inventories(level,colony))for(int i=0;i<source.getSlots();i++) {
                var stack=source.getStackInSlot(i);if(!stack.has(DataComponents.FOOD))continue;
                int accepted=insert(citizen.getInventory(),stack.copyWithCount(Math.min(8,stack.getCount())),true);
                if(accepted>0){var extracted=source.extractItem(i,accepted,false);int inserted=insert(citizen.getInventory(),extracted,false);if(inserted<extracted.getCount())insert(source,extracted.copyWithCount(extracted.getCount()-inserted),false);return;}
            }
        }
    }
    static void equipOne(ServerLevel level,IColony colony) {
        for(var building:colony.getServerBuildingManager().getBuildings().values()) {
            if(!(building instanceof BuildingBuilder)||!ColonyIntegration.buildingAreaLoaded(level,building))continue;
            for(var worker:building.getAllAssignedCitizen())for(var tag:List.of(net.minecraft.tags.ItemTags.AXES,net.minecraft.tags.ItemTags.PICKAXES,net.minecraft.tags.ItemTags.SHOVELS)) {
                boolean equipped=false;var inventory=worker.getInventory();
                for(int slot=0;slot<inventory.getSlots();slot++)if(inventory.getStackInSlot(slot).is(tag)){equipped=true;break;}
                if(equipped)continue;
                for(var source:inventories(level,colony))for(int slot=0;slot<source.getSlots();slot++) {
                    var item=source.getStackInSlot(slot);if(!item.is(tag)||insert(inventory,item.copyWithCount(1),true)!=1)continue;
                    var actual=source.extractItem(slot,1,false);if(actual.isEmpty())continue;
                    if(insert(inventory,actual,false)!=1)insert(source,actual,false);else {building.markDirty();return;}
                }
                // Native worker AI creates tool/crafting requests when no real tool is available.
            }
        }
    }
    static final class KitScan {
        final Blueprint blueprint;final NaturalSettlementsData.BuildingPlan plan;final StructurePlacer placer;
        int cursor;
        KitScan(ServerLevel level,Blueprint blueprint,NaturalSettlementsData.BuildingPlan plan) {
            this.blueprint=blueprint;this.plan=plan;
            placer=new StructurePlacer(new CreativeStructureHandler(level,BlockPos.of(plan.anchor()),blueprint,RotationMirror.NONE,false){
                @Override public boolean isStackFree(ItemStack stack){return false;}
            });
        }
        boolean tick(ServerLevel level,MayorSavedData.Mayor m) {
            int volume=blueprint.getSizeX()*blueprint.getSizeY()*blueprint.getSizeZ();
            long deadline=System.nanoTime()+3_000_000;
            for(int budget=NaturalSettlementConfig.SCAN_BUDGET.get()*8;budget>0&&cursor<volume&&System.nanoTime()<deadline;budget--) {
                int i=cursor++;var local=new BlockPos(i%blueprint.getSizeX(),i/(blueprint.getSizeX()*blueprint.getSizeZ()),(i/blueprint.getSizeX())%blueprint.getSizeZ());
                var world=new BlockPos(plan.minX()+local.getX(),plan.minY()+local.getY(),plan.minZ()+local.getZ());
                for(var stack:placer.getResourceRequirements(level,world,local,blueprint.getBlockState(local),blueprint.getTileEntityData(world,local)).getRequiredItems()) {
                    addStock(level,m,stack,plan.anchor());var key=itemTag(level,stack);
                    for(var entry:m.stock)if(entry.getLongOr("for",Long.MIN_VALUE)==plan.anchor()&&entry.getCompoundOrEmpty("item").equals(key))entry.putBoolean("material",true);
                }
            }
            return cursor==volume;
        }
    }
}
