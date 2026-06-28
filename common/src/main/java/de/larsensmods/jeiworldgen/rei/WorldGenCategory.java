package de.larsensmods.jeiworldgen.rei;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.client.LootData;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorldGenCategory implements DisplayCategory<WorldGenTypeHelper> {

    public static final CategoryIdentifier<WorldGenTypeHelper> WORLD_GEN = CategoryIdentifier.of(JEIWorldGenMod.MOD_ID, "world_generation");

    public static final int COORDS_BASE_X = 29;
    public static final int COORDS_BASE_Y = 80;
    public static final int COORDS_SIZE_X = 145;
    public static final int COORDS_SIZE_Y = 65;

    private final int width = 185, height = 90;

    @Override
    public CategoryIdentifier<? extends WorldGenTypeHelper> getCategoryIdentifier() {
        return WORLD_GEN;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jeiwg.title");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.GRASS_BLOCK);
    }

    @Override
    public int getDisplayHeight() {
        return height;
    }

    @Override
    public int getDisplayWidth(WorldGenTypeHelper display) {
        return width;
    }

    @Override
    public List<Widget> setupDisplay(WorldGenTypeHelper display, Rectangle bounds) {
        Point startingPoint = new Point(bounds.x, bounds.y);

        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        List<Component> biomeHover = new ArrayList<>();
        biomeHover.add(Component.translatable("jeiwg.biomes"));
        biomeHover.addAll(display.getBiomeInfo().stream().map(Component::literal).toList());
        widgets.add(Widgets.createLabel(new Point(startingPoint.x + 26, startingPoint.y + 4), Component.literal(display.getBiomeString())).tooltip(biomeHover.toArray(new Component[0])).leftAligned());

        widgets.add(Widgets.createDrawableWidget((p1, mouseX, mouseY, p4) -> display.drawTooltip(mouseX, mouseY, startingPoint)));
        widgets.add(Widgets.createDrawableWidget((graphics, p2, p3, p4) -> display.drawInfo(graphics, startingPoint)));

        List<ItemStack> genBlockStacks = display.blocks.stream().toList();
        widgets.add(Widgets.createSlot(new Point(startingPoint.x + 6, startingPoint.y + 6)).entries(genBlockStacks.stream().map(EntryStacks::of).toList()).markInput());

        widgets.add(Widgets.createSlotBackground(new Point(startingPoint.x + 6, startingPoint.y + 6 + 26)));
        widgets.add(Widgets.createSlotBackground(new Point(startingPoint.x + 6, startingPoint.y + 6 + 26 + (18))));
        widgets.add(Widgets.createSlotBackground(new Point(startingPoint.x + 6, startingPoint.y + 6 + 26 + (2 * 18))));

        if(ClientDataStore.LOOT_INFO != null) {
            List<ItemStack> dropStacks = new ArrayList<>();
            List<List<Component>> tooltipLines = new ArrayList<>();
            LootData lootData = ClientDataStore.LOOT_INFO.data();
            for (ItemStack block : genBlockStacks) {
                Set<LootData.BlockLootData> blockLootData = lootData.dataForEntry(block.getItem());
                addMissingLoot(dropStacks, tooltipLines, block, blockLootData, genBlockStacks);
            }
            List<List<ItemStack>> outputStacks = new ArrayList<>();
            List<List<Component>> outputTooltipLines = new ArrayList<>();

            List<ItemStack> selfStacks = new ArrayList<>();

            for(int i = 0; i < dropStacks.size(); i++){
                boolean contained = false;
                for(ItemStack genStack : genBlockStacks) {
                    if(ItemStack.isSameItemSameTags(genStack, dropStacks.get(i))){
                        contained = true;
                        break;
                    }
                }
                if(selfStacks != null && contained){
                    selfStacks.add(dropStacks.get(i));
                }else if(selfStacks != null && !selfStacks.isEmpty()){
                    outputStacks.add(selfStacks);
                    outputTooltipLines.add(tooltipLines.get(0));
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
                outputTooltipLines.add(tooltipLines.get(0));
            }

            for(int i = 0; i < Math.min(outputStacks.size(), 3); i++){
                int finalI = i;
                widgets.add(Widgets.createSlot(new Point(startingPoint.x + 6, startingPoint.y + 6 + 26 + (i * 18))).disableBackground().entries(
                        outputStacks.get(i).stream().map(EntryStacks::of).peek(entry -> entry.tooltip(outputTooltipLines.get(finalI))).toList()
                ).markOutput());
            }
        }

        return widgets;
    }

    public static void addMissingLoot(List<ItemStack> dropStackList, List<List<Component>> tooltipLines, ItemStack block, Set<LootData.BlockLootData> lootData, List<ItemStack> genBlockStacks){
        for(LootData.BlockLootData data : lootData){
            if(data instanceof LootData.ItemDropData itemData){
                ItemStack stack = itemData.dropItem;
                boolean contained = false;
                for(ItemStack existingStack : dropStackList){
                    if(ItemStack.isSameItemSameTags(existingStack, stack)){
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
                        dropStackList.add(0, stack);
                        tooltipLines.add(0, tooltip);
                    }else {
                        dropStackList.add(stack);
                        tooltipLines.add(tooltip);
                    }
                }
            }else if(data instanceof LootData.AlternativesLootData altData){
                addMissingLoot(dropStackList, tooltipLines, block, altData.alternatives, genBlockStacks);
            }else{
                JEIWorldGenMod.LOGGER.warn("Unknown loot data type for block {}: {}", block.getItem(), data.getClass().getName());
            }
        }
    }
}
