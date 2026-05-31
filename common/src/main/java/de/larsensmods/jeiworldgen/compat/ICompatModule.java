package de.larsensmods.jeiworldgen.compat;

import de.larsensmods.jeiworldgen.client.OreGenData;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Set;

public interface ICompatModule {

    Set<OreGenData.OreData> processBiome(ResourceKey<Biome> biome, Set<Holder<PlacedFeature>> mergedHolderSet, MinecraftServer minecraftServer);

}
