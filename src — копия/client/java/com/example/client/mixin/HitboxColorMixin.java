package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public class HitboxColorMixin {

    @Redirect(
        method = "renderHitbox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLineBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDDDDFFFF)V"
        )
    )
    private static void redirectHitboxColor(PoseStack poseStack, VertexConsumer vertexConsumer, double d, double e, double f, double g, double h, double i, float r, float g2, float b, float a) {
        if (PvpMenuScreen.hitboxesEnabled) {
            // Раскладываем hex-цвет из меню на RGB составляющие
            float red = ((PvpMenuScreen.hitboxColor >> 16) & 0xFF) / 255.0f;
            float green = ((PvpMenuScreen.hitboxColor >> 8) & 0xFF) / 255.0f;
            float blue = (PvpMenuScreen.hitboxColor & 0xFF) / 255.0f;
            
            // Рендерим хитбокс кастомным цветом
            LevelRenderer.renderLineBox(poseStack, vertexConsumer, d, e, f, g, h, i, red, green, blue, 1.0f);
        } else {
            // Если выключено — рендерим стандартным
            LevelRenderer.renderLineBox(poseStack, vertexConsumer, d, e, f, g, h, i, r, g2, b, a);
        }
    }
}