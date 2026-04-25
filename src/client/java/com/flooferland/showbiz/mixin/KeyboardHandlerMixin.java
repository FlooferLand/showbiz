package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.network.packets.ProgrammerKeyPressPacket;
import com.flooferland.showbiz.screens.ProgrammerScreen;
import com.flooferland.showbiz.types.entity.PlayerProgrammingData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	void showbiz_keyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
		var minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;

		// Consuming hotbar inputs
		var data = PlayerProgrammingData.Companion.getFromPlayer(minecraft.player);
		if (data.getActive()) {
			// 0 key
			if (key == GLFW.GLFW_KEY_0 && action == 1 && !(minecraft.screen instanceof ProgrammerScreen)) {
				minecraft.setScreen(new ProgrammerScreen());
				return;
			}

			// Number buttons
			var hotbarKeys = minecraft.options.keyHotbarSlots;
			for (int i = 0; i < hotbarKeys.length; i++) {
				if (hotbarKeys[i].matches(key, scanCode)) {
					if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) {
						ci.cancel();
						return;
					}
					var pressed = (action == GLFW.GLFW_PRESS);
					data.getHeldKeys()[i] = pressed;
					ClientPlayNetworking.send(new ProgrammerKeyPressPacket(i, data.mapKeyToBit(i), pressed));
					ci.cancel();
					return;
				}
			}

			// client-only!!
			data.saveToPlayer(minecraft.player);
		}
	}
}
