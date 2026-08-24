package com.shenfnx.fissionevolved;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

/** Creative tab reserved for Fission Evolved fuel rods. */
public final class FissionEvolvedTab extends CreativeTabs {

    public FissionEvolvedTab() {
        super("fissionevolved");
    }

    @Override
    public Item getTabIconItem() {
        return SuperFuelRods.SUPERNOVA_EMBER;
    }
}
