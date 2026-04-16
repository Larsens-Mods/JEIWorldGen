package de.larsensmods.jeiworldgen.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class BGDrawable implements IDrawable {

    private final ResourceLocation texture;
    private final int width;
    private final int height;

    public BGDrawable(ResourceLocation texture, int width, int height){
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
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        guiGraphics.blit(this.texture, xOffset + 5, yOffset + 5, 0, 0, width, height, 256, 256);
    }
}
