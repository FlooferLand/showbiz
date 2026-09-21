package com.flooferland.showbiz.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenSelectedTabAccessor {
	@Accessor("selectedTab")
	static CreativeModeTab showbiz_getSelectedTab() {
		throw new AssertionError();
	}
}