package gregtech.common.tileentities.machines.multi;

import static gregtech.api.enums.GTValues.VN;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizons.modularui.api.NumberFormatMUI;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SoundResource;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.gui.widgets.LockedWhileActiveButton;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetricsExporter;
import gregtech.api.objects.GTChunkManager;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.blocks.BlockOresAbstract;
import gregtech.common.blocks.TileEntityOres;
import gregtech.crossmod.visualprospecting.VisualProspectingDatabase;

public abstract class MTEOreDrillingPlantBase extends MTEDrillerBase implements IMetricsExporter {

    private final List<ChunkPosition> oreBlockPositions = new ArrayList<>();
    protected int mTier = 1;
    private int chunkRadiusConfig = getRadiusInChunks();
    private boolean replaceWithCobblestone = true;

    /** Used to drive the remaining ores count in the UI. */
    private int clientOreListSize = 0;

    /** Used to drive the current chunk number in the UI. */
    private int clientCurrentChunk = 0;

    /** Used to drive the total chunk count in the UI. */
    private int clientTotalChunks = 0;

    /** Used to drive the drill's y-level in the UI. */
    private int clientYHead = 0;

    /** Contains the name of the currently mined vein. Used for driving metrics cover output. */
    private String veinName = null;

    MTEOreDrillingPlantBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    MTEOreDrillingPlantBase(String aName) {
        super(aName);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("chunkRadiusConfig", chunkRadiusConfig);
        aNBT.setBoolean("replaceWithCobblestone", replaceWithCobblestone);
        if (veinName != null) {
            aNBT.setString("veinName", veinName);
        } else if (aNBT.hasKey("veinName")) {
            aNBT.removeTag("veinName");
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (aNBT.hasKey("chunkRadiusConfig")) {
            chunkRadiusConfig = aNBT.getInteger("chunkRadiusConfig");
        }
        if (aNBT.hasKey("replaceWithCobblestone")) {
            replaceWithCobblestone = aNBT.getBoolean("replaceWithCobblestone");
        }
        if (aNBT.hasKey("veinName")) {
            veinName = aNBT.getString("veinName");
        } else {
            veinName = null;
        }
    }

    private void adjustChunkRadius(boolean increase) {
        if (increase) {
            if (chunkRadiusConfig <= getRadiusInChunks()) {
                chunkRadiusConfig++;
            }
            if (chunkRadiusConfig > getRadiusInChunks()) chunkRadiusConfig = 1;
        } else {
            if (chunkRadiusConfig > 0) {
                chunkRadiusConfig--;
            }
            if (chunkRadiusConfig == 0) chunkRadiusConfig = getRadiusInChunks();
        }

        if (mCurrentChunk != null && mChunkLoadingEnabled) {
            GTChunkManager.releaseChunk((TileEntity) getBaseMetaTileEntity(), mCurrentChunk);
        }

        oreBlockPositions.clear();
        createInitialWorkingChunk();
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        super.onScrewdriverRightClick(side, aPlayer, aX, aY, aZ, aTool);

        if (getBaseMetaTileEntity().isActive()) {
            GTUtility.sendChatToPlayer(aPlayer, StatCollector.translateToLocal("GT5U.machines.workarea_fail"));
        } else {
            adjustChunkRadius(!aPlayer.isSneaking());
            GTUtility.sendChatToPlayer(
                aPlayer,
                StatCollector.translateToLocal("GT5U.machines.workareaset") + " "
                    + GTUtility.formatNumbers((long) chunkRadiusConfig << 4)
                    + " "
                    + StatCollector.translateToLocal("GT5U.machines.radius"));
        }
    }

    @Override
    public boolean onWireCutterRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool) {
        replaceWithCobblestone = !replaceWithCobblestone;
        GTUtility.sendChatToPlayer(aPlayer, "Replace with cobblestone " + replaceWithCobblestone);
        return true;
    }

