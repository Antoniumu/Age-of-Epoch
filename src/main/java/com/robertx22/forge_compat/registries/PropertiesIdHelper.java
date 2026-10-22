package com.robertx22.forge_compat.registries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minecraft 1.21.2+ / 26.3 requires {@code BlockBehaviour.Properties#setId(ResourceKey)} and
 * {@code Item.Properties#setId(ResourceKey)} before constructing a {@link Block} or {@link Item}.
 * Legacy 1.16.5 code constructs {@code Properties.of()} / {@code new Item.Properties()} without
 * calling {@code setId}; this helper populates the {@code ResourceKey} automatically and updates
 * the final {@code descriptionId} and {@code DataComponentInitializers} when registered via {@link DeferredRegister}.
 */
public final class PropertiesIdHelper {

    public static final ThreadLocal<Identifier> CURRENT_ID = new ThreadLocal<>();
    private static final AtomicInteger AUTO_COUNTER = new AtomicInteger();

    private static Method blockPropsSetId;
    private static Field blockPropsIdField;
    private static boolean blockPropsResolved;

    private static Method itemPropsSetId;
    private static Field itemPropsIdField;
    private static boolean itemPropsResolved;

    private PropertiesIdHelper() {
    }

    public static void ensureBlockPropertiesId(BlockBehaviour.Properties props) {
        if (props == null) {
            return;
        }
        try {
            resolveBlockProps();
            Identifier cur = CURRENT_ID.get();
            if (blockPropsIdField != null) {
                Object existing = blockPropsIdField.get(props);
                if (existing != null && cur == null) {
                    return;
                }
            }
            Identifier id = cur != null
                ? cur
                : Identifier.fromNamespaceAndPath("mmorpg", "auto_block_" + AUTO_COUNTER.incrementAndGet());
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
            if (blockPropsSetId != null) {
                blockPropsSetId.invoke(props, key);
            } else if (blockPropsIdField != null) {
                blockPropsIdField.set(props, key);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void ensureItemPropertiesId(Item.Properties props) {
        if (props == null) {
            return;
        }
        try {
            resolveItemProps();
            Identifier cur = CURRENT_ID.get();
            if (itemPropsIdField != null) {
                Object existing = itemPropsIdField.get(props);
                if (existing != null && cur == null) {
                    return;
                }
            }
            Identifier id = cur != null
                ? cur
                : Identifier.fromNamespaceAndPath("mmorpg", "auto_item_" + AUTO_COUNTER.incrementAndGet());
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            if (itemPropsSetId != null) {
                itemPropsSetId.invoke(props, key);
            } else if (itemPropsIdField != null) {
                itemPropsIdField.set(props, key);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void fixRegisteredDescriptionId(Object entry, Identifier id) {
        if (entry == null || id == null) {
            return;
        }
        try {
            if (entry instanceof Item) {
                String expected = "item." + id.getNamespace() + "." + id.getPath();
                String oldDesc = setDescriptionIdField(Item.class, entry, expected);
                if (oldDesc != null && oldDesc.startsWith("item.mmorpg.auto_item_")) {
                    String autoPath = oldDesc.substring("item.mmorpg.".length());
                    ResourceKey<Item> oldKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mmorpg", autoPath));
                    ResourceKey<Item> newKey = ResourceKey.create(Registries.ITEM, id);
                    remapDataComponentInitializer(oldKey, newKey);
                }
            } else if (entry instanceof Block) {
                String expected = "block." + id.getNamespace() + "." + id.getPath();
                String oldDesc = setDescriptionIdField(BlockBehaviour.class, entry, expected);
                if (oldDesc != null && oldDesc.startsWith("block.mmorpg.auto_block_")) {
                    String autoPath = oldDesc.substring("block.mmorpg.".length());
                    ResourceKey<Block> oldKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("mmorpg", autoPath));
                    ResourceKey<Block> newKey = ResourceKey.create(Registries.BLOCK, id);
                    remapDataComponentInitializer(oldKey, newKey);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static String setDescriptionIdField(Class<?> owner, Object instance, String value) {
        try {
            Field f = owner.getDeclaredField("descriptionId");
            f.setAccessible(true);
            Object cur = f.get(instance);
            if (cur instanceof String s && (s.contains("auto_item_") || s.contains("auto_block_"))) {
                f.set(instance, value);
                return s;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static java.util.List<?> initializerEntries() {
        // MC 26.3: BuiltInRegistries.DATA_COMPONENT_INITIALIZERS holds a private
        // List<InitializerEntry(key, initializer)> - entries are appended by the
        // Item/Block CONSTRUCTOR using properties.itemIdOrThrow()
        try {
            Field f = net.minecraft.core.component.DataComponentInitializers.class.getDeclaredField("initializers");
            f.setAccessible(true);
            return (java.util.List<?>) f.get(BuiltInRegistries.DATA_COMPONENT_INITIALIZERS);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void remapDataComponentInitializer(ResourceKey<?> oldKey, ResourceKey<?> newKey) {
        // 26.3: swap the key in place on every entry recorded under the auto-minted id
        try {
            java.util.List<?> entries = initializerEntries();
            if (entries == null) {
                return;
            }
            for (Object entry : entries) {
                try {
                    Field kf = entry.getClass().getDeclaredField("key");
                    kf.setAccessible(true);
                    if (oldKey.equals(kf.get(entry))) {
                        kf.set(entry, newKey);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void cleanupDataComponentInitializers() {
        // 26.3: prune initializer entries whose key was auto-minted but never registered;
        // DataComponentInitializers.build() throws "Missing element" on those at world start
        try {
            java.util.List<?> entries = initializerEntries();
            if (entries == null) {
                return;
            }
            List<Object> toRemove = new ArrayList<>();
            for (Object entry : entries) {
                try {
                    Field kf = entry.getClass().getDeclaredField("key");
                    kf.setAccessible(true);
                    Object k = kf.get(entry);
                    if (!(k instanceof ResourceKey<?> rk)) {
                        continue;
                    }
                    Identifier loc = rk.identifier();
                    if (!"mmorpg".equals(loc.getNamespace())) {
                        continue;
                    }
                    if (!loc.getPath().startsWith("auto_item_") && !loc.getPath().startsWith("auto_block_")) {
                        continue;
                    }
                    boolean inItem = Registries.ITEM.identifier().equals(rk.registry()) && BuiltInRegistries.ITEM.containsKey(loc);
                    boolean inBlock = Registries.BLOCK.identifier().equals(rk.registry()) && BuiltInRegistries.BLOCK.containsKey(loc);
                    if (!inItem && !inBlock) {
                        toRemove.add(entry);
                    }
                } catch (Throwable ignored) {
                }
            }
            if (!toRemove.isEmpty()) {
                entries.removeAll(toRemove);
            }
        } catch (Throwable ignored) {
        }
    }

    private static synchronized void resolveBlockProps() {
        if (blockPropsResolved) {
            return;
        }
        blockPropsResolved = true;
        try {
            blockPropsSetId = BlockBehaviour.Properties.class.getMethod("setId", ResourceKey.class);
            blockPropsSetId.setAccessible(true);
        } catch (Throwable ignored) {
        }
        for (Field f : BlockBehaviour.Properties.class.getDeclaredFields()) {
            if (ResourceKey.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                blockPropsIdField = f;
                break;
            }
        }
    }

    private static synchronized void resolveItemProps() {
        if (itemPropsResolved) {
            return;
        }
        itemPropsResolved = true;
        try {
            itemPropsSetId = Item.Properties.class.getMethod("setId", ResourceKey.class);
            itemPropsSetId.setAccessible(true);
        } catch (Throwable ignored) {
        }
        for (Field f : Item.Properties.class.getDeclaredFields()) {
            if (ResourceKey.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                itemPropsIdField = f;
                break;
            }
        }
    }
}
