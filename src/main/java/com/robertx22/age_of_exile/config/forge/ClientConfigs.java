package com.robertx22.age_of_exile.config.forge;

import com.robertx22.age_of_exile.gui.overlays.GuiPosition;
import com.robertx22.age_of_exile.uncommon.enumclasses.PlayerGUIs;
import com.robertx22.forge_compat.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ClientConfigs {

    public static final ForgeConfigSpec clientSpec;
    public static final ClientConfigs CLIENT;

    static {
        final Pair<ClientConfigs, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfigs::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    ClientConfigs(ForgeConfigSpec.Builder b) {
        b.comment("Client Configs")
            .push("general");

        SHOW_LOW_ENERGY_MANA_WARNING = b.define("show_low_mana_warning", true);
        RENDER_SIMPLE_MOB_BAR = b.define("render_mob_bar", true);
        RENDER_DEATH_STATISTICS = b.define("render_death_stats", true);
        RENDER_ITEM_RARITY_BACKGROUND = b.define("render_item_rarity_background", true);
        SHOW_DURABILITY = b.define("show_durability", true);
        RENDER_MOB_HEALTH_GUI = b.define("render_mob_hp_gui", true);
        ONLY_RENDER_MOB_LOOKED_AT = b.define("only_render_mob_looked_at", true);
        SHOW_DURABILITY = b.define("show_durability", true);
        ENABLE_FLOATING_DMG = b.define("render_floating_damage", true);

        b.comment("Mob plates: nameplates rendered above mobs.")
            .push("mob_plates");
        MOB_PLATE_STYLE = b.defineEnum("mob_plate_style", MobPlateStyle.MODERN);
        MOB_PLATE_SCALE = b.defineInRange("mob_plate_scale", 1.0D, 0.6D, 1.8D);
        MOB_PLATE_MAX_DISTANCE = b.defineInRange("mob_plate_max_distance", 24, 8, 64);
        MOB_PLATE_SHOW_LEVEL = b.define("mob_plate_show_level", true);
        MOB_PLATE_SHOW_HP_TEXT = b.define("mob_plate_show_hp_text", true);
        MOB_PLATE_ONLY_LOOKED_AT = b.define("mob_plate_only_looked_at", false);
        MOB_PLATE_HIDE_FULL_HEALTH = b.define("mob_plate_hide_full_health", false);
        MOB_PLATE_SHOW_PLAYERS = b.define("mob_plate_show_players", true);
        b.pop();

        ITEM_RARITY_OPACITY = b.defineInRange("ITEM_RARITY_OPACITY", 0.75F, 0, 1F);

        GUI_POSITION = b.defineEnum("GUI_POSITION", GuiPosition.TOP_LEFT);
        ITEM_RARITY_BACKGROUND_TYPE = b.defineEnum("ITEM_RARITY_BACKGROUND_TYPE", GlintType.FULL);
        PLAYER_GUI_TYPE = b.defineEnum("PLAYER_GUI_TYPE", PlayerGUIs.RPG);

        b.pop();
    }

    public ForgeConfigSpec.BooleanValue SHOW_LOW_ENERGY_MANA_WARNING;
    public ForgeConfigSpec.BooleanValue ENABLE_FLOATING_DMG;
    public ForgeConfigSpec.BooleanValue RENDER_SIMPLE_MOB_BAR;
    public ForgeConfigSpec.BooleanValue RENDER_DEATH_STATISTICS;
    public ForgeConfigSpec.BooleanValue RENDER_ITEM_RARITY_BACKGROUND;
    public ForgeConfigSpec.BooleanValue SHOW_DURABILITY;
    public ForgeConfigSpec.BooleanValue RENDER_MOB_HEALTH_GUI;
    public ForgeConfigSpec.BooleanValue ONLY_RENDER_MOB_LOOKED_AT;

    public ForgeConfigSpec.EnumValue<MobPlateStyle> MOB_PLATE_STYLE;
    public ForgeConfigSpec.DoubleValue MOB_PLATE_SCALE;
    public ForgeConfigSpec.IntValue MOB_PLATE_MAX_DISTANCE;
    public ForgeConfigSpec.BooleanValue MOB_PLATE_SHOW_LEVEL;
    public ForgeConfigSpec.BooleanValue MOB_PLATE_SHOW_HP_TEXT;
    public ForgeConfigSpec.BooleanValue MOB_PLATE_ONLY_LOOKED_AT;
    public ForgeConfigSpec.BooleanValue MOB_PLATE_HIDE_FULL_HEALTH;
    public ForgeConfigSpec.BooleanValue MOB_PLATE_SHOW_PLAYERS;

    public ForgeConfigSpec.EnumValue<GlintType> ITEM_RARITY_BACKGROUND_TYPE;
    public ForgeConfigSpec.EnumValue<GuiPosition> GUI_POSITION;
    public ForgeConfigSpec.EnumValue<PlayerGUIs> PLAYER_GUI_TYPE;
    public ForgeConfigSpec.DoubleValue ITEM_RARITY_OPACITY;

    public int REMOVE_EMPTY_TOOLTIP_LINES_IF_MORE_THAN_X_LINES = 35;

    public enum GlintType {
        BORDER, FULL;
    }

    public static ClientConfigs getConfig() {
        return CLIENT;
    }

}
