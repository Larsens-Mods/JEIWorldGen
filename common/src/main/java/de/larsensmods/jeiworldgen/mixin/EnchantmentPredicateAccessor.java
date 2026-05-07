package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnchantmentPredicate.class)
public interface EnchantmentPredicateAccessor {

    @Accessor("enchantment")
    Enchantment jeiwg$enchantment();

}
