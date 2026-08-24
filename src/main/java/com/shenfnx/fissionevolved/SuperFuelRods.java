package com.shenfnx.fissionevolved;

import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
import static gregtech.api.util.GTRecipeConstants.PRECISE_ASSEMBLER_CASING_TIER;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.items.ItemRadioactiveCellIC;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.recipe.Scanning;
import gtnhintergalactic.recipe.IGRecipeMaps;

/** Registration for Fission Evolved fuel rods and their deterministic processing recipes. */
public final class SuperFuelRods {

    private static final String TST_MOD_ID = "TwistSpaceTechnology";
    private static final String TST_META_ITEM = "MetaItem01";
    private static final String GTNL_MOD_ID = "sciencenotleisure";
    private static final String GTNL_META_ITEM = "MetaItem";
    private static final String TST_ITEM_LIST = "com.Nxer.TwistSpaceTechnology.common.GTCMItemList";
    private static final String GTNL_ITEM_LIST = "com.science.gtnl.utils.enums.GTNLItemList";
    private static final float CUSTOM_FUEL_POWER_SCALE = 1_000.0F;

    private static final OptionalItemSpec GTNL_EXCITED_MIXED_NAQUADAH_ROD = new OptionalItemSpec(
        "GTNL Excited Mixed Naquadah Fuel Rod",
        GTNL_MOD_ID,
        GTNL_META_ITEM,
        1,
        GTNL_ITEM_LIST,
        "DepletedExcitedNaquadahFuelRod");
    private static final OptionalItemSpec TST_SPACE_WARPER = new OptionalItemSpec(
        "TST Space Warper",
        TST_MOD_ID,
        TST_META_ITEM,
        1,
        TST_ITEM_LIST,
        "SpaceWarper");
    private static final OptionalItemSpec TST_ANTIMATTER_FUEL_ROD = new OptionalItemSpec(
        "TST Antimatter Fuel Rod",
        TST_MOD_ID,
        TST_META_ITEM,
        16,
        TST_ITEM_LIST,
        "AntimatterFuelRod");
    private static final OptionalItemSpec TST_STELLAR_CONSTRUCTION_FRAME = new OptionalItemSpec(
        "TST Stellar Construction Frame Material",
        TST_MOD_ID,
        TST_META_ITEM,
        17,
        TST_ITEM_LIST,
        "StellarConstructionFrameMaterial");
    private static final OptionalItemSpec TST_STRANGE_ANNIHILATION_FUEL_ROD = new OptionalItemSpec(
        "TST Strange Annihilation Fuel Rod",
        TST_MOD_ID,
        TST_META_ITEM,
        29,
        TST_ITEM_LIST,
        "StrangeAnnihilationFuelRod");
    private static final OptionalItemSpec TST_PROOF_OF_HEROES = new OptionalItemSpec(
        "TST Proof of Heroes",
        TST_MOD_ID,
        "ProofOfHeroes",
        0,
        TST_ITEM_LIST,
        "ProofOfHeroes");
    private static final Map<String, ItemStack> OPTIONAL_ITEM_CACHE = new HashMap<>();
    private static final Set<String> OPTIONAL_ITEM_RESOLUTION_ATTEMPTED = new HashSet<>();

    public static final FissionEvolvedTab TAB = new FissionEvolvedTab();

    public static SuperFuelRodItem SUPERNOVA_EMBER;
    public static SuperDepletedFuelRodItem SUPERNOVA_EMBER_DEPLETED;
    public static SuperFuelRodItem INFERNAL_PLUTONIUM_CORE;
    public static SuperDepletedFuelRodItem INFERNAL_PLUTONIUM_CORE_DEPLETED;
    public static SuperFuelRodItem NAQUADAH_STARHEART;
    public static SuperDepletedFuelRodItem NAQUADAH_STARHEART_DEPLETED;
    public static SuperFuelRodItem RIFT_SINGULARITY;
    public static SuperDepletedFuelRodItem RIFT_SINGULARITY_DEPLETED;
    public static SuperFuelRodItem TIBERIUM_CATACLYSM;
    public static SuperDepletedFuelRodItem TIBERIUM_CATACLYSM_DEPLETED;
    public static SuperFuelRodItem ETERNAL_ENTROPY_EMBER;
    public static SuperDepletedFuelRodItem ETERNAL_ENTROPY_EMBER_DEPLETED;
    public static SuperFuelRodItem OMNIVERSAL_STELLAR_CORE;
    public static SuperDepletedFuelRodItem OMNIVERSAL_STELLAR_CORE_DEPLETED;
    public static SuperFuelRodItem SPACETIME_DEVOURER;
    public static SuperDepletedFuelRodItem SPACETIME_DEVOURER_DEPLETED;
    public static SuperFuelRodItem PRIMORDIAL_GODBLIGHT;
    public static SuperDepletedFuelRodItem PRIMORDIAL_GODBLIGHT_DEPLETED;