    @Override
    protected boolean workingDownward(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe,
        int yHead, int oldYHead) {
        if (yHead != oldYHead) oreBlockPositions.clear();

        if (mWorkChunkNeedsReload && mChunkLoadingEnabled) { // ask to load machine itself
            GTChunkManager.requestChunkLoad((TileEntity) getBaseMetaTileEntity(), null);
            mWorkChunkNeedsReload = false;
        }
        fillMineListIfEmpty(xDrill, yDrill, zDrill, xPipe, zPipe, yHead);
        if (oreBlockPositions.isEmpty()) {
            if (veinName == null) {
                updateVeinNameFromVP(getDrillCoords());
            }

            switch (tryLowerPipeState()) {
                case 2 -> {
                    mMaxProgresstime = 0;
                    setRuntimeFailureReason(CheckRecipeResultRegistry.MISSING_MINING_PIPE);
                    return false;
                }
                case 3 -> {
                    workState = STATE_UPWARD;
                    return true;
                }
                case 1 -> {
                    workState = STATE_AT_BOTTOM;
                    return true;
                }
            }
            // new layer - fill again
            fillMineListIfEmpty(xDrill, yDrill, zDrill, xPipe, zPipe, yHead);
        }
        return tryProcessOreList();
    }

    private boolean tryProcessOreList() {
        // Even though it works fine without this check,
        // it can save tiny amount of CPU time when void protection is disabled
        if (protectsExcessItem()) {
            boolean simulateResult = processOreList(true);
            if (!simulateResult) {
                mEUt = 0;
                mMaxProgresstime = 0;
                return false;
            }
        }

        boolean result = processOreList(false);
        if (!result) {
            mEUt = 0;
            mMaxProgresstime = 0;
            return false;
        }
        return true;
    }

    private boolean processOreList(boolean simulate) {
        ChunkPosition oreBlockPos = null;
        List<ChunkPosition> oreBlockPositions = simulate ? copyOreBlockPositions(this.oreBlockPositions)
            : this.oreBlockPositions;
        int x = 0, y = 0, z = 0;
        Block oreBlock = null;
        int oreBlockMetadata = 0;

        while ((oreBlock == null || !GTUtility.isOre(oreBlock, oreBlockMetadata)) && !oreBlockPositions.isEmpty()) {
            oreBlockPos = oreBlockPositions.remove(0);
            x = oreBlockPos.chunkPosX;
            y = oreBlockPos.chunkPosY;
            z = oreBlockPos.chunkPosZ;
            if (GTUtility.eraseBlockByFakePlayer(getFakePlayer(getBaseMetaTileEntity()), x, y, z, true))
                oreBlock = getBaseMetaTileEntity().getBlock(x, y, z);
            oreBlockMetadata = getBaseMetaTileEntity().getWorld()
                .getBlockMetadata(x, y, z);
        }

        if (!tryConsumeDrillingFluid(simulate)) {
            oreBlockPositions.add(0, oreBlockPos);
            setRuntimeFailureReason(CheckRecipeResultRegistry.NO_DRILLING_FLUID);
            return false;
        }
        if (oreBlock != null && GTUtility.isOre(oreBlock, oreBlockMetadata)) {
            short metaData = 0;
            TileEntity tTileEntity = getBaseMetaTileEntity().getTileEntity(x, y, z);
            if (tTileEntity instanceof TileEntityOres) {
                metaData = ((TileEntityOres) tTileEntity).mMetaData;
            }

            Collection<ItemStack> oreBlockDrops = getBlockDrops(oreBlock, x, y, z);
            ItemStack cobble = GTUtility.getCobbleForOre(oreBlock, metaData);
            if (!simulate) {
                if (replaceWithCobblestone) {
                    getBaseMetaTileEntity().getWorld()
                        .setBlock(x, y, z, Block.getBlockFromItem(cobble.getItem()), cobble.getItemDamage(), 3);
                } else {
                    getBaseMetaTileEntity().getWorld()
                        .setBlockToAir(oreBlockPos.chunkPosX, oreBlockPos.chunkPosY, oreBlockPos.chunkPosZ);
                }
            }
            ItemStack[] toOutput = getOutputByDrops(oreBlockDrops);
            if (simulate && !canOutputAll(toOutput)) {
                setRuntimeFailureReason(CheckRecipeResultRegistry.ITEM_OUTPUT_FULL);
                return false;
            }
            mOutputItems = toOutput;
        }
        return true;
    }

