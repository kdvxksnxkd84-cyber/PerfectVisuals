package com.example.client;

import com.example.client.mixin.Aimbot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.InputConstants;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.ArrayList;
import java.util.List;

public class ExampleModClient implements ClientModInitializer {
    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.pvp_mod.open_menu", 
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, 
            "category.pvp_mod.title"
        ));

        // Логика тиков, спринта и автоматической наводки
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.consumeClick() && client.screen == null) {
                client.setScreen(new PvpMenuScreen());
            }

            if (client.player != null) {
                if (PvpMenuScreen.sprintEnabled && client.player.input.up) {
                    client.player.setSprinting(true);
                }
                
                // Скрываем стандартные хитбоксы игры (потому что мы рисуем свои)
                client.getEntityRenderDispatcher().setRenderHitBoxes(false);

                AttributeInstance reachAttribute = client.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                if (reachAttribute != null) {
                    reachAttribute.setBaseValue(PvpMenuScreen.reachRadius);
                }
            }
        });

        // ПЕРЕХВАТ УДАРА ПО ФЕЙК ИГРОКУ
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && FakePlayerManager.isFakePlayer(entity)) {
                FakePlayerManager.handleHit(player); // Отыгрываем урон, партиклы и откидывание
                return InteractionResult.SUCCESS;    // Блокируем пакет на сервер
            }
            return InteractionResult.PASS;
        });

        // =========================================================================
        // КАСТОМНЫЙ РЕНДЕРИНГ: МАРКЕРЫ СУЩЕСТВ И 3D ХИТБОКСЫ
        // =========================================================================
        WorldRenderEvents.LAST.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            Vec3 camPos = context.camera().getPosition();
            PoseStack matrices = context.matrixStack();
            Matrix4f pose = matrices.last().pose();

            // Настройки OpenGL для рендеринга поверх мира
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            // Находим все цели в радиусе 16 блоков для хитбоксов и точек
            List<Entity> targets = new ArrayList<>();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity && entity != mc.player) {
                    if (mc.player.distanceToSqr(entity) <= 256.0) {
                        targets.add(entity);
                    }
                }
            }

            if (!targets.isEmpty()) {
                // 1. ОТРИСОВКА ВИЗУАЛЬНЫХ 3D ХИТБОКСОВ СУЩЕСТВ
                if (PvpMenuScreen.hitboxesEnabled) {
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

                    int color = PvpMenuScreen.hitboxColor;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int alpha = 180;

                    float margin = PvpMenuScreen.hitboxScale - 1.0f;

                    for (Entity entity : targets) {
                        AABB baseBox = entity.getBoundingBox();
                        
                        double minX = baseBox.minX - camPos.x - margin;
                        double minY = baseBox.minY - camPos.y - margin;
                        double minZ = baseBox.minZ - camPos.z - margin;
                        double maxX = baseBox.maxX - camPos.x + margin;
                        double maxY = baseBox.maxY - camPos.y + margin;
                        double maxZ = baseBox.maxZ - camPos.z + margin;

                        addBoxLine(buffer, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, alpha);

                        addBoxLine(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, alpha);

                        addBoxLine(buffer, pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, alpha);
                        addBoxLine(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, alpha);
                    }

                    MeshData meshData = buffer.build();
                    if (meshData != null) {
                        BufferUploader.drawWithShader(meshData);
                    }
                }

                // 2. ОТРИСОВКА 2D МАРКЕРОВ
                if (PvpMenuScreen.targetDotEnabled) {
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                    for (Entity entity : targets) {
                        // Исправленный метод получения тиков для версии 1.21
                        Vec3 entityPos = entity.getPosition(context.tickCounter().getGameTimeDeltaTicks());
                        double x = entityPos.x - camPos.x;
                        double y = entityPos.y - camPos.y + (entity.getBbHeight() / 2.0f);
                        double z = entityPos.z - camPos.z;

                        matrices.pushPose();
                        matrices.translate(x, y, z);
                        matrices.mulPose(context.camera().rotation()); 
                        
                        float size = 0.08f;
                        matrices.scale(size, size, size);

                        Matrix4f dotPose = matrices.last().pose();

                        int color = PvpMenuScreen.targetDotColor;
                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = color & 0xFF;

                        buffer.addVertex(dotPose, -0.5f, -0.5f, 0).setColor(r, g, b, 255);
                        buffer.addVertex(dotPose, -0.5f,  0.5f, 0).setColor(r, g, b, 255);
                        buffer.addVertex(dotPose,  0.5f,  0.5f, 0).setColor(r, g, b, 255);
                        buffer.addVertex(dotPose,  0.5f, -0.5f, 0).setColor(r, g, b, 255);

                        matrices.popPose();
                    }

                    MeshData meshData = buffer.build();
                    if (meshData != null) {
                        BufferUploader.drawWithShader(meshData);
                    }
                }
            }

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }

    private static void addBoxLine(BufferBuilder buffer, Matrix4f pose, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b, int a) {
        buffer.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(r, g, b, a);
        buffer.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(r, g, b, a);
    }
}