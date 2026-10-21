package com.robertx22.age_of_exile.database.data.spells.entities;

import com.google.gson.Gson;
import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.mixin_ducks.FallingBlockAccessor;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class StationaryFallingBlockEntity extends FallingBlockEntity implements IDatapackSpellEntity {

    public StationaryFallingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, Level world) {
        super(SlashEntities.SIMPLE_BLOCK_ENTITY.get(), world);
    }

    public StationaryFallingBlockEntity(Level world, BlockPos pos, BlockState block) {
        this(SlashEntities.SIMPLE_BLOCK_ENTITY.get(), world);
        FallingBlockAccessor acc = (FallingBlockAccessor) this;
        acc.setBlockState(block);
        acc.setDestroyedOnLanding(false);
        this.entityData.set(BLOCK, BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());
        this.blocksBuilding = true;
        this.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = pos.getX() + 0.5D;
        this.yo = pos.getY();
        this.zo = pos.getZ() + 0.5D;
        this.setStartPos(this.blockPosition());
    }

    @Override
    public BlockState getBlockState() {
        try {
            String blockStr = this.entityData.get(BLOCK);
            if (blockStr != null && !blockStr.isEmpty()) {
                Identifier id = Identifier.parse(blockStr);
                if (BuiltInRegistries.BLOCK.containsKey(id)) {
                    return BuiltInRegistries.BLOCK.getValue(id)
                        .defaultBlockState();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.getBlockState();
    }

    int lifespan = 1000;

    EntitySavedSpellData spellData;

    private static final EntityDataAccessor<String> SPELL_DATA = SynchedEntityData.defineId(StationaryFallingBlockEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(StationaryFallingBlockEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> BLOCK = SynchedEntityData.defineId(StationaryFallingBlockEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_FALLING = SynchedEntityData.defineId(StationaryFallingBlockEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> FALL_SPEED = SynchedEntityData.defineId(StationaryFallingBlockEntity.class, EntityDataSerializers.FLOAT);

    public Iterable<ItemStack> getArmorSlots() {
        return new ArrayList<>();
    }

    boolean removeNextTick = false;

    public void scheduleRemoval() {
        removeNextTick = true;
    }

    // own age counter: this class fully overrides tick() and never calls super.tick(),
    // so vanilla tickCount can not be relied on for fall acceleration / lifespan checks
    private int age = 0;

    @Override
    public void tick() {

        if (this.removeNextTick) {
            this.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        this.age++;

        if (entityData.get(IS_FALLING)) {
            if (!this.isNoGravity()) {

                float speed = entityData.get(FALL_SPEED);
                speed *= 1 + 0.03F * this.age;

                this.setDeltaMovement(this.getDeltaMovement()
                    .add(0.0D, speed, 0.0D));
            }
            this.move(MoverType.SELF, this.getDeltaMovement());

            if (this.onGround()) {
                this.remove(Entity.RemovalReason.DISCARDED);
                return;
            }

        }

        try {
            this.getSpellData()
                .getSpell()
                .getAttached()
                .tryActivate(getScoreboardName(), SpellCtx.onTick(getSpellData().getCaster(this.level()), this, getSpellData()));
        } catch (Exception e) {
            e.printStackTrace();
            this.remove(Entity.RemovalReason.DISCARDED);
        }

        if (this.age > lifespan || this.getY() < this.level().getMinY() - 32) {
            this.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    private boolean expireEffectsDone = false;

    // In modern MC a server-initiated removal reaches the client through the
    // onClientRemoval() hook (invoked by the final setRemoved()), NOT through remove()
    // (that changed after 1.16, where the destroy-entities packet handler called
    // entity.remove()). So the on-expire effects (which include all the client-side
    // impact particles) must run from both remove() and onClientRemoval(),
    // guarded to run exactly once per side.
    private void runExpireEffects() {
        if (this.expireEffectsDone) {
            return;
        }
        this.expireEffectsDone = true;

        try {
            if (getSpellData() != null) {
                LivingEntity caster = getSpellData().getCaster(this.level());

                if (caster != null) {
                    this.getSpellData()
                        .getSpell()
                        .getAttached()
                        .tryActivate(getScoreboardName(), SpellCtx.onExpire(caster, this, getSpellData()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        runExpireEffects();
        super.remove(reason);
    }

    // client-side counterpart of remove(): Entity.setRemoved() is final and calls
    // this hook when the server removes the entity; remove() is skipped in that path
    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        runExpireEffects();
    }

    static Gson GSON = new Gson();

    public EntitySavedSpellData getSpellData() {
        if (this.level().isClientSide()) {
            if (spellData == null) {
                String json = entityData.get(SPELL_DATA);
                if (json != null && !json.isEmpty()) {
                    this.spellData = GSON.fromJson(json, EntitySavedSpellData.class);
                }
            }
        }
        return spellData;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPELL_DATA, "");
        builder.define(ENTITY_NAME, "");
        builder.define(BLOCK, "");
        builder.define(IS_FALLING, false);
        builder.define(FALL_SPEED, -0.04F);
    }

    public String getScoreboardName() {
        return entityData.get(ENTITY_NAME);
    }

    @Override
    public void init(LivingEntity caster, EntitySavedSpellData data, MapHolder holder) {
        this.spellData = data;

        this.lifespan = holder.get(MapField.LIFESPAN_TICKS)
            .intValue();

        data.item_id = holder.get(MapField.ITEM);
        entityData.set(SPELL_DATA, GSON.toJson(spellData));
        entityData.set(ENTITY_NAME, holder.get(MapField.ENTITY_NAME));
        entityData.set(BLOCK, holder.get(MapField.BLOCK));
        entityData.set(FALL_SPEED, holder.getOrDefault(MapField.BLOCK_FALL_SPEED, -0.04D)
            .floatValue());

    }
}
