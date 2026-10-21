package com.robertx22.age_of_exile.a_libraries.mob_plates;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.config.forge.ClientConfigs;
import com.robertx22.age_of_exile.config.forge.MobPlateStyle;
import com.robertx22.age_of_exile.database.data.rarities.MobRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.utilityclasses.HealthUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LookUtils;
import com.robertx22.forge_compat.common.CompatChatFormatting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws the "plates" (nameplates) hovering above mobs.
 *
 * <p>Three styles are available, selected in the client config or cycled with a
 * keybind:</p>
 *
 * <ul>
 *     <li>{@link MobPlateStyle#CLASSIC} - the original Age of Exile name tag pair.</li>
 *     <li>{@link MobPlateStyle#MODERN} - dark glass panel, level badge, gradient HP bar.</li>
 *     <li>{@link MobPlateStyle#RUNE} - ornate bronze-trimmed panel with a gem badge.</li>
 * </ul>
 *
 * <p>The panel geometry is submitted at order {@code -1} and the glyphs at order
 * {@code 0}, which guarantees that the text always sits on top of its own panel
 * regardless of how the transparent phase sorts the geometry.</p>
 */
public final class MobPlateRenderer {

    /** Full-bright lightmap coordinate, so plates stay readable at night. */
    private static final int LIGHT = 0xF000F0;

    private static final int TEXT_OUTLINE = 0x90000000;

    private MobPlateRenderer() {
    }

    public static void submitWorldMobLabels(LevelRenderContext context) {
        ClientConfigs cfg = ClientConfigs.getConfig();
        if (!cfg.RENDER_MOB_HEALTH_GUI.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack poseStack = context.poseStack();
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (poseStack == null || camera == null || camera.pos == null || camera.orientation == null) {
            return;
        }

        MobPlateStyle style = cfg.MOB_PLATE_STYLE.get();
        boolean showLevel = cfg.MOB_PLATE_SHOW_LEVEL.get();
        boolean showHpText = cfg.MOB_PLATE_SHOW_HP_TEXT.get();
        boolean onlyLookedAt = cfg.MOB_PLATE_ONLY_LOOKED_AT.get();
        boolean hideFullHealth = cfg.MOB_PLATE_HIDE_FULL_HEALTH.get();
        boolean showPlayers = cfg.MOB_PLATE_SHOW_PLAYERS.get();
        double maxDistance = cfg.MOB_PLATE_MAX_DISTANCE.get();
        float userScale = (float) (double) cfg.MOB_PLATE_SCALE.get();

        Entity lookedAt = onlyLookedAt ? LookUtils.getEntityLookedAt(mc.player) : null;
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        EntityData playerData = Load.Unit(mc.player);
        int playerLevel = playerData != null ? playerData.getLevel() : 1;

        Vec3 cam = camera.pos;
        // Camera forward is the camera-local -Z axis rotated into world space
        // (same constant vanilla's Camera uses), so entities behind the camera can
        // be skipped cheaply.
        Vector3f viewDir = new Vector3f(0.0F, 0.0F, -1.0F).rotate(camera.orientation);

        Font font = mc.font;

        for (Entity raw : mc.level.entitiesForRendering()) {
            if (!(raw instanceof LivingEntity entity)) {
                continue;
            }
            if (entity == mc.player || entity instanceof ArmorStand || entity.isInvisible() || !entity.isAlive()) {
                continue;
            }
            if (entity.hasPassenger(mc.player)) {
                continue;
            }
            if (!showPlayers && entity instanceof Player) {
                continue;
            }
            if (onlyLookedAt && lookedAt != entity) {
                continue;
            }
            EntityData data = Load.Unit(entity);
            if (data == null) {
                continue;
            }

            double dx = Mth.lerp(partialTick, entity.xOld, entity.getX()) - cam.x;
            double dy = Mth.lerp(partialTick, entity.yOld, entity.getY()) - cam.y;
            double dz = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cam.z;

            // Camera distance, not player distance: freecam / spectator should behave.
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > maxDistance * maxDistance) {
                continue;
            }

            // Behind the camera: feature submission would project to garbage.
            if (dx * viewDir.x + dy * viewDir.y + dz * viewDir.z <= 0.05D) {
                continue;
            }

            try {
                Info info = Info.of(entity, data, playerLevel, showLevel, showHpText);
                if (hideFullHealth && info.percent >= 0.999F && !info.boss) {
                    continue;
                }

                if (style == MobPlateStyle.CLASSIC) {
                    submitClassic(context, poseStack, camera, entity, info, dx, dy, dz);
                } else {
                    submitPanel(context, poseStack, camera, font, info, style, userScale, Math.sqrt(distanceSq),
                        dx, dy, dz);
                }
            } catch (Exception ignored) {
                // One broken entity must never take down the whole pass.
            }
        }
    }

    // ------------------------------------------------------------------
    // classic style
    // ------------------------------------------------------------------

    private static void submitClassic(LevelRenderContext context, PoseStack poseStack, CameraRenderState camera,
                                      Entity entity, Info info, double dx, double dy, double dz) {
        MutableComponent title = Component.empty();
        if (info.showLevel) {
            title.append(Component.literal("[Ур. " + info.level + "] ")
                .withStyle(info.levelHidden ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        title.append(info.name.copy()
            .withStyle(info.format, ChatFormatting.BOLD));

        MutableComponent bar = Component.literal("[")
            .withStyle(ChatFormatting.DARK_RED);
        int filled = Math.round(info.percent * 10.0F);
        for (int i = 0; i < 10; i++) {
            bar.append(Component.literal("|")
                .withStyle(i < filled ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
            if (i == 4 && info.showHpText) {
                bar.append(Component.literal(" " + info.hpCurrent + "/" + info.hpMax + " ")
                    .withStyle(ChatFormatting.GOLD));
            } else if (i == 4) {
                bar.append(Component.literal(" "));
            }
        }
        bar.append(Component.literal("]")
            .withStyle(ChatFormatting.DARK_RED));

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        context.submitNodeCollector()
            .order(0)
            .submitNameTag(poseStack, new Vec3(0.0D, entity.getBbHeight() + 0.55D, 0.0D), 0, title, false, LIGHT, camera);
        context.submitNodeCollector()
            .order(0)
            .submitNameTag(poseStack, new Vec3(0.0D, entity.getBbHeight() + 0.28D, 0.0D), 0, bar, false, LIGHT, camera);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // panel styles
    // ------------------------------------------------------------------

    private static void submitPanel(LevelRenderContext context, PoseStack poseStack, CameraRenderState camera, Font font,
                                    Info info, MobPlateStyle style, float userScale, double distance,
                                    double dx, double dy, double dz) {
        Layout layout = Layout.of(font, info, style);
        // Far-away plates grow a little so the name stays legible without turning
        // into a wall of text up close.
        float proximity = Mth.clamp((float) Math.sqrt(Math.max(distance, 1.0D) / 16.0D), 1.0F, 1.9F);
        float scale = 0.025F * Mth.clamp(userScale, 0.5F, 2.0F) * proximity;

        // The plate grows downwards from its anchor, so lift the anchor by the whole
        // plate + pointer height and keep a small gap above the mob's head.
        double anchorY = dy + info.bbHeight + 0.04D + (layout.height + layout.tailHeight) * scale;
        double anchorX = dx;
        double anchorZ = dz;

        poseStack.pushPose();
        poseStack.translate(anchorX, anchorY, anchorZ);
        poseStack.rotate(camera.orientation);
        poseStack.scale(scale, -scale, scale);

        // Geometry sits BEHIND the glyph plane (camera-local +Z points at the viewer,
        // so "behind" is negative); every layer is pulled another hair forward so the
        // depth-sorted translucent pass keeps painting them in order.
        poseStack.pushPose();
        context.submitNodeCollector()
            .order(-1)
            .submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
                if (style == MobPlateStyle.RUNE) {
                    drawRune(pose, buffer, font, info, layout);
                } else {
                    drawModern(pose, buffer, font, info, layout);
                }
            });
        poseStack.popPose();

        if (style == MobPlateStyle.RUNE) {
            drawRuneText(context, poseStack, font, info, layout);
        } else {
            drawModernText(context, poseStack, font, info, layout);
        }

        poseStack.popPose();
    }

    // ---- modern -------------------------------------------------------

    private static void drawModern(PoseStack.Pose pose, VertexConsumer buffer, Font font, Info info, Layout l) {
        int rarity = info.rarityColor;
        int rarityBorder = PlateDraw.alpha(rarity, 0xF2);
        int rarityBorderDark = PlateDraw.alpha(PlateDraw.shade(rarity, 0.55F), 0xE0);
        float left = -l.width / 2.0F;
        float right = l.width / 2.0F;

        // soft halo, drop shadow, border and body
        PlateDraw.layer(-1.40F);
        PlateDraw.rounded(pose, buffer, left - 2.5F, -2.5F, right + 2.5F, l.height + 2.5F, 6,
            PlateDraw.alpha(rarity, 0x30), PlateDraw.alpha(rarity, 0x06));
        PlateDraw.layer(-1.30F);
        PlateDraw.rounded(pose, buffer, left + 1.0F, 1.6F, right + 1.0F, l.height + 1.6F, 5,
            0x50000000, 0x20000000);
        PlateDraw.layer(-1.20F);
        PlateDraw.rounded(pose, buffer, left, 0.0F, right, l.height, 5, rarityBorder, rarityBorderDark);
        PlateDraw.layer(-1.10F);
        PlateDraw.rounded(pose, buffer, left + 1.0F, 1.0F, right - 1.0F, l.height - 1.0F, 4,
            0xF0161B24, 0xF0080A0F);

        // thin rarity accent across the top edge
        PlateDraw.layer(-1.05F);
        PlateDraw.rect(pose, buffer, left + 3.0F, 1.6F, right - 3.0F, 2.6F,
            PlateDraw.alpha(rarity, 0xA0), PlateDraw.alpha(rarity, 0x30));

        drawPointer(pose, buffer, l, rarity, -0.70F);

        // level badge
        if (info.showLevel) {
            PlateDraw.layer(-1.00F);
            PlateDraw.rounded(pose, buffer, l.badgeX, l.badgeY, l.badgeX + l.badgeW, l.badgeY + l.badgeH, 3,
                PlateDraw.alpha(rarity, 0x80), PlateDraw.alpha(rarity, 0x38));
            PlateDraw.layer(-0.95F);
            PlateDraw.rounded(pose, buffer, l.badgeX + 1.0F, l.badgeY + 1.0F, l.badgeX + l.badgeW - 1.0F,
                l.badgeY + l.badgeH - 1.0F, 2, 0xE0141820, 0xE00B0E14);
        }

        drawHealthBar(pose, buffer, info, l, rarity, false);
    }

    private static void drawModernText(LevelRenderContext context, PoseStack poseStack, Font font, Info info, Layout l) {
        text(context, poseStack, info.name, l.nameX, l.nameY, 1.0F, 0xFFFFFFFF, TEXT_OUTLINE, true);

        if (info.showLevel) {
            float lvlScale = 0.78F;
            float lvlW = font.width(info.level) * lvlScale;
            text(context, poseStack, levelComponent(info), l.badgeX + (l.badgeW - lvlW) / 2.0F,
                l.badgeY + (l.badgeH - 9.0F * lvlScale) / 2.0F, lvlScale, 0xFFFFFFFF, 0x80000000, true);
        }

        if (info.showHpText) {
            text(context, poseStack, hpComponent(info), l.hpX, l.hpY, l.hpScale, 0xFFC9D3E0, TEXT_OUTLINE, true);
        }
    }

    // ---- rune ---------------------------------------------------------

    private static void drawRune(PoseStack.Pose pose, VertexConsumer buffer, Font font, Info info, Layout l) {
        int rarity = info.rarityColor;
        float left = -l.width / 2.0F;
        float right = l.width / 2.0F;

        int gold = 0xFFD9AC52;
        int goldDark = 0xFF6E5120;

        // halo + shadow
        PlateDraw.layer(-1.40F);
        PlateDraw.rounded(pose, buffer, left - 3.0F, -3.0F, right + 3.0F, l.height + 3.0F, 7,
            PlateDraw.alpha(gold, 0x26), PlateDraw.alpha(gold, 0x05));
        PlateDraw.layer(-1.30F);
        PlateDraw.rounded(pose, buffer, left + 1.0F, 1.8F, right + 1.0F, l.height + 1.8F, 6,
            0x50000000, 0x20000000);

        // bronze frame, rarity inlay, parchment-dark body
        PlateDraw.layer(-1.20F);
        PlateDraw.rounded(pose, buffer, left, 0.0F, right, l.height, 5, gold, goldDark);
        PlateDraw.layer(-1.15F);
        PlateDraw.rounded(pose, buffer, left + 1.5F, 1.5F, right - 1.5F, l.height - 1.5F, 4,
            PlateDraw.alpha(rarity, 0xD0), PlateDraw.alpha(PlateDraw.shade(rarity, 0.5F), 0xA0));
        PlateDraw.layer(-1.10F);
        PlateDraw.rounded(pose, buffer, left + 2.5F, 2.5F, right - 2.5F, l.height - 2.5F, 3,
            0xF02A1D10, 0xF0120A04);

        // corner studs
        PlateDraw.layer(-1.05F);
        float stud = 2.2F;
        PlateDraw.rect(pose, buffer, left + 1.6F, 1.6F, left + 1.6F + stud, 1.6F + stud, 0xFFFFE2A0);
        PlateDraw.rect(pose, buffer, right - 1.6F - stud, 1.6F, right - 1.6F, 1.6F + stud, 0xFFFFE2A0);
        PlateDraw.rect(pose, buffer, left + 1.6F, l.height - 1.6F - stud, left + 1.6F + stud, l.height - 1.6F, 0xFFD0A64A);
        PlateDraw.rect(pose, buffer, right - 1.6F - stud, l.height - 1.6F - stud, right - 1.6F, l.height - 1.6F, 0xFFD0A64A);

        // thin gold divider between the name row and the bar
        PlateDraw.layer(-1.02F);
        PlateDraw.rect(pose, buffer, left + 3.0F, l.barY - 1.7F, right - 3.0F, l.barY - 1.1F,
            0xD0FFE2A0, 0x406E5120);

        drawPointer(pose, buffer, l, PlateDraw.alpha(gold, 0xE8), -0.70F);

        // gem badge for the level
        if (info.showLevel) {
            PlateDraw.layer(-1.00F);
            PlateDraw.diamond(pose, buffer, l.badgeX + l.badgeW / 2.0F, l.badgeY + l.badgeH / 2.0F,
                l.badgeH + 1.5F, PlateDraw.alpha(rarity, 0xF0), PlateDraw.alpha(PlateDraw.shade(rarity, 0.45F), 0xE0));
            PlateDraw.layer(-0.95F);
            PlateDraw.diamond(pose, buffer, l.badgeX + l.badgeW / 2.0F, l.badgeY + l.badgeH / 2.0F,
                l.badgeH - 2.5F, 0xFF1A1410, 0xFF0B0805);
        }

        drawHealthBar(pose, buffer, info, l, rarity, true);
    }

    private static void drawRuneText(LevelRenderContext context, PoseStack poseStack, Font font, Info info, Layout l) {
        text(context, poseStack, info.name, l.nameX, l.nameY, 1.0F, 0xFFFFF3DA, 0xA0301800, true);

        if (info.showLevel) {
            float lvlScale = 0.70F;
            float lvlW = font.width(info.level) * lvlScale;
            text(context, poseStack, levelComponent(info), l.badgeX + (l.badgeW - lvlW) / 2.0F,
                l.badgeY + (l.badgeH - 9.0F * lvlScale) / 2.0F + 0.6F, lvlScale, 0xFFFFF6DC, 0xA0301800, true);
        }

        if (info.showHpText) {
            text(context, poseStack, hpComponent(info), l.hpX, l.hpY, l.hpScale, 0xFFF3E3C4, 0xA0301800, true);
        }
    }

    // ------------------------------------------------------------------
    // shared pieces
    // ------------------------------------------------------------------

    private static void drawPointer(PoseStack.Pose pose, VertexConsumer buffer, Layout l, int color, float z) {
        PlateDraw.layer(z);
        PlateDraw.tail(pose, buffer, 0.0F, l.height - 0.4F, l.tailHeight, 7.0F,
            PlateDraw.alpha(color, 0xC0), PlateDraw.alpha(color, 0x08));
    }

    private static void drawHealthBar(PoseStack.Pose pose, VertexConsumer buffer, Info info, Layout l, int rarity,
                                      boolean segmented) {
        float x0 = l.barX0;
        float x1 = l.barX1;
        float y0 = l.barY;
        float y1 = l.barY + l.barH;
        float inner = y1 - y0;

        PlateDraw.layer(-0.92F);
        PlateDraw.rounded(pose, buffer, x0, y0, x1, y1, 2, 0xFF20262F, 0xFF0B0E13);

        float width = (x1 - x0 - 1.0F) * info.percent;
        if (width > 0.4F) {
            int base = PlateDraw.hpColor(info.percent);
            int top = PlateDraw.brighten(base, 0.28F);
            int bottom = PlateDraw.shade(base, 0.72F);
            PlateDraw.layer(-0.88F);
            PlateDraw.rounded(pose, buffer, x0 + 0.5F, y0 + 0.5F, x0 + 0.5F + width, y1 - 0.5F, 1, top, bottom);
            if (width > 6.0F) {
                PlateDraw.layer(-0.85F);
                PlateDraw.rect(pose, buffer, x0 + 2.0F, y0 + 1.0F, x0 + width - 1.0F, y0 + 1.9F,
                    0x38FFFFFF, 0x08FFFFFF);
            }
        }

        int ticks = segmented ? 5 : 4;
        PlateDraw.layer(-0.82F);
        for (int i = 1; i < ticks; i++) {
            float x = x0 + (x1 - x0) * i / (float) ticks;
            PlateDraw.rect(pose, buffer, x - 0.4F, y0 + 0.7F, x + 0.4F, y1 - 0.7F, 0x66000000, 0x66000000);
        }

        if (segmented) {
            // ornamental caps
            PlateDraw.layer(-0.78F);
            PlateDraw.diamond(pose, buffer, x0 + 0.6F, (y0 + y1) / 2.0F, 4.6F,
                PlateDraw.alpha(rarity, 0xF0), PlateDraw.alpha(PlateDraw.shade(rarity, 0.5F), 0xE0));
            PlateDraw.diamond(pose, buffer, x1 - 0.6F, (y0 + y1) / 2.0F, 4.6F,
                PlateDraw.alpha(rarity, 0xF0), PlateDraw.alpha(PlateDraw.shade(rarity, 0.5F), 0xE0));
        }

        // thin inner outline keeps the bar readable on bright backgrounds
        if (inner > 2.0F) {
            PlateDraw.layer(-0.80F);
            PlateDraw.rect(pose, buffer, x0 + 1.0F, y0, x1 - 1.0F, y0 + 0.6F, 0x22000000, 0x00000000);
        }
    }

    private static Component levelComponent(Info info) {
        return Component.literal(info.level)
            .withStyle(ChatFormatting.BOLD);
    }

    private static Component hpComponent(Info info) {
        return Component.literal(info.hpText);
    }

    private static void text(LevelRenderContext context, PoseStack poseStack, Component component, float x, float y,
                             float scale, int color, int outline, boolean shadow) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, 1.0F);
        }
        context.submitNodeCollector()
            .order(0)
            .submitText(poseStack, 0.0F, 0.0F, component.getVisualOrderText(), shadow,
                Font.DisplayMode.NORMAL, LIGHT, color, 0, outline);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // data
    // ------------------------------------------------------------------

    private static final class Info {
        MutableComponent name;
        String level = "";
        String hpText = "";
        int hpCurrent;
        int hpMax;
        float percent;
        float bbHeight;
        int rarityColor = 0xFFFF5555;
        ChatFormatting format = ChatFormatting.RED;
        boolean boss;
        boolean levelHidden;
        boolean showLevel;
        boolean showHpText;

        static Info of(LivingEntity entity, EntityData data, int playerLevel, boolean showLevel, boolean showHpText) {
            Info info = new Info();
            info.showLevel = showLevel;
            info.showHpText = showHpText;
            info.bbHeight = entity.getBbHeight();

            ChatFormatting format = ChatFormatting.RED;
            String icon = "";
            try {
                if (ExileDB.MobRarities()
                    .isRegistered(data.getRarity())) {
                    MobRarity rarity = ExileDB.MobRarities()
                        .get(data.getRarity());
                    if (rarity != null) {
                        if (rarity.textFormatting() != null) {
                            format = rarity.textFormatting();
                        }
                        if (rarity.name_add != null && !rarity.name_add.isEmpty()) {
                            icon = rarity.name_add + " ";
                        }
                        info.boss = IRarity.BOSS_ID.equals(data.getRarity());
                    }
                }
            } catch (Exception ignored) {
            }

            info.format = format;
            info.rarityColor = CompatChatFormatting.getColor(format);
            // A single flat literal keeps the panel text one uniform colour instead of
            // inheriting the per-rarity colours baked into the source component.
            info.name = Component.literal(icon + data.getName()
                    .getString())
                .withStyle(ChatFormatting.BOLD);

            int mobLevel = Math.max(1, data.getLevel());
            info.levelHidden = mobLevel - 10 > playerLevel;
            info.level = info.levelHidden ? "??" : String.valueOf(mobLevel);

            float current = Math.max(0, HealthUtils.getCurrentHealth(entity));
            float max = Math.max(1, HealthUtils.getMaxHealth(entity));
            info.percent = Mth.clamp(current / max, 0.0F, 1.0F);
            info.hpCurrent = (int) Math.ceil(current);
            info.hpMax = (int) Math.ceil(max);
            info.hpText = info.hpCurrent + "/" + info.hpMax;

            return info;
        }
    }

    /**
     * Pixel layout shared by both sides of a panel: the vertex geometry and the
     * text glyphs are positioned from the same numbers so they can never drift
     * apart.
     */
    private static final class Layout {
        float width;
        float height;
        float tailHeight;

        float badgeX;
        float badgeY;
        float badgeW;
        float badgeH;

        float nameX;
        float nameY;

        float hpX;
        float hpY;
        float hpScale;

        float barX0;
        float barX1;
        float barY;
        float barH;

        static Layout of(Font font, Info info, MobPlateStyle style) {
            Layout l = new Layout();
            if (style == MobPlateStyle.RUNE) {
                float padX = 5.0F;
                float padTop = 3.0F;
                float nameW = font.width(info.name);
                float lvlScale = 0.70F;
                float hpScale = 0.68F;

                l.badgeW = info.showLevel ? Math.max(11.0F, font.width(info.level) * lvlScale + 5.0F) : 0.0F;
                l.badgeH = 11.0F;
                float hpW = info.showHpText ? font.width(info.hpText) * hpScale : 0.0F;

                l.width = Math.max(70.0F, padX * 2.0F + (info.showLevel ? l.badgeW + 4.0F : 0.0F) + nameW
                    + (info.showHpText ? 8.0F + hpW : 0.0F));
                l.barH = 6.0F;
                l.height = padTop + l.badgeH + 2.0F + l.barH + padTop;
                l.tailHeight = 3.0F;

                l.badgeX = -l.width / 2.0F + padX;
                l.badgeY = padTop;
                l.nameX = l.badgeX + (info.showLevel ? l.badgeW + 4.0F : 0.0F);
                l.nameY = padTop + 1.0F;

                l.barY = padTop + l.badgeH + 2.0F;
                l.barX0 = -l.width / 2.0F + padX;
                l.barX1 = l.width / 2.0F - padX;

                l.hpScale = hpScale;
                l.hpX = l.width / 2.0F - padX - hpW;
                l.hpY = padTop + 2.2F;
            } else {
                float padX = 2.5F;
                float padTop = 2.0F;

                l.badgeH = 11.0F;
                l.badgeW = info.showLevel ? Math.max(11.0F, font.width(info.level) + 6.0F) : 0.0F;
                float nameW = font.width(info.name);
                float hpScale = 0.72F;
                float hpW = info.showHpText ? font.width(info.hpText) * hpScale : 0.0F;

                l.width = Math.max(56.0F, padX * 2.0F + (info.showLevel ? l.badgeW + 4.0F : 0.0F) + nameW
                    + (info.showHpText ? 6.0F + hpW : 0.0F));
                l.barH = 4.0F;
                l.height = padTop + l.badgeH + 2.0F + l.barH + padTop;
                l.tailHeight = 2.6F;

                l.badgeX = -l.width / 2.0F + padX;
                l.badgeY = padTop;
                l.nameX = l.badgeX + (info.showLevel ? l.badgeW + 4.0F : 0.0F);
                l.nameY = padTop + 1.0F;

                l.barY = padTop + l.badgeH + 2.0F;
                l.barX0 = -l.width / 2.0F + padX;
                l.barX1 = l.width / 2.0F - padX;

                l.hpScale = hpScale;
                l.hpX = l.width / 2.0F - padX - hpW;
                l.hpY = padTop + 2.2F;
            }
            return l;
        }
    }
}
