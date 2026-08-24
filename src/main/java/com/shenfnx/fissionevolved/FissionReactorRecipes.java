package com.shenfnx.fissionevolved;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.items.ItemRadioactiveCellIC;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;

/** Fuel discovery, recovery flattening, NEI recipes, and the reactor's deliberately simple power formula. */
public final class FissionReactorRecipes {

    private static final int CHANCE_SCALE = 10_000;
    private static final int MAX_INTRINSIC_PULSES = 11;
    private static final double REACTOR_CONVERSION = 900.0D;
    private static final String NUCLEAR_CONFIG_KEY = "balance/energy/generator/nuclear";
    private static final String NHU_MOD_ID = "NHUtilities";
    private static final String NHU_FUEL_ROD_CLASS = "com.xir.NHUtilities.common.items.itemFuelRod.FuelRod";
    private static final String[] NHU_FUEL_RODS = { "RodRadianceUranium", "RodRadianceUranium2", "RodRadianceUranium4",
        "RodPrimordialPearlPlutonium", "RodPrimordialPearlPlutonium2", "RodPrimordialPearlPlutonium4" };

    public static final RecipeMap<RecipeMapBackend> RECIPES = RecipeMapBuilder
        .of("fissionevolved.recipe.fission_reactor")
        .maxIO(1, 12, 0, 4)
        .minInputs(1, 0)
        .frontend(FissionReactorNEIFrontend::new)
        .dontUseProgressBar()
        .neiRecipeComparator(Comparator.comparing(FissionReactorRecipes::getRecipeBaseOutput))
        .neiSpecialInfoFormatter(recipeInfo -> getRecipeInformation(recipeInfo.recipe))
        .build();

    private static final Map<Item, FuelProfile> PROFILES = new IdentityHashMap<>();
    private static final Map<Item, Item> DEPLETED_TO_ACTIVE = new IdentityHashMap<>();
    private static final LateOptionalFuelLoader LATE_OPTIONAL_FUEL_LOADER = new LateOptionalFuelLoader();
    private static boolean registered;
    private static boolean optionalFuelRegistrationScheduled;
    private static boolean optionalFuelsRegistered;

    private FissionReactorRecipes() {}

    /** Runs in post-init, after GTNH has registered both its rods and their depleted-rod processing recipes. */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        List<ItemRadioactiveCellIC> rods = discoverFuelRods();
        for (ItemRadioactiveCellIC rod : rods) {
            if (rod.sDepleted != null && rod.sDepleted.getItem() != null) {
                DEPLETED_TO_ACTIVE.put(rod.sDepleted.getItem(), rod);
            }
        }

        int missingRecovery = 0;
        for (ItemRadioactiveCellIC rod : rods) {
            ItemStack fuel = new ItemStack(rod, 1, 0);
            Recovery recovery = resolveRecovery(rod.sDepleted, 0);
            if (recovery == null) {
                recovery = new Recovery();
                missingRecovery++;
                FissionEvolved.LOG.warn("No depleted-rod processing recipe found for {}", fuel.getUnlocalizedName());
            }

            FuelProfile profile = new FuelProfile(fuel, calculateBaseOutput(rod), getDurationTicks(rod), recovery);
            PROFILES.put(rod, profile);
            registerNEIRecipe(profile);
        }

