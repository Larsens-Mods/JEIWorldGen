package de.larsensmods.jeiworldgen.forge;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.events.ClientEvents;
import de.larsensmods.jeiworldgen.forge.networking.ServerNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkConstants;

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

        /*@SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event){
            if (event.getEntity() instanceof Player player && FMLLoader.getDist().isClient() && Minecraft.getInstance().player != null && player.getUUID().equals(Minecraft.getInstance().player.getUUID())) {
                ClientEvents.playerJoinedWorld(player);
            }
        }*/
    }
}
