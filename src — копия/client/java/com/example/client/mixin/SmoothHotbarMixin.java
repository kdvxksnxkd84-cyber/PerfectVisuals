package com.example.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class SmoothHotbarMixin {

    @Shadow @Final protected Minecraft minecraft;

    @Unique
    private float smoothSelectedSlot = 0.0f;
    
    @Unique
    private long lastTime = System.currentTimeMillis();

    // ИСПРАВЛЕНО: float partialTick теперь идет ПЕРВЫМ, а GuiGraphics — ВТОРЫМ!
    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void onRenderHotbarHead(float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (this.minecraft.player != null) {
            int targetSlot = this.minecraft.player.getInventory().selected;
            
            long now = System.currentTimeMillis();
            float delta = (now - lastTime) / 1000.0f;
            lastTime = now;
            delta = Math.min(0.1f, delta);

            // Расчет циклического перехода
            float diff = targetSlot - smoothSelectedSlot;
            if (Math.abs(diff) > 4.5f) {
                if (diff > 0) {
                    smoothSelectedSlot += 9.0f;
                } else {
                    smoothSelectedSlot -= 9.0f;
                }
            }
            
            // Скорость скольжения рамки (16.0f)
            smoothSelectedSlot += (targetSlot - smoothSelectedSlot) * delta * 16.0f;
        }
    }

    // Перенаправляем отрисовку спрайта рамки выделения
    @Redirect(
        method = "renderHotbar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V")
    )
    private void redirectBlitSprite(GuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height) {
        if (this.minecraft.player != null && sprite.getPath().contains("slot_selection")) {
            int selected = this.minecraft.player.getInventory().selected;
            
            // Вычисляем центр хотбара
            int centerX = x + 91 + 1 - selected * 20;
            
            // Плавно смещаем рамку
            int smoothX = (int) (centerX - 91 - 1 + smoothSelectedSlot * 20);
            
            instance.blitSprite(sprite, smoothX, y, width, height);
        } else {
            instance.blitSprite(sprite, x, y, width, height);
        }
    }
}