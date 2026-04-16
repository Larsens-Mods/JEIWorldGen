package de.larsensmods.jeiworldgen.fabric.networking;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.networking.Channels;
import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

public class ServerNetworkHandler implements INetworkHandler {

    private WorldGenInfo worldGenInfo;

    public ServerNetworkHandler(){
        ServerLoginNetworking.registerGlobalReceiver(Channels.BIOME_DATA_SYNC, ((server, handler, understood, buf, synchronizer, responseSender) -> {
            if(understood){
                JEIWorldGenMod.LOGGER.info("Received data sync response");
            }else{
                JEIWorldGenMod.LOGGER.info("Client didn't understand data sync");
            }
        }));
    }

    @Override
    public void setWorldGenInfo(WorldGenInfo worldGenInfo) {
        this.worldGenInfo = worldGenInfo;
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
    }
}
