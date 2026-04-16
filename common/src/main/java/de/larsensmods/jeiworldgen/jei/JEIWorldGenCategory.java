package de.larsensmods.jeiworldgen.jei;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.utils.RenderUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JEIWorldGenCategory implements IRecipeCategory<WorldGenTypeHelper> {

    public static final int COORDS_BASE_X = 29;
    public static final int COORDS_BASE_Y = 80;
    public static final int COORDS_SIZE_X = 128;
    public static final int COORDS_SIZE_Y = 70;

    private final IJeiHelpers helpers;
    private final IDrawable background, icon;

    public JEIWorldGenCategory(IJeiHelpers helpers) {
        this.helpers = helpers;
        this.background = new BGDrawable(ResourceLocation.fromNamespaceAndPath(JEIWorldGenMod.MOD_ID, "textures/gui/category_bg.png"), 156, 80);
        this.icon = this.helpers.getGuiHelper().createDrawable(ResourceLocation.fromNamespaceAndPath(JEIWorldGenMod.MOD_ID, "textures/gui/category_icon.png"), 0, 0, 16, 16);
    }

    @Override
    public @NotNull RecipeType<WorldGenTypeHelper> getRecipeType() {
        return WorldGenTypeHelper.RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jeiwg.title");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        //return this.icon;
        return this.helpers.getGuiHelper().createDrawableItemLike(Items.GRASS_BLOCK);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WorldGenTypeHelper recipe, IFocusGroup focuses) {
        builder.addOutputSlot(6, 6)
                .addItemStacks(recipe.blocks.stream().toList());
    }

    @Override
    public int getWidth() {
        return this.background.getWidth();
    }

    @Override
    public int getHeight() {
        return this.background.getHeight();
    }

    @Override
    public void draw(WorldGenTypeHelper recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        //Draw Background
        this.background.draw(guiGraphics);

        //Draw Coords Grid
        RenderUtils.drawLine(guiGraphics, COORDS_BASE_X, COORDS_BASE_Y, COORDS_BASE_X + COORDS_SIZE_X, COORDS_BASE_Y, 0xFF444444);
        RenderUtils.drawLine(guiGraphics, COORDS_BASE_X, COORDS_BASE_Y, COORDS_BASE_X, COORDS_BASE_Y - COORDS_SIZE_Y, 0xFF444444);

        recipe.drawInfo(recipe, getWidth(), getHeight(), guiGraphics, mouseX, mouseY);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, WorldGenTypeHelper recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        recipe.getTooltip(tooltip, recipe, mouseX, mouseY);
    }
}
