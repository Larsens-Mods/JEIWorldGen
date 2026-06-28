package de.larsensmods.jeiworldgen.rei;

import de.larsensmods.jeiworldgen.events.ClientEvents;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.minecraft.client.Minecraft;

public class REIWGPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new WorldGenCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        ClientEvents.playerJoinedWorld(Minecraft.getInstance().player);
        WorldGenTypeHelper.buildRecipes().forEach(registry::add);
    }
}
