package gtPlusPlus.xmod.forestry;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import binnie.extratrees.genetics.ExtraTreeSpecies;
import forestry.api.arboriculture.EnumGermlingType;
import forestry.api.arboriculture.EnumWoodType;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.TreeManager;
import forestry.arboriculture.genetics.TreeDefinition;
import forestry.plugins.PluginArboriculture;
import gregtech.mixin.interfaces.accessors.TreeDefinitionAccessor;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTETreeFarm;

public class ForestryTreeHandler {

    public static void generateForestryTrees() {
        for (TreeDefinition tree : TreeDefinition.values()) {
            String speciesUID = tree.getUID();

            ItemStack sapling = tree.getMemberStack(EnumGermlingType.SAPLING);

            ItemStack log;
            EnumWoodType woodType = ((TreeDefinitionAccessor) tree).gt5u$getWoodType();
            if (woodType != null) {
                log = TreeManager.woodItemAccess.getLog(woodType, false);
            } else {
                log = ((TreeDefinitionAccessor) tree).gt5u$getVanillaWood();
            }

            ItemStack leaves = new ItemStack(PluginArboriculture.blocks.leaves, 1, 0);
            if (speciesUID != null) {
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                nbtTagCompound.setString("species", speciesUID);
                leaves.setTagCompound(nbtTagCompound);
            }

            ItemStack fruit = null;
            ITree individual = tree.getIndividual();
            if (individual.canBearFruit()) {
                ItemStack[] produceList = individual.getProduceList();
                if (produceList != null && produceList.length > 0) {
                    fruit = individual.getProduceList()[0];
                }
            }

            MTETreeFarm.registerForestryTree(
                speciesUID,
                sapling == null ? null : sapling.copy(),
                log == null ? null : log.copy(),
                leaves.copy(),
                fruit == null ? null : fruit.copy());
        }
    }

    public static void generateExtraTreesTrees() {
        for (ExtraTreeSpecies species : ExtraTreeSpecies.values()) {

            String speciesUID = species.getUID();

            ITree individual = TreeManager.treeRoot.templateAsIndividual(species.getTemplate());
            ItemStack sapling = TreeManager.treeRoot.getMemberStack(individual, 0);

            ItemStack log = null;
            if (species.getLog() != null) {
                log = species.getLog()
                    .getItemStack();
            }

            ItemStack leaves = new ItemStack(PluginArboriculture.blocks.leaves, 1, 0);

            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            nbtTagCompound.setString("species", speciesUID);
            leaves.setTagCompound(nbtTagCompound);

            ItemStack fruit = null;
            if (individual.canBearFruit()) {
                ItemStack[] produceList = individual.getProduceList();
                if (produceList != null && produceList.length > 0) {
                    fruit = individual.getProduceList()[0];
                }
            }

            MTETreeFarm.registerForestryTree(
                speciesUID,
                sapling == null ? null : sapling.copy(),
                log == null ? null : log.copy(),
                leaves.copy(),
                fruit == null ? null : fruit.copy());
        }
    }
}
