package com.robertx22.age_of_exile.uncommon.utilityclasses;

import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public class ItemUtils {
    public static Item.Properties getDefaultGearProperties() {

        Item.Properties prop = new Item.Properties();

        return prop;
    }

    /**
     * Компонент «питья»: анимация бутылки и стандартный звук сёрбанья
     * ванильного зелья (entity.generic.drink).
     * <p>
     * Начиная с MC 26.x звук выпивания берётся из компонента
     * {@code minecraft:consumable}, а не из самого предмета. Предметы без этого
     * компонента (модовые зелья) пьются совершенно молча.
     *
     * @param consumeSeconds длительность выпивания в секундах (должна совпадать
     *                      с {@code getUseDuration} предмета)
     */
    public static Consumable drinkConsumable(float consumeSeconds) {
        return Consumable.builder()
            .consumeSeconds(consumeSeconds)
            .animation(ItemUseAnimation.DRINK)
            .sound(SoundEvents.GENERIC_DRINK)
            .hasConsumeParticles(false)
            .build();
    }

    /**
     * То же самое, но для еды — стандартный звук жевания (entity.generic.eat).
     */
    public static Consumable eatConsumable(float consumeSeconds) {
        return Consumable.builder()
            .consumeSeconds(consumeSeconds)
            .animation(ItemUseAnimation.EAT)
            .sound(SoundEvents.GENERIC_EAT)
            .hasConsumeParticles(false)
            .build();
    }

    /**
     * Длительность использования (в тиках) по компоненту {@code CONSUMABLE}.
     * <p>
     * Компонент — единый источник правды и для анимации/звука питья, и для
     * длительности: если они разойдутся, звук питья просто не прозвучит.
     *
     * @param fallback длительность на случай, если компонента у предмета нет
     */
    public static int useDuration(ItemStack stack, int fallback) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);

        return consumable != null ? consumable.consumeTicks() : fallback;
    }

    /**
     * Финальный «глоток» в момент, когда зелье допито. Звук берётся из
     * компонента {@code minecraft:consumable} самого предмета (у ванильного
     * зелья это {@code entity.generic.drink}).
     * <p>
     * Играется только на сервере: {@code finishUsingItem} выполняется и на
     * клиенте, иначе звук продублируется.
     */
    public static void playConsumeSound(Level world, LivingEntity en, ItemStack stack) {
        if (world.isClientSide()) {
            return;
        }

        Consumable consumable = stack.get(DataComponents.CONSUMABLE);

        if (consumable != null) {
            SoundUtils.playSound(en, consumable.sound()
                .value(), 1F, 1F);
        }
    }

    public static void tryAnnounceItem(ItemStack stack, Player player) {

        try {
            if (player == null) {
                return;
            }
            if (!ServerContainer.get().ENABLE_LOOT_ANNOUNCEMENTS.get()) {
                return;
            }

            GearItemData gear = Gear.Load(stack);

            if (gear != null) {

                GearRarity rar = (GearRarity) gear.getRarity();

                if (rar.announce_in_chat) {

                    MutableComponent name = Component.literal("").append(player.getName())
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(ChatFormatting.LIGHT_PURPLE);

                    Component txt = name
                        .append(" нашёл предмет редкости ")
                        .append(rar.locName()
                            .withStyle(rar.textFormatting())
                            .withStyle(ChatFormatting.BOLD))
                        .append("!");

                    player.level().getServer()
                        .getPlayerList()
                        .getPlayers()
                        .forEach(x -> x.sendSystemMessage(txt));

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
