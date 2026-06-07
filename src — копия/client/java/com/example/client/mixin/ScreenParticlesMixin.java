package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class ScreenParticlesMixin {

    @Shadow @Final protected Minecraft minecraft;

    // Плоские массивы — 100% защита от краша
    @Unique private final float[] px = new float[50];
    @Unique private final float[] py = new float[50];
    @Unique private final float[] pSpeedX = new float[50];
    @Unique private final float[] pSpeedY = new float[50];
    @Unique private final float[] pSize = new float[50];
    @Unique private boolean initialized = false;
    @Unique private long lastTime = System.currentTimeMillis();

    @Unique
    private void initParticle(int i, int width, int height, boolean randY) {
        px[i] = (float) (Math.random() * width);
        py[i] = randY ? (float) (Math.random() * height) : -5.0f;
        pSpeedX[i] = (float) (Math.random() * 16.0f - 8.0f);
        pSpeedY[i] = (float) (Math.random() * 26.0f + 20.0f);
        pSize[i] = (float) (Math.random() * 2.0f + 1.0f);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PvpMenuScreen.worldParticlesEnabled && this.minecraft.level != null && !this.minecraft.isPaused()) {
            int width = this.minecraft.getWindow().getGuiScaledWidth();
            int height = this.minecraft.getWindow().getGuiScaledHeight();

            if (!initialized) {
                for (int i = 0; i < 50; i++) {
                    initParticle(i, width, height, true);
                }
                initialized = true;
            }

            long now = System.currentTimeMillis();
            float delta = (now - lastTime) / 1000.0f;
            lastTime = now;
            delta = Math.min(0.1f, delta);

            int color = PvpMenuScreen.hudParticlesColor;
            int finalColor = (0x90 << 24) | (color & 0x00FFFFFF); 

            for (int i = 0; i < 50; i++) {
                px[i] += pSpeedX[i] * delta;
                py[i] += pSpeedY[i] * delta;

                if (py[i] > height + 5 || px[i] < -5 || px[i] > width + 5) {
                    initParticle(i, width, height, false);
                }

                guiGraphics.fill((int) px[i], (int) py[i], (int) (px[i] + pSize[i]), (int) (py[i] + pSize[i]), finalColor);
            }
        }
    }
}