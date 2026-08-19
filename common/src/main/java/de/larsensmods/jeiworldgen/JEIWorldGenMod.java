package de.larsensmods.jeiworldgen;

import de.larsensmods.jeiworldgen.cache.DataCache;
import de.larsensmods.jeiworldgen.client.LootData;
import de.larsensmods.jeiworldgen.compat.ICompatModule;
import de.larsensmods.jeiworldgen.config.ConfigManager;
import de.larsensmods.jeiworldgen.mixin.*;
import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import de.larsensmods.jeiworldgen.util.LootDataBuilder;
import de.larsensmods.jeiworldgen.util.OreGenDataBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class JEIWorldGenMod {
    public static final String MOD_ID = "jeiworldgen";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static INetworkHandler networkHandler;
    private static Set<ICompatModule> compatModules;

    private static WorldGenInfo wgInfo = null;

    public static void init(INetworkHandler netHandler, Set<ICompatModule> compatibilityModules) {
        networkHandler = netHandler;
        compatModules = compatibilityModules;

        if(ConfigManager.getConfig().cacheLastDataset()){
            DataCache.loadIfAvailable();
        }
    }

    public static void buildBiomeData(Registry<Biome> biomeRegistry, MinecraftServer minecraftServer) {
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

        wgInfo = new WorldGenInfo(OreGenDataBuilder.fromRaw(biomeOreFeatures, biomeDecoFeatures, compatModules, minecraftServer));

        LOGGER.info("Built biome data");
    }

    public static void buildLootData(ReloadableServerRegistries.Holder lootRegistryHolder){
        LootData data = LootDataBuilder.fromRaw(lootRegistryHolder, wgInfo);

        networkHandler.setWorldGenInfo(wgInfo);
        networkHandler.setLootInfo(new LootInfo(data));
        LOGGER.info("Built loot table data and submitted");
    }
}
