package de.larsensmods.jeiworldgen.networking;

import de.larsensmods.jeiworldgen.client.OreGenData;
import net.minecraft.network.FriendlyByteBuf;

public record WorldGenInfo(OreGenData data) {

    public static WorldGenInfo decode(FriendlyByteBuf byteBuf) {
        OreGenData data = OreGenData.readFrom(byteBuf);
        return new WorldGenInfo(data);
    }

    public void encode(FriendlyByteBuf byteBuf) {
        this.data.writeTo(byteBuf);
    }

}
