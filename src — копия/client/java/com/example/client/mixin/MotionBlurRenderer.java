package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

public class MotionBlurRenderer {
    private static RenderTarget blurTarget = null;

    public static void renderBlur() {
        if (!PvpMenuScreen.motionBlurEnabled) {
            if (blurTarget != null) {
                blurTarget.destroyBuffers();
                blurTarget = null;
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int width = mc.getWindow().getScreenWidth();
        int height = mc.getWindow().getScreenHeight();

        if (blurTarget == null || blurTarget.width != width || blurTarget.height != height) {
            if (blurTarget != null) blurTarget.destroyBuffers();
            blurTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.50f); 
        blurTarget.bindRead();
        mc.getMainRenderTarget().bindWrite(true);
        mc.getMainRenderTarget().blitToScreen(width, height);
        
        mc.getMainRenderTarget().bindRead();
        blurTarget.bindWrite(true);
        blurTarget.blitToScreen(width, height);

        mc.getMainRenderTarget().bindWrite(true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}