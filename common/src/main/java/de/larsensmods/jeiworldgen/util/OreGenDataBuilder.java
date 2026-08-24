package de.larsensmods.jeiworldgen.util;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.compat.ICompatModule;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OreGenDataBuilder {

    private static final int[] MAX_BLOCK_TABLE =
            {0, 0, 0, 4, 5, 8, 9, 10, 10, 13,
                    16, 17, 23, 24, 24, 29, 32, 37, 46, 52,
                    52, 60, 68, 68, 74, 82, 94, 104, 106, 120,
                    128, 135, 149, 160, 180, 190, 204, 212, 228, 246,
                    262, 276, 292, 308, 324, 344, 360, 381, 403, 429,
                    452, 480, 500, 530, 558, 584, 616, 634, 664, 694,
                    730, 760, 790, 826, 864};

    public static OreGenData fromRaw(Map<ResourceKey<Biome>, HolderSet<PlacedFeature>> biomeOreFeatures, Map<ResourceKey<Biome>, HolderSet<PlacedFeature>> biomeDecoFeatures, Set<ICompatModule> compatModules, MinecraftServer minecraftServer) {
        OreGenData data = new OreGenData();

        Set<ResourceKey<Biome>> mergedBiomeSet = new HashSet<>();
        mergedBiomeSet.addAll(biomeOreFeatures.keySet());
        mergedBiomeSet.addAll(biomeDecoFeatures.keySet());

        for(ResourceKey<Biome> biome : mergedBiomeSet){
            OreGenData.BiomeData biomeData = new OreGenData.BiomeData();

            Set<Holder<PlacedFeature>> mergedHolderSet = new HashSet<>();
            mergedHolderSet.addAll(biomeOreFeatures.getOrDefault(biome, HolderSet.empty()).stream().toList());
            mergedHolderSet.addAll(biomeDecoFeatures.getOrDefault(biome, HolderSet.empty()).stream().toList());

            for(Holder<PlacedFeature> placedFeatureHolder : mergedHolderSet){
                PlacedFeature placed = placedFeatureHolder.value();
                ConfiguredFeature<?, ?> configured = placed.feature().value();
                List<PlacementModifier> placement = placed.placement();
                FeatureConfiguration config = configured.config();
                boolean isScatter = (placed.feature().value().feature() instanceof ScatteredOreFeature);
                if(config instanceof OreConfiguration oreConfig){
                    Set<ItemStack> targets = new HashSet<>();
                    oreConfig.targetStates.forEach(targetState -> targets.add(new ItemStack(targetState.state.getBlock())));

                    CountPlacement countModifier = null;
                    RarityFilter rarityFilter = null;
                    HeightRangePlacement heightModifier = null;

                    int size = isScatter || oreConfig.size >= MAX_BLOCK_TABLE.length ? oreConfig.size : MAX_BLOCK_TABLE[oreConfig.size];

                    for(PlacementModifier modifier : placement){
                        if(modifier.type().equals(PlacementModifierType.COUNT)){
                            countModifier = (CountPlacement) modifier;
                        }else if(modifier.type().equals(PlacementModifierType.RARITY_FILTER)){
                            rarityFilter = (RarityFilter) modifier;
                        } else if(modifier.type().equals(PlacementModifierType.HEIGHT_RANGE)){
                            heightModifier = (HeightRangePlacement) modifier;
                        }
                    }
                    if(countModifier != null && heightModifier != null) {
                        biomeData.addOreData(new OreGenData.OreData(targets, oreConfig.size, countModifier, heightModifier));
                    }else if(rarityFilter != null && heightModifier != null){
                        biomeData.addOreData(new OreGenData.OreData(targets, oreConfig.size, rarityFilter, heightModifier));
                    }else if(countModifier == null && rarityFilter == null && heightModifier != null){
                        biomeData.addOreData(new OreGenData.OreData(targets, oreConfig.size, CountPlacement.of(1), heightModifier));
                    }else{
                        JEIWorldGenMod.LOGGER.warn("Missing data for {} in {}", placedFeatureHolder, biome.location());
                        for(PlacementModifier mod : placement){
                            JEIWorldGenMod.LOGGER.info(" - Modifier: {} with type {}", mod, mod.type());
                        }
                    }
                }
            }

            for(ICompatModule compatModule : compatModules){
                for(OreGenData.OreData compatData : compatModule.processBiome(biome, mergedHolderSet, minecraftServer)){
                    biomeData.addOreData(compatData);
                }
            }

            data.addBiomeData(biome.location(), biomeData);
        }

        return data;
    }

}
