package de.larsensmods.jeiworldgen.forge.networking;

import de.larsensmods.jeiworldgen.networking.Channels;
import de.larsensmods.jeiworldgen.networking.LootInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record LootInfoPayload(LootInfo info) implements CustomPacketPayload {

    public static final Type<LootInfoPayload> TYPE = new Type<>(Channels.LOOT_DATA_SYNC);

    public static final StreamCodec<ByteBuf, LootInfoPayload> STREAM_CODEC = StreamCodec.of(
            (byteBuf, payload) -> {
                FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
                payload.info.encode(buf);
            },
            byteBuf -> {
                FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
                return new LootInfoPayload(LootInfo.decode(buf));
            }
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
