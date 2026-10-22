package com.robertx22.mine_and_slash.gui.wiki.reworked.filters.all;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.addon.ExtendedOrb;
import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.mine_and_slash.gui.wiki.BestiaryEntry;
import com.robertx22.mine_and_slash.gui.wiki.reworked.filters.GroupFilterEntry;
import net.minecraft.network.chat.MutableComponent;

public class CurrencyLeagueFilter extends GroupFilterEntry {
    League league;

    public CurrencyLeagueFilter(League league) {
        this.league = league;
    }

    @Override
    public boolean isValid(BestiaryEntry e) {
        var ext = ExtendedOrb.from(e.obj);

        if (ext != null) {
            if (ext.drop_req.hasLeague() && !ext.drop_req.getLeague().GUID().equals(league.GUID())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableComponent getName() {
        return league.getPrettifiedName();
    }
}
