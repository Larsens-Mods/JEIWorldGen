package de.larsensmods.jeiworldgen.forge.networking;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.networking.Channels;
import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ServerNetworkHandler implements INetworkHandler {

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Channels.BIOME_DATA_SYNC,
            () -> Channels.PROTOCOL_VERSION,
            ver -> true,
            ver -> true);

    private WorldGenInfo worldGenInfo;

    public ServerNetworkHandler(){
        CHANNEL.registerMessage(0, WorldGenInfo.class, WorldGenInfo::encode, WorldGenInfo::decode, this::handleMessage);
    }

    private void handleMessage(WorldGenInfo message, Supplier<NetworkEvent.Context> ctx){
        JEIWorldGenMod.LOGGER.info("Received data sync packet");
        ClientDataStore.WG_INFO = message;
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void setWorldGenInfo(WorldGenInfo worldGenInfo) {
        this.worldGenInfo = worldGenInfo;
    }

    public void sendWorldGenInfo(ServerPlayer player) {
        if(worldGenInfo != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), worldGenInfo);
            JEIWorldGenMod.LOGGER.info("Sent data sync packet");
        }else{
            JEIWorldGenMod.LOGGER.warn("No WorldGenInfo present on client connect");
        }
    }
}
