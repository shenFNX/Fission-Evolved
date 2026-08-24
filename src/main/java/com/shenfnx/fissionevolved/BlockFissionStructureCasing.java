package com.shenfnx.fissionevolved;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The three non-tiered structural components unique to the colossal fission reactor. */
public final class BlockFissionStructureCasing extends Block {

    public static final int CONTAINMENT = 0;
    public static final int REFLECTOR = 1;
    public static final int CORE_CONDUIT = 2;
    private static final int VARIANT_COUNT = 3;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockFissionStructureCasing() {
        super(Material.iron);
        setBlockName("fission_structure_casing");
        setHardness(12.0F);
        setResistance(80.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 3);
        setCreativeTab(SuperFuelRods.TAB);
    }

    @Override
    public int damageDropped(int meta) {
        return clampMeta(meta);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int meta = 0; meta < VARIANT_COUNT; meta++) {
            list.add(new ItemStack(item, 1, meta));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[] { register.registerIcon(FissionEvolved.MOD_ID + ":reactor_containment_casing"),
            register.registerIcon(FissionEvolved.MOD_ID + ":neutron_reflector_matrix"),
            register.registerIcon(FissionEvolved.MOD_ID + ":fission_core_conduit") };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons[clampMeta(meta)];
    }

    public static int clampMeta(int meta) {
        return Math.max(0, Math.min(VARIANT_COUNT - 1, meta));
    }
}
