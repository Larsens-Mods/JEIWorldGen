package de.larsensmods.jeiworldgen.emi;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.client.utils.RenderUtils;
import de.larsensmods.jeiworldgen.config.ConfigManager;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.util.CompareUtils;
import de.larsensmods.jeiworldgen.util.ValueHelpers;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.heightproviders.*;

import java.util.*;

public class WorldGenTypeHelper extends BasicEmiRecipe {

    public static final Set<ResourceLocation> NON_OVERWORLD_BIOMES = Set.of(
            Biomes.NETHER_WASTES.location(),
            Biomes.SOUL_SAND_VALLEY.location(),
            Biomes.CRIMSON_FOREST.location(),
            Biomes.WARPED_FOREST.location(),
            Biomes.BASALT_DELTAS.location(),
            Biomes.THE_END.location(),
            Biomes.SMALL_END_ISLANDS.location(),
            Biomes.END_MIDLANDS.location(),
            Biomes.END_HIGHLANDS.location(),
            Biomes.END_BARRENS.location()
    );

    public static final int COORDS_BASE_X = 29;
    public static final int COORDS_BASE_Y = 80;
    public static final int COORDS_SIZE_X = 145;
    public static final int COORDS_SIZE_Y = 65;

    private static final Map<String, Integer> COMBINATION_COUNTS = new HashMap<>();