    private static List<ChunkPosition> copyOreBlockPositions(List<ChunkPosition> oreBlockPositions) {
        List<ChunkPosition> ret = new ArrayList<>();
        for (ChunkPosition chunkPosition : oreBlockPositions) {
            ret.add(new ChunkPosition(chunkPosition.chunkPosX, chunkPosition.chunkPosY, chunkPosition.chunkPosZ));
        }
        return ret;
    }

    @Override
    protected boolean workingAtBottom(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe,
        int yHead, int oldYHead) {
        if (!mChunkLoadingEnabled)
            return super.workingAtBottom(aStack, xDrill, yDrill, zDrill, xPipe, zPipe, yHead, oldYHead);

        if (mCurrentChunk == null) {
            createInitialWorkingChunk();
            return true;
        }

        if (mWorkChunkNeedsReload) {
            GTChunkManager.requestChunkLoad((TileEntity) getBaseMetaTileEntity(), mCurrentChunk);
            mWorkChunkNeedsReload = false;
            return true;
        }
        if (oreBlockPositions.isEmpty()) {
            fillChunkMineList(yHead, yDrill);
            if (oreBlockPositions.isEmpty()) {
                GTChunkManager.releaseChunk((TileEntity) getBaseMetaTileEntity(), mCurrentChunk);
                if (!moveToNextChunk(xDrill >> 4, zDrill >> 4)) {
                    workState = STATE_UPWARD;
                    updateVeinNameFromVP();
                }
                return true;
            }
        }
        return tryProcessOreList();
    }

    private void createInitialWorkingChunk() {
        mCurrentChunk = getTopLeftChunkCoords();
        updateVeinNameFromVP();
        if (mChunkLoadingEnabled) {
            GTChunkManager.requestChunkLoad((TileEntity) getBaseMetaTileEntity(), mCurrentChunk);
            mWorkChunkNeedsReload = false;
        }
    }

    @NotNull
    private ChunkCoordIntPair getTopLeftChunkCoords() {
        return getCornerCoords(-1, -1);
    }

    @NotNull
    private ChunkCoordIntPair getBottomRightChunkCoords() {
        return getCornerCoords(1, 1);
    }

    @NotNull
    private ChunkCoordIntPair getCornerCoords(int xMultiplier, int zMultiplier) {
        final ChunkCoordIntPair drillPos = getDrillCoords();
        // use corner closest to the drill as mining area center
        return new ChunkCoordIntPair(
            drillPos.chunkXPos + xMultiplier * chunkRadiusConfig
                + ((getXDrill() - (drillPos.chunkXPos << 4)) < 8 ? 0 : 1),
            drillPos.chunkZPos + zMultiplier * chunkRadiusConfig
                + ((getZDrill() - (drillPos.chunkZPos << 4)) < 8 ? 0 : 1));
    }

    @NotNull
    private ChunkCoordIntPair getDrillCoords() {
        return new ChunkCoordIntPair(getXDrill() >> 4, getZDrill() >> 4);
    }

    private int getTotalChunkCount() {
        final ChunkCoordIntPair topLeft = getTopLeftChunkCoords();
        final ChunkCoordIntPair bottomRight = getBottomRightChunkCoords();
        return (bottomRight.chunkXPos - topLeft.chunkXPos) * (bottomRight.chunkZPos - topLeft.chunkZPos);
    }

