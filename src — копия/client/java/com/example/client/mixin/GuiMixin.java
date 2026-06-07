package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow @Final protected Minecraft minecraft;

    // =========================================================================
    // СТИЛЬНЫЙ HUD ОСТРОВОК PERFECT VISUALS С FPS (Эффект Glassmorphism)
    // =========================================================================
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderIsland(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = this.minecraft; 
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return; 

        int fps = mc.getFps();
        String pText = "PERFECT";
        String vText = " VISUALS";
        String fText = " | " + fps + " FPS";
        
        // Резервируем место под зелёную точку статуса (10 пикселей)
        int totalTextWidth = mc.font.width(pText + vText + fText);
        int width = totalTextWidth + 24; 
        int height = 16;
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = (screenWidth - width) / 2; 
        int y = 4; 

        // 1. Задний фон островка (сверхтемный матовый закругленный прямоугольник)
        PvpMenuScreen.drawSmoothRoundedRect(guiGraphics, x, y, width, height, 4, 0xCC07070A);
        
        // 2. Тонкая стеклянная полупрозрачная рамка вокруг островка
        PvpMenuScreen.drawSmoothRoundedRect(guiGraphics, x, y, width, height, 4, 0x1AFFFFFF);
        
        // 3. Переливающаяся неоновая полоска на самом верху островка
        float hue = (System.currentTimeMillis() % 6000) / 6000f;
        int chromaColor = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.9f);
        guiGraphics.fill(x + 4, y, x + width - 4, y + 1, 0xFF000000 | (chromaColor & 0x00FFFFFF));

        // 4. Маленький зелёный неоновый статус-маркер (микро-кружок слева)
        PvpMenuScreen.drawSmoothRoundedRect(guiGraphics, x + 6, y + 6, 4, 4, 2, 0xFF3AF27F);

        // 5. Отрисовка текста (сдвинута вправо с учетом маркера)
        int currentX = x + 14;
        guiGraphics.drawString(mc.font, pText, currentX, y + 4, 0x3AF27F, true); 
        currentX += mc.font.width(pText);
        
        guiGraphics.drawString(mc.font, vText, currentX, y + 4, 0xFFFFFF, true); 
        currentX += mc.font.width(vText);
        
        guiGraphics.drawString(mc.font, fText, currentX, y + 4, 0xAAAAAA, true); 
    }

    // Отрисовка кастомного точечного прицела с настраиваемым цветом
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void drawCustomCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PvpMenuScreen.customCrosshairEnabled) {
            ci.cancel(); 
            
            int centerX = this.minecraft.getWindow().getGuiScaledWidth() / 2;
            int centerY = this.minecraft.getWindow().getGuiScaledHeight() / 2;
            
            // Получаем цвет из сохраненного конфига
            int color = 0xFF000000 | PvpMenuScreen.customCrosshairColor;
            
            // Отрисовываем точку по центру
            guiGraphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, color);
        }
    }
}