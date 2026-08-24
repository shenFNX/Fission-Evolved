package com.shenfnx.fissionevolved;

import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.recipe.Scanning;
import tectech.thing.CustomItemList;

/** Registration for the mod's GregTech machines. */
public final class FissionMachines {

    public static MTEFissionReactor FISSION_REACTOR;
    public static ItemStack FISSION_REACTOR_CONTROLLER;
    public static MTEColossalFissionReactor COLOSSAL_FISSION_REACTOR;
    public static ItemStack COLOSSAL_FISSION_REACTOR_CONTROLLER;

    private static boolean registered;
    private static boolean recipesRegistered;

    private FissionMachines() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        FISSION_REACTOR = new MTEFissionReactor(
            Config.fissionReactorControllerId,
            "fissionevolved.fission_reactor_controller",
            "Fission Reactor Controller");
        FISSION_REACTOR_CONTROLLER = FISSION_REACTOR.getStackForm(1L);
        COLOSSAL_FISSION_REACTOR = new MTEColossalFissionReactor(
            Config.colossalFissionReactorControllerId,
            "fissionevolved.colossal_fission_reactor_controller",
            "Ultimate Universe-Destroying Generator Controller");
        COLOSSAL_FISSION_REACTOR_CONTROLLER = COLOSSAL_FISSION_REACTOR.getStackForm(1L);
    }

    public static void registerRecipes() {
        if (recipesRegistered) {
            return;
        }
        recipesRegistered = true;

        GTModHandler.addCraftingRecipe(
            FISSION_REACTOR_CONTROLLER.copy(),
            GTModHandler.RecipeBits.BITS_STD,
            new Object[] { "PCP", "SHS", "PCP", 'P', OrePrefixes.plate.get(Materials.TungstenSteel), 'C',
                OrePrefixes.circuit.get(Materials.IV), 'S', ItemList.Sensor_IV, 'H', ItemList.Hull_IV });

        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, ItemList.Transdimensional_Alignment_Matrix.get(1))
            .metadata(SCANNING, new Scanning(10 * 60 * 20, TierEU.RECIPE_UMV))
            .itemInputs(
                ItemList.Hull_UMV.get(1),
                FissionStructureBlocks.containment(64),
                FissionStructureBlocks.reflector(64),
                FissionStructureBlocks.coreConduit(64),
                CustomItemList.SpacetimeCompressionFieldGeneratorTier2.get(8),
                CustomItemList.StabilisationFieldGeneratorTier2.get(8),
                CustomItemList.TimeAccelerationFieldGeneratorTier2.get(8),
                ItemList.Field_Generator_UMV.get(16),
                ItemList.Emitter_UMV.get(16),
                ItemList.Sensor_UMV.get(16),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UMV, 32),
                ItemList.Transdimensional_Alignment_Matrix.get(8))
            .fluidInputs(
                Materials.Infinity.getMolten(36_864),
                Materials.SpaceTime.getMolten(36_864),
                Materials.Universium.getMolten(18_432),
                Materials.SolderingAlloy.getMolten(36_864))
            .itemOutputs(COLOSSAL_FISSION_REACTOR_CONTROLLER.copy())
            .duration(5 * 60 * 20)
            .eut(TierEU.RECIPE_UMV)
            .addTo(GTRecipeConstants.AssemblyLine);

        FissionEvolved.LOG.info("Registered UMV Assembly Line recipe for the colossal generator controller");
    }
}
