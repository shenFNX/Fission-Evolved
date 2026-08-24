package com.shenfnx.fissionevolved;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import ic2.api.item.IBoxable;

/** The visible depleted counterpart of a SUPER fuel rod. */
public final class SuperDepletedFuelRodItem extends Item implements IBoxable {

    private final String displayNameKey;
    private final String loreKey;
    private final String textureName;
    private IIcon icon;

    public SuperDepletedFuelRodItem(String registryName, String textureName) {
        this.displayNameKey = "item." + FissionEvolved.MOD_ID + "." + registryName + ".name";
        this.loreKey = "item." + FissionEvolved.MOD_ID + "." + registryName + ".lore";
        this.textureName = resolveTextureName(textureName);

        setUnlocalizedName(FissionEvolved.MOD_ID + "." + registryName);
        setTextureName(this.textureName);
        setCreativeTab(SuperFuelRods.TAB);
        setMaxStackSize(64);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.canTranslate(displayNameKey) ? StatCollector.translateToLocal(displayNameKey)
            : super.getItemStackDisplayName(stack);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        icon = register.registerIcon(textureName);
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }

    @Override
    public void getSubItems(Item item, net.minecraft.creativetab.CreativeTabs tab, List<ItemStack> items) {
        if (tab == getCreativeTab()) {
            items.add(new ItemStack(this, 1, 0));
        }
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack stack) {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        if (StatCollector.canTranslate(loreKey)) {
            tooltip.add(StatCollector.translateToLocal(loreKey));
        }
    }

    private static String resolveTextureName(String textureName) {
        return textureName.indexOf(':') >= 0 ? textureName : "gregtech:gt." + textureName;
    }
}
