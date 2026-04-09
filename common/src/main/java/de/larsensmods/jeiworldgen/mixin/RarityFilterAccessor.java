package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.world.level.levelgen.placement.RarityFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RarityFilter.class)
public interface RarityFilterAccessor {

    @Accessor("chance")
    int jeiwg$chance();

}
