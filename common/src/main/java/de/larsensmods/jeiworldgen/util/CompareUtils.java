package de.larsensmods.jeiworldgen.util;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.mixin.CountPlacementAccessor;
import de.larsensmods.jeiworldgen.mixin.HeightRangePlacementAccessor;
import de.larsensmods.jeiworldgen.mixin.RarityFilterAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.heightproviders.*;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.Set;

public class CompareUtils {

    public static boolean areItemStackSetsEqual(Set<ItemStack> set1, Set<ItemStack> set2){
        return itemStackSetTestOneWay(set1, set2) && itemStackSetTestOneWay(set2, set1);
    }

    private static boolean itemStackSetTestOneWay(Set<ItemStack> set1, Set<ItemStack> set2){
        for(ItemStack stack1 : set1){
            boolean foundMatch = false;
            for(ItemStack stack2 : set2){
                if(ItemStack.isSameItem(stack1, stack2)){
                    foundMatch = true;
                    break;
                }
            }
            if(!foundMatch){
                return false;
            }
        }
        return true;
    }

    public static boolean areResourceLocationSetsEqual(Set<ResourceLocation> set1, Set<ResourceLocation> set2){
        for(ResourceLocation rl1 : set1){
            if(!set2.contains(rl1)) {
                return false;
            }
        }
        for (ResourceLocation rl2 : set2){
            if(!set1.contains(rl2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean countPlacementEquals(CountPlacement a, CountPlacement b){
        if(a == null && b == null){
            return true;
        }else if(a == null || b == null){
            return false;
        }
        IntProvider aProvider = ((CountPlacementAccessor) a).jeiwg$count();
        IntProvider bProvider = ((CountPlacementAccessor) b).jeiwg$count();
        return aProvider.getType().equals(bProvider.getType()) && aProvider.getMinValue() == bProvider.getMinValue() && aProvider.getMaxValue() == bProvider.getMaxValue();
    }

    public static boolean rarityFilterEquals(RarityFilter a, RarityFilter b){
        if(a == null && b == null){
            return true;
        }else if(a == null || b == null){
            return false;
        }
        return ((RarityFilterAccessor) a).jeiwg$chance() == ((RarityFilterAccessor) b).jeiwg$chance();
    }

    public static boolean heightPlacementEquals(HeightRangePlacement a, HeightRangePlacement b){
        if(a == null && b == null){
            return true;
        }else if(a == null || b == null){
            return false;
        }
        HeightProvider aProvider = ((HeightRangePlacementAccessor) a).jeiwg$height();
        HeightProvider bProvider = ((HeightRangePlacementAccessor) b).jeiwg$height();
        if(aProvider.getType().equals(bProvider.getType())){
            if(aProvider.getType().equals(HeightProviderType.CONSTANT)){
                ConstantHeight aHeight = (ConstantHeight) aProvider;
                ConstantHeight bHeight = (ConstantHeight) bProvider;
                return aHeight.toString().equals(bHeight.toString());
            }else if(aProvider.getType().equals(HeightProviderType.UNIFORM)){
                UniformHeight aHeight = (UniformHeight) aProvider;
                UniformHeight bHeight = (UniformHeight) bProvider;
                return aHeight.toString().equals(bHeight.toString());
            }else if(aProvider.getType().equals(HeightProviderType.BIASED_TO_BOTTOM)){
                BiasedToBottomHeight aHeight = (BiasedToBottomHeight) aProvider;
                BiasedToBottomHeight bHeight = (BiasedToBottomHeight) bProvider;
                return aHeight.toString().equals(bHeight.toString());
            }else if(aProvider.getType().equals(HeightProviderType.VERY_BIASED_TO_BOTTOM)){
                VeryBiasedToBottomHeight aHeight = (VeryBiasedToBottomHeight) aProvider;
                VeryBiasedToBottomHeight bHeight = (VeryBiasedToBottomHeight) bProvider;
                return aHeight.toString().equals(bHeight.toString());
            }else if(aProvider.getType().equals(HeightProviderType.TRAPEZOID)){
                TrapezoidHeight aHeight = (TrapezoidHeight) aProvider;
                TrapezoidHeight bHeight = (TrapezoidHeight) bProvider;
                return aHeight.toString().equals(bHeight.toString());
            }else if(aProvider.getType().equals(HeightProviderType.WEIGHTED_LIST)){
                WeightedListHeight aHeight = (WeightedListHeight) aProvider;
                WeightedListHeight bHeight = (WeightedListHeight) bProvider;
                return aHeight.equals(bHeight); //TODO: Maybe change if relevant
            }else{
                JEIWorldGenMod.LOGGER.error("Encountered unknown HeightProviderType, assuming inequality");
                return false;
            }
        }
        return false;
    }

}
