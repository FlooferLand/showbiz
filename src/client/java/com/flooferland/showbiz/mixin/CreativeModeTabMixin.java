package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.registry.ModItemGroups;
import com.flooferland.showbiz.utils.CreateAeronauticsBurglary;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {
	@Shadow private Collection<ItemStack> displayItems;
	@Shadow private Set<ItemStack> displayItemsSearchTab;

	@WrapMethod(method = "buildContents")
	private void showbiz_buildContents(final CreativeModeTab.ItemDisplayParameters parameters, final Operation<Void> original) {
		final CreativeModeTab self = (CreativeModeTab) (Object) this;
		if (self == ModItemGroups.Main.getTab()) {
			final List<ItemStack> displayItems = new LinkedList<>();
			final Set<ItemStack> searchItems = new LinkedHashSet<>();
			CreateAeronauticsBurglary.processItems(displayItems::add, searchItems::add);
			this.displayItems = displayItems;
			this.displayItemsSearchTab = searchItems;
			return;
		}
		original.call(parameters);
	}
}