package com.robertx22.age_of_exile.dimension.dungeon_data;

import com.robertx22.age_of_exile.database.data.affixes.Affix;
import com.robertx22.age_of_exile.database.data.tiers.base.Difficulty;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.ExactStatData;
import com.robertx22.library_of_exile.utils.RandomUtils;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Storable
public class DungeonAffixes {

    @Store
    public List<String> s = new ArrayList<>();
    @Store
    public List<String> p = new ArrayList<>();

    public List<ExactStatData> getStats(int lvl) {
        List<ExactStatData> list = new ArrayList<>();

        getAffixes().forEach(x -> {
            list.addAll(x.getStats()
                .stream()
                .map(s -> s.ToExactStat(100, lvl))
                .collect(Collectors.toList()));
        });

        return list;
    }

    public List<Affix> getAffixes() {
        List<Affix> list = new ArrayList<>();
        s.forEach(x -> {
            list.add(ExileDB.Affixes()
                .get(x));
        });
        p.forEach(x -> {
            list.add(ExileDB.Affixes()
                .get(x));
        });
        return list;

    }

    public void randomize(Difficulty diff) {
        s.clear();
        p.clear();

        int rank = diff == null ? 0 : Math.max(0, diff.rank);

        // чем выше сложность, тем больше аффиксов вешается на подземелье
        int prefixAmount = rank >= 2 ? 2 : 1;
        int suffixAmount = rank >= 1 ? 2 : 1;

        roll(s, Affix.Type.dungeon_prefix, prefixAmount);
        roll(p, Affix.Type.dungeon_suffix, suffixAmount);
    }

    private void roll(List<String> target, Affix.Type type, int amount) {
        List<Affix> pool = ExileDB.Affixes()
            .getFiltered(x -> x != null && x.type == type);

        for (int i = 0; i < amount && !pool.isEmpty(); i++) {
            Affix affix = RandomUtils.weightedRandom(pool);
            if (affix == null) {
                return;
            }
            target.add(affix.GUID());
            pool = pool.stream()
                .filter(x -> !x.GUID()
                    .equals(affix.GUID()))
                .collect(Collectors.toList());
        }
    }

    /**
     * Собирает читаемое имя подземелья из префиксов/суффиксов. Если аффиксов нет
     * (старые сохранения), возвращается просто "Подземелье".
     */
    public String buildName() {
        List<String> words = new ArrayList<>();

        s.forEach(x -> {
            String n = affixName(x);
            if (!n.isEmpty()) {
                words.add(n);
            }
        });

        words.add("Подземелье");

        p.forEach(x -> {
            String n = affixName(x);
            if (!n.isEmpty()) {
                words.add(n);
            }
        });

        return String.join(" ", words);
    }

    private String affixName(String guid) {
        if (guid == null || !ExileDB.Affixes()
            .isRegistered(guid)) {
            return "";
        }
        Affix affix = ExileDB.Affixes()
            .get(guid);
        return affix == null || affix.loc_name == null ? "" : affix.loc_name;
    }

}
