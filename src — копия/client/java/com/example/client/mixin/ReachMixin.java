package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ReachMixin {
    
    // Внедряемся в метод, который определяет дальность взаимодействия/удара игрока
    @Inject(method = "getPickRange", at = @At("HEAD"), cancellable = true)
    private void customReach(CallbackInfoReturnable<Float> cir) {
        // Мы берем значение напрямую из твоего меню!
        float customReachValue = PvpMenuScreen.reachRadius;
        
        // Если ползунок сдвинут больше стандарта (3.0), применяем его
        if (customReachValue > 3.0f) {
            cir.setReturnValue(customReachValue);
        }
    }
}