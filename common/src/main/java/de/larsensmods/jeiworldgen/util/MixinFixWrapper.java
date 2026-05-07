package de.larsensmods.jeiworldgen.util;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

public interface MixinFixWrapper {

    Iterable<LootPool> getPools(LootTable table);

}
