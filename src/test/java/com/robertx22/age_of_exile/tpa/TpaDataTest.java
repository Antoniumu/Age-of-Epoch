package com.robertx22.age_of_exile.tpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Логика зарядов TPA без привязки к миру Minecraft и NBT:
 * лимит 10, заряд тратится только при успешном использовании,
 * восстанавливается 1 заряд каждые 10 минут по реальному времени.
 */
class TpaDataTest {

    private static long now() {
        return System.currentTimeMillis();
    }

    @Test
    @DisplayName("новый игрок начинает с полных 10 зарядов")
    void startsWithFullCharges() {
        TpaData data = new TpaData();
        assertEquals(TpaData.MAX_CHARGES, data.getCharges(now()));
    }

    @Test
    @DisplayName("нельзя потратить больше зарядов, чем есть")
    void cannotConsumeMoreThanAvailable() {
        TpaData data = new TpaData();
        long now = now();
        for (int i = TpaData.MAX_CHARGES; i > 0; i--) {
            assertEquals(i, data.getCharges(now));
            assertTrue(data.consumeCharge(now));
        }
        assertEquals(0, data.getCharges(now));
        assertFalse(data.consumeCharge(now));
    }

    @Test
    @DisplayName("один заряд восстанавливается каждые 10 минут")
    void oneChargeRechargesEveryTenMinutes() {
        TpaData data = new TpaData();
        long now = now();
        for (int i = 0; i < TpaData.MAX_CHARGES; i++) {
            assertTrue(data.consumeCharge(now));
        }
        assertEquals(0, data.getCharges(now));
        assertTrue(data.getNextRechargeDelayMillis(now) > 0);

        // Ещё не прошло 10 минут — зарядов нет.
        assertEquals(0, data.getCharges(now + TpaData.RECHARGE_MILLIS - 1));
        // Прошло ровно 10 минут — один заряд на месте.
        assertEquals(1, data.getCharges(now + TpaData.RECHARGE_MILLIS));
    }

    @Test
    @DisplayName("через 100 минут заряды полностью восстанавливаются")
    void rechargesToFullAfter100Minutes() {
        TpaData data = new TpaData();
        long now = now();
        for (int i = 0; i < TpaData.MAX_CHARGES; i++) {
            assertTrue(data.consumeCharge(now));
        }
        long full = now + TpaData.RECHARGE_MILLIS * TpaData.MAX_CHARGES;
        assertEquals(TpaData.MAX_CHARGES, data.getCharges(full));
        assertEquals(0L, data.getNextRechargeDelayMillis(full));
    }
}
