package com.robertx22.forge_compat.client.gui.overlay;
import net.minecraft.resources.Identifier;
public enum VanillaGuiOverlay {
    VIGNETTE, SPYGLASS, HELMET, FROSTBITE, PORTAL, HOTBAR, CROSSHAIR, BOSS_EVENT_PROGRESS,
    PLAYER_HEALTH, ARMOR_LEVEL, FOOD_LEVEL, PLAYER_LIST, MOUNT_HEALTH, JUMP_BAR, EXPERIENCE_BAR,
    ITEM_NAME, SLEEP_FADE, POTION_ICONS, DEBUG_TEXT, FPS_GRAPH, RECORD_OVERLAY, TITLE_TEXT,
    SUBTITLES, SCOREBOARD, CHAT_PANEL, AIR_LEVEL;
    private final Identifier id = Identifier.withDefaultNamespace(name().toLowerCase());
    public Identifier id() { return id; }
    public VanillaGuiOverlay type() { return this; }
}
