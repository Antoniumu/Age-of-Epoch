package com.robertx22.mine_and_slash.database.data.spells.entities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.AoeSelector;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.IMyRenderAsItem;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.Utilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import com.robertx22.forge_compat.fml.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SimpleProjectileEntity extends AbstractArrow implements IMyRenderAsItem, IDatapackSpellEntity {

    CalculatedSpellData spellData;

    private int xTile;
    private int yTile;
    private int zTile;

    protected boolean inGround;

    private int ticksInGround = 0;

    public boolean moveTowardsEnemies = false;

    private static final EntityDataAccessor<CompoundTag> SPELL_DATA = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> EXPIRE_ON_ENTITY_HIT = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HIT_ALLIES = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PIERCE = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DEATH_TIME = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHAINS = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPIRE_ON_BLOCK_HIT = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    public Entity ignoreEntity;

    boolean collidedAlready = false;


    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    protected boolean onExpireProc(LivingEntity caster) {
        return true;
    }

    

    

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override // seems to help making it hit easier?
    public float getPickRadius() {
        return 1.0F;
    }

    public int getTicksInGround() {
        return this.ticksInGround;
    }

    public int getDeathTime() {
        return entityData.get(DEATH_TIME);
    }

    public void setDeathTime(int newVal) {
        this.entityData.set(DEATH_TIME, newVal);
    }

    public SimpleProjectileEntity(EntityType<? extends Entity> type, Level worldIn) {
        super((EntityType<? extends AbstractArrow>) type, worldIn);
        this.xTile = -1;
        this.yTile = -1;
        this.zTile = -1;

        this.setSoundEvent(com.robertx22.forge_compat.common.CompatHelper.unwrapSound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("intentionally_empty"))));
    }

    protected void moveToImpactPosition(HitResult result) {

        if (result instanceof EntityHitResult enres) {
            // the result just contains entity position, so we must clip against the AABB ourselves
            AABB aabb = enres.getEntity().getBoundingBox().inflate(0.3D);
            Vec3 traceEnd = this.position().add(this.getDeltaMovement());
            Optional<Vec3> clipped = aabb.clip(this.position(), traceEnd);

            if (clipped.isPresent()) {
                // move to where we hit the entity
                this.setPos(clipped.get());
            }
        } else {
            // move all the way to where we impacted
            this.setPos(result.getLocation());
        }

    }

    public Entity getEntityHit(HitResult result, double radius) {

        EntityHitResult enres = null;

        if (result instanceof EntityHitResult) {
            enres = (EntityHitResult) result;
        }

        if (enres == null) {
            return null;
        }

        if (enres.getEntity() instanceof Entity) {
            if (enres.getEntity() != this.getCaster()) {
                return enres.getEntity();
            }
        }

        if (getCaster() != null) {
            List<LivingEntity> entities = EntityFinder.start(getCaster(), LivingEntity.class, position())
                    .radius(radius)
                    .build();

            if (entities.size() > 0) {

                LivingEntity closest = entities.get(0);

                for (LivingEntity en : entities) {
                    if (en != closest) {
                        if (this.distanceTo(en) < this.distanceTo(closest)) {
                            closest = en;
                        }
                    }
                }

                return closest;
            }
        }

        return null;

    }

    public void onTick() {

        if (getCaster() != null) {

            tryMoveTowardsTargets();

            if (!level().isClientSide()) {
                this.getSpellData()
                        .getSpell()
                        .getAttached()
                        .tryActivate(getScoreboardName(), SpellCtx.onTick(getCaster(), this, getSpellData()));

            }
        }
    }

    boolean exploded = false;

    @Override
    public void remove(RemovalReason r) {

        if (!exploded) {
            exploded = true;

            LivingEntity caster = getCaster();

            if (caster != null) {
                if (!level().isClientSide()) {
                    this.getSpellData()
                            .getSpell()
                            .getAttached()
                            .tryActivate(getScoreboardName(), SpellCtx.onExpire(caster, this, getSpellData()));
                }
            }
        }

        super.remove(r);
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public final void tick() {

        if (this.removeNextTick) {
            this.remove(RemovalReason.KILLED);
            return;
        }

        try {
            super.tick();
        } catch (Exception e) {
            e.printStackTrace();
            this.scheduleRemoval();
        }

        if (this.getSpellData() == null || getCaster() == null) {
            if (tickCount > 100) {
                this.scheduleRemoval();
            }
            return;
        }

        try {
            onTick();

            if (this.inGround) {
                ticksInGround++;
            }


            if (this.tickCount >= this.getDeathTime()) {
                onExpireProc(this.getCaster());
                this.scheduleRemoval();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.scheduleRemoval();
        }

    }

    Entity target = null;

    public void tryMoveTowardsTargets() {
        if (moveTowardsEnemies) {

            if (target == null || !target.isAlive() || this.tickCount % 20 == 0) {

                int radius = getSpellData().getSpell().config.tracking_radius;

                var b = EntityFinder.start(getCaster(), LivingEntity.class, position())
                        .finder(EntityFinder.SelectionType.RADIUS)
                        .searchFor(getSpellData().getSpell().config.tracks)
                        .predicate(x -> AoeSelector.canHit(this.position(), x))
                        .radius(radius);

                target = b.getClosest();
            }

            if (target != null) {
                var vel = ProjectileCastHelper.positionToVelocity(new MyPosition(position()), new MyPosition(target.getEyePosition()));
                vel = vel.normalize().multiply(speed, speed, speed); // todo this doesnt fix the speed problem
                setDeltaMovement(vel);


                PlayerUtils.getNearbyPlayers(level(), blockPosition(), 40)
                        .forEach(p -> {
                            ((ServerPlayer) p).connection.send(new ClientboundSetEntityMotionPacket(this));
                        });
            }
        }
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 pos, Vec3 posPlusMotion) {

        EntityHitResult res = ProjectileUtil.getEntityHitResult(
                this.level(), this, pos, posPlusMotion, this.getBoundingBox()
                        .expandTowards(this.getDeltaMovement())
                        .inflate(1D), (e) -> {
                    return !e.isSpectator() && e.isPickable() && e instanceof Entity && e != this.getCaster() && e != this.ignoreEntity;
                });

        if (!this.entityData.get(HIT_ALLIES)) {
            if (res != null && getCaster() != null && res.getEntity() instanceof LivingEntity) {
                if (AllyOrEnemy.allies.is(getCaster(), (LivingEntity) res.getEntity())) {
                    return null; // don't hit allies with spells, let them pass
                }
            }
        }
        return res;
    }

    @Override
    protected void onHit(HitResult raytraceResultIn) {

        // super.onHit(raytraceResultIn); // adding this back seemed to fix proj a bit

        HitResult.Type raytraceresult$type = raytraceResultIn.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {

            this.onImpact(raytraceResultIn);

        } else if (raytraceresult$type == HitResult.Type.BLOCK) {

            if (collidedAlready) {
                return;
            }
            this.onImpact(raytraceResultIn);

            collidedAlready = true;

            this.inGround = true;

        }

    }

    protected void onImpact(HitResult result) {

        this.moveToImpactPosition(result);

        Entity entityHit = getEntityHit(result, 0.3D);

        if (entityHit != null) {
            if (level().isClientSide()) {
                SoundUtils.playSound(this, com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.GENERIC_HURT), 1F, 0.9F);
            }

            LivingEntity caster = getCaster();

            LivingEntity en = null;

            if (entityHit instanceof LivingEntity == false) {
                // HARDCODED support for dumb ender dragon non living entity dragon parts
                if (entityHit instanceof EnderDragonPart) {
                    EnderDragonPart part = (EnderDragonPart) entityHit;
                    if (true) {
                        en = part.parentMob;
                    }
                }
            } else if (entityHit instanceof LivingEntity) {
                en = (LivingEntity) entityHit;
            }

            if (en == null) {
                return;
            }

            if (caster != null) {
                if (!Load.Unit(caster)
                        .alreadyHit(this, en)) {
                    if (!level().isClientSide()) {
                        var ctx = SpellCtx.onHit(caster, this, en, getSpellData());

                        this.getSpellData()
                                .getSpell()
                                .getAttached()
                                .tryActivate(getScoreboardName(), ctx);
                    }
                }
            }

        } else {

            if (level().isClientSide()) {
                SoundUtils.playSound(this, com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.STONE_HIT), 0.7F, 0.9F);
            }

        }

        if (entityHit != null) {
            if (!entityData.get(EXPIRE_ON_ENTITY_HIT)) {
                return;
            } else {
                scheduleRemoval();
            }
        }

        if (result instanceof BlockHitResult && entityData.get(EXPIRE_ON_BLOCK_HIT)) {
            scheduleRemoval();
        }


        if (!level().isClientSide()) {


            if (getCaster() != null) {


                int chains = this.entityData.get(CHAINS).intValue();

                if (chains > 0) {
                    chains--;

                    if (entityHit == null) {
                        chains = 0;
                    }


                    var radius = getSpellData().data.getNumber(EventData.AREA_MULTI, 1F).number;

                    var b = EntityFinder.start(getCaster(), LivingEntity.class, position())
                            .finder(EntityFinder.SelectionType.RADIUS)
                            .searchFor(AllyOrEnemy.enemies)
                            .radius(5 * radius);

                    if (entityHit instanceof LivingEntity hit) {
                        b.excludeEntity(hit);
                    }
                    var target = b.getClosest();

                    if (target != null) {

                        SimpleProjectileEntity en = (SimpleProjectileEntity) getType().create(level(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                        en.setPos(position());
                        var sd = this.getSpellDataCopy(); // important so it doesnt affect old ones
                        sd.chains_did++; // when upping chain count
                        en.init(caster, sd, holder);
                        en.entityData.set(CHAINS, chains);
                        var vel = ProjectileCastHelper.positionToVelocity(new MyPosition(position()), new MyPosition(target.getEyePosition()));
                        en.setDeltaMovement(vel.normalize().multiply(speed, speed, speed));
                        level().addFreshEntity(en);

                    }
                }
            }
        }

    }

    boolean removeNextTick = false;

    public void scheduleRemoval() {
        if (!this.isRemoved()) {
            this.discard();
            removeNextTick = true;
        }
    }

    static Gson GSON = new Gson();

    public void addAdditionalSaveDataLegacy(CompoundTag nbt) {

        try {

            // super.writeCustomDataToTag(nbt);

            nbt.putInt("xTile", this.xTile);
            nbt.putInt("yTile", this.yTile);
            nbt.putInt("zTile", this.zTile);

            nbt.putByte("inGround", (byte) (this.inGround ? 1 : 0));

            nbt.putInt("deathTime", this.getDeathTime());

            nbt.putString("data", GSON.toJson(spellData));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void readAdditionalSaveDataLegacy(CompoundTag nbt) {

        try {

//            super.readCustomDataFromTag(nbt);

            this.xTile = nbt.getIntOr("xTile", 0);
            this.yTile = nbt.getIntOr("yTile", 0);
            this.zTile = nbt.getIntOr("zTile", 0);

            this.inGround = nbt.getByteOr("inGround", (byte) 0) == 1;

            this.setDeathTime(nbt.getIntOr("deathTime", 0));

            this.spellData = GSON.fromJson(nbt.getStringOr("data", ""), CalculatedSpellData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    LivingEntity caster;

    public LivingEntity getCaster() {
        if (caster == null) {
            try {
                this.caster = Utilities.getLivingEntityByUUID(level(), UUID.fromString(getSpellData().caster_uuid));
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        return caster;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(SPELL_DATA, new CompoundTag());
        builder.define(ENTITY_NAME, "");
        builder.define(EXPIRE_ON_ENTITY_HIT, true);
        builder.define(EXPIRE_ON_BLOCK_HIT, true);
        builder.define(HIT_ALLIES, false);
        builder.define(PIERCE, false);
        builder.define(DEATH_TIME, 100);
        builder.define(CHAINS, 0);
        super.defineSynchedData(builder);
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public CalculatedSpellData getSpellData() {
        try {
            if (level().isClientSide()) {
                if (spellData == null) {
                    CompoundTag nbt = entityData.get(SPELL_DATA);
                    if (nbt != null) {
                        this.spellData = GSON.fromJson(nbt.getStringOr("spell", ""), CalculatedSpellData.class);
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return spellData;
    }

    public CalculatedSpellData getSpellDataCopy() {
        return GSON.fromJson(GSON.toJson(getSpellData()), CalculatedSpellData.class);
    }

    @Override
    public ItemStack getItem() {
        try {
            Item item = VanillaUTIL.REGISTRY.items().get(Identifier.parse(String.valueOf(getSpellData().data.getString(EventData.ITEM_ID))));
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return new ItemStack(Items.AIR);
    }

    public String getScoreboardName() {
        return entityData.get(ENTITY_NAME);
    }

    @Override
    public void playerTouch(Player player) {
        // don't allow player to pickup lol
    }

    MapHolder holder;
    float speed = 0;

    @Override
    public void init(LivingEntity caster, CalculatedSpellData data, MapHolder holder) {
        this.holder = holder;
        this.spellData = data;

        this.pickup = Pickup.DISALLOWED;

        this.setNoGravity(!holder.getOrDefault(MapField.GRAVITY, true));
        this.setDeathTime(holder.get(MapField.LIFESPAN_TICKS)
                .intValue());

        this.entityData.set(EXPIRE_ON_ENTITY_HIT, holder.getOrDefault(MapField.EXPIRE_ON_ENTITY_HIT, true));
        this.entityData.set(EXPIRE_ON_BLOCK_HIT, holder.getOrDefault(MapField.EXPIRE_ON_BLOCK_HIT, true));
        this.entityData.set(HIT_ALLIES, holder.getOrDefault(MapField.HITS_ALLIES, false));
        this.entityData.set(CHAINS, holder.getOrDefault(MapField.CHAIN_COUNT, 0D).intValue() + (int) data.data.getNumber(EventData.BONUS_CHAINS).number);

        

        if (data.data.getBoolean(EventData.PIERCE)) {
            this.entityData.set(EXPIRE_ON_ENTITY_HIT, false);
        }

        this.moveTowardsEnemies = holder.getOrDefault(MapField.TRACKS_ENEMIES, false);
        this.speed = holder.getOrDefault(MapField.PROJECTILE_SPEED, 1D).floatValue();

        data.data.setString(EventData.ITEM_ID, holder.get(MapField.ITEM));
        CompoundTag nbt = new CompoundTag();
        nbt.putString("spell", GSON.toJson(spellData));
        entityData.set(SPELL_DATA, nbt);
        this.setOwner(caster);

        String name = holder.get(MapField.ENTITY_NAME);
        entityData.set(ENTITY_NAME, name);

    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("aoe_ent_nbt", CompoundTag.CODEC).ifPresent(this::readAdditionalSaveDataLegacy);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        CompoundTag __nbt = new CompoundTag();
        this.addAdditionalSaveDataLegacy(__nbt);
        output.store("aoe_ent_nbt", CompoundTag.CODEC, __nbt);
    }


    public String getSpellId() { return spellData != null ? spellData.spell_id : this.entityData.get(ENTITY_NAME); }
}