    private static boolean registered;
    private static boolean recipesRegistered;

    private SuperFuelRods() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        FuelRodPair pair = registerFuelRod(
            "supernova_ember_fuel_rod",
            "Supernova Ember Fuel Rod",
            ItemList.RodUranium4,
            40.0F);
        SUPERNOVA_EMBER = pair.active;
        SUPERNOVA_EMBER_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "infernal_plutonium_core_fuel_rod",
            "Infernal Plutonium Core Fuel Rod",
            ItemList.RodMOX4,
            80.0F);
        INFERNAL_PLUTONIUM_CORE = pair.active;
        INFERNAL_PLUTONIUM_CORE_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "naquadah_starheart_fuel_rod",
            "Naquadah Starheart Fuel Rod",
            ItemList.RodNaquadah4,
            640.0F);
        NAQUADAH_STARHEART = pair.active;
        NAQUADAH_STARHEART_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "rift_singularity_fuel_rod",
            "Rift Singularity Fuel Rod",
            ItemList.RodNaquadria4,
            5_120.0F);
        RIFT_SINGULARITY = pair.active;
        RIFT_SINGULARITY_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "tiberium_cataclysm_fuel_rod",
            "Tiberium Cataclysm Fuel Rod",
            ItemList.RodTiberium4,
            40_960.0F);
        TIBERIUM_CATACLYSM = pair.active;
        TIBERIUM_CATACLYSM_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "eternal_entropy_ember_fuel_rod",
            "fissionevolved:ultimate_fuel_rod_01_crimson",
            "fissionevolved:depleted_supernova_ember_fuel_rod",
            "Eternal Entropy Ember Fuel Rod",
            ItemList.RodTiberium4,
            327_680.0F);
        ETERNAL_ENTROPY_EMBER = pair.active;
        ETERNAL_ENTROPY_EMBER_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "omniversal_stellar_core_fuel_rod",
            "fissionevolved:ultimate_fuel_rod_02_celestial",
            "fissionevolved:depleted_naquadah_starheart_fuel_rod",
            "Omniversal Stellar Core Fuel Rod",
            ItemList.RodTiberium4,
            2_621_440.0F);
        OMNIVERSAL_STELLAR_CORE = pair.active;
        OMNIVERSAL_STELLAR_CORE_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "spacetime_devourer_fuel_rod",
            "fissionevolved:ultimate_fuel_rod_03_void",
            "fissionevolved:depleted_rift_singularity_fuel_rod",
            "Spacetime Devourer Fuel Rod",
            ItemList.RodTiberium4,
            20_971_520.0F);
        SPACETIME_DEVOURER = pair.active;
        SPACETIME_DEVOURER_DEPLETED = pair.depleted;

        pair = registerFuelRod(
            "primordial_godblight_fuel_rod",
            "fissionevolved:ultimate_fuel_rod_04_tiberium",
            "fissionevolved:depleted_tiberium_cataclysm_fuel_rod",
            "Primordial Godblight Fuel Rod",
            ItemList.RodTiberium4,
            167_772_160.0F);
        PRIMORDIAL_GODBLIGHT = pair.active;
        PRIMORDIAL_GODBLIGHT_DEPLETED = pair.depleted;
    }

    public static void registerRecipes() {
        if (recipesRegistered) {
            return;
        }
        recipesRegistered = true;

        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.RodUranium4.get(10), Materials.Tungsten.getDust(4))
            .itemOutputs(new ItemStack(SUPERNOVA_EMBER, 10, 0))
            .duration(10 * 20)
            .eut(1920)
            .addTo(RecipeMaps.assemblerRecipes);

        registerRecoveryRecipe(
            SUPERNOVA_EMBER_DEPLETED,
            Materials.TungstenSteel,
            Materials.Helium.getGas(32_000),
            1_920);

        registerFuelRecipe(ItemList.RodMOX4, Materials.Osmiridium, INFERNAL_PLUTONIUM_CORE, 7_680);
        registerRecoveryRecipe(
            INFERNAL_PLUTONIUM_CORE_DEPLETED,
            Materials.Osmiridium,
            Materials.Radon.getGas(64_000),
            7_680);

        registerFuelRecipe(ItemList.RodNaquadah4, Materials.Trinium, NAQUADAH_STARHEART, 122_880);
        registerRecoveryRecipe(
            NAQUADAH_STARHEART_DEPLETED,
            Materials.Trinium,
            Materials.Helium3.getGas(128_000),
            122_880);

        registerFuelRecipe(ItemList.RodNaquadria4, Materials.Neutronium, RIFT_SINGULARITY, 1_966_080);
        registerRecoveryRecipe(
            RIFT_SINGULARITY_DEPLETED,
            Materials.Neutronium,
            Materials.Tritium.getGas(256_000),
            1_966_080);

        registerFuelRecipe(ItemList.RodTiberium4, Materials.CosmicNeutronium, TIBERIUM_CATACLYSM, 7_864_320);
        registerRecoveryRecipe(
            TIBERIUM_CATACLYSM_DEPLETED,
            Materials.CosmicNeutronium,
            Materials.ExcitedDTRC.getFluid(512_000),
            7_864_320);

        registerUltimateFuelRecipes();
        registerRecoveryRecipe(
            ETERNAL_ENTROPY_EMBER_DEPLETED,
            Materials.Infinity,
            Materials.ExcitedDTEC.getFluid(1_024_000),
            TierEU.RECIPE_UIV);
        registerRecoveryRecipe(
            OMNIVERSAL_STELLAR_CORE_DEPLETED,
            Materials.Universium,
            Materials.RawStarMatter.getFluid(2_048_000),
            TierEU.RECIPE_UMV);
        registerRecoveryRecipe(
            SPACETIME_DEVOURER_DEPLETED,
            Materials.SpaceTime,
            Materials.SpaceTime.getMolten(4_096_000),
            TierEU.RECIPE_UXV);
        registerRecoveryRecipe(
            PRIMORDIAL_GODBLIGHT_DEPLETED,
            Materials.Eternity,
            Materials.PrimordialMatter.getFluid(8_192_000),
            TierEU.RECIPE_MAX);
    }

    /** Resolves TST's semantic item-list entry without creating a hard runtime dependency on TST. */
    public static ItemStack getProofOfHeroesTemplate() {
        return findOptionalItem(TST_PROOF_OF_HEROES, 1);
    }

    private static void registerUltimateFuelRecipes() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, new ItemStack(TIBERIUM_CATACLYSM, 1, 0))
            .metadata(SCANNING, new Scanning(20 * 60 * 20, TierEU.RECIPE_UIV))
            .itemInputs(
                new ItemStack(TIBERIUM_CATACLYSM, 8, 0),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Infinity, 8),
                ItemList.Field_Generator_UEV.get(4),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UIV, 8))
            .fluidInputs(Materials.Infinity.getMolten(9_216), Materials.ExcitedDTEC.getFluid(16_000))
            .itemOutputs(new ItemStack(ETERNAL_ENTROPY_EMBER, 8, 0))
            .duration(60 * 20)
            .eut(TierEU.RECIPE_UIV)
            .addTo(GTRecipeConstants.AssemblyLine);

        ItemStack gtnlRod = findOptionalItem(GTNL_EXCITED_MIXED_NAQUADAH_ROD, 4);
        ItemStack omniversalAddonInput = gtnlRod != null ? gtnlRod : ItemList.NuclearStar.get(16);
        logRecipeRoute(
            "Omniversal Stellar Core",
            gtnlRod != null ? "GTNL Excited Mixed Naquadah Fuel Rods" : "GTNH Nuclear Stars");
        GTValues.RA.stdBuilder()
            .metadata(PRECISE_ASSEMBLER_CASING_TIER, 4)
            .itemInputs(
                new ItemStack(ETERNAL_ENTROPY_EMBER, 8, 0),
                omniversalAddonInput,
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Universium, 8),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UMV, 8))
            .fluidInputs(
                Materials.SpaceTime.getMolten(9_216),
                Materials.RawStarMatter.getFluid(32_000),
                preferredFluid("molten.mutatedlivingsolder", 18_432, Materials.SolderingAlloy.getMolten(18_432)))
            .itemOutputs(new ItemStack(OMNIVERSAL_STELLAR_CORE, 8, 0))
            .duration(90 * 20)
            .eut(TierEU.RECIPE_UMV)
            .addTo(GoodGeneratorRecipeMaps.preciseAssemblerRecipes);

        ItemStack tstAntimatterRod = findOptionalItem(TST_ANTIMATTER_FUEL_ROD, 4);
        ItemStack tstStellarFrame = findOptionalItem(TST_STELLAR_CONSTRUCTION_FRAME, 16);
        ItemStack tstSpaceWarper = findOptionalItem(TST_SPACE_WARPER, 8);
        boolean useTstSpacetime = tstAntimatterRod != null && tstStellarFrame != null && tstSpaceWarper != null;
        ItemStack[] spacetimeAddonInputs = useTstSpacetime
            ? new ItemStack[] { tstAntimatterRod, tstStellarFrame, tstSpaceWarper }
            : new ItemStack[] { ItemList.Transdimensional_Alignment_Matrix.get(4),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.SpaceTime, 16),
                ItemList.EnergisedTesseract.get(8) };
        logRecipeRoute(
            "Spacetime Devourer",
            useTstSpacetime ? "TST antimatter rods, stellar frames and space warpers"
                : "GTNH alignment matrices, SpaceTime frames and energised tesseracts");
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(OMNIVERSAL_STELLAR_CORE, 8, 0),
                spacetimeAddonInputs[0],
                spacetimeAddonInputs[1],
                spacetimeAddonInputs[2],
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UXV, 8))
            .fluidInputs(
                Materials.Space.getMolten(9_216),
                Materials.Time.getMolten(9_216),
                Materials.PrimordialMatter.getFluid(32_000))
            .itemOutputs(new ItemStack(SPACETIME_DEVOURER, 8, 0))
            .duration(120 * 20)
            .eut(TierEU.RECIPE_UXV)
            .specialValue(3)
            .addTo(IGRecipeMaps.spaceAssemblerRecipes);

        ItemStack tstAnnihilationRod = findOptionalItem(TST_STRANGE_ANNIHILATION_FUEL_ROD, 4);
        ItemStack gtnlFinalRod = findOptionalItem(GTNL_EXCITED_MIXED_NAQUADAH_ROD, 8);
        ItemStack primordialTstInput = tstAnnihilationRod != null ? tstAnnihilationRod
            : firstAvailableGtnhItem(
                "Primordial Godblight TST fallback",
                ItemList.Field_Generator_UXV.get(4),
                ItemList.Transdimensional_Alignment_Matrix.get(4),
                ItemList.NuclearStar.get(16));
        ItemStack primordialGtnlInput = gtnlFinalRod != null ? gtnlFinalRod
            : firstAvailableGtnhItem(
                "Primordial Godblight GTNL fallback",
                ItemList.NuclearStar.get(16),
                ItemList.Gravistar.get(16),
                ItemList.QuantumStar.get(16));
        ItemStack primordialFieldGenerators = firstAvailableGtnhItem(
            "Primordial Godblight field generators",
            ItemList.Field_Generator_MAX.get(4),
            ItemList.Field_Generator_UXV.get(4),
            ItemList.Transdimensional_Alignment_Matrix.get(4));
        logRecipeRoute(
            "Primordial Godblight",
            (tstAnnihilationRod != null ? "TST Strange Annihilation rods" : "GTNH UXV field generators") + ", "
                + (gtnlFinalRod != null ? "GTNL Excited Mixed Naquadah Fuel Rods" : "GTNH Nuclear Stars"));
        GTValues.RA.stdBuilder()
            .metadata(COIL_HEAT, 13_501)
            .itemInputs(
                new ItemStack(SPACETIME_DEVOURER, 8, 0),
                primordialTstInput,
                primordialGtnlInput,
                primordialFieldGenerators,
                ItemList.Transdimensional_Alignment_Matrix.get(1))
            .fluidInputs(
                Materials.PrimordialMatter.getFluid(128_000),
                Materials.Eternity.getMolten(36_864),
                Materials.Universium.getMolten(36_864),
                Materials.RawStarMatter.getFluid(128_000))
            .itemOutputs(new ItemStack(PRIMORDIAL_GODBLIGHT, 8, 0))
            .duration(300 * 20)
            .eut(TierEU.RECIPE_MAX)
            .addTo(RecipeMaps.plasmaForgeRecipes);
    }

    private static FuelRodPair registerFuelRod(String registryName, String englishName, ItemList baseRodEntry,
        float energyMultiplier) {
        return registerFuelRod(
            registryName,
            "fissionevolved:" + registryName,
            "fissionevolved:depleted_" + registryName,
            englishName,
            baseRodEntry,
            energyMultiplier);
    }

    private static FuelRodPair registerFuelRod(String registryName, String activeTexture, String depletedTexture,
        String englishName, ItemList baseRodEntry, float energyMultiplier) {
        ItemStack baseStack = baseRodEntry.get(1);
        if (baseStack == null || !(baseStack.getItem() instanceof ItemRadioactiveCellIC)) {
            throw new IllegalStateException("Required GT5U fuel rod is unavailable: " + baseRodEntry);
        }

        String depletedRegistryName = registryName + "_depleted";
        SuperDepletedFuelRodItem depleted = new SuperDepletedFuelRodItem(depletedRegistryName, depletedTexture);
        GameRegistry.registerItem(depleted, depletedRegistryName, FissionEvolved.MOD_ID);

        SuperFuelRodItem active = new SuperFuelRodItem(
            registryName,
            activeTexture,
            englishName,
            (ItemRadioactiveCellIC) baseStack.getItem(),
            new ItemStack(depleted, 1, 0),
            energyMultiplier * CUSTOM_FUEL_POWER_SCALE);
        return new FuelRodPair(active, depleted);
    }

    private static void registerFuelRecipe(ItemList baseRod, Materials additive, SuperFuelRodItem output, int eut) {
        GTValues.RA.stdBuilder()
            .itemInputs(baseRod.get(10), additive.getDust(4))
            .itemOutputs(new ItemStack(output, 10, 0))
            .duration(10 * 20)
            .eut(eut)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    /** No output chances are supplied: every custom depleted rod always returns the full listed products. */
    private static void registerRecoveryRecipe(SuperDepletedFuelRodItem depleted, Materials recoveredMaterial,
        FluidStack recoveredFluid, long eut) {
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(depleted, 1, 0))
            .itemOutputs(recoveredMaterial.getDust(32))
            .fluidOutputs(recoveredFluid)
            .duration(5 * 20)
            .eut(eut)
            .addTo(RecipeMaps.centrifugeRecipes);
    }

    /**
     * Resolve optional-addon ingredients through the addon's semantic item list. The registry tuple is retained only
     * as an audit of the versions this integration was written against; it is never trusted by itself.
     */
    private static ItemStack findOptionalItem(OptionalItemSpec spec, int amount) {
        if (!Loader.isModLoaded(spec.modId)) {
            return null;
        }

        String cacheKey = spec.itemListClass + '#' + spec.itemListField;
        if (OPTIONAL_ITEM_RESOLUTION_ATTEMPTED.contains(cacheKey)) {
            return copyWithAmount(OPTIONAL_ITEM_CACHE.get(cacheKey), amount);
        }
        OPTIONAL_ITEM_RESOLUTION_ATTEMPTED.add(cacheKey);

        ItemStack semanticStack;
        try {
            Class<?> itemListClass = Class.forName(spec.itemListClass);
            Object itemListEntry = itemListClass.getField(spec.itemListField)
                .get(null);
            Object resolvedStack = itemListClass.getMethod("getInternalStack_unsafe")
                .invoke(itemListEntry);
            if (!(resolvedStack instanceof ItemStack)) {
                return rejectOptionalItem(spec, "the addon item-list entry did not return an ItemStack", null);
            }
            semanticStack = ((ItemStack) resolvedStack).copy();
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException
            | InvocationTargetException | LinkageError | RuntimeException exception) {
            return rejectOptionalItem(spec, "the addon item-list entry could not be read", exception);
        }

        if (semanticStack.getItem() == null) {
            return rejectOptionalItem(spec, "the addon item-list entry is empty", null);
        }

        String semanticRegistryName = (String) Item.itemRegistry.getNameForObject(semanticStack.getItem());
        if (semanticRegistryName == null || !semanticRegistryName.startsWith(spec.modId + ':')) {
            return rejectOptionalItem(
                spec,
                "the addon item-list entry points outside " + spec.modId
                    + " (resolved as "
                    + semanticRegistryName
                    + ')',
                null);
        }

        Item legacyItem = GameRegistry.findItem(spec.modId, spec.legacyRegistryName);
        if (legacyItem == semanticStack.getItem() && semanticStack.getItemDamage() == spec.legacyMeta) {
            FissionEvolved.LOG.info(
                "Verified optional integration {} through {}.{} and {}:{}@{}",
                spec.semanticName,
                spec.itemListClass,
                spec.itemListField,
                spec.modId,
                spec.legacyRegistryName,
                spec.legacyMeta);
        } else {
            FissionEvolved.LOG.warn(
                "Optional integration mapping for {} moved from {}:{}@{} to {}@{}; using the addon's semantic "
                    + "item-list entry instead of the stale metadata",
                spec.semanticName,
                spec.modId,
                spec.legacyRegistryName,
                spec.legacyMeta,
                semanticRegistryName,
                semanticStack.getItemDamage());
        }

        semanticStack.stackSize = 1;
        OPTIONAL_ITEM_CACHE.put(cacheKey, semanticStack);
        return copyWithAmount(semanticStack, amount);
    }

    private static ItemStack rejectOptionalItem(OptionalItemSpec spec, String reason, Throwable exception) {
        FissionEvolved.LOG.warn(
            "Optional integration semantic check failed for {}: {}; using the GTNH fallback recipe. No unverified "
                + "addon item will be consumed.",
            spec.semanticName,
            reason);
        if (exception != null) {
            FissionEvolved.LOG.debug("Optional integration resolution failure for " + spec.semanticName, exception);
        }
        return null;
    }

    private static ItemStack copyWithAmount(ItemStack template, int amount) {
        if (template == null) {
            return null;
        }
        ItemStack copy = template.copy();
        copy.stackSize = amount;
        return copy;
    }

    private static FluidStack preferredFluid(String registryName, int amount, FluidStack fallback) {
        FluidStack preferred = FluidRegistry.getFluidStack(registryName, amount);
        return preferred != null ? preferred : fallback;
    }

    private static ItemStack firstAvailableGtnhItem(String purpose, ItemStack... candidates) {
        for (ItemStack candidate : candidates) {
            if (candidate != null && candidate.getItem() != null) {
                FissionEvolved.LOG.info("Resolved {} as {}", purpose, candidate.getUnlocalizedName());
                return candidate;
            }
        }
        throw new IllegalStateException("No registered GTNH item is available for " + purpose);
    }

    private static void logRecipeRoute(String fuelName, String route) {
        FissionEvolved.LOG.info("{} recipe route: {}", fuelName, route);
    }

    private static final class FuelRodPair {

        private final SuperFuelRodItem active;
        private final SuperDepletedFuelRodItem depleted;

        private FuelRodPair(SuperFuelRodItem active, SuperDepletedFuelRodItem depleted) {
            this.active = active;
            this.depleted = depleted;
        }
    }

    private static final class OptionalItemSpec {

        private final String semanticName;
        private final String modId;
        private final String legacyRegistryName;
        private final int legacyMeta;
        private final String itemListClass;
        private final String itemListField;

        private OptionalItemSpec(String semanticName, String modId, String legacyRegistryName, int legacyMeta,
            String itemListClass, String itemListField) {
            this.semanticName = semanticName;
            this.modId = modId;
            this.legacyRegistryName = legacyRegistryName;
            this.legacyMeta = legacyMeta;
            this.itemListClass = itemListClass;
            this.itemListField = itemListField;
        }
    }
}