    /**
     * Returns a number corresponding to which chunk the drill is operating on. Only really useful for driving outputs
     * in the controller UI.
     *
     * @return 0 if the miner is not in operation, positive integer corresponding to the chunk currently being drilled
     */
    @SuppressWarnings("ExtractMethodRecommender")
    private int getChunkNumber() {
        if (mCurrentChunk == null) {
            return 0;
        }

        final ChunkCoordIntPair topLeft = getTopLeftChunkCoords();
        final ChunkCoordIntPair drillPos = getDrillCoords();

        if (workState == STATE_DOWNWARD) {
            return 1;
        } else if (workState == STATE_UPWARD) {
            // Technically, the miner isn't mining anything now; it's retracting the pipes in preparation to end
            // operation.
            return 0;
        }

        int chunkNumber = (chunkRadiusConfig * 2) * (mCurrentChunk.chunkZPos - topLeft.chunkZPos)
            + mCurrentChunk.chunkXPos
            - topLeft.chunkXPos
            + 1;

        // Drills mine the chunk they're in first, so if we're not there yet, bump the number to indicate that it
        // was already mined.
        if (mCurrentChunk.chunkZPos < drillPos.chunkZPos
            || (mCurrentChunk.chunkZPos == drillPos.chunkZPos && mCurrentChunk.chunkXPos < drillPos.chunkXPos)) {
            chunkNumber += 1;
        }
        return chunkNumber;
    }

    @Override
    protected boolean workingUpward(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe,
        int yHead, int oldYHead) {
        boolean result;
        if (!mChunkLoadingEnabled || oreBlockPositions.isEmpty()) {
            result = super.workingUpward(aStack, xDrill, yDrill, zDrill, xPipe, zPipe, yHead, oldYHead);
        } else {
            result = tryProcessOreList();
            if (oreBlockPositions.isEmpty()) GTChunkManager.releaseTicket((TileEntity) getBaseMetaTileEntity());
        }

        if (!result) {
            setShutdownReason(StatCollector.translateToLocal("GT5U.gui.text.drill_exhausted"));
        }

        return result;
    }

    @Override
    protected void onAbort() {
        oreBlockPositions.clear();
        if (mCurrentChunk != null) {
            GTChunkManager.releaseChunk((TileEntity) getBaseMetaTileEntity(), mCurrentChunk);
        }
        mCurrentChunk = null;
        updateVeinNameFromVP();
    }

    private boolean moveToNextChunk(int centerX, int centerZ) {
        if (mCurrentChunk == null) return false;
        // use corner closest to the drill as mining area center
        final int left = centerX - chunkRadiusConfig + ((getXDrill() - (centerX << 4)) < 8 ? 0 : 1);
        final int right = left + chunkRadiusConfig * 2;
        final int bottom = centerZ + chunkRadiusConfig + ((getZDrill() - (centerZ << 4)) < 8 ? 0 : 1);

        int nextChunkX = mCurrentChunk.chunkXPos + 1;
        int nextChunkZ = mCurrentChunk.chunkZPos;

        // step to the next chunk
        if (nextChunkX >= right) {
            nextChunkX = left;
            ++nextChunkZ;
        }
        // skip center chunk - dug in workingDownward()
        if (nextChunkX == centerX && nextChunkZ == centerZ) {
            ++nextChunkX;

            if (nextChunkX >= right) {
                nextChunkX = left;
                ++nextChunkZ;
            }
        }

        if (nextChunkZ >= bottom) {
            mCurrentChunk = null;
            return false;
        }

        mCurrentChunk = new ChunkCoordIntPair(nextChunkX, nextChunkZ);
        updateVeinNameFromVP();

        GTChunkManager
            .requestChunkLoad((TileEntity) getBaseMetaTileEntity(), new ChunkCoordIntPair(nextChunkX, nextChunkZ));
        return true;
    }

    private void updateVeinNameFromVP() {
        updateVeinNameFromVP(mCurrentChunk);
    }

    private void updateVeinNameFromVP(@NotNull ChunkCoordIntPair coords) {
        veinName = VisualProspectingDatabase
            .getVeinName(getBaseMetaTileEntity().getWorld().provider.dimensionId, coords)
            .orElse(null);
    }

    @Override
    protected boolean checkHatches() {
        return !mMaintenanceHatches.isEmpty() && !mInputHatches.isEmpty()
            && !mOutputBusses.isEmpty()
            && !mEnergyHatches.isEmpty();
    }

