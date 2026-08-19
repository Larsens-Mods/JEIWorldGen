package de.larsensmods.jeiworldgen.cache;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DataCache {

    public static File worldGenInfoFile = new File("./config/jeiworldgen-generation-cache.dat");
    public static File lootInfoFile = new File("./config/jeiworldgen-loot-cache.dat");

    public static void writeWorldGenInfo(WorldGenInfo data){
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        data.encode(byteBuf);
        try {
            Files.write(worldGenInfoFile.toPath(), byteBuf.array());
        } catch (IOException e) {
            JEIWorldGenMod.LOGGER.error("Unable to write cache file.", e);
        }
    }

    public static void writeLootInfo(LootInfo data){
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        data.encode(byteBuf);
        try {
            Files.write(lootInfoFile.toPath(), byteBuf.array());
        } catch (IOException e) {
            JEIWorldGenMod.LOGGER.error("Unable to write cache file.", e);
        }
    }

    public static void loadIfAvailable(){
        if(worldGenInfoFile.exists()){
            try {
                byte[] bytes = Files.readAllBytes(worldGenInfoFile.toPath());
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.copiedBuffer(bytes));
                ClientDataStore.WG_INFO = WorldGenInfo.decode(buf);
                JEIWorldGenMod.LOGGER.info("Read trades data from cache file.");
            }catch (IOException e){
                JEIWorldGenMod.LOGGER.error("Unable to read from existing cache file.", e);
            }
        }
        if(lootInfoFile.exists()){
            try {
                byte[] bytes = Files.readAllBytes(lootInfoFile.toPath());
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.copiedBuffer(bytes));
                ClientDataStore.LOOT_INFO = LootInfo.decode(buf);
                JEIWorldGenMod.LOGGER.info("Read bartering data from cache file.");
            }catch (IOException e){
                JEIWorldGenMod.LOGGER.error("Unable to read from existing cache file.", e);
            }
        }
    }

}
