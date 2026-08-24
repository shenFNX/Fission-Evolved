package com.shenfnx.fissionevolved;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.ITexture;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/** Registration and stack helpers for the reactor's dedicated structural blocks. */
public final class FissionStructureBlocks {

    public static BlockFissionStructureCasing CASINGS;
    private static int containmentHatchTextureId = Textures.BlockIcons.ERROR_TEXTURE_INDEX;
    private static boolean registered;
    private static boolean recipesRegistered;

    private FissionStructureBlocks() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CASINGS = new BlockFissionStructureCasing();
        GameRegistry.registerBlock(CASINGS, ItemBlockFissionStructureCasing.class, "fission_structure_casing");
        registerContainmentHatchTexture();
    }

    /**
     * GT hatches render their surrounding casing through a numeric texture page/index rather than directly from the
     * structure block. Give this add-on its own page so every hatch can use the exact containment-casing texture.
     */
    private static void registerContainmentHatchTexture() {
        ITexture texture = TextureFactory.of(CASINGS, BlockFissionStructureCasing.CONTAINMENT);
        for (int page = 127; page >= 0; page--) {
            byte pageId = (byte) page;
            if (!GTUtility.addTexturePage(pageId)) {
                continue;
            }
            containmentHatchTextureId = GTUtility.getTextureId(pageId, (byte) 0);
            Textures.BlockIcons.setCasingTextureForId(containmentHatchTextureId, texture);
            FissionEvolved.LOG
                .info("Registered containment casing hatch texture at GT texture id {}", containmentHatchTextureId);
            return;
        }
        throw new IllegalStateException("No free GT casing texture page is available for Fission Evolved");
    }

    public static int getContainmentHatchTextureId() {
        return containmentHatchTextureId;
    }

    public static ItemStack containment(int amount) {
        return new ItemStack(CASINGS, amount, BlockFissionStructureCasing.CONTAINMENT);
    }

    public static ItemStack reflector(int amount) {
        return new ItemStack(CASINGS, amount, BlockFissionStructureCasing.REFLECTOR);
    }

    public static ItemStack coreConduit(int amount) {
        return new ItemStack(CASINGS, amount, BlockFissionStructureCasing.CORE_CONDUIT);
    }

    /**
     * Batch recipes are intentional: the colossal generator consumes thousands of structural blocks, so making
     * players run one assembler operation per casing would add waiting without adding a meaningful decision.
     */
    public static void registerRecipes() {
        if (recipesRegistered) {
            return;
        }
        recipesRegistered = true;

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Casing_UIV.get(64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Infinity, 16),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.CosmicNeutronium, 16),
                ItemList.Field_Generator_UMV.get(4))
            .fluidInputs(Materials.SpaceTime.getMolten(18_432))
            .itemOutputs(containment(64))
            .duration(60 * 20)
            .eut(TierEU.RECIPE_UMV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                containment(64),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Infinity, 32),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.CosmicNeutronium, 32),
                ItemList.Sensor_UMV.get(8))
            .fluidInputs(Materials.Infinity.getMolten(18_432))
            .itemOutputs(reflector(64))
            .duration(60 * 20)
            .eut(TierEU.RECIPE_UMV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                containment(64),
                ItemList.UMV_Coil.get(64),
                ItemList.Superconducting_Magnet_Solenoid_UMV.get(64),
                ItemList.Emitter_UMV.get(8),
                GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.SuperconductorUMV, 64))
            .fluidInputs(Materials.SpaceTime.getMolten(18_432))
            .itemOutputs(coreConduit(64))
            .duration(60 * 20)
            .eut(TierEU.RECIPE_UMV)
            .addTo(RecipeMaps.assemblerRecipes);

        FissionEvolved.LOG.info("Registered UMV batch recipes for all three colossal generator structure blocks");
    }
}
