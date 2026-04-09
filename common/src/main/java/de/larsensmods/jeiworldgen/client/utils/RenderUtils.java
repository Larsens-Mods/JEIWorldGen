package de.larsensmods.jeiworldgen.client.utils;

import net.minecraft.client.gui.GuiGraphics;

public class RenderUtils {

    public static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color){
        graphics.fill(x1, y1, x1 == x2 ? x2 + 1 : x2, y1 == y2 ? y2 + 1 : y2, color);
    }

}
