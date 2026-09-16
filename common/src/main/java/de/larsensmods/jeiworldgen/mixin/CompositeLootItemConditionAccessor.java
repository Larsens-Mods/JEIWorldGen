package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompositeLootItemCondition.class)
public interface CompositeLootItemConditionAccessor {

    @Accessor("terms")
    HolderSet<LootItemCondition> jeiwg$terms();

}
