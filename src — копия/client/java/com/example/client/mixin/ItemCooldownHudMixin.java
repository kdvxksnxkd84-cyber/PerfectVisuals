package com.example.client.mixin;

import com.example.client.PvpMenuScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class ItemCooldownHudMixin {

    // В 1.21 метод render принимает GuiGraphics и DeltaTracker
    @Inject(method = "render", at = @At("TAIL"))
    private void renderItemCooldowns(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!PvpMenuScreen.itemCooldownEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Список предметов для отслеживания
        Item[] trackItems = {
            Items.ENDER_PEARL,
            Items.CHORUS_FRUIT,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.SHIELD
        };

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Позиция: правее центра экрана
        int startX = screenWidth / 2 + 15;
        int y = screenHeight / 2 - 10;

        for (Item item : trackItems) {
            // Получаем float значение из нового DeltaTracker'а
            float tickDelta = deltaTracker.getGameTimeDeltaTicks();
            float cooldown = mc.player.getCooldowns().getCooldownPercent(item, tickDelta);
            
            if (cooldown > 0.0f) {
                ItemStack stack = new ItemStack(item);

                // Отрисовка самой иконки предмета
                guiGraphics.renderItem(stack, startX, y);

                // Цвет текста
                int r = (int) (255 * cooldown);
                int g = (int) (255 * (1.0f - cooldown));
                int color = (0xFF << 24) | (r << 16) | (g << 8) | 0x00;

                // Проценты
                String text = String.format("%d%%", (int)(cooldown * 100));
                
                // Текст с тенью
                guiGraphics.drawString(mc.font, text, startX + 20, y + 4, color, true);

                y += 20;
            }
        }
    }
}