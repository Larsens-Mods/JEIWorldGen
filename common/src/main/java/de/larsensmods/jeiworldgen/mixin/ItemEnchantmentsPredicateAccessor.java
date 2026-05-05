package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ItemEnchantmentsPredicate.class)
public interface ItemEnchantmentsPredicateAccessor {

    @Accessor("enchantments")
    List<EnchantmentPredicate> jeiwg$enchantments();

}
