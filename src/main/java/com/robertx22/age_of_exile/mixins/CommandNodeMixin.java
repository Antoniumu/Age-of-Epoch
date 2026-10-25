package com.robertx22.age_of_exile.mixins;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.CommandNode;
import com.robertx22.age_of_exile.mixin_ducks.CommandNodeAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

/**
 * Brigadier не даёт менять требование и обработчик уже построенного узла:
 * поле {@code requirement} — final, а у {@code command} нет сеттера.
 * Добавляем сеттеры, чтобы ванильный {@code /tp} можно было перенастроить
 * после его регистрации (см. TpCommandBridge).
 */
@Mixin(CommandNode.class)
public interface CommandNodeMixin extends CommandNodeAccessor {

    @Override
    @Mutable
    @Accessor("requirement")
    @SuppressWarnings("rawtypes")
    void tpa$setRequirement(Predicate requirement);

    @Override
    @Mutable
    @Accessor("command")
    @SuppressWarnings("rawtypes")
    void tpa$setCommand(Command command);
}