    @Override
    protected List<IHatchElement<? super MTEDrillerBase>> getAllowedHatches() {
        return ImmutableList.of(InputHatch, InputBus, OutputBus, Maintenance, Energy);
    }

    @Override
    protected void setElectricityStats() {
        this.mEfficiency = getCurrentEfficiency(null);
        this.mEfficiencyIncrease = 10000;
        int tier = Math.max(1, GTUtility.getTier(getMaxInputVoltage()));
        this.mEUt = -3 * (1 << (tier << 1));
        this.mMaxProgresstime = calculateMaxProgressTime(tier);
    }

    @Override
    public int calculateMaxProgressTime(int tier, boolean simulateWorking) {
        return (int) Math.max(
            1,
            ((workState == STATE_DOWNWARD || workState == STATE_AT_BOTTOM || simulateWorking) ? getBaseProgressTime()
                : 80) / GTUtility.powInt(2, tier));
    }

    private ItemStack[] getOutputByDrops(Collection<ItemStack> oreBlockDrops) {
        long voltage = getMaxInputVoltage();
        Collection<ItemStack> outputItems = new HashSet<>();
        oreBlockDrops.forEach(currentItem -> {
            if (!doUseMaceratorRecipe(currentItem)) {
                outputItems.add(multiplyStackSize(currentItem));
                return;
            }
            GTRecipe tRecipe = RecipeMaps.maceratorRecipes.findRecipeQuery()
                .caching(false)
                .items(currentItem)
                .voltage(voltage)
                .find();
            if (tRecipe == null) {
                outputItems.add(currentItem);
                return;
            }
            for (int i = 0; i < tRecipe.mOutputs.length; i++) {
                ItemStack recipeOutput = tRecipe.mOutputs[i].copy();
                if (getBaseMetaTileEntity().getRandomNumber(10000) < tRecipe.getOutputChance(i))
                    multiplyStackSize(recipeOutput);
                outputItems.add(recipeOutput);
            }
        });
        return outputItems.toArray(new ItemStack[0]);
    }

    private boolean doUseMaceratorRecipe(ItemStack currentItem) {
        ItemData itemData = GTOreDictUnificator.getItemData(currentItem);
        return itemData == null || itemData.mPrefix != OrePrefixes.crushed && itemData.mPrefix != OrePrefixes.dustImpure
            && itemData.mPrefix != OrePrefixes.dust
            && itemData.mPrefix != OrePrefixes.gem
            && itemData.mPrefix != OrePrefixes.gemChipped
            && itemData.mPrefix != OrePrefixes.gemExquisite
            && itemData.mPrefix != OrePrefixes.gemFlawed
            && itemData.mPrefix != OrePrefixes.gemFlawless
            && itemData.mMaterial.mMaterial != Materials.Oilsands;
    }

    private ItemStack multiplyStackSize(ItemStack itemStack) {
        itemStack.stackSize *= getBaseMetaTileEntity().getRandomNumber(4) + 1;
        return itemStack;
    }

    private Collection<ItemStack> getBlockDrops(final Block oreBlock, int posX, int posY, int posZ) {
        final int blockMeta = getBaseMetaTileEntity().getMetaID(posX, posY, posZ);
        if (oreBlock.canSilkHarvest(getBaseMetaTileEntity().getWorld(), null, posX, posY, posZ, blockMeta)) {
            return Collections.singleton(new ItemStack(oreBlock, 1, blockMeta));
        }
        if (oreBlock instanceof BlockOresAbstract) {
            TileEntity tTileEntity = getBaseMetaTileEntity().getTileEntity(posX, posY, posZ);
            if (tTileEntity instanceof TileEntityOres tTileEntityOres) {
                if (tTileEntityOres.mMetaData >= 16000) {
                    // Small ore
                    return oreBlock
                        .getDrops(getBaseMetaTileEntity().getWorld(), posX, posY, posZ, blockMeta, mTier + 3);
                } else {
                    return tTileEntityOres.getSilkTouchDrops(oreBlock);
                }
            }
        }
        // Regular ore
        return oreBlock.getDrops(getBaseMetaTileEntity().getWorld(), posX, posY, posZ, blockMeta, 0);
    }