    public static List<WorldGenTypeHelper> buildRecipes(){
        JEIWorldGenMod.LOGGER.info("Building world gen JEI recipes");
        COMBINATION_COUNTS.clear();

        OreGenData data = ClientDataStore.WG_INFO != null ? ClientDataStore.WG_INFO.getData() : null;
        if(data == null){
            JEIWorldGenMod.LOGGER.warn("Could not find generation data");
            return List.of();
        }

        List<OreGenData.OreData> foundOreData = new ArrayList<>();
        List<Set<ResourceLocation>> dataBiomes = new ArrayList<>();

        for(Map.Entry<ResourceLocation, OreGenData.BiomeData> biomeEntry : data.biomeData.entrySet()){
            ResourceLocation biome = biomeEntry.getKey();
            OreGenData.BiomeData biomeData = biomeEntry.getValue();

            for(OreGenData.OreData oreData : biomeData.ores){
                if(foundOreData.contains(oreData)){
                    dataBiomes.get(foundOreData.indexOf(oreData)).add(biome);
                }else{
                    foundOreData.add(oreData);
                    Set<ResourceLocation> biomeSet = new HashSet<>();
                    biomeSet.add(biome);
                    dataBiomes.add(biomeSet);
                }
            }
        }

        List<WorldGenTypeHelper> output = new ArrayList<>();

        for(int i = 0; i < foundOreData.size(); i++){
            OreGenData.OreData oreData = foundOreData.get(i);
            Set<ResourceLocation> biomes = dataBiomes.get(i);
            JEIWorldGenMod.LOGGER.debug("Found ore gen data set for {} biomes: {}", biomes.size(), String.join(", ", biomes.stream().map(ResourceLocation::toString).toList()));

            Set<ItemStack> shownTargets = new HashSet<>();
            ItemStack baseTarget = null;
            for(ItemStack stack : oreData.getTargets()){
                if(!Set.of(ConfigManager.getConfig().hiddenBlocks()).contains(stack.getItemHolder().unwrapKey().orElseGet(() -> ResourceKey.create(Registries.ITEM, new ResourceLocation("air"))).location().toString())){
                    shownTargets.add(stack);
                    if(baseTarget == null){
                        baseTarget = stack;
                    }
                }else{
                    JEIWorldGenMod.LOGGER.info("Excluding {} from display, because its hidden in the config", stack.getItemHolder().unwrapKey().orElseGet(() -> ResourceKey.create(Registries.ITEM, new ResourceLocation("air"))).location());
                }
            }

            if(shownTargets.isEmpty()){
                JEIWorldGenMod.LOGGER.debug("Skipping empty entry");
                continue;
            }

            ResourceLocation sampleBiome = biomes.stream().toList().get(0);

            String baseTag = baseTarget.getItem().toString();
            int iteration = COMBINATION_COUNTS.getOrDefault(baseTag, 0);
            COMBINATION_COUNTS.put(baseTag, iteration + 1);
            WorldGenTypeHelper entry = new WorldGenTypeHelper(baseTag + "/" + iteration, biomes, shownTargets);

            float multiplier = 1;
            if(oreData.getCountPlacement() != null){
                IntProvider intProvider = ((CountPlacementAccessor) oreData.getCountPlacement()).jeiwg$count();
                multiplier = (intProvider.getMinValue() + intProvider.getMaxValue()) / 2f;
            }else if(oreData.getRarityFilter() != null){
                int chance = ((RarityFilterAccessor) oreData.getRarityFilter()).jeiwg$chance();
                multiplier = 1f / chance;
            }

            HeightProvider heightProvider = ((HeightRangePlacementAccessor) oreData.getHeightRangePlacement()).jeiwg$height();
            if(heightProvider.getType().equals(HeightProviderType.CONSTANT)){
                ConstantHeight aHeight = (ConstantHeight) heightProvider;
                try {
                    int height = Integer.parseInt(aHeight.toString().split(" ")[0]);
                    int amount = (int) Math.ceil(oreData.getSize() * multiplier);
                    entry.addDistributionCornerPoint(height, amount);
                }catch (NumberFormatException ignored){}

                JEIWorldGenMod.LOGGER.info("Has CONSTANT");
            }else if(heightProvider.getType().equals(HeightProviderType.UNIFORM)){
                UniformHeight aHeight = (UniformHeight) heightProvider;

                int minHeight = ValueHelpers.vertAnchorToInt(((UniformHeightAccessor) aHeight).jeiwg$minInclusive(), !NON_OVERWORLD_BIOMES.contains(sampleBiome));
                int maxHeight = ValueHelpers.vertAnchorToInt(((UniformHeightAccessor) aHeight).jeiwg$maxInclusive(), !NON_OVERWORLD_BIOMES.contains(sampleBiome));

                int height = maxHeight - minHeight;
                int amount = (int) Math.ceil((oreData.getSize() * multiplier) / height);

                entry.addDistributionCornerPoint(minHeight, amount);
                entry.addDistributionCornerPoint(maxHeight, amount);
            }else if(heightProvider.getType().equals(HeightProviderType.BIASED_TO_BOTTOM)){
                BiasedToBottomHeight aHeight = (BiasedToBottomHeight) heightProvider;

                JEIWorldGenMod.LOGGER.info("Has BIASED_TO_BOTTOM"); //TODO: UNUSED BY VANILLA, maybe by other mods
            }else if(heightProvider.getType().equals(HeightProviderType.VERY_BIASED_TO_BOTTOM)){
                VeryBiasedToBottomHeight aHeight = (VeryBiasedToBottomHeight) heightProvider;

                JEIWorldGenMod.LOGGER.info("Has VERY_BIASED_TO_BOTTOM"); //TODO: UNUSED BY VANILLA, maybe by other mods
            }else if(heightProvider.getType().equals(HeightProviderType.TRAPEZOID)){
                TrapezoidHeight aHeight = (TrapezoidHeight) heightProvider;

                int minHeight = ValueHelpers.vertAnchorToInt(((TrapezoidHeightAccessor) aHeight).jeiwg$minInclusive(), !NON_OVERWORLD_BIOMES.contains(sampleBiome));
                int maxHeight = ValueHelpers.vertAnchorToInt(((TrapezoidHeightAccessor) aHeight).jeiwg$maxInclusive(), !NON_OVERWORLD_BIOMES.contains(sampleBiome));
                int plateau = ((TrapezoidHeightAccessor) aHeight).jeiwg$plateau();

                if(plateau == 0){
                    int height = maxHeight - minHeight;
                    int amount = (int) Math.ceil((oreData.getSize() * multiplier) / height) * 2;

                    entry.addDistributionCornerPoint(minHeight, 0);
                    entry.addDistributionCornerPoint(minHeight + (height / 2), amount);
                    entry.addDistributionCornerPoint(maxHeight, 0);
                }else{
                    int height = maxHeight - minHeight;
                    int amount = (int) Math.ceil((oreData.getSize() * multiplier) / height) * 2; //TODO: Account for plateau

                    entry.addDistributionCornerPoint(minHeight, 0);
                    entry.addDistributionCornerPoint(minHeight + (height / 2) - (plateau / 2), amount);
                    entry.addDistributionCornerPoint(minHeight + (height / 2) + (plateau / 2), amount);
                    entry.addDistributionCornerPoint(maxHeight, 0);
                }
            }else if(heightProvider.getType().equals(HeightProviderType.WEIGHTED_LIST)){
                WeightedListHeight aHeight = (WeightedListHeight) heightProvider;

                JEIWorldGenMod.LOGGER.info("Has WEIGHTED_LIST"); //TODO: UNUSED BY VANILLA, maybe by other mods
            }else{
                JEIWorldGenMod.LOGGER.error("Encountered unknown HeightProviderType '{}'", heightProvider.getType().getClass().getSimpleName());
                continue;
            }

            output.add(entry);
        }

        return ConfigManager.getConfig().combineSimilarDatasets() ? mergeSimilar(output) : output;
    }

