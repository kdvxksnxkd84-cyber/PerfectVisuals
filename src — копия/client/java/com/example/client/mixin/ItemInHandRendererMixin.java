package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void onRenderItem(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        // Меняем положение только если это руки самого игрока
        if (livingEntity == net.minecraft.client.Minecraft.getInstance().player) {
            if (leftHanded) {
                // Применяем раздельные настройки смещения ЛЕВОЙ руки
                poseStack.translate(PvpMenuScreen.leftX, PvpMenuScreen.leftY, PvpMenuScreen.leftZ);
            } else {
                // Применяем раздельные настройки смещения ПРАВОЙ руки
                poseStack.translate(PvpMenuScreen.rightX, PvpMenuScreen.rightY, PvpMenuScreen.rightZ);
            }
        }
    }
}