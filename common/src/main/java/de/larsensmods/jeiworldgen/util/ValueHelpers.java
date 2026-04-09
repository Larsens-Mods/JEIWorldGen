package de.larsensmods.jeiworldgen.util;

import net.minecraft.world.level.levelgen.VerticalAnchor;

public class ValueHelpers {

    public static int vertAnchorToInt(VerticalAnchor anchor, boolean applyOverworldContext){
        if(anchor instanceof VerticalAnchor.Absolute absolute){
            return absolute.y();
        }else if(anchor instanceof VerticalAnchor.AboveBottom aboveBottom){
            if(applyOverworldContext) {
                return -64 + aboveBottom.offset();
            }else{
                return aboveBottom.offset();
            }
        }else if(anchor instanceof VerticalAnchor.BelowTop belowTop){
            if(applyOverworldContext) {
                return 320 - belowTop.offset();
            }else{
                return 256 - belowTop.offset();
            }
        }else {
            try {
                return Integer.parseInt(anchor.toString().split(" ")[0]);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }

}
