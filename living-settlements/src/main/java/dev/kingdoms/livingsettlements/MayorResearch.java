package dev.kingdoms.livingsettlements;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.research.*;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingUniversity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Uses native research eligibility/progression, paying real university ingredients without GUI/fake connections. */
@SuppressWarnings("removal")
final class MayorResearch {
    static boolean start(ServerLevel level,IColony colony,MayorSavedData.Mayor mayor){
        var university=colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b instanceof BuildingUniversity&&b.getBuildingLevel()>0&&ColonyIntegration.buildingAreaLoaded(level,b)).findFirst().orElse(null);
        if(university==null)return false;
        var local=colony.getResearchManager().getResearchTree();if(local.getResearchInProgress().size()>=university.getBuildingLevel())return false;
        var global=IGlobalResearchTree.getInstance();var all=new ArrayList<IGlobalResearch>();var pending=new ArrayDeque<net.minecraft.resources.Identifier>();var seen=new HashSet<net.minecraft.resources.Identifier>();
        global.getBranches().stream().sorted().forEach(branch->pending.addAll(global.getPrimaryResearch(branch)));
        while(!pending.isEmpty()&&seen.size()<512){var id=pending.remove();if(!seen.add(id))continue;var research=global.getResearch(id);if(research!=null){all.add(research);pending.addAll(research.getChildren());}}
        all.sort(Comparator.comparingInt(IGlobalResearch::getDepth).thenComparingInt(IGlobalResearch::getSortOrder).thenComparing(r->r.getId().toString()));
        var inventory=university.getItemHandlerCap();if(inventory==null)return false;
        net.neoforged.neoforge.common.crafting.SizedIngredient shortage=null;int shortageCount=0;
        for(var research:all){
            if(research.isHidden()||!research.canResearch(university,local)||!global.isResearchRequirementsFulfilled(research.getResearchRequirements(),colony))continue;
            int[] remaining=new int[inventory.getSlots()],take=new int[inventory.getSlots()];
            for(int i=0;i<remaining.length;i++)remaining[i]=inventory.getStackInSlot(i).getCount();
            boolean affordable=true;
            for(var cost:research.getCostList()){
                int required=cost.count();for(int slot=0;slot<remaining.length&&required>0;slot++)if(IGlobalResearch.isUniversityResearchMatch(inventory.getStackInSlot(slot),cost)){
                    int amount=Math.min(required,remaining[slot]);take[slot]+=amount;remaining[slot]-=amount;required-=amount;
                }
                if(required>0){if(shortage==null){shortage=cost;shortageCount=required;}affordable=false;break;}
            }
            if(!affordable)continue;
            var paid=new ArrayList<ItemStack>();boolean complete=true;
            for(int slot=0;slot<take.length;slot++)if(take[slot]>0){var expected=inventory.getStackInSlot(slot).copy();var actual=inventory.extractItem(slot,take[slot],false);paid.add(actual);if(actual.getCount()!=take[slot]||!ItemStack.isSameItemSameComponents(expected,actual)){complete=false;break;}}
            if(!complete){for(var item:paid)MayorResources.insert(inventory,item,false);return false;}
            research.startResearch(local);colony.getResearchManager().markDirty();university.markDirty();
            if(university.getTileEntity()!=null)university.getTileEntity().setChanged();
            mayor.report="Started native research with university materials: "+research.getId();return true;
        }
        if(shortage!=null){
            for(var donor:colony.getServerBuildingManager().getBuildings().values()){
                if(donor==university||!ColonyIntegration.buildingAreaLoaded(level,donor))continue;
                var source=donor.getItemHandlerCap();if(source==null)continue;
                for(int slot=0;slot<source.getSlots();slot++){
                    var held=source.getStackInSlot(slot);if(!IGlobalResearch.isUniversityResearchMatch(held,shortage))continue;
                    int accepted=MayorResources.insert(inventory,held.copyWithCount(Math.min(shortageCount,held.getCount())),true);if(accepted==0)continue;
                    var actual=source.extractItem(slot,accepted,false);int inserted=MayorResources.insert(inventory,actual,false);
                    if(inserted<actual.getCount())MayorResources.insert(source,actual.copyWithCount(actual.getCount()-inserted),false);
                    if(inserted>0){university.overruleNextOpenRequestWithStack(actual.copyWithCount(inserted));university.markDirty();mayor.report="Transferred real research ingredients to the university";return true;}
                }
            }
            var wanted=shortage.ingredient().items().findFirst().map(holder->new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
            if(!wanted.isEmpty()&&IGlobalResearch.isUniversityResearchMatch(wanted,shortage)){
                wanted.setCount(Math.min(wanted.getMaxStackSize(),shortageCount));
                boolean requested=university.getOpenRequests(-1).stream().anyMatch(r->r.getRequest() instanceof com.minecolonies.api.colony.requestsystem.requestable.IDeliverable delivery&&delivery.matches(wanted));
                if(!requested){university.createRequest(new com.minecolonies.api.colony.requestsystem.requestable.Stack(wanted),false);mayor.report="Requested research ingredients through native production/delivery";return true;}
            }
        }
        return false;
    }
}
