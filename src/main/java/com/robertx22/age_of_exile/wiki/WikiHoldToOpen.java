package com.robertx22.age_of_exile.wiki;

import com.robertx22.age_of_exile.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * «Удерживай, чтобы открыть в вики»: наведение на предмет + удержание
 * клавиши {@link KeybindsRegister#WIKI_HOLD_KEY} (по умолчанию B,
 * переназначается в настройках управления).
 *
 * <p>Прямо в тултипе предмета заполняется полоса удержания; когда она
 * полная, открытие экрана ставится в очередь и выполняется из клиентского
 * тика ({@link #consumePendingOpen()} вызывается из OnKeyPress) — экраны
 * нельзя безопасно менять посреди рендера тултипа.
 *
 * <p>Куда открываем:
 * предмет мода → его статья; снаряжение с данными → гайд по снаряжению;
 * всё остальное (например, ванильный ингредиент) → вики с поиском по ID,
 * где видны страницы рецептов с этим предметом.
 *
 * <p>Класс клиентский: вызывается только из TooltipMethod на клиенте,
 * все обращения к клавишам и каталогу защищены от исключений.
 */
public final class WikiHoldToOpen {

    /** Сколько держать клавишу, миллисекунд. */
    public static final long HOLD_MS = 650;
    private static final int BAR_SEGMENTS = 12;
    /** Пауза между кадрами тултипа, после которой удержание считается прерванным. */
    private static final long SESSION_GAP_MS = 350;

    private static String sessionKey = null;
    private static long holdStartedAt = 0;
    private static long lastFrameAt = 0;
    private static boolean latchedUntilRelease = false;

    private static String pendingPageId = null;
    private static String pendingSearch = null;

    private WikiHoldToOpen() {
    }

    /** Результат разрешения предмета в цель вики. */
    private static final class Target {
        final String pageId;   // точная страница или null
        final String search;   // запрос поиска или null
        final String key;      // ключ сессии удержания

        Target(String pageId, String search, String key) {
            this.pageId = pageId;
            this.search = search;
            this.key = key;
        }
    }

    private static Target resolve(ItemStack stack) {
        try {
            if (Gear.Load(stack) != null && WikiCatalog.exists("guides/gear")) {
                return new Target("guides/gear", null, "page:guides/gear");
            }
        } catch (Throwable ignored) {
        }
        try {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) {
                return null;
            }
            String slug = id.getPath().replace('/', '_');
            if (!"mmorpg".equals(id.getNamespace())) {
                slug = id.getNamespace() + "_" + slug;
            }
            if (WikiCatalog.exists("items/" + slug)) {
                return new Target("items/" + slug, null, "page:items/" + slug);
            }
            if (WikiCatalog.exists("blocks/" + slug)) {
                return new Target("blocks/" + slug, null, "page:blocks/" + slug);
            }
            String ns = id.getNamespace();
            if ("mmorpg".equals(ns) || "divine_missions".equals(ns) || "infinite_dungeons".equals(ns)) {
                // предмет мода, но страницы ещё нет (каталог не перегенерирован)
                return new Target(WikiCatalog.HOME_ID, null, "page:" + WikiCatalog.HOME_ID);
            }
            return new Target(null, id.toString(), "search:" + id.toString());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean keyHeld() {
        try {
            return KeybindsRegister.WIKI_HOLD_KEY != null && KeybindsRegister.WIKI_HOLD_KEY.isDown();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String keyLabel() {
        try {
            String label = KeybindsRegister.WIKI_HOLD_KEY.getTranslatedKeyMessage().getString();
            return label == null || label.length() == 0 ? "B" : label;
        } catch (Throwable ignored) {
            return "B";
        }
    }

    private static String bar(double progress) {
        int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * BAR_SEGMENTS);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BAR_SEGMENTS; i++) {
            sb.append(i < filled ? '█' : '░');
        }
        return sb.toString();
    }

    /**
     * Вызывается при каждом построении тултипа (TooltipMethod). Добавляет
     * подсказку или полосу удержания и, когда полоса полная, ставит открытие
     * вики в очередь для следующего клиентского тика.
     */
    public static void appendToTooltip(ItemStack stack, List<Component> tooltip) {
        try {
            if (stack == null || stack.isEmpty() || tooltip == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                return;
            }
            Target target = resolve(stack);
            if (target == null) {
                return;
            }

            long now = System.currentTimeMillis();
            if (!keyHeld()) {
                latchedUntilRelease = false;
                sessionKey = null;
                lastFrameAt = now;
                // подсказку показываем только предметам со своей статьёй,
                // чтобы не шуметь в ванильных тултипах
                if (target.pageId != null) {
                    tooltip.add(Component.literal("[" + keyLabel() + "] удерживать — открыть в Вики Эпохи")
                        .withStyle(ChatFormatting.DARK_GRAY));
                }
                return;
            }

            if (latchedUntilRelease) {
                tooltip.add(progressLine(1.0, true));
                lastFrameAt = now;
                return;
            }

            boolean restart = sessionKey == null || !sessionKey.equals(target.key)
                || now - lastFrameAt > SESSION_GAP_MS;
            if (restart) {
                sessionKey = target.key;
                holdStartedAt = now;
            }
            lastFrameAt = now;
            double progress = (double) (now - holdStartedAt) / (double) HOLD_MS;

            if (progress >= 1.0) {
                latchedUntilRelease = true;
                sessionKey = null;
                pendingPageId = target.pageId;
                pendingSearch = target.search;
                tooltip.add(progressLine(1.0, true));
            } else {
                tooltip.add(progressLine(progress, false));
            }
        } catch (Throwable ignored) {
            // вики-подсказка никогда не должна ломать тултип
        }
    }

    private static Component progressLine(double progress, boolean done) {
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * 100);
        MutableComponent line = Component.literal("Вики Эпохи ")
            .withStyle(ChatFormatting.YELLOW);
        line.append(Component.literal(bar(progress)).withStyle(ChatFormatting.GOLD));
        line.append(Component.literal(done ? " открываю…" : " " + percent + "% — держите " + keyLabel())
            .withStyle(ChatFormatting.GRAY));
        return line;
    }

    /**
     * Если удержание завершилось — открыть вики и вернуть true.
     * Вызывать из клиентского тика (OnKeyPress.onEndTick), а не из рендера.
     */
    public static boolean consumePendingOpen() {
        if (pendingPageId == null && pendingSearch == null) {
            return false;
        }
        String page = pendingPageId;
        String search = pendingSearch;
        pendingPageId = null;
        pendingSearch = null;
        try {
            if (page != null) {
                ModWikiScreen.openAt(page);
            } else {
                ModWikiScreen.openWithSearch(search);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}
