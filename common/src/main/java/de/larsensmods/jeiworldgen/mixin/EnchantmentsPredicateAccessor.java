package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(EnchantmentsPredicate.class)
public interface EnchantmentsPredicateAccessor {

    @Accessor("enchantments")
    List<EnchantmentPredicate> jeiwg$enchantments();

}
