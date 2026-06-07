package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class AutoCrouchCritMixin {

    private int autoCrouchTicks = 0;

    @Inject(method = "attack", at = @At("HEAD"))
    public void onAttack(Entity target, CallbackInfo ci) {
        if (PvpMenuScreen.autoCrouchCritEnabled && (Object) this instanceof LocalPlayer player) {

            boolean isCrit = player.fallDistance > 0.0F
                    && !player.onGround()
                    && !player.onClimbable()
                    && !player.isInWater()
                    && !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                    && !player.isPassenger();

            if (isCrit) {
                autoCrouchTicks = 4;
                Minecraft.getInstance().options.keyShift.setDown(true);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTick(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) {
            // Наша проверенная логика авто-приседания при крите
            if (autoCrouchTicks > 0) {
                autoCrouchTicks--;
                if (autoCrouchTicks <= 0) {
                    Minecraft.getInstance().options.keyShift.setDown(false);
                }
            }
            // Старые 3D-частицы убраны для предотвращения вылета игры при входе в мир
        }
    }
}