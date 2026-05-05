package de.larsensmods.jeiworldgen.util;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LootDataBuilder {

    public static LootData fromRaw(ReloadableServerRegistries.Holder lootRegistry, WorldGenInfo wgInfo){
        LootData data = new LootData();
        for(OreGenData.BiomeData biomeData : wgInfo.data().biomeData.values()){
            for(OreGenData.OreData oreData : biomeData.ores){
                for(ItemStack itemStack : oreData.getTargets()){
                    if(data.knownBlock(itemStack.getItemHolder())){
                        continue;
                    }
                    Item item = itemStack.getItem();
                    if(item instanceof BlockItem bItem){
                        Block block = bItem.getBlock();
                        LootTable table = lootRegistry.getLootTable(block.getLootTable());
                        for(LootPool pool : ((LootTableAccessor) table).jeiwg$pools()){
                            List<LootPoolEntryContainer> entries = ((LootPoolAccessor) pool).jeiwg$entries();
                            data.addLootData(itemStack.getItemHolder(), unwrapLootEntry(entries));
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
                List<LootItemCondition> lootConditions = ((LootPoolEntryContainerAccessor) lootItem).jeiwg$conditions();
                List<LootItemFunction> lootFunctions = ((LootPoolSingletonContainerAccessor) lootItem).jeiwg$functions();

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
                JEIWorldGenMod.LOGGER.warn("Found unsupported loot entry type: {}", entry.getType());
            }
        }

        return blockLootData;
    }

    private static boolean hasSilkTouchCondition(List<LootItemCondition> conditions){
        for(LootItemCondition condition : conditions){
            if(condition instanceof MatchTool(java.util.Optional<ItemPredicate> pred) && pred.isPresent()){
                ItemPredicate toolPredicate = pred.get();
                if(toolPredicate.subPredicates().containsKey(ItemSubPredicates.ENCHANTMENTS)){
                    if(toolPredicate.subPredicates().get(ItemSubPredicates.ENCHANTMENTS) instanceof ItemEnchantmentsPredicate predicate){
                        for(EnchantmentPredicate enchantment : ((ItemEnchantmentsPredicateAccessor) predicate).jeiwg$enchantments()){
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
                NumberProvider provider = ((SetItemCountFunctionAccessor) countFunction).jeiwg$value();
                return minFromNumberProvider(provider);
            }
        }
        return 1;
    }

    private static int calcMaxCount(List<LootItemFunction> functions){
        for(LootItemFunction function : functions){
            if(function instanceof SetItemCountFunction countFunction){
                NumberProvider provider = ((SetItemCountFunctionAccessor) countFunction).jeiwg$value();
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

    private static int minFromNumberProvider(NumberProvider provider){
        switch (provider) {
            case ConstantValue constant -> {
                return (int) constant.value();
            }
            case BinomialDistributionGenerator ignored -> {
                return 0;
            }
            case UniformGenerator uniform -> {
                return minFromNumberProvider(uniform.min());
            }
            default -> {
                JEIWorldGenMod.LOGGER.warn("Found unsupported loot item function number provider type (min): {}", provider.getType());
                return Integer.MIN_VALUE;
            }
        }
    }

    private static int maxFromNumberProvider(NumberProvider provider){
        switch (provider) {
            case ConstantValue constant -> {
                return (int) constant.value();
            }
            case BinomialDistributionGenerator binomial -> {
                return maxFromNumberProvider(binomial.n());
            }
            case UniformGenerator uniform -> {
                return minFromNumberProvider(uniform.max());
            }
            default -> {
                JEIWorldGenMod.LOGGER.warn("Found unsupported loot item function number provider type (max): {}", provider.getType());
                return Integer.MAX_VALUE;
            }
        }
    }

}
