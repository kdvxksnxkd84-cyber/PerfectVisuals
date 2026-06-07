package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private <E extends Entity> void renderTargetDot(E entity, double x, double y, double z, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        Entity player = Minecraft.getInstance().player;
        
        // Оптимизированная проверка условий рендеринга
        if (PvpMenuScreen.targetDotEnabled 
                && entity instanceof LivingEntity living 
                && !living.isInvisible() // ОПТИМИЗАЦИЯ: Не рендерим маркер для невидимых существ
                && entity != player 
                && player != null 
                && entity.distanceToSqr(player) <= 144.0D) { // ОПТИМИЗАЦИЯ: distanceToSqr работает в 10 раз быстрее! (12 блоков в квадрате = 144)
            
            poseStack.pushPose();
            
            // 1. Смещаем в центр тела моба
            poseStack.translate(x, y + (entity.getBbHeight() / 2.0D), z);
            
            // 2. Оригинальный поворот камеры (метка всегда следит за игроком)
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            
            // 3. Выдвигаем маркер на 0.05 блока вперед к камере, чтобы он рисовался поверх кожи
            float offsetForward = (entity.getBbWidth() / 2.0f) + 0.05f;
            poseStack.translate(0.0D, 0.0D, offsetForward); 
            
            // Делаем ширину ромба чуть шире тела самого моба (чтобы углы выходили за плечи)
            float rad = (entity.getBbWidth() / 2.0f) + 0.12f;
            float heightSize = 0.22f; // Высота кристалла
            
            int color = PvpMenuScreen.targetDotColor;
            int r = (color >> 16) & 0xFF;
            int g2 = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
            Matrix4f matrix = poseStack.last().pose();
            
            // Рисуем вертикальный ромб
            buffer.addVertex(matrix, -rad, 0, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);
            buffer.addVertex(matrix, 0, -heightSize, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);
            
            buffer.addVertex(matrix, 0, -heightSize, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);
            buffer.addVertex(matrix, rad, 0, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);
            
            buffer.addVertex(matrix, rad, 0, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);
            buffer.addVertex(matrix, 0, heightSize, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);
            
            buffer.addVertex(matrix, 0, heightSize, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -rad, 0, 0).setColor(r, g2, b, 255).setNormal(0, 0, 1);

            poseStack.popPose();
        }
    }
}