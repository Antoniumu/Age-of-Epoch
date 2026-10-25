package com.robertx22.age_of_exile.capability.player.data;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

@Storable
public class TeamData {
    /** Максимальная длина названия команды (см. {@link TeamNames}). */
    public static final int MAX_NAME_LENGTH = TeamNames.MAX_LENGTH;

    @Store
    public String team_id = "";
    /**
     * Название команды, которое игрок задаёт сам. Пустое — значит команда
     * создана старой версией мода или названия не дали: тогда в интерфейсе
     * показывается короткий id, как раньше.
     */
    @Store
    public String team_name = "";
    @Store
    public String invitedToTeam = "";
    @Store
    public boolean isLeader = false;

    public void joinTeamOf(Player other) {
        this.team_id = Load.playerRPGData(other).team.team_id;
    }

    public void leaveTeam() {
        this.team_id = "";
        this.team_name = "";
        this.isLeader = false;
        this.invitedToTeam = "";
    }

    public boolean isOnTeam() {
        return !team_id.isEmpty();
    }

    /** Что показывать в интерфейсе: заданное название или короткий id. */
    public String displayName() {
        return TeamNames.display(team_name, team_id);
    }

    public boolean isOnSameTeam(Player other) {

        if (team_id.isEmpty() || Load.playerRPGData(other).team.team_id.isEmpty()) {
            return false;
        }

        return team_id.equals(Load.playerRPGData(other).team.team_id);
    }

    /** Чистка названия вынесена в {@link TeamNames} — там же и тесты. */
    public static String sanitizeName(String raw) {
        return TeamNames.sanitize(raw);
    }

    public void createTeam() {
        createTeam("");
    }

    public void createTeam(String name) {
        this.team_id = UUID.randomUUID()
            .toString();
        this.team_name = sanitizeName(name);
        this.isLeader = true;
    }

}
