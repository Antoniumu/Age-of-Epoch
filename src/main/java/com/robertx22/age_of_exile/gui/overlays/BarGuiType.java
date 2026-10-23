package com.robertx22.age_of_exile.gui.overlays;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.data.stats.types.resources.blood.BloodUser;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.HealthUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;

import net.minecraft.world.level.Level;
public enum BarGuiType {
    NONE {
        @Override
        public float getCurrent(EntityData data, Player en) {
            return 0;
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return 0;
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("empty");
        }
    },
    EXP {
        @Override
        public float getCurrent(EntityData data, Player en) {
            return data.getExp();
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return data.getExpRequiredForLevelUp();
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("textures/gui/overlay/exp.png");
        }

        @Override
        public String getText(EntityData data, Player en) {
            if (data.getLevel() >= com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig.get().MAX_LEVEL) {
                return "Ур. " + data.getLevel() + " (МАКС.)";
            }
            return "Ур. " + data.getLevel() + " (" + (int) (getMulti(data, en) * 100) + "%)";
        }
    },
    ENERGY {
        @Override
        public float getCurrent(EntityData data, Player en) {

            return data.getResources()
                .getEnergy();
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return data.getUnit()
                .energyData()
                .getValue();
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("textures/gui/overlay/energy.png");
        }

        @Override
        public String getText(EntityData data, Player en) {
            return "Энергия: " + (int) getCurrent(data, en) + "/" + (int) getMax(data, en);
        }
    },

    MANA {
        @Override
        public float getCurrent(EntityData data, Player en) {
            if (data.getUnit()
                .isBloodMage()) {
                return data.getResources()
                    .getBlood();
            }
            return data.getResources()
                .getMana();
        }

        @Override
        public float getMax(EntityData data, Player en) {
            if (data.getUnit()
                .isBloodMage()) {
                return data.getUnit()
                    .bloodData()
                    .getValue();
            }
            return data.getUnit()
                .manaData()
                .getValue();
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            if (data.getUnit()
                .getCalculatedStat(BloodUser.getInstance())
                .getValue() > 0) {
                return SlashRef.id("textures/gui/overlay/blood.png");
            } else {
                return SlashRef.id("textures/gui/overlay/mana.png");
            }
        }

        @Override
        public String getText(EntityData data, Player en) {
            String label = data.getUnit().isBloodMage() ? "Кровь: " : "Мана: ";
            return label + (int) getCurrent(data, en) + "/" + (int) getMax(data, en);
        }
    },
    HEALTH {
        @Override
        public float getCurrent(EntityData data, Player en) {
            return HealthUtils.getCurrentHealth(en);
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return HealthUtils.getMaxHealth(en);
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("textures/gui/overlay/health.png");
        }

        @Override
        public String getText(EntityData data, Player en) {
            return "Здоровье: " + (int) getCurrent(data, en) + "/" + (int) getMax(data, en);
        }

    },
    SCALING_DIFFICULTY {
        @Override
        public float getCurrent(EntityData data, Player en) {
            return Load.playerRPGData(en).scalingDifficulty.getDifficulty();
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return ServerContainer.get().MAX_DIFFICULTY.get()
                .floatValue();
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("textures/gui/overlay/difficulty.png");
        }
    },
    SHIELD {
        @Override
        public float getCurrent(EntityData data, Player en) {
            return data.getResources()
                .getShield();
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return HealthUtils.getMaxHealth(en);
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("textures/gui/overlay/shield.png");
        }

        @Override
        public boolean shouldRender(EntityData data, Player en) {
            return getCurrent(data, en) > 0;
        }

    },

    HUNGER {
        @Override
        public float getCurrent(EntityData data, Player en) {
            return en.getFoodData()
                .getFoodLevel();
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return 20; // ?
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("textures/gui/overlay/hunger.png");
        }

        @Override
        public String getText(EntityData data, Player en) {
            return "H: " + (int) getCurrent(data, en) + " S: " + (int) en.getFoodData()
                .getSaturationLevel();
        }

    },
    AIR {
        @Override
        public float getCurrent(EntityData data, Player en) {
            return en.getAirSupply();
        }

        @Override
        public float getMax(EntityData data, Player en) {
            return en.getMaxAirSupply();
        }

        @Override
        public Identifier getTexture(EntityData data, Player en) {
            return SlashRef.id("textures/gui/overlay/air.png");
        }

        @Override
        public boolean shouldRender(EntityData data, Player en) {
            return getCurrent(data, en) < getMax(data, en);
        }
    };

    public float getMulti(EntityData data, Player en) {
        return Math.min(getCurrent(data, en) / getMax(data, en), 1);
    }

    public String getText(EntityData data, Player en) {
        return (int) getCurrent(data, en) + "/" + (int) getMax(data, en);
    }

    public Identifier getIcon(EntityData data, Player en) {
        return Identifier.parse(getTexture(data, en).toString()
            .replaceAll(".png", "_icon.png"));
    }

    public boolean shouldRender(EntityData data, Player en) {
        return true;
    }

    public abstract float getCurrent(EntityData data, Player en);

    public abstract float getMax(EntityData data, Player en);

    public abstract Identifier getTexture(EntityData data, Player en);

}
