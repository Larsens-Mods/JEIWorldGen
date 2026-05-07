package de.larsensmods.jeiworldgen.forge.util;

import de.larsensmods.jeiworldgen.forge.mixin.LootTableAccessor;
import de.larsensmods.jeiworldgen.util.MixinFixWrapper;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

public class ForgeMixinFixWrapper implements MixinFixWrapper {
    @Override
    public Iterable<LootPool> getPools(LootTable table) {
        return ((LootTableAccessor) table).jeiwg$pools();
    }
}
