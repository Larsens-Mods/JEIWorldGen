package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SequenceFunction.class)
public interface SequenceFunctionAccessor {

    @Accessor("functions")
    HolderSet<LootItemFunction> jeiwg$functions();

}
