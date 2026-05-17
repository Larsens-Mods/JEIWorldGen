package de.larsensmods.jeiworldgen.jei;

import mezz.jei.api.gui.drawable.IDrawableStatic;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class BGDrawable implements IDrawableStatic {

    private final Identifier texture;
    private final int width;
    private final int height;

    public BGDrawable(Identifier texture, int width, int height){
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return this.width + 10;
    }

    @Override
    public int getHeight() {
        return this.height + 10;
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, xOffset + 5, yOffset + 5, 0, 0, width, height, 256, 256);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset, int maskTop, int maskBottom, int maskLeft, int maskRight) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.texture, width, height, 0, 0, xOffset + 5, yOffset + 5, 256, 256);
    }
}
