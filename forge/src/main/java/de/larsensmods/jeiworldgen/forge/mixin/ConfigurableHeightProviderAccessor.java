package de.larsensmods.jeiworldgen.forge.mixin;

import mekanism.common.world.height.ConfigurableHeightProvider;
import mekanism.common.world.height.ConfigurableHeightRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConfigurableHeightProvider.class)
public interface ConfigurableHeightProviderAccessor {

    @Accessor("range")
    ConfigurableHeightRange jeiwg$range();

}
