package de.larsensmods.jeiworldgen.events;

import de.larsensmods.jeiworldgen.client.ClientDataStore;
import de.larsensmods.jeiworldgen.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ClientEvents {

    public static void playerJoinedWorld(Player player){
        if(ClientDataStore.WG_INFO == null && ConfigManager.getConfig().showNoDataMessage()){
            player.displayClientMessage(Component.translatable("jeiwg.no_data"), false);
        }
    }

}