    private boolean tryConsumeDrillingFluid(boolean simulate) {
        return depleteInput(new FluidStack(ItemList.sDrillingFluid, 2000), simulate);
    }

    private void fillChunkMineList(int yHead, int yDrill) {
        if (mCurrentChunk == null || !oreBlockPositions.isEmpty()) return;
        final int minX = mCurrentChunk.chunkXPos << 4;
        final int maxX = minX + 16;
        final int minZ = mCurrentChunk.chunkZPos << 4;
        final int maxZ = minZ + 16;
        for (int x = minX; x < maxX; ++x)
            for (int z = minZ; z < maxZ; ++z) for (int y = yHead; y < yDrill; ++y) tryAddOreBlockToMineList(x, y, z);
    }

    private void fillMineListIfEmpty(int xDrill, int yDrill, int zDrill, int xPipe, int zPipe, int yHead) {
        if (!oreBlockPositions.isEmpty()) return;

        tryAddOreBlockToMineList(xPipe, yHead - 1, zPipe);
        if (yHead == yDrill) return; // skip controller block layer

        if (mChunkLoadingEnabled) {
            int startX = (xDrill >> 4) << 4;
            int startZ = (zDrill >> 4) << 4;
            for (int x = startX; x < (startX + 16); ++x)
                for (int z = startZ; z < (startZ + 16); ++z) tryAddOreBlockToMineList(x, yHead, z);
        } else {
            int radius = chunkRadiusConfig << 4;
            for (int xOff = -radius; xOff <= radius; xOff++) for (int zOff = -radius; zOff <= radius; zOff++)
                tryAddOreBlockToMineList(xDrill + xOff, yHead, zDrill + zOff);
        }
    }

    private void tryAddOreBlockToMineList(int x, int y, int z) {
        Block block = getBaseMetaTileEntity().getBlock(x, y, z);
        int blockMeta = getBaseMetaTileEntity().getMetaID(x, y, z);
        ChunkPosition blockPos = new ChunkPosition(x, y, z);
        if (!oreBlockPositions.contains(blockPos)) {
            if (block instanceof BlockOresAbstract) {
                TileEntity tTileEntity = getBaseMetaTileEntity().getTileEntity(x, y, z);
                if (tTileEntity instanceof TileEntityOres && ((TileEntityOres) tTileEntity).mNatural)
                    oreBlockPositions.add(blockPos);
            } else if (GTUtility.isOre(block, blockMeta)) oreBlockPositions.add(blockPos);
        }
    }

    protected abstract int getRadiusInChunks();

    protected abstract int getBaseProgressTime();

    protected MultiblockTooltipBuilder createTooltip(String tierSuffix) {
        String casings = getCasingBlockItem().get(0)
            .getDisplayName();

        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        final int baseCycleTime = calculateMaxProgressTime(getMinTier(), true);
        tt.addMachineType("Miner, MBM")
            .addInfo("Use a Screwdriver to configure block radius")
            .addInfo("Maximum radius is " + GTUtility.formatNumbers((long) getRadiusInChunks() << 4) + " blocks")
            .addInfo("Use Soldering iron to turn off chunk mode")
            .addInfo("Use Wire Cutter to toggle replacing mined blocks with cobblestone")
            .addInfo("In chunk mode, working area center is the chunk corner nearest to the drill")
            .addInfo("Gives ~3x as much crushed ore vs normal processing")
            .addInfo("Fortune bonus of " + GTUtility.formatNumbers(mTier + 3) + ". Only works on small ores")
            .addInfo("Minimum energy hatch tier: " + GTUtility.getColoredTierNameFromTier((byte) getMinTier()))
            .addInfo(
                "Base cycle time: " + (baseCycleTime < 20 ? GTUtility.formatNumbers(baseCycleTime) + " ticks"
                    : GTUtility.formatNumbers(baseCycleTime / 20.0) + " seconds"))
            .beginStructureBlock(3, 7, 3, false)
            .addController("Front bottom")
            .addOtherStructurePart(casings, "form the 3x1x3 Base")
            .addOtherStructurePart(casings, "1x3x1 pillar above the center of the base (2 minimum total)")
            .addOtherStructurePart(getFrameMaterial().mName + " Frame Boxes", "Each pillar's side and 1x3x1 on top")
            .addEnergyHatch(VN[getMinTier()] + "+, Any base casing", 1)
            .addMaintenanceHatch("Any base casing", 1)
            .addInputBus("Mining Pipes, optional, any base casing", 1)
            .addInputHatch("Drilling Fluid, any base casing", 1)
            .addOutputBus("Any base casing", 1)
            .toolTipFinisher();
        return tt;
    }

    protected static final NumberFormatMUI numberFormat = new NumberFormatMUI();

    @Override
    protected void drawTexts(DynamicPositionedColumn screenElements, SlotWidget inventorySlot) {
        super.drawTexts(screenElements, inventorySlot);
        screenElements
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                            "GT5U.gui.text.drill_ores_left_chunk",
                            numberFormat.format(clientOreListSize)))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled(
                        widget -> getBaseMetaTileEntity().isActive() && clientOreListSize > 0
                            && workState == STATE_AT_BOTTOM))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                            "GT5U.gui.text.drill_ores_left_layer",
                            numberFormat.format(clientYHead),
                            numberFormat.format(clientOreListSize)))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled(
                        widget -> getBaseMetaTileEntity().isActive() && clientYHead > 0 && workState == STATE_DOWNWARD))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                            "GT5U.gui.text.drill_chunks_left",
                            numberFormat.format(clientCurrentChunk),
                            numberFormat.format(clientTotalChunks)))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled(
                        widget -> getBaseMetaTileEntity().isActive() && clientCurrentChunk > 0
                            && workState == STATE_AT_BOTTOM))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> EnumChatFormatting.GRAY
                            + StatCollector.translateToLocalFormatted("GT5U.gui.text.drill_current_vein", veinName))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled(
                        widget -> veinName != null && (workState == STATE_AT_BOTTOM || workState == STATE_DOWNWARD)))
            .widget(new FakeSyncWidget.IntegerSyncer(oreBlockPositions::size, (newInt) -> clientOreListSize = newInt))
            .widget(new FakeSyncWidget.IntegerSyncer(this::getTotalChunkCount, (newInt) -> clientTotalChunks = newInt))
            .widget(new FakeSyncWidget.IntegerSyncer(this::getChunkNumber, (newInt) -> clientCurrentChunk = newInt))
            .widget(new FakeSyncWidget.IntegerSyncer(() -> workState, (newInt) -> workState = newInt))
            .widget(new FakeSyncWidget.IntegerSyncer(this::getYHead, (newInt) -> clientYHead = newInt))
            .widget(new FakeSyncWidget.StringSyncer(() -> veinName, (newString) -> veinName = newString));
    }

    @Override
    protected List<ButtonWidget> getAdditionalButtons(ModularWindow.Builder builder, UIBuildContext buildContext) {
        return ImmutableList.of(
            (ButtonWidget) new LockedWhileActiveButton(this.getBaseMetaTileEntity(), builder)
                .setOnClick((clickData, widget) -> adjustChunkRadius(clickData.mouseButton == 0))
                .setPlayClickSound(true)
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_WORK_AREA)
                .attachSyncer(
                    new FakeSyncWidget.IntegerSyncer(() -> chunkRadiusConfig, (val) -> chunkRadiusConfig = val),
                    builder,
                    (widget, val) -> widget.notifyTooltipChange())
                .dynamicTooltip(
                    () -> ImmutableList.of(
                        StatCollector.translateToLocalFormatted(
                            "GT5U.gui.button.ore_drill_radius_1",
                            GTUtility.formatNumbers((long) chunkRadiusConfig << 4)),
                        StatCollector.translateToLocal("GT5U.gui.button.ore_drill_radius_2")))
                .setTooltipShowUpDelay(TOOLTIP_DELAY)
                .setSize(16, 16),
            (ButtonWidget) new LockedWhileActiveButton(this.getBaseMetaTileEntity(), builder)
                .setOnClick((clickData, widget) -> replaceWithCobblestone = !replaceWithCobblestone)
                .setPlayClickSound(true)
                .setBackground(() -> {
                    if (replaceWithCobblestone) {
                        return new IDrawable[] { GTUITextures.BUTTON_STANDARD_PRESSED,
                            GTUITextures.OVERLAY_BUTTON_REPLACE_COBBLE_ON };
                    }
                    return new IDrawable[] { GTUITextures.BUTTON_STANDARD,
                        GTUITextures.OVERLAY_BUTTON_REPLACE_COBBLE_OFF };
                })
                .attachSyncer(
                    new FakeSyncWidget.BooleanSyncer(
                        () -> replaceWithCobblestone,
                        (val) -> replaceWithCobblestone = val),
                    builder,
                    (widget, val) -> widget.notifyTooltipChange())
                .dynamicTooltip(
                    () -> ImmutableList.of(
                        StatCollector.translateToLocal(
                            replaceWithCobblestone ? "GT5U.gui.button.ore_drill_cobblestone_on"
                                : "GT5U.gui.button.ore_drill_cobblestone_off")))
                .setTooltipShowUpDelay(TOOLTIP_DELAY)
                .setSize(16, 16));
    }

    @Override
    protected SoundResource getProcessStartSound() {
        return SoundResource.IC2_MACHINES_MINER_OP;
    }

    @Override
    public String[] getInfoData() {
        final String diameter = GTUtility.formatNumbers(chunkRadiusConfig * 2L);
        return new String[] {
            EnumChatFormatting.BLUE + StatCollector.translateToLocal("GT5U.machines.minermulti")
                + EnumChatFormatting.RESET,
            StatCollector.translateToLocal("GT5U.machines.workarea") + ": "
                + EnumChatFormatting.GREEN
                + diameter
                + "x"
                + diameter
                + EnumChatFormatting.RESET
                + " "
                + StatCollector.translateToLocal("GT5U.machines.chunks") };
    }

    @Override
    public @NotNull List<String> reportMetrics() {
        if (getBaseMetaTileEntity().isActive()) {
            return switch (workState) {
                case STATE_AT_BOTTOM -> ImmutableList.of(
                    StatCollector.translateToLocalFormatted(
                        "GT5U.gui.text.drill_ores_left_chunk",
                        GTUtility.formatNumbers(oreBlockPositions.size())),
                    StatCollector.translateToLocalFormatted(
                        "GT5U.gui.text.drill_chunks_left",
                        GTUtility.formatNumbers(getChunkNumber()),
                        GTUtility.formatNumbers(getTotalChunkCount())),
                    veinName == null ? ""
                        : StatCollector.translateToLocalFormatted("GT5U.gui.text.drill_current_vein", veinName));
                case STATE_DOWNWARD -> ImmutableList.of(
                    StatCollector.translateToLocalFormatted(
                        "GT5U.gui.text.drill_ores_left_layer",
                        getYHead(),
                        GTUtility.formatNumbers(oreBlockPositions.size())),
                    veinName == null ? ""
                        : StatCollector.translateToLocalFormatted("GT5U.gui.text.drill_current_vein", veinName));
                case STATE_UPWARD, STATE_ABORT -> ImmutableList
                    .of(StatCollector.translateToLocal("GT5U.gui.text.retracting_pipe"));

                default -> ImmutableList.of();
            };
        }

        return ImmutableList.of(
            getFailureReason()
                .map(reason -> StatCollector.translateToLocalFormatted("GT5U.gui.text.drill_offline_reason", reason))
                .orElseGet(() -> StatCollector.translateToLocalFormatted("GT5U.gui.text.drill_offline_generic")));
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }
}
