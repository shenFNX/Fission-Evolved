package com.shenfnx.fissionevolved;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class ItemBlockFissionStructureCasing extends ItemBlock {

    public ItemBlockFissionStructureCasing(Block block) {
        super(block);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return BlockFissionStructureCasing.clampMeta(damage);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + BlockFissionStructureCasing.clampMeta(stack.getItemDamage());
    }
}
