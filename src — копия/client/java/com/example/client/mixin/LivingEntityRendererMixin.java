package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer; 
import com.mojang.math.Axis; 
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType; 
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player; 
import org.joml.Matrix4f; 
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    // Структура для хранения точек шлейфа
    private static class TrailPoint {
        final double x, y, z;
        final long time;

        TrailPoint(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    // Хранилище шлейфов для всех игроков
    private static final Map<UUID, List<TrailPoint>> playerTrails = new HashMap<>();

    // --- ФУНКЦИЯ 1: Отрисовка маркера над головой ---
    @Inject(method = "render", at = @At("RETURN"))
    private void renderTargetDot(T entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (PvpMenuScreen.targetDotEnabled) {
            Minecraft mc = Minecraft.getInstance();
            
            if (entity != mc.player) {
                matrices.pushPose();
                
                double heightOffset = entity.getBbHeight() + 0.25F;
                matrices.translate(0.0F, (float) heightOffset, 0.0F);
                
                matrices.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                matrices.scale(-0.05F, -0.05F, 0.05F);
                
                Font font = mc.font;
                String dotText = "●"; 
                
                float xOffset = -font.width(dotText) / 2.0F;
                
                font.drawInBatch(
                    dotText, 
                    xOffset, 
                    0.0F, 
                    PvpMenuScreen.targetDotColor, 
                    false, 
                    matrices.last().pose(), 
                    vertexConsumers, 
                    Font.DisplayMode.NORMAL, 
                    0, 
                    light
                );
                
                matrices.popPose();
            }
        }
    }

    // --- ФУНКЦИЯ 2: Отрисовка вращающейся компактной 3D Китайской Шляпы ---
    @Inject(method = "render", at = @At("RETURN"))
    private void renderChinaHat(T entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (PvpMenuScreen.chinaHatEnabled && entity instanceof Player) {
            matrices.pushPose();

            double height = entity.getBbHeight() + 0.28; 
            if (entity.isCrouching()) {
                height -= 0.22;
            }

            matrices.translate(0.0D, height, 0.0D);

            float rotation = (float)(System.currentTimeMillis() % 4000) / 4000.0F * 360.0F;
            matrices.mulPose(Axis.YP.rotationDegrees(rotation));
            matrices.mulPose(Axis.XP.rotationDegrees(5.0F));

            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.lightning());
            Matrix4f matrix = matrices.last().pose();

            int color = PvpMenuScreen.chinaHatColor;
            int r = (color >> 16) & 0xFF;
            int gColor = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = 150; 

            float radius = 0.42F; 
            float heightCone = 0.14F; 
            int segments = 32; 

            for (int j = 0; j < segments; j++) {
                float angle1 = (float) (j * Math.PI * 2 / segments);
                float angle2 = (float) ((j + 1) * Math.PI * 2 / segments);

                float x1 = (float) Math.sin(angle1) * radius;
                float z1 = (float) Math.cos(angle1) * radius;

                float x2 = (float) Math.sin(angle2) * radius;
                float z2 = (float) Math.cos(angle2) * radius;

                vertexConsumer.addVertex(matrix, 0.0F, heightCone, 0.0F).setColor(r, gColor, b, a).setLight(15728880);
                vertexConsumer.addVertex(matrix, x1, 0.0F, z1).setColor(r, gColor, b, a).setLight(15728880);
                vertexConsumer.addVertex(matrix, x2, 0.0F, z2).setColor(r, gColor, b, a).setLight(15728880);
                vertexConsumer.addVertex(matrix, 0.0F, heightCone, 0.0F).setColor(r, gColor, b, a).setLight(15728880);
            }

            matrices.popPose();
        }
    }

    // --- ФУНКЦИЯ 3: Отрисовка 3D шлейфа за спиной игрока ---
    @Inject(method = "render", at = @At("RETURN"))
    private void renderTrail(T entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (PvpMenuScreen.trailEnabled && entity instanceof Player player) {
            UUID uuid = player.getUUID();
            List<TrailPoint> points = playerTrails.computeIfAbsent(uuid, k -> new ArrayList<>());

            long now = System.currentTimeMillis();
            long maxAge = (long) (PvpMenuScreen.trailLength * 1000L);

            // Очищаем старые точки
            points.removeIf(p -> now - p.time > maxAge);

            // Вычисляем, движется ли игрок
            double speedX = player.getX() - player.xo;
            double speedZ = player.getZ() - player.zo;
            boolean isMoving = (speedX * speedX + speedZ * speedZ) > 0.0001;

            // Ограничение на добавление точек (не чаще одного раза в 30 мс, чтобы избежать перегрузки на высоких FPS)
            boolean canAdd = points.isEmpty() || (now - points.get(points.size() - 1).time > 30);

            if (isMoving && canAdd) {
                // Определяем точку за спиной игрока
                float bodyYaw = player.yBodyRot;
                float rad = bodyYaw * ((float) Math.PI / 180.0F);
                
                double backOffset = 0.22; // Небольшое расстояние за моделью
                double spawnX = player.getX() + Math.sin(rad) * backOffset;
                double spawnY = player.getY() + player.getBbHeight() * 0.65; // Примерно уровень лопаток/шеи
                double spawnZ = player.getZ() - Math.cos(rad) * backOffset;

                points.add(new TrailPoint(spawnX, spawnY, spawnZ, now));
            }

            if (points.size() > 1) {
                matrices.pushPose();

                // Вычитаем интерполированную позицию сущности
                double renderX = player.xo + (player.getX() - player.xo) * tickDelta;
                double renderY = player.yo + (player.getY() - player.yo) * tickDelta;
                double renderZ = player.zo + (player.getZ() - player.zo) * tickDelta;
                matrices.translate(-renderX, -renderY, -renderZ);

                Matrix4f matrix = matrices.last().pose();
                VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.lightning()); // Светящийся тип рендера без теней

                int color = PvpMenuScreen.trailColor;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                float ribbonHeight = 0.25F; // Высота шлейфа

                for (int i = 0; i < points.size() - 1; i++) {
                    TrailPoint p1 = points.get(i);
                    TrailPoint p2 = points.get(i + 1);

                    // Коэффициент увядания для плавного исчезновения хвоста
                    float factor1 = (float) (now - p1.time) / maxAge;
                    float factor2 = (float) (now - p2.time) / maxAge;

                    int alpha1 = (int) ((1.0F - factor1) * 160.0F);
                    int alpha2 = (int) ((1.0F - factor2) * 160.0F);

                    if (alpha1 < 0) alpha1 = 0;
                    if (alpha2 < 0) alpha2 = 0;

                    float halfHeight = ribbonHeight / 2.0F;

                    // --- СТОРОНА 1: Лицевая сторона полигона ---
                    buffer.addVertex(matrix, (float) p1.x, (float) (p1.y - halfHeight), (float) p1.z).setColor(r, g, b, alpha1).setLight(15728880);
                    buffer.addVertex(matrix, (float) p2.x, (float) (p2.y - halfHeight), (float) p2.z).setColor(r, g, b, alpha2).setLight(15728880);
                    buffer.addVertex(matrix, (float) p2.x, (float) (p2.y + halfHeight), (float) p2.z).setColor(r, g, b, alpha2).setLight(15728880);
                    buffer.addVertex(matrix, (float) p1.x, (float) (p1.y + halfHeight), (float) p1.z).setColor(r, g, b, alpha1).setLight(15728880);

                    // --- СТОРОНА 2: Обратная сторона полигона (с измененным порядком вершин) ---
                    buffer.addVertex(matrix, (float) p1.x, (float) (p1.y + halfHeight), (float) p1.z).setColor(r, g, b, alpha1).setLight(15728880);
                    buffer.addVertex(matrix, (float) p2.x, (float) (p2.y + halfHeight), (float) p2.z).setColor(r, g, b, alpha2).setLight(15728880);
                    buffer.addVertex(matrix, (float) p2.x, (float) (p2.y - halfHeight), (float) p2.z).setColor(r, g, b, alpha2).setLight(15728880);
                    buffer.addVertex(matrix, (float) p1.x, (float) (p1.y - halfHeight), (float) p1.z).setColor(r, g, b, alpha1).setLight(15728880);
                }

                matrices.popPose();
            }
        }
    }
}