package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static goodgenerator.loader.Loaders.supercriticalFluidTurbineCasing;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofFrame;
import static gregtech.api.util.GTUtility.validMTEList;
import static net.minecraft.util.EnumChatFormatting.BOLD;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.utils.item.LimitingItemStackHandler;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import fox.spiteful.avaritia.blocks.LudicrousBlocks;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.enums.MaterialsUEVplus;
import gregtech.api.enums.Mods;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.IToolStats;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.recipe.metadata.CentrifugeRecipeKey;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTDataUtils;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.blocks.BlockCasings12;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.misc.GTStructureChannels;
import gregtech.common.tileentities.machines.multi.gui.MTEChamberCentrifugeGui;
import gregtech.common.tools.ToolTurbineHuge;
import gregtech.common.tools.ToolTurbineLarge;
import gregtech.common.tools.ToolTurbineNormal;
import gregtech.common.tools.ToolTurbineSmall;
import gtPlusPlus.api.recipe.GTPPRecipeMaps;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchTurbine;

public class MTEChamberCentrifuge extends MTEExtendedPowerMultiBlockBase<MTEChamberCentrifuge>
    implements ISurvivalConstructable {

    public boolean tier2Fluid = false;
    public double mMode = 1.0; // i think it has to be a double cuz slider. 0 = speed, 1 = normal, 2 = heavy
    public int RP = 0;
    public float speed = 3F;
    private final int horizontalOffset = 8; // base offset for tier 1
    private final int verticalOffset = 8; // base offset for tier 2
    private final int depthOffset = 2;
    private int amountToDrain = 1; // drain amount.
    private int mTier;
    private static FluidStack kerosene100;
    private static FluidStack kerosene10;
    public final LimitingItemStackHandler turbineHolder = new LimitingItemStackHandler(8, 1);
    private static final String STRUCTURE_TIER_1 = "t1";
    private static final String STRUCTURE_TIER_2 = "t2";
    private static final String STRUCTURE_TIER_3 = "t3";
    private static final String STRUCTURE_TIER_4 = "t4";
    private static final IIconContainer TEXTURE_CONTROLLER = new Textures.BlockIcons.CustomIcon("iconsets/TFFT");
    private static final IIconContainer TEXTURE_CONTROLLER_ACTIVE = new Textures.BlockIcons.CustomIcon(
        "iconsets/TFFT_ACTIVE");
    private static final IIconContainer TEXTURE_CONTROLLER_ACTIVE_GLOW = new Textures.BlockIcons.CustomIcon(
        "iconsets/TFFT_ACTIVE_GLOW");
    public ArrayList<MTEHatchTurbine> mTurbineRotorHatches = new ArrayList<>();

    private boolean mStaticAnimations = false;
    // spotless:off
    private static final IStructureDefinition<MTEChamberCentrifuge> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEChamberCentrifuge>builder()
        .addShape(
            STRUCTURE_TIER_1,
            transpose(
                new String[][] {
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H    B    H   ", "  H    BEB    H  ", "  H   BEEEB   H  ", "  H  BEEEEEB  H  ", "  H   BEEEB   H  ", "  H    BEB    H  ", "   H    B    H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H    B    H   ", "  H    BBB    H  ", "  I   BEBEB   I  ", "  I  BBBEBBB  I  ", "  I   BEBEB   I  ", "  H    BBB    H  ", "   H    B    H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   CCC   H   ", "  H   CCCCC   H  ", "  I  CCEEECC  I  ", "  I  CCEEECC  I  ", "  I  CCEEECC  I  ", "  H   CCCCC   H  ", "   H   CCC   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HH~HH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  A  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H   CCC   H   ", "  H   CCCCC   H  ", "  I  CCEEECC  I  ", "  I  CCEEECC  I  ", "  I  CCEEECC  I  ", "  H   CCCCC   H  ", "   H   CCC   H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "       GJG       ", "      HG GH      ", "    BH G G HB    ", "   B   GGG   B   ", "   H    F    H   ", "  H    BFB    H  ", "  I   BEFEB   I  ", "  I  BBBEBBB  I  ", "  I   BEFEB   I  ", "  H    BFB    H  ", "   H    F    H   ", "   B   GGG   B   ", "    BH G G HB    ", "      HG GH      ", "       GJG       ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H    B    H   ", "  H    BEB    H  ", "  H   BEEEB   H  ", "  H  BEEEEEB  H  ", "  H   BEEEB   H  ", "  H    BEB    H  ", "   H    B    H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " } }))
        .addShape(
            STRUCTURE_TIER_2,
            transpose(
                new String[][] {
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H    B    H   ", "  H    BEB    H  ", "  H   BEEEB   H  ", "  H  BEEEEEB  H  ", "  H   BEEEB   H  ", "  H    BEB    H  ", "   H    B    H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H    B    H   ", "  H    BBB    H  ", "  I   BEBEB   I  ", "  I  BBBEBBB  I  ", "  I   BEBEB   I  ", "  H    BBB    H  ", "   H    B    H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   KKK   H   ", "  H   KKKKK   H  ", "  I  KKEEEKK  I  ", "  I  KKEEEKK  I  ", "  I  KKEEEKK  I  ", "  H   KKKKK   H  ", "   H   KKK   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HH~HH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  L  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  H  D     D  H  ", "  H  D  L  D  H  ", "  H  D     D  H  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H   KKK   H   ", "  H   KKKKK   H  ", " GGGGKKEEEKKGGGG ", " GGGGKKEEEKKGGGG ", " GGGGKKEEEKKGGGG ", "  H   KKKKK   H  ", "   H   KKK   H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "       GJG       ", "      HG GH      ", "    BH G G HB    ", "   B   GGG   B   ", "   H    F    H   ", "  H    BFB    H  ", " GGGG BEFEB GGGG ", " J  GFFFEFFFG  J ", " GGGG BEFEB GGGG ", "  H    BFB    H  ", "   H    F    H   ", "   B   GGG   B   ", "    BH G G HB    ", "      HG GH      ", "       GJG       ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H    B    H   ", "  H    BEB    H  ", " GGGG BEEEB GGGG ", " GGGGBEEEEEBGGGG ", " GGGG BEEEB GGGG ", "  H    BEB    H  ", "   H    B    H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " } }))
        .addShape(
            STRUCTURE_TIER_3,
            transpose(
                new String[][] {
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H    F    H   ", "  H    BEB    H  ", "  H   BEEEB   H  ", "  H  BEEEEEB  H  ", "  H   BEEEB   H  ", "  H    BEB    H  ", "   H    B    H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "       GJG       ", "      HG GH      ", "    BH G G HB    ", "   B   GGG   B   ", "   H    B    H   ", "  H    BFB    H  ", "  I   BEBEB   I  ", "  I  BBBEBBB  I  ", "  I   BEBEB   I  ", "  H    BBB    H  ", "   H    B    H   ", "   B   GGG   B   ", "    BH G G HB    ", "      HG GH      ", "       GJG       ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H   MMM   H   ", "  H   MMMMM   H  ", "  I  MMEEEMM  I  ", "  I  MMEEEMM  I  ", "  I  MMEEEMM  I  ", "  H   MMMMM   H  ", "   H   MMM   H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HH~HH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  N  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  H  D     D  H  ", "  H  D  N  D  H  ", "  H  D     D  H  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H   MMM   H   ", "  H   MMMMM   H  ", " GGGGMMEEEMMGGGG ", " GGGGMMEEEMMGGGG ", " GGGGMMEEEMMGGGG ", "  H   MMMMM   H  ", "   H   MMM   H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "       GJG       ", "      HG GH      ", "    BH G G HB    ", "   B   GGG   B   ", "   H    F    H   ", "  H    BFB    H  ", " GGGG BEFEB GGGG ", " J  GFFFEFFFG  J ", " GGGG BEFEB GGGG ", "  H    BFB    H  ", "   H    F    H   ", "   B   GGG   B   ", "    BH G G HB    ", "      HG GH      ", "       GJG       ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H    B    H   ", "  H    BEB    H  ", " GGGG BEEEB GGGG ", " GGGGBEEEEEBGGGG ", " GGGG BEEEB GGGG ", "  H    BEB    H  ", "   H    B    H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " } }))
        .addShape(
            STRUCTURE_TIER_4,
            transpose(
                new String[][] {
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H    B    H   ", "  H    BEB    H  ", " GGGG BEEEB GGGG ", " GGGGBEEEEEBGGGG ", " GGGG BEEEB GGGG ", "  H    BEB    H  ", "   H    B    H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "       GJG       ", "      HG GH      ", "    BH G G HB    ", "   B   GGG   B   ", "   H    F    H   ", "  H    BFB    H  ", " GGGG BEFEB GGGG ", " J  GFFFEFFFG  J ", " GGGG BEFEB GGGG ", "  H    BFB    H  ", "   H    F    H   ", "   B   GGG   B   ", "    BH G G HB    ", "      HG GH      ", "       GJG       ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H   OOO   H   ", "  H   OOOOO   H  ", " GGGGOOEEEOOGGGG ", " GGGGOOEEEOOGGGG ", " GGGGOOEEEOOGGGG ", "  H   OOOOO   H  ", "   H   OOO   H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  H  D     D  H  ", "  H  D  P  D  H  ", "  H  D     D  H  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  P  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  P  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  P  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HH~HH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  P  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  P  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  P  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HIIIH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  I  D     D  I  ", "  I  D  P  D  I  ", "  I  D     D  I  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HIIIH      ", "                 ", "                 " },
                    { "                 ", "                 ", "      HHHHH      ", "    BH     HB    ", "   B         B   ", "   H   DDD   H   ", "  H   D   D   H  ", "  H  D     D  H  ", "  H  D  P  D  H  ", "  H  D     D  H  ", "  H   D   D   H  ", "   H   DDD   H   ", "   B         B   ", "    BH     HB    ", "      HHHHH      ", "                 ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H   OOO   H   ", "  H   OOOOO   H  ", " GGGGOOEEEOOGGGG ", " GGGGOOEEEOOGGGG ", " GGGGOOEEEOOGGGG ", "  H   OOOOO   H  ", "   H   OOO   H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "                 ", "       GJG       ", "      HG GH      ", "    BH G G HB    ", "   B   GGG   B   ", "   H    F    H   ", "  H    BFB    H  ", " GGGG BEFEB GGGG ", " J  GFFFEFFFG  J ", " GGGG BEFEB GGGG ", "  H    BFB    H  ", "   H    F    H   ", "   B   GGG   B   ", "    BH G G HB    ", "      HG GH      ", "       GJG       ", "                 " },
                    { "                 ", "       GGG       ", "      HGGGH      ", "    BH GGG HB    ", "   B   GGG   B   ", "   H    B    H   ", "  H    BEB    H  ", " GGGG BEEEB GGGG ", " GGGGBEEEEEBGGGG ", " GGGG BEEEB GGGG ", "  H    BEB    H  ", "   H    B    H   ", "   B   GGG   B   ", "    BH GGG HB    ", "      HGGGH      ", "       GGG       ", "                 " },
                    { "      HHHHH      ", "    HHHHHHHHH    ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", "HHHHHHHHHHHHHHHHH", " HHHHHHHHHHHHHHH ", " HHHHHHHHHHHHHHH ", "  HHHHHHHHHHHHH  ", "  HHHHHHHHHHHHH  ", "    HHHHHHHHH    ", "      HHHHH      " } }))
        .addElement(
            'A',
            lazy(
                t -> ofBlock(
                    Block.getBlockFromItem(
                        MaterialsAlloy.PIKYONIUM.getFrameBox(1)
                            .getItem()),
                    0)))// t1 frame
        .addElement('B', ofBlock(GregTechAPI.sBlockCasings9, 0))
        .addElement('C', ofBlock(GregTechAPI.sBlockMetal4, 13)) // t1 block
        .addElement('D', ofBlock(GregTechAPI.sBlockGlass1, 6))
        .addElement('E', Casings.IsamillGearBoxCasing.asElement())
        .addElement('F', Casings.TurbineShaft.asElement())
        .addElement('G', ofBlock(supercriticalFluidTurbineCasing, 0))
        .addElement(
            'H',
            buildHatchAdder(MTEChamberCentrifuge.class)
                .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Maintenance, Energy, ExoticEnergy)
                .casingIndex(((BlockCasings12) GregTechAPI.sBlockCasings12).getTextureIndex(9))
                .dot(1)
                .buildAndChain(
                    onElementPass(MTEChamberCentrifuge::onCasingAdded, ofBlock(GregTechAPI.sBlockCasings12, 9))))
        .addElement('I', chainAllGlasses())
        .addElement(
            'J',
            buildHatchAdder(MTEChamberCentrifuge.class).adder(MTEChamberCentrifuge::addTurbineHatch)
                .hatchClass(MTEHatchTurbine.class)
                .casingIndex(1538)
                .dot(2)
                .build())
        .addElement('K', lazy(t -> Mods.Avaritia.isModLoaded() ? ofBlock(LudicrousBlocks.resource_block , 0) : ofBlock(GregTechAPI.sBlockMetal5,2))) // t2 block
        .addElement('L', ofFrame(Materials.Neutronium)) // t2 frame
        .addElement('M', lazy(t -> ofBlock(LudicrousBlocks.resource_block, 1))) // t3 block
        .addElement('N', ofFrame(Materials.Infinity)) // t3 frame
        .addElement('O', ofBlock(GregTechAPI.sBlockMetal9, 6))
        .addElement('P', lazy(t -> ofFrame(MaterialsUEVplus.SpaceTime))) // t4 frame
        .build();
    //spotless:on

    public MTEChamberCentrifuge(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEChamberCentrifuge(String aName) {
        super(aName);
    }

    @Override
    public IStructureDefinition<MTEChamberCentrifuge> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void onBlockDestroyed() {

        final IGregTechTileEntity meta = getBaseMetaTileEntity();
        World w = getBaseMetaTileEntity().getWorld();
        final int aX = meta.getXCoord(), aY = meta.getYCoord(), aZ = meta.getZCoord();
        for (int i = 0; i < turbineHolder.getSlots(); i++) {
            if (turbineHolder.getStackInSlot(i) != null) {
                ItemStack currentItem = turbineHolder.extractItem(i, 1, false);
                EntityItem entityItem = new EntityItem(w, aX, aY, aZ, currentItem);
                w.spawnEntityInWorld(entityItem);

            }
        }
        this.setTurbineInactive();
        this.mTurbineRotorHatches.clear();
        super.onBlockDestroyed();
    }

    public boolean addTurbineHatch(final IGregTechTileEntity aTileEntity, final int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        }
        final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity instanceof MTEHatchTurbine) {
            mTurbineRotorHatches.add((MTEHatchTurbine) aMetaTileEntity);

            return true;
        }
        return false;
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        mTier = aNBT.getInteger("multiTier");
        mMode = aNBT.getDouble("multiMode");
        RP = aNBT.getInteger("RP");
        mStaticAnimations = aNBT.getBoolean("turbineAnimationsStatic");
        tier2Fluid = aNBT.getBoolean("tier2FluidOn");
        if (turbineHolder != null) {
            turbineHolder.deserializeNBT(aNBT.getCompoundTag("inventory"));
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("multiTier", mTier);
        aNBT.setDouble("multiMode", mMode);
        aNBT.setBoolean("tier2FluidOn", tier2Fluid);
        aNBT.setInteger("RP", RP);
        aNBT.setBoolean("turbineAnimationsStatic", mStaticAnimations);

        if (turbineHolder != null) {
            aNBT.setTag("inventory", turbineHolder.serializeNBT());
        }

    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEChamberCentrifuge(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] {
                    Textures.BlockIcons
                        .getCasingTextureForId(GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings12, 9)),
                    TextureFactory.builder()
                        .addIcon(TEXTURE_CONTROLLER_ACTIVE)
                        .extFacing()
                        .build(),
                    TextureFactory.builder()
                        .addIcon(TEXTURE_CONTROLLER_ACTIVE_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            } else {
                rTexture = new ITexture[] {
                    Textures.BlockIcons
                        .getCasingTextureForId(GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings12, 9)),
                    TextureFactory.builder()
                        .addIcon(TEXTURE_CONTROLLER)
                        .extFacing()
                        .build() };
            }
        } else {
            rTexture = new ITexture[] { Textures.BlockIcons
                .getCasingTextureForId(GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings12, 9)) };
        }
        return rTexture;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Centrifuge")
            .addInfo(
                "3 Modes: " + EnumChatFormatting.LIGHT_PURPLE
                    + "Light"
                    + EnumChatFormatting.GRAY
                    + " | "
                    + EnumChatFormatting.GOLD
                    + "Standard"
                    + EnumChatFormatting.GRAY
                    + " | "
                    + EnumChatFormatting.GREEN
                    + "Heavy")
            .addInfo("200% faster than singleblock machines of the same voltage")
            .addInfo("Only uses 70% of the EU/t normally required")
            .addInfo("Will not perform overclocks over the hatch tier+1.")
            .addTecTechHatchInfo()
            .addSeparator()
            .addInfo(
                "Gains " + EnumChatFormatting.WHITE
                    + "2"
                    + EnumChatFormatting.GRAY
                    + " Turbine Slots per Structure Tier")
            .addInfo(
                "Gains " + EnumChatFormatting.WHITE
                    + "4"
                    + EnumChatFormatting.GRAY
                    + " * (Total Rotor Tier) Parallels. Non-Huge Turbines have lowered effectiveness.")
            .addInfo(
                "Requires Recipe Tier * " + EnumChatFormatting.BLUE
                    + "10L/s"
                    + EnumChatFormatting.GRAY
                    + " of "
                    + EnumChatFormatting.DARK_PURPLE
                    + "Kerosene"
                    + EnumChatFormatting.GRAY
                    + " to operate by default")
            .addInfo(
                "Supply " + EnumChatFormatting.DARK_PURPLE
                    + "Biocatalyzed Propulsion Fluid"
                    + EnumChatFormatting.GRAY
                    + " instead for a "
                    + EnumChatFormatting.WHITE
                    + "1.25x "
                    + EnumChatFormatting.GRAY
                    + "Parallel multiplier.")
            .addSeparator()
            .addInfo(
                EnumChatFormatting.LIGHT_PURPLE + "Light Mode"
                    + EnumChatFormatting.GRAY
                    + ": +"
                    + EnumChatFormatting.LIGHT_PURPLE
                    + "100%"
                    + EnumChatFormatting.GRAY
                    + " Speed Bonus, "
                    + EnumChatFormatting.LIGHT_PURPLE
                    + "0.9x"
                    + EnumChatFormatting.GRAY
                    + " Parallels, Maximum Recipe Tier is "
                    + EnumChatFormatting.LIGHT_PURPLE
                    + "Voltage Tier - 3")
            .addInfo(EnumChatFormatting.GOLD + "Standard Mode" + EnumChatFormatting.GRAY + ": No Changes")
            .addInfo(
                EnumChatFormatting.GREEN + "Heavy Mode"
                    + EnumChatFormatting.GRAY
                    + ": Divides Parallels by "
                    + EnumChatFormatting.GREEN
                    + "32"
                    + EnumChatFormatting.GRAY
                    + ", Requires T3 Structure and "
                    + EnumChatFormatting.DARK_PURPLE
                    + "Biocatalyzed Propulsion Fluid.")
            .addInfo(
                "Some recipes " + EnumChatFormatting.RED + BOLD + "require" + EnumChatFormatting.GRAY + " Heavy Mode.")

            .addSeparator()
            .addInfo(EnumChatFormatting.ITALIC + "" + EnumChatFormatting.DARK_RED + "Maahes guides the way...")
            .beginStructureBlock(17, 17, 17, false)
            .addController("Front Center")
            .addCasingInfoRange("Any Tiered Glass", 81, 135, true)
            .addCasingInfoMin("Vibration-Safe Casing", 550, false)
            .addCasingInfoExactly("Chamber Grate", 144, false)
            .addCasingInfoExactly("Central Frame Blocks", 9, true)
            .addCasingInfoExactly("Central Rotor Blocks", 56, true)
            .addCasingInfoExactly("IsaMill Gearbox Casing", 54, false)
            .addCasingInfoRange("PBI Pipe Casing", 160, 178, false)
            .addCasingInfoRange("Turbine Shaft", 6, 24, false)
            .addCasingInfoRange("Rotor Assembly", 2, 8, false)
            .addCasingInfoRange("SC Turbine Casing", 66, 264, false)
            .addInputBus("Any Vibration-Safe Casing", 1)
            .addOutputBus("Any Vibration-Safe Casing", 1)
            .addInputHatch("Any Vibration-Safe Casing", 1)
            .addOutputHatch("Any Vibration-Safe Casing", 1)
            .addEnergyHatch("Any Vibration-Safe Casing", 1)
            .addMaintenanceHatch("Any Vibration-Safe Casing", 1)
            .addSubChannelUsage(GTStructureChannels.BOROGLASS)
            .toolTipFinisher(EnumChatFormatting.GRAY, 50);

        return tt;
    }

    @Override
    public void construct(ItemStack holoStack, boolean hintsOnly) {
        if (holoStack.stackSize == 1) {
            buildPiece(STRUCTURE_TIER_1, holoStack, hintsOnly, horizontalOffset, verticalOffset, depthOffset);
        }
        if (holoStack.stackSize == 2) {
            buildPiece(STRUCTURE_TIER_2, holoStack, hintsOnly, horizontalOffset, verticalOffset, depthOffset);
        }

        if (holoStack.stackSize == 3) {
            buildPiece(STRUCTURE_TIER_3, holoStack, hintsOnly, horizontalOffset, verticalOffset, depthOffset);
        }

        if (holoStack.stackSize >= 4) {
            buildPiece(STRUCTURE_TIER_4, holoStack, hintsOnly, horizontalOffset, verticalOffset, depthOffset);
        }

    }

    @Override
    public int survivalConstruct(ItemStack holoStack, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        if (holoStack.stackSize == 1) {
            return survivalBuildPiece(
                STRUCTURE_TIER_1,
                holoStack,
                horizontalOffset,
                verticalOffset,
                depthOffset,
                elementBudget,
                env,
                false,
                true);
        }
        if (holoStack.stackSize == 2) {
            return survivalBuildPiece(
                STRUCTURE_TIER_2,
                holoStack,
                horizontalOffset,
                verticalOffset,
                depthOffset,
                elementBudget,
                env,
                false,
                true);
        }
        if (holoStack.stackSize == 3) {
            return survivalBuildPiece(
                STRUCTURE_TIER_3,
                holoStack,
                horizontalOffset,
                verticalOffset,
                depthOffset,
                elementBudget,
                env,
                false,
                true);
        }
        if (holoStack.stackSize >= 4) {
            return survivalBuildPiece(
                STRUCTURE_TIER_4,
                holoStack,
                horizontalOffset,
                verticalOffset,
                depthOffset,
                elementBudget,
                env,
                false,
                true);
        }
        return 0;
    }

    private int mCasingAmount;

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {

        mCasingAmount = 0;
        mTier = 0;
        int mMaxCasingsFound = 0;
        if (checkPiece(STRUCTURE_TIER_1, horizontalOffset, verticalOffset, depthOffset)) {
            mTier = 1;
        }
        mMaxCasingsFound = Math.max(mMaxCasingsFound, mCasingAmount);
        mCasingAmount = 0;
        if (checkPiece(STRUCTURE_TIER_2, horizontalOffset, verticalOffset, depthOffset)) {
            mTier = 2;
        }
        mMaxCasingsFound = Math.max(mMaxCasingsFound, mCasingAmount);
        mCasingAmount = 0;
        if (checkPiece(STRUCTURE_TIER_3, horizontalOffset, verticalOffset, depthOffset)) {
            mTier = 3;
        }
        mMaxCasingsFound = Math.max(mMaxCasingsFound, mCasingAmount);
        mCasingAmount = 0;
        if (checkPiece(STRUCTURE_TIER_4, horizontalOffset, verticalOffset, depthOffset)) {
            mTier = 4;
        }
        mMaxCasingsFound = Math.max(mMaxCasingsFound, mCasingAmount);
        GTDataUtils.dedupList(mExoticEnergyHatches);
        GTDataUtils.dedupList(mEnergyHatches);
        GTDataUtils.dedupList(mOutputBusses);
        GTDataUtils.dedupList(mOutputHatches);
        GTDataUtils.dedupList(mInputHatches);
        GTDataUtils.dedupList(mInputBusses);
        GTDataUtils.dedupList(mMaintenanceHatches);

        // if someone knows a better workaround, please let me know in review.
        return mTier > 0 && mMaxCasingsFound >= 550;
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setMaxParallel(getTrueParallel());
        if (mExoticEnergyHatches.isEmpty()) {
            logic.setAvailableVoltage(GTUtility.roundUpVoltage(this.getMaxInputVoltage()));
            logic.setAvailableAmperage(1L);
        } else super.setProcessingLogicPower(logic);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @NotNull
            @Override
            protected CheckRecipeResult validateRecipe(@NotNull GTRecipe recipe) {
                amountToDrain = GTUtility.getTier(recipe.mEUt) * 10;
                if (!checkFluid(5 * amountToDrain)) return SimpleCheckRecipeResult.ofFailure("invalidfluidsup");
                if (mMode == 0.0 && GTUtility.getTier(getAverageInputVoltage()) - GTUtility.getTier(recipe.mEUt) < 3)
                    return CheckRecipeResultRegistry.NO_RECIPE;
                if (mMode == 2.0 && !tier2Fluid) return SimpleCheckRecipeResult.ofFailure("invalidfluidsup");

                if (recipe.getMetadataOrDefault(CentrifugeRecipeKey.INSTANCE, Boolean.FALSE) && mMode != 2.0)
                    return CheckRecipeResultRegistry.NO_RECIPE;

                getSpeed();
                setSpeedBonus(1F / speed);
                return super.validateRecipe(recipe);
            }

            @Override
            protected @NotNull CheckRecipeResult onRecipeStart(@NotNull GTRecipe recipe) {
                setTurbineActive();
                return super.onRecipeStart(recipe);
            }

            @NotNull
            @Override
            protected OverclockCalculator createOverclockCalculator(@NotNull GTRecipe recipe) { // implements Hatch+1 OC
                return super.createOverclockCalculator(recipe).setMaxOverclocks(
                    (GTUtility.getTier(getAverageInputVoltage()) - GTUtility.getTier(recipe.mEUt)) + 1);
            }
        }.setEuModifier(0.7F);
    }

    @Override
    public void stopMachine(@NotNull ShutDownReason reason) {
        setTurbineInactive();
        super.stopMachine(reason);
    }

    public boolean isTurbine(ItemStack aStack) { // thank you airfilter!
        if (aStack == null) return false;
        if (!(aStack.getItem() instanceof MetaGeneratedTool01 tool)) return false;
        if (aStack.getItemDamage() < 170 || aStack.getItemDamage() > 179) return false;

        IToolStats stats = tool.getToolStats(aStack);
        if (stats == null || stats.getSpeedMultiplier() <= 0) return false;

        Materials material = MetaGeneratedTool.getPrimaryMaterial(aStack);
        return material != null && material.mToolSpeed > 0;
    }

    private int getSumRotorLevels() {
        int sumRotorLevels = 0;

        for (int i = 0; i < mTier * 2; i++) {
            if (turbineHolder.getStackInSlot(i) != null) { // operate under the assumption the tool in the slot IS a
                // rotor.
                ItemStack currentItem = turbineHolder.getStackInSlot(i);
                IToolStats toolStats = ((MetaGeneratedTool) currentItem.getItem()).getToolStats(currentItem);
                int harvestLevel = ((MetaGeneratedTool) currentItem.getItem()).getHarvestLevel(currentItem, "test");

                if (toolStats instanceof ToolTurbineHuge) {
                    sumRotorLevels += harvestLevel;
                    continue;
                }
                if (toolStats instanceof ToolTurbineLarge) {
                    sumRotorLevels += (int) (0.75F * harvestLevel);
                    continue;
                }
                if (toolStats instanceof ToolTurbineNormal) {
                    sumRotorLevels += (int) (0.5F * harvestLevel);
                    continue;
                }
                if (toolStats instanceof ToolTurbineSmall) {
                    sumRotorLevels += (int) (0.25F * harvestLevel);
                }

            }
        }

        return sumRotorLevels;
    }

    private boolean checkFluid(int amount) // checks if 5 seconds worth of fluid is found in ANY of the machines input
                                           // hatches
    {
        // checks for fluid in hatch, does not drain it.
        FluidStack tFluid = tier2Fluid ? MaterialsUEVplus.BiocatalyzedPropulsionFluid.getFluid(amount)
            : new FluidStack(GTPPFluids.Kerosene, amount);
        for (MTEHatchInput mInputHatch : mInputHatches) {
            if (drain(mInputHatch, tFluid, false)) {
                return true;
            }
        }
        return false; // fluid was not found.
    }

    public void setTurbineActive() {
        if (mStaticAnimations) return;

        for (MTEHatchTurbine h : validMTEList(this.mTurbineRotorHatches)) {
            h.setActive(true);
            h.onTextureUpdate();
        }
    }

    public void setTurbineInactive() {
        for (MTEHatchTurbine h : validMTEList(this.mTurbineRotorHatches)) {
            h.setActive(false);
            h.onTextureUpdate();
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aTick % 100 == 0) {
            if (!getBaseMetaTileEntity().isActive() && !this.mTurbineRotorHatches.isEmpty()) {
                setTurbineInactive();
            }
        }
    }

    @Override
    public int getMaxParallelRecipes() {

        getRP(); // updates RP
        int parallels = RP;
        if (tier2Fluid) {
            parallels = (int) Math.floor(parallels * 1.25);
        }
        if (mMode == 2.0) {
            parallels /= 32;
        }
        return parallels > 0 ? parallels : 1; // if its 1, something messed up lol, just a failsafe in case i mess up
        // during testing
    }

    private int ticker = 1; // shoutout pcb fac source code, just increments and drains (amountToDrain) of the given
    // fluid every second

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (!super.onRunningTick(aStack)) {
            return false;
        }
        // might need a cleanup here
        if (ticker % 21 == 0) {

            FluidStack tFluid = tier2Fluid ? MaterialsUEVplus.BiocatalyzedPropulsionFluid.getFluid(amountToDrain)
                : new FluidStack(GTPPFluids.Kerosene, amountToDrain); // gets fluid to drain
            for (MTEHatchInput mInputHatch : mInputHatches) {
                if (drain(mInputHatch, tFluid, true)) {
                    ticker = 1;
                    return true;
                }
            }
            stopMachine(ShutDownReasonRegistry.outOfFluid(tFluid));
            ticker = 1;
            return false;
        }
        ticker++;
        return true;
    }

    @Override
    protected @NotNull MTEChamberCentrifugeGui getGui() {
        return new MTEChamberCentrifugeGui(this);
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    // helper methods for all the silly variables in this class

    public int getRP() {
        RP = 4 * getSumRotorLevels();
        if (mMode == 0.0) {
            RP = (int) (RP * 0.9);
        }
        return RP;
    }

    public float getSpeed() {
        speed = 3F;
        if (mMode == 0.0) {
            speed = 4.0F;
        }
        return speed;
    }

    public String getSpeedStr() {
        return (getSpeed() - 1) * 100 + "%";
    }

    public String modeToString() {
        if (mMode == 0.0) {
            return "Light";
        }
        if (mMode == 1.0) {
            return "Standard";
        }
        if (mMode == 2.0) {
            return "Heavy";
        }
        return "Unset";
    }

    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        mStaticAnimations = !mStaticAnimations;
        GTUtility
            .sendChatToPlayer(aPlayer, "Using " + (mStaticAnimations ? "Static" : "Animated") + " Turbine Texture.");
        for (MTEHatchTurbine h : validMTEList(this.mTurbineRotorHatches)) {
            h.mUsingAnimation = mStaticAnimations;
        }
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTPPRecipeMaps.centrifugeNonCellRecipes;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }
}
