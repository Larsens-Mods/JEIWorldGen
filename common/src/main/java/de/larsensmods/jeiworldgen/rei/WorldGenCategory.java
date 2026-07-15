package de.larsensmods.jeiworldgen.rei;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.gui.BiomeListScreen;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Label;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
        biomeHover.add(Component.translatable("jeiwg.biomes").withStyle(ChatFormatting.BOLD));
        biomeHover.addAll(display.getBiomeInfoComponent());
        widgets.add(Widgets.createLabel(new Point(startingPoint.x + 26, startingPoint.y + 4), Component.literal(display.getBiomeString())).tooltip(biomeHover.toArray(new Component[0])).leftAligned().clickable().onClick(label -> {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            );
            Screen currentScreen = Minecraft.getInstance().gui.screen();
            Minecraft.getInstance().setScreenAndShow(
                    new BiomeListScreen(display.getBiomeStrings(), currentScreen)
            );
        }));

        widgets.add(Widgets.createDrawableWidget((_, mouseX, mouseY, _) -> display.drawTooltip(mouseX, mouseY, startingPoint)));
        widgets.add(Widgets.createDrawableWidget((graphics, _, _, _) -> display.drawInfo(graphics, startingPoint)));

        List<ItemStack> genBlockStacks = display.blocks.stream().map(ItemStackTemplate::create).toList();
        widgets.add(Widgets.createSlot(new Point(startingPoint.x + 6, startingPoint.y + 6)).entries(genBlockStacks.stream().map(EntryStacks::of).toList()).markInput());

        widgets.add(Widgets.createSlotBackground(new Point(startingPoint.x + 6, startingPoint.y + 6 + 26)));
        widgets.add(Widgets.createSlotBackground(new Point(startingPoint.x + 6, startingPoint.y + 6 + 26 + (18))));
        widgets.add(Widgets.createSlotBackground(new Point(startingPoint.x + 6, startingPoint.y + 6 + 26 + (2 * 18))));

        if(ClientDataStore.LOOT_INFO != null) {
            List<ItemStack> dropStacks = new ArrayList<>();
            List<List<Component>> tooltipLines = new ArrayList<>();
            LootData lootData = ClientDataStore.LOOT_INFO.data();
            for (ItemStack block : genBlockStacks) {
                Set<LootData.BlockLootData> blockLootData = lootData.dataForEntry(Holder.direct(block.getItem()));
                addMissingLoot(dropStacks, tooltipLines, block, blockLootData, genBlockStacks);
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
                ItemStack stack = itemData.dropItem.create();
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
                addMissingLoot(dropStackList, tooltipLines, block, altData.alternatives, genBlockStacks);
            }else{
                JEIWorldGenMod.LOGGER.warn("Unknown loot data type for block {}: {}", block.getItem(), data.getClass().getName());
            }
        }
    }
}
