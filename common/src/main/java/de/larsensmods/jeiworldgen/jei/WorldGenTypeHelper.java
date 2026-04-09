package de.larsensmods.jeiworldgen.jei;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.client.OreGenData;
import de.larsensmods.jeiworldgen.client.utils.RenderUtils;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.util.ValueHelpers;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.heightproviders.*;

import java.util.*;

import static de.larsensmods.jeiworldgen.jei.JEIWorldGenCategory.COORDS_BASE_X;
import static de.larsensmods.jeiworldgen.jei.JEIWorldGenCategory.COORDS_BASE_Y;
import static de.larsensmods.jeiworldgen.jei.JEIWorldGenCategory.COORDS_SIZE_X;
import static de.larsensmods.jeiworldgen.jei.JEIWorldGenCategory.COORDS_SIZE_Y;

public class WorldGenTypeHelper implements IRecipeCategoryExtension {

    private static final Set<ResourceLocation> NON_OVERWORLD_BIOMES = Set.of(
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

    public static final RecipeType<WorldGenTypeHelper> RECIPE_TYPE = new RecipeType<>(new ResourceLocation(JEIWorldGenMod.MOD_ID, "world_generation"), WorldGenTypeHelper.class);

    public static List<WorldGenTypeHelper> buildRecipes(){
        JEIWorldGenMod.LOGGER.info("Building world gen JEI recipes");

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

            ResourceLocation sampleBiome = biomes.stream().toList().get(0);

            WorldGenTypeHelper entry = new WorldGenTypeHelper(biomes, oreData.getTargets());

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
                JEIWorldGenMod.LOGGER.error("Encountered unknown HeightProviderType, assuming inequality");
                continue;
            }
            
            output.add(entry);
        }

        return output;
    }

    //Implementation

    public final Set<ResourceLocation> biomes;
    public final Set<ItemStack> blocks;

    private final Set<int[]> distributionDrawParams = new HashSet<>();

    public WorldGenTypeHelper(Set<ResourceLocation> biomes, Set<ItemStack> blocks){
        this.biomes = biomes;
        this.blocks = blocks;
    }

    public void addDistributionCornerPoint(int height, int amount){
        this.distributionDrawParams.add(new int[]{height, amount});
    }

    public List<int[]> getDistributionDrawParams(){
        return this.distributionDrawParams.stream().sorted(Comparator.comparingInt(o -> o[0])).toList();
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

    //GUI STUFF

    private static final double drawHeightMultiplier = 3.5;

    private int minY = -200, maxY = 500, displayHeight = 700;
    private double scale = 1;

    private int[] drawHeights = new int[0];

    @Override
    public void drawInfo(int recipeWidth, int recipeHeight, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        recalcDrawMeta();

        int labelX = 40, labelY = 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(labelX, labelY, 0);
        guiGraphics.pose().scale(0.75f, 0.75f, 1);
        guiGraphics.drawString(Minecraft.getInstance().font, getBiomeString(), 0, 0, 8, false);
        guiGraphics.pose().popPose();

        drawGraphNumbering(guiGraphics);

        /*for(int[] point : getDistributionDrawParams()){
            RenderUtils.drawLine(guiGraphics, (int) Math.round(fromWorldHeight(point[0])) - 1, COORDS_BASE_Y - point[1] - 1, (int) Math.round(fromWorldHeight(point[0])) + 1, COORDS_BASE_Y - point[1] + 1, 0xFF0000FF);
        }*/

        for(int y = this.minY; y < this.maxY; y++){
            int drawHeight = this.drawHeights[y - this.minY];
            if(drawHeight > 0){
                RenderUtils.drawLine(guiGraphics, (int) Math.round(fromWorldHeight(y)), COORDS_BASE_Y - drawHeight, (int) Math.round(fromWorldHeight(y)), COORDS_BASE_Y, 0xFFFF0000);
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if(mouseX >= COORDS_BASE_X - 1
                && mouseX < COORDS_BASE_X + COORDS_SIZE_X
                && mouseY >= COORDS_BASE_Y - COORDS_SIZE_Y - 1
                && mouseY < COORDS_BASE_Y){
            Minecraft mc = Minecraft.getInstance();
            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            double mouseXExact = mc.mouseHandler.xpos() * scaledWidth / (double) mc.getWindow().getWidth();
            double mouseXFraction = mouseXExact - Math.floor(mouseXExact);
            mouseX += mouseXFraction;

            tooltip.add(Component.literal("Y: " + getWorldHeight(mouseX)));
        }

        if(mouseX >= 39
                && mouseX < 180
                && mouseY >= 1
                && mouseY < 9){
            tooltip.add(Component.translatable("jeiwg.biomes"));
            tooltip.addAll(getBiomeInfo().stream().map(Component::literal).toList());
        }
    }

    private void drawGraphNumbering(GuiGraphics guiGraphics){
        int labelY = COORDS_BASE_Y + 2;

        int ySteps = displayHeight / 8;
        for(int y = minY; y <= maxY; y += ySteps){
            double xPos = fromWorldHeight(y);
            int labelX = (int) Math.round(xPos);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(labelX, labelY, 0);
            guiGraphics.pose().scale(0.65f, 0.65f, 1);
            guiGraphics.drawString(Minecraft.getInstance().font, Integer.toString(y), 0, 0, 8, false);
            guiGraphics.pose().popPose();
        }
    }

    private int getWorldHeight(double atX){
        atX -= (COORDS_BASE_X - 1);
        return (int) Math.round(minY + atX * scale);
    }

    private double fromWorldHeight(int worldHeight){
        return ((worldHeight - minY) / scale) + COORDS_BASE_X;
    }

    private void recalcDrawMeta(){
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

    private int calcDrawHeightAt(int y){
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
}
