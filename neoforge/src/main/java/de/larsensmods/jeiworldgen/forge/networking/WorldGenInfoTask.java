package de.larsensmods.jeiworldgen.forge.networking;

import de.larsensmods.jeiworldgen.networking.Channels;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record WorldGenInfoTask(ServerNetworkHandler networkHandler, ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {

    public static final ConfigurationTask.Type TYPE = new Type(Channels.BIOME_DATA_SYNC);

    @Override
    public void run(@NotNull Consumer<CustomPacketPayload> consumer) {
        consumer.accept(new WorldGenInfoPayload(networkHandler.worldGenInfo));
        listener.finishCurrentTask(type());
    }

    @Override
    public @NotNull Type type() {
        return TYPE;
    }
}
