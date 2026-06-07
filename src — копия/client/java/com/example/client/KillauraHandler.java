package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillauraHandler {
    private static final Minecraft mc = Minecraft.getInstance();
    private static long lastAttackTime = 0;
    private static long nextAttackDelay = 0; // Рандомизированная задержка для обхода античитов
    private static boolean wasWalking = false; // Флаг для отслеживания состояния ходьбы
    
    // Переменные состояния авто-еды
    private static int originalSlot = -1;
    private static boolean isEating = false;

    // Переменные состояния кружения
    private static boolean circleDirectionLeft = true;

    public static void onClientTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            resetMovement();
            stopEating();
            return;
        }

        // 1. Логика Авто-Еды
        if (PvpMenuScreen.killauraAutoEat) {
            handleAutoEat();
        }

        // Если в данный момент персонаж ест, блокируем атаки
        if (isEating) {
            int hunger = mc.player.getFoodData().getFoodLevel();
            if (hunger >= 20 || !hasFoodInHotbar()) {
                stopEating();
            }
            return; 
        }

        // Если модуль выключен или открыто меню — сбрасываем движение и выходим
        if (!PvpMenuScreen.killauraEnabled || mc.screen != null) {
            resetMovement();
            return;
        }

        // Находим цели (для лука используем расширенный диапазон 30 блоков)
        double searchRange = PvpMenuScreen.killauraBow && isHoldingBow() ? 30.0 : 12.0;
        AABB area = mc.player.getBoundingBox().inflate(searchRange);

        List<LivingEntity> targets = mc.level.getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (entity == mc.player) return false;
            if (!entity.isAlive()) return false;

            if (PvpMenuScreen.killauraPlayersOnly) {
                return entity instanceof Player;
            }
            return true; 
        });

        if (targets.isEmpty()) {
            resetMovement();
            if (PvpMenuScreen.killauraBow && isHoldingBow()) {
                mc.options.keyUse.setDown(false); // Отпускаем тетиву, если нет целей
            }
            return;
        }

        // Выбираем ближайшую цель
        targets.sort(Comparator.comparingDouble(mc.player::distanceTo));
        LivingEntity target = targets.get(0);

        // Расстояние до цели
        double distance = mc.player.distanceTo(target);

        // 2. Логика Авто-Лука (Bow Aura)
        if (PvpMenuScreen.killauraBow && isHoldingBow()) {
            handleBowAura(target);
            return; // Прерываем выполнение обычной ближней киллауры при стрельбе
        }

        // Поворачиваем камеру в сторону цели сглаженным образом
        rotateTowards(target);

        // 3. Логика авто-преследования, кружения и обхода препятствий (Chase)
        if (PvpMenuScreen.killauraChase) {
            wasWalking = true;

            if (mc.player.horizontalCollision && mc.player.tickCount % 20 == 0) {
                circleDirectionLeft = !circleDirectionLeft;
            }

            if (PvpMenuScreen.killauraCircleStrafe && distance <= PvpMenuScreen.killauraRange) {
                if (circleDirectionLeft) {
                    mc.options.keyLeft.setDown(true);
                    mc.options.keyRight.setDown(false);
                } else {
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(true);
                }

                if (distance > 3.0) {
                    mc.options.keyUp.setDown(true);
                    mc.options.keyDown.setDown(false);
                } else if (distance < 2.0) {
                    mc.options.keyUp.setDown(false);
                    mc.options.keyDown.setDown(true);
                } else {
                    mc.options.keyUp.setDown(false);
                    mc.options.keyDown.setDown(false);
                }

                if (mc.player.horizontalCollision && mc.player.onGround() && !mc.player.onClimbable() && !mc.player.isInWater()) {
                    mc.player.jumpFromGround();
                }

            } else {
                mc.options.keyDown.setDown(false);

                if (distance > 1.8) {
                    mc.options.keyUp.setDown(true);

                    if (mc.player.horizontalCollision && mc.player.onGround() && !mc.player.onClimbable() && !mc.player.isInWater()) {
                        mc.player.jumpFromGround();
                    }

                    if (mc.player.horizontalCollision) {
                        long cycle = System.currentTimeMillis() % 1000;
                        if (cycle < 500) {
                            mc.options.keyLeft.setDown(true);
                            mc.options.keyRight.setDown(false);
                        } else {
                            mc.options.keyLeft.setDown(false);
                            mc.options.keyRight.setDown(true);
                        }
                    } else {
                        mc.options.keyLeft.setDown(false);
                        mc.options.keyRight.setDown(false);
                    }
                } else {
                    mc.options.keyUp.setDown(false);
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                }
            }

            if (mc.player.onGround() && !mc.player.isSprinting()) {
                mc.player.setSprinting(true);
            }
        }

        // Если цель вне зоны атаки мечом, выходим
        if (distance > PvpMenuScreen.killauraRange) {
            return;
        }

        // 4. Логика автоматических критических ударов (Jump-Crit)
        if (PvpMenuScreen.killauraAutoCrit) {
            if (mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.onClimbable()) {
                mc.player.jumpFromGround();
                return;
            }

            if (mc.player.fallDistance <= 0.0F) {
                return;
            }
        }

        // Ограничение и рандомизация CPS (обход эвристики античитов)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime < nextAttackDelay) {
            return;
        }

        // Нанесение удара ближнего боя (проверка isLookingAt убрана для стабильности попаданий мечом)
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        
        lastAttackTime = currentTime;

        double baseCps = Math.max(1.0f, PvpMenuScreen.killauraCps);
        double randomMultiplier = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
        double currentCps = baseCps * randomMultiplier;
        nextAttackDelay = (long) (1000.0 / currentCps);
    }

    /**
     * Логика работы автоматического лука
     */
    private static void handleBowAura(LivingEntity target) {
        if (mc.player == null) return;

        // Поворачиваемся к цели
        rotateTowards(target);

        // Для стрельбы из лука проверка взгляда остается активной, чтобы стрелы летели точно
        if (isLookingAt(target, 20.0f)) {
            if (!mc.player.isUsingItem()) {
                mc.options.keyUse.setDown(true);
            } else {
                int ticksUsed = mc.player.getTicksUsingItem();
                if (ticksUsed >= 21) {
                    mc.options.keyUse.setDown(false); // Выстрел
                }
            }
        } else {
            if (mc.player.isUsingItem()) {
                mc.options.keyUse.setDown(false); // Сброс тетивы при потере цели из вида
            }
        }
    }

    private static boolean isHoldingBow() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        return !stack.isEmpty() && stack.getItem() instanceof BowItem;
    }

    private static void handleAutoEat() {
        if (mc.player == null) return;

        float hp = mc.player.getHealth();
        int hunger = mc.player.getFoodData().getFoodLevel();

        if (!isEating) {
            if (hp <= 12.0f || hunger <= 15) {
                int foodSlot = findFoodSlot();
                if (foodSlot != -1) {
                    originalSlot = mc.player.getInventory().selected;
                    mc.player.getInventory().selected = foodSlot;
                    mc.options.keyUse.setDown(true);
                    isEating = true;
                }
            }
        }
    }

    private static void stopEating() {
        if (isEating) {
            if (mc.options != null && mc.options.keyUse != null) {
                mc.options.keyUse.setDown(false);
            }
            if (originalSlot != -1 && mc.player != null) {
                mc.player.getInventory().selected = originalSlot;
            }
            originalSlot = -1;
            isEating = false;
        }
    }

    private static int findFoodSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isSafeFood(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasFoodInHotbar() {
        return findFoodSlot() != -1;
    }

    private static boolean isSafeFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        String name = item.toString();

        if (name.contains("rotten_flesh") || name.contains("spider_eye") || name.contains("pufferfish") || name.contains("poisonous_potato")) {
            return false;
        }

        return stack.has(net.minecraft.core.component.DataComponents.FOOD);
    }

    private static void resetMovement() {
        if (wasWalking) {
            if (mc.options != null) {
                mc.options.keyUp.setDown(false);
                mc.options.keyDown.setDown(false);
                mc.options.keyLeft.setDown(false);
                mc.options.keyRight.setDown(false);
            }
            wasWalking = false;
        }
    }

    private static void rotateTowards(LivingEntity target) {
        if (mc.player == null) return;
        
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        
        double targetHeight = target.getEyeY() - 0.4 + ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
        
        // Компенсация гравитации стрелы при стрельбе из лука
        if (PvpMenuScreen.killauraBow && isHoldingBow()) {
            double distance = mc.player.distanceTo(target);
            targetHeight += (distance * 0.05); 
        }

        double diffY = targetHeight - mc.player.getEyeY();
        double distanceXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(diffY, distanceXZ) * 180.0 / Math.PI);

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float maxSpeed = (float) ThreadLocalRandom.current().nextDouble(22.0, 38.0);

        float nextYaw = currentYaw + clamp(yawDiff, -maxSpeed, maxSpeed);
        float nextPitch = currentPitch + clamp(pitchDiff, -maxSpeed / 2.0f, maxSpeed / 2.0f);

        nextYaw += (float) ThreadLocalRandom.current().nextDouble(-0.4, 0.4);
        nextPitch += (float) ThreadLocalRandom.current().nextDouble(-0.2, 0.2);

        mc.player.setYRot(nextYaw);
        mc.player.setXRot(clamp(nextPitch, -90.0f, 90.0f));
    }

    private static boolean isLookingAt(LivingEntity target, float maxAngle) {
        if (mc.player == null) return false;
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double diffY = (target.getEyeY() - 0.4) - mc.player.getEyeY();
        double distanceXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) -(Math.atan2(diffY, distanceXZ) * 180.0 / Math.PI);

        float yawDiff = Math.abs(wrapDegrees(targetYaw - mc.player.getYRot()));
        float pitchDiff = Math.abs(targetPitch - mc.player.getXRot());

        return yawDiff <= maxAngle && pitchDiff <= maxAngle;
    }

    private static float wrapDegrees(float value) {
        float f = value % 360.0F;
        if (f >= 180.0F) {
            f -= 360.0F;
        }
        if (f < -180.0F) {
            f += 360.0F;
        }
        return f;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}