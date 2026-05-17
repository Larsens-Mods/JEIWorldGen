package de.larsensmods.jeiworldgen.networking;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import net.minecraft.resources.Identifier;

public class Channels {

    public static final String PROTOCOL_VERSION = "1";
    public static final Identifier BIOME_DATA_SYNC = Identifier.fromNamespaceAndPath(JEIWorldGenMod.MOD_ID, "biome_data_sync");
    public static final Identifier LOOT_DATA_SYNC = Identifier.fromNamespaceAndPath(JEIWorldGenMod.MOD_ID, "loot_data_sync");

}
