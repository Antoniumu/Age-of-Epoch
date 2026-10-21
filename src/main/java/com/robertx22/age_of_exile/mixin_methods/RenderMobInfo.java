package com.robertx22.age_of_exile.mixin_methods;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.config.forge.ClientConfigs;
import com.robertx22.age_of_exile.database.data.rarities.MobRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.HealthUtils;
import com.robertx22.forge_compat.client.MultiBufferSource;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

public class RenderMobInfo {

    static Entity lastLooked;

    public static void submitWorldMobLabels(LevelRenderContext context) {
        try {
            if (!ClientConfigs.getConfig().RENDER_MOB_HEALTH_GUI.get()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                return;
            }
            PoseStack poseStack = context.poseStack();
            CameraRenderState cameraState = context.levelState().cameraRenderState;
            if (poseStack == null || cameraState == null || cameraState.pos == null) {
                return;
            }
            Vec3 camPos = cameraState.pos;
            EntityData playerData = Load.Unit(mc.player);
            int playerLvl = playerData != null ? playerData.getLevel() : 1;

            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity entity)) {
                    continue;
                }
                if (entity == mc.player || entity instanceof ArmorStand || entity.isInvisible() || !entity.isAlive()) {
                    continue;
                }
                if (entity.hasPassenger(mc.player)) {
                    continue;
                }
                double distSq = entity.distanceToSqr(mc.player);
                if (distSq > 24.0D * 24.0D) {
                    continue;
                }

                EntityData data = Load.Unit(entity);
                if (data == null) {
                    continue;
                }

                int mobLvl = Math.max(1, data.getLevel());
                boolean hideLvl = mobLvl - 10 > playerLvl;
                String lvlStr = hideLvl ? "???" : String.valueOf(mobLvl);

                ChatFormatting rarFormat = ChatFormatting.RED;
                String rarIcon = "";
                try {
                    if (ExileDB.MobRarities().isRegistered(data.getRarity())) {
                        MobRarity rar = ExileDB.MobRarities().get(data.getRarity());
                        if (rar != null) {
                            if (rar.textFormatting() != null) {
                                rarFormat = rar.textFormatting();
                            }
                            if (rar.name_add != null && !rar.name_add.isEmpty()) {
                                rarIcon = rar.name_add + " ";
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                MutableComponent lvlComp = Component.literal("[Ур. " + lvlStr + "] ")
                    .withStyle(hideLvl ? ChatFormatting.RED : ChatFormatting.YELLOW);
                MutableComponent nameComp = Component.literal(rarIcon)
                    .append(data.getName().copy())
                    .withStyle(rarFormat, ChatFormatting.BOLD);
                MutableComponent titleComp = Component.empty().append(lvlComp).append(nameComp);

                float curHp = Math.max(0, HealthUtils.getCurrentHealth(entity));
                float maxHp = Math.max(1, HealthUtils.getMaxHealth(entity));
                float percent = (curHp / maxHp) * 100.0F;

                MutableComponent hpText = Component.literal("[").withStyle(ChatFormatting.DARK_RED);
                for (int i = 0; i < 10; i++) {
                    if (percent > i * 10) {
                        hpText.append(Component.literal("|").withStyle(ChatFormatting.RED));
                    } else {
                        hpText.append(Component.literal("|").withStyle(ChatFormatting.DARK_GRAY));
                    }
                    if (i == 4) {
                        hpText.append(Component.literal(" " + (int) Math.ceil(curHp) + "/" + (int) Math.ceil(maxHp) + " ")
                            .withStyle(ChatFormatting.GOLD));
                    }
                }
                hpText.append(Component.literal("]").withStyle(ChatFormatting.DARK_RED));

                double dx = entity.getX() - camPos.x;
                double dy = entity.getY() - camPos.y;
                double dz = entity.getZ() - camPos.z;

                poseStack.pushPose();
                poseStack.translate(dx, dy, dz);

                boolean seeThrough = false;
                context.submitNodeCollector()
                    .order(0)
                    .submitNameTag(poseStack, new Vec3(0, entity.getBbHeight() + 0.55D, 0), 0, titleComp, seeThrough, 0xF000F0, cameraState);
                context.submitNodeCollector()
                    .order(0)
                    .submitNameTag(poseStack, new Vec3(0, entity.getBbHeight() + 0.28D, 0), 0, hpText, seeThrough, 0xF000F0, cameraState);

                poseStack.popPose();
            }
        } catch (Exception ignored) {
        }
    }

    public static void renderLivingEntityLabelIfPresent(Font textRenderer, EntityRenderDispatcher dispatcher, LivingEntity entity,
                                                        PoseStack matrixStack,
                                                        MultiBufferSource vertex, int i) {
        // Handled via submitWorldMobLabels in LevelRenderEvents.COLLECT_SUBMITS
    }
}
