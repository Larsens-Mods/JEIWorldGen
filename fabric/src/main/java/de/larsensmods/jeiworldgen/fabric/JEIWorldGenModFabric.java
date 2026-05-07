package de.larsensmods.jeiworldgen.fabric;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.fabric.networking.ServerNetworkHandler;
import de.larsensmods.jeiworldgen.fabric.util.FabricMixinFixWrapper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class JEIWorldGenModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerNetworkHandler networkHandler = new ServerNetworkHandler();

        ServerLoginConnectionEvents.QUERY_START.register(networkHandler::sendWorldGenInfo);

        JEIWorldGenMod.init(networkHandler, new FabricMixinFixWrapper());

        ServerWorldEvents.LOAD.register(new ServerWorldEvents.Load() {
            boolean loaded = false;

            @Override
            public void onWorldLoad(MinecraftServer server, ServerLevel world) {
                if(!loaded) {
                    world.registryAccess().registry(Registries.BIOME).ifPresent(JEIWorldGenMod::buildBiomeData);
                    JEIWorldGenMod.buildLootData(server.getLootData());
                    loaded = true;
                }
            }
        });
    }
}
