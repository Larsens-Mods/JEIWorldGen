package de.larsensmods.jeiworldgen.util;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ints.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;

import java.util.*;

public class LootDataBuilder {

    public static LootData fromRaw(ReloadableServerRegistries.Holder lootRegistry, WorldGenInfo wgInfo){
        LootData data = new LootData();
        for(OreGenData.BiomeData biomeData : wgInfo.data().biomeData.values()){
            for(OreGenData.OreData oreData : biomeData.ores){
                for(ItemStackTemplate itemStack : oreData.getTargets()){
                    if(data.knownBlock(itemStack.item())){
                        continue;
                    }
                    Item item = itemStack.item().value();
                    if(item instanceof BlockItem bItem){
                        Block block = bItem.getBlock();
                        if(block.getLootTable().isPresent()) {
                            LootTable table = lootRegistry.getLootTable(block.getLootTable().get());
                            for (LootPool pool : ((LootTableAccessor) table).jeiwg$pools()) {
                                List<LootPoolEntryContainer> entries = ((LootPoolAccessor) pool).jeiwg$entries();
                                data.addLootData(itemStack.item(), unwrapLootEntry(entries));
                            }
                        }
                    }else{
                        JEIWorldGenMod.LOGGER.warn("Found non-block item in ore target list: {}", item.getDescriptionId());
                    }
                }
            }
        }
        return data;
    }

    private static Set<LootData.BlockLootData> unwrapLootEntry(List<LootPoolEntryContainer> entries){
        Set<LootData.BlockLootData> blockLootData = new HashSet<>();
        for(LootPoolEntryContainer entry : entries){
            if(entry instanceof LootItem lootItem){
                Holder<Item> itemHolder = ((LootItemAccessor) lootItem).jeiwg$item();

                Optional<Holder<LootItemCondition>> lootConditionHolder = ((LootPoolEntryContainerAccessor) lootItem).jeiwg$condition();
                Optional<Holder<LootItemFunction>> lootFunctionHolder = ((LootPoolEntryContainerAccessor) lootItem).jeiwg$modifier();

                List<LootItemCondition> lootConditions = List.of();
                List<LootItemFunction> lootFunctions = List.of();

                if(lootConditionHolder.isPresent()){
                    lootConditions = new ArrayList<>();
                    LootItemCondition baseCondition = lootConditionHolder.get().value();
                    if(baseCondition instanceof CompositeLootItemCondition compositeCondition){
                        for(Holder<LootItemCondition> holder : ((CompositeLootItemConditionAccessor) compositeCondition).jeiwg$terms()){
                            lootConditions.add(holder.value());
                        }
                    }else{
                        lootConditions.add(baseCondition);
                    }
                }
                if(lootFunctionHolder.isPresent()){
                    lootFunctions = new ArrayList<>();
                    LootItemFunction baseFunction = lootFunctionHolder.get().value();
                    if(baseFunction instanceof SequenceFunction sequenceFunction){
                        for(Holder<LootItemFunction> holder : ((SequenceFunctionAccessor) sequenceFunction).jeiwg$functions()){
                            lootFunctions.add(holder.value());
                        }
                    }else{
                        lootFunctions.add(baseFunction);
                    }
                }

                LootData.ItemDropData itemDropData = new LootData.ItemDropData(itemHolder.value());
                itemDropData.silkTouchOnly = hasSilkTouchCondition(lootConditions);
                itemDropData.minCount = calcMinCount(lootFunctions);
                itemDropData.maxCount = calcMaxCount(lootFunctions);
                itemDropData.affectedByFortune = hasFortuneBonus(lootFunctions);

                blockLootData.add(itemDropData);
            }else if(entry instanceof AlternativesEntry alternativesEntry){
                List<LootPoolEntryContainer> children = ((CompositeEntryBaseAccessor) alternativesEntry).jeiwg$children();

                blockLootData.add(new LootData.AlternativesLootData(unwrapLootEntry(children)));
            }else{
                JEIWorldGenMod.LOGGER.warn("Found unsupported loot entry type: {}", entry.getClass().getName());
            }
        }

        return blockLootData;
    }

    private static boolean hasSilkTouchCondition(List<LootItemCondition> conditions){
        for(LootItemCondition condition : conditions){
            if(condition instanceof MatchTool(java.util.Optional<ItemPredicate> pred) && pred.isPresent()){
                ItemPredicate toolPredicate = pred.get();
                if(toolPredicate.components().partial().containsKey(DataComponentPredicates.ENCHANTMENTS)){
                    if(toolPredicate.components().partial().get(DataComponentPredicates.ENCHANTMENTS) instanceof EnchantmentsPredicate predicate){
                        for(EnchantmentPredicate enchantment : ((EnchantmentsPredicateAccessor) predicate).jeiwg$enchantments()){
                            for(Holder<Enchantment> enchHolder : enchantment.enchantments().orElseGet(HolderSet::empty)){
                                if(enchHolder.is(Enchantments.SILK_TOUCH)){
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static int calcMinCount(List<LootItemFunction> functions){
        for(LootItemFunction function : functions){
            if(function instanceof SetItemCountFunction countFunction){
                ContextIntProvider provider = ((SetItemCountFunctionAccessor) countFunction).jeiwg$count().value();
                return minFromNumberProvider(provider);
            }
        }
        return 1;
    }

    private static int calcMaxCount(List<LootItemFunction> functions){
        for(LootItemFunction function : functions){
            if(function instanceof SetItemCountFunction countFunction){
                ContextIntProvider provider = ((SetItemCountFunctionAccessor) countFunction).jeiwg$count().value();
                return maxFromNumberProvider(provider);
            }
        }
        return 1;
    }

    private static boolean hasFortuneBonus(List<LootItemFunction> functions){
        for(LootItemFunction function : functions){
            if(function instanceof ApplyBonusCount bonusCount){
                if(((ApplyBonusCountAccessor) bonusCount).jeiwg$enchantment().is(Enchantments.FORTUNE)){
                    return true;
                }
            }
        }
        return false;
    }

    private static int minFromNumberProvider(ContextIntProvider provider){
        switch (provider) {
            case ConstantValue constant -> {
                return (int) constant.value();
            }
            case BinomialDistributionGenerator ignored -> {
                return 0;
            }
            case UniformGenerator uniform -> {
                return minFromNumberProvider(uniform.min().value());
            }
            default -> {
                JEIWorldGenMod.LOGGER.warn("Found unsupported loot item function number provider type (min): {}", provider.getClass().getName());
                return Integer.MIN_VALUE;
            }
        }
    }

    private static int maxFromNumberProvider(ContextIntProvider provider){
        switch (provider) {
            case ConstantValue constant -> {
                return (int) constant.value();
            }
            case BinomialDistributionGenerator binomial -> {
                return maxFromNumberProvider(binomial.n().value());
            }
            case UniformGenerator uniform -> {
                return minFromNumberProvider(uniform.max().value());
            }
            default -> {
                JEIWorldGenMod.LOGGER.warn("Found unsupported loot item function number provider type (max): {}", provider.getClass().getName());
                return Integer.MAX_VALUE;
            }
        }
    }

}
