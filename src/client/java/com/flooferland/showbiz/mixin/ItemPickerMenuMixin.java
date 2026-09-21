package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.mixin.accessor.CreativeModeInventoryScreenSelectedTabAccessor;
import com.flooferland.showbiz.registry.ModItemGroups;
import com.flooferland.showbiz.utils.CreateAeronauticsBurglary;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// We love the Create Aeronautics devs Mwwwah
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class ItemPickerMenuMixin {
	@Shadow protected abstract int getRowIndexForScroll(float f);

	@Final @Shadow public NonNullList<ItemStack> items;

	@Inject(method = "scrollTo", at = @At("HEAD"))
	private void showbiz_scrollTo(final float f, final CallbackInfo ci) {
		if (CreativeModeInventoryScreenSelectedTabAccessor.showbiz_getSelectedTab() == ModItemGroups.Main.getTab()) {
			CreateAeronauticsBurglary.padMenuItems(this.items);
		}
		CreateAeronauticsBurglary.CURRENT_ROW = this.getRowIndexForScroll(f);
	}
}