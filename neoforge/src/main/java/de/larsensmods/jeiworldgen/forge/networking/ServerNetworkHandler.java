package de.larsensmods.jeiworldgen.forge.networking;

import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;

public class ServerNetworkHandler implements INetworkHandler {

    public WorldGenInfo worldGenInfo;

    @Override
    public void setWorldGenInfo(WorldGenInfo worldGenInfo) {
        this.worldGenInfo = worldGenInfo;
    }
}
