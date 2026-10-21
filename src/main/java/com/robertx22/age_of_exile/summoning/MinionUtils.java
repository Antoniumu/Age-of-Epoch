package com.robertx22.age_of_exile.summoning;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.stats.Stat;
import com.robertx22.age_of_exile.database.data.stats.types.summoning.SummonStat;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Прислужники древа «Божественный» (data/mmorpg/talent_tree/divine.json).
 * <p>
 * Две взаимоисключающие ветки:
 * <ul>
 * <li><b>Некромант</b> — поднимает убитых им врагов ({@link #summonUndead}), лимит:
 * {@link SummonStat#MAX_UNDEAD_MINIONS};</li>
 * <li><b>Призыватель</b> — держит рядом стаю зверей ({@link #summonBeast}), лимит:
 * {@link SummonStat#MAX_BEAST_MINIONS}, вид зверя выбирается перком (волки / медведь / мамонт).</li>
 * </ul>
 * Прислужник помечается тегами сущности (они же пишутся в NBT и переживают перезаход):
 * {@link #TAG}, владелец, время исчезновения и вид. Сила прислужника — это характеристики хозяина
 * ({@link SummonStat}), которые навешиваются модификаторами атрибутов при призыве.
 */
public class MinionUtils {

    /** Маркер «это прислужник игрока». */
    public static final String TAG = "mmorpg_minion";

    public static final String OWNER_PREFIX = "mmorpg_minion_owner:";
    public static final String EXPIRE_PREFIX = "mmorpg_minion_expire:";
    public static final String KIND_PREFIX = "mmorpg_minion_kind:";

    /** Базовое время жизни прислужника, тиков (30 секунд). */
    public static final int BASE_LIFETIME_TICKS = 20 * 30;
    /** Максимальное время жизни, тиков (5 минут). */
    public static final int MAX_LIFETIME_TICKS = 20 * 60 * 5;

    public static final double FOLLOW_DISTANCE = 10;
    public static final double TELEPORT_DISTANCE = 28;
    public static final double SEARCH_RADIUS = 20;

    /** Как часто пересчитывать зверей у хозяина, тиков. */
    public static final int BEAST_CHECK_TICKS = 40;

    public enum Kind {
        UNDEAD("undead", "Нежить"),
        WOLF("wolf", "Волк"),
        BEAR("bear", "Медведь"),
        MAMMOTH("mammoth", "Боевой мамонт");

        public final String id;
        public final String locname;

        Kind(String id, String locname) {
            this.id = id;
            this.locname = locname;
        }

        public static Kind fromId(String id) {
            for (Kind k : values()) {
                if (k.id.equals(id)) {
                    return k;
                }
            }
            return null;
        }

        public boolean isBeast() {
            return this != UNDEAD;
        }
    }

    // ================================ чтение тегов ================================

    public static boolean isMinion(LivingEntity en) {
        try {
            return en != null && en.getTags()
                .contains(TAG);
        } catch (Exception e) {
            return false;
        }
    }

    private static String tagValue(LivingEntity en, String prefix) {
        try {
            for (String tag : en.getTags()) {
                if (tag.startsWith(prefix)) {
                    return tag.substring(prefix.length());
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static UUID getOwnerUUID(LivingEntity en) {
        String str = tagValue(en, OWNER_PREFIX);
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static Player getOwner(LivingEntity en) {
        UUID id = getOwnerUUID(en);
        if (id == null) {
            return null;
        }
        Level level = en.level();
        if (level instanceof ServerLevel) {
            return ((ServerLevel) level).getPlayerByUUID(id);
        }
        return null;
    }

    public static Kind getKind(LivingEntity en) {
        return Kind.fromId(tagValue(en, KIND_PREFIX));
    }

    public static long getExpireTime(LivingEntity en) {
        String str = tagValue(en, EXPIRE_PREFIX);
        if (str == null) {
            return 0;
        }
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void setTag(LivingEntity en, String prefix, String value) {
        String old = tagValue(en, prefix);
        if (old != null) {
            en.removeTag(prefix + old);
        }
        en.addTag(prefix + value);
    }

    // ================================ чтение статов хозяина ================================

    public static float stat(Player owner, Stat stat) {
        try {
            EntityData data = Load.Unit(owner);
            if (data == null || data.getUnit() == null) {
                return 0;
            }
            return data.getUnit()
                .getCalculatedStat(stat)
                .getValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Как долго живёт прислужник, с учётом {@link SummonStat#SUMMON_DURATION}. */
    public static int lifetimeTicks(Player owner) {
        float multi = 1F + stat(owner, SummonStat.SUMMON_DURATION) / 100F;
        int ticks = (int) (BASE_LIFETIME_TICKS * multi);
        return Math.max(20, Math.min(ticks, MAX_LIFETIME_TICKS));
    }

    // ================================ призыв ================================

    public static boolean summonUndead(Player owner, BlockPos pos) {
        if (owner == null || pos == null) {
            return false;
        }
        int max = (int) stat(owner, SummonStat.MAX_UNDEAD_MINIONS);
        if (max < 1) {
            return false;
        }
        if (countMinions(owner, Kind.UNDEAD) >= max) {
            return false;
        }
        return spawnMinion(owner, randomUndeadType(owner), Kind.UNDEAD, pos, null) != null;
    }

    public static boolean summonBeast(Player owner, Kind kind, BlockPos pos) {
        if (owner == null || pos == null || kind == null || !kind.isBeast()) {
            return false;
        }
        int max = (int) stat(owner, SummonStat.MAX_BEAST_MINIONS);
        if (max < 1) {
            return false;
        }
        if (countMinions(owner, kind) >= max) {
            return false;
        }
        return spawnMinion(owner, beastType(kind), kind, pos, kind.locname) != null;
    }

    /** Вид зверя, которого призывает игрок (или null, если ветка призывателя не выбрана). */
    public static Kind getBeastKind(Player owner) {
        if (stat(owner, SummonStat.BEAST_PACK_MAMMOTH) > 0) {
            return Kind.MAMMOTH;
        }
        if (stat(owner, SummonStat.BEAST_PACK_BEAR) > 0) {
            return Kind.BEAR;
        }
        if (stat(owner, SummonStat.BEAST_PACK_WOLVES) > 0) {
            return Kind.WOLF;
        }
        return null;
    }

    private static EntityType<?> beastType(Kind kind) {
        switch (kind) {
            case BEAR:
                return EntityType.POLAR_BEAR;
            case MAMMOTH:
                // самого мамонта в ванили нет — берём самое крупное боевое животное
                return EntityType.RAVAGER;
            case WOLF:
            default:
                return EntityType.WOLF;
        }
    }

    /** Нежить становится сильнее вместе с уровнем хозяина. */
    private static EntityType<?> randomUndeadType(Player owner) {
        int lvl = 1;
        EntityData data = Load.Unit(owner);
        if (data != null) {
            lvl = data.getLevel();
        }

        List<EntityType<?>> list = new ArrayList<>();
        list.add(EntityType.ZOMBIE);
        list.add(EntityType.SKELETON);

        if (lvl >= 10) {
            list.add(EntityType.HUSK);
            list.add(EntityType.STRAY);
        }
        if (lvl >= 25) {
            list.add(EntityType.DROWNED);
        }
        if (lvl >= 40) {
            list.add(EntityType.WITHER_SKELETON);
        }

        return list.get(RandomUtils.RandomRange(0, list.size() - 1));
    }

    private static LivingEntity spawnMinion(Player owner, EntityType<?> type, Kind kind, BlockPos pos, String customName) {

        if (owner == null || type == null || kind == null || pos == null) {
            return null;
        }
        if (!(owner.level() instanceof ServerLevel)) {
            return null;
        }

        ServerLevel world = (ServerLevel) owner.level();

        try {

            Entity created = type.create(world, EntitySpawnReason.MOB_SUMMONED);

            if (!(created instanceof Mob)) {
                return null;
            }

            Mob mob = (Mob) created;

            mob.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, world.random.nextFloat() * 360F, 0F);
            mob.setPersistenceRequired();

            try {
                mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
            } catch (Exception ignored) {
                // сигнатура finalizeSpawn differs between versions, прислужник и без неё рабочий
            }

            mob.addTag(TAG);
            setTag(mob, OWNER_PREFIX, owner.getUUID()
                .toString());
            setTag(mob, KIND_PREFIX, kind.id);
            setTag(mob, EXPIRE_PREFIX, Long.toString(world.getGameTime() + lifetimeTicks(owner)));

            if (customName != null && !customName.isEmpty()) {
                mob.setCustomName(Component.literal(customName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
                mob.setCustomNameVisible(false);
            }

            // уровень и статы моба — как у любого моба рядом с хозяином
            EntityData mobData = Load.Unit(mob);
            if (mobData != null) {
                mobData.setType();
                mobData.setLevel(Math.max(1, Load.Unit(owner)
                    .getLevel()));
                mobData.setMobStats = true;
                mobData.forceRecalculateStats();
            }

            applyPerkStats(mob, owner, kind);

            if (!world.addFreshEntity(mob)) {
                return null;
            }

            SoundUtils.playSound(mob, kind == Kind.UNDEAD ? SoundEvents.EVOKER_PREPARE_SUMMON : SoundEvents.WOLF_HOWL);

            return mob;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Сила прислужника берётся из статов хозяина и навешивается модификаторами атрибутов. */
    public static void applyPerkStats(LivingEntity minion, Player owner, Kind kind) {

        float healthPerc = stat(owner, SummonStat.SUMMON_HEALTH);

        float dmgPerc = stat(owner, SummonStat.SUMMON_DAMAGE);
        if (kind == Kind.UNDEAD) {
            dmgPerc += stat(owner, SummonStat.UNDEAD_MINION_DAMAGE);
        } else {
            dmgPerc += stat(owner, SummonStat.BEAST_MINION_DAMAGE);
        }

        float speedPerc = stat(owner, SummonStat.SUMMON_SPEED);
        float armor = stat(owner, SummonStat.SUMMON_ARMOR);

        addModifier(minion, Attributes.MAX_HEALTH, "minion_health", healthPerc / 100F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(minion, Attributes.ATTACK_DAMAGE, "minion_damage", dmgPerc / 100F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(minion, Attributes.MOVEMENT_SPEED, "minion_speed", speedPerc / 100F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(minion, Attributes.ARMOR, "minion_armor", armor, AttributeModifier.Operation.ADD_VALUE);
        addModifier(minion, Attributes.KNOCKBACK_RESISTANCE, "minion_knockback", 0.4F, AttributeModifier.Operation.ADD_VALUE);
        addModifier(minion, Attributes.FOLLOW_RANGE, "minion_follow_range", 24F, AttributeModifier.Operation.ADD_VALUE);

        minion.setHealth(minion.getMaxHealth());
    }

    private static void addModifier(LivingEntity en, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String id, float value, AttributeModifier.Operation operation) {
        try {
            AttributeInstance inst = en.getAttribute(attribute);
            if (inst == null) {
                return; // у части мобов атрибута нет (например, ATTACK_DAMAGE у скелета)
            }
            Identifier modId = Identifier.fromNamespaceAndPath(SlashRef.MODID, id);
            if (inst.hasModifier(modId)) {
                inst.removeModifier(modId);
            }
            inst.addTransientModifier(new AttributeModifier(modId, value, operation));
        } catch (Exception e) {
        }
    }

    // ================================ подсчёт и обслуживание ================================

    public static List<LivingEntity> getMinions(Player owner, Kind kind) {

        List<LivingEntity> list = new ArrayList<>();

        if (owner == null) {
            return list;
        }

        try {
            AABB box = new AABB(owner.blockPosition()).inflate(64);
            List<LivingEntity> found = owner.level()
                .getEntitiesOfClass(LivingEntity.class, box, x -> isMinion(x) && owner.getUUID()
                    .equals(getOwnerUUID(x)));

            for (LivingEntity en : found) {
                if (kind == null || getKind(en) == kind) {
                    list.add(en);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static int countMinions(Player owner, Kind kind) {
        return getMinions(owner, kind).size();
    }

    /** Тик прислужника: срок жизни, хозяин, цель. */
    public static void onMinionTick(LivingEntity minion) {

        Level level = minion.level();

        if (level.isClientSide() || !(level instanceof ServerLevel)) {
            return;
        }

        ServerLevel world = (ServerLevel) level;

        if (world.getGameTime() > getExpireTime(minion)) {
            despawn(minion);
            return;
        }

        Player owner = getOwner(minion);

        if (owner == null || !owner.isAlive() || owner.level() != level) {
            despawn(minion);
            return;
        }

        if (!(minion instanceof Mob)) {
            return;
        }

        Mob mob = (Mob) minion;

        double distanceToOwner = mob.distanceTo(owner);

        if (distanceToOwner > TELEPORT_DISTANCE) {
            mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            return;
        }

        LivingEntity target = mob.getTarget();

        boolean targetIsFine = target != null && target.isAlive() && isEnemyOf(owner, target)
            && target.distanceTo(owner) < SEARCH_RADIUS * 2;

        if (!targetIsFine) {
            mob.setTarget(findTarget(world, owner, mob));
        }

        if (mob.getTarget() == null && distanceToOwner > FOLLOW_DISTANCE && mob.tickCount % 20 == 0) {
            try {
                mob.getNavigation()
                    .moveTo(owner, 1.2D);
            } catch (Exception e) {
            }
        }
    }

    /** Тик хозяина: держим нужное количество зверей рядом. */
    public static void onPlayerTick(Player player) {

        if (player.level()
            .isClientSide()) {
            return;
        }
        if (player.tickCount % BEAST_CHECK_TICKS != 0) {
            return;
        }

        Kind kind = getBeastKind(player);

        if (kind == null) {
            return;
        }

        int max = (int) stat(player, SummonStat.MAX_BEAST_MINIONS);

        if (max < 1) {
            return;
        }
        if (countMinions(player, kind) >= max) {
            return;
        }

        BlockPos pos = player.blockPosition()
            .offset(RandomUtils.RandomRange(-3, 3), 0, RandomUtils.RandomRange(-3, 3));

        summonBeast(player, kind, pos);
    }

    public static boolean isEnemyOf(Player owner, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player) {
            return false; // прислужники не воюют с игроками
        }
        if (isMinion(target)) {
            return false;
        }
        return target instanceof Enemy;
    }

    private static LivingEntity findTarget(ServerLevel world, Player owner, Mob mob) {
        try {
            AABB box = new AABB(mob.blockPosition()).inflate(SEARCH_RADIUS);

            List<Mob> list = world.getEntitiesOfClass(Mob.class, box, x -> isEnemyOf(owner, x));

            LivingEntity closest = null;
            double bestDistance = Double.MAX_VALUE;

            for (Mob en : list) {
                double dist = en.distanceTo(mob);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    closest = en;
                }
            }

            return closest;
        } catch (Exception e) {
            return null;
        }
    }

    public static void despawn(LivingEntity minion) {
        try {
            if (minion == null || !minion.isAlive()) {
                return;
            }
            SoundUtils.playSound(minion, SoundEvents.GENERIC_EXTINGUISH_FIRE);
            minion.discard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
