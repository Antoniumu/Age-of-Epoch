package com.robertx22.age_of_exile.ascension.fx;

import com.robertx22.age_of_exile.aoe_data.database.spells.PartBuilder;
import com.robertx22.age_of_exile.database.data.spells.components.ComponentPart;
import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SpellAction;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;

/**
 * Тонкий билдер 3D-фигур прилива для определений заклинаний Вознесённых классов.
 * Позволяет описывать зрелищность навыка в одну строку:
 * {@code TideFx.cast(Shape.FUNNEL_VORTEX, 220, 3.5, 2.5)}.
 */
public class TideFx {

    /**
     * Одноразовая 3D-фигура при касте/удаче (onCast, onExpire...).
     */
    public static ComponentPart cast(TideShapeAction.Shape shape, double count, double radius, double height) {
        return PartBuilder.justAction(SpellAction.TIDE_SHAPE.create(shape, count, radius, height));
    }

    /**
     * Одноразовая 3D-фигура с вылетом частиц (motion multiplier).
     */
    public static ComponentPart castBurst(TideShapeAction.Shape shape, double count, double radius, double height, double motion) {
        MapHolder m = SpellAction.TIDE_SHAPE.create(shape, count, radius, height);
        m.put(MapField.MOTION_MULTI, motion);
        return PartBuilder.justAction(m);
    }

    /**
     * Фигура, пере-рисующаяся каждые ticks тиков (для водоворотов, аур, огней).
     */
    public static ComponentPart tick(double ticks, TideShapeAction.Shape shape, double count, double radius, double height) {
        return PartBuilder.particleOnTick(ticks, SpellAction.TIDE_SHAPE.create(shape, count, radius, height));
    }

    /**
     * Фигура с фазовым поворотом (градусы) — для дуг/колец, ориентированных иначе.
     */
    public static ComponentPart castRotated(TideShapeAction.Shape shape, double count, double radius, double height, double phaseDeg) {
        MapHolder m = SpellAction.TIDE_SHAPE.create(shape, count, radius, height);
        m.put(MapField.DISTANCE, phaseDeg);
        return PartBuilder.justAction(m);
    }
}
