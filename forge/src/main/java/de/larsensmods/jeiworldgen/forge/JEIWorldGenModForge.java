package de.larsensmods.jeiworldgen.forge;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import de.larsensmods.jeiworldgen.compat.ICompatModule;
import de.larsensmods.jeiworldgen.forge.compat.MekanismCompatModule;
import de.larsensmods.jeiworldgen.forge.networking.ServerNetworkHandler;
import de.larsensmods.jeiworldgen.forge.util.ForgeMixinFixWrapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;

import java.util.HashSet;
import java.util.Set;

@Mod(JEIWorldGenMod.MOD_ID)
public final class JEIWorldGenModForge {

    private static ServerNetworkHandler networkHandler;

    public JEIWorldGenModForge() {
        ModLoadingContext.get().registerDisplayTest(new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        Set<ICompatModule> compatModules = new HashSet<>();

        if(ModList.get().isLoaded("mekanism")) {
            JEIWorldGenMod.LOGGER.info("Detected Mekanism, loading compat module");
            compatModules.add(new MekanismCompatModule());
        }

        networkHandler = new ServerNetworkHandler();
        JEIWorldGenMod.init(networkHandler, new ForgeMixinFixWrapper(), compatModules);
    }

    @Mod.EventBusSubscriber
    public static class ServerStartHandler {

        static boolean loaded = false;

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            if(!loaded) {
                event.getServer().registryAccess().registry(Registries.BIOME).ifPresent(registry -> JEIWorldGenMod.buildBiomeData(registry, event.getServer()));
                JEIWorldGenMod.buildLootData(event.getServer().getLootData());
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
