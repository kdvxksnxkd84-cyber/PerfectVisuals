package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    
    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
    private void onGetPickRadius(CallbackInfoReturnable<Float> cir) {
        // Если ползунок больше 1.0 (обычный размер хитбокса)
        if (PvpMenuScreen.hitboxScale > 1.0f) {
            // Добавляем разницу к радиусу клика по мобу
            float expandedMargin = PvpMenuScreen.hitboxScale - 1.0f;
            cir.setReturnValue(expandedMargin);
        }
    }
}