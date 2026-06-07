package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void onSetupFog(net.minecraft.client.Camera camera, FogRenderer.FogMode fogMode, float viewDistance, boolean thickFog, float partialTick, CallbackInfo ci) {
        if (PvpMenuScreen.customFogEnabled) {
            // Делаем красивую плотную стену тумана поближе (на 40% от дальности прорисовки)
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(viewDistance * 0.05f);
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(viewDistance * 0.45f);
            
            // Получаем цвета из HSB-слайдера нашего меню
            float r = ((PvpMenuScreen.fogColor >> 16) & 0xFF) / 255.0f;
            float g = ((PvpMenuScreen.fogColor >> 8) & 0xFF) / 255.0f;
            float b = (PvpMenuScreen.fogColor & 0xFF) / 255.0f;
            
            // Применяем кастомный цвет к туману в игре
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(r, g, b);
        }
    }
}