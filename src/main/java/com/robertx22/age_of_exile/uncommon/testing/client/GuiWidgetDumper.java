package com.robertx22.age_of_exile.uncommon.testing.client;

import com.robertx22.age_of_exile.gui.bases.IGuiDebugDump;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Дампер виджетов открытого GUI-экрана — «глаза» e2e-сценария gui.txt.
 *
 * <p>Зачем: сценарий должен кликать кнопки настоящей мышью (xdotool), но зашитые
 * пиксели ломаются от любого сдвига вёрстки. Вместо этого клиент сам пишет
 * {@code run/gui-widgets.txt}: какой экран открыт, масштаб GUI и все виджеты
 * с координатами, подписями и классами. Сценарий читает файл и кликает в центр
 * найденного виджета — тест переживает передвижку кнопок.</p>
 *
 * <p>Активируется флаг-файлом {@code run/aoe-gui-dump.flag} (его кладёт сценарий;
 * без флага дампер ничего не делает и в обычной игре не существует). При смене
 * экрана пишет в лог строку {@code AOEGUI_SCREEN <класс> widgets=N} — по ней
 * сценарий понимает, что экран открылся.</p>
 *
 * <p>Кроме обычных кнопок дампит «отладочные цели» экранов с
 * {@link IGuiDebugDump} — например, узлы дерева Вознесения, которые кнопками
 * не являются и обрабатывают клики в mouseClicked.</p>
 */
public final class GuiWidgetDumper {

    public static final String TAG = "AOEGUI_SCREEN";
    /** Флаг-файл: есть — дампер работает. Кладёт CI-сценарий. */
    public static final String FLAG_FILE = "aoe-gui-dump.flag";
    private static final String OUT_FILE = "gui-widgets.txt";

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-GuiDump");

    /** Как часто перечитывать экран (тиков): виджеты могут появиться без смены экрана. */
    private static final int DUMP_EVERY_TICKS = 5;

    private static String lastSignature = "";
    private static int ticks = 0;

    private GuiWidgetDumper() {
    }

    /** Вызывается из клиентского тика (см. Client.register). */
    public static void onEndTick(Minecraft mc) {
        if (++ticks % DUMP_EVERY_TICKS != 0) {
            return;
        }
        if (!flagPresent()) {
            lastSignature = "";
            return;
        }
        try {
            dump(mc);
        } catch (Throwable error) {
            // Дампер не должен ломать игру: максимум — предупреждение раз в прогон.
            LOGGER.warn("{} дамп не удался: {}", TAG, error.toString());
        }
    }

    private static Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    private static boolean flagPresent() {
        return Files.exists(gameDir().resolve(FLAG_FILE));
    }

    private static void dump(Minecraft mc) {
        net.minecraft.client.gui.screens.Screen screen = mc.gui.screen();
        String screenName = screen == null ? "" : screen.getClass().getSimpleName();
        double scale = mc.getWindow().getGuiScale();

        StringBuilder json = new StringBuilder();
        json.append("{\"screen\":\"").append(esc(screenName)).append("\"");
        json.append(",\"title\":\"").append(esc(screen == null || screen.getTitle() == null
            ? "" : screen.getTitle().getString())).append("\"");
        json.append(",\"scale\":").append(String.format(java.util.Locale.ROOT, "%.3f", scale));
        json.append(",\"widgets\":[");

        List<String> widgets = new ArrayList<>();
        if (screen instanceof com.robertx22.forge_compat.client.Screen compat) {
            // Экраны мода держат кнопки в своём списке (см. forge_compat Screen).
            for (com.robertx22.forge_compat.client.Button b : new ArrayList<>(compat.buttons)) {
                if (b == null || !b.visible) {
                    continue;
                }
                widgets.add(widgetJson(b.getClass().getSimpleName(),
                    b.getMessage() == null ? "" : b.getMessage().getString(),
                    b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.active));
            }
        } else if (screen != null) {
            // Ванильные экраны (создание мира и т.п.): виджеты — в children().
            for (GuiEventListener child : new ArrayList<>(screen.children())) {
                if (child instanceof AbstractWidget w && w.visible) {
                    widgets.add(widgetJson(w.getClass().getSimpleName(),
                        w.getMessage() == null ? "" : w.getMessage().getString(),
                        w.getX(), w.getY(), w.getWidth(), w.getHeight(), w.active));
                }
            }
        }
        if (screen instanceof IGuiDebugDump debug) {
            List<IGuiDebugDump.DebugTarget> targets = new ArrayList<>();
            debug.collectDebugTargets(targets);
            for (IGuiDebugDump.DebugTarget t : targets) {
                widgets.add("{\"class\":\"DebugTarget\",\"id\":\"" + esc(t.id)
                    + "\",\"x\":" + t.x + ",\"y\":" + t.y + ",\"w\":" + t.w
                    + ",\"h\":" + t.h + ",\"active\":true}");
            }
        }
        json.append(String.join(",", widgets));
        json.append("]}");

        String signature = screenName + "|" + widgets.size() + "|" + json.length();
        // Экран объявляем при смене имени/состава — сценарий ждёт эту строку.
        if (!signature.equals(lastSignature)) {
            LOGGER.info("{} {} widgets={}", TAG, screenName.isEmpty() ? "(нет)" : screenName, widgets.size());
            lastSignature = signature;
        }
        try {
            Files.write(gameDir().resolve(OUT_FILE),
                (json + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private static String widgetJson(String cls, String label, int x, int y, int w, int h, boolean active) {
        return "{\"class\":\"" + esc(cls) + "\",\"label\":\"" + esc(label)
            + "\",\"x\":" + x + ",\"y\":" + y + ",\"w\":" + w + ",\"h\":" + h
            + ",\"active\":" + active + "}";
    }

    /** Экранирует строку для JSON: кавычки и слэши. Цветовые §-коды срезает. */
    private static String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("§", "")
            .replace("\\", "\\\\")
            .replace("\"", "'")
            .replace("\n", " ")
            .trim();
    }
}
