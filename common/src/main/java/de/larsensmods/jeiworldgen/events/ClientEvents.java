package de.larsensmods.jeiworldgen.events;

import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ClientEvents {

    private static boolean sentMessageInSession = false;

    public static void playerJoinedWorld(Player player){
        if(!sentMessageInSession && ClientDataStore.WG_INFO == null && ConfigManager.getConfig().showNoDataMessage()){
            player.displayClientMessage(Component.translatable("jeiwg.no_data"), false);
            sentMessageInSession = true;
        }
    }

}
