package de.larsensmods.jeiworldgen.forge;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.forge.networking.ServerNetworkHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.network.NetworkEvent;

@Mod(JEIWorldGenMod.MOD_ID)
public final class JEIWorldGenModForge {

    private static ServerNetworkHandler networkHandler;

    public JEIWorldGenModForge() {
        ModLoadingContext.get().registerDisplayTest(new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        networkHandler = new ServerNetworkHandler();
        JEIWorldGenMod.init(networkHandler);
    }

    @Mod.EventBusSubscriber
    public static class ServerStartHandler {

        static boolean loaded = false;

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            if(!loaded) {
                event.getServer().registryAccess().registry(Registries.BIOME).ifPresent(JEIWorldGenMod::buildBiomeData);
                loaded = true;
            }
        }

        @SubscribeEvent
        public static void onConnectionFromClientEvent(PlayerEvent.PlayerLoggedInEvent event){
            if(event.getEntity() instanceof ServerPlayer serverPlayer){
                JEIWorldGenMod.LOGGER.info("Player logged in, sending world gen data");
                networkHandler.sendWorldGenInfo(serverPlayer);
            }
        }
    }
}
