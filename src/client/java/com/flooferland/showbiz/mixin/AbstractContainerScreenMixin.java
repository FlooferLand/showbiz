package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.handbook.Handbook;
import com.flooferland.showbiz.registry.ModClientInput;
import com.flooferland.showbiz.screens.HandbookPageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow protected Slot hoveredSlot;

	@Inject(method = "keyPressed", at = @At("HEAD"))
	private void showbiz_keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		var instance = Minecraft.getInstance();

		if (hoveredSlot == null) return;
		var stack = hoveredSlot.getItem();

		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		var entry = Handbook.INSTANCE.getCache().getItems().get(id);
		if (entry == null) return;

		var pressed = ModClientInput.OpenInHandbook.getMapping().matches(keyCode, scanCode);
		if (pressed && !(instance.screen instanceof HandbookPageScreen)) {
			instance.setScreen(new HandbookPageScreen(instance.screen, id));
		}
	}
}
