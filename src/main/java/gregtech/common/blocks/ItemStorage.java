package gregtech.common.blocks;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.Optional;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import mods.railcraft.common.items.firestone.IItemFirestoneBurning;

@Optional.Interface(
    iface = "mods.railcraft.common.items.firestone.IItemFirestoneBurning",
    modid = Mods.ModIDs.RAILCRAFT)
public class ItemStorage extends ItemBlock implements IItemFirestoneBurning {

    public ItemStorage(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(true);
        setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);
    }

    @Override
    public String getUnlocalizedName(ItemStack aStack) {
        return this.field_150939_a.getUnlocalizedName() + "." + getDamage(aStack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack aStack) {
        String aName = super.getItemStackDisplayName(aStack);
        if (this.field_150939_a instanceof BlockMetal) {
            int aDamage = aStack.getItemDamage();
            if (aDamage >= 0 && aDamage < ((BlockMetal) this.field_150939_a).mMats.length) {
                aName = ((BlockMetal) this.field_150939_a).mMats[aDamage].getLocalizedNameForItem(aName);
            }
        }
        return aName;
    }

    @Override
    public int getMetadata(int aMeta) {
        return aMeta;
    }

    @Override
    public void addInformation(ItemStack aStack, EntityPlayer aPlayer, List<String> aList, boolean aF3_H) {
        super.addInformation(aStack, aPlayer, aList, aF3_H);
    }

    @Override
    @Optional.Method(modid = Mods.ModIDs.RAILCRAFT)
    public boolean shouldBurn(ItemStack itemStack) {
        if (this.field_150939_a instanceof BlockMetal metal) {
            int damage = itemStack.getItemDamage();
            return (damage >= 0 && damage < metal.mMats.length && metal.mMats[damage] == Materials.Firestone);
        }
        return false;
    }
}
