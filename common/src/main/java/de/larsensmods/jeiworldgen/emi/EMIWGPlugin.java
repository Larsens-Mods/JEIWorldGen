package de.larsensmods.jeiworldgen.emi;

import de.larsensmods.jeiworldgen.JEIWorldGenMod;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

@EmiEntrypoint
public class EMIWGPlugin implements EmiPlugin {

    public static final EmiRecipeCategory WORLD_GEN_CATEGORY = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(JEIWorldGenMod.MOD_ID, "world_generation"), EmiStack.of(Items.GRASS_BLOCK), EmiStack.of(Items.DIAMOND_PICKAXE));

    @Override
    public void register(EmiRegistry emiRegistry) {
        emiRegistry.addCategory(WORLD_GEN_CATEGORY);

        for(WorldGenTypeHelper genRecipe : WorldGenTypeHelper.buildRecipes()){
            emiRegistry.addRecipe(genRecipe);
        }
    }
}
