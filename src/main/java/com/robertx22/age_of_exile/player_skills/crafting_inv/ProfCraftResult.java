package com.robertx22.age_of_exile.player_skills.crafting_inv;

import com.robertx22.age_of_exile.uncommon.localization.Words;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfCraftResult {

    public boolean canCraft;

    /**
     * Translation key of the failure reason (empty when the craft is possible).
     * The actual localization happens on the client, otherwise the player
     * would see a raw key like "mmorpg.word.noingredients".
     */
    public String reasonKey = "";

    public List<String> reasonArgs = Collections.emptyList();

    public static ProfCraftResult success() {
        ProfCraftResult r = new ProfCraftResult();
        r.canCraft = true;
        r.reasonKey = "";
        r.reasonArgs = Collections.emptyList();
        return r;
    }

    public static ProfCraftResult fail(Words reason, Object... args) {
        ProfCraftResult r = new ProfCraftResult();
        r.canCraft = false;
        r.reasonKey = reason == null ? "" : reason.formattedLocNameLangFileGUID();

        if (args != null && args.length > 0) {
            List<String> list = new ArrayList<>();
            for (Object arg : args) {
                list.add(arg == null ? "" : String.valueOf(arg));
            }
            r.reasonArgs = list;
        }
        return r;
    }
}
