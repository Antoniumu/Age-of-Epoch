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

    /**
     * MC 26.3 / Fabric: every custom ArgumentType must be registered with
     * ArgumentTypeInfos before the command tree is synced to the client
     * ("Unrecognized argument type" crash on player join otherwise).
     */
    public static void registerCompat() {
        net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry.registerArgumentType(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("mmorpg", "enum"),
            (Class<? extends EnumArgument<?>>) (Class<?>) EnumArgument.class,
            Info.INSTANCE
        );
    }

    public static class Info implements net.minecraft.commands.synchronization.ArgumentTypeInfo<EnumArgument<?>, Info.Template> {
        public static final Info INSTANCE = new Info();

        public static final class Template implements net.minecraft.commands.synchronization.ArgumentTypeInfo.Template<EnumArgument<?>> {
            private final Class<? extends Enum<?>> enumClass;

            Template(Class<? extends Enum<?>> enumClass) {
                this.enumClass = enumClass;
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public EnumArgument<?> instantiate(net.minecraft.commands.CommandBuildContext ctx) {
                return EnumArgument.enumArgument((Class) enumClass);
            }

            @Override
            public ArgumentTypeInfo<EnumArgument<?>, ?> type() {
                return Info.INSTANCE;
            }
        }

        @Override
        public void serializeToNetwork(Template t, net.minecraft.network.FriendlyByteBuf buf) {
            // the enum class travels with the template so client-side suggestions work
            buf.writeUtf(t.enumClass.getName());
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Template deserializeFromNetwork(net.minecraft.network.FriendlyByteBuf buf) {
            try {
                return new Template(Class.forName(buf.readUtf()).asSubclass(Enum.class));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unknown enum argument class received", e);
            }
        }

        @Override
        public void serializeToJson(Template t, com.google.gson.JsonObject json) {
            json.addProperty("enum", t.enumClass.getName());
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Template unpack(EnumArgument<?> arg) {
            return new Template(((EnumArgument) arg).enumClass);
        }
    }
}
