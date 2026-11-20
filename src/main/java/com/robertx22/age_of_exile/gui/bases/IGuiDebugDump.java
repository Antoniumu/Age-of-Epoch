package com.robertx22.age_of_exile.gui.bases;

import java.util.List;

/**
 * Экран может отдать CI-дамперу свои «кликабельные области», которые не являются
 * кнопками (узлы дерева Вознесения и т.п. — они обрабатываются в mouseClicked,
 * а не живут в списке виджетов).
 *
 * <p>Нужно для e2e-сценария {@code tools/e2e/scenarios/gui.txt}: сценарий кликает
 * не по зашитым пикселям, а по реальным прямоугольникам, которые экран сообщает
 * сам — поэтому тест не ломается от сдвига вёрстки.</p>
 */
public interface IGuiDebugDump {

    /**
     * Дополнительные цели для клика: id — по нему матчит сценарий
     * ({@code click_widget node:tide_bolt}), прямоугольник — в gui-координатах.
     */
    void collectDebugTargets(List<DebugTarget> out);

    final class DebugTarget {
        public final String id;
        public final int x;
        public final int y;
        public final int w;
        public final int h;

        public DebugTarget(String id, int x, int y, int w, int h) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
