package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

public class Aimbot {
    public static void onTick(Minecraft mc) {
        // Работает автоматически, если просто включен в меню
        if (!PvpMenuScreen.aimbotEnabled || mc.player == null || mc.level == null) return;

        // Не наводить прицел, если открыт инвентарь, чат или меню
        if (mc.screen != null) return;

        Entity target = null;
        // Увеличили дистанцию автоматической наводки до стабильных 8 блоков
        double closestDist = 8.0; 

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isValidTarget(entity, mc.player, closestDist)) {
                target = entity;
                closestDist = mc.player.distanceTo(entity);
            }
        }

        if (target != null) {
            rotateTowards(mc.player, target);
        }
    }

    private static void rotateTowards(Player player, Entity target) {
        double diffX = target.getX() - player.getX();
        // Направляем взгляд в шею/грудь цели
        double diffY = (target.getY() + target.getBbHeight() * 0.65F) - player.getEyeY();
        double diffZ = target.getZ() - player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        // Скорость 1.0f — это жесткий моментальный захват цели (работает на 100%)
        float speed = 1.0f; 
        float yaw = player.getYRot() + Mth.wrapDegrees(targetYaw - player.getYRot()) * speed;
        float pitch = player.getXRot() + (targetPitch - player.getXRot()) * speed;

        // Принудительно устанавливаем углы вращения камеры
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));

        // Синхронизируем кадры для плавности рендеринга без дерганий
        player.yRotO = yaw;
        player.xRotO = pitch;
        
        // Синхронизируем поворот головы персонажа
        player.yHeadRot = yaw;
    }

    private static boolean isValidTarget(Entity entity, Player player, double range) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == player) return false;
        if (!entity.isAlive()) return false;
        return player.distanceTo(entity) <= range;
    }
}