    private static List<WorldGenTypeHelper> mergeSimilar(List<WorldGenTypeHelper> fromList){
        List<WorldGenTypeHelper> merged = new ArrayList<>();

        Set<Integer> alreadyMerged = new HashSet<>();

        for(int a = 0; a < fromList.size(); a++){
            if(alreadyMerged.contains(a)){
                continue;
            }
            boolean doMerge = false;
            WorldGenTypeHelper entryA = fromList.get(a);
            Set<WorldGenTypeHelper> toMerge = new HashSet<>();
            toMerge.add(entryA);
            for(int b = 0; b < fromList.size(); b++) {
                WorldGenTypeHelper entryB = fromList.get(b);

                if (entryA.metaEquals(entryB)) {
                    toMerge.add(entryB);
                    alreadyMerged.add(b);
                    doMerge = true;
                }
            }
            if(doMerge){
                merged.add(new WorldGenTypeHelper.Merged(entryA.biomes, entryA.blocks, toMerge));
            }else{
                merged.add(entryA);
            }
        }

        return merged;
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

    //Implementation

    private final String recipeTag;

    public final Set<ResourceLocation> biomes;
    public final Set<ItemStack> blocks;

    protected final Set<int[]> distributionDrawParams = new HashSet<>();

    public WorldGenTypeHelper(String recipeTag, Set<ResourceLocation> biomes, Set<ItemStack> blocks) {
        super(EMIWGPlugin.WORLD_GEN_CATEGORY, new ResourceLocation(JEIWorldGenMod.MOD_ID, "/world_gen_recipe/" + recipeTag.replace("minecraft:", "").replace(':', '/')), 185, 95);
        this.biomes = biomes;
        this.blocks = blocks;

        this.recipeTag = recipeTag;

        this.inputs.addAll(blocks.stream().map(block -> EmiIngredient.of(Ingredient.of(block))).toList());
        this.outputs.addAll(getOutputEntries());
    }

    private void addDistributionCornerPoint(int height, int amount){
        this.distributionDrawParams.add(new int[]{height, amount});
    }

    public String getBiomeString(){
        return getBiomeInfo().get(0) + (biomes.size() > 1 ? " (+" + (biomes.size() - 1) + ")" : "");
    }

    public List<String> getBiomeInfo(){
        List<String> info = biomes.stream().map(ResourceLocation::toString).toList();
        if(info.size() > 5){
            info = new ArrayList<>(info.subList(0, 5));
            info.add(" + " + (biomes.size() - 5));
        }
        return info;
    }

    public String getTag() {
        return recipeTag;
    }

    public boolean metaEquals(WorldGenTypeHelper other){
        return CompareUtils.areItemStackSetsEqual(this.blocks, other.blocks) && CompareUtils.areResourceLocationSetsEqual(this.biomes, other.biomes);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        List<Component> biomeHover = new ArrayList<>();
        biomeHover.add(Component.translatable("jeiwg.biomes"));
        biomeHover.addAll(getBiomeInfo().stream().map(Component::literal).toList());
        Bounds textBounds = widgets.addText(Component.literal(getBiomeString()), 26, 4, 8, false).getBounds();
        widgets.addTooltipText(biomeHover, textBounds.x(), textBounds.y(), textBounds.width(), textBounds.height());

        widgets.addDrawable(COORDS_BASE_X, COORDS_BASE_Y, COORDS_SIZE_X, COORDS_SIZE_Y, this::drawInfo);
        widgets.addTooltip(this::drawTooltip, 0, 0, this.width, this.height);

        List<ItemStack> genBlockStacks = blocks.stream().toList();
        widgets.addSlot(EmiIngredient.of(genBlockStacks.stream().map(stack -> EmiIngredient.of(Ingredient.of(stack))).toList()), 6, 6);

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
                widgets.addSlot(EmiIngredient.of(
                        outputStacks.get(i).stream().map(Ingredient::of).map(EmiIngredient::of).toList()), 6, 6 + 26 + (i * 18))
                        .appendTooltip(outputTooltipLines.get(i).stream().reduce((component, component2) -> Component.empty().append(component).append(", ").append(component2)).orElse(Component.empty()))
                        .recipeContext(this);
            }
        }
    }

    private List<EmiStack> getOutputEntries() {
        List<ItemStack> genBlockStacks = blocks.stream().toList();
        if(ClientDataStore.LOOT_INFO != null) {
            List<ItemStack> dropStacks = new ArrayList<>();
            List<List<Component>> tooltipLines = new ArrayList<>();
            LootData lootData = ClientDataStore.LOOT_INFO.data();
            for (ItemStack block : genBlockStacks) {
                Set<LootData.BlockLootData> blockLootData = lootData.dataForEntry(block.getItem());
                addMissingLoot(dropStacks, tooltipLines, block, blockLootData, genBlockStacks);
            }
            List<List<ItemStack>> outputStacks = new ArrayList<>();

            List<ItemStack> selfStacks = new ArrayList<>();

            for (ItemStack dropStack : dropStacks) {
                boolean contained = false;
                for (ItemStack genStack : genBlockStacks) {
                    if (ItemStack.isSameItemSameTags(genStack, dropStack)) {
                        contained = true;
                        break;
                    }
                }
                if (selfStacks != null && contained) {
                    selfStacks.add(dropStack);
                } else if (selfStacks != null && !selfStacks.isEmpty()) {
                    outputStacks.add(selfStacks);
                    selfStacks = null;

                    outputStacks.add(List.of(dropStack));
                } else {
                    outputStacks.add(List.of(dropStack));
                }
            }
            if(selfStacks != null && !selfStacks.isEmpty()){
                outputStacks.add(selfStacks);
            }

            List<EmiStack> stacks = new ArrayList<>();
            outputStacks.forEach(stackList -> stacks.addAll(stackList.stream().map(EmiStack::of).toList()));

            return stacks;
        }else{
            return List.of();
        }
    }

    //GUI STUFF

    protected static final double drawHeightMultiplier = 3.5;

    protected int minY = -200, maxY = 500, displayHeight = 700;
    protected double scale = 1;

    protected int[] drawHeights = new int[0];

    private void drawInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.drawInfo(guiGraphics, 0, 0);
    }

    public void drawInfo(GuiGraphics graphics, int basePointX, int basePointY) {
        RenderUtils.drawLine(graphics, basePointX, basePointY, basePointX + COORDS_SIZE_X, basePointY, 0xFF444444);
        RenderUtils.drawLine(graphics, basePointX, basePointY, basePointX, basePointY - COORDS_SIZE_Y, 0xFF444444);

        recalcDrawMeta();

        drawGraphNumbering(graphics, basePointX, basePointY);

        for(int y = this.minY; y < this.maxY; y++){
            int drawHeight = this.drawHeights[y - this.minY];
            if(drawHeight > 0){
                if(drawHeight > COORDS_SIZE_Y){
                    RenderUtils.drawLine(graphics, basePointX + (int) Math.round(fromWorldHeight(y)), basePointY - COORDS_SIZE_Y, basePointX + (int) Math.round(fromWorldHeight(y)), basePointY, 0xFFFF9900);
                }else {
                    RenderUtils.drawLine(graphics, basePointX + (int) Math.round(fromWorldHeight(y)), basePointY - drawHeight, basePointX + (int) Math.round(fromWorldHeight(y)), basePointY, 0xFFFF0000);
                }
            }
        }
    }

    public List<ClientTooltipComponent> drawTooltip(int mouseX, int mouseY) {
        if(mouseX >= COORDS_BASE_X - 1
                && mouseX < COORDS_BASE_X + COORDS_SIZE_X
                && mouseY >= COORDS_BASE_Y - COORDS_SIZE_Y - 1
                && mouseY < COORDS_BASE_Y){
            Minecraft mc = Minecraft.getInstance();
            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            double mouseXExact = mc.mouseHandler.xpos() * scaledWidth / (double) mc.getWindow().getWidth();
            double mouseXFraction = mouseXExact - Math.floor(mouseXExact);
            double accurateMouseX = mouseX + mouseXFraction;

            return List.of(ClientTooltipComponent.create(FormattedCharSequence.forward("Y: " + getWorldHeight(accurateMouseX), Style.EMPTY)));
        }else{
            return List.of();
        }
    }

    private void drawGraphNumbering(GuiGraphics guiGraphics, int x, int yUnused){
        int labelY = 2;

        int ySteps = displayHeight / 8;
        for(int y = minY; y <= maxY; y += ySteps){
            double xPos = fromWorldHeight(y);
            int labelX = (int) Math.round(xPos);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(labelX, labelY, 0);
            guiGraphics.pose().scale(0.65f, 0.65f, 0);
            guiGraphics.drawString(Minecraft.getInstance().font, Integer.toString(y), 0, 0, 8, false);
            guiGraphics.pose().popPose();
        }
    }

    private int getWorldHeight(double atX){
        atX -= (COORDS_BASE_X - 1);
        return (int) Math.round(minY + atX * scale);
    }

    private double fromWorldHeight(int worldHeight){
        return ((worldHeight - minY) / scale);
    }

    protected void recalcDrawMeta(){
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for(int[] point : distributionDrawParams){
            if(point[0] < minY){
                minY = point[0];
            }
            if(point[0] > maxY){
                maxY = point[0];
            }
        }
        minY -= 5;
        maxY += 5;

        this.minY = minY;
        this.maxY = maxY;
        this.displayHeight = maxY - minY;
        this.scale = this.displayHeight / (double) COORDS_SIZE_X;

        this.drawHeights = new int[this.displayHeight];
        for(int i = 0; i < this.displayHeight; i++){
            int y = minY + i;
            this.drawHeights[i] = calcDrawHeightAt(y);
        }
    }

    protected int calcDrawHeightAt(int y){
        if(y < minY + 5 || y > maxY - 5){
            return 0;
        }

        int nextLowestY = minY;
        int nextLowestHeight = 0;
        int nextHighestY = maxY;
        int nextHighestHeight = 0;
        for(int[] point : distributionDrawParams){
            if(point[0] <= y && point[0] > nextLowestY){
                nextLowestY = point[0];
                nextLowestHeight = point[1];
            }else if(point[0] >= y && point[0] < nextHighestY){
                nextHighestY = point[0];
                nextHighestHeight = point[1];
            }
        }

        if(nextLowestY <= y && y <= nextHighestY){
            int heightDiff = nextHighestHeight - nextLowestHeight;
            double heightAtY = nextLowestHeight + ((y - nextLowestY) / (double) (nextHighestY - nextLowestY)) * heightDiff;
            return (int) Math.round(heightAtY * drawHeightMultiplier);
        }
        return 0;
    }

    public static class Merged extends WorldGenTypeHelper {

        private final Set<WorldGenTypeHelper> underlyingHelpers;

        public Merged(Set<ResourceLocation> biomes, Set<ItemStack> blocks, Set<WorldGenTypeHelper> toMerge) {
            super(toMerge.stream().toList().get(0).getTag() + "_merged", biomes, blocks); //TODO: Tag?
            this.underlyingHelpers = toMerge;
        }

        @Override
        protected void recalcDrawMeta() {
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;

            for(WorldGenTypeHelper helper : underlyingHelpers){
                helper.recalcDrawMeta();
                if(helper.minY < minY){
                    minY = helper.minY;
                }
                if(helper.maxY > maxY){
                    maxY = helper.maxY;
                }
            }

            this.minY = minY;
            this.maxY = maxY;
            this.displayHeight = maxY - minY;
            this.scale = this.displayHeight / (double) COORDS_SIZE_X;

            this.drawHeights = new int[this.displayHeight];
            for(int i = 0; i < this.displayHeight; i++){
                int y = minY + i;
                this.drawHeights[i] = calcDrawHeightAt(y);
            }
        }

        @Override
        protected int calcDrawHeightAt(int y) {
            int drawHeight = 0;
            for(WorldGenTypeHelper helper : underlyingHelpers){
                drawHeight += helper.calcDrawHeightAt(y);
            }
            return drawHeight;
        }
    }
}
