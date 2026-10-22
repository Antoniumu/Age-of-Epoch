package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.mixin_ducks.IDirty;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(AttributeMap.class)
public class AttributeMixin implements IDirty {

    private static final List<Holder<Attribute>> WATCHED_ATTRIBUTES = List.of(
        Attributes.MAX_HEALTH,
        Attributes.ARMOR,
        Attributes.ARMOR_TOUGHNESS,
        Attributes.ATTACK_DAMAGE,
        Attributes.ATTACK_SPEED,
        Attributes.MOVEMENT_SPEED,
        Attributes.KNOCKBACK_RESISTANCE
    );

    private boolean atriDirty = true;
    private double lastAttrSignature = Double.NaN;

    @Override
    public boolean isAttribDirty() {
        AttributeMap self = (AttributeMap) (Object) this;
        double sig = 1.0;
        for (Holder<Attribute> holder : WATCHED_ATTRIBUTES) {
            AttributeInstance inst = self.getInstance(holder);
            if (inst != null) {
                sig = sig * 31.0 + inst.getValue();
            }
        }
        if (Double.compare(sig, lastAttrSignature) != 0) {
            lastAttrSignature = sig;
            return true;
        }
        return atriDirty;
    }

    @Override
    public void setAttribDirty(boolean b) {
        this.atriDirty = b;
    }
}
