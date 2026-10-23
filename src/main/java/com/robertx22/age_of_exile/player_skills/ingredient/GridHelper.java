package com.robertx22.age_of_exile.player_skills.ingredient;

import com.robertx22.age_of_exile.database.data.stats.types.crafting.craft_grid.BaseCraftGridStat;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftSlotData;
import com.robertx22.age_of_exile.saveclasses.PointData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GridHelper {

    public static final int GRID_WIDTH = 3;
    public static final int GRID_HEIGHT = 2;

    public List<CraftSlotData> ingredients = new ArrayList<>();

    CraftSlotData[][] grid = new CraftSlotData[GRID_WIDTH][GRID_HEIGHT];

    HashMap<Integer, PointData> placePointMap = new HashMap<>();

    public GridHelper(List<CraftSlotData> ingredients) {
        this.ingredients = ingredients == null ? new ArrayList<>() : ingredients;

        int i = 0;

        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                grid[x][y] = getSlot(i);
                placePointMap.put(i, new PointData(x, y));
                i++;
            }
        }

    }

    private CraftSlotData getSlot(int index) {
        // old items can have a shorter list than the current grid size
        if (index < 0 || index >= ingredients.size()) {
            return new CraftSlotData();
        }
        CraftSlotData data = ingredients.get(index);
        return data == null ? new CraftSlotData() : data;
    }

    boolean isValidPoint(int x, int y) {
        return x > -1 && y > -1 && x < GRID_WIDTH && y < GRID_HEIGHT;
    }

    public void calcStats() {
        for (CraftSlotData x : ingredients) {
            if (x == null || x.ing == null || x.ing.getIngredient() == null) {
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
                            return;
                        }

                        stat.getIngredientsAffected(point)
                            .forEach(s -> {
                                if (isValidPoint(s.x, s.y)) {
                                    CraftSlotData data = grid[s.x][s.y];
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
