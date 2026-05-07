package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemPredicate.class)
public interface ItemPredicateAccessor {

    @Accessor("enchantments")
    EnchantmentPredicate[] jeiwg$enchantments();

}
