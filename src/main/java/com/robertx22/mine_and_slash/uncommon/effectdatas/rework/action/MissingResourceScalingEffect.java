package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.action;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.world.entity.LivingEntity;

public class MissingResourceScalingEffect extends StatEffect {

    public ResourceType resourceType = ResourceType.mana;
    public EffectSides side = EffectSides.Source;
    public float scalingPer = 1f; // How much stat per X% missing resource

    public MissingResourceScalingEffect(String id, ResourceType resourceType, float scalingPer) {
        super(id, "missing_resource_scaling");
        this.resourceType = resourceType;
        this.scalingPer = scalingPer;
    }

    // Default constructor for serialization
    public MissingResourceScalingEffect() {
        super("", "missing_resource_scaling");
    }

    @Override
    public void activate(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        LivingEntity entity = statSource == EffectSides.Source ? event.source : event.target;

        if (entity == null) return;

        var resources = Load.Unit(entity).getResources();
        float current = resources.get(entity, this.resourceType);
        float max = resources.getMax(entity, this.resourceType);

        if (max <= 0) return;

        float missingPercent = ((max - current) / max) * 100f;
        if (missingPercent <= 0) return;

        // Use the stat's value for scaling
        float statValue = data.getValue();

        // Calculate scaling based on the stat's configuration
        float effectiveScaling = (missingPercent / scalingPer) * statValue;

        // Apply the calculated value to the event's number
        event.data.getNumber(EventData.NUMBER).number += effectiveScaling;
    }

    @Override
    public Class<? extends StatEffect> getSerClass() {
        return MissingResourceScalingEffect.class;
    }
}