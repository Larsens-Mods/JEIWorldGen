package de.larsensmods.jeiworldgen.fabric.client;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.events.ClientEvents;
import de.larsensmods.jeiworldgen.networking.Channels;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

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

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Player player && Minecraft.getInstance().player != null && player.getUUID().equals(Minecraft.getInstance().player.getUUID())) {
                ClientEvents.playerJoinedWorld(player);
            }
        });
    }
}
