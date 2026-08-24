package com.shenfnx.fissionevolved;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.GregTechTileClientEvents;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.GTStructureChannels;
import tectech.thing.casing.TTCasingsContainer;

/**
 * Independent chunk-scale successor to the original 7x7x7 reactor. It reuses the proven fuel, coolant, buffer, GUI,
 * and wireless implementation without changing the original controller or its structure.
 */
public final class MTEColossalFissionReactor extends MTEFissionReactor {

    private static final String STRUCTURE_PIECE_MAIN = "colossal_main";
    private static final int STRUCTURE_WIDTH = 48;
    private static final int STRUCTURE_HEIGHT = 32;
    private static final int STRUCTURE_DEPTH = 48;
    private static final int OFFSET_X = 23;
    private static final int OFFSET_Y = 15;
    private static final int OFFSET_Z = 0;
    private static final int OUTER_CHAMFER = 14;
    private static final int CAP_TAPER_LAYERS = 4;
    private static final int FIXED_MAX_PARALLEL = 1 << 20;
    private static final int UNSET_TIER = -1;
    private static final int[] ACCEPTED_EOH_METAS = { 2, 5, 8 };
    private static final int[] ACCEPTED_EOH_TIERS = { 3, 6, 9 };
    private static final String[][] SHAPE_MAIN = buildStructureShape();

    private static IStructureDefinition<MTEFissionReactor> structureDefinition;
    private int fieldTier = UNSET_TIER;

    public MTEColossalFissionReactor(int id, String name, String regionalName) {
        super(id, name, regionalName);
        super.setWirelessEnabled(true);
        coilLevel = HeatingCoilLevel.None;
    }

    private MTEColossalFissionReactor(String name) {
        super(name);
        super.setWirelessEnabled(true);
        coilLevel = HeatingCoilLevel.None;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tileEntity) {
        return new MTEColossalFissionReactor(mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        ITexture containmentCasing = TextureFactory
            .of(FissionStructureBlocks.CASINGS, BlockFissionStructureCasing.CONTAINMENT, side);
        if (side != facing) {
            return new ITexture[] { containmentCasing };
        }
        return new ITexture[] { containmentCasing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_NUCLEAR_REACTOR_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_NUCLEAR_REACTOR)
            .extFacing()
            .build() };
    }

