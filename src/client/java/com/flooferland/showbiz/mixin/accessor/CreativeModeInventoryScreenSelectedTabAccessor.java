package com.flooferland.showbiz.mixin.accessor;

import com.flooferland.showbiz.Showbiz;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenSelectedTabAccessor {
	@Accessor("selectedTab")
	static CreativeModeTab showbiz_getSelectedTab() {
		Showbiz.INSTANCE.getLog().error("The mixin for showbiz_getSelectedTab failed. No implementation");
		return CreativeModeTabs.getDefaultTab();
	}
}