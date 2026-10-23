package com.robertx22.age_of_exile.uncommon.effectdatas;

import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.effectdatas.rework.RestoreType;
import com.robertx22.age_of_exile.uncommon.utilityclasses.EntityFinder;
import com.robertx22.age_of_exile.uncommon.utilityclasses.NumberUtils;
import com.robertx22.age_of_exile.vanilla_mc.packets.DmgNumPacket;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;

import java.util.List;

public class RestoreResourceEvent extends EffectEvent {

    public static String ID = "on_spend_resource";

    protected RestoreResourceEvent(float num, LivingEntity source, LivingEntity target) {
        super(num, source, target);
    }

    @Override
    public String GUID() {
        return ID;
    }

    /**
     * Регенерация даёт и плоское значение стата, и процент от максимума.
     * Без процента энергия на 50-м уровне восстанавливалась на 4 в секунду
     * при стоимости удара в 13 — бой превращался в ожидание.
     */
    private float getAmountWithPercentRegen() {
        float amount = data.getNumber();
        if (data.getRestoreType() != RestoreType.regen) {
            return amount;
        }
        float percent;
        if (data.getResourceType() == ResourceType.energy) {
            percent = com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig.get().ENERGY_REGEN_PERCENT_PER_SECOND;
        } else if (data.getResourceType() == ResourceType.mana) {
            percent = com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig.get().MANA_REGEN_PERCENT_PER_SECOND;
        } else {
            return amount;
        }
        if (percent <= 0) {
            return amount;
        }
        float max = this.targetData.getResources()
            .getMax(target, data.getResourceType());
        if (max <= 0) {
            return amount;
        }
        // тик регенерации приходит раз в 3 секунды (60 тиков) — берём именно эту долю
        return amount + max * percent * 3F;
    }

    @Override
    protected void activate() {

        if (data.isCanceled()) {
            return;
        }

        this.targetData.getResources()
            .restore(target, data.getResourceType(), getAmountWithPercentRegen());

        if (this.data.getResourceType() == ResourceType.health) {
            if (data.getRestoreType() == RestoreType.heal) {
                if (source instanceof Player) {

                    if (source != target) {
                        String text = NumberUtils.format(data.getNumber());

                        DmgNumPacket packet = new DmgNumPacket(target, text, data.isCrit(), ChatFormatting.GREEN);
                        Packets.sendToClient((Player) source, packet);
                    }

                    float threat = (int) (data.getNumber() * 0.1F);
                    List<Mob> mobs = EntityFinder.start(source, Mob.class, source.blockPosition())
                        .radius(10)
                        .build();
                    for (Mob x : mobs) {
                        GenerateThreatEvent threatEvent = new GenerateThreatEvent((Player) source, x, ThreatGenType.heal, threat);
                        threatEvent.Activate();
                    }
                }
            }
        }

    }
}