package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;

@Mixin(Gui.class)
public class CustomImageHudMixin {

    private float slideProgress = 0.0f;
    private long lastRenderTime = System.currentTimeMillis();

    // Загруженная текстура
    private static ResourceLocation loadedTexture = null;
    private static boolean attemptedLoad = false;

    // Метод для загрузки текстуры из файла
    private void tryLoadCustomImage() {
        if (attemptedLoad) return;
        attemptedLoad = true;

        try {
            // Ищем файл perfect_visuals.png в папке .minecraft/config/
            File imageFile = new File(Minecraft.getInstance().gameDirectory, "config/perfect_visuals.png");
            
            if (imageFile.exists()) {
                NativeImage nativeImage = NativeImage.read(new FileInputStream(imageFile));
                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                // Регистрируем новую текстуру в игре
                loadedTexture = Minecraft.getInstance().getTextureManager().register("perfect_visuals_img", dynamicTexture);
            }
        } catch (Exception e) {
            System.err.println("Не удалось загрузить кастомную картинку: " + e.getMessage());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustomImage(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000.0f;
        lastRenderTime = currentTime;

        // Загружаем картинку 1 раз при первой отрисовке
        tryLoadCustomImage();

        if (PvpMenuScreen.customImageEnabled) {
            slideProgress += deltaTime * 3.5f; 
        } else {
            slideProgress -= deltaTime * 3.5f; 
        }
        
        slideProgress = Math.max(0.0f, Math.min(1.0f, slideProgress));

        if (slideProgress > 0.0f) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // Базовый размер 
            int sizeX = 80;
            int sizeY = 80;
            float padding = 15.0f; 
            
            float hiddenX;
            float visibleX;

            // Логика позиции (Слева или Справа)
            if (PvpMenuScreen.customImageLeft) {
                // Если слева: прячем за левым краем, выводим с левого края
                hiddenX = -sizeX - 20.0f;
                visibleX = padding + PvpMenuScreen.customImageOffsetX;
            } else {
                // Если справа (по умолчанию): прячем за правым, выводим с правого
                hiddenX = screenWidth + sizeX + 20.0f; 
                visibleX = screenWidth - sizeX - padding + PvpMenuScreen.customImageOffsetX;
            }
            
            float easeProgress = 1.0f - (float) Math.pow(1.0f - slideProgress, 3);
            float currentX = hiddenX + (visibleX - hiddenX) * easeProgress;
            float currentY = screenHeight / 2.0f - (sizeY / 2.0f) + PvpMenuScreen.customImageOffsetY; 

            // --- АНИМАЦИЯ ЛЕВИТАЦИИ (ФЛАУИ) ---
            if (PvpMenuScreen.customImageFloatAnim) {
                // ИСЯПРАВЛЕНИЕ: Используем остаток от деления (modulo), чтобы число не становилось слишком большим.
                // В Java огромные float числа ломают Math.sin().
                float timeSec = (currentTime % 100000L) / 1000.0f;
                
                // Плавное движение по X (влево-вправо)
                float floatX = (float) Math.sin(timeSec * 2.0f) * 4.0f; 
                // Плавное движение по Y (вверх-вниз)
                float floatY = (float) Math.cos(timeSec * 2.5f) * 6.0f; 
                
                currentX += floatX;
                currentY += floatY;
            }

            // Рендер
            if (loadedTexture != null) {
                // Если картинка загружена - рисуем её
                guiGraphics.blit(loadedTexture, (int)currentX, (int)currentY, 0.0f, 0.0f, sizeX, sizeY, sizeX, sizeY);
            } else {
                // Если картинки нет - рисуем стандартный белый квадрат
                guiGraphics.fill((int)currentX - 4, (int)currentY - 4, (int)currentX + sizeX + 4, (int)currentY + sizeY + 4, 0x50000000); // Тень
                guiGraphics.fill((int)currentX, (int)currentY, (int)currentX + sizeX, (int)currentY + sizeY, 0xFFFFFFFF); // Квадрат
                guiGraphics.drawCenteredString(mc.font, "НЕТ", (int)currentX + sizeX / 2, (int)currentY + sizeY / 2 - 10, 0x000000);
                guiGraphics.drawCenteredString(mc.font, "ФОТО", (int)currentX + sizeX / 2, (int)currentY + sizeY / 2 + 5, 0x000000);
            }
        }
    }
}