    @Override
    public IStructureDefinition<MTEFissionReactor> getStructureDefinition() {
        if (structureDefinition == null) {
            IStructureElement<MTEFissionReactor> compressionField = GTStructureChannels.EOH_COMPRESSION.use(
                ofBlocksTiered(
                    MTEColossalFissionReactor::getAcceptedFieldTier,
                    getFieldCandidates(TTCasingsContainer.SpacetimeCompressionFieldGenerators),
                    UNSET_TIER,
                    (reactor, tier) -> ((MTEColossalFissionReactor) reactor).setFieldTier(tier),
                    reactor -> ((MTEColossalFissionReactor) reactor).fieldTier));
            IStructureElement<MTEFissionReactor> stabilisationField = GTStructureChannels.EOH_STABILISATION.use(
                ofBlocksTiered(
                    MTEColossalFissionReactor::getAcceptedFieldTier,
                    getFieldCandidates(TTCasingsContainer.StabilisationFieldGenerators),
                    UNSET_TIER,
                    (reactor, tier) -> ((MTEColossalFissionReactor) reactor).setFieldTier(tier),
                    reactor -> ((MTEColossalFissionReactor) reactor).fieldTier));
            IStructureElement<MTEFissionReactor> dilationField = GTStructureChannels.EOH_DILATION.use(
                ofBlocksTiered(
                    MTEColossalFissionReactor::getAcceptedFieldTier,
                    getFieldCandidates(TTCasingsContainer.TimeAccelerationFieldGenerator),
                    UNSET_TIER,
                    (reactor, tier) -> ((MTEColossalFissionReactor) reactor).setFieldTier(tier),
                    reactor -> ((MTEColossalFissionReactor) reactor).fieldTier));
            IStructureElement<MTEFissionReactor> glass = chainAllGlasses(
                UNSET_TIER,
                (reactor, tier) -> ((MTEColossalFissionReactor) reactor).setGlassTier(tier),
                reactor -> ((MTEColossalFissionReactor) reactor).getGlassTier());

            structureDefinition = StructureDefinition.<MTEFissionReactor>builder()
                .addShape(STRUCTURE_PIECE_MAIN, SHAPE_MAIN)
                .addElement(
                    'C',
                    hatchesOr(ofBlock(FissionStructureBlocks.CASINGS, BlockFissionStructureCasing.CONTAINMENT)))
                .addElement('G', hatchesOr(glass))
                .addElement('R', ofBlock(FissionStructureBlocks.CASINGS, BlockFissionStructureCasing.REFLECTOR))
                .addElement('K', ofBlock(FissionStructureBlocks.CASINGS, BlockFissionStructureCasing.CORE_CONDUIT))
                .addElement('P', compressionField)
                .addElement('S', stabilisationField)
                .addElement('T', dilationField)
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
        fieldTier = UNSET_TIER;
        glassTier = UNSET_TIER;
        coilLevel = HeatingCoilLevel.None;

        if (!checkPiece(STRUCTURE_PIECE_MAIN, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) {
            return;
        }
        if (errors.isEmpty()) {
            updateHatchTextures();
            baseTileEntity.sendBlockEvent(GregTechTileClientEvents.CHANGE_CUSTOM_DATA, getUpdateData());
            baseTileEntity.issueTextureUpdate();
        }
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        // The colossal reactor has no coil mechanic. Reset the inherited implementation's multiplier input before
        // every recipe start so old NBT or another add-on cannot reintroduce a coil bonus.
        coilLevel = HeatingCoilLevel.None;
        return super.checkProcessing();
    }

    @Override
    protected boolean isFuelProfileAllowed(FissionReactorRecipes.FuelProfile profile) {
        return profile != null && ColossalFissionReactorRecipes.isUltimateFuel(profile.getFuel(1));
    }

    @Override
    protected BigInteger calculateRecipeOutput(FissionReactorRecipes.FuelProfile profile, int parallel) {
        BigInteger baseOutput = ColossalFissionReactorRecipes.getBaseOutput(profile.getFuel(1));
        return baseOutput.multiply(BigInteger.valueOf(parallel))
            .multiply(BigInteger.valueOf(getStructurePowerMultiplier()));
    }

    @Override
    protected int getRecipeDurationTicks(FissionReactorRecipes.FuelProfile profile, List<ItemStack> inputs) {
        return ColossalFissionReactorRecipes.isProofOfHeroes(getControllerSlot())
            ? ColossalFissionReactorRecipes.CATALYZED_DURATION_TICKS
            : ColossalFissionReactorRecipes.ONE_HOUR_TICKS;
    }

    @Override
    protected ItemStack[] createRecipeItemOutputs(FissionReactorRecipes.FuelProfile profile, int parallel) {
        return new ItemStack[0];
    }

    @Override
    protected FluidStack[] createRecipeFluidOutputs(FissionReactorRecipes.FuelProfile profile, int parallel) {
        return new FluidStack[0];
    }

    @Override
    public boolean usesCoolantBoost() {
        return false;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return ColossalFissionReactorRecipes.RECIPES;
    }

    public int getStructurePowerMultiplier() {
        return switch (fieldTier) {
            case 6 -> 5;
            case 9 -> 10;
            default -> 1;
        };
    }

    @Override
    public int getMaxFuelParallel() {
        return FIXED_MAX_PARALLEL;
    }

    @Override
    public boolean getWirelessMode() {
        return true;
    }

    @Override
    public void setWirelessEnabled(boolean enabled) {
        super.setWirelessEnabled(true);
    }

    @Override
    protected int getSelectedCasingTextureId() {
        return FissionStructureBlocks.getContainmentHatchTextureId();
    }

    private static int getDefaultCasingTextureId() {
        return FissionStructureBlocks.getContainmentHatchTextureId();
    }

    private void setFieldTier(int tier) {
        fieldTier = tier;
        casingTier = switch (tier) {
            case 3 -> 2;
            case 6 -> 5;
            case 9 -> 7;
            default -> UNSET_TIER;
        };
    }

    @Nullable
    private static Integer getAcceptedFieldTier(Block block, int meta) {
        if (block != TTCasingsContainer.SpacetimeCompressionFieldGenerators
            && block != TTCasingsContainer.StabilisationFieldGenerators
            && block != TTCasingsContainer.TimeAccelerationFieldGenerator) {
            return null;
        }
        for (int i = 0; i < ACCEPTED_EOH_METAS.length; i++) {
            if (ACCEPTED_EOH_METAS[i] == meta) {
                return ACCEPTED_EOH_TIERS[i];
            }
        }
        return null;
    }

    private static List<Pair<Block, Integer>> getFieldCandidates(Block block) {
        List<Pair<Block, Integer>> candidates = new ArrayList<>(ACCEPTED_EOH_METAS.length);
        for (int meta : ACCEPTED_EOH_METAS) {
            candidates.add(Pair.of(block, meta));
        }
        return candidates;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(tr("fissionevolved.colossal_reactor.type"))
            .addInfo(tr("fissionevolved.colossal_reactor.info.dimensions"))
            .addInfo(tr("fissionevolved.colossal_reactor.info.exclusive_fuels"))
            .addInfo(tr("fissionevolved.colossal_reactor.info.base_outputs"))
            .addInfo(tr("fissionevolved.colossal_reactor.info.duration"))
            .addInfo(tr("fissionevolved.colossal_reactor.info.catalyst"))
            .addInfo(tr("fissionevolved.colossal_reactor.info.structure_multiplier"))
            .addInfo(
                StatCollector.translateToLocalFormatted(
                    "fissionevolved.colossal_reactor.info.fixed_parallel",
                    Integer.toString(FIXED_MAX_PARALLEL)))
            .addInfo(tr("fissionevolved.colossal_reactor.info.no_outputs"))
            .addInfo(tr("fissionevolved.colossal_reactor.info.forced_wireless"))
            .beginStructureBlock(STRUCTURE_DEPTH, STRUCTURE_WIDTH, STRUCTURE_HEIGHT, false)
            .addController(tr("fissionevolved.reactor.structure.controller"))
            .addCasing(
                Integer.toString(countStructureSymbol('C')),
                tr("fissionevolved.colossal_reactor.structure.containment"),
                true)
            .addCasing(Integer.toString(countStructureSymbol('G')), tr("fissionevolved.reactor.structure.glass"), true)
            .addCasing(
                Integer.toString(countStructureSymbol('R')),
                tr("fissionevolved.colossal_reactor.structure.reflector"),
                false)
            .addCasing(
                Integer.toString(countStructureSymbol('K')),
                tr("fissionevolved.colossal_reactor.structure.conduit"),
                false)
            .addCasing(
                Integer.toString(countStructureSymbol('P')),
                tr("fissionevolved.colossal_reactor.structure.compression"),
                false)
            .addCasing(
                Integer.toString(countStructureSymbol('S')),
                tr("fissionevolved.colossal_reactor.structure.stabilisation"),
                false)
            .addCasing(
                Integer.toString(countStructureSymbol('T')),
                tr("fissionevolved.colossal_reactor.structure.dilation"),
                false)
            .addStructureInfo(tr("fissionevolved.colossal_reactor.structure.same_tiers"))
            .addStructureInfo(tr("fissionevolved.colossal_reactor.structure.free_interior"))
            .addStructureInfo("")
            .addSubChannel(GTStructureChannels.EOH_COMPRESSION)
            .addSubChannel(GTStructureChannels.EOH_STABILISATION)
            .addSubChannel(GTStructureChannels.EOH_DILATION)
            .addSubChannel(GTStructureChannels.BOROGLASS)
            .toolTipFinisher(
                EnumChatFormatting.GRAY,
                41,
                EnumChatFormatting.GOLD + tr("fissionevolved.mod_name") + EnumChatFormatting.RESET);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("FissionColossalFieldTier", fieldTier);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        super.setWirelessEnabled(true);
        coilLevel = HeatingCoilLevel.None;
        int storedFieldTier = tag.hasKey("FissionColossalFieldTier") ? tag.getInteger("FissionColossalFieldTier")
            : casingTier == 2 ? 3 : casingTier == 5 ? 6 : casingTier == 7 ? 9 : UNSET_TIER;
        if (storedFieldTier == 3 || storedFieldTier == 6 || storedFieldTier == 9) {
            setFieldTier(storedFieldTier);
        } else {
            fieldTier = UNSET_TIER;
        }
    }

    private static String[][] buildStructureShape() {
        char[][][] blocks = new char[STRUCTURE_DEPTH][STRUCTURE_HEIGHT][STRUCTURE_WIDTH];
        for (char[][] layer : blocks) {
            for (char[] row : layer) {
                // StructureLib treats ASCII spaces as navigation-only cells: they are absent from occupiedSpaces
                // and impose no air/block requirement, so automation may live anywhere the reactor does not use.
                Arrays.fill(row, ' ');
            }
        }
        for (int z = 0; z < STRUCTURE_DEPTH; z++) {
            for (int y = 0; y < STRUCTURE_HEIGHT; y++) {
                for (int x = 0; x < STRUCTURE_WIDTH; x++) {
                    if (isOuterVesselBlock(x, y, z)) {
                        blocks[z][y][x] = isOuterGlassPanel(x, y, z) ? 'G' : 'C';
                    }
                    if (blocks[z][y][x] == ' ' && isReflectorShell(x, y, z)) {
                        blocks[z][y][x] = 'R';
                    }
                    if (blocks[z][y][x] == ' ' && isCoreConduit(x, y, z)) {
                        blocks[z][y][x] = 'K';
                    }
                    if (isCompressionRing(x, y, z)) {
                        blocks[z][y][x] = 'P';
                    }
                    if (isStabilisationRing(x, y, z)) {
                        blocks[z][y][x] = 'S';
                    }
                    if (isDilationRing(x, y, z)) {
                        blocks[z][y][x] = 'T';
                    }
                }
            }
        }
        blocks[0][OFFSET_Y][OFFSET_X] = '~';
        String[][] shape = new String[STRUCTURE_DEPTH][STRUCTURE_HEIGHT];
        for (int z = 0; z < STRUCTURE_DEPTH; z++) {
            for (int y = 0; y < STRUCTURE_HEIGHT; y++) {
                shape[z][y] = new String(blocks[z][y]);
            }
        }
        return shape;
    }

    private static boolean isOuterVesselBlock(int x, int y, int z) {
        if (!isInsideOuterOctagon(x, y, z)) {
            return false;
        }
        return y == 0 || y == STRUCTURE_HEIGHT - 1
            || isOuterOctagonBoundary(x, y, z)
            || !isInsideOuterOctagon(x, y - 1, z)
            || !isInsideOuterOctagon(x, y + 1, z);
    }

    private static boolean isInsideOuterOctagon(int x, int y, int z) {
        if (x < 0 || x >= STRUCTURE_WIDTH || y < 0 || y >= STRUCTURE_HEIGHT || z < 0 || z >= STRUCTURE_DEPTH) {
            return false;
        }
        int chamfer = getOuterChamfer(y);
        int oppositeX = STRUCTURE_WIDTH - 1 - x;
        int oppositeZ = STRUCTURE_DEPTH - 1 - z;
        return x + z >= chamfer && x + oppositeZ >= chamfer
            && oppositeX + z >= chamfer
            && oppositeX + oppositeZ >= chamfer;
    }

    private static int getOuterChamfer(int y) {
        int distanceToCap = Math.min(y, STRUCTURE_HEIGHT - 1 - y);
        return OUTER_CHAMFER + Math.max(0, CAP_TAPER_LAYERS - distanceToCap);
    }

    private static boolean isOuterOctagonBoundary(int x, int y, int z) {
        return !isInsideOuterOctagon(x - 1, y, z) || !isInsideOuterOctagon(x + 1, y, z)
            || !isInsideOuterOctagon(x, y, z - 1)
            || !isInsideOuterOctagon(x, y, z + 1);
    }

    private static boolean isOuterGlassPanel(int x, int y, int z) {
        if (!isInsideOuterOctagon(x, y, z) || !isOuterOctagonBoundary(x, y, z)
            || y < 3
            || y > STRUCTURE_HEIGHT - 4
            || isHorizontalBelt(y)) {
            return false;
        }
        boolean centralRib = Math.abs(x - 23) <= 1 || Math.abs(x - 24) <= 1
            || Math.abs(z - 23) <= 1
            || Math.abs(z - 24) <= 1;
        int chamfer = getOuterChamfer(y);
        int farVertex = STRUCTURE_WIDTH - 1 - chamfer;
        int farEdge = STRUCTURE_WIDTH - 1;
        boolean octagonVertex = nearVertex(x, z, 0, chamfer) || nearVertex(x, z, 0, farVertex)
            || nearVertex(x, z, chamfer, 0)
            || nearVertex(x, z, farVertex, 0)
            || nearVertex(x, z, farEdge, chamfer)
            || nearVertex(x, z, farEdge, farVertex)
            || nearVertex(x, z, chamfer, farEdge)
            || nearVertex(x, z, farVertex, farEdge);
        return !centralRib && !octagonVertex;
    }

    private static boolean isHorizontalBelt(int y) {
        return y == 5 || y == 10 || y == 15 || y == 16 || y == 21 || y == 26;
    }

    private static boolean nearVertex(int x, int z, int vertexX, int vertexZ) {
        return Math.abs(x - vertexX) + Math.abs(z - vertexZ) <= 1;
    }

    private static boolean isCompressionRing(int x, int y, int z) {
        if (y != 15 && y != 16) {
            return false;
        }
        double dx = x - 23.5D;
        double dz = z - 23.5D;
        double radius = Math.sqrt(dx * dx + dz * dz);
        return radius >= 16.2D && radius <= 17.6D;
    }

    private static boolean isStabilisationRing(int x, int y, int z) {
        return (z == 23 || z == 24) && isEllipseRing(x - 23.5D, y - 15.5D, 13.5D, 11.5D, 0.095D);
    }

    private static boolean isDilationRing(int x, int y, int z) {
        return (x == 23 || x == 24) && isEllipseRing(z - 23.5D, y - 15.5D, 9.5D, 8.0D, 0.13D);
    }

    private static boolean isEllipseRing(double horizontal, double vertical, double horizontalRadius,
        double verticalRadius, double thickness) {
        double normalized = horizontal * horizontal / (horizontalRadius * horizontalRadius)
            + vertical * vertical / (verticalRadius * verticalRadius);
        return Math.abs(normalized - 1.0D) <= thickness;
    }

    private static boolean isReflectorShell(int x, int y, int z) {
        double dx = x - 23.5D;
        double dy = y - 15.5D;
        double dz = z - 23.5D;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        return distanceSquared >= 35.0D && distanceSquared <= 50.0D;
    }

    private static boolean isCoreConduit(int x, int y, int z) {
        boolean centerX = x == 23 || x == 24;
        boolean centerY = y == 15 || y == 16;
        boolean centerZ = z == 23 || z == 24;
        double dx = Math.abs(x - 23.5D);
        double dy = Math.abs(y - 15.5D);
        double dz = Math.abs(z - 23.5D);
        return centerY && centerZ && dx >= 3.5D && dx <= 16.5D || centerX && centerZ && dy >= 3.5D && dy <= 10.5D
            || centerX && centerY && dz >= 3.5D && dz <= 16.5D;
    }

    private static int countStructureSymbol(char symbol) {
        int count = 0;
        for (String[] layer : SHAPE_MAIN) {
            for (String row : layer) {
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == symbol) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
