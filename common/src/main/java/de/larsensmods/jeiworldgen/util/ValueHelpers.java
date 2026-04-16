package de.larsensmods.jeiworldgen.util;

import net.minecraft.world.level.levelgen.VerticalAnchor;

public class ValueHelpers {

    public static int vertAnchorToInt(VerticalAnchor anchor, boolean applyOverworldContext){
        switch (anchor) {
            case VerticalAnchor.Absolute absolute -> {
                return absolute.y();
            }
            case VerticalAnchor.AboveBottom aboveBottom -> {
                if (applyOverworldContext) {
                    return -64 + aboveBottom.offset();
                } else {
                    return aboveBottom.offset();
                }
            }
            case VerticalAnchor.BelowTop belowTop -> {
                if (applyOverworldContext) {
                    return 320 - belowTop.offset();
                } else {
                    return 256 - belowTop.offset();
                }
            }
            default -> {
                try {
                    return Integer.parseInt(anchor.toString().split(" ")[0]);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
    }

}
