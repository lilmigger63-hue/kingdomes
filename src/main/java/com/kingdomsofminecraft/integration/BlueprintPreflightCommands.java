package com.kingdomsofminecraft.integration;

import com.kingdomsofminecraft.geography.BlueprintFootprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** One bounded read-only request at a time. Blueprint IO runs upstream; all world reads run on-server. */
public final class BlueprintPreflightCommands {
    private final BlueprintSiteInspector inspector=new BlueprintSiteInspector();
    private boolean pending;
    private long generation,nextTick;
    public void clear(){pending=false;nextTick=0;generation++;}
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("plan")
                .then(Commands.argument("pack",StringArgumentType.string())
                .then(Commands.argument("blueprint",StringArgumentType.string()).executes(context -> {
                    var source=context.getSource();
                    var player=source.getPlayerOrException();
                    var level=source.getLevel();
                    var server=source.getServer();
                    String pack=StringArgumentType.getString(context,"pack"),path=StringArgumentType.getString(context,"blueprint");
                    if(!BlueprintFootprint.validReference(pack,path) || StructurePacks.getStructurePack(pack)==null) {
                        source.sendFailure(Component.literal("Choose a registered MineColonies pack and a relative .blueprint path"));return 0;
                    }
                    long now=server.getTickCount();
                    if(pending || now<nextTick){source.sendFailure(Component.literal("Blueprint inspection busy; try again shortly"));return 0;}
                    if(player.level()!=level){source.sendFailure(Component.literal("Inspect from your current dimension"));return 0;}
                    var anchor=BlockPos.containing(source.getPosition());
                    pending=true;nextTick=now+20;
                    long request=++generation;
                    source.sendSuccess(() -> Component.literal("Inspecting original blueprint orientation at "+anchor.toShortString()),false);
                    StructurePacks.getBlueprintFuture(pack,path,level.registryAccess()).whenComplete((blueprint,error) -> server.execute(() -> {
                        if(request!=generation)return;
                        pending=false;
                        if(server.isStopped() || player.isRemoved() || player.level()!=level)return;
                        if(error!=null || blueprint==null){source.sendFailure(Component.literal("Blueprint could not be loaded from the registered pack"));return;}
                        try {
                            var result=inspector.inspect(level,player,anchor,blueprint);
                            source.sendSuccess(() -> Component.literal("Blueprint preflight "+(result.passed()?"passed":"rejected")+": "+result.detail()),false);
                        } catch(RuntimeException failure) {
                            source.sendFailure(Component.literal("Blueprint preflight failed; no construction requested"));
                            com.mojang.logging.LogUtils.getLogger().warn("Blueprint preflight failed for {}:{}",pack,path,failure);
                        }
                    }));
                    return 1;
                })));
    }
}
