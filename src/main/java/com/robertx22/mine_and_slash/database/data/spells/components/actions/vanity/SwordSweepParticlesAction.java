package com.robertx22.mine_and_slash.database.data.spells.components.actions.vanity;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Collection;

public class SwordSweepParticlesAction extends SpellAction {

    public SwordSweepParticlesAction() {
        super(Arrays.asList());
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        targets.forEach(x -> {
            if (x instanceof Player p) {
                ParticleUtils.spawnDefaultSlashingWeaponParticles(p);
            }
        });
    }

    public MapHolder create() {
        MapHolder c = new MapHolder();
        c.type = GUID();
        this.validate(c);
        return c;
    }

    @Override
    public String GUID() {
        return "sword_sweep_particles";
    }
}
