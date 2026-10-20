package com.robertx22.forge_compat.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minecraft 1.21.2+ / 26.3 requires {@code BlockBehaviour.Properties#setId(ResourceKey)} and
 * {@code Item.Properties#setId(ResourceKey)} before constructing a {@link Block} or {@link Item}.
 * Legacy 1.16.5 code constructs {@code Properties.of()} / {@code new Item.Properties()} without
 * calling {@code setId}; this helper populates the {@code ResourceKey} automatically and updates
 * the final {@code descriptionId} when registered via {@link DeferredRegister}.
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
                setDescriptionIdField(Item.class, entry, expected);
            } else if (entry instanceof Block) {
                String expected = "block." + id.getNamespace() + "." + id.getPath();
                setDescriptionIdField(BlockBehaviour.class, entry, expected);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setDescriptionIdField(Class<?> owner, Object instance, String value) {
        try {
            Field f = owner.getDeclaredField("descriptionId");
            f.setAccessible(true);
            Object cur = f.get(instance);
            if (cur instanceof String s && (s.contains("auto_item_") || s.contains("auto_block_"))) {
                f.set(instance, value);
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
