package de.larsensmods.jeiworldgen.forge.networking;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.networking.Channels;
import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ServerNetworkHandler implements INetworkHandler {

    public static final SimpleChannel BIOME_CHANNEL = NetworkRegistry.newSimpleChannel(
            Channels.BIOME_DATA_SYNC,
            () -> Channels.PROTOCOL_VERSION,
            ver -> true,
            ver -> true);

    public static final SimpleChannel LOOT_CHANNEL = NetworkRegistry.newSimpleChannel(
            Channels.LOOT_DATA_SYNC,
            () -> Channels.PROTOCOL_VERSION,
            ver -> true,
            ver -> true);

    private WorldGenInfo worldGenInfo;
    private LootInfo lootInfo;

    public ServerNetworkHandler(){
        BIOME_CHANNEL.registerMessage(0, WorldGenInfo.class, WorldGenInfo::encode, WorldGenInfo::decode, this::handleWorldGenMessage);
        LOOT_CHANNEL.registerMessage(0, LootInfo.class, LootInfo::encode, LootInfo::decode, this::handleLootMessage);
    }

    private void handleWorldGenMessage(WorldGenInfo message, Supplier<NetworkEvent.Context> ctx){
        JEIWorldGenMod.LOGGER.info("Received data sync packet");
        ClientDataStore.WG_INFO = message;
        ctx.get().setPacketHandled(true);
    }

    private void handleLootMessage(LootInfo message, Supplier<NetworkEvent.Context> ctx){
        JEIWorldGenMod.LOGGER.info("Received loot sync packet");
        ClientDataStore.LOOT_INFO = message;
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void setWorldGenInfo(WorldGenInfo worldGenInfo) {
        this.worldGenInfo = worldGenInfo;
    }

    @Override
    public void setLootInfo(LootInfo lootInfo) {
        this.lootInfo = lootInfo;
    }

    public void sendWorldGenInfo(ServerPlayer player) {
        if(worldGenInfo != null) {
            BIOME_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), worldGenInfo);
            JEIWorldGenMod.LOGGER.info("Sent data sync packet");
        }else{
            JEIWorldGenMod.LOGGER.warn("No WorldGenInfo present on client connect");
        }
        if(lootInfo != null) {
            LOOT_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), lootInfo);
            JEIWorldGenMod.LOGGER.info("Sent loot sync packet");
        }else{
            JEIWorldGenMod.LOGGER.warn("No LootInfo present on client connect");
        }
    }
}
