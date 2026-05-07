package de.larsensmods.jeiworldgen.util;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.HashSet;
import java.util.Set;

public class LootDataBuilder {

    public static LootData fromRaw(LootDataManager lootDataManager, WorldGenInfo wgInfo){
        LootData data = new LootData();
        for(OreGenData.BiomeData biomeData : wgInfo.getData().biomeData.values()){
            for(OreGenData.OreData oreData : biomeData.ores){
                for(ItemStack itemStack : oreData.getTargets()){
                    if(data.knownBlock(itemStack.getItem())){
                        continue;
                    }
                    Item item = itemStack.getItem();
                    if(item instanceof BlockItem bItem){
                        Block block = bItem.getBlock();
                        LootTable table = lootDataManager.getLootTable(block.getLootTable());
                        for(LootPool pool : JEIWorldGenMod.getMixinWrapper().getPools(table)){
                            LootPoolEntryContainer[] entries = ((LootPoolAccessor) pool).jeiwg$entries();
                            data.addLootData(itemStack.getItem(), unwrapLootEntry(entries));
                        }
                    }else{
                        JEIWorldGenMod.LOGGER.warn("Found non-block item in ore target list: {}", item.getDescriptionId());
                    }
                }
            }
        }
        return data;
    }

    private static Set<LootData.BlockLootData> unwrapLootEntry(LootPoolEntryContainer[] entries){
        Set<LootData.BlockLootData> blockLootData = new HashSet<>();
        for(LootPoolEntryContainer entry : entries){
            if(entry instanceof LootItem lootItem){
                Item item = ((LootItemAccessor) lootItem).jeiwg$item();
                LootItemCondition[] lootConditions = ((LootPoolEntryContainerAccessor) lootItem).jeiwg$conditions();
                LootItemFunction[] lootFunctions = ((LootPoolSingletonContainerAccessor) lootItem).jeiwg$functions();

                LootData.ItemDropData itemDropData = new LootData.ItemDropData(item);
                itemDropData.silkTouchOnly = hasSilkTouchCondition(lootConditions);
                itemDropData.minCount = calcMinCount(lootFunctions);
                itemDropData.maxCount = calcMaxCount(lootFunctions);
                itemDropData.affectedByFortune = hasFortuneBonus(lootFunctions);

                blockLootData.add(itemDropData);
            }else if(entry instanceof AlternativesEntry alternativesEntry){
                LootPoolEntryContainer[] children = ((CompositeEntryBaseAccessor) alternativesEntry).jeiwg$children();

                blockLootData.add(new LootData.AlternativesLootData(unwrapLootEntry(children)));
            }else{
                JEIWorldGenMod.LOGGER.warn("Found unsupported loot entry type: {}", entry.getType());
            }
        }

        return blockLootData;
    }

    private static boolean hasSilkTouchCondition(LootItemCondition[] conditions){
        for(LootItemCondition condition : conditions){
            if(condition instanceof MatchTool matchTool){
                ItemPredicate toolPredicate = ((MatchToolAccessor) matchTool).jeiwg$predicate();
                for(EnchantmentPredicate enchantment : ((ItemPredicateAccessor) toolPredicate).jeiwg$enchantments()){
                    if(((EnchantmentPredicateAccessor) enchantment).jeiwg$enchantment().equals(Enchantments.SILK_TOUCH)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int calcMinCount(LootItemFunction[] functions){
        for(LootItemFunction function : functions){
            if(function instanceof SetItemCountFunction countFunction){
                NumberProvider provider = ((SetItemCountFunctionAccessor) countFunction).jeiwg$value();
                return minFromNumberProvider(provider);
            }
        }
        return 1;
    }

    private static int calcMaxCount(LootItemFunction[] functions){
        for(LootItemFunction function : functions){
            if(function instanceof SetItemCountFunction countFunction){
                NumberProvider provider = ((SetItemCountFunctionAccessor) countFunction).jeiwg$value();
                return maxFromNumberProvider(provider);
            }
        }
        return 1;
    }

    private static boolean hasFortuneBonus(LootItemFunction[] functions){
        for(LootItemFunction function : functions){
            if(function instanceof ApplyBonusCount bonusCount){
                if(((ApplyBonusCountAccessor) bonusCount).jeiwg$enchantment().equals(Enchantments.BLOCK_FORTUNE)){
                    return true;
                }
            }
        }
        return false;
    }

    private static int minFromNumberProvider(NumberProvider provider){
        if(provider instanceof ConstantValue constantValue){
            return (int) constantValue.getFloat(null);
        }else if(provider instanceof BinomialDistributionGenerator){
            return 0;
        }else if(provider instanceof UniformGenerator uniformGenerator){
            return minFromNumberProvider(((UniformGeneratorAccessor) uniformGenerator).jeiwg$min());
        }else{
            JEIWorldGenMod.LOGGER.warn("Found unsupported loot item function number provider type (min): {}", provider.getType());
            return Integer.MIN_VALUE;
        }
    }

    private static int maxFromNumberProvider(NumberProvider provider){
        if(provider instanceof ConstantValue constantValue){
            return (int) constantValue.getFloat(null);
        }else if(provider instanceof BinomialDistributionGenerator){
            JEIWorldGenMod.LOGGER.warn("Binomial distribution currently not supported because the class is final and i have no interest in fighting around with access transformers since they always break on forge and i have no desire to fight around with that shit!");
            return 0;
        }else if(provider instanceof UniformGenerator uniformGenerator){
            return minFromNumberProvider(((UniformGeneratorAccessor) uniformGenerator).jeiwg$max());
        }else{
            JEIWorldGenMod.LOGGER.warn("Found unsupported loot item function number provider type (max): {}", provider.getType());
            return Integer.MAX_VALUE;
        }
    }

}
