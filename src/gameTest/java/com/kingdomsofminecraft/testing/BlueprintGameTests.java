package com.kingdomsofminecraft.testing;

import com.kingdomsofminecraft.geography.BlueprintFootprint;
import com.kingdomsofminecraft.integration.BlueprintSiteInspector;
import com.kingdomsofminecraft.integration.BlueprintPreflightCommands;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.IColony;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.List;

/** Disposable fixtures test preflight only; no construction or production governor claims. */
public final class BlueprintGameTests {
    private BlueprintGameTests() {}
    public static void verify(GameTestHelper helper,IColony colony,ServerPlayer player) {
        var level=helper.getLevel();
        var origin=helper.absolutePos(BlockPos.ZERO);
        var chunk=level.getChunk(origin);
        int x=chunk.getPos().getMinBlockX()+8,z=chunk.getPos().getMinBlockZ()+8;
        // GameTest clears/rebuilds terrain around relocated tests. Establish a deterministic
        // disposable dry platform above that scaffolding, instead of assuming natural ground.
        var anchor=new BlockPos(x,origin.getY()+10,z);
        var saved=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=-1;dy<=2;dy++) {
            var pos=anchor.offset(dx,dy,dz);
            var state=level.getBlockState(pos);
            helper.assertTrue(!state.hasBlockEntity(),"Disposable test platform must not replace inventories");
            saved.put(pos,state);
        }
        try {
            saved.keySet().forEach(pos -> level.setBlock(pos,pos.getY()==anchor.getY()-1?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),2));
            verifyAt(helper,colony,player,anchor);
        } finally {saved.forEach((pos,state)->level.setBlock(pos,state,2));}
        helper.assertTrue(saved.entrySet().stream().allMatch(e -> level.getBlockState(e.getKey()).equals(e.getValue())),"Disposable platform restored");
    }
    private static void verifyAt(GameTestHelper helper,IColony colony,ServerPlayer player,BlockPos anchor) {
        var level=helper.getLevel();
        var origin=helper.absolutePos(BlockPos.ZERO);
        short[][][] data=new short[3][3][3];data[0][1][1]=1;
        var blueprint=new Blueprint((short)3,(short)3,(short)3,(short)2,
                List.of(Blocks.AIR.defaultBlockState(),com.minecolonies.api.blocks.ModBlocks.blockHutBuilder.defaultBlockState()),
                data,new net.minecraft.nbt.CompoundTag[0],List.of(),level.registryAccess());
        var inspector=new BlueprintSiteInspector();
        var owner=colony.getPermissions().getOwner();
        var before=level.getBlockState(anchor);
        var good=inspector.inspect(level,player,anchor,blueprint);
        helper.assertTrue(good.passed(),"Owner's flat dry loaded footprint passes preflight: "+good.detail());
        var foreign=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("2ee82f5e-613a-4349-bc15-daf2f6f88247"),"UntrustedPlanner"));
        helper.assertTrue(!inspector.inspect(level,foreign,anchor,blueprint).passed(),"Untrusted player cannot pass colony permissions");
        helper.assertTrue(!inspector.inspect(level,player,anchor.above(10),blueprint).passed(),"Excessive fill rejected");
        helper.assertTrue(!inspector.inspect(level,player,anchor.below(10),blueprint).passed(),"Excessive cutting rejected");
        var remote=new BlockPos(5000000,80,5000000);
        helper.assertTrue(!inspector.inspect(level,player,remote,blueprint).passed(),"Unloaded footprint rejected");
        helper.assertTrue(!level.hasChunk(312500,312500),"Blueprint preflight cannot generate remote chunks");
        boolean boundaryTested=false;
        int baseX=Math.floorDiv(origin.getX(),16),baseZ=Math.floorDiv(origin.getZ(),16);
        for(int cx=baseX-16;cx<=baseX+16 && !boundaryTested;cx++)for(int cz=baseZ-16;cz<=baseZ+16 && !boundaryTested;cz++) {
            if(level.getChunkSource().getChunkNow(cx,cz)!=null && level.getChunkSource().getChunkNow(cx+1,cz)==null) {
                var edge=new BlockPos(cx*16+15,anchor.getY(),cz*16+8);
                var result=inspector.inspect(level,player,edge,blueprint);
                helper.assertTrue(!result.passed() && result.detail().contains("loaded"),"Loaded anchor with unloaded blueprint corner rejected");
                helper.assertTrue(!level.hasChunk(cx+1,cz),"Footprint guard does not generate neighboring chunk");
                boundaryTested=true;
            }
        }
        helper.assertTrue(boundaryTested,"Exercise an actual loaded/unloaded chunk boundary");
        // A corner lies between the previous 25-column sampler's offsets.
        var hazard=anchor.offset(1,0,1);
        var saved=level.getBlockState(hazard);
        try {
            for(var block:List.of(Blocks.LAVA,Blocks.WATER,Blocks.CHEST,Blocks.BEDROCK)) {
                level.setBlock(hazard,block.defaultBlockState(),2);
                helper.assertTrue(!inspector.inspect(level,player,anchor,blueprint).passed(),"Full-volume hazard rejected: "+block);
                if(block==Blocks.CHEST) {
                    var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(hazard);
                    chest.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
                    inspector.inspect(level,player,anchor,blueprint);
                    helper.assertTrue(chest.getItem(0).getCount()==7,"Preflight preserves existing inventory contents");
                }
            }
        } finally {level.setBlock(hazard,saved,2);}
        helper.assertTrue(level.getBlockState(anchor).equals(before) && level.getBlockState(hazard).equals(saved),"Inspector preserves blocks and fixture restored");
        helper.assertTrue(colony.getPermissions().getOwner().equals(owner) && colony.getServerBuildingManager().getBuildings().isEmpty(),"Preflight never changes owner or queues buildings");
        var actual=StructurePacks.getBlueprint(colony.getStructurePack(),"fundamentals/builder1.blueprint",level.registryAccess());
        helper.assertTrue(actual!=null,"Actual upstream builder blueprint loads");
        var offset=actual.getPrimaryBlockOffset();
        var bounds=BlueprintFootprint.at(actual.getSizeX(),actual.getSizeY(),actual.getSizeZ(),offset.getX(),offset.getY(),offset.getZ(),remote.getX(),remote.getY(),remote.getZ());
        helper.assertTrue(bounds.maxX()-bounds.minX()+1==actual.getSizeX(),"Use actual upstream blueprint dimensions");
        helper.assertTrue(!inspector.inspect(level,player,remote,actual).passed() && !level.hasChunk(312500,312500),"Actual blueprint respects unloaded boundaries");
        try {
            var messages=new java.util.ArrayList<String>();
            var sink=new net.minecraft.commands.CommandSource() {
                public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                    helper.assertTrue(level.getServer().isSameThread(),"Preflight feedback/world work stays on server thread");
                    messages.add(message.getString());
                }
                public boolean acceptsSuccess(){return true;}
                public boolean acceptsFailure(){return true;}
                public boolean shouldInformAdmins(){return false;}
            };
            var source=player.createCommandSourceStack().withSource(sink).withPosition(net.minecraft.world.phys.Vec3.atLowerCornerOf(anchor));
            helper.assertTrue(level.getServer().getCommands().getDispatcher().execute("kingdoms plan \"Minecolonies Original\" \"../secret.blueprint\"",source)==0,"Invalid blueprint path rejected before IO");
            helper.assertTrue(level.getServer().getCommands().getDispatcher().execute("kingdoms plan \"Missing Pack\" \"builder1.blueprint\"",source)==0,"Unknown pack rejected");
            String command="kingdoms plan \"Minecolonies Original\" \"fundamentals/builder1.blueprint\"";
            helper.assertTrue(level.getServer().getCommands().getDispatcher().execute(command,source)==1,"Real blueprint inspection queued");
            helper.assertTrue(level.getServer().getCommands().getDispatcher().execute(command,source)==0,"Concurrent request/cooldown is bounded");
            helper.runAfterDelay(100,() -> helper.assertTrue(messages.stream().anyMatch(m -> m.startsWith("Blueprint preflight passed:") || m.startsWith("Blueprint preflight rejected:")),"Async real blueprint returns an inspection result, not an exception"));
            // Independent command lifecycle: invalidate a pending IO callback exactly as server stop does.
            var cancelledMessages=new java.util.ArrayList<String>();
            var cancelledSink=new net.minecraft.commands.CommandSource() {
                public void sendSystemMessage(net.minecraft.network.chat.Component message){cancelledMessages.add(message.getString());}
                public boolean acceptsSuccess(){return true;}
                public boolean acceptsFailure(){return true;}
                public boolean shouldInformAdmins(){return false;}
            };
            var commands=new BlueprintPreflightCommands();
            var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
            dispatcher.register(commands.command());
            helper.assertTrue(dispatcher.execute(command.substring("kingdoms ".length()),source.withSource(cancelledSink))==1,"Isolated lifecycle request queued");
            int countAtClear=cancelledMessages.size();
            commands.clear();
            helper.runAfterDelay(100,() -> helper.assertTrue(cancelledMessages.size()==countAtClear,"Cleared lifecycle produces no later feedback, even if IO finished before clear"));
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw helper.assertionException(error.getMessage());}
    }
}
