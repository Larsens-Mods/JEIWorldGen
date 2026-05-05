package de.larsensmods.jeiworldgen.jei;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.client.LootData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        List<ItemStack> genBlockStacks = recipe.blocks.stream().toList();
        builder.addInputSlot(6, 6)
                .addItemStacks(genBlockStacks);
        if(ClientDataStore.LOOT_INFO != null) {
            List<ItemStack> dropStacks = new ArrayList<>();
            List<List<Component>> tooltipLines = new ArrayList<>();
            LootData lootData = ClientDataStore.LOOT_INFO.data();
            for (ItemStack block : genBlockStacks) {
                Set<LootData.BlockLootData> blockLootData = lootData.dataForEntry(block.getItemHolder());
                this.addMissingLoot(dropStacks, tooltipLines, block, blockLootData, genBlockStacks);
            }
            List<List<ItemStack>> outputStacks = new ArrayList<>();
            List<List<Component>> outputTooltipLines = new ArrayList<>();

            List<ItemStack> selfStacks = new ArrayList<>();

            for(int i = 0; i < dropStacks.size(); i++){
                boolean contained = false;
                for(ItemStack genStack : genBlockStacks) {
                    if(ItemStack.isSameItemSameComponents(genStack, dropStacks.get(i))){
                        contained = true;
                        break;
                    }
                }
                if(selfStacks != null && contained){
                    selfStacks.add(dropStacks.get(i));
                }else if(selfStacks != null && !selfStacks.isEmpty()){
                    outputStacks.add(selfStacks);
                    outputTooltipLines.add(tooltipLines.getFirst());
                    selfStacks = null;

                    outputStacks.add(List.of(dropStacks.get(i)));
                    outputTooltipLines.add(tooltipLines.get(i));
                }else{
                    outputStacks.add(List.of(dropStacks.get(i)));
                    outputTooltipLines.add(tooltipLines.get(i));
                }
            }
            if(selfStacks != null && !selfStacks.isEmpty()){
                outputStacks.add(selfStacks);
                outputTooltipLines.add(tooltipLines.getFirst());
            }

            for(int i = 0; i < Math.min(outputStacks.size(), 3); i++){
                int finalI = i;
                builder.addOutputSlot(6, 6 + 26 + (i * 18))
                        .addItemStacks(outputStacks.get(i))
                        .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                            tooltip.addAll(outputTooltipLines.get(finalI));
                        });
            }
        }
    }

    private void addMissingLoot(List<ItemStack> dropStackList, List<List<Component>> tooltipLines, ItemStack block, Set<LootData.BlockLootData> lootData, List<ItemStack> genBlockStacks){
        for(LootData.BlockLootData data : lootData){
            if(data instanceof LootData.ItemDropData itemData){
                ItemStack stack = itemData.dropItem.getItem().getDefaultInstance();
                boolean contained = false;
                for(ItemStack existingStack : dropStackList){
                    if(ItemStack.isSameItemSameComponents(existingStack, stack)){
                        contained = true;
                        break;
                    }
                }
                boolean isGenStack = false;
                for(ItemStack genStack : genBlockStacks){
                    if(ItemStack.isSameItem(genStack, stack)){
                        isGenStack = true;
                        break;
                    }
                }
                if(!contained){
                    List<Component> tooltip = new ArrayList<>();

                    if(itemData.minCount != 1 || itemData.minCount != itemData.maxCount){
                        tooltip.add(Component.translatable("jeiwg.loot_info.count", itemData.minCount, itemData.maxCount));
                    }
                    if(itemData.affectedByFortune){
                        tooltip.add(Component.translatable("jeiwg.loot_info.fortune"));
                    }
                    if(itemData.silkTouchOnly){
                        tooltip.add(Component.translatable("jeiwg.loot_info.silk_touch"));
                    }

                    if(isGenStack){
                        dropStackList.addFirst(stack);
                        tooltipLines.addFirst(tooltip);
                    }else {
                        dropStackList.add(stack);
                        tooltipLines.add(tooltip);
                    }
                }
            }else if(data instanceof LootData.AlternativesLootData altData){
                this.addMissingLoot(dropStackList, tooltipLines, block, altData.alternatives, genBlockStacks);
            }else{
                JEIWorldGenMod.LOGGER.warn("Unknown loot data type for block {}: {}", block.getItem(), data.getClass().getName());
            }
        }
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
