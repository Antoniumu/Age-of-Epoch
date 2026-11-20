package com.robertx22.age_of_exile.uncommon.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Маркер «клик дошёл до сервера» для e2e-проверки GUI.
 *
 * <p>Пакеты-обработчики кнопок (статы, заклинания, таланты, Вознесение) зовут
 * {@link #log(String)}: в лог клиента падает строка {@code AOEGUI packet=... ok=...}.
 * Сценарий {@code tools/e2e/scenarios/gui.txt} кликает по кнопке настоящей мышью
 * и ждёт эту строку — так доказывается, что клик прошёл весь путь:
 * мышь → виджет → C2S-пакет → серверная логика.</p>
 *
 * <p>Громкость минимальна: строка пишется только когда игрок реально кликает GUI,
 * в обычной игре это единицы строк в минуту.</p>
 */
public final class GuiPacketLog {

    public static final String TAG = "AOEGUI";

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-GuiPackets");

    private GuiPacketLog() {
    }

    public static void log(String what) {
        LOGGER.info("{} {}", TAG, what);
    }
}
