package de.larsensmods.jeiworldgen.forge.compat;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.compat.ICompatModule;
import de.larsensmods.jeiworldgen.forge.mixin.ConfigurableHeightProviderAccessor;
import de.larsensmods.jeiworldgen.mixin.HeightRangePlacementAccessor;
import mekanism.common.world.ResizableOreFeatureConfig;
import mekanism.common.world.height.ConfigurableHeightProvider;
import mekanism.common.world.height.ConfigurableHeightRange;
import mekanism.common.world.height.HeightShape;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MekanismCompatModule implements ICompatModule {

    @Override
    public Set<OreGenData.OreData> processBiome(ResourceKey<Biome> biome, Set<Holder<PlacedFeature>> mergedHolderSet, MinecraftServer minecraftServer) {
        Set<OreGenData.OreData> oreGenData = new HashSet<>();

        for(Holder<PlacedFeature> placedFeatureHolder : mergedHolderSet){
            PlacedFeature placed = placedFeatureHolder.value();
            ConfiguredFeature<?, ?> configured = placed.feature().value();
            List<PlacementModifier> placement = placed.placement();
            FeatureConfiguration config = configured.config();
            if(config instanceof ResizableOreFeatureConfig oreConfig){
                Set<ItemStack> targets = new HashSet<>();
                oreConfig.targetStates().forEach(targetState -> targets.add(new ItemStack(targetState.state.getBlock())));

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

                if(heightModifier != null){
                    HeightProvider original = ((HeightRangePlacementAccessor) heightModifier).jeiwg$height();
                    if(original instanceof ConfigurableHeightProvider mekanismProvider){
                        ConfigurableHeightRange mekanismRange = ((ConfigurableHeightProviderAccessor) mekanismProvider).jeiwg$range();

                        HeightShape shape = mekanismRange.shape().get();
                        int plateau = mekanismRange.plateau().getAsInt();
                        VerticalAnchor minHeight, maxHeight;
                        switch(mekanismRange.minInclusive().anchorType().get()){
                            case ABSOLUTE -> minHeight = VerticalAnchor.absolute(mekanismRange.minInclusive().value().getOrDefault());
                            case ABOVE_BOTTOM -> minHeight = VerticalAnchor.aboveBottom(mekanismRange.minInclusive().value().getOrDefault());
                            case BELOW_TOP -> minHeight = VerticalAnchor.belowTop(mekanismRange.minInclusive().value().getOrDefault());
                            default -> minHeight = VerticalAnchor.absolute(0);
                        }
                        switch(mekanismRange.maxInclusive().anchorType().get()){
                            case ABSOLUTE -> maxHeight = VerticalAnchor.absolute(mekanismRange.maxInclusive().value().getOrDefault());
                            case ABOVE_BOTTOM -> maxHeight = VerticalAnchor.aboveBottom(mekanismRange.maxInclusive().value().getOrDefault());
                            case BELOW_TOP -> maxHeight = VerticalAnchor.belowTop(mekanismRange.maxInclusive().value().getOrDefault());
                            default -> maxHeight = VerticalAnchor.absolute(0);
                        }

                        switch (shape){
                            case UNIFORM -> heightModifier = HeightRangePlacement.uniform(minHeight, maxHeight);
                            case TRAPEZOID -> heightModifier = HeightRangePlacement.of(TrapezoidHeight.of(minHeight, maxHeight, plateau));
                        }
                    }
                }

                if(countModifier != null && heightModifier != null) {
                    oreGenData.add(new OreGenData.OreData(targets, oreConfig.size().getAsInt(), countModifier, heightModifier));
                }else if(rarityFilter != null && heightModifier != null){
                    oreGenData.add(new OreGenData.OreData(targets, oreConfig.size().getAsInt(), rarityFilter, heightModifier));
                }else if(countModifier == null && rarityFilter == null && heightModifier != null){
                    oreGenData.add(new OreGenData.OreData(targets, oreConfig.size().getAsInt(), CountPlacement.of(1), heightModifier));
                }else{
                    JEIWorldGenMod.LOGGER.warn("Missing data for {} in {}", placedFeatureHolder, biome.location());
                    for(PlacementModifier mod : placement){
                        JEIWorldGenMod.LOGGER.info(" - Modifier: {} with type {}", mod, mod.type());
                    }
                }
            }
        }
        return oreGenData;
    }

}
