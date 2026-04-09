package de.larsensmods.jeiworldgen.networking;

import de.larsensmods.jeiworldgen.client.OreGenData;
import net.minecraft.network.FriendlyByteBuf;

public class WorldGenInfo {

    private final OreGenData data;

    public WorldGenInfo(OreGenData data){
        this.data = data;
    }

    public OreGenData getData(){
        return this.data;
    }

    public static WorldGenInfo decode(FriendlyByteBuf byteBuf){
        OreGenData data = OreGenData.readFrom(byteBuf);
        return new WorldGenInfo(data);
    }

    public void encode(FriendlyByteBuf byteBuf){
        this.data.writeTo(byteBuf);
    }

}
