package com.robertx22.mine_and_slash.gui.wiki.reworked.filters.all;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.gui.wiki.BestiaryEntry;
import com.robertx22.mine_and_slash.gui.wiki.reworked.filters.GroupFilterEntry;
import net.minecraft.network.chat.MutableComponent;

public class UniqueLeagueFilter extends GroupFilterEntry {
    League league;

    public UniqueLeagueFilter(League league) {
        this.league = league;
    }

    @Override
    public boolean isValid(BestiaryEntry e) {
        return e.obj instanceof UniqueGear u && u.league.equals(league.GUID());
    }

    @Override
    public MutableComponent getName() {
        return league.getPrettifiedName();
    }
}
