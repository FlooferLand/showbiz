package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.registry.ModItemGroups;
import com.flooferland.showbiz.utils.CreateAeronauticsBurglary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// We love the Create Aeronautics devs Mwwwah
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
	@Shadow private static CreativeModeTab selectedTab;

	@Inject(method = "render", at = @At("TAIL"))
	private void showbiz_render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick, final CallbackInfo ci) {
		if (selectedTab == ModItemGroups.Main.getTab()) {
			CreateAeronauticsBurglary.renderBanners((CreativeModeInventoryScreen) (Object) this, guiGraphics, mouseX, mouseY);
		}
	}

	/*@Inject(method = "getTooltipFromContainerItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;tabs()Ljava/util/List;"))
	private void showbiz_getTooltipFromContainerItem(final ItemStack stack, final CallbackInfoReturnable<List<Component>> cir, @Local(ordinal = 1) final List<Component> list1, @Local final int i) {
		final ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		final ResourceLocation id = SimulatedRegistrate.ITEM_TO_SECTION.get(key);
		if(id != null) {
			final SimulatedSection section = SimResourceManagers.SIMULATED_SECTION.get(id);
			if(section != null) {
				list1.add(i, section.title().text().copy().withStyle(ChatFormatting.BLUE));
			}
		}
	}*/
}