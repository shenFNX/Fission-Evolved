package com.shenfnx.fissionevolved;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static gregtech.api.util.GTStructureUtility.activeCoils;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofCoil;
import static gregtech.api.util.GTUtility.validMTEList;
import static gregtech.common.misc.WirelessNetworkManager.addEUToGlobalEnergyMap;
import static gregtech.common.misc.WirelessNetworkManager.strongCheckOrAddUser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Materials;
import gregtech.api.enums.ParticleFX;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.GregTechTileClientEvents;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.WorldSpawnedEventBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.GTStructureChannels;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.xmod.thermalfoundation.fluid.TFFluids;

/**
 * Original seven-by-seven-by-seven fission reactor. Its structure and tier progression are intentionally kept stable
 * for existing worlds; larger successors live in separate controller classes.
 */
public class MTEFissionReactor extends MTEExtendedPowerMultiBlockBase<MTEFissionReactor>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int OFFSET_X = 3;
    private static final int OFFSET_Y = 3;
    private static final int OFFSET_Z = 0;
    private static final int UNSET_TIER = -1;
    private static final int DEFAULT_CASING_META = 0;
    private static final BigInteger SCIENTIFIC_DISPLAY_THRESHOLD = BigInteger.TEN.pow(12);

    private static IStructureDefinition<MTEFissionReactor> structureDefinition;
    private static List<CasingOption> casingOptions;
    private static List<CoolantOption> coolantOptions;
    private static IIconContainer frontInactive;
    private static IIconContainer frontInactiveGlow;
    private static IIconContainer frontActive;
    private static IIconContainer frontActiveGlow;

    protected int casingTier = UNSET_TIER;
    protected int glassTier = UNSET_TIER;
    protected HeatingCoilLevel coilLevel = HeatingCoilLevel.None;
    private boolean wirelessEnabled;
    private UUID ownerUuid;
    private BigInteger energyBuffer = BigInteger.ZERO;
    /** Total recipe EU/t after rod, coil, and parallel multipliers, before efficiency and coolant. */
    private BigInteger activeRecipeOutput = BigInteger.ZERO;
    private String activeFuelNameKey = "fissionevolved.reactor.fuel.none";
    private String activeCoolantKey = "fissionevolved.reactor.coolant.none";
    private int coolantMultiplierPercent = 100;
    private int activeParallel;

    public MTEFissionReactor(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected MTEFissionReactor(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tileEntity) {
        return new MTEFissionReactor(mName);
    }

    @Override
    public IStructureDefinition<MTEFissionReactor> getStructureDefinition() {
        if (structureDefinition == null) {
            IStructureElement<MTEFissionReactor> casing = GTStructureChannels.METAL_MACHINE_CASING.use(
                ofBlocksTiered(
                    MTEFissionReactor::getCasingTier,
                    getCasingCandidates(),
                    UNSET_TIER,
                    MTEFissionReactor::setCasingTier,
                    MTEFissionReactor::getSelectedCasingTier));
            IStructureElement<MTEFissionReactor> glass = chainAllGlasses(
                UNSET_TIER,
                MTEFissionReactor::setGlassTier,
                MTEFissionReactor::getGlassTier);
            IStructureElement<MTEFissionReactor> coils = GTStructureChannels.HEATING_COIL
                .use(activeCoils(ofCoil(MTEFissionReactor::setCoilLevel, MTEFissionReactor::getCoilLevel)));

            structureDefinition = StructureDefinition.<MTEFissionReactor>builder()
                .addShape(
                    STRUCTURE_PIECE_MAIN,
                    new String[][] { { "XXXXXXX", "XGGGGGX", "XGGGGGX", "XGG~GGX", "XGGGGGX", "XGGGGGX", "XXXXXXX" },
                        { "XGGGGGX", "GRRRRRG", "GR...RG", "GR...RG", "GR...RG", "GRRRRRG", "XGGGGGX" },
                        { "XGGGGGX", "GR...RG", "G.HHH.G", "G.HHH.G", "G.HHH.G", "GR...RG", "XGGGGGX" },
                        { "XGGGGGX", "GR...RG", "G.HHH.G", "G.HHH.G", "G.HHH.G", "GR...RG", "XGGGGGX" },
                        { "XGGGGGX", "GR...RG", "G.HHH.G", "G.HHH.G", "G.HHH.G", "GR...RG", "XGGGGGX" },
                        { "XGGGGGX", "GRRRRRG", "GR...RG", "GR...RG", "GR...RG", "GRRRRRG", "XGGGGGX" },
                        { "XXXXXXX", "XGGGGGX", "XGGGGGX", "XGGGGGX", "XGGGGGX", "XGGGGGX", "XXXXXXX" } })
                .addElement('X', hatchesOr(casing))
                .addElement('G', hatchesOr(glass))
                .addElement('R', casing)
                .addElement('H', coils)
                .addElement('.', isAir())
                .build();
        }
        return structureDefinition;
    }

    private static IStructureElement<MTEFissionReactor> hatchesOr(IStructureElement<MTEFissionReactor> fallback) {
        return buildHatchAdder(MTEFissionReactor.class)
            .hatchClasses(
                MTEHatchInputBus.class,
                MTEHatchOutputBus.class,
                MTEHatchInput.class,
                MTEHatchOutput.class,
                MTEHatchMaintenance.class,
                MTEHatchDynamo.class)
            .descriptions(
                () -> Arrays.asList(
                    "fissionevolved.reactor.hatch.input_bus",
                    "fissionevolved.reactor.hatch.output_bus",
                    "fissionevolved.reactor.hatch.input_fluid",
                    "fissionevolved.reactor.hatch.output_fluid",
                    "fissionevolved.reactor.hatch.maintenance",
                    "fissionevolved.reactor.hatch.dynamo",
                    "fissionevolved.reactor.hatch.exotic_dynamo"))
            .adder(MTEFissionReactor::addSupportedHatch)
            .casingIndex(getDefaultCasingTextureId())
            .hint(1)
            .buildAndChain(fallback);
    }

    protected boolean addSupportedHatch(IGregTechTileEntity tileEntity, int casingTextureId) {
        if (tileEntity == null || tileEntity.getMetaTileEntity() == null) {
            return false;
        }

        IMetaTileEntity metaTileEntity = tileEntity.getMetaTileEntity();
        if (addExoticDynamoToMachineList(tileEntity, casingTextureId)) {
            return true;
        }
        if (metaTileEntity instanceof MTEHatchDynamo || metaTileEntity instanceof MTEHatchInput
            || metaTileEntity instanceof MTEHatchOutput
            || metaTileEntity instanceof MTEHatchInputBus
            || metaTileEntity instanceof MTEHatchOutputBus
            || metaTileEntity instanceof MTEHatchMaintenance) {
            // Base fluid hatch classes deliberately include multi-fluid and ME fluid hatch subclasses.
            // Generic registration also accepts high-amperage dynamo subclasses.
            return addToMachineList(tileEntity, casingTextureId);
        }
        return false;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (mMachine) {
            return -1;
        }
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stack,
            OFFSET_X,
            OFFSET_Y,
            OFFSET_Z,
            elementBudget,
            environment,
            false,
            true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseTileEntity, ItemStack stack, List<StructureError> errors) {
        casingTier = UNSET_TIER;
        glassTier = UNSET_TIER;
        coilLevel = HeatingCoilLevel.None;

        if (!checkPiece(STRUCTURE_PIECE_MAIN, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) {
            return;
        }

        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        if (!wirelessEnabled) {
            checkHasAnyDynamo(errors);
        }

        if (errors.isEmpty()) {
            updateHatchTextures();
            baseTileEntity.sendBlockEvent(GregTechTileClientEvents.CHANGE_CUSTOM_DATA, getUpdateData());
            baseTileEntity.issueTextureUpdate();
        }
    }

    protected void updateHatchTextures() {
        int textureId = getSelectedCasingTextureId();
        for (MTEHatch hatch : mInputBusses) {
            hatch.updateTexture(textureId);
        }
        for (MTEHatch hatch : mOutputBusses) {
            hatch.updateTexture(textureId);
        }
        for (MTEHatch hatch : mInputHatches) {
            hatch.updateTexture(textureId);
        }
        for (MTEHatch hatch : mOutputHatches) {
            hatch.updateTexture(textureId);
        }
        for (MTEHatch hatch : mMaintenanceHatches) {
            hatch.updateTexture(textureId);
        }
        for (MTEHatch hatch : mDynamoHatches) {
            hatch.updateTexture(textureId);
        }
        for (MTEHatch hatch : getExoticDynamoHatches()) {
            hatch.updateTexture(textureId);
        }
    }

    @NotNull
    @Override
    public CheckRecipeResult checkProcessing() {
        FissionReactorRecipes.FuelProfile selected = null;
        ArrayList<ItemStack> inputs = getStoredInputs();
        for (ItemStack input : inputs) {
            FissionReactorRecipes.FuelProfile candidate = FissionReactorRecipes.findProfile(input);
            if (candidate != null && isFuelProfileAllowed(candidate)) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return CheckRecipeResultRegistry.NO_FUEL_FOUND;
        }

        int maxParallel = getTrueParallel();
        int available = 0;
        ItemStack template = selected.getFuel(1);
        for (ItemStack input : inputs) {
            if (input != null && GTUtility.areStacksEqual(template, input, true)) {
                available += Math.min(Math.max(0, input.stackSize), maxParallel - available);
                if (available >= maxParallel) {
                    break;
                }
            }
        }
        int parallel = Math.min(available, maxParallel);
        if (parallel <= 0) {
            return CheckRecipeResultRegistry.NO_FUEL_FOUND;
        }

        BigInteger totalOutput = calculateRecipeOutput(selected, parallel);
        if (totalOutput.signum() <= 0) {
            return CheckRecipeResultRegistry.INTERNAL_ERROR;
        }
        if (!consumeFuelAtStart(selected, inputs, parallel)) {
            return CheckRecipeResultRegistry.NO_FUEL_FOUND;
        }

        activeRecipeOutput = totalOutput;
        activeFuelNameKey = getFuelNameKey(selected.getFuel(1));
        // GT's lifecycle and save format still expect primitive EU/t fields. A positive sentinel keeps generator
        // semantics intact; onRunningTick below never hands this value to GT's dynamo/explosion output path.
        lEUt = 1L;
        mEUt = 1;
        mMaxProgresstime = getRecipeDurationTicks(selected, inputs);
        mProgresstime = 0;
        mEfficiency = 10_000;
        mEfficiencyIncrease = 10_000;
        activeParallel = parallel;
        mOutputItems = createRecipeItemOutputs(selected, parallel);
        mOutputFluids = createRecipeFluidOutputs(selected, parallel);
        updateSlots();
        return CheckRecipeResultRegistry.GENERATING;
    }

    protected boolean isFuelProfileAllowed(FissionReactorRecipes.FuelProfile profile) {
        return true;
    }

    protected BigInteger calculateRecipeOutput(FissionReactorRecipes.FuelProfile profile, int parallel) {
        return profile.getBaseOutput()
            .multiply(BigInteger.valueOf(FissionReactorRecipes.getCoilMultiplier(coilLevel.getTier())))
            .multiply(BigInteger.valueOf(parallel));
    }

    protected int getRecipeDurationTicks(FissionReactorRecipes.FuelProfile profile, List<ItemStack> inputs) {
        return profile.getDurationTicks();
    }

    protected ItemStack[] createRecipeItemOutputs(FissionReactorRecipes.FuelProfile profile, int parallel) {
        return profile.rollItemOutputs(parallel, getBaseMetaTileEntity());
    }

    protected FluidStack[] createRecipeFluidOutputs(FissionReactorRecipes.FuelProfile profile, int parallel) {
        return profile.rollFluidOutputs(parallel, getBaseMetaTileEntity());
    }

    private boolean consumeFuelAtStart(FissionReactorRecipes.FuelProfile profile, List<ItemStack> inputs,
        int parallel) {
        int remaining = parallel;
        ItemStack template = profile.getFuel(1);
        for (ItemStack input : inputs) {
            if (remaining <= 0) {
                return true;
            }
            if (input == null || !GTUtility.areStacksEqual(template, input, true)) {
                continue;
            }
            int amount = Math.min(remaining, input.stackSize);
            if (amount <= 0 || !depleteInput(profile.getFuel(amount))) {
                return false;
            }
            remaining -= amount;
        }
        return remaining <= 0;
    }

    public int getMaxFuelParallel() {
        int tier = Math.max(0, Math.min(7, casingTier));
        return 64 << tier;
    }

    @Override
    public int getMaxParallelRecipes() {
        return getMaxFuelParallel();
    }

    public int getActiveFuelParallel() {
        return mMaxProgresstime > 0 ? activeParallel : 0;
    }

    public String getCurrentGenerationText() {
        if (mMaxProgresstime <= 0 || activeRecipeOutput.signum() <= 0) {
            return "0";
        }
        return formatBigInteger(getCurrentGeneration(coolantMultiplierPercent));
    }

    public String getActiveFuelNameKey() {
        return mMaxProgresstime > 0 && activeRecipeOutput.signum() > 0 ? normalizeFuelNameKey(activeFuelNameKey)
            : "fissionevolved.reactor.fuel.none";
    }

    private static String getFuelNameKey(ItemStack fuel) {
        if (fuel != null && fuel.getItem() instanceof SuperFuelRodItem customFuel) {
            return customFuel.getDisplayNameKey();
        }
        return fuel == null ? "fissionevolved.reactor.fuel.unknown" : fuel.getUnlocalizedName() + ".name";
    }

    private static String normalizeFuelNameKey(String key) {
        String brokenCustomPrefix = "gt." + FissionEvolved.MOD_ID;
        if (key != null && key.startsWith(brokenCustomPrefix) && key.endsWith(".name")) {
            String registryName = key.substring(brokenCustomPrefix.length(), key.length() - ".name".length());
            if (registryName.startsWith(".")) {
                registryName = registryName.substring(1);
            }
            return "item." + FissionEvolved.MOD_ID + "." + registryName + ".name";
        }
        return key == null || key.isEmpty() ? "fissionevolved.reactor.fuel.unknown" : key;
    }

    public int getRemainingTicks() {
        return mMaxProgresstime > 0 ? Math.max(0, mMaxProgresstime - mProgresstime) : 0;
    }

    public String getEstimatedRunGenerationText() {
        if (mMaxProgresstime <= 0 || activeRecipeOutput.signum() <= 0) {
            return "0";
        }
        return formatBigInteger(
            getCurrentGeneration(coolantMultiplierPercent).multiply(BigInteger.valueOf(mMaxProgresstime)));
    }

    public String getPauseReasonKey() {
        if (!mMachine) {
            return "fissionevolved.reactor.pause.invalid_structure";
        }
        if (mMaxProgresstime <= 0 || activeRecipeOutput.signum() <= 0) {
            return "fissionevolved.reactor.pause.idle";
        }
        BigInteger generated = getCurrentGeneration(coolantMultiplierPercent);
        if (!canStoreGeneratedEnergy(generated)) {
            return "fissionevolved.reactor.pause.buffer_full";
        }
        return "fissionevolved.reactor.pause.none";
    }

    public int getDynamoOutputCount() {
        return mDynamoHatches.size() + getExoticDynamoHatches().size();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return FissionReactorRecipes.RECIPES;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (activeRecipeOutput.signum() <= 0) {
            // A legacy save can only reach here if it did not contain either the new BigInteger field or a valid
            // former long EU/t value. Pausing is safer than invoking GT's primitive generator output path.
            return false;
        }

        if (!usesCoolantBoost()) {
            BigInteger generated = getCurrentGeneration(100);
            if (!canStoreGeneratedEnergy(generated)) {
                return false;
            }
            activeCoolantKey = "fissionevolved.reactor.coolant.none";
            coolantMultiplierPercent = 100;
            energyBuffer = energyBuffer.add(generated);
            return true;
        }

        boolean selectCoolant = mProgresstime % 20 == 0;
        CoolantOption nextCoolant = selectCoolant ? findBestCoolantForSecond() : null;
        int nextCoolantMultiplier = selectCoolant ? nextCoolant == null ? 100 : nextCoolant.multiplierPercent
            : coolantMultiplierPercent;

        BigInteger generated = getCurrentGeneration(nextCoolantMultiplier);
        if (!canStoreGeneratedEnergy(generated)) {
            // A full controller buffer pauses this tick. Progress, fuel life, and recovery outputs are preserved.
            return false;
        }
        if (selectCoolant) {
            activateCoolantForSecond(nextCoolant);
        }
        energyBuffer = energyBuffer.add(generated);
        return true;
    }

    public boolean usesCoolantBoost() {
        return true;
    }

    private BigInteger getCurrentGeneration(int coolantPercent) {
        return activeRecipeOutput.multiply(BigInteger.valueOf(Math.max(0, mEfficiency)))
            .divide(BigInteger.valueOf(10_000L))
            .multiply(BigInteger.valueOf(Math.max(0, coolantPercent)))
            .divide(BigInteger.valueOf(100L));
    }

    private boolean canStoreGeneratedEnergy(BigInteger generatedEU) {
        if (generatedEU == null || generatedEU.signum() <= 0) {
            return true;
        }
        // Always admit at least one complete generation tick. Otherwise a valid ultra-long recipe whose per-tick
        // output exceeds the nominal casing buffer would enter a permanent zero-buffer deadlock.
        BigInteger effectiveCapacity = getEnergyBufferCapacity().max(generatedEU);
        return energyBuffer.add(generatedEU)
            .compareTo(effectiveCapacity) <= 0;
    }

    private BigInteger getEnergyBufferCapacity() {
        int tier = Math.max(0, Math.min(7, casingTier));
        return BigInteger.TEN.pow(15 + tier * 2);
    }

    /** Drains stored power without invoking GT's overflow-explosion path. */
    private void drainEnergyBuffer() {
        if (energyBuffer.signum() <= 0) {
            return;
        }
        if (wirelessEnabled) {
            ensureWirelessOwner();
            if (ownerUuid != null && addEUToGlobalEnergyMap(ownerUuid, energyBuffer)) {
                energyBuffer = BigInteger.ZERO;
                markDirty();
            }
            return;
        }

        for (MTEHatch hatch : validMTEList(mDynamoHatches)) {
            drainToDynamo(hatch);
            if (energyBuffer.signum() <= 0) {
                return;
            }
        }
        for (MTEHatch hatch : validMTEList(getExoticDynamoHatches())) {
            drainToDynamo(hatch);
            if (energyBuffer.signum() <= 0) {
                return;
            }
        }
    }

    private void drainToDynamo(MTEHatch hatch) {
        if (hatch == null || hatch.getBaseMetaTileEntity() == null || energyBuffer.signum() <= 0) {
            return;
        }
        long voltage = Math.max(0L, hatch.maxEUOutput());
        long amperage = Math.max(0L, hatch.maxAmperesOut());
        long capacity = Math.max(
            0L,
            hatch.getBaseMetaTileEntity()
                .getEUCapacity());
        long stored = Math.max(
            0L,
            hatch.getBaseMetaTileEntity()
                .getStoredEU());
        long freeSpace = capacity > stored ? capacity - stored : 0L;
        if (voltage <= 0 || amperage <= 0 || freeSpace <= 0) {
            return;
        }

        BigInteger tickThroughput = BigInteger.valueOf(voltage)
            .multiply(BigInteger.valueOf(amperage));
        long amount = energyBuffer.min(tickThroughput)
            .min(BigInteger.valueOf(freeSpace))
            .min(BigInteger.valueOf(Long.MAX_VALUE))
            .longValue();
        if (amount > 0 && hatch.getBaseMetaTileEntity()
            .increaseStoredEnergyUnits(amount, false)) {
            energyBuffer = energyBuffer.subtract(BigInteger.valueOf(amount));
            markDirty();
        }
    }

    @Override
    protected void outputAfterRecipe() {
        clearActiveRecipeState();
    }

    @Override
    public void stopMachine(@NotNull ShutDownReason reason) {
        clearActiveRecipeState();
        super.stopMachine(reason);
    }

    private void clearActiveRecipeState() {
        activeRecipeOutput = BigInteger.ZERO;
        activeFuelNameKey = "fissionevolved.reactor.fuel.none";
        activeParallel = 0;
        activeCoolantKey = "fissionevolved.reactor.coolant.none";
        coolantMultiplierPercent = 100;
        lEUt = 0L;
        mEUt = 0;
    }

    @Nullable
    private CoolantOption findBestCoolantForSecond() {
        for (CoolantOption option : getCoolantOptions()) {
            if (depleteInput(option.fluid, true)) {
                return option;
            }
        }
        return null;
    }

    private void activateCoolantForSecond(@Nullable CoolantOption option) {
        if (option != null && depleteInput(option.fluid, false)) {
            activeCoolantKey = option.langKey;
            coolantMultiplierPercent = option.multiplierPercent;
        } else {
            activeCoolantKey = "fissionevolved.reactor.coolant.none";
            coolantMultiplierPercent = 100;
        }
    }

    private static List<CoolantOption> getCoolantOptions() {
        if (coolantOptions == null) {
            coolantOptions = new ArrayList<>();
            addCoolant(Materials.Time.getMolten(20L), "Material.temporalfluid", 1_600);
            if (TFFluids.fluidCryotheum != null) {
                addCoolant(
                    new FluidStack(TFFluids.fluidCryotheum, 1_000),
                    "fissionevolved.reactor.coolant.cryotheum",
                    800);
            }
            addCoolant(Materials.SuperCoolant.getFluid(1_000L), "fissionevolved.reactor.coolant.super_coolant", 400);
            addCoolant(GTModHandler.getIC2Coolant(1_000L), "fissionevolved.reactor.coolant.ic2", 200);
            addCoolant(GTModHandler.getDistilledWater(1_000L), "fissionevolved.reactor.coolant.distilled_water", 150);
            addCoolant(Materials.Water.getFluid(1_000L), "fissionevolved.reactor.coolant.water", 125);
        }
        return coolantOptions;
    }

    private static void addCoolant(FluidStack fluid, String langKey, int multiplierPercent) {
        if (fluid != null && fluid.getFluid() != null) {
            coolantOptions.add(new CoolantOption(fluid, langKey, multiplierPercent));
        }
    }

    @Override
    public void onPreTick(IGregTechTileEntity baseTileEntity, long tick) {
        super.onPreTick(baseTileEntity, tick);
        if (baseTileEntity.isServerSide()) {
            if (tick == 1) {
                ensureWirelessOwner();
            }
            if (mMachine && energyBuffer.signum() > 0) {
                drainEnergyBuffer();
            }
        }
    }

    private void ensureWirelessOwner() {
        if (ownerUuid == null && getBaseMetaTileEntity() != null) {
            ownerUuid = getBaseMetaTileEntity().getOwnerUuid();
        }
        if (ownerUuid != null) {
            strongCheckOrAddUser(ownerUuid);
        }
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        setWirelessEnabled(!wirelessEnabled);
        GTUtility.sendChatTrans(
            player,
            wirelessEnabled ? "fissionevolved.reactor.wireless.enabled" : "fissionevolved.reactor.wireless.disabled");
    }

    public boolean getWirelessMode() {
        return wirelessEnabled;
    }

    /** Sets the stored wireless mode without touching a structure that may not be attached to the world yet. */
    protected final void setWirelessEnabledSilently(boolean enabled) {
        wirelessEnabled = enabled;
    }

    public void setWirelessEnabled(boolean enabled) {
        setWirelessEnabledSilently(enabled);
        if (enabled) {
            ensureWirelessOwner();
        }
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isServerSide()) {
            checkStructure(true, base);
        }
    }

    public String getActiveCoolantKey() {
        return activeCoolantKey;
    }

    public int getCoolantMultiplierPercent() {
        return coolantMultiplierPercent;
    }

    public String getEnergyBufferText() {
        return formatBigInteger(energyBuffer);
    }

    private static String formatBigInteger(BigInteger value) {
        BigInteger nonNegative = value == null ? BigInteger.ZERO : value.max(BigInteger.ZERO);
        String digits = nonNegative.toString();
        if (nonNegative.compareTo(SCIENTIFIC_DISPLAY_THRESHOLD) < 0) {
            StringBuilder grouped = new StringBuilder(digits.length() + digits.length() / 3);
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && (digits.length() - i) % 3 == 0) {
                    grouped.append(',');
                }
                grouped.append(digits.charAt(i));
            }
            return grouped.toString();
        }

        StringBuilder mantissa = new StringBuilder().append(digits.charAt(0));
        int fractionalDigits = Math.min(3, digits.length() - 1);
        if (fractionalDigits > 0) {
            mantissa.append('.')
                .append(digits, 1, 1 + fractionalDigits);
            while (mantissa.charAt(mantissa.length() - 1) == '0') {
                mantissa.setLength(mantissa.length() - 1);
            }
            if (mantissa.charAt(mantissa.length() - 1) == '.') {
                mantissa.setLength(mantissa.length() - 1);
            }
        }
        return mantissa + " × 10^" + (digits.length() - 1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister iconRegister) {
        super.registerIcons(iconRegister);
        frontInactive = Textures.BlockIcons.custom("fissionevolved:iconsets/fission_reactor_front");
        frontInactiveGlow = Textures.BlockIcons.custom("fissionevolved:iconsets/fission_reactor_front_glow");
        frontActive = Textures.BlockIcons.custom("fissionevolved:iconsets/fission_reactor_front_active");
        frontActiveGlow = Textures.BlockIcons.custom("fissionevolved:iconsets/fission_reactor_front_active_glow");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onRandomDisplayTick(IGregTechTileEntity baseTileEntity) {
        if (!Config.enableReactorParticles || baseTileEntity == null || !baseTileEntity.isActive()) {
            return;
        }
        ForgeDirection front = baseTileEntity.getFrontFacing();
        double spreadX = (baseTileEntity.getWorld().rand.nextDouble() - 0.5D) * 0.42D;
        double spreadY = (baseTileEntity.getWorld().rand.nextDouble() - 0.5D) * 0.42D;
        double spreadZ = (baseTileEntity.getWorld().rand.nextDouble() - 0.5D) * 0.42D;
        double x = baseTileEntity.getXCoord() + 0.5D + front.offsetX * 0.56D + spreadX;
        double y = baseTileEntity.getYCoord() + 0.5D + spreadY;
        double z = baseTileEntity.getZCoord() + 0.5D + front.offsetZ * 0.56D + spreadZ;
        new WorldSpawnedEventBuilder.ParticleEventBuilder().setWorld(baseTileEntity.getWorld())
            .setIdentifier(
                baseTileEntity.getWorld().rand.nextBoolean() ? ParticleFX.FIREWORKS_SPARK : ParticleFX.PORTAL)
            .setPosition(x, y, z)
            .setMotion(front.offsetX * 0.015D, 0.018D, front.offsetZ * 0.015D)
            .run();
    }

    @Override
    protected SoundResource getActivitySoundLoop() {
        return Config.enableReactorSounds ? SoundResource.GT_MACHINES_FUSION_LOOP : null;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        if (Config.enableAnimatedController && frontInactive != null
            && frontInactiveGlow != null
            && frontActive != null
            && frontActiveGlow != null) {
            return Textures.BlockIcons.createTextureWithCasing(
                this,
                side,
                facing,
                active,
                frontInactive,
                frontInactiveGlow,
                frontActive,
                frontActiveGlow);
        }
        ITexture casingTexture = getCasingTexture();
        if (side != facing) {
            return new ITexture[] { casingTexture };
        }
        return new ITexture[] { casingTexture, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_NUCLEAR_REACTOR_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_NUCLEAR_REACTOR)
            .extFacing()
            .build() };
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(getSelectedCasingTextureId());
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(tr("fissionevolved.reactor.type"))
            .addInfo(tr("fissionevolved.reactor.info.fuels"))
            .addInfo(tr("fissionevolved.reactor.info.power_formula"))
            .addInfo(tr("fissionevolved.reactor.info.duration"))
            .addInfo(tr("fissionevolved.reactor.info.parallel"))
            .addInfo(tr("fissionevolved.reactor.info.coils"))
            .addSeparator()
            .addInfo(tr("fissionevolved.reactor.info.fuel_flow"))
            .addInfo(tr("fissionevolved.reactor.info.recovery"))
            .addInfo(tr("fissionevolved.reactor.info.interruption"))
            .addSeparator()
            .addInfo(tr("fissionevolved.reactor.info.coolant"))
            .addInfo(tr("fissionevolved.reactor.info.coolant.water"))
            .addInfo(tr("fissionevolved.reactor.info.coolant.distilled_water"))
            .addInfo(tr("fissionevolved.reactor.info.coolant.ic2"))
            .addInfo(tr("fissionevolved.reactor.info.coolant.super_coolant"))
            .addInfo(tr("fissionevolved.reactor.info.coolant.cryotheum"))
            .addInfo(
                StatCollector.translateToLocalFormatted(
                    "fissionevolved.reactor.info.coolant.temporal",
                    tr("Material.temporalfluid")))
            .addSeparator()
            .addInfo(tr("fissionevolved.reactor.info.wireless"))
            .addInfo(tr("fissionevolved.reactor.info.buffer"))
            .beginStructureBlock(7, 7, 7, false)
            .addController(tr("fissionevolved.reactor.structure.controller"))
            .addCasing("112", tr("fissionevolved.reactor.structure.casing"), true)
            .addCasing("149", tr("fissionevolved.reactor.structure.glass"), true)
            .addCasing("27", tr("fissionevolved.reactor.structure.coil"), true)
            .addInputBus("1+", tr("fissionevolved.reactor.structure.surface"), 1)
            .addOutputBus("1+", tr("fissionevolved.reactor.structure.surface"), 1)
            .addInputHatch("0+", tr("fissionevolved.reactor.structure.surface"), 1)
            .addOutputHatch("0+", tr("fissionevolved.reactor.structure.surface"), 1)
            .addDynamoHatch("1+", tr("fissionevolved.reactor.structure.surface"), 1)
            .addMaintenanceHatch("0+", tr("fissionevolved.reactor.structure.surface"), 1)
            .addStructureInfo(tr("fissionevolved.reactor.structure.unlimited_hatches"))
            .addStructureInfo(tr("fissionevolved.reactor.structure.same_tiers"))
            .addStructureInfo("")
            .addSubChannel(GTStructureChannels.METAL_MACHINE_CASING)
            .addSubChannel(GTStructureChannels.HEATING_COIL)
            .addSubChannel(GTStructureChannels.BOROGLASS)
            /*
             * .toolTipFinisher(
             * EnumChatFormatting.GRAY,
             * 41,
             * EnumChatFormatting.GOLD + tr("fissionevolved.mod_name") + EnumChatFormatting.RESET);
             */
            .toolTipFinisher(EnumChatFormatting.GOLD + tr("fissionevolved.mod_name") + EnumChatFormatting.RESET);
    }

    protected static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setBoolean("FissionWirelessEnabled", wirelessEnabled);
        tag.setInteger("FissionCasingTier", casingTier);
        tag.setInteger("FissionGlassTier", glassTier);
        tag.setByte("FissionCoilTier", coilLevel.getTier());
        tag.setByteArray("FissionEnergyBuffer", energyBuffer.toByteArray());
        tag.setByteArray("FissionActiveRecipeOutput", activeRecipeOutput.toByteArray());
        tag.setString("FissionActiveFuelName", activeFuelNameKey);
        tag.setInteger("FissionActiveParallel", activeParallel);
        tag.setString("FissionActiveCoolant", activeCoolantKey);
        tag.setInteger("FissionCoolantMultiplier", coolantMultiplierPercent);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        wirelessEnabled = tag.getBoolean("FissionWirelessEnabled");
        casingTier = tag.getInteger("FissionCasingTier");
        glassTier = tag.getInteger("FissionGlassTier");
        coilLevel = HeatingCoilLevel.getFromTier(tag.getByte("FissionCoilTier"));
        byte[] storedBuffer = tag.getByteArray("FissionEnergyBuffer");
        energyBuffer = storedBuffer.length == 0 ? BigInteger.ZERO : new BigInteger(storedBuffer).max(BigInteger.ZERO);
        byte[] storedRecipeOutput = tag.getByteArray("FissionActiveRecipeOutput");
        if (storedRecipeOutput.length > 0) {
            activeRecipeOutput = new BigInteger(storedRecipeOutput).max(BigInteger.ZERO);
        } else if (mMaxProgresstime > 0 && lEUt > 0) {
            // Migration from the former long-backed implementation.
            activeRecipeOutput = BigInteger.valueOf(lEUt);
        } else {
            activeRecipeOutput = BigInteger.ZERO;
        }
        activeFuelNameKey = normalizeFuelNameKey(
            tag.hasKey("FissionActiveFuelName") ? tag.getString("FissionActiveFuelName")
                : "fissionevolved.reactor.fuel.unknown");
        activeParallel = Math.max(0, tag.getInteger("FissionActiveParallel"));
        activeCoolantKey = tag.hasKey("FissionActiveCoolant") ? tag.getString("FissionActiveCoolant")
            : "fissionevolved.reactor.coolant.none";
        coolantMultiplierPercent = tag.hasKey("FissionCoolantMultiplier")
            ? Math.max(0, tag.getInteger("FissionCoolantMultiplier"))
            : 100;
        if (mMaxProgresstime > 0 && activeRecipeOutput.signum() > 0) {
            // Re-establish the harmless lifecycle sentinel even when loading a future/edited NBT with mEUt = 0.
            lEUt = 1L;
            mEUt = 1;
        }
    }

    @Override
    public byte getUpdateData() {
        return (byte) Math.max(0, casingTier);
    }

    @Override
    public void receiveClientEvent(byte eventId, byte value) {
        super.receiveClientEvent(eventId, value);
        if (eventId == GregTechTileClientEvents.CHANGE_CUSTOM_DATA && (value & 0x80) == 0) {
            casingTier = value;
        }
    }

    private void setCasingTier(int tier) {
        casingTier = tier;
    }

    private int getSelectedCasingTier() {
        return casingTier;
    }

    protected void setGlassTier(int tier) {
        glassTier = tier;
    }

    protected int getGlassTier() {
        return glassTier;
    }

    protected void setCoilLevel(HeatingCoilLevel level) {
        coilLevel = level;
    }

    protected HeatingCoilLevel getCoilLevel() {
        return coilLevel;
    }

    @Nullable
    private static Integer getCasingTier(Block block, int meta) {
        for (CasingOption option : getCasingOptions()) {
            if (option.block == block && option.meta == meta) {
                return option.tier;
            }
        }
        return null;
    }

    protected int getSelectedCasingTextureId() {
        for (CasingOption option : getCasingOptions()) {
            if (option.tier == casingTier) {
                return option.textureId;
            }
        }
        return getDefaultCasingTextureId();
    }

    private static int getDefaultCasingTextureId() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings4, DEFAULT_CASING_META);
    }

    private static List<Pair<Block, Integer>> getCasingCandidates() {
        return getCasingOptions().stream()
            .map(option -> Pair.of(option.block, option.meta))
            .toList();
    }

    private static List<CasingOption> getCasingOptions() {
        if (casingOptions == null) {
            casingOptions = Arrays.asList(
                new CasingOption(0, ModBlocks.blockCustomMachineCasings, 0, 10),
                new CasingOption(1, GregTechAPI.sBlockCasings2, 0, 16),
                new CasingOption(2, ModBlocks.blockCustomMachineCasings, 1, 17),
                new CasingOption(3, GregTechAPI.sBlockCasings4, 1, 49),
                new CasingOption(4, GregTechAPI.sBlockCasings4, 2, 50),
                new CasingOption(5, GregTechAPI.sBlockCasings4, 0, 48),
                new CasingOption(6, ModBlocks.blockCustomMachineCasings, 2, 84),
                new CasingOption(7, ModBlocks.blockCustomMachineCasings, 3, 100));
        }
        return casingOptions;
    }

    private static final class CasingOption {

        private final int tier;
        private final Block block;
        private final int meta;
        private final int textureId;

        private CasingOption(int tier, Block block, int meta, int textureId) {
            this.tier = tier;
            this.block = block;
            this.meta = meta;
            this.textureId = textureId;
        }
    }

    private static final class CoolantOption {

        private final FluidStack fluid;
        private final String langKey;
        private final int multiplierPercent;

        private CoolantOption(FluidStack fluid, String langKey, int multiplierPercent) {
            this.fluid = fluid;
            this.langKey = langKey;
            this.multiplierPercent = multiplierPercent;
        }
    }

    @NotNull
    @Override
    protected MTEMultiBlockBaseGui<MTEFissionReactor> getGui() {
        return new FissionReactorGui(this);
    }
}
