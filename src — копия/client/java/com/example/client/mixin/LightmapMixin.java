package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightmapMixin {
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void onGetBrightness(DimensionType dimensionType, int i, CallbackInfoReturnable<Float> cir) {
        if (PvpMenuScreen.fullbrightEnabled) {
            cir.setReturnValue(1.0f);
        }
    }
}