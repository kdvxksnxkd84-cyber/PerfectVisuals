package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenAnimationMixin {
    @Unique private long openTime = 0;

    @Inject(method = "render", at = @At("HEAD"))
    private void renderAnimation(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (!PvpMenuScreen.animationsEnabled) return;

        Screen screen = (Screen) (Object) this;
        
        // Анимируем только Чат
        if (screen instanceof ChatScreen) {
            if (openTime == 0) openTime = System.currentTimeMillis();

            long elapsed = System.currentTimeMillis() - openTime;
            float progress = Math.min(1.0f, elapsed / 150.0f); // Анимация длится 150мс
            float scale = 0.95f + (0.05f * progress); // Легкий зум
            float yOffset = (1.0f - progress) * 15;   // Выезд снизу вверх

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, yOffset, 0);
            guiGraphics.pose().scale(1, scale, 1);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (PvpMenuScreen.animationsEnabled && screen instanceof ChatScreen) {
            guiGraphics.pose().popPose();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemove(CallbackInfo ci) {
        openTime = 0; // Сброс анимации при закрытии
    }
}