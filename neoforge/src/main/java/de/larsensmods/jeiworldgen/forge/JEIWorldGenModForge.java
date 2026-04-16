package de.larsensmods.jeiworldgen.forge;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.forge.networking.ServerNetworkHandler;
import de.larsensmods.jeiworldgen.forge.networking.WorldGenInfoPayload;
import de.larsensmods.jeiworldgen.forge.networking.WorldGenInfoTask;
import de.larsensmods.jeiworldgen.networking.Channels;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(JEIWorldGenMod.MOD_ID)
public final class JEIWorldGenModForge {

    public static ServerNetworkHandler networkHandler;

    public JEIWorldGenModForge() {
        //ModLoadingContext.get().registerDisplayTest(new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        networkHandler = new ServerNetworkHandler();
        JEIWorldGenMod.init(networkHandler);
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
    public static class ServerStartHandlerGameBus {

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
                //networkHandler.sendWorldGenInfo(serverPlayer);
            }
        }
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    public static class ServerStartHandlerModBus {

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar(Channels.PROTOCOL_VERSION).optional();
            registrar.configurationToClient(
                    WorldGenInfoPayload.TYPE,
                    WorldGenInfoPayload.STREAM_CODEC,
                    (payload, context) -> {
                        JEIWorldGenMod.LOGGER.info("Received data sync packet");
                        ClientDataStore.WG_INFO = payload.info();
                    }
            );
        }

        @SubscribeEvent
        public static void onConfigurationTaskRegister(RegisterConfigurationTasksEvent event){
            if(event.getListener().hasChannel(WorldGenInfoPayload.TYPE)) {
                event.register(new WorldGenInfoTask(networkHandler, event.getListener()));
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
