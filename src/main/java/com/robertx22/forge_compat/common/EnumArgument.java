package com.robertx22.forge_compat.common;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EnumArgument<T extends Enum<T>> implements ArgumentType<T> {
    private static final DynamicCommandExceptionType INVALID_ENUM =
        new DynamicCommandExceptionType(v -> Component.literal("Invalid enum value: " + v));

    private final Class<T> enumClass;

    private EnumArgument(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    public static <R extends Enum<R>> EnumArgument<R> enumArgument(Class<R> enumClass) {
        return new EnumArgument<>(enumClass);
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        for (T val : enumClass.getEnumConstants()) {
            if (val.name().equalsIgnoreCase(name)) {
                return val;
            }
        }
        throw INVALID_ENUM.createWithContext(reader, name);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(enumClass.getEnumConstants()).map(Enum::name),
            builder
        );
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
    }
}
