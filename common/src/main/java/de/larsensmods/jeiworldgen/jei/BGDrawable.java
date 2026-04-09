package de.larsensmods.jeiworldgen.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

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
        RenderSystem.clearColor(1, 1, 1, 1);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.texture);
        this.drawTexturedModalRect(guiGraphics, xOffset + 5, yOffset + 5, 0, 0, this.width, this.height, 0);
        RenderSystem.applyModelViewMatrix();
    }

    private void drawTexturedModalRect(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height, float zLevel) {
        final float uScale = 1f / 0x100;
        final float vScale = 1f / 0x100;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder wr = tesselator.getBuilder();
        wr.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        wr.vertex(matrix, x, y + height, zLevel).uv(u * uScale, ((v + height) * vScale)).endVertex();
        wr.vertex(matrix, x + width, y + height, zLevel).uv((u + width) * uScale, ((v + height) * vScale)).endVertex();
        wr.vertex(matrix, x + width, y, zLevel).uv((u + width) * uScale, (v * vScale)).endVertex();
        wr.vertex(matrix, x, y, zLevel).uv(u * uScale, (v * vScale)).endVertex();
        tesselator.end();
    }
}
