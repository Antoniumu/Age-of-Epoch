package com.robertx22.age_of_exile.tpa;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent per-player TPA state.
 *
 * <p>Charges regenerate in real time, including while the player is offline. The
 * timestamp of the next charge is saved instead of a remaining cooldown so a
 * restart does not reset or pause regeneration.</p>
 */
public class TpaData {

    public static final int MAX_CHARGES = 10;
    public static final long RECHARGE_MILLIS = 10L * 60L * 1000L;

    private static final String CHARGES = "charges";
    private static final String NEXT_RECHARGE_AT = "next_recharge_at";
    private static final String TRUSTED_PLAYERS = "trusted_players";
    private static final String UUID_KEY = "uuid";
    private static final String NAME_KEY = "name";

    private int charges = MAX_CHARGES;
    private long nextRechargeAt = 0L;
    private final LinkedHashMap<UUID, String> trustedPlayers = new LinkedHashMap<>();

    /** Returns the current amount after applying all elapsed regeneration steps. */
    public int getCharges(long now) {
        recharge(now);
        return charges;
    }

    /** Returns zero at the charge cap, otherwise the time until the next charge. */
    public long getNextRechargeDelayMillis(long now) {
        recharge(now);
        if (charges >= MAX_CHARGES) {
            return 0L;
        }
        return Math.max(0L, nextRechargeAt - now);
    }

    /**
     * Consumes one charge if available. The regeneration timer starts when the
     * first charge below the cap is consumed and is not restarted by later uses.
     */
    public boolean consumeCharge(long now) {
        recharge(now);
        if (charges <= 0) {
            return false;
        }

        if (charges == MAX_CHARGES) {
            nextRechargeAt = now + RECHARGE_MILLIS;
        }
        charges--;
        return true;
    }

    private void recharge(long now) {
        if (charges >= MAX_CHARGES) {
            charges = MAX_CHARGES;
            nextRechargeAt = 0L;
            return;
        }

        charges = Math.max(0, charges);

        // Also repairs old or partially written data with no timer.
        if (nextRechargeAt <= 0L) {
            nextRechargeAt = now + RECHARGE_MILLIS;
            return;
        }

        if (now < nextRechargeAt) {
            return;
        }

        long regenerated = (now - nextRechargeAt) / RECHARGE_MILLIS + 1L;
        int missing = MAX_CHARGES - charges;

        if (regenerated >= missing) {
            charges = MAX_CHARGES;
            nextRechargeAt = 0L;
        } else {
            charges += (int) regenerated;
            nextRechargeAt += regenerated * RECHARGE_MILLIS;
        }
    }

    public boolean trusts(UUID playerId) {
        return trustedPlayers.containsKey(playerId);
    }

    /** Adds a trusted player or refreshes their cached display name. */
    public void trust(UUID playerId, String displayName) {
        trustedPlayers.put(playerId, displayName == null || displayName.isBlank()
            ? playerId.toString()
            : displayName);
    }

    public boolean untrust(UUID playerId) {
        return trustedPlayers.remove(playerId) != null;
    }

    /** A copy, so command code cannot accidentally bypass persistence methods. */
    public Map<UUID, String> getTrustedPlayers() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(trustedPlayers));
    }

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(CHARGES, charges);
        tag.putLong(NEXT_RECHARGE_AT, nextRechargeAt);

        ListTag trusted = new ListTag();
        trustedPlayers.forEach((id, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(UUID_KEY, id.toString());
            entry.putString(NAME_KEY, name);
            trusted.add(entry);
        });
        tag.put(TRUSTED_PLAYERS, trusted);
        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        charges = MAX_CHARGES;
        nextRechargeAt = 0L;
        trustedPlayers.clear();

        if (tag == null) {
            return;
        }

        charges = Math.max(0, Math.min(MAX_CHARGES, tag.getIntOr(CHARGES, MAX_CHARGES)));
        nextRechargeAt = Math.max(0L, tag.getLongOr(NEXT_RECHARGE_AT, 0L));

        ListTag trusted = tag.getListOrEmpty(TRUSTED_PLAYERS);
        for (int i = 0; i < trusted.size(); i++) {
            CompoundTag entry = trusted.getCompoundOrEmpty(i);
            String idString = entry.getStringOr(UUID_KEY, "");
            if (idString.isEmpty()) {
                continue;
            }

            try {
                UUID id = UUID.fromString(idString);
                String name = entry.getStringOr(NAME_KEY, id.toString());
                trust(id, name);
            } catch (IllegalArgumentException ignored) {
                // Skip only the malformed entry; the rest of the player data stays usable.
            }
        }

        if (charges >= MAX_CHARGES) {
            nextRechargeAt = 0L;
        }
    }
}
