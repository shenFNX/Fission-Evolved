package com.shenfnx.fissionevolved;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

/** Exact BigInteger fuel rules and NEI entries for the chunk-scale generator only. */
public final class ColossalFissionReactorRecipes {

    public static final int ONE_HOUR_TICKS = 20 * 60 * 60;
    public static final int CATALYZED_DURATION_TICKS = 24 * ONE_HOUR_TICKS;

    public static final RecipeMap<RecipeMapBackend> RECIPES = RecipeMapBuilder
        .of("fissionevolved.recipe.colossal_generator")
        .maxIO(1, 0, 0, 0)
        .minInputs(1, 0)
        .useSpecialSlot()
        .specialSlotSensitive()
        .frontend(FissionReactorNEIFrontend::new)
        .dontUseProgressBar()
        .neiRecipeComparator(
            Comparator.comparing(ColossalFissionReactorRecipes::getRecipeBaseOutput)
                .thenComparingInt(recipe -> recipe.mDuration))
        .neiSpecialInfoFormatter(recipeInfo -> getRecipeInformation(recipeInfo.recipe))
        .build();

    private static List<FuelDefinition> fuels = Collections.emptyList();
    private static ItemStack proofOfHeroes;
    private static boolean registered;

    private ColossalFissionReactorRecipes() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        fuels = Arrays.asList(
            new FuelDefinition(SuperFuelRods.ETERNAL_ENTROPY_EMBER, 30),
            new FuelDefinition(SuperFuelRods.OMNIVERSAL_STELLAR_CORE, 40),
            new FuelDefinition(SuperFuelRods.SPACETIME_DEVOURER, 50),
            new FuelDefinition(SuperFuelRods.PRIMORDIAL_GODBLIGHT, 60));
        proofOfHeroes = normalizeOne(SuperFuelRods.getProofOfHeroesTemplate());

        for (FuelDefinition fuel : fuels) {
            registerNEIRecipe(fuel, false);
            if (proofOfHeroes != null) {
                registerNEIRecipe(fuel, true);
            }
        }

        FissionEvolved.LOG.info(
            "Registered {} colossal generator fuels; TST Proof of Heroes catalyst {}",
            fuels.size(),
            proofOfHeroes == null ? "unavailable" : "verified");
    }

    public static boolean isUltimateFuel(ItemStack stack) {
        return findDefinition(stack) != null;
    }

    public static BigInteger getBaseOutput(ItemStack stack) {
        FuelDefinition definition = findDefinition(stack);
        return definition == null ? BigInteger.ZERO : definition.baseOutput;
    }

    public static boolean isProofOfHeroes(ItemStack stack) {
        return proofOfHeroes != null && stack != null
            && stack.stackSize > 0
            && stack.getItem() == proofOfHeroes.getItem()
            && stack.getItemDamage() == proofOfHeroes.getItemDamage();
    }

    private static void registerNEIRecipe(FuelDefinition fuel, boolean catalyzed) {
        GTRecipeBuilder builder = GTValues.RA.stdBuilder()
            .itemInputs(fuel.getFuel())
            .duration(catalyzed ? CATALYZED_DURATION_TICKS : ONE_HOUR_TICKS)
            .eut(0);
        if (catalyzed) {
            builder.special(proofOfHeroes.copy());
        }
        builder.addTo(RECIPES);
    }

    private static BigInteger getRecipeBaseOutput(GTRecipe recipe) {
        if (recipe == null || recipe.mInputs.length == 0) {
            return BigInteger.ZERO;
        }
        return getBaseOutput(recipe.mInputs[0]);
    }

    private static List<String> getRecipeInformation(GTRecipe recipe) {
        FuelDefinition fuel = recipe == null || recipe.mInputs.length == 0 ? null : findDefinition(recipe.mInputs[0]);
        String output = fuel == null ? "0" : "10^" + fuel.powerExponent;
        boolean catalyzed = recipe != null && recipe.mSpecialItems instanceof ItemStack;
        return Arrays.asList(
            StatCollector.translateToLocalFormatted("fissionevolved.colossal_nei.base_output", output),
            StatCollector.translateToLocal(
                catalyzed ? "fissionevolved.colossal_nei.duration_catalyzed"
                    : "fissionevolved.colossal_nei.duration_normal"),
            StatCollector.translateToLocal("fissionevolved.colossal_nei.structure_multipliers"),
            StatCollector.translateToLocal("fissionevolved.colossal_nei.total_formula"),
            StatCollector.translateToLocal("fissionevolved.colossal_nei.no_outputs"));
    }

    private static FuelDefinition findDefinition(ItemStack stack) {
        if (stack == null || stack.getItemDamage() != 0) {
            return null;
        }
        for (FuelDefinition fuel : fuels) {
            if (stack.getItem() == fuel.item) {
                return fuel;
            }
        }
        return null;
    }

    private static ItemStack normalizeOne(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }

    private static final class FuelDefinition {

        private final Item item;
        private final int powerExponent;
        private final BigInteger baseOutput;

        private FuelDefinition(Item item, int powerExponent) {
            if (item == null) {
                throw new IllegalStateException("Ultimate fuel items must be registered before colossal recipes");
            }
            this.item = item;
            this.powerExponent = powerExponent;
            this.baseOutput = BigInteger.TEN.pow(powerExponent);
        }

        private ItemStack getFuel() {
            return new ItemStack(item, 1, 0);
        }
    }
}
