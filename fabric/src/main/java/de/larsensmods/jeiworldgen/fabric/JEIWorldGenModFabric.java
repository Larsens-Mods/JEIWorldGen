package de.larsensmods.jeiworldgen.fabric;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.fabric.networking.ServerNetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NonNull;

public final class JEIWorldGenModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerNetworkHandler networkHandler = new ServerNetworkHandler();

        ServerLoginConnectionEvents.QUERY_START.register(networkHandler::sendWorldGenInfo);

        JEIWorldGenMod.init(networkHandler);

        ServerLevelEvents.LOAD.register(new ServerLevelEvents.Load() {
            boolean loaded = false;

            @Override
            public void onLevelLoad(@NonNull MinecraftServer server, @NonNull ServerLevel world) {
                if(!loaded) {
                    world.registryAccess().lookup(Registries.BIOME).ifPresent(JEIWorldGenMod::buildBiomeData);
                    JEIWorldGenMod.buildLootData(server.reloadableRegistries());
                    loaded = true;
                }
            }
        });
    }
}
