package com.robertx22.age_of_exile.ascension.fx;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SpellAction;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ParticleUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;

import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.*;

/**
 * Сердце «не-ванильных» эффектов Вознесённых классов: рисует в мире
 * <b>настоящие 3D-образы</b> — дуги бича, двойные спирали, торы, воронки,
 * сферы-оболочки, бегущие волновые кольца. Каждая частица ставится на точку
 * параметрической кривой (или поверхности), поэтому итог читается как объёмная
 * фигура воды и света, а не случайное облако брызг.
 *
 * <p>Работает только на клиенте (как и остальные vanity-экшены заклинаний).
 * Параметры пробрасываются стандартными полями MapField, поэтому заклинания
 * сериализуются без новых ключей:
 * <ul>
 *   <li>{@code PARTICLE_SHAPE} — имя {@link Shape};</li>
 *   <li>{@code PARTICLE_COUNT}, {@code RADIUS}, {@code HEIGHT} — геометрия;</li>
 *   <li>{@code DISTANCE} — фаза/поворот фигуры в градусах (по курсу смотрителя);</li>
 *   <li>{@code MOTION_MULTI} — скорость вылета частиц (вдоль нормали фигуры);</li>
 *   <li>{@code PARTICLE_TYPE} — частица (по умолчанию mmorpg:tide_glow).</li>
 * </ul>
 */
public class TideShapeAction extends SpellAction {

    public enum Shape {
        /**
         * Дуга бича: широкий полукруг перед кастером с параболой высоты
         */
        WHIP_ARC,
        /**
         * Две встречные спирали, поднимающиеся вверх
         */
        HELIX_DOUBLE,
        /**
         * Тор: горизонтальное кольцо с лёгким «дыханием» по вертикали
         */
        TORUS,
        /**
         * Воронка-водоворот: радиус уменьшается с высотой
         */
        FUNNEL_VORTEX,
        /**
         * Оболочка сферы (равномерное распределение Фибоначчи)
         */
        SPHERE_SHELL,
        /**
         * Кольцо волны на земле, разбегающееся наружу
         */
        WAVE_RING,
        /**
         * Кольца, падающие ВНУТРЬ к центру (стягивание)
         */
        COLLAPSE_RING,
        /**
         * Вертикальный гейзер призматических искр
         */
        PRISM_GEYSER
    }

    public TideShapeAction() {
        super(Arrays.asList(PARTICLE_SHAPE, PARTICLE_COUNT, RADIUS));
    }

    @Override
    public String GUID() {
        return "tide_shape";
    }

