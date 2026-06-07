package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
        method = "renderShape",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onRenderShape(
        PoseStack poseStack,
        VertexConsumer vertexConsumer,
        VoxelShape voxelShape,
        double d,
        double e,
        double f,
        float g,
        float h,
        float i,
        float j,
        CallbackInfo ci) {
        
        if (PvpMenuScreen.blockOutlineEnabled) {
            ci.cancel(); 

            int color = PvpMenuScreen.blockOutlineColor;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float gVal = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = 0.8f; 

            PoseStack.Pose lastPose = poseStack.last();
            Matrix4f poseMatrix = lastPose.pose();

            // Рендерим по граням VoxelShape
            voxelShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                float dx = (float)(x2 - x1);
                float dy = (float)(y2 - y1);
                float dz = (float)(z2 - z1);
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 0.0f) {
                    dx /= len;
                    dy /= len;
                    dz /= len;
                }
                // Исправлено: удален аргумент normalMatrix, теперь только (dx, dy, dz)
                vertexConsumer.addVertex(poseMatrix, (float)(x1 + d), (float)(y1 + e), (float)(z1 + f))
                              .setColor(r, gVal, b, a)
                              .setNormal(dx, dy, dz);
                vertexConsumer.addVertex(poseMatrix, (float)(x2 + d), (float)(y2 + e), (float)(z2 + f))
                              .setColor(r, gVal, b, a)
                              .setNormal(dx, dy, dz);
            });
        }
    }
}