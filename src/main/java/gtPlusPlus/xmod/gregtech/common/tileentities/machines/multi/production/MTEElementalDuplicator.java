package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.enums.TAE;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.pollution.PollutionConfig;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchElementalDataOrbHolder;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GTPPMultiBlockBase;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;

public class MTEElementalDuplicator extends GTPPMultiBlockBase<MTEElementalDuplicator>
    implements ISurvivalConstructable {

    private final ArrayList<MTEHatchElementalDataOrbHolder> mReplicatorDataOrbHatches = new ArrayList<>();
    private static final int CASING_TEXTURE_ID = TAE.getIndexFromPage(0, 3);
    private int mCasing = 0;

    public MTEElementalDuplicator(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEElementalDuplicator(final String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(final IGregTechTileEntity aTileEntity) {
        return new MTEElementalDuplicator(this.mName);
    }

    @Override
    public String getMachineType() {
        return "Replicator";
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {

        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(getMachineType())
            .addInfo("Produces Elemental Material from UU Matter")
            .addInfo("Speed: +100% | EU Usage: 100% | Parallel: 8 * Tier")
            .addInfo("Maximum 1x of each bus/hatch.")
            .addInfo("Requires circuit 1-16 in your Data Orb Repository")
            .addInfo("depending on what Data Orb you want to prioritize")
            .addPerfectOCInfo()
            .addPollutionAmount(getPollutionPerSecond(null))
            .beginStructureBlock(9, 6, 9, true)
            .addController("Top Center")
            .addCasingInfoMin("Elemental Confinement Shell", 120, false)
            .addCasingInfoMin("Matter Fabricator Casing", 24, false)
            .addCasingInfoMin("Particle Containment Casing", 24, false)
            .addCasingInfoMin("Matter Generation Coil", 24, false)
            .addCasingInfoMin("High Voltage Current Capacitor", 20, false)
            .addCasingInfoMin("Resonance Chamber III", 24, false)
            .addCasingInfoMin("Modulator III", 16, false)
            .addOtherStructurePart(
                StatCollector.translateToLocal("GTPP.tooltip.structure.data_orb_repository"),
                "1x",
                1)
            .addInputHatch("Any 1 dot hint", 1)
            .addOutputBus("Any 1 dot hint", 1)
            .addOutputHatch("Any 1 dot hint", 1)
            .addEnergyHatch("Any 1 dot hint", 1)
            .addMaintenanceHatch("Any 1 dot hint", 1)
            .addMufflerHatch("Any 1 dot hint", 1)
            .toolTipFinisher();
        return tt;
    }

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static IStructureDefinition<MTEElementalDuplicator> STRUCTURE_DEFINITION = null;

    @Override
    public IStructureDefinition<MTEElementalDuplicator> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<MTEElementalDuplicator>builder()

                // h = Hatch
                // c = Casing

                // a = MF Casing 1
                // b = Matter Gen Coil

                // d = Current Capacitor
                // e = Particle

                // f = Resonance III
                // g = Modulator III

                .addShape(
                    STRUCTURE_PIECE_MAIN,
                    (new String[][] {
                        { "   ccc   ", "  ccccc  ", " ccccccc ", "ccccccccc", "cccc~cccc", "ccccccccc", " ccccccc ",
                            "  ccccc  ", "   ccc   " },
                        { "   cac   ", "  abfba  ", " abfgfba ", "cbfgdgfbc", "afgdddgfa", "cbfgdgfbc", " abfgfba ",
                            "  abfba  ", "   cac   " },
                        { "   cec   ", "  e   e  ", " e     e ", "c   d   c", "e  ddd  e", "c   d   c", " e     e ",
                            "  e   e  ", "   cec   " },
                        { "   cec   ", "  e   e  ", " e     e ", "c   d   c", "e  ddd  e", "c   d   c", " e     e ",
                            "  e   e  ", "   cec   " },
                        { "   cac   ", "  abfba  ", " abfgfba ", "cbfgdgfbc", "afgdddgfa", "cbfgdgfbc", " abfgfba ",
                            "  abfba  ", "   cac   " },
                        { "   ccc   ", "  ccccc  ", " ccccccc ", "ccccccccc", "ccccccccc", "ccccccccc", " ccccccc ",
                            "  ccccc  ", "   ccc   " }, }))
                .addElement('a', ofBlock(getCasingBlock4(), getCasingMeta6()))
                .addElement('b', ofBlock(getCasingBlock4(), getCasingMeta7()))
                .addElement('d', ofBlock(getCasingBlock2(), getCasingMeta2()))
                .addElement('e', ofBlock(getCasingBlock2(), getCasingMeta3()))
                .addElement('f', ofBlock(getCasingBlock3(), getCasingMeta4()))
                .addElement('g', ofBlock(getCasingBlock3(), getCasingMeta5()))
                .addElement(
                    'c',
                    lazy(
                        t -> ofChain(
                            buildHatchAdder(MTEElementalDuplicator.class)
                                .atLeast(InputHatch, OutputBus, OutputHatch, Maintenance, Muffler, Energy)
                                .casingIndex(getCasingTextureIndex())
                                .dot(1)
                                .build(),
                            buildHatchAdder(MTEElementalDuplicator.class)
                                .hatchClass(MTEHatchElementalDataOrbHolder.class)
                                .shouldReject(x -> !x.mReplicatorDataOrbHatches.isEmpty())
                                .adder(MTEElementalDuplicator::addDataOrbHatch)
                                .casingIndex(getCasingTextureIndex())
                                .dot(1)
                                .build(),
                            onElementPass(x -> ++x.mCasing, ofBlock(getCasingBlock(), getCasingMeta())))))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 4, 4, 0);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasing = 0;
        boolean aDidBuild = checkPiece(STRUCTURE_PIECE_MAIN, 4, 4, 0);
        if (this.mReplicatorDataOrbHatches.size() != 1) {
            return false;
        }
        log("Casings: " + mCasing);
        return aDidBuild && mCasing >= 120 && checkHatch();
    }

    @Override
    public int survivalConstruct(ItemStack itemStack, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(STRUCTURE_PIECE_MAIN, itemStack, 4, 4, 0, elementBudget, env, false, true);
    }

    protected static int getCasingTextureIndex() {
        return CASING_TEXTURE_ID;
    }

    protected static Block getCasingBlock() {
        return ModBlocks.blockCasings5Misc;
    }

    protected static Block getCasingBlock2() {
        return ModBlocks.blockSpecialMultiCasings;
    }

    protected static Block getCasingBlock3() {
        return ModBlocks.blockSpecialMultiCasings2;
    }

    protected static Block getCasingBlock4() {
        return ModBlocks.blockCasingsMisc;
    }

    protected static int getCasingMeta() {
        return 3;
    }

    protected static int getCasingMeta2() {
        return 12;
    }

    protected static int getCasingMeta3() {
        return 13;
    }

    protected static int getCasingMeta4() {
        return 2;
    }

    protected static int getCasingMeta5() {
        return 6;
    }

    protected static int getCasingMeta6() {
        return 9;
    }

    protected static int getCasingMeta7() {
        return 8;
    }

    private boolean addDataOrbHatch(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        } else {
            IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity == null) {
                return false;
            }
            if (aMetaTileEntity instanceof MTEHatchElementalDataOrbHolder) {
                try {
                    return addToMachineListInternal(mReplicatorDataOrbHatches, aMetaTileEntity, aBaseCasingIndex);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d == ForgeDirection.UP;
    }

    @Override
    protected IIconContainer getActiveOverlay() {
        return TexturesGtBlock.oMCAElementalDuplicatorActive;
    }

    @Override
    protected IIconContainer getActiveGlowOverlay() {
        return TexturesGtBlock.oMCAElementalDuplicatorActiveGlow;
    }

    @Override
    protected IIconContainer getInactiveOverlay() {
        return TexturesGtBlock.oMCAElementalDuplicator;
    }

    @Override
    protected IIconContainer getInactiveGlowOverlay() {
        return TexturesGtBlock.oMCAElementalDuplicatorGlow;
    }

    @Override
    protected int getCasingTextureId() {
        return CASING_TEXTURE_ID;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.replicatorRecipes;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().setSpeedBonus(1F / 2F)
            .enablePerfectOverclock()
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        for (MTEHatchElementalDataOrbHolder hatch : validMTEList(mReplicatorDataOrbHatches)) {
            ItemStack orb = hatch.getOrbByCircuit();
            logic.setSpecialSlotItem(orb);
            break;
        }
    }

    @Override
    public int getMaxParallelRecipes() {
        return (8 * GTUtility.getTier(this.getMaxInputVoltage()));
    }

    @Override
    public int getPollutionPerSecond(final ItemStack aStack) {
        return PollutionConfig.pollutionPerSecondElementalDuplicator;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide()) {
            if (this.mUpdate == 1 || this.mStartUpCheck == 1) {
                this.mReplicatorDataOrbHatches.clear();
            }
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
    }

    @Override
    public ArrayList<ItemStack> getStoredInputsForColor(Optional<Byte> color) {
        ArrayList<ItemStack> tItems = super.getStoredInputsForColor(Optional.empty());
        for (MTEHatchElementalDataOrbHolder tHatch : validMTEList(mReplicatorDataOrbHatches)) {
            byte busColor = tHatch.getBaseMetaTileEntity()
                .getColorization();
            if (color.isPresent() && busColor != -1 && busColor != color.get()) continue;
            tItems.add(tHatch.getOrbByCircuit());
        }
        tItems.removeAll(Collections.singleton(null));
        return tItems;
    }
}
