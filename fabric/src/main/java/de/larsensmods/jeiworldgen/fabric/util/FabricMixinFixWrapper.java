package de.larsensmods.jeiworldgen.fabric.util;

import de.larsensmods.jeiworldgen.util.MixinFixWrapper;
import net.fabricmc.fabric.mixin.loot.LootTableAccessor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

public class FabricMixinFixWrapper implements MixinFixWrapper {
    @Override
    public Iterable<LootPool> getPools(LootTable table) {
        return List.of(((LootTableAccessor) table).fabric_getPools());
    }
}
