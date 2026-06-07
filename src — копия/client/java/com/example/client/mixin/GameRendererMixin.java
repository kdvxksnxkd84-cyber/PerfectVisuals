package com.example.client.mixin;

import net.minecraft.client.DeltaTracker; // Добавлен отсутствующий импорт
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderEnd(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        MotionBlurRenderer.renderBlur();
    }
}