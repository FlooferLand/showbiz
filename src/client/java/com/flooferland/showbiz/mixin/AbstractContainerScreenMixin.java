package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.mixin.accessor.AbstractContainerScreenAccessor;
import com.flooferland.showbiz.screens.HandbookPageScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow protected Slot hoveredSlot;

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void showbiz_keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		var self = (AbstractContainerScreenAccessor) (Object) this;
		HandbookPageScreen.Companion.handleKeyPressed(self, keyCode, scanCode, modifiers, cir);
	}
}