        FissionEvolved.LOG.info(
            "Registered {} core fission reactor fuels ({} without recovery outputs), nuclear multiplier {}",
            PROFILES.size(),
            missingRecovery,
            getNuclearMultiplier());
    }

    /** Defers optional fuel discovery until the first tick after every mod has received load-complete. */
    public static synchronized void scheduleLateOptionalFuelRegistration() {
        if (optionalFuelRegistrationScheduled || optionalFuelsRegistered) {
            return;
        }
        optionalFuelRegistrationScheduled = true;
        FMLCommonHandler.instance()
            .bus()
            .register(LATE_OPTIONAL_FUEL_LOADER);
    }

    private static synchronized void registerLateOptionalFuels() {
        if (optionalFuelsRegistered) {
            return;
        }
        optionalFuelsRegistered = true;

        OptionalFuelRegistration nhuRegistration = registerNHUtilitiesFuels();
        if (Loader.isModLoaded(NHU_MOD_ID)) {
            FissionEvolved.LOG.info(
                "Registered {} NH Utilities fission reactor fuels ({} without recovery outputs); {} total fuels",
                nhuRegistration.registered,
                nhuRegistration.missingRecovery,
                PROFILES.size());
        }
    }

    public static Collection<FuelProfile> getProfiles() {
        return PROFILES.values();
    }

    public static FuelProfile findProfile(ItemStack stack) {
        if (stack == null || stack.getItemDamage() != 0) {
            return null;
        }
        return PROFILES.get(stack.getItem());
    }

    /**
     * Base EU/t per rod, before coils, coolant, and parallel.
     *
     * <p>
     * GT's own pulse model is kept visible: energy * cells * intrinsic pulses * 5 * nuclear config. The reactor
     * conversion is the single x900 scale inferred from the reference reactor table.
     * </p>
     */
    public static BigInteger calculateBaseOutput(ItemRadioactiveCellIC rod) {
        if (rod == null || rod.sEnergy <= 0) {
            return BigInteger.ZERO;
        }
        double characteristicMultiplier = getFuelCharacteristicMultiplier(rod);
        return calculateBaseOutput(rod.sEnergy, rod.numberOfCells, characteristicMultiplier);
    }

    private static BigInteger calculateBaseOutput(double energy, int cellCount, double characteristicMultiplier) {
        if (!Double.isFinite(energy) || energy <= 0.0D) {
            return BigInteger.ZERO;
        }
        int cells = Math.max(1, cellCount);
        int pulses = Math.min(cells / 2 + 1, MAX_INTRINSIC_PULSES);
        double nuclearMultiplier = getNuclearMultiplier();
        if (!Double.isFinite(nuclearMultiplier) || nuclearMultiplier <= 0.0D
            || !Double.isFinite(characteristicMultiplier)
            || characteristicMultiplier <= 0.0D) {
            return BigInteger.ZERO;
        }

        // Keep every multiplicative stage outside primitive integer ranges. HALF_UP matches Math.round for the
        // positive values accepted above, while continuing to work beyond Long.MAX_VALUE.
        return BigDecimal.valueOf(energy)
            .multiply(BigDecimal.valueOf(cells))
            .multiply(BigDecimal.valueOf(pulses))
            .multiply(BigDecimal.valueOf(5L))
            .multiply(BigDecimal.valueOf(nuclearMultiplier))
            .multiply(BigDecimal.valueOf(REACTOR_CONVERSION))
            .multiply(BigDecimal.valueOf(characteristicMultiplier))
            .setScale(0, RoundingMode.HALF_UP)
            .toBigInteger()
            .max(BigInteger.ZERO);
    }

    public static double getNuclearMultiplier() {
        return ConfigUtil.getFloat(MainConfig.get(), NUCLEAR_CONFIG_KEY);
    }

    public static long getCoilMultiplier(byte coilTier) {
        return 1L << Math.max(0, Math.min(13, coilTier));
    }

    private static int getDurationTicks(ItemRadioactiveCellIC rod) {
        // One durability point corresponds to one second in the IC2 reactor. This machine runs for 1/1000 of that.
        return Math.max(20, (rod.getMaxDamageEx() + 49) / 50);
    }

    private static double getFuelCharacteristicMultiplier(ItemRadioactiveCellIC rod) {
        if (isAnyRod(rod, ItemList.RodTiberium, ItemList.RodTiberium2, ItemList.RodTiberium4)) {
            return 2.5D;
        }
        if (isAnyRod(rod, ItemList.RodNaquadria, ItemList.RodNaquadria2, ItemList.RodNaquadria4)) {
            return 1.25D;
        }
        if (rod.sMox) {
            return 1.0D + 0.99D * rod.sHeatBonus;
        }
        return 1.0D;
    }

    private static boolean isAnyRod(ItemRadioactiveCellIC rod, ItemList... candidates) {
        for (ItemList candidate : candidates) {
            ItemStack stack = candidate.get(1);
            if (stack != null && stack.getItem() == rod) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemRadioactiveCellIC> discoverFuelRods() {
        List<ItemRadioactiveCellIC> rods = new ArrayList<>();
        for (Object object : Item.itemRegistry) {
            if (object instanceof ItemRadioactiveCellIC rod && rod.sEnergy > 0 && rod.sDepleted != null) {
                rods.add(rod);
            }
        }
        rods.sort(Comparator.comparing(rod -> String.valueOf(Item.itemRegistry.getNameForObject(rod))));
        return rods;
    }

    /**
     * NH Utilities uses its own IC2 reactor-component class instead of GT's ItemRadioactiveCellIC. Keep this
     * adapter reflection-only so NH Utilities remains a completely optional dependency.
     */
    private static OptionalFuelRegistration registerNHUtilitiesFuels() {
        OptionalFuelRegistration result = new OptionalFuelRegistration();
        if (!Loader.isModLoaded(NHU_MOD_ID)) {
            return result;
        }

        final Class<?> fuelRodClass;
        final Field powerField;
        final Field durationField;
        final Field cellsField;
        final Field depletedField;
        final Field heatBonusField;
        try {
            fuelRodClass = Class.forName(NHU_FUEL_ROD_CLASS, false, FissionReactorRecipes.class.getClassLoader());
            powerField = accessibleField(fuelRodClass, "Power");
            durationField = accessibleField(fuelRodClass, "Duration");
            cellsField = accessibleField(fuelRodClass, "numberOfCells");
            depletedField = accessibleField(fuelRodClass, "depletedResult");
            heatBonusField = accessibleField(fuelRodClass, "HeatBonus");
        } catch (ReflectiveOperationException exception) {
            FissionEvolved.LOG.warn("NH Utilities is loaded, but its fuel-rod API is incompatible", exception);
            return result;
        }

        List<OptionalFuelData> fuels = new ArrayList<>();
        for (String registryName : NHU_FUEL_RODS) {
            Item item = GameRegistry.findItem(NHU_MOD_ID, registryName);
            if (item == null || !fuelRodClass.isInstance(item)) {
                FissionEvolved.LOG
                    .warn("Skipped missing or incompatible NH Utilities fuel {}:{}", NHU_MOD_ID, registryName);
                continue;
            }
            try {
                float power = powerField.getFloat(item);
                int duration = durationField.getInt(item);
                int cells = cellsField.getInt(item);
                ItemStack depleted = (ItemStack) depletedField.get(item);
                float heatBonus = heatBonusField.getFloat(item);
                if (power <= 0.0F || duration <= 0 || cells <= 0 || depleted == null || depleted.getItem() == null) {
                    FissionEvolved.LOG.warn("Skipped invalid NH Utilities fuel {}:{}", NHU_MOD_ID, registryName);
                    continue;
                }
                fuels.add(new OptionalFuelData(item, power, duration, cells, depleted, heatBonus));
                DEPLETED_TO_ACTIVE.put(depleted.getItem(), item);
            } catch (IllegalAccessException | ClassCastException exception) {
                FissionEvolved.LOG.warn("Could not read NH Utilities fuel {}:{}", NHU_MOD_ID, registryName, exception);
            }
        }

        for (OptionalFuelData data : fuels) {
            Recovery recovery = resolveRecovery(data.depleted, 0);
            if (recovery == null) {
                recovery = new Recovery();
                result.missingRecovery++;
                FissionEvolved.LOG
                    .warn("No depleted-rod processing recipe found for {}", data.fuel.getUnlocalizedName());
            }

            // NHU's Power is the EU pulse value used by acceptUraniumPulse. Its HeatBonus follows the same
            // full-heat multiplier model as GT MOX rods, so this machine evaluates it at the established 99% point.
            double characteristicMultiplier = 1.0D + 0.99D * Math.max(0.0D, data.heatBonus);
            BigInteger baseOutput = calculateBaseOutput(data.power, data.cells, characteristicMultiplier);
            int durationTicks = Math.max(20, (data.duration + 49) / 50);
            FuelProfile profile = new FuelProfile(new ItemStack(data.fuel, 1, 0), baseOutput, durationTicks, recovery);
            PROFILES.put(data.fuel, profile);
            registerNEIRecipe(profile);
            result.registered++;
        }
        return result;
    }

    private static Field accessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Recovery resolveRecovery(ItemStack depleted, int depth) {
        if (depleted == null || depth > 4) {
            return null;
        }
        ItemStack oneDepletedRod = depleted.copy();
        oneDepletedRod.stackSize = 1;
        GTRecipe source = findRecoveryRecipe(oneDepletedRod);
        if (source == null) {
            return null;
        }

        Recovery recovery = new Recovery();
        for (int i = 0; i < source.mOutputs.length; i++) {
            ItemStack output = source.getOutput(i);
            if (output == null) {
                continue;
            }
            int chance = source.getOutputChance(i);
            Item parentRod = DEPLETED_TO_ACTIVE.get(output.getItem());
            if (parentRod != null) {
                Recovery nested = resolveRecovery(output, depth + 1);
                if (nested != null) {
                    recovery.addNested(nested, output.stackSize, chance);
                } else {
                    FissionEvolved.LOG.warn(
                        "Skipped unresolved nested depleted-rod output {} while flattening reactor recovery",
                        output.getUnlocalizedName());
                }
                // Never expose a depleted rod as a product of this reactor.
                continue;
            }
            recovery.items.add(new ItemRecovery(output, 1, chance));
        }
        for (int i = 0; i < source.mFluidOutputs.length; i++) {
            FluidStack output = source.getFluidOutput(i);
            if (output != null) {
                recovery.fluids.add(new FluidRecovery(output, 1, source.getFluidOutputChance(i)));
            }
        }
        return recovery;
    }

    private static GTRecipe findRecoveryRecipe(ItemStack depleted) {
        List<RecipeMap<?>> maps = new ArrayList<>();
        // Newer GT5U builds expose a dedicated non-cell centrifuge map, while GTNH 2.9.0-beta-2 does not.
        // Resolve it by name so one jar remains loadable on both sides of that API addition.
        addOptionalRecipeMap(maps, "centrifugeNonCellRecipes");
        maps.addAll(
            Arrays.asList(
                RecipeMaps.thermalCentrifugeRecipes,
                RecipeMaps.centrifugeRecipes,
                RecipeMaps.fluidExtractionRecipes));
        for (RecipeMap<?> map : maps) {
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (recipe.mInputs.length == 1 && recipe.mInputs[0] != null
                    && recipe.mInputs[0].stackSize == depleted.stackSize
                    && GTUtility.areStacksEqual(recipe.mInputs[0], depleted, true)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private static void addOptionalRecipeMap(List<RecipeMap<?>> maps, String fieldName) {
        try {
            Object value = RecipeMaps.class.getField(fieldName)
                .get(null);
            if (value instanceof RecipeMap<?>recipeMap) {
                maps.add(recipeMap);
            }
        } catch (ReflectiveOperationException ignored) {
            FissionEvolved.LOG.debug("Optional GT recipe map {} is unavailable in this pack version", fieldName);
        }
    }

    private static void registerNEIRecipe(FuelProfile profile) {
        ItemStack[] itemOutputs = profile.recovery.getDisplayItems();
        int[] itemChances = profile.recovery.getDisplayItemChances();
        FluidStack[] fluidOutputs = profile.recovery.getDisplayFluids();
        int[] fluidChances = profile.recovery.getDisplayFluidChances();

        GTRecipeBuilder builder = GTValues.RA.stdBuilder()
            .itemInputs(profile.fuel.copy())
            .duration(profile.durationTicks)
            .eut(0)
            .specialValue(
                profile.baseOutput.min(BigInteger.valueOf(Integer.MAX_VALUE))
                    .intValue());
        if (itemOutputs.length > 0) {
            builder.itemOutputs(itemOutputs)
                .outputChances(itemChances);
        }
        if (fluidOutputs.length > 0) {
            builder.fluidOutputs(fluidOutputs)
                .fluidOutputChances(fluidChances);
        }
        builder.addTo(RECIPES);
    }

    private static BigInteger getRecipeBaseOutput(GTRecipe recipe) {
        if (recipe.mInputs.length > 0 && recipe.mInputs[0] != null) {
            FuelProfile profile = PROFILES.get(recipe.mInputs[0].getItem());
            if (profile != null) {
                return profile.baseOutput;
            }
        }
        return BigInteger.ZERO;
    }

    private static List<String> getRecipeInformation(GTRecipe recipe) {
        BigInteger baseOutput = getRecipeBaseOutput(recipe);
        return Arrays.asList(
            StatCollector.translateToLocalFormatted("fissionevolved.nei.base_output", formatNumber(baseOutput)),
            StatCollector.translateToLocalFormatted(
                "fissionevolved.nei.nuclear_multiplier",
                formatNumber(getNuclearMultiplier())),
            StatCollector.translateToLocal("fissionevolved.nei.coil_rule"),
            StatCollector.translateToLocal("fissionevolved.nei.consume_rule"));
    }

    public static final class FuelProfile {

        private final ItemStack fuel;
        private final BigInteger baseOutput;
        private final int durationTicks;
        private final Recovery recovery;

        private FuelProfile(ItemStack fuel, BigInteger baseOutput, int durationTicks, Recovery recovery) {
            this.fuel = fuel;
            this.baseOutput = baseOutput;
            this.durationTicks = durationTicks;
            this.recovery = recovery;
        }

        public ItemStack getFuel(int amount) {
            ItemStack stack = fuel.copy();
            stack.stackSize = amount;
            return stack;
        }

        public int getDurationTicks() {
            return durationTicks;
        }

        public BigInteger getBaseOutput() {
            return baseOutput;
        }

        public ItemStack[] rollItemOutputs(int parallel, gregtech.api.interfaces.tileentity.IGregTechTileEntity tile) {
            List<ItemStack> rolled = new ArrayList<>();
            for (ItemRecovery output : recovery.items) {
                long attempts = (long) output.attempts * parallel;
                long successes = rollAttempts(attempts, output.chance, tile);
                long amount = successes * output.stack.stackSize;
                mergeItemOutput(rolled, output.stack, amount);
            }
            return rolled.toArray(new ItemStack[0]);
        }

        public FluidStack[] rollFluidOutputs(int parallel,
            gregtech.api.interfaces.tileentity.IGregTechTileEntity tile) {
            List<FluidStack> rolled = new ArrayList<>();
            for (FluidRecovery output : recovery.fluids) {
                long attempts = (long) output.attempts * parallel;
                long successes = rollAttempts(attempts, output.chance, tile);
                long amount = successes * output.stack.amount;
                mergeFluidOutput(rolled, output.stack, amount);
            }
            return rolled.toArray(new FluidStack[0]);
        }

        private static long rollAttempts(long attempts, int chance,
            gregtech.api.interfaces.tileentity.IGregTechTileEntity tile) {
            if (chance >= CHANCE_SCALE) {
                return attempts;
            }
            long successes = 0;
            for (long i = 0; i < attempts; i++) {
                if (tile.getRandomNumber(CHANCE_SCALE) < chance) {
                    successes++;
                }
            }
            return successes;
        }

        private static void mergeItemOutput(List<ItemStack> outputs, ItemStack template, long amount) {
            if (amount <= 0) {
                return;
            }
            for (ItemStack existing : outputs) {
                if (GTUtility.areStacksEqual(existing, template, true)) {
                    existing.stackSize = safeAdd(existing.stackSize, amount);
                    return;
                }
            }
            ItemStack copy = template.copy();
            copy.stackSize = safeAmount(amount);
            outputs.add(copy);
        }

        private static void mergeFluidOutput(List<FluidStack> outputs, FluidStack template, long amount) {
            if (amount <= 0) {
                return;
            }
            for (FluidStack existing : outputs) {
                if (existing.isFluidEqual(template)) {
                    existing.amount = safeAdd(existing.amount, amount);
                    return;
                }
            }
            FluidStack copy = template.copy();
            copy.amount = safeAmount(amount);
            outputs.add(copy);
        }

        private static int safeAdd(int current, long amount) {
            return safeAmount((long) current + amount);
        }

        private static int safeAmount(long amount) {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, amount));
        }
    }

    private static final class OptionalFuelData {

        private final Item fuel;
        private final float power;
        private final int duration;
        private final int cells;
        private final ItemStack depleted;
        private final float heatBonus;

        private OptionalFuelData(Item fuel, float power, int duration, int cells, ItemStack depleted, float heatBonus) {
            this.fuel = fuel;
            this.power = power;
            this.duration = duration;
            this.cells = cells;
            this.depleted = depleted.copy();
            this.heatBonus = heatBonus;
        }
    }

    private static final class OptionalFuelRegistration {

        private int registered;
        private int missingRecovery;
    }

    public static final class LateOptionalFuelLoader {

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                loadOnce();
            }
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                loadOnce();
            }
        }

        private void loadOnce() {
            FMLCommonHandler.instance()
                .bus()
                .unregister(this);
            registerLateOptionalFuels();
        }
    }

    private static final class Recovery {

        private final List<ItemRecovery> items = new ArrayList<>();
        private final List<FluidRecovery> fluids = new ArrayList<>();

        private void addNested(Recovery nested, int repetitions, int parentChance) {
            for (ItemRecovery item : nested.items) {
                items.add(
                    new ItemRecovery(
                        item.stack,
                        safeMultiply(item.attempts, repetitions),
                        combineChances(parentChance, item.chance)));
            }
            for (FluidRecovery fluid : nested.fluids) {
                fluids.add(
                    new FluidRecovery(
                        fluid.stack,
                        safeMultiply(fluid.attempts, repetitions),
                        combineChances(parentChance, fluid.chance)));
            }
        }

        private ItemStack[] getDisplayItems() {
            ItemStack[] result = new ItemStack[items.size()];
            for (int i = 0; i < items.size(); i++) {
                ItemRecovery output = items.get(i);
                result[i] = output.stack.copy();
                result[i].stackSize = FuelProfile.safeAmount((long) output.stack.stackSize * output.attempts);
            }
            return result;
        }

        private int[] getDisplayItemChances() {
            return items.stream()
                .mapToInt(output -> output.chance)
                .toArray();
        }

        private FluidStack[] getDisplayFluids() {
            FluidStack[] result = new FluidStack[fluids.size()];
            for (int i = 0; i < fluids.size(); i++) {
                FluidRecovery output = fluids.get(i);
                result[i] = output.stack.copy();
                result[i].amount = FuelProfile.safeAmount((long) output.stack.amount * output.attempts);
            }
            return result;
        }

        private int[] getDisplayFluidChances() {
            return fluids.stream()
                .mapToInt(output -> output.chance)
                .toArray();
        }

        private static int safeMultiply(int left, int right) {
            return (int) Math.min(Integer.MAX_VALUE, (long) left * right);
        }

        private static int combineChances(int left, int right) {
            return (int) Math.min(CHANCE_SCALE, Math.round((double) left * right / CHANCE_SCALE));
        }
    }

    private static final class ItemRecovery {

        private final ItemStack stack;
        private final int attempts;
        private final int chance;

        private ItemRecovery(ItemStack stack, int attempts, int chance) {
            this.stack = stack.copy();
            this.attempts = attempts;
            this.chance = chance;
        }
    }

    private static final class FluidRecovery {

        private final FluidStack stack;
        private final int attempts;
        private final int chance;

        private FluidRecovery(FluidStack stack, int attempts, int chance) {
            this.stack = stack.copy();
            this.attempts = attempts;
            this.chance = chance;
        }
    }
}
