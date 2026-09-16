package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Optional;

@Mixin(LootPoolEntryContainer.class)
public interface LootPoolEntryContainerAccessor {

    @Accessor("condition")
    Optional<Holder<LootItemCondition>> jeiwg$condition();

    @Accessor("modifier")
    Optional<Holder<LootItemFunction>> jeiwg$modifier();

}
