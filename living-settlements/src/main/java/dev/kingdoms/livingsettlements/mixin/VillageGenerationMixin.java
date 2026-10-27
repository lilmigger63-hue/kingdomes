package dev.kingdoms.livingsettlements.mixin;

import dev.kingdoms.livingsettlements.VillageReplacement;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Predicate;

@Mixin(Structure.class)
public abstract class VillageGenerationMixin {
    @Inject(method="generate", at=@At("RETURN"), cancellable=true)
    private void livingSettlements$select(Holder<Structure> selected, ResourceKey<Level> dimension, RegistryAccess registry,
        ChunkGenerator generator, BiomeSource biomes, RandomState random, StructureTemplateManager templates,
        long seed, ChunkPos chunk, int references, LevelHeightAccessor heights, Predicate<Holder<Biome>> validBiomes,
        CallbackInfoReturnable<StructureStart> callback) {
        var id = selected.unwrapKey().map(ResourceKey::identifier).orElse(null);
        if (!VillageReplacement.eligible(id)) return;
        var context = new Structure.GenerationContext(registry, generator, biomes, random, templates, seed, chunk, heights, validBiomes);
        callback.setReturnValue(VillageReplacement.replace(callback.getReturnValue(), context, id, dimension.identifier().toString()));
    }
}
