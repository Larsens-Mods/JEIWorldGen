package de.larsensmods.jeiworldgen.mixin;

import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrapezoidHeight.class)
public interface TrapezoidHeightAccessor {

    @Accessor("minInclusive")
    VerticalAnchor jeiwg$minInclusive();

    @Accessor("maxInclusive")
    VerticalAnchor jeiwg$maxInclusive();

    @Accessor("plateau")
    int jeiwg$plateau();

}
