package com.example.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Mixin(AbstractContainerScreen.class)
public abstract class SmoothSlotsMixin {

    @Shadow protected int leftPos; // Смещение по X
    @Shadow protected int topPos;  // Смещение по Y
    @Shadow public abstract AbstractContainerMenu getMenu(); // Безопасное получение меню

    @Unique
    private final WeakHashMap<Slot, Float> slotScales = new WeakHashMap<>();
    
    @Unique
    private long lastTime = System.currentTimeMillis();

    @Unique
    private boolean isMouseOverSlot(Slot slot, double mouseX, double mouseY) {
        if (slot == null) return false;
        int slotX = this.leftPos + slot.x;
        int slotY = this.topPos + slot.y;
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void onRenderHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            long now = System.currentTimeMillis();
            float delta = (now - lastTime) / 1000.0f;
            lastTime = now;

            delta = Math.min(0.1f, delta);

            AbstractContainerMenu containerMenu = this.getMenu();
            if (containerMenu != null && containerMenu.slots != null) {
                for (Slot slot : containerMenu.slots) {
                    if (slot == null) continue;
                    
                    boolean isHovered = isMouseOverSlot(slot, mouseX, mouseY);
                    float current = slotScales.getOrDefault(slot, 1.0f);
                    float target = isHovered ? 1.20f : 1.0f; // Увеличиваем до 1.2x

                    if (current != target) {
                        float speed = 1.5f;
                        if (current < target) {
                            current = Math.min(target, current + delta * speed);
                        } else {
                            current = Math.max(target, current - delta * speed);
                        }
                        slotScales.put(slot, current);
                    }
                }
            }
        } catch (Exception e) {
            // Если что-то пошло не так, просто пропускаем тик, не ломая игру
            e.printStackTrace();
        }
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    public void onRenderSlotHead(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        try {
            if (slot == null) return;
            float scale = slotScales.getOrDefault(slot, 1.0f);
            if (scale > 1.0f) {
                guiGraphics.pose().pushPose();
                float centerX = slot.x + 8.0f;
                float centerY = slot.y + 8.0f;
                
                guiGraphics.pose().translate(centerX, centerY, 0);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                guiGraphics.pose().translate(-centerX, -centerY, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    public void onRenderSlotTail(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        try {
            if (slot == null) return;
            float scale = slotScales.getOrDefault(slot, 1.0f);
            if (scale > 1.0f) {
                guiGraphics.pose().popPose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}