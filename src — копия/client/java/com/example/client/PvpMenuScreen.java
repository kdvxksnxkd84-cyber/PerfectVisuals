package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import com.example.client.FakePlayerManager; 

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class PvpMenuScreen extends Screen {
    private final long openTime;
    private final float animationDuration = 250.0f; 
    private boolean isClosing = false;
    private long closeStartTime = 0;

    // --- ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ (Настройки) ---
    public static boolean sprintEnabled = false;
    public static boolean aimbotEnabled = false; 
    public static float hitboxScale = 1.0f; 
    public static boolean hitboxesEnabled = false;
    public static int hitboxColor = 0xFFFFFF; 
    public static boolean customFogEnabled = false;
    public static float reachRadius = 3.0f;   
    public static boolean animationsEnabled = true; 
    public static boolean motionBlurEnabled = false; 
    
    // НАСТРОЙКИ KILL AURA
    public static boolean killauraEnabled = false;
    public static float killauraRange = 4.0f;
    public static float killauraCps = 10.0f;
    public static boolean killauraPlayersOnly = true;
    public static boolean killauraAutoCrit = true;
    public static boolean killauraChase = false;
    public static boolean killauraAutoEat = true;
    public static boolean killauraCircleStrafe = false; 
    public static boolean killauraBow = false; 

    // АВТО-ПРИСЕД
    public static boolean autoCrouchCritEnabled = false;

    // КД НА ПРЕДМЕТЫ
    public static boolean itemCooldownEnabled = false;

    // --- НАСТРОЙКИ КАРТИНКИ ---
    public static boolean customImageEnabled = false;
    public static boolean customImageFloatAnim = true;  
    public static boolean customImageLeft = false;      
    public static float customImageOffsetX = 0.0f;      
    public static float customImageOffsetY = 0.0f;      

    // ЧАСТИЦЫ И ИХ ЦВЕТ
    public static boolean worldParticlesEnabled = false;
    public static float hudParticlesHue = 0.5f; 
    public static int hudParticlesColor = 0x3AF27F; 
    
    // Фейк Игрок
    public static boolean fakePlayerEnabled = false;

    // Китайская Шляпа
    public static boolean chinaHatEnabled = false;
    public static int chinaHatColor = 0x3AF27F; 
    public static float chinaHatHue = 0.33f;    

    // Прицел
    public static boolean customCrosshairEnabled = false;
    public static int customCrosshairColor = 0x3AF27F; 
    public static float customCrosshairHue = 0.33f;    
    
    // Обводка Блоков
    public static boolean blockOutlineEnabled = false;
    public static int blockOutlineColor = 0x3AF27F;
    public static float blockOutlineHue = 0.33f;
    public static float blockOutlineThickness = 2.0f;

    // Шлейф игрока
    public static boolean trailEnabled = false;
    public static int trailColor = 0x3AF27F;
    public static float trailHue = 0.33f;
    public static float trailLength = 1.5f; 

    // Маркер сущностей
    public static boolean targetDotEnabled = false;
    public static int targetDotColor = 0xFFFFFF; 
    public static float targetDotHue = 0.0f; 

    // Туман
    public static int fogColor = 0xFFFFFF;
    public static float fogHue = 0.5f; 
    
    // Изменение положения ПРАВОЙ руки
    public static float rightX = 0.0f; 
    public static float rightY = 0.0f; 
    public static float rightZ = 0.0f; 

    // Изменение положения ЛЕВОЙ руки
    public static float leftX = 0.0f; 
    public static float leftY = 0.0f; 
    public static float leftZ = 0.0f; 
    
    public static boolean fullbrightEnabled = false; 
    // -----------------------------------------

    private static final java.io.File CONFIG_FILE = new java.io.File(Minecraft.getInstance().gameDirectory, "config/perfect_visuals.properties");

    static {
        loadConfig(); 
    }

    public enum Category {
        COMBAT("Бой"), RENDER("Визуал"), HUD("Интерфейс"), WORLD("Мир");
        public final String name;
        Category(String name) { this.name = name; }
    }

    public static class Module {
        public String name;
        public Category category;
        public Module(String name, Category category) {
            this.name = name;
            this.category = category;
        }
    }

    private final List<Module> allModules = new ArrayList<>();
    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule = null;

    private boolean draggingReach = false;
    private boolean draggingHitboxScale = false; 
    private boolean draggingDotColor = false; 
    private boolean draggingFogColor = false; 
    private boolean draggingCrosshairColor = false; 
    private boolean draggingChinaHatColor = false; 
    private boolean draggingOutlineColor = false;     
    private boolean draggingOutlineThickness = false; 
    private boolean draggingTrailColor = false; 
    private boolean draggingTrailLength = false; 
    private boolean draggingWorldParticlesColor = false; 
    
    // Слайдеры для Kill Aura
    private boolean draggingKillauraRange = false;
    private boolean draggingKillauraCps = false;

    // Слайдеры правой руки
    private boolean draggingRX = false;
    private boolean draggingRY = false;
    private boolean draggingRZ = false;

    // Слайдеры левой руки
    private boolean draggingLX = false;
    private boolean draggingLY = false;
    private boolean draggingLZ = false;

    // Слайдеры картинки
    private boolean draggingImgX = false;
    private boolean draggingImgY = false;

    private float scrollAmount = 0.0f;
    private float settingsScrollAmount = 0.0f; // Прокрутка настроек модуля

    private final List<Particle> particles = new ArrayList<>();
    private String searchText = "";
    private boolean isSearchFocused = false;

    public PvpMenuScreen() {
        super(Component.literal("Perfect Visuals"));
        this.openTime = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) particles.add(new Particle());

        // Категория: БОЙ
        allModules.add(new Module("Kill Aura", Category.COMBAT));
        allModules.add(new Module("КД на предметы", Category.COMBAT));
        allModules.add(new Module("Авто-Спринт", Category.COMBAT));
        allModules.add(new Module("Aimbot", Category.COMBAT)); 
        allModules.add(new Module("Увеличение хитбоксов", Category.COMBAT)); 
        allModules.add(new Module("Reach", Category.COMBAT));
        allModules.add(new Module("Авто-Присед (Крит)", Category.COMBAT));
        
        // Категория: ВИЗУАЛ
        allModules.add(new Module("Фейк Игрок", Category.RENDER));
        allModules.add(new Module("Частицы в мире", Category.RENDER));
        allModules.add(new Module("Отрисовка Хитбоксов", Category.RENDER));
        allModules.add(new Module("Китайская Шляпа", Category.RENDER));
        allModules.add(new Module("Обводка блоков", Category.RENDER)); 
        allModules.add(new Module("Шлейф игрока", Category.RENDER)); 
        allModules.add(new Module("Размытие камеры", Category.RENDER));
        allModules.add(new Module("Маркер сущностей", Category.RENDER));

        // Категория: ИНТЕРФЕЙС
        allModules.add(new Module("Кастомная Картинка", Category.HUD));
        allModules.add(new Module("Анимации GUI", Category.HUD));
        allModules.add(new Module("PvP Прицел", Category.HUD));
        allModules.add(new Module("Положение рук", Category.HUD)); 
        
        // Категория: МИР
        allModules.add(new Module("Кастомный Туман", Category.WORLD));
        allModules.add(new Module("Яркость (Fullbright)", Category.WORLD));

        updateSelectedModule();
    }

    private void updateSelectedModule() {
        if (searchText.isEmpty()) {
            for (Module m : allModules) {
                if (m.category == selectedCategory) {
                    selectedModule = m;
                    settingsScrollAmount = 0.0f; 
                    return;
                }
            }
        } else {
            for (Module m : allModules) {
                if (m.name.toLowerCase().contains(searchText.toLowerCase())) {
                    selectedModule = m;
                    settingsScrollAmount = 0.0f; 
                    return;
                }
            }
        }
        selectedModule = null;
        settingsScrollAmount = 0.0f;
    }

    private int getModuleSettingsHeight(String moduleName) {
        switch (moduleName) {
            case "Kill Aura":
                return 245; 
            case "Кастомная Картинка":
                return 165; 
            case "Положение рук":
                return 185; 
            case "Обводка блоков":
                return 115;
            case "Шлейф игрока":
                return 115;
            case "Отрисовка Хитбоксов":
                return 95;
            case "Частицы в мире":
                return 95;
            case "Китайская Шляпа":
                return 95;
            case "PvP Прицел":
                return 95;
            case "Кастомный Туман":
                return 95;
            default:
                return 40; 
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int menuWidth = 480;
        int x = (this.width - menuWidth) / 2;
        int modX = x + 130;
        int setX = modX + 140;
        int setWidth = menuWidth - (setX - x) - 10;

        if (mouseX >= modX && mouseX <= modX + 130) {
            int visibleModsCount = 0;
            for (Module mod : allModules) {
                boolean matchesSearch = searchText.isEmpty() ? (mod.category == selectedCategory) : mod.name.toLowerCase().contains(searchText.toLowerCase());
                if (matchesSearch) visibleModsCount++;
            }

            int maxVisible = 7;
            if (visibleModsCount > maxVisible) {
                int maxScroll = (visibleModsCount - maxVisible) * 30;
                scrollAmount = (float) Math.max(0, Math.min(maxScroll, scrollAmount - scrollY * 15));
            } else {
                scrollAmount = 0;
            }
            return true;
        }

        if (selectedModule != null && mouseX >= setX && mouseX <= setX + setWidth) {
            int totalHeight = getModuleSettingsHeight(selectedModule.name);
            int viewportHeight = 205; 

            if (totalHeight > viewportHeight) {
                int maxScroll = totalHeight - viewportHeight;
                settingsScrollAmount = (float) Math.max(0, Math.min(maxScroll, settingsScrollAmount - scrollY * 15));
            } else {
                settingsScrollAmount = 0; 
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        float easeProgress;

        if (!animationsEnabled) {
            if (isClosing) {
                super.onClose();
                return;
            }
            easeProgress = 1.0f;
        } else {
            if (isClosing) {
                float closeProgress = Math.min(1.0f, (System.currentTimeMillis() - closeStartTime) / animationDuration);
                easeProgress = 1.0f - (closeProgress * closeProgress * closeProgress); 
                if (closeProgress >= 1.0f) { super.onClose(); return; }
            } else {
                float openProgress = Math.min(1.0f, (System.currentTimeMillis() - openTime) / animationDuration);
                float openX = 1.0f - openProgress;
                easeProgress = 1.0f - (openX * openX * openX); 
            }
        }

        int alpha = (int) (255 * Math.max(0.0f, Math.min(1.0f, easeProgress)));

        if (alpha > 50) {
            for (Particle p : particles) {
                p.update(this.width, this.height);
                p.render(guiGraphics, alpha);
            }
        }

        guiGraphics.pose().pushPose();
        
        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;
        
        guiGraphics.pose().translate(centerX, centerY, 0.0f);
        float scale = 0.85f + 0.15f * easeProgress; 
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.pose().translate(-centerX, -centerY, 0.0f);

        int menuWidth = 480, menuHeight = 280;
        int x = (this.width - menuWidth) / 2; 
        int y = (this.height - menuHeight) / 2;

        drawGlow(guiGraphics, x, y, menuWidth, menuHeight, alpha, 0x1A0C2E);
        
        int menuBgColor = ((int)(alpha * 0.9f) << 24) | 0x090A0F; 
        drawSmoothRoundedRect(guiGraphics, x, y, menuWidth, menuHeight, 10, menuBgColor);
        
        float hue = (System.currentTimeMillis() % 6000) / 6000f;
        int chromaColor1 = Color.HSBtoRGB(hue, 0.8f, 0.9f);
        int chromaColor2 = Color.HSBtoRGB((hue + 0.25f) % 1.0f, 0.8f, 0.9f);
        drawSmoothGradientRect(guiGraphics, x, y, menuWidth, 4, 2, (alpha << 24) | (chromaColor1 & 0x00FFFFFF), (alpha << 24) | (chromaColor2 & 0x00FFFFFF));

        guiGraphics.drawString(this.font, "PERFECT VISUALS", x + 15, y + 15, (alpha << 24) | 0xFFFFFF, true);
        drawSearchBar(guiGraphics, x + menuWidth - 120, y + 10, 105, 18, alpha);
        guiGraphics.fill(x + 10, y + 35, x + menuWidth - 10, y + 36, (alpha << 24) | 0x1E2030);

        int catX = x + 10;
        int currentY = y + 45;
        
        if (searchText.isEmpty()) {
            for (Category cat : Category.values()) {
                boolean isSelected = cat == selectedCategory;
                int bgColor = isSelected ? 0x2A2D40 : 0x12131C;
                int textColor = isSelected ? 0xFFFFFF : 0x888899;
                
                drawSmoothRoundedRect(guiGraphics, catX, currentY, 110, 25, 4, (alpha << 24) | bgColor);
                guiGraphics.drawString(this.font, cat.name, catX + 10, currentY + 8, (alpha << 24) | textColor, false);
                currentY += 30;
            }
        } else {
            drawSmoothRoundedRect(guiGraphics, catX, currentY, 110, 25, 4, (alpha << 24) | 0x1D2C1F);
            guiGraphics.drawString(this.font, "🔍 Поиск...", catX + 10, currentY + 8, (alpha << 24) | 0x3AF27F, false);
        }

        int modX = catX + 120;
        int listTop = y + 42;
        int listBottom = y + menuHeight - 15;

        guiGraphics.enableScissor(modX, listTop, modX + 135, listBottom);

        int modY = listTop - (int) scrollAmount;
        for (Module mod : allModules) {
            boolean matchesSearch = searchText.isEmpty() ? (mod.category == selectedCategory) : mod.name.toLowerCase().contains(searchText.toLowerCase());
            
            if (matchesSearch) {
                boolean isSelected = mod == selectedModule;
                int bgColor = isSelected ? 0x3A4D80 : 0x1A1C28;
                int textColor = isSelected ? 0xFFFFFF : 0xAAAAAA;

                if (modY + 25 >= listTop && modY <= listBottom) {
                    drawSmoothRoundedRect(guiGraphics, modX, modY, 130, 25, 4, (alpha << 24) | bgColor);
                    guiGraphics.drawString(this.font, mod.name, modX + 10, modY + 8, (alpha << 24) | textColor, false);
                }
                modY += 30;
            }
        }
        guiGraphics.disableScissor();

        int visibleModsCount = 0;
        for (Module mod : allModules) {
            boolean matchesSearch = searchText.isEmpty() ? (mod.category == selectedCategory) : mod.name.toLowerCase().contains(searchText.toLowerCase());
            if (matchesSearch) visibleModsCount++;
        }
        int maxVisible = 7;
        if (visibleModsCount > maxVisible) {
            int maxScroll = (visibleModsCount - maxVisible) * 30;
            float scrollRatio = scrollAmount / maxScroll;
            int barHeight = (int) ((float) (listBottom - listTop) * ((float) maxVisible / visibleModsCount));
            int barY = listTop + (int) ((listBottom - listTop - barHeight) * scrollRatio);
            
            drawSmoothRoundedRect(guiGraphics, modX + 132, listTop, 3, listBottom - listTop, 1, (alpha << 24) | 0x10FFFFFF);
            drawSmoothRoundedRect(guiGraphics, modX + 132, barY, 3, barHeight, 1, (alpha << 24) | 0x60FFFFFF);
        }

        int setX = modX + 140;
        int setWidth = menuWidth - (setX - x) - 10;
        guiGraphics.fill(setX - 10, y + 45, setX - 9, y + menuHeight - 10, (alpha << 24) | 0x1E2030); 

        if (selectedModule != null) {
            guiGraphics.drawString(this.font, "Настройки: " + selectedModule.name, setX, y + 45, (alpha << 24) | 0x3AF27F, false);
            
            int setTop = y + 60;
            int setBottom = y + menuHeight - 15;
            int viewportHeight = setBottom - setTop; // 205 пикселей

            int totalHeight = getModuleSettingsHeight(selectedModule.name);
            boolean needsScroll = totalHeight > viewportHeight;

            // Включаем Scissor ТОЛЬКО если нужен скролл. Правую границу уводим вбок (x + menuWidth), чтобы длинный текст не обрезался!
            if (needsScroll) {
                guiGraphics.enableScissor(setX - 15, setTop, x + menuWidth - 5, setBottom);
            }

            // Координата Y смещается только если активен скролл
            int setY = y + 70 - (int) settingsScrollAmount;

            switch (selectedModule.name) {
                case "Kill Aura":
                    guiGraphics.drawString(this.font, "Включить Kill Aura", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, killauraEnabled, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Радиус атаки", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, killauraRange, 3.0f, 6.0f, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Скорость (CPS)", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, killauraCps, 1.0f, 20.0f, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Только игроки", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, killauraPlayersOnly, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Авто-Криты", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, killauraAutoCrit, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Преследование цели", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, killauraChase, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Авто-Еда", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, killauraAutoEat, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Кружение вокруг цели", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, killauraCircleStrafe, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Авто-Лук (Bow Aura)", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, killauraBow, alpha);
                    break;
                case "Кастомная Картинка":
                    guiGraphics.drawString(this.font, "Включить картинку", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, customImageEnabled, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Левитация (Анимация)", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, customImageFloatAnim, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Позиция: Слева", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, customImageLeft, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Смещение X", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, customImageOffsetX, -300.0f, 300.0f, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Смещение Y", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, customImageOffsetY, -300.0f, 300.0f, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Файл: config/", setX, setY, (alpha << 24) | 0xAAAAAA, false);
                    guiGraphics.drawString(this.font, "perfect_visuals.png", setX, setY + 12, (alpha << 24) | 0x3AF27F, false);
                    break;
                case "КД на предметы":
                    guiGraphics.drawString(this.font, "Отображать КД на экране", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, itemCooldownEnabled, alpha);
                    break;
                case "Авто-Спринт":
                    guiGraphics.drawString(this.font, "Включить спринт", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, sprintEnabled, alpha);
                    break;
                case "Aimbot": 
                    guiGraphics.drawString(this.font, "Авто-наведение", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, aimbotEnabled, alpha);
                    break;
                case "Авто-Присед (Крит)":
                    guiGraphics.drawString(this.font, "Приседать при крите", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, autoCrouchCritEnabled, alpha);
                    break;
                case "Частицы в мире":
                    guiGraphics.drawString(this.font, "Включить частицы", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, worldParticlesEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Цвет частиц", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRainbowSlider(guiGraphics, mouseX, setY, setX + 90, 100, hudParticlesHue, alpha); 
                    break;
                case "Увеличение хитбоксов":
                    guiGraphics.drawString(this.font, "Множитель размера", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, hitboxScale, 1.0f, 3.0f, alpha);
                    break;
                case "Reach":
                    guiGraphics.drawString(this.font, "Радиус удара", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, reachRadius, 3.0f, 6.0f, alpha);
                    break;
                case "Фейк Игрок":
                    guiGraphics.drawString(this.font, "Призвать фейк игрока", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, fakePlayerEnabled, alpha);
                    break;
                case "Отрисовка Хитбоксов":
                    guiGraphics.drawString(this.font, "Показывать", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, hitboxesEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Цвет", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawColorPicker(guiGraphics, setX + setWidth - 75, setY, 0xFFFFFF, hitboxColor, alpha);
                    drawColorPicker(guiGraphics, setX + setWidth - 55, setY, 0xFF0000, hitboxColor, alpha);
                    drawColorPicker(guiGraphics, setX + setWidth - 35, setY, 0x00FF00, hitboxColor, alpha);
                    break;
                case "Китайская Шляпа": 
                    guiGraphics.drawString(this.font, "Включить шляпу", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, chinaHatEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Цвет шляпы", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRainbowSlider(guiGraphics, mouseX, setY, setX + 90, 100, chinaHatHue, alpha); 
                    break;
                case "Обводка блоков": 
                    guiGraphics.drawString(this.font, "Кастомная обводка", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, blockOutlineEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Цвет обводки", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRainbowSlider(guiGraphics, mouseX, setY, setX + 90, 100, blockOutlineHue, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Толщина линии", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, blockOutlineThickness, 1.0f, 5.0f, alpha);
                    break;
                case "Шлейф игрока": 
                    guiGraphics.drawString(this.font, "Включить шлейф", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, trailEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Цвет шлейфа", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRainbowSlider(guiGraphics, mouseX, setY, setX + 90, 100, trailHue, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Время жизни", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, trailLength, 0.5f, 4.0f, alpha);
                    break;
                case "Размытие камеры": 
                    guiGraphics.drawString(this.font, "Включить размытие", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, motionBlurEnabled, alpha);
                    break;
                case "Маркер сущностей":
                    guiGraphics.drawString(this.font, "Показывать маркер", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, targetDotEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Палитра цвета", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRainbowSlider(guiGraphics, mouseX, setY, setX + 90, 100, targetDotHue, alpha);
                    break;
                case "Анимации GUI":
                    guiGraphics.drawString(this.font, "Плавное меню", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, animationsEnabled, alpha);
                    break;
                case "PvP Прицел":
                    guiGraphics.drawString(this.font, "Точка вместо креста", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, customCrosshairEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Цвет прицела", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false); 
                    drawRainbowSlider(guiGraphics, mouseX, setY, setX + 90, 100, customCrosshairHue, alpha); 
                    break;
                case "Положение рук": 
                    guiGraphics.drawString(this.font, "Пр. Рука X", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, rightX, -1.0f, 1.0f, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Пр. Рука Y", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, rightY, -1.0f, 1.0f, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Пр. Рука Z", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, rightZ, -1.0f, 1.0f, alpha);
                    setY += 30; 
                    guiGraphics.drawString(this.font, "Лев. Рука X", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, leftX, -1.0f, 1.0f, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Лев. Рука Y", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, leftY, -1.0f, 1.0f, alpha);
                    setY += 25;
                    guiGraphics.drawString(this.font, "Лев. Рука Z", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedSlider(guiGraphics, mouseX, setY, setX + 90, 100, leftZ, -1.0f, 1.0f, alpha);
                    break;
                case "Кастомный Туман":
                    guiGraphics.drawString(this.font, "Кастомный туман", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, customFogEnabled, alpha);
                    setY += 30;
                    guiGraphics.drawString(this.font, "Цвет тумана", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRainbowSlider(guiGraphics, mouseX, setY, setX + 90, 100, fogHue, alpha);
                    break;
                case "Яркость (Fullbright)":
                    guiGraphics.drawString(this.font, "Яркость в темноте", setX, setY + 3, (alpha << 24) | 0xFFFFFF, false);
                    drawRoundedToggle(guiGraphics, setX + setWidth - 35, setY, fullbrightEnabled, alpha);
                    break;
            }

            if (needsScroll) {
                guiGraphics.disableScissor();
            }

            // Отрисовываем ползунок прокрутки для настроек только если высота контента действительно больше viewport
            if (totalHeight > viewportHeight) {
                float scrollRatio = settingsScrollAmount / (totalHeight - viewportHeight);
                int barHeight = (int) (viewportHeight * ((float) viewportHeight / totalHeight));
                int barY = setTop + (int) ((viewportHeight - barHeight) * scrollRatio);
                
                drawSmoothRoundedRect(guiGraphics, setX + setWidth - 3, setTop, 3, viewportHeight, 1, (alpha << 24) | 0x10FFFFFF);
                drawSmoothRoundedRect(guiGraphics, setX + setWidth - 3, barY, 3, barHeight, 1, (alpha << 24) | 0x60FFFFFF);
            }

        } else {
            guiGraphics.drawString(this.font, "Выберите мод", setX, y + 45, (alpha << 24) | 0x888899, false);
        }

        guiGraphics.pose().popPose();
    }

    private void updateDotColor(double mouseX, int x, int width) {
        float percent = (float) ((mouseX - x) / width);
        targetDotHue = Math.max(0.0f, Math.min(1.0f, percent));
        targetDotColor = Color.HSBtoRGB(targetDotHue, 1.0f, 1.0f);
    }
    private void updateFogColor(double mouseX, int x, int width) {
        float percent = (float) ((mouseX - x) / width);
        fogHue = Math.max(0.0f, Math.min(1.0f, percent));
        fogColor = Color.HSBtoRGB(fogHue, 1.0f, 1.0f);
    }
    private void updateCrosshairColor(double mouseX, int x, int width) { 
        float percent = (float) ((mouseX - x) / width);
        customCrosshairHue = Math.max(0.0f, Math.min(1.0f, percent));
        customCrosshairColor = Color.HSBtoRGB(customCrosshairHue, 1.0f, 1.0f);
    }
    private void updateChinaHatColor(double mouseX, int x, int width) { 
        float percent = (float) ((mouseX - x) / width);
        chinaHatHue = Math.max(0.0f, Math.min(1.0f, percent));
        chinaHatColor = Color.HSBtoRGB(chinaHatHue, 1.0f, 1.0f);
    }
    private void updateOutlineColor(double mouseX, int x, int width) { 
        float percent = (float) ((mouseX - x) / width);
        blockOutlineHue = Math.max(0.0f, Math.min(1.0f, percent));
        blockOutlineColor = Color.HSBtoRGB(blockOutlineHue, 1.0f, 1.0f);
    }
    private void updateTrailColor(double mouseX, int x, int width) { 
        float percent = (float) ((mouseX - x) / width);
        trailHue = Math.max(0.0f, Math.min(1.0f, percent));
        trailColor = Color.HSBtoRGB(trailHue, 1.0f, 1.0f);
    }
    private void updateWorldParticlesColor(double mouseX, int x, int width) {
        float percent = (float) ((mouseX - x) / width);
        hudParticlesHue = Math.max(0.0f, Math.min(1.0f, percent));
        hudParticlesColor = Color.HSBtoRGB(hudParticlesHue, 1.0f, 1.0f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        
        int menuWidth = 480, menuHeight = 280;
        int x = (this.width - menuWidth) / 2;
        int y = (this.height - menuHeight) / 2;

        int catX = x + 10;
        int modX = catX + 120;
        int setX = modX + 140;
        int setWidth = menuWidth - (setX - x) - 10;

        isSearchFocused = mouseX >= x + menuWidth - 120 && mouseX <= x + menuWidth - 15 && mouseY >= y + 10 && mouseY <= y + 28;

        if (searchText.isEmpty()) {
            int currentY = y + 45;
            for (Category cat : Category.values()) {
                if (mouseX >= catX && mouseX <= catX + 110 && mouseY >= currentY && mouseY <= currentY + 25) {
                    selectedCategory = cat;
                    updateSelectedModule(); 
                    scrollAmount = 0; 
                    return true;
                }
                currentY += 30;
            }
        }

        int modY = y + 42 - (int) scrollAmount;
        for (Module mod : allModules) {
            boolean matchesSearch = searchText.isEmpty() ? (mod.category == selectedCategory) : mod.name.toLowerCase().contains(searchText.toLowerCase());
            if (matchesSearch) {
                if (mouseY >= y + 42 && mouseY <= y + menuHeight - 15) {
                    if (mouseX >= modX && mouseX <= modX + 130 && mouseY >= modY && mouseY <= modY + 25) {
                        selectedModule = mod;
                        settingsScrollAmount = 0.0f; 
                        return true;
                    }
                }
                modY += 30;
            }
        }

        if (selectedModule != null) {
            int setY = y + 70 - (int) settingsScrollAmount;

            if (mouseY >= y + 60 && mouseY <= y + menuHeight - 15) {
                switch (selectedModule.name) {
                    case "Kill Aura":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) killauraEnabled = !killauraEnabled;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingKillauraRange = true;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingKillauraCps = true;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) killauraPlayersOnly = !killauraPlayersOnly;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) killauraAutoCrit = !killauraAutoCrit;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) killauraChase = !killauraChase;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) killauraAutoEat = !killauraAutoEat;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) killauraCircleStrafe = !killauraCircleStrafe;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) killauraBow = !killauraBow;
                        break;
                    case "Кастомная Картинка":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) customImageEnabled = !customImageEnabled;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY + 25, 28, 12)) customImageFloatAnim = !customImageFloatAnim;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY + 50, 28, 12)) customImageLeft = !customImageLeft;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 75 + 13, 100, 10)) draggingImgX = true;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 100 + 13, 100, 10)) draggingImgY = true;
                        break;
                    case "КД на предметы":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) itemCooldownEnabled = !itemCooldownEnabled;
                        break;
                    case "Авто-Спринт":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) sprintEnabled = !sprintEnabled;
                        break;
                    case "Aimbot": 
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) aimbotEnabled = !aimbotEnabled;
                        break;
                    case "Авто-Присед (Крит)":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) autoCrouchCritEnabled = !autoCrouchCritEnabled;
                        break;
                    case "Частицы в мире":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) worldParticlesEnabled = !worldParticlesEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingWorldParticlesColor = true;
                            updateWorldParticlesColor(mouseX, setX + 90, 100);
                        }
                        break;
                    case "Увеличение хитбоксов":
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingHitboxScale = true;
                        break;
                    case "Reach":
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingReach = true;
                        break;
                    case "Фейк Игрок":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) {
                            fakePlayerEnabled = !fakePlayerEnabled;
                            FakePlayerManager.toggleFakePlayer(fakePlayerEnabled);
                        }
                        break;
                    case "Отрисовка Хитбоксов":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) hitboxesEnabled = !hitboxesEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 75, setY + 10, 11, 11)) hitboxColor = 0xFFFFFF;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 55, setY + 10, 11, 11)) hitboxColor = 0xFF0000;
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY + 10, 11, 11)) hitboxColor = 0x00FF00;
                        break;
                    case "Китайская Шляпа": 
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) chinaHatEnabled = !chinaHatEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingChinaHatColor = true;
                            updateChinaHatColor(mouseX, setX + 90, 100);
                        }
                        break;
                    case "Обводка блоков": 
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) blockOutlineEnabled = !blockOutlineEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingOutlineColor = true;
                            updateOutlineColor(mouseX, setX + 90, 100);
                        }
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingOutlineThickness = true;
                        }
                        break;
                    case "Шлейф игрока": 
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) trailEnabled = !trailEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingTrailColor = true;
                            updateTrailColor(mouseX, setX + 90, 100);
                        }
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingTrailLength = true;
                        }
                        break;
                    case "Размытие камеры": 
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) motionBlurEnabled = !motionBlurEnabled;
                        break;
                    case "Маркер сущностей":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) targetDotEnabled = !targetDotEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingDotColor = true;
                            updateDotColor(mouseX, setX + 90, 100);
                        }
                        break;
                    case "Анимации GUI":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) animationsEnabled = !animationsEnabled;
                        break;
                    case "PvP Прицел":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) customCrosshairEnabled = !customCrosshairEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) { 
                            draggingCrosshairColor = true;
                            updateCrosshairColor(mouseX, setX + 90, 100);
                        }
                        break;
                    case "Положение рук": 
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingRX = true;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingRY = true;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingRZ = true;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingLX = true;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingLY = true;
                        setY += 25;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) draggingLZ = true;
                        break;
                    case "Кастомный Туман":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) customFogEnabled = !customFogEnabled;
                        setY += 30;
                        if (isHovered(mouseX, mouseY, setX + 90, setY + 13, 100, 10)) {
                            draggingFogColor = true;
                            updateFogColor(mouseX, setX + 90, 100);
                        }
                        break;
                    case "Яркость (Fullbright)":
                        if (isHovered(mouseX, mouseY, setX + setWidth - 35, setY, 28, 12)) fullbrightEnabled = !fullbrightEnabled;
                        break;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) { 
        draggingReach = false; 
        draggingHitboxScale = false; 
        draggingDotColor = false;
        draggingFogColor = false;
        draggingCrosshairColor = false; 
        draggingChinaHatColor = false;
        draggingOutlineColor = false;     
        draggingOutlineThickness = false; 
        draggingTrailColor = false; 
        draggingTrailLength = false; 
        draggingWorldParticlesColor = false; 
        draggingKillauraRange = false;
        draggingKillauraCps = false;
        draggingRX = false;
        draggingRY = false;
        draggingRZ = false;
        draggingLX = false;
        draggingLY = false;
        draggingLZ = false;
        draggingImgX = false;
        draggingImgY = false;
        return super.mouseReleased(mouseX, mouseY, button); 
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int menuWidth = 480;
        int x = (this.width - menuWidth) / 2;
        int setX = x + 10 + 120 + 140;

        float percent = Math.max(0.0f, Math.min(1.0f, (float) ((mouseX - (setX + 90)) / 100.0f)));

        if (draggingImgX) {
            customImageOffsetX = -300.0f + (percent * 600.0f);
        }
        if (draggingImgY) {
            customImageOffsetY = -300.0f + (percent * 600.0f);
        }
        if (draggingReach) {
            reachRadius = 3.0f + (percent * 3.0f); 
        }
        if (draggingHitboxScale) { 
            hitboxScale = 1.0f + (percent * 2.0f); 
        }
        if (draggingOutlineThickness) { 
            blockOutlineThickness = 1.0f + (percent * 4.0f); 
        }
        if (draggingDotColor) {
            updateDotColor(mouseX, setX + 90, 100);
        }
        if (draggingFogColor) {
            updateFogColor(mouseX, setX + 90, 100);
        }
        if (draggingCrosshairColor) { 
            updateCrosshairColor(mouseX, setX + 90, 100);
        }
        if (draggingChinaHatColor) { 
            updateChinaHatColor(mouseX, setX + 90, 100);
        }
        if (draggingOutlineColor) { 
            updateOutlineColor(mouseX, setX + 90, 100);
        }
        if (draggingTrailColor) { 
            updateTrailColor(mouseX, setX + 90, 100);
        }
        if (draggingWorldParticlesColor) { 
            updateWorldParticlesColor(mouseX, setX + 90, 100);
        }
        if (draggingTrailLength) { 
            trailLength = 0.5f + (percent * 3.5f); 
        }
        if (draggingKillauraRange) {
            killauraRange = 3.0f + (percent * 3.0f);
        }
        if (draggingKillauraCps) {
            killauraCps = 1.0f + (percent * 19.0f);
        }
        
        if (draggingRX) {
            rightX = -1.0f + (percent * 2.0f);
        }
        if (draggingRY) {
            rightY = -1.0f + (percent * 2.0f);
        }
        if (draggingRZ) {
            rightZ = -1.0f + (percent * 2.0f);
        }
        
        if (draggingLX) {
            leftX = -1.0f + (percent * 2.0f);
        }
        if (draggingLY) {
            leftY = -1.0f + (percent * 2.0f);
        }
        if (draggingLZ) {
            leftZ = -1.0f + (percent * 2.0f);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (isSearchFocused) {
            searchText += codePoint;
            updateSelectedModule(); 
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSearchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            }
            updateSelectedModule();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() { 
        if (!isClosing) { 
            if (!animationsEnabled) {
                saveConfig();
                super.onClose();
                return;
            }
            isClosing = true; 
            closeStartTime = System.currentTimeMillis(); 
            saveConfig(); 
        } 
    }
    
    @Override
    public boolean isPauseScreen() { return false; }

    public static void saveConfig() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            java.util.Properties props = new java.util.Properties();
            
            // Параметры Kill Aura
            props.setProperty("killauraEnabled", String.valueOf(killauraEnabled));
            props.setProperty("killauraRange", String.valueOf(killauraRange));
            props.setProperty("killauraCps", String.valueOf(killauraCps));
            props.setProperty("killauraPlayersOnly", String.valueOf(killauraPlayersOnly));
            props.setProperty("killauraAutoCrit", String.valueOf(killauraAutoCrit));
            props.setProperty("killauraChase", String.valueOf(killauraChase));
            props.setProperty("killauraAutoEat", String.valueOf(killauraAutoEat));
            props.setProperty("killauraCircleStrafe", String.valueOf(killauraCircleStrafe));
            props.setProperty("killauraBow", String.valueOf(killauraBow)); 

            props.setProperty("customImageEnabled", String.valueOf(customImageEnabled));
            props.setProperty("customImageFloatAnim", String.valueOf(customImageFloatAnim));
            props.setProperty("customImageLeft", String.valueOf(customImageLeft));
            props.setProperty("customImageOffsetX", String.valueOf(customImageOffsetX));
            props.setProperty("customImageOffsetY", String.valueOf(customImageOffsetY));

            props.setProperty("itemCooldownEnabled", String.valueOf(itemCooldownEnabled));
            
            props.setProperty("sprintEnabled", String.valueOf(sprintEnabled));
            props.setProperty("aimbotEnabled", String.valueOf(aimbotEnabled)); 
            props.setProperty("autoCrouchCritEnabled", String.valueOf(autoCrouchCritEnabled));
            props.setProperty("worldParticlesEnabled", String.valueOf(worldParticlesEnabled));
            props.setProperty("hudParticlesHue", String.valueOf(hudParticlesHue)); 
            props.setProperty("hudParticlesColor", String.valueOf(hudParticlesColor));
            props.setProperty("hitboxScale", String.valueOf(hitboxScale)); 
            props.setProperty("hitboxesEnabled", String.valueOf(hitboxesEnabled));
            props.setProperty("hitboxColor", String.valueOf(hitboxColor));
            props.setProperty("fakePlayerEnabled", String.valueOf(fakePlayerEnabled));
            props.setProperty("blockOutlineEnabled", String.valueOf(blockOutlineEnabled));
            props.setProperty("blockOutlineColor", String.valueOf(blockOutlineColor));
            props.setProperty("blockOutlineHue", String.valueOf(blockOutlineHue));
            props.setProperty("blockOutlineThickness", String.valueOf(blockOutlineThickness));
            props.setProperty("chinaHatEnabled", String.valueOf(chinaHatEnabled));
            props.setProperty("chinaHatColor", String.valueOf(chinaHatColor)); 
            props.setProperty("chinaHatHue", String.valueOf(chinaHatHue));     
            props.setProperty("trailEnabled", String.valueOf(trailEnabled));
            props.setProperty("trailColor", String.valueOf(trailColor));
            props.setProperty("trailHue", String.valueOf(trailHue));
            props.setProperty("trailLength", String.valueOf(trailLength));
            props.setProperty("customFogEnabled", String.valueOf(customFogEnabled));
            props.setProperty("fogColor", String.valueOf(fogColor));
            props.setProperty("fogHue", String.valueOf(fogHue));
            props.setProperty("reachRadius", String.valueOf(reachRadius));
            props.setProperty("animationsEnabled", String.valueOf(animationsEnabled));
            props.setProperty("motionBlurEnabled", String.valueOf(motionBlurEnabled));
            props.setProperty("customCrosshairEnabled", String.valueOf(customCrosshairEnabled));
            props.setProperty("customCrosshairColor", String.valueOf(customCrosshairColor)); 
            props.setProperty("customCrosshairHue", String.valueOf(customCrosshairHue)); 
            props.setProperty("targetDotEnabled", String.valueOf(targetDotEnabled));
            props.setProperty("targetDotColor", String.valueOf(targetDotColor));
            props.setProperty("targetDotHue", String.valueOf(targetDotHue));
            props.setProperty("fullbrightEnabled", String.valueOf(fullbrightEnabled));
            
            props.setProperty("rightX", String.valueOf(rightX));
            props.setProperty("rightY", String.valueOf(rightY));
            props.setProperty("rightZ", String.valueOf(rightZ));

            props.setProperty("leftX", String.valueOf(leftX));
            props.setProperty("leftY", String.valueOf(leftY));
            props.setProperty("leftZ", String.valueOf(leftZ));

            try (java.io.FileWriter writer = new java.io.FileWriter(CONFIG_FILE)) {
                props.store(writer, "Perfect Visuals Config");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig() {
        try {
            if (CONFIG_FILE.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileReader reader = new java.io.FileReader(CONFIG_FILE)) {
                    props.load(reader);
                }
                
                // Параметры Kill Aura
                killauraEnabled = Boolean.parseBoolean(props.getProperty("killauraEnabled", "false"));
                killauraRange = Float.parseFloat(props.getProperty("killauraRange", "4.0"));
                killauraCps = Float.parseFloat(props.getProperty("killauraCps", "10.0"));
                killauraPlayersOnly = Boolean.parseBoolean(props.getProperty("killauraPlayersOnly", "true"));
                killauraAutoCrit = Boolean.parseBoolean(props.getProperty("killauraAutoCrit", "true"));
                killauraChase = Boolean.parseBoolean(props.getProperty("killauraChase", "false"));
                killauraAutoEat = Boolean.parseBoolean(props.getProperty("killauraAutoEat", "true"));
                killauraCircleStrafe = Boolean.parseBoolean(props.getProperty("killauraCircleStrafe", "false"));
                killauraBow = Boolean.parseBoolean(props.getProperty("killauraBow", "false")); 

                customImageEnabled = Boolean.parseBoolean(props.getProperty("customImageEnabled", "false"));
                customImageFloatAnim = Boolean.parseBoolean(props.getProperty("customImageFloatAnim", "true"));
                customImageLeft = Boolean.parseBoolean(props.getProperty("customImageLeft", "false"));
                customImageOffsetX = Float.parseFloat(props.getProperty("customImageOffsetX", "0.0"));
                customImageOffsetY = Float.parseFloat(props.getProperty("customImageOffsetY", "0.0"));

                itemCooldownEnabled = Boolean.parseBoolean(props.getProperty("itemCooldownEnabled", "false"));

                sprintEnabled = Boolean.parseBoolean(props.getProperty("sprintEnabled", "false"));
                aimbotEnabled = Boolean.parseBoolean(props.getProperty("aimbotEnabled", "false")); 
                autoCrouchCritEnabled = Boolean.parseBoolean(props.getProperty("autoCrouchCritEnabled", "false"));
                worldParticlesEnabled = Boolean.parseBoolean(props.getProperty("worldParticlesEnabled", "false"));
                hudParticlesHue = Float.parseFloat(props.getProperty("hudParticlesHue", "0.5")); 
                hudParticlesColor = Integer.parseInt(props.getProperty("hudParticlesColor", "3863167"));
                hitboxScale = Float.parseFloat(props.getProperty("hitboxScale", "1.0")); 
                hitboxesEnabled = Boolean.parseBoolean(props.getProperty("hitboxesEnabled", "false"));
                hitboxColor = Integer.parseInt(props.getProperty("hitboxColor", "16777215"));
                fakePlayerEnabled = Boolean.parseBoolean(props.getProperty("fakePlayerEnabled", "false"));
                blockOutlineEnabled = Boolean.parseBoolean(props.getProperty("blockOutlineEnabled", "false"));
                blockOutlineColor = Integer.parseInt(props.getProperty("blockOutlineColor", "3863167")); 
                blockOutlineHue = Float.parseFloat(props.getProperty("blockOutlineHue", "0.33"));
                blockOutlineThickness = Float.parseFloat(props.getProperty("blockOutlineThickness", "2.0"));
                chinaHatEnabled = Boolean.parseBoolean(props.getProperty("chinaHatEnabled", "false"));
                chinaHatColor = Integer.parseInt(props.getProperty("chinaHatColor", "3863167")); 
                chinaHatHue = Float.parseFloat(props.getProperty("chinaHatHue", "0.33"));       
                trailEnabled = Boolean.parseBoolean(props.getProperty("trailEnabled", "false"));
                trailColor = Integer.parseInt(props.getProperty("trailColor", "3863167")); 
                trailHue = Float.parseFloat(props.getProperty("trailHue", "0.33"));
                trailLength = Float.parseFloat(props.getProperty("trailLength", "1.5"));
                customFogEnabled = Boolean.parseBoolean(props.getProperty("customFogEnabled", "false"));
                fogColor = Integer.parseInt(props.getProperty("fogColor", "16777215"));
                fogHue = Float.parseFloat(props.getProperty("fogHue", "0.5"));
                reachRadius = Float.parseFloat(props.getProperty("reachRadius", "3.0"));
                animationsEnabled = Boolean.parseBoolean(props.getProperty("animationsEnabled", "true"));
                motionBlurEnabled = Boolean.parseBoolean(props.getProperty("motionBlurEnabled", "false"));
                customCrosshairEnabled = Boolean.parseBoolean(props.getProperty("customCrosshairEnabled", "false"));
                customCrosshairColor = Integer.parseInt(props.getProperty("customCrosshairColor", "3863167")); 
                customCrosshairHue = Float.parseFloat(props.getProperty("customCrosshairHue", "0.33"));       
                targetDotEnabled = Boolean.parseBoolean(props.getProperty("targetDotEnabled", "false"));
                targetDotColor = Integer.parseInt(props.getProperty("targetDotColor", "16777215"));
                targetDotHue = Float.parseFloat(props.getProperty("targetDotHue", "0.0"));
                fullbrightEnabled = Boolean.parseBoolean(props.getProperty("fullbrightEnabled", "false"));
                
                rightX = Float.parseFloat(props.getProperty("rightX", "0.0"));
                rightY = Float.parseFloat(props.getProperty("rightY", "0.0"));
                rightZ = Float.parseFloat(props.getProperty("rightZ", "0.0"));

                leftX = Float.parseFloat(props.getProperty("leftX", "0.0"));
                leftY = Float.parseFloat(props.getProperty("leftY", "0.0"));
                leftZ = Float.parseFloat(props.getProperty("leftZ", "0.0"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawSearchBar(GuiGraphics gui, int x, int y, int width, int height, int alpha) {
        int bgColor = isSearchFocused ? 0x1E2030 : 0x12131C;
        drawSmoothRoundedRect(gui, x, y, width, height, 4, (alpha << 24) | bgColor);
        
        String displayText = searchText.isEmpty() && !isSearchFocused ? "Поиск..." : searchText + (isSearchFocused && System.currentTimeMillis() % 1000 > 500 ? "_" : "");
        int textColor = searchText.isEmpty() && !isSearchFocused ? 0x565975 : 0xFFFFFF;
        gui.drawString(this.font, displayText, x + 6, y + 5, (alpha << 24) | textColor, false);
    }

    private void drawRainbowSlider(GuiGraphics gui, int mouseX, int y, int x, int width, float value, int alpha) {
        for (int i = 0; i < width; i++) {
            float huePercent = (float) i / width;
            int rainbowColor = Color.HSBtoRGB(huePercent, 1.0f, 1.0f);
            gui.fill(x + i, y + 13, x + i + 1, y + 17, (alpha << 24) | (rainbowColor & 0x00FFFFFF));
        }
        
        int knobX = x + (int) (width * value);
        drawSmoothRoundedRect(gui, knobX - 3, y + 9, 6, 12, 3, (alpha << 24) | 0xFFFFFF);
        
        int selectedColor = Color.HSBtoRGB(value, 1.0f, 1.0f);
        drawSmoothRoundedRect(gui, x - 18, y + 10, 10, 10, 3, (alpha << 24) | (selectedColor & 0x00FFFFFF));
    }

    public static void drawSmoothRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) { gui.fill(x, y, x + width, y + height, color); return; }
        gui.fill(x, y + radius, x + width, y + height - radius, color);
        for (int dy = 0; dy < radius; dy++) {
            int dx = (int) Math.round(radius - Math.sqrt(radius * radius - (radius - dy) * (radius - dy)));
            gui.fill(x + dx, y + dy, x + width - dx, y + dy + 1, color);
        }
        for (int dy = 0; dy < radius; dy++) {
            int dx = (int) Math.round(radius - Math.sqrt(radius * radius - (radius - dy) * (radius - dy)));
            gui.fill(x + dx, y + height - dy - 1, x + width - dx, y + height - dy, color);
        }
    }

    private void drawSmoothGradientRect(GuiGraphics gui, int x, int y, int width, int height, int radius, int color1, int color2) {
        for (int i = 0; i < width; i++) {
            float ratio = (float) i / width;
            int r = (int) (((color1 >> 16) & 0xFF) * (1 - ratio) + ((color2 >> 16) & 0xFF) * ratio);
            int g = (int) (((color1 >> 8) & 0xFF) * (1 - ratio) + ((color2 >> 8) & 0xFF) * ratio);
            int b = (int) (((color1) & 0xFF) * (1 - ratio) + ((color2) & 0xFF) * ratio);
            int a = (int) (((color1 >> 24) & 0xFF) * (1 - ratio) + ((color2 >> 24) & 0xFF) * ratio);
            int color = (a << 24) | (r << 16) | (g << 8) | b;
            int dy = 0;
            if (i < radius) dy = (int) Math.round(radius - Math.sqrt(radius * radius - (radius - i) * (radius - i)));
            else if (i > width - radius) dy = (int) Math.round(radius - Math.sqrt(radius * radius - (i - (width - radius)) * (i - (width - radius))));
            gui.fill(x + i, y + dy, x + i + 1, y + height, color);
        }
    }

    private void drawRoundedToggle(GuiGraphics gui, int x, int y, boolean state, int alpha) {
        int color = state ? 0x3AF27F : 0x242636; 
        drawSmoothRoundedRect(gui, x, y, 28, 12, 6, (alpha << 24) | color); 
        if (state) drawSmoothRoundedRect(gui, x + 16, y + 2, 10, 8, 4, (alpha << 24) | 0xFFFFFF);
        else drawSmoothRoundedRect(gui, x + 2, y + 2, 10, 8, 4, (alpha << 24) | 0x888899);
    }

    private void drawRoundedSlider(GuiGraphics gui, int mouseX, int y, int x, int width, float value, float min, float max, int alpha) {
        drawSmoothRoundedRect(gui, x, y + 13, width, 4, 2, (alpha << 24) | 0x1B1C28); 
        float percent = (value - min) / (max - min);
        int fillWidth = (int) (width * percent);
        drawSmoothRoundedRect(gui, x, y + 13, fillWidth, 4, 2, (alpha << 24) | 0x487CFF); 
        drawSmoothRoundedRect(gui, x + fillWidth - 3, y + 9, 6, 12, 3, (alpha << 24) | 0xFFFFFF); 
        String valStr = max > 100.0f || min < -100.0f ? String.format("%d", (int) value) : String.format("%.1f", value);
        gui.drawString(this.font, valStr, x - 35, y + 11, (alpha << 24) | 0x888899, false);
    }

    private void drawColorPicker(GuiGraphics gui, int x, int y, int color, int currentColor, int alpha) {
        drawSmoothRoundedRect(gui, x, y + 10, 11, 11, 3, (alpha << 24) | color);
        if (color == currentColor) gui.renderOutline(x - 2, y + 8, 15, 15, (alpha << 24) | 0xFFFFFF); 
    }

    private void drawGlow(GuiGraphics gui, int x, int y, int w, int h, int alpha, int glowColor) {
        for (int i = 1; i <= 6; i++) {
            int glowAlpha = (int)((alpha / 255.0f) * (18 - i * 2.5f));
            drawSmoothRoundedRect(gui, x - i, y - i, w + i*2, h + i*2, 10, (glowAlpha << 24) | glowColor);
        }
    }

    private static class Particle {
        float x, y, speedX, speedY, size;
        Particle() {
            x = (float) (Math.random() * 1000); y = (float) (Math.random() * 600);
            speedX = (float) (Math.random() * 0.5 - 0.25); speedY = (float) (Math.random() * -0.5 - 0.1);
            size = (float) (Math.random() * 2 + 1);
        }
        void update(int screenWidth, int screenHeight) {
            x += speedX; y += speedY;
            if (y < -10) y = screenHeight + 10; if (x < -10) x = screenWidth + 10; if (x > screenWidth + 10) x = -10;
        }
        void render(GuiGraphics gui, int alpha) {
            int color = (Math.min(alpha, 100) << 24) | 0xFFFFFF;
            gui.fill((int)x, (int)y, (int)(x + size), (int)(y + size), color);
        }
    }
}