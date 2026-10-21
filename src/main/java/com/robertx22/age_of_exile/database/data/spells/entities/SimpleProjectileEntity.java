package com.robertx22.age_of_exile.database.data.spells.entities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.entities.renders.IMyRenderAsItem;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.age_of_exile.uncommon.utilityclasses.EntityFinder;
import com.robertx22.age_of_exile.uncommon.utilityclasses.Utilities;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SimpleProjectileEntity extends AbstractArrow implements IMyRenderAsItem, IDatapackSpellEntity {

    EntitySavedSpellData spellData;

    private int xTile;
    private int yTile;
    private int zTile;

    protected boolean inGround;

    private int ticksInGround = 0;

    private static final EntityDataAccessor<String> SPELL_DATA = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> EXPIRE_ON_ENTITY_HIT = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HIT_ALLIES = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PIERCE = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DEATH_TIME = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPIRE_ON_BLOCK_HIT = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    public Entity ignoreEntity;

    boolean collidedAlready = false;

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    protected boolean onExpireProc(LivingEntity caster) {
        return true;
    }

    public Iterable<ItemStack> getArmorSlots() {
        return new ArrayList<>();
    }

    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {

    }

    @Override
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

    @SuppressWarnings("unchecked")
    public SimpleProjectileEntity(EntityType<? extends Entity> type, Level worldIn) {
        super((EntityType<? extends AbstractArrow>) type, worldIn);
        this.xTile = -1;
        this.yTile = -1;
        this.zTile = -1;
    }

    public Entity getEntityHit(HitResult result, double radius) {

        EntityHitResult enres = null;

        if (result instanceof EntityHitResult) {
            enres = (EntityHitResult) result;
        }

        if (enres != null && enres.getEntity() != null) {
            if (enres.getEntity() != this.getCaster()) {
                return enres.getEntity();
            }
        }

        // Fallback for block impacts (and self hits): the swept ray is clipped at the
        // first block face, so a target right in front of the caster (point blank,
        // ground/steps/walls between caster and target) is never found by the ray.
        // Without this, spells like Fireball fizzle with zero damage in melee range.
        LivingEntity caster = getCaster();

        if (caster != null) {
            Vec3 center = result != null ? result.getLocation() : position();

            List<LivingEntity> entities = EntityFinder.start(caster, LivingEntity.class, center)
                .radius(radius)
                .build();

            if (entities.size() > 0) {

                LivingEntity closest = entities.get(0);
                double closestDist = center.distanceToSqr(closest.position());

                for (LivingEntity en : entities) {
                    double dist = center.distanceToSqr(en.position());
                    if (dist < closestDist) {
                        closest = en;
                        closestDist = dist;
                    }
                }

                return closest;
            }
        }

        return null;

    }

    public void onTick() {

        if (getCaster() != null) {
            this.getSpellData()
                .getSpell()
                .getAttached()
                .tryActivate(getScoreboardName(), SpellCtx.onTick(getCaster(), this, getSpellData()));

        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {

        LivingEntity caster = getCaster();

        if (caster != null) {
            this.getSpellData()
                .getSpell()
                .getAttached()
                .tryActivate(getScoreboardName(), SpellCtx.onExpire(caster, this, getSpellData()));
        }

        super.remove(reason);
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public final void tick() {

        if (this.removeNextTick) {
            this.remove(Entity.RemovalReason.DISCARDED);
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

    @Override
    protected EntityHitResult findHitEntity(Vec3 pos, Vec3 posPlusMotion) {

        EntityHitResult res = ProjectileUtil.getEntityHitResult(
            this.level(), this, pos, posPlusMotion, this.getBoundingBox()
                .expandTowards(this.getDeltaMovement())
                .inflate(1D), (e) -> {
                return !e.isSpectator() && e.isPickable() && e != this.getCaster() && e != this.ignoreEntity;
            });

        if (res == null && this.tickCount <= 2) {
            // Point blank forgiveness: the exact ray can graze past a hugging enemy.
            // During the first ticks (melee range) fatten the sweep slightly so
            // close range casts reliably register hits.
            res = findForgivenHitEntity(pos, posPlusMotion);
        }

        if (!this.entityData.get(HIT_ALLIES)) {
            if (res != null && getCaster() != null && res.getEntity() instanceof LivingEntity) {
                if (AllyOrEnemy.allies.is(getCaster(), (LivingEntity) res.getEntity())) {
                    return null;
                }
            }
        }
        return res;
    }

    private EntityHitResult findForgivenHitEntity(Vec3 pos, Vec3 posPlusMotion) {
        LivingEntity caster = getCaster();

        List<LivingEntity> list = this.level()
            .getEntitiesOfClass(LivingEntity.class, this.getBoundingBox()
                .expandTowards(posPlusMotion.subtract(pos))
                .inflate(1D), (e) -> {
                return e.isAlive() && !e.isSpectator() && e.isPickable()
                    && e != caster && e != this.ignoreEntity
                    && (this.entityData.get(HIT_ALLIES) || caster == null
                    || !AllyOrEnemy.allies.is(caster, e));
            });

        EntityHitResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity en : list) {
            AABB box = en.getBoundingBox()
                .inflate(0.25D);

            if (box.contains(pos)) {
                return new EntityHitResult(en);
            }

            Optional<Vec3> hit = box.clip(pos, posPlusMotion);
            if (hit.isPresent()) {
                double dist = pos.distanceToSqr(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new EntityHitResult(en);
                }
            }
        }

        return best;
    }

    @Override
    protected void onHit(HitResult raytraceResultIn) {

        HitResult.Type raytraceresult$type = raytraceResultIn.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onImpact(raytraceResultIn);
            this.playSound(SoundEvents.SHULKER_BULLET_HIT, 1.0F, 1.2F / (this.getRandom().nextFloat() * 0.2F + 0.9F));

        } else if (raytraceresult$type == HitResult.Type.BLOCK) {

            if (collidedAlready) {
                return;
            }
            collidedAlready = true;

            BlockHitResult blockraytraceresult = (BlockHitResult) raytraceResultIn;
            BlockState blockstate = this.level().getBlockState(blockraytraceresult.getBlockPos());

            Vec3 vec3d = blockraytraceresult.getLocation()
                .subtract(this.getX(), this.getY(), this.getZ());
            this.setDeltaMovement(vec3d);

            this.inGround = true;

            this.onImpact(blockraytraceresult);

            blockstate.onProjectileHit(this.level(), blockstate, blockraytraceresult, this);
        }

    }

    protected void onImpact(HitResult result) {

        Entity entityHit = getEntityHit(result, 1.0D);

        if (entityHit != null) {
            if (this.level().isClientSide()) {
                SoundUtils.playSound(this, SoundEvents.GENERIC_HURT, 1F, 0.9F);
            }

            LivingEntity caster = getCaster();

            LivingEntity en = null;

            if (entityHit instanceof LivingEntity == false) {
                if (entityHit instanceof EnderDragonPart) {
                    EnderDragonPart part = (EnderDragonPart) entityHit;
                    en = part.parentMob;
                }
            } else if (entityHit instanceof LivingEntity) {
                en = (LivingEntity) entityHit;
            }

            if (en == null) {
                return;
            }

            if (caster != null) {
                if (!Load.spells(caster)
                    .alreadyHit(this, en)) {
                    this.getSpellData()
                        .getSpell()
                        .getAttached()
                        .tryActivate(getScoreboardName(), SpellCtx.onHit(caster, this, en, getSpellData()));
                }
            }

        } else {
            if (this.level().isClientSide()) {
                SoundUtils.playSound(this, SoundEvents.STONE_HIT, 0.7F, 0.9F);
            }
        }

        if (entityHit != null && !entityData.get(EXPIRE_ON_ENTITY_HIT)) {
            return;
        }

        if (entityHit != null) {
            scheduleRemoval();
        }

        if (result instanceof BlockHitResult && entityData.get(EXPIRE_ON_BLOCK_HIT)) {
            scheduleRemoval();
        }

    }

    boolean removeNextTick = false;

    public void scheduleRemoval() {
        removeNextTick = true;
    }

    static Gson GSON = new Gson();

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        try {
            output.putInt("xTile", this.xTile);
            output.putInt("yTile", this.yTile);
            output.putInt("zTile", this.zTile);
            output.putByte("inGround", (byte) (this.inGround ? 1 : 0));
            output.putInt("deathTime", this.getDeathTime());
            output.putString("data", GSON.toJson(spellData));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        try {
            this.xTile = input.getIntOr("xTile", 0);
            this.yTile = input.getIntOr("yTile", 0);
            this.zTile = input.getIntOr("zTile", 0);
            this.inGround = input.getByteOr("inGround", (byte) 0) == 1;
            this.setDeathTime(input.getIntOr("deathTime", 100));
            String data = input.getStringOr("data", "");
            if (!data.isEmpty()) {
                this.spellData = GSON.fromJson(data, EntitySavedSpellData.class);
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    LivingEntity caster;

    public LivingEntity getCaster() {
        if (caster == null) {
            try {
                this.caster = Utilities.getLivingEntityByUUID(this.level(), UUID.fromString(getSpellData().caster_uuid));
            } catch (Exception e) {
            }
        }

        return caster;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPELL_DATA, "");
        builder.define(ENTITY_NAME, "");
        builder.define(EXPIRE_ON_ENTITY_HIT, true);
        builder.define(EXPIRE_ON_BLOCK_HIT, true);
        builder.define(HIT_ALLIES, false);
        builder.define(PIERCE, false);
        builder.define(DEATH_TIME, 100);
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public EntitySavedSpellData getSpellData() {
        try {
            if (this.level().isClientSide()) {
                if (spellData == null || spellData.item_id == null || spellData.item_id.isEmpty()) {
                    String json = entityData.get(SPELL_DATA);
                    if (json != null && !json.isEmpty()) {
                        this.spellData = GSON.fromJson(json, EntitySavedSpellData.class);
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return spellData;
    }

    @Override
    public ItemStack getItem() {
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(getSpellData().item_id));
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
        }

        return new ItemStack(Items.AIR);
    }

    public String getScoreboardName() {
        return entityData.get(ENTITY_NAME);
    }

    @Override
    public void playerTouch(Player player) {
    }

    @Override
    public void init(LivingEntity caster, EntitySavedSpellData data, MapHolder holder) {
        this.spellData = data;

        this.pickup = AbstractArrow.Pickup.DISALLOWED;

        this.setNoGravity(!holder.getOrDefault(MapField.GRAVITY, true));
        this.setDeathTime(holder.get(MapField.LIFESPAN_TICKS)
            .intValue());

        this.entityData.set(EXPIRE_ON_ENTITY_HIT, holder.getOrDefault(MapField.EXPIRE_ON_ENTITY_HIT, true));
        this.entityData.set(EXPIRE_ON_BLOCK_HIT, holder.getOrDefault(MapField.EXPIRE_ON_BLOCK_HIT, true));
        this.entityData.set(HIT_ALLIES, holder.getOrDefault(MapField.HITS_ALLIES, false));

        if (data.pierce) {
            this.entityData.set(EXPIRE_ON_ENTITY_HIT, false);
        }

        data.item_id = holder.get(MapField.ITEM);
        entityData.set(SPELL_DATA, GSON.toJson(spellData));
        this.setOwner(caster);

        String name = holder.get(MapField.ENTITY_NAME);
        entityData.set(ENTITY_NAME, name);

    }
}