    public MapHolder create(Shape shape, double count, double radius, double height) {
        MapHolder c = new MapHolder();
        c.type = GUID();
        c.put(PARTICLE_SHAPE, shape.name());
        c.put(PARTICLE_COUNT, count);
        c.put(RADIUS, radius);
        c.put(HEIGHT, height);
        c.put(PARTICLE_TYPE, SlashRef.MODID + ":tide_glow");
        c.put(MOTION_MULTI, 0.0D);
        c.put(DISTANCE, 0.0D);
        this.validate(c);
        return c;
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        if (ctx.world == null || !ctx.world.isClientSide()) {
            return;
        }

        Shape shape;
        try {
            shape = Shape.valueOf(data.get(PARTICLE_SHAPE));
        } catch (Exception e) {
            shape = Shape.WAVE_RING;
        }

        ParticleOptions particle = data.getParticle();

        int count = Math.max(1, data.get(PARTICLE_COUNT)
            .intValue());
        float radius = Math.max(0.1F, data.get(RADIUS)
            .floatValue() * ctx.calculatedSpellData.area_multi);
        float height = data.getOrDefault(HEIGHT, 1.0D)
            .floatValue();
        double phase = Math.toRadians(data.getOrDefault(DISTANCE, 0D)
            .doubleValue());
        double motionMulti = data.getOrDefault(MOTION_MULTI, 0D)
            .doubleValue();

        Vec3 o = ctx.vecPos;
        // Фигуры, завязанные на курс кастера (дуга бича), поворачиваются по yaw.
        double yaw = Math.toRadians(ctx.caster != null ? ctx.caster.getYRot() : 0.0F);

        net.minecraft.util.RandomSource rand = ctx.world.getRandom();

        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 0.5D : (double) i / (double) (count - 1);
            Vec3 p;
            Vec3 v = Vec3.ZERO;

            switch (shape) {
                case WHIP_ARC: {
                    // пологая дуга шириной ~2*radius перед кастером:
                    // угол от -100° до +100° (у кастера за спиной 0 — смотрим вперёд)
                    double sweep = -Math.PI * 0.62 + t * Math.PI * 1.24;
                    double ang = yaw + Math.PI / 2 + sweep; // yaw-поправка: 0 курса смотрит на +Z
                    double r = radius * (0.75D + 0.35D * Math.abs(Math.sin(t * Math.PI)));
                    double px = o.x - Math.sin(ang) * r;
                    double pz = o.z + Math.cos(ang) * r;
                    double py = o.y + height + Math.sin(t * Math.PI) * (0.9D + radius * 0.15D);
                    p = new Vec3(px, py, pz);
                    // кончик бича «щёлкает» — частицам даётся тангенциальная скорость
                    v = new Vec3(-Math.sin(ang + 0.4), 0.12D, Math.cos(ang + 0.4)).scale(motionMulti * 0.18D);
                    break;
                }
                case HELIX_DOUBLE: {
                    int arm = i % 2; // две встречные ветви
                    double tt = (double) (i / 2) / (double) Math.max(1, count / 2);
                    double ang = phase + tt * Math.PI * 4 + arm * Math.PI;
                    double r = radius * (1.0D - 0.35D * tt);
                    p = new Vec3(o.x + Math.cos(ang) * r, o.y + height * tt, o.z + Math.sin(ang) * r);
                    v = new Vec3(Math.cos(ang), 0.35D, Math.sin(ang)).scale(motionMulti * 0.15D);
                    break;
                }
                case TORUS: {
                    double ang = phase + t * Math.PI * 2;
                    double r = radius * (1.0D + 0.08D * Math.sin(ang * 3));
                    double py = o.y + height + Math.sin(ang * 2) * 0.12D;
                    p = new Vec3(o.x + Math.cos(ang) * r, py, o.z + Math.sin(ang) * r);
                    break;
                }
                case FUNNEL_VORTEX: {
                    double tt = t;
                    double r = radius * (1.0D - 0.72D * tt);
                    double ang = phase + tt * Math.PI * 7;
                    p = new Vec3(o.x + Math.cos(ang) * r, o.y + height * tt, o.z + Math.sin(ang) * r);
                    // тангенциальная скорость по воронке
                    v = new Vec3(-Math.sin(ang), 0.05D, Math.cos(ang)).scale(0.12D + motionMulti * 0.1D);
                    break;
                }
                case SPHERE_SHELL: {
                    // равномерное распределение Фибоначчи по сфере
                    double ga = Math.PI * (3.0D - Math.sqrt(5.0D));
                    double yy = 1.0D - 2.0D * (i + 0.5D) / (double) count;
                    double rr = Math.sqrt(Math.max(0.0D, 1.0D - yy * yy));
                    double ang = ga * i + phase;
                    p = new Vec3(o.x + Math.cos(ang) * rr * radius, o.y + height + yy * radius * 0.7D, o.z + Math.sin(ang) * rr * radius);
                    Vec3 dir = p.subtract(new Vec3(o.x, o.y + height, o.z)).normalize();
                    v = dir.scale(motionMulti * 0.08D);
                    break;
                }
                case WAVE_RING: {
                    double ang = phase + t * Math.PI * 2;
                    double jitter = (rand.nextDouble() - 0.5D) * 0.15D;
                    double r = radius + jitter;
                    p = new Vec3(o.x + Math.cos(ang) * r, o.y + height, o.z + Math.sin(ang) * r);
                    v = new Vec3(Math.cos(ang), 0.04D, Math.sin(ang)).scale(0.25D + motionMulti * 0.2D);
                    break;
                }
                case COLLAPSE_RING: {
                    double ang = phase + t * Math.PI * 2;
                    double r = radius * (0.6D + 0.4D * rand.nextDouble());
                    double py = o.y + height + rand.nextDouble() * 0.8D;
                    p = new Vec3(o.x + Math.cos(ang) * r, py, o.z + Math.sin(ang) * r);
                    // движение ВНУТРЬ, к центру — водоворот затягивает
                    Vec3 toCenter = new Vec3(o.x - p.x, 0, o.z - p.z).normalize();
                    v = toCenter.scale(0.30D + motionMulti * 0.2D);
                    break;
                }
                case PRISM_GEYSER: {
                    double a = rand.nextDouble() * Math.PI * 2;
                    double r = radius * Math.sqrt(rand.nextDouble());
                    double py = o.y + height * t + t * t * radius * 1.4D;
                    p = new Vec3(o.x + Math.cos(a) * r * 0.5D, py, o.z + Math.sin(a) * r * 0.5D);
                    v = new Vec3(0, 0.25D + motionMulti * 0.1D, 0);
                    break;
                }
                default:
                    continue;
            }

            ParticleUtils.spawn(particle, ctx.world, p, v);
        }
    }
}
