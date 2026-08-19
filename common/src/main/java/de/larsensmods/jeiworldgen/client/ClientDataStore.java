package de.larsensmods.jeiworldgen.client;

import de.larsensmods.jeiworldgen.cache.DataCache;
import de.larsensmods.jeiworldgen.config.ConfigManager;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;

public class ClientDataStore {

    public static WorldGenInfo WG_INFO = null;
    public static LootInfo LOOT_INFO = null;

    public static void storeWorldGenInfo(WorldGenInfo data){
        WG_INFO = data;
        if(ConfigManager.getConfig().cacheLastDataset()){
            DataCache.writeWorldGenInfo(data);
        }
    }

    public static void storeLootInfo(LootInfo data){
        LOOT_INFO = data;
        if(ConfigManager.getConfig().cacheLastDataset()){
            DataCache.writeLootInfo(data);
        }
    }

}
