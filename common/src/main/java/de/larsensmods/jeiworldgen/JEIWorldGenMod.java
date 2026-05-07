package de.larsensmods.jeiworldgen;

import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import de.larsensmods.jeiworldgen.util.LootDataBuilder;
import de.larsensmods.jeiworldgen.util.MixinFixWrapper;
import de.larsensmods.jeiworldgen.util.OreGenDataBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class JEIWorldGenMod {
    public static final String MOD_ID = "jeiworldgen";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MixinFixWrapper FIX_WRAPPER = null;

    private static INetworkHandler networkHandler;

    private static WorldGenInfo wgInfo = null;

    public static void init(INetworkHandler netHandler, MixinFixWrapper fixWrapper) {
        FIX_WRAPPER = fixWrapper;

        networkHandler = netHandler;
    }

    public static void buildBiomeData(Registry<Biome> biomeRegistry){
        Map<ResourceKey<Biome>, HolderSet<PlacedFeature>> biomeOreFeatures = new HashMap<>();
        Map<ResourceKey<Biome>, HolderSet<PlacedFeature>> biomeDecoFeatures = new HashMap<>();

        biomeRegistry.entrySet().forEach(biomeEntry -> {
            if(biomeEntry.getValue().getGenerationSettings().features().size() > GenerationStep.Decoration.UNDERGROUND_ORES.ordinal()){
                biomeOreFeatures.put(biomeEntry.getKey(), biomeEntry.getValue().getGenerationSettings().features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal()));
            }
            if(biomeEntry.getValue().getGenerationSettings().features().size() > GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal()){
                biomeDecoFeatures.put(biomeEntry.getKey(), biomeEntry.getValue().getGenerationSettings().features().get(GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal()));
            }
        });

        wgInfo = new WorldGenInfo(OreGenDataBuilder.fromRaw(biomeOreFeatures, biomeDecoFeatures));

        LOGGER.info("Built biome data");
    }

    public static void buildLootData(LootDataManager lootDataManager){
        LootData data = LootDataBuilder.fromRaw(lootDataManager, wgInfo);

        networkHandler.setWorldGenInfo(wgInfo);
        networkHandler.setLootInfo(new LootInfo(data));
        LOGGER.info("Built loot table data and submitted");
    }

    public static MixinFixWrapper getMixinWrapper(){
        if(FIX_WRAPPER == null){
            throw new IllegalStateException("Mixin Wrapper has not been initialized yet");
        }
        return FIX_WRAPPER;
    }
}
