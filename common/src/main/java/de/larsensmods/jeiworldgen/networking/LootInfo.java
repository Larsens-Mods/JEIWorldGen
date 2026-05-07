package de.larsensmods.jeiworldgen.networking;

import de.larsensmods.jeiworldgen.client.LootData;
import net.minecraft.network.FriendlyByteBuf;

public record LootInfo(LootData data) {

    public static LootInfo decode(FriendlyByteBuf byteBuf) {
        LootData data = LootData.readFrom(byteBuf);
        return new LootInfo(data);
    }

    public void encode(FriendlyByteBuf byteBuf) {
        this.data.writeTo(byteBuf);
    }

}
