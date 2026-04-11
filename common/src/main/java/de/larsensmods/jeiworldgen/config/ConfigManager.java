package de.larsensmods.jeiworldgen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.larsensmods.jeiworldgen.JEIWorldGenMod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigManager {

    private static final File configFile = new File("./config/jeiworldgen-client.json");

    private static ClientConfig currentConfig = null;

    public static ClientConfig getConfig(){
        if(currentConfig == null){
            if(configFile.exists()){
                currentConfig = tryLoadConfig();
            }
            if(currentConfig == null){
                currentConfig = new ClientConfig();
                writeConfig();
            }
        }
        return currentConfig;
    }

    private static ClientConfig tryLoadConfig(){
        Gson gson = new Gson();
        try {
            String string = Files.readString(configFile.toPath());
            //JEIWorldGenMod.LOGGER.info("Read config file with content:\n {}", string);
            return gson.fromJson(string, ClientConfig.class);
        } catch (Exception e) {
            JEIWorldGenMod.LOGGER.warn("Unable to load config file. Is it corrupted?");
            return null;
        }
    }

    private static void writeConfig(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String config = gson.toJson(currentConfig);
        //JEIWorldGenMod.LOGGER.info("Writing config file with content:\n {}", config);
        try {
            Files.writeString(configFile.toPath(), config);
        } catch (IOException e) {
            JEIWorldGenMod.LOGGER.error("Unable to write config file.", e);
        }
    }

}
