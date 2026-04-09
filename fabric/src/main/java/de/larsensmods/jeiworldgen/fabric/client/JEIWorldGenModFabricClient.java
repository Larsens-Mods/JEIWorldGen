package de.larsensmods.jeiworldgen.fabric.client;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.networking.Channels;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.concurrent.CompletableFuture;

public final class JEIWorldGenModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        ClientLoginNetworking.registerGlobalReceiver(Channels.BIOME_DATA_SYNC, (client, handler, buf, listenerAdder) -> {
            JEIWorldGenMod.LOGGER.info("Received data sync packet");
            ClientDataStore.WG_INFO = WorldGenInfo.decode(buf);
            return CompletableFuture.completedFuture(PacketByteBufs.empty());
        });
    }
}
