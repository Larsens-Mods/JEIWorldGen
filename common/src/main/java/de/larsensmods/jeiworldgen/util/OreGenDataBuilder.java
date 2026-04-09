package de.larsensmods.jeiworldgen.util;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.OreGenData;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OreGenDataBuilder {

    public static OreGenData fromRaw(Map<ResourceKey<Biome>, HolderSet<PlacedFeature>> biomeOreFeatures, Map<ResourceKey<Biome>, HolderSet<PlacedFeature>> biomeDecoFeatures){
        OreGenData data = new OreGenData();

        Set<ResourceKey<Biome>> mergedBiomeSet = new HashSet<>();
        mergedBiomeSet.addAll(biomeOreFeatures.keySet());
        mergedBiomeSet.addAll(biomeDecoFeatures.keySet());

        for(ResourceKey<Biome> biome : mergedBiomeSet){
            OreGenData.BiomeData biomeData = new OreGenData.BiomeData();

            Set<Holder<PlacedFeature>> mergedHolderSet = new HashSet<>();
            mergedHolderSet.addAll(biomeOreFeatures.get(biome).stream().toList());
            mergedHolderSet.addAll(biomeDecoFeatures.get(biome).stream().toList());

            for(Holder<PlacedFeature> placedFeatureHolder : mergedHolderSet){
                PlacedFeature placed = placedFeatureHolder.value();
                ConfiguredFeature<?, ?> configured = placed.feature().value();
                List<PlacementModifier> placement = placed.placement();
                FeatureConfiguration config = configured.config();
                if(config instanceof OreConfiguration oreConfig){
                    Set<ItemStack> targets = new HashSet<>();
                    oreConfig.targetStates.forEach(targetState -> targets.add(new ItemStack(targetState.state.getBlock())));

                    CountPlacement countModifier = null;
                    RarityFilter rarityFilter = null;
                    HeightRangePlacement heightModifier = null;

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

            data.addBiomeData(biome.location(), biomeData);
        }

        return data;
    }

}
