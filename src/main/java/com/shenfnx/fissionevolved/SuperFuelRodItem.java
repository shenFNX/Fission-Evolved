package com.shenfnx.fissionevolved;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import gregtech.api.items.ItemRadioactiveCellIC;

/** A native GT5U fuel rod with Fission Evolved presentation and balance. */
public final class SuperFuelRodItem extends ItemRadioactiveCellIC {

    private final String displayNameKey;
    private final String loreKeyBase;
    private final String textureName;
    private IIcon icon;

    public SuperFuelRodItem(String registryName, String textureName, String englishName, ItemRadioactiveCellIC baseRod,
        ItemStack depletedStack, float energyMultiplier) {
        super(
            FissionEvolved.MOD_ID + "." + registryName,
            englishName,
            baseRod.numberOfCells,
            baseRod.getMaxDamageEx(),
            baseRod.sEnergy * energyMultiplier,
            baseRod.sRadiation,
            baseRod.sHeat,
            depletedStack,
            baseRod.sMox,
            baseRod.sHeatBonus);
        this.displayNameKey = "item." + FissionEvolved.MOD_ID + "." + registryName + ".name";
        this.loreKeyBase = "item." + FissionEvolved.MOD_ID + "." + registryName + ".lore";
        this.textureName = textureName;
        setCreativeTab(SuperFuelRods.TAB);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.canTranslate(displayNameKey) ? StatCollector.translateToLocal(displayNameKey)
            : super.getItemStackDisplayName(stack);
    }

    public String getDisplayNameKey() {
        return displayNameKey;
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
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocalFormatted("fissionevolved.tooltip.output", sEnergy));
        tooltip.add("");
        addTranslatedLore(tooltip, loreKeyBase + ".1");
        addTranslatedLore(tooltip, loreKeyBase + ".2");
    }

    private static void addTranslatedLore(List<String> tooltip, String key) {
        if (StatCollector.canTranslate(key)) {
            tooltip.add(StatCollector.translateToLocal(key));
        }
    }
}
