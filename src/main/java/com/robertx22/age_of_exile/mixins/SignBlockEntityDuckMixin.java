package com.robertx22.age_of_exile.mixins;

import com.robertx22.world_of_exile.mixin_ducks.SignDuck;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityDuckMixin implements SignDuck {

    @Override
    public Component[] getMessages() {
        return ((SignBlockEntity) (Object) this).getMessages(false);
    }
}
