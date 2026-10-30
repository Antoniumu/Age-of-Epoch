package com.robertx22.age_of_exile.player_skills.ingredient;

import com.robertx22.age_of_exile.database.data.stats.types.crafting.craft_grid.BaseCraftGridStat;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftSlotData;
import com.robertx22.age_of_exile.saveclasses.PointData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GridHelper {

    public List<CraftSlotData> ingredients = new ArrayList<>();

    CraftSlotData[][] grid = new CraftSlotData[3][3];

    HashMap<Integer, PointData> placePointMap = new HashMap<>();

    public GridHelper(List<CraftSlotData> ingredients) {
        this.ingredients = ingredients;
        int i = 0;

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 2; y++) {
                // Сетка всегда 3x2; если в сохранении меньше записей (старый NBT
                // или другая версия контейнера), просто оставляем слот пустым.
                grid[x][y] = i < ingredients.size() ? ingredients.get(i) : null;
                placePointMap.put(i, new PointData(x, y));
                i++;
            }
        }

    }

    boolean isValidPoint(int x, int y) {
        return x > -1 && y > -1 && x < 3 && y < 2;
    }

    public void calcStats() {
        for (CraftSlotData x : ingredients) {
            if (x.ing == null) {
                continue;
            }

            if (!x.calc) {

                x.ing.getIngredient().stats.forEach(e -> {

                    if (e.GetStat() instanceof BaseCraftGridStat) {
                        x.calc = true;

                        BaseCraftGridStat stat = (BaseCraftGridStat) e.GetStat();

                        int num = (int) ((e.min + e.max) / 2);

                        float multiadd = -1F + ((100F + num) / 100F);

                        PointData point = placePointMap.get(x.place);

                        if (point == null) {
                            return; // выход из итерации forEach, а не из цикла
                        }

                        stat.getIngredientsAffected(point)
                            .forEach(s -> {
                                if (isValidPoint(s.x, s.y)) {
                                    CraftSlotData data = grid[s.x][s.y];
                                    // В старом NBT могло быть меньше 6 записей — тогда
                                    // часть сетки пустая, и трогать нечего.
                                    if (data != null) {
                                        data.multi += multiadd;
                                    }
                                }
                            });

                    }

                });
            }
        }
    }

}
