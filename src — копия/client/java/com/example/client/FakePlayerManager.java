package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class FakePlayerManager {
    private static RemotePlayer fakePlayer = null;
    private static final int FAKE_PLAYER_ID = -1337;

    public static void toggleFakePlayer(boolean enable) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (enable) {
            if (fakePlayer == null) {
                GameProfile profile = new GameProfile(UUID.fromString("606e2ff0-ed77-4842-96c5-111407e53b11"), "PvP_Dummy");
                
                fakePlayer = new RemotePlayer(mc.level, profile);
                
                Vec3 lookAngle = mc.player.getLookAngle();
                double posX = mc.player.getX() + lookAngle.x * 3.0D;
                double posY = mc.player.getY();
                double posZ = mc.player.getZ() + lookAngle.z * 3.0D;
                
                fakePlayer.setPos(posX, posY, posZ);
                
                fakePlayer.setYRot(mc.player.getYRot() + 180.0F);
                fakePlayer.setXRot(-mc.player.getXRot());
                fakePlayer.yRotO = fakePlayer.getYRot();
                fakePlayer.xRotO = fakePlayer.getXRot();
                fakePlayer.yHeadRot = fakePlayer.getYRot();
                fakePlayer.yHeadRotO = fakePlayer.getYRot();
                
                fakePlayer.setId(FAKE_PLAYER_ID);
                mc.level.addEntity(fakePlayer);
            }
        } else {
            despawn();
        }
    }

    public static void despawn() {
        Minecraft mc = Minecraft.getInstance();
        if (fakePlayer != null && mc.level != null) {
            mc.level.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }

    public static boolean isFakePlayer(Entity entity) {
        return fakePlayer != null && entity != null && entity.getId() == FAKE_PLAYER_ID;
    }

    public static void handleHit(Player attacker) {
        Minecraft mc = Minecraft.getInstance();
        if (fakePlayer != null && mc.level != null) {
            
            // 1. Анимация покраснения (урон)
            fakePlayer.hurtTime = 10;
            fakePlayer.hurtDuration = 10;
            fakePlayer.animateHurt(0.0f); // Покачивание

            // 2. Звук получения урона
            mc.level.playLocalSound(
                fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F,
                false
            );

            // 3. Эффект партиклов (крит)
            mc.particleEngine.createTrackingEmitter(fakePlayer, net.minecraft.core.particles.ParticleTypes.CRIT);

            // 4. Реалистичное откидывание (Knockback) от удара
            if (attacker != null) {
                double dx = fakePlayer.getX() - attacker.getX();
                double dz = fakePlayer.getZ() - attacker.getZ();
                double magnitude = Math.sqrt(dx * dx + dz * dz);
                if (magnitude > 0) {
                    // Толкаем фейк-игрока немного назад и вверх
                    fakePlayer.push((dx / magnitude) * 0.3, 0.1, (dz / magnitude) * 0.3);
                }
            }
        }
    }
}