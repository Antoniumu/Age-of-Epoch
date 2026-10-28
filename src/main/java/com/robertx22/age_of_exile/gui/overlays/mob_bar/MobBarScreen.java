package com.robertx22.age_of_exile.gui.overlays.mob_bar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.config.forge.ClientConfigs;
import com.robertx22.age_of_exile.database.data.mob_affixes.MobAffix;
import com.robertx22.age_of_exile.database.data.rarities.MobRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.HealthUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LookUtils;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.common.CompatChatFormatting;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

public class MobBarScreen extends AbstractGui {

    private final Minecraft mc;

    static final Identifier BASE_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/overlay/base.png");
    static final Identifier HEALTH_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/overlay/health.png");

    static final int BAR_WIDTH = 107;
    static final int BAR_HEIGHT = 11;
    static final int INNER_WIDTH = 101;
    static final int INNER_HEIGHT = 5;

    private static LivingEntity currentTarget;
    private static long lastSeenTimeMs = 0L;

    public MobBarScreen() {
        super();
        this.mc = Minecraft.getInstance();
    }

    public static void onMobDamaged(LivingEntity entity) {
        if (entity != null && !(entity instanceof ArmorStand)) {
            currentTarget = entity;
            lastSeenTimeMs = System.currentTimeMillis();
        }
    }

    public void onHudRender(PoseStack matrix, float v) {
        try {
            if (mc.player == null || mc.level == null) {
                return;
            }
            if (mc.getDebugOverlay().showDebugScreen() || mc.player.isSpectator()) {
                return;
            }
            if (!ClientConfigs.getConfig().RENDER_SIMPLE_MOB_BAR.get()) {
                return;
            }

            Entity looked = LookUtils.getEntityLookedAt(mc.player);
            if (looked != null && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(looked.getType()).toString().equals("minecraft:mannequin")) {
                currentTarget = null; return; // Crew and decorative mannequins are not combat targets.
            }
            if (looked instanceof LivingEntity le && !(le instanceof ArmorStand) && le != mc.player && le.isAlive()) {
                currentTarget = le;
                lastSeenTimeMs = System.currentTimeMillis();
            } else if (currentTarget != null) {
                if (!currentTarget.isAlive() || currentTarget.isRemoved()
                    || System.currentTimeMillis() - lastSeenTimeMs > 3000L
                    || currentTarget.distanceToSqr(mc.player) > 32.0D * 32.0D) {
                    currentTarget = null;
                }
            }

            if (currentTarget != null) {
                EntityData data = Load.Unit(currentTarget);
                if (data != null) {
                    int currentHp = Math.max(0, (int) Math.ceil(HealthUtils.getCurrentHealth(currentTarget)));
                    int maxHP = Math.max(1, (int) Math.ceil(HealthUtils.getMaxHealth(currentTarget)));
                    float multi = Mth.clamp((float) currentHp / (float) maxHP, 0.0F, 1.0F);

                    int width = mc.getWindow().getGuiScaledWidth();
                    int x = width / 2 - BAR_WIDTH / 2;
                    int y = 18;

                    AbstractGui.bindTexture(BASE_TEX);
                    blit(matrix, x, y, BAR_WIDTH, BAR_HEIGHT, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

                    int fillW = (int) (multi * INNER_WIDTH);
                    if (fillW > 0) {
                        AbstractGui.bindTexture(HEALTH_TEX);
                        blit(matrix, x + 3, y + 3, fillW, INNER_HEIGHT, 0, 0, fillW, INNER_HEIGHT, INNER_WIDTH, INNER_HEIGHT);
                    }

                    int mobLvl = Math.max(1, data.getLevel());
                    int playerLvl = Load.Unit(mc.player) != null ? Load.Unit(mc.player).getLevel() : 1;
                    String lvlStr = (mobLvl - 10 > playerLvl) ? "???" : String.valueOf(mobLvl);

                    ChatFormatting rarFormat = ChatFormatting.WHITE;
                    String rarName = "";
                    try {
                        if (ExileDB.MobRarities().isRegistered(data.getRarity())) {
                            MobRarity rar = ExileDB.MobRarities().get(data.getRarity());
                            if (rar != null) {
                                if (rar.textFormatting() != null) {
                                    rarFormat = rar.textFormatting();
                                }
                                rarName = CLOC.translate(rar.locName());
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    String name = "[Ур. " + lvlStr + "] " + CLOC.translate(data.getName());
                    if (rarName != null && !rarName.isEmpty() && ! "common".equals(data.getRarity())) {
                        name += " (" + rarName + ")";
                    }

                    AbstractGui.drawShadow(
                        mc.font,
                        matrix,
                        name,
                        width / 2 - mc.font.width(name) / 2,
                        y - 10,
                        CompatChatFormatting.getColor(rarFormat)
                    );

                    String hpText = currentHp + "/" + maxHP;
                    GuiUtils.renderScaledText(matrix, width / 2, y + 6, 0.75D, hpText, ChatFormatting.WHITE);

                    if (data.getAffixData() != null && data.getAffixData().getAffixes() != null && !data.getAffixData().getAffixes().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (MobAffix affix : data.getAffixData().getAffixes()) {
                            if (affix != null) {
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(CLOC.translate(affix.locName()));
                            }
                        }
                        if (sb.length() > 0) {
                            GuiUtils.renderScaledText(matrix, width / 2, y + 16, 0.75D, sb.toString(), ChatFormatting.AQUA);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
