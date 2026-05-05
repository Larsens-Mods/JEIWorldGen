package de.larsensmods.jeiworldgen.fabric.networking;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.networking.Channels;
import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

public class ServerNetworkHandler implements INetworkHandler {

    private WorldGenInfo worldGenInfo;
    private LootInfo lootInfo;

    public ServerNetworkHandler(){
        ServerLoginNetworking.registerGlobalReceiver(Channels.BIOME_DATA_SYNC, ((server, handler, understood, buf, synchronizer, responseSender) -> {
            if(understood){
                JEIWorldGenMod.LOGGER.info("Received data sync response");
            }else{
                JEIWorldGenMod.LOGGER.info("Client didn't understand data sync");
            }
        }));
        ServerLoginNetworking.registerGlobalReceiver(Channels.LOOT_DATA_SYNC, ((server, handler, understood, buf, synchronizer, responseSender) -> {
            if(understood){
                JEIWorldGenMod.LOGGER.info("Received loot sync response");
            }else{
                JEIWorldGenMod.LOGGER.info("Client didn't understand loot sync");
            }
        }));
    }

    @Override
    public void setWorldGenInfo(WorldGenInfo worldGenInfo) {
        this.worldGenInfo = worldGenInfo;
    }

    @Override
    public void setLootInfo(LootInfo lootInfo) {
        this.lootInfo = lootInfo;
    }

    public void sendWorldGenInfo(ServerLoginPacketListenerImpl handler, MinecraftServer server, LoginPacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
        if(worldGenInfo != null) {
            FriendlyByteBuf byteBuf = PacketByteBufs.create();
            worldGenInfo.encode(byteBuf);
            sender.sendPacket(Channels.BIOME_DATA_SYNC, byteBuf);
            JEIWorldGenMod.LOGGER.info("Sent data sync packet");
        }else{
            JEIWorldGenMod.LOGGER.warn("No WorldGenInfo present on client connect");
        }
        if(lootInfo != null){
            FriendlyByteBuf byteBuf = PacketByteBufs.create();
            lootInfo.encode(byteBuf);
            sender.sendPacket(Channels.LOOT_DATA_SYNC, byteBuf);
            JEIWorldGenMod.LOGGER.info("Sent loot sync packet");
        }else{
            JEIWorldGenMod.LOGGER.warn("No LootInfo present on client connect");
        }
    }
}
