package com.robertx22.forge_compat.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;

import java.net.URI;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public final class CompatHelper {
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = new DecimalFormat("#.##");

    public static InteractionResult toUseResult(InteractionResultHolder<ItemStack> holder) {
        return holder == null ? InteractionResult.PASS : holder.getResult();
    }

    public static ItemEntity dropStack(Entity entity, ItemStack stack) {
        if (entity == null || stack == null || stack.isEmpty()) return null;
        if (entity.level() instanceof ServerLevel sl) {
            return entity.spawnAtLocation(sl, stack);
        }
        return null;
    }

    public static ItemEntity dropStack(Entity entity, ItemStack stack, float yOffset) {
        if (entity == null || stack == null || stack.isEmpty()) return null;
        if (entity.level() instanceof ServerLevel sl) {
            return entity.spawnAtLocation(sl, stack, yOffset);
        }
        return null;
    }

    public static <T extends Entity> T createEntity(EntityType<T> type, Level level) {
        return type == null || level == null ? null : type.create(level, EntitySpawnReason.TRIGGERED);
    }

    public static Vec3i dirNormal(Direction dir) {
        return dir == null ? Vec3i.ZERO : new Vec3i(dir.getStepX(), dir.getStepY(), dir.getStepZ());
    }

    public static int minBuildHeight(LevelHeightAccessor acc) {
        return acc == null ? -64 : acc.getMinY();
    }

    public static int maxBuildHeight(LevelHeightAccessor acc) {
        return acc == null ? 320 : acc.getMaxY();
    }

    public static AttributeModifier attrMod(UUID uuid, String name, double amount, AttributeModifier.Operation op) {
        String safe = (uuid != null ? uuid.toString() : String.valueOf(name).toLowerCase().replaceAll("[^a-z0-9_./-]", "_"));
        return new AttributeModifier(Identifier.fromNamespaceAndPath("mmorpg", safe), amount, op);
    }

    public static AttributeModifier attrMod(String name, double amount, AttributeModifier.Operation op) {
        String safe = String.valueOf(name).toLowerCase().replaceAll("[^a-z0-9_./-]", "_");
        return new AttributeModifier(Identifier.fromNamespaceAndPath("mmorpg", safe), amount, op);
    }

    public static AttributeModifier attrMod(Identifier id, double amount, AttributeModifier.Operation op) {
        return new AttributeModifier(id, amount, op);
    }

    public static Identifier attrModId(UUID uuid) {
        return Identifier.fromNamespaceAndPath("mmorpg", uuid.toString());
    }

    public static Holder<Attribute> attrHolder(Attribute attr) {
        return attr == null ? null : BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attr);
    }

    public static AttributeInstance getAttribute(LivingEntity entity, Attribute attr) {
        return entity == null || attr == null ? null : entity.getAttribute(attrHolder(attr));
    }

    public static AttributeInstance getAttribute(LivingEntity entity, Holder<Attribute> attr) {
        return entity == null || attr == null ? null : entity.getAttribute(attr);
    }

    public static Holder<MobEffect> wrapEffect(MobEffect effect) {
        return effect == null ? null : BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    public static Holder<MobEffect> wrapEffect(Holder<MobEffect> holder) {
        return holder;
    }

    public static boolean hasEffect(LivingEntity entity, MobEffect effect) {
        return entity != null && effect != null && entity.hasEffect(wrapEffect(effect));
    }

    public static boolean hasEffect(LivingEntity entity, Holder<MobEffect> effect) {
        return entity != null && effect != null && entity.hasEffect(effect);
    }

    public static MobEffectInstance getEffect(LivingEntity entity, MobEffect effect) {
        return entity == null || effect == null ? null : entity.getEffect(wrapEffect(effect));
    }

    public static MobEffectInstance getEffect(LivingEntity entity, Holder<MobEffect> effect) {
        return entity == null || effect == null ? null : entity.getEffect(effect);
    }

    public static boolean removeEffect(LivingEntity entity, MobEffect effect) {
        return entity != null && effect != null && entity.removeEffect(wrapEffect(effect));
    }

    public static boolean removeEffect(LivingEntity entity, Holder<MobEffect> effect) {
        return entity != null && effect != null && entity.removeEffect(effect);
    }

    public static SoundEvent unwrapSound(SoundEvent s) {
        return s;
    }

    public static SoundEvent unwrapSound(Holder<SoundEvent> h) {
        return h == null ? null : h.value();
    }

    public static void addCooldown(ItemCooldowns cd, Item item, int ticks) {
        if (cd != null && item != null) cd.addCooldown(new ItemStack(item), ticks);
    }

    public static void addCooldown(ItemCooldowns cd, ItemStack stack, int ticks) {
        if (cd != null && stack != null) cd.addCooldown(stack, ticks);
    }

    public static boolean isOnCooldown(ItemCooldowns cd, Item item) {
        return cd != null && item != null && cd.isOnCooldown(new ItemStack(item));
    }

    public static boolean isOnCooldown(ItemCooldowns cd, ItemStack stack) {
        return cd != null && stack != null && cd.isOnCooldown(stack);
    }

    public static int getEnchantLevel(Object entityOrLevel, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack == null || stack.isEmpty() || key == null) return 0;
        try {
            Level level = entityOrLevel instanceof Level l ? l : (entityOrLevel instanceof Entity e ? e.level() : null);
            RegistryAccess access = level != null ? level.registryAccess()
                : (com.robertx22.forge_compat.fml.server.ServerLifecycleHooks.getCurrentServer() != null
                    ? com.robertx22.forge_compat.fml.server.ServerLifecycleHooks.getCurrentServer().registryAccess()
                    : null);
            if (access != null) {
                var opt = access.lookup(Registries.ENCHANTMENT).flatMap(r -> r.get(key));
                if (opt.isPresent()) {
                    return EnchantmentHelper.getItemEnchantmentLevel(opt.get(), stack);
                }
            }
        } catch (Throwable ignored) {}
        return stack.getEnchantments().entrySet().stream()
            .filter(e -> e.getKey().unwrapKey().map(k -> k.equals(key)).orElse(false))
            .mapToInt(e -> e.getIntValue())
            .findFirst()
            .orElse(0);
    }

    public static List<Component> getTooltipLines(ItemStack stack, Player player, TooltipFlag flag) {
        if (stack == null) return List.of();
        Item.TooltipContext ctx = (player != null && player.level() != null)
            ? Item.TooltipContext.of(player.level())
            : Item.TooltipContext.EMPTY;
        return stack.getTooltipLines(ctx, player, flag);
    }

    public static CompoundTag saveStack(ItemStack stack, CompoundTag target) {
        if (target == null) target = new CompoundTag();
        if (stack == null || stack.isEmpty()) return target;
        try {
            var res = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
            if (res.result().isPresent() && res.result().get() instanceof CompoundTag encoded) {
                for (String k : encoded.keySet()) {
                    target.put(k, encoded.get(k));
                }
            }
        } catch (Throwable ignored) {}
        return target;
    }

    public static ItemStack loadStack(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return ItemStack.EMPTY;
        try {
            return ItemStack.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(ItemStack.EMPTY);
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static ListTag saveContainer(SimpleContainer inv) {
        ListTag list = new ListTag();
        if (inv == null) return list;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag itemTag = saveStack(s, new CompoundTag());
                itemTag.putByte("Slot", (byte) i);
                list.add(itemTag);
            }
        }
        return list;
    }

    public static void loadContainer(SimpleContainer inv, ListTag list) {
        if (inv == null) return;
        inv.clearContent();
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompoundOrEmpty(i);
            int slot = itemTag.contains("Slot") ? (itemTag.getByteOr("Slot", (byte) 0) & 255) : i;
            ItemStack s = loadStack(itemTag);
            if (slot >= 0 && slot < inv.getContainerSize()) {
                inv.setItem(slot, s);
            }
        }
    }

    public static ClickEvent openUrl(String url) {
        try {
            return new ClickEvent.OpenUrl(URI.create(url));
        } catch (Throwable t) {
            return new ClickEvent.CopyToClipboard(url);
        }
    }

    public static int getEnchantLevelDirect(ItemStack stack, Enchantment ench) {
        if (stack == null || stack.isEmpty() || ench == null) return 0;
        for (var entry : stack.getEnchantments().entrySet()) { if (entry.getKey().value() == ench) return entry.getIntValue(); } return 0;
    }

    public static double getAttributeValue(LivingEntity en, Attribute attr) {
        if (en == null || attr == null) return 0;
        return en.getAttributeValue(attrHolder(attr));
    }

    public static double getAttributeValue(LivingEntity en, Holder<Attribute> attr) {
        return en == null || attr == null ? 0 : en.getAttributeValue(attr);
    }

    public static void killEntity(Entity en) {
        if (en != null && en.level() instanceof ServerLevel sl) {
            en.kill(sl);
        } else if (en != null) {
            en.discard();
        }
    }

    public static void hurtEntity(Entity en, net.minecraft.world.damagesource.DamageSource src, float amt) {
        if (en != null && en.level() instanceof ServerLevel sl) {
            en.hurtServer(sl, src, amt);
        }
    }

    public static java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> getAllEnchantments(ItemStack stack) {
        java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> map = new java.util.LinkedHashMap<>();
        if (stack == null || stack.isEmpty()) return map;
        try {
            for (var entry : stack.getEnchantments().entrySet()) {
                map.put(entry.getKey().value(), entry.getIntValue());
            }
        } catch (Throwable ignored) {}
        return map;
    }
}
