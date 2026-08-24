package com.shenfnx.fissionevolved;

import java.util.List;

import net.minecraft.client.Minecraft;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.maps.LargeNEIFrontend;
import gregtech.nei.RecipeDisplayInfo;

/** Large recipe layout with pixel-width wrapping for localized reactor descriptions. */
public final class FissionReactorNEIFrontend extends LargeNEIFrontend {

    private static final int DESCRIPTION_WIDTH = 158;

    public FissionReactorNEIFrontend(BasicUIPropertiesBuilder uiPropertiesBuilder,
        NEIRecipePropertiesBuilder neiPropertiesBuilder) {
        super(uiPropertiesBuilder, neiPropertiesBuilder);
    }

    @Override
    protected void drawSpecialInfo(RecipeDisplayInfo recipeInfo) {
        List<String> descriptions = neiProperties.neiSpecialInfoFormatter.format(recipeInfo);
        for (String description : descriptions) {
            for (String line : Minecraft.getMinecraft().fontRenderer
                .listFormattedStringToWidth(description, DESCRIPTION_WIDTH)) {
                recipeInfo.drawText(line);
            }
        }
    }
}
