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
    private static void remapDataComponentInitializer(ResourceKey<?> oldKey, ResourceKey<?> newKey) {
        try {
            for (Map map : findResourceKeyMaps()) {
                if (map.containsKey(oldKey)) {
                    Object val = map.remove(oldKey);
                    if (val != null) {
                        map.put(newKey, val);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void cleanupDataComponentInitializers() {
        try {
            for (Map map : findResourceKeyMaps()) {
                List<Object> toRemove = new ArrayList<>();
                for (Object k : map.keySet()) {
                    if (k instanceof ResourceKey<?> rk) {
                        Identifier loc = rk.identifier();
                        if ("mmorpg".equals(loc.getNamespace())
                            && (loc.getPath().startsWith("auto_item_") || loc.getPath().startsWith("auto_block_"))) {
                            if (!BuiltInRegistries.ITEM.containsKey(loc) && !BuiltInRegistries.BLOCK.containsKey(loc)) {
                                toRemove.add(k);
                            }
                        }
                    }
                }
                for (Object k : toRemove) {
                    map.remove(k);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("rawtypes")
    private static List<Map> findResourceKeyMaps() {
        List<Map> result = new ArrayList<>();
        try {
            for (Field sf : BuiltInRegistries.class.getDeclaredFields()) {
                if (!Modifier.isStatic(sf.getModifiers())) {
                    continue;
                }
                sf.setAccessible(true);
                Object root = sf.get(null);
                if (root == null || !root.getClass().getSimpleName().contains("DataComponentInitializers")) {
                    continue;
                }
                collectResourceKeyMaps(root, result, 0);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static void collectResourceKeyMaps(Object obj, List<Map> result, int depth) {
        if (obj == null || depth > 3) {
            return;
        }
        if (obj instanceof Map map) {
            boolean hasResourceKeyValues = false;
            for (Object entryObj : map.entrySet()) {
                if (entryObj instanceof Map.Entry entry) {
                    Object k = entry.getKey();
                    Object v = entry.getValue();
                    if (v instanceof Map || (v != null && v.getClass().getName().startsWith("net.minecraft."))) {
                        hasResourceKeyValues = true;
                        collectResourceKeyMaps(v, result, depth + 1);
                    }
                    if (k instanceof ResourceKey<?> && !(v instanceof Map)) {
                        result.add(map);
                        break;
                    }
                }
            }
            if (hasResourceKeyValues) {
                return;
            }
        }
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object fieldVal = f.get(obj);
                    if (fieldVal instanceof Map) {
                        collectResourceKeyMaps(fieldVal, result, depth + 1);
                    }
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
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
