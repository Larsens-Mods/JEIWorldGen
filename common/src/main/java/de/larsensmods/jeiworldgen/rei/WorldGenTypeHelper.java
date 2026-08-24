package de.larsensmods.jeiworldgen.rei;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.client.utils.RenderUtils;
import de.larsensmods.jeiworldgen.config.ConfigManager;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.util.CompareUtils;
import de.larsensmods.jeiworldgen.util.ValueHelpers;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.heightproviders.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static de.larsensmods.jeiworldgen.rei.WorldGenCategory.*;

public class WorldGenTypeHelper implements Display {

    public static final Set<Identifier> NON_OVERWORLD_BIOMES = Set.of(
            Biomes.NETHER_WASTES.identifier(),
            Biomes.SOUL_SAND_VALLEY.identifier(),
            Biomes.CRIMSON_FOREST.identifier(),
            Biomes.WARPED_FOREST.identifier(),
            Biomes.BASALT_DELTAS.identifier(),
            Biomes.THE_END.identifier(),
            Biomes.SMALL_END_ISLANDS.identifier(),
            Biomes.END_MIDLANDS.identifier(),
            Biomes.END_HIGHLANDS.identifier(),
            Biomes.END_BARRENS.identifier()
    );

    public static List<WorldGenTypeHelper> buildRecipes(){
        JEIWorldGenMod.LOGGER.info("Building world gen JEI recipes");

        OreGenData data = ClientDataStore.WG_INFO != null ? ClientDataStore.WG_INFO.data() : null;
        if(data == null){
            JEIWorldGenMod.LOGGER.warn("Could not find generation data");
            return List.of();
        }

        List<OreGenData.OreData> foundOreData = new ArrayList<>();
        List<Set<Identifier>> dataBiomes = new ArrayList<>();

        for(Map.Entry<Identifier, OreGenData.BiomeData> biomeEntry : data.biomeData.entrySet()){
            Identifier biome = biomeEntry.getKey();
            OreGenData.BiomeData biomeData = biomeEntry.getValue();

            for(OreGenData.OreData oreData : biomeData.ores){
                if(foundOreData.contains(oreData)){
                    dataBiomes.get(foundOreData.indexOf(oreData)).add(biome);
                }else{
                    foundOreData.add(oreData);
                    Set<Identifier> biomeSet = new HashSet<>();
                    biomeSet.add(biome);
                    dataBiomes.add(biomeSet);
                }
            }
        }

        List<WorldGenTypeHelper> output = new ArrayList<>();

        for(int i = 0; i < foundOreData.size(); i++){
            OreGenData.OreData oreData = foundOreData.get(i);
            Set<Identifier> biomes = dataBiomes.get(i);
            JEIWorldGenMod.LOGGER.debug("Found ore gen data set for {} biomes: {}", biomes.size(), String.join(", ", biomes.stream().map(Identifier::toString).toList()));

            Set<ItemStackTemplate> shownTargets = new HashSet<>();
            for(ItemStackTemplate stack : oreData.getTargets()){
                if(!Set.of(ConfigManager.getConfig().hiddenBlocks()).contains(stack.item().getRegisteredName())){
                    shownTargets.add(stack);
                }else{
                    JEIWorldGenMod.LOGGER.info("Excluding {} from display, because its hidden in the config", stack.item().getRegisteredName());
                }
            }

            if(shownTargets.isEmpty()){
                JEIWorldGenMod.LOGGER.debug("Skipping empty entry");
                continue;
            }

            Identifier sampleBiome = biomes.stream().toList().getFirst();

            WorldGenTypeHelper entry = new WorldGenTypeHelper(biomes, shownTargets);

            float multiplier = 1;
            if(oreData.getCountPlacement() != null){
                IntProvider intProvider = ((CountPlacementAccessor) oreData.getCountPlacement()).jeiwg$count();
                multiplier = (intProvider.minInclusive() + intProvider.maxInclusive()) / 2f;
            }else if(oreData.getRarityFilter() != null){
                int chance = ((RarityFilterAccessor) oreData.getRarityFilter()).jeiwg$chance();
                multiplier = 1f / chance;
            }
            entry.spawnAttempts = Math.round(multiplier);
            entry.maxBlobSize = oreData.getSize();

            HeightProvider heightProvider = ((HeightRangePlacementAccessor) oreData.getHeightRangePlacement()).jeiwg$height();
            if(heightProvider.getType().equals(HeightProviderType.CONSTANT)){
                ConstantHeight aHeight = (ConstantHeight) heightProvider;
                try {
                    int height = Integer.parseInt(aHeight.toString().split(" ")[0]);
                    int amount = (int) Math.ceil(oreData.getSize() * multiplier);
                    entry.addDistributionCornerPoint(height, amount);
                    entry.minSpawnY = height;
                    entry.maxSpawnY = height;
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
                entry.minSpawnY = minHeight;
                entry.maxSpawnY = maxHeight;
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
                entry.minSpawnY = minHeight;
                entry.maxSpawnY = maxHeight;
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
                merged.add(new Merged(entryA.biomes, entryA.blocks, toMerge));
            }else{
                merged.add(entryA);
            }
        }

        return merged;
    }

    //Implementation

    public final Set<Identifier> biomes;
    public final Set<ItemStackTemplate> blocks;

    protected final Set<int[]> distributionDrawParams = new HashSet<>();

    public int maxBlobSize = 0, spawnAttempts = 0, minSpawnY = 0, maxSpawnY = 0;

    public WorldGenTypeHelper(Set<Identifier> biomes, Set<ItemStackTemplate> blocks){
        this.biomes = biomes;
        this.blocks = blocks;
    }

    private void addDistributionCornerPoint(int height, int amount){
        this.distributionDrawParams.add(new int[]{height, amount});
    }

    public String getBiomeString(){
        return biomes.stream().toList().getFirst().toString() + (biomes.size() > 1 ? " (+" + (biomes.size() - 1) + ")" : "");
    }

    public List<Identifier> getBiomeStrings(){
        return biomes.stream().toList();
    }

    public List<MutableComponent> getBiomeInfoComponent(){
        List<String> info = biomes.stream().map(Identifier::toString).toList();
        List<MutableComponent> components;
        if(info.size() > 5){
            components = new ArrayList<>(info.subList(0, 5).stream().map(Component::literal).toList());
            components.add(Component.translatable("jeiwg.biome_tooltip.view_more_biomes", biomes.size() - 5).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }else{
            components = info.stream().map(Component::literal).toList();
        }
        return components;
    }

    public boolean metaEquals(WorldGenTypeHelper other){
        return CompareUtils.areItemStackTemplateSetsEqual(this.blocks, other.blocks) && CompareUtils.areIdentifierSetsEqual(this.biomes, other.biomes);
    }

    public int getSpawnAttemptsAt(int y){
        return y >= minSpawnY && y <= maxSpawnY ? spawnAttempts : 0;
    }

    public int getMaxBlobSizeAt(int y){
        return y >= minSpawnY && y <= maxSpawnY ? maxBlobSize : 0;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return blocks.stream().map(EntryIngredients::of).toList();
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        List<ItemStack> genBlockStacks = blocks.stream().map(ItemStackTemplate::create).toList();
        if(ClientDataStore.LOOT_INFO != null) {
            List<ItemStack> dropStacks = new ArrayList<>();
            LootData lootData = ClientDataStore.LOOT_INFO.data();
            for (ItemStack block : genBlockStacks) {
                Set<LootData.BlockLootData> blockLootData = lootData.dataForEntry(Holder.direct(block.getItem()));
                WorldGenCategory.addMissingLoot(dropStacks, new ArrayList<>(), block, blockLootData, genBlockStacks);
            }
            List<List<ItemStack>> outputStacks = new ArrayList<>();

            List<ItemStack> selfStacks = new ArrayList<>();

            for (ItemStack dropStack : dropStacks) {
                boolean contained = false;
                for (ItemStack genStack : genBlockStacks) {
                    if (ItemStack.isSameItemSameComponents(genStack, dropStack)) {
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

            List<EntryIngredient> outputEntries = new ArrayList<>();

            for(int i = 0; i < Math.min(outputStacks.size(), 3); i++){
                outputEntries.addAll(outputStacks.get(i).stream().map(EntryIngredients::of).toList());
            }
            return outputEntries;
        }else{
            return List.of();
        }
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return WorldGenCategory.WORLD_GEN;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.empty();
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }

    //GUI STUFF

    protected static final double drawHeightMultiplier = 3.5;

    protected int minY = -200, maxY = 500, displayHeight = 700;
    protected double scale = 1;

    protected int[] drawHeights = new int[0];

    public void drawInfo(GuiGraphics graphics, Point basePoint) {
        RenderUtils.drawLine(graphics, basePoint.x + COORDS_BASE_X, basePoint.y + COORDS_BASE_Y, basePoint.x + COORDS_BASE_X + COORDS_SIZE_X, basePoint.y + COORDS_BASE_Y, 0xFF444444);
        RenderUtils.drawLine(graphics, basePoint.x + COORDS_BASE_X, basePoint.y + COORDS_BASE_Y, basePoint.x + COORDS_BASE_X, basePoint.y + COORDS_BASE_Y - COORDS_SIZE_Y, 0xFF444444);

        recalcDrawMeta();

        drawGraphNumbering(graphics, basePoint);

        for(int y = this.minY; y < this.maxY; y++){
            int drawHeight = this.drawHeights[y - this.minY];
            if(drawHeight > 0){
                if(drawHeight > COORDS_SIZE_Y){
                    RenderUtils.drawLine(graphics, basePoint.x + (int) Math.round(fromWorldHeight(y)), basePoint.y + COORDS_BASE_Y - COORDS_SIZE_Y, basePoint.x + (int) Math.round(fromWorldHeight(y)), basePoint.y + COORDS_BASE_Y, 0xFFFF9900);
                }else {
                    RenderUtils.drawLine(graphics, basePoint.x + (int) Math.round(fromWorldHeight(y)), basePoint.y + COORDS_BASE_Y - drawHeight, basePoint.x + (int) Math.round(fromWorldHeight(y)), basePoint.y + COORDS_BASE_Y, 0xFFFF0000);
                }
            }
        }
    }

    public void drawTooltip(int mouseX, int mouseY, Point basePoint) {
        if(mouseX >= basePoint.x + COORDS_BASE_X - 1
                && mouseX < basePoint.x + COORDS_BASE_X + COORDS_SIZE_X
                && mouseY >= basePoint.y + COORDS_BASE_Y - COORDS_SIZE_Y - 1
                && mouseY < basePoint.y + COORDS_BASE_Y){
            Minecraft mc = Minecraft.getInstance();
            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            double mouseXExact = mc.mouseHandler.xpos() * scaledWidth / (double) mc.getWindow().getWidth();
            double mouseXFraction = mouseXExact - Math.floor(mouseXExact);
            mouseX += mouseXFraction;

            int y = getWorldHeight(mouseX - basePoint.x);
            Tooltip tooltip = Tooltip.create(Component.literal("Y: " + y));
            if(getSpawnAttemptsAt(y) > 0) tooltip.add(Component.translatable("jeiwg.spawnattempts", getSpawnAttemptsAt(y)));
            if(getMaxBlobSizeAt(y) > 0) tooltip.add(Component.translatable("jeiwg.maxblobsize", getMaxBlobSizeAt(y)));
            tooltip.queue();
        }
    }

    private void drawGraphNumbering(GuiGraphicsExtractor guiGraphics, Point basePoint){
        int labelY = COORDS_BASE_Y + 2;

        int ySteps = displayHeight / 8;
        for(int y = minY; y <= maxY; y += ySteps){
            double xPos = fromWorldHeight(y);
            int labelX = (int) Math.round(xPos);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(labelX + basePoint.x, labelY + basePoint.y);
            guiGraphics.pose().scale(0.65f, 0.65f);
            guiGraphics.text(Minecraft.getInstance().font, Integer.toString(y), 0, 0, ARGB.opaque(8), false);
            guiGraphics.pose().popMatrix();
        }
    }

    private int getWorldHeight(double atX){
        atX -= (COORDS_BASE_X - 1);
        return (int) Math.round(minY + atX * scale);
    }

    private double fromWorldHeight(int worldHeight){
        return ((worldHeight - minY) / scale) + COORDS_BASE_X;
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

        public Merged(Set<Identifier> biomes, Set<ItemStackTemplate> blocks, Set<WorldGenTypeHelper> toMerge) {
            super(biomes, blocks);
            this.underlyingHelpers = toMerge;
        }

        @Override
        public int getSpawnAttemptsAt(int y) {
            int sum = 0;
            for(WorldGenTypeHelper helper : underlyingHelpers){
                sum += helper.getSpawnAttemptsAt(y);
            }
            return sum;
        }

        @Override
        public int getMaxBlobSizeAt(int y) {
            int maxSize = 0;
            for(WorldGenTypeHelper helper : underlyingHelpers){
                maxSize = Math.max(maxSize, helper.getMaxBlobSizeAt(y));
            }
            return maxSize;
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
