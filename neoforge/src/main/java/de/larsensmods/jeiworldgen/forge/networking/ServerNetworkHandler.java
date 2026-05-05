package de.larsensmods.jeiworldgen.forge.networking;

import de.larsensmods.jeiworldgen.networking.INetworkHandler;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import de.larsensmods.jeiworldgen.networking.WorldGenInfo;

public class ServerNetworkHandler implements INetworkHandler {

    public WorldGenInfo worldGenInfo;
    public LootInfo lootInfo;

    @Override
    public void setWorldGenInfo(WorldGenInfo worldGenInfo) {
        this.worldGenInfo = worldGenInfo;
    }

    @Override
    public void setLootInfo(LootInfo lootInfo) {
        this.lootInfo = lootInfo;
    }
}
