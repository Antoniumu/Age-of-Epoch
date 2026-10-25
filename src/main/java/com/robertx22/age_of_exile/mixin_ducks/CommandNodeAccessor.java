package com.robertx22.age_of_exile.mixin_ducks;

import com.mojang.brigadier.Command;

import java.util.function.Predicate;

/**
 * Доступ к полям Brigadier-узла, которых нет в публичном API:
 * требованию (кто может пользоваться командой) и обработчику узла.
 *
 * <p>Реализуется миксином {@code CommandNodeMixin}: он добавляет эти сеттеры прямо
 * в {@link com.mojang.brigadier.tree.CommandNode}. Нужен, чтобы ванильный {@code /tp}
 * можно было открыть обычным игрокам и подменить ему обработчики (см. TpCommandBridge).</p>
 */
public interface CommandNodeAccessor {

    /** Меняет требование узла (в Brigadier поле {@code requirement} — final). */
    @SuppressWarnings("rawtypes")
    void tpa$setRequirement(Predicate requirement);

    /** Меняет обработчик узла (в Brigadier поле {@code command}). */
    @SuppressWarnings("rawtypes")
    void tpa$setCommand(Command command);
}
