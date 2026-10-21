package com.robertx22.world_of_exile.delve;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlockEntities;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.world_of_exile.tower.TowerAbility;
import com.robertx22.world_of_exile.tower.TowerThemes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One-shot finite room encounter. All progression belongs to the saved block entity,
 * not a static map or a player session. Several players share the same expedition.
 */
public class DelveMarkerBlockEntity extends BlockEntity {
    private static final String HOME = "aoe_delve_room:";
    private static final String BOSS = "aoe_delve_warden";
    private CompoundTag data = new CompoundTag();
    private final List<UUID> active = new ArrayList<>();
    private final Map<UUID, Integer> missing = new HashMap<>();
    private int warmup = 0;
    private long pulseDue = -1;

    public DelveMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.DELVE_MARKER.get(), pos, state);
    }

    private boolean configured() {
        return data.contains("theme") && data.contains("center_x") && data.contains("exit_x") && data.contains("reward_x");
    }
    private int depth() { return Math.max(1, Math.min(4, data.getIntOr("depth", 1))); }
    private String kind() { return data.getStringOr("kind", "combat"); }
    private boolean guardian() { return kind().equals("warden") || kind().equals("final"); }
    private BlockPos point(String prefix) {
        return new BlockPos(data.getIntOr(prefix + "_x", worldPosition.getX()),
            data.getIntOr(prefix + "_y", worldPosition.getY()), data.getIntOr(prefix + "_z", worldPosition.getZ()));
    }
    private AABB room() {
        BlockPos p = point("center");
        return new AABB(p.getX()-4, p.getY(), p.getZ()-4, p.getX()+5, p.getY()+6, p.getZ()+5);
    }
    private List<ServerPlayer> players(ServerLevel world) {
        return world.getEntitiesOfClass(ServerPlayer.class, room(), p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }
    private void tell(ServerLevel world, String key, Object... args) {
        for (ServerPlayer p : players(world)) p.sendSystemMessage(Component.translatable("mmorpg.delve." + key, args));
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel world) || world.getGameTime() % 20 != 0) return;
        if (data.getBooleanOr("cleared", false)) return;
        if (!configured()) return; // an unconfigured /setblock altar is not an encounter
        BlockPos c = point("center");
        // Do not interpret missing entities as deaths while any part of the room is unloaded.
        for (int x=(c.getX()-6)>>4; x<=(c.getX()+6)>>4; x++) {
            for (int z=(c.getZ()-6)>>4; z<=(c.getZ()+6)>>4; z++) {
                if (!world.getChunkSource().hasChunk(x,z)) return;
            }
        }
        if (++warmup < 5) return; // allow entities to deserialize after chunk loading
        List<ServerPlayer> nearby = players(world);
        if (nearby.isEmpty() || world.getDifficulty() == Difficulty.PEACEFUL) return;
        if (kind().equals("rest")) {
            data.putBoolean("cleared", true);
            unlockReward(world);
            tell(world, "floor", depth());
            setChanged();
            return;
        }
        if (!data.getBooleanOr("started", false)) {
            data.putBoolean("started", true);
            data.putInt("party", Math.min(3, nearby.size()));
            int base = 1;
            for (ServerPlayer player : nearby) {
                var unit = Load.Unit(player);
                if (unit != null) base = Math.max(base, unit.getLevel());
            }
            data.putInt("base_level", base);
            tell(world, "encounter", depth());
            setChanged();
        }
        reconcile(world);
        if (!active.isEmpty()) {
            fight(world, nearby.get(0));
            return;
        }
        int wave = data.getIntOr("wave", 0);
        int waves = guardian() ? (kind().equals("final") ? 3 : 2) : (depth() >= 3 ? 2 : 1);
        if (wave >= waves) {
            data.putBoolean("cleared", true);
            unlockReward(world);
            if (data.getBooleanOr("has_gate", false)) {
                BlockPos gate = point("gate");
                Direction facing = Direction.WEST;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (direction.getName().equals(data.getStringOr("ladder_facing", "west"))) facing = direction;
                }
                if (world.getBlockState(gate).is(SlashBlocks.DELVE_SEAL.get())) {
                    world.setBlock(gate, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing), 3);
                }
            }
            tell(world, kind().equals("final") ? "complete" : guardian() ? "descent" : "room_clear");
            setChanged();
            return;
        }
        // A partial wave retries unspawned members, never silently skips the warden.
        int count = guardian() && wave == waves-1 ? 1 : 2 + depth() + data.getIntOr("party", 1)-1;
        int spawned = data.getIntOr("spawned", 0);
        for (int i=spawned; i<count; i++) {
            Mob mob = spawn(world, nearby.get(0), guardian() && wave == waves-1, i);
            if (mob == null) break; // wait for a genuinely free, supported spawn point
            active.add(mob.getUUID());
            spawned++;
            data.putInt("spawned", spawned);
            setChanged();
        }
        if (spawned == count) {
            data.putInt("wave", wave+1);
            data.putInt("spawned", 0);
            tell(world, "wave", wave+1, waves);
            setChanged();
        }
    }

    private void reconcile(ServerLevel world) {
        boolean changed = active.removeIf(id -> {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                missing.remove(id);
                return !entity.isAlive();
            }
            // Persistent mobs never despawn naturally. /kill, peaceful removal or
            // external mods must not permanently lock a hatch. Wait 30 loaded seconds.
            return missing.merge(id, 1, Integer::sum) >= 30;
        });
        missing.keySet().retainAll(active);
        if (changed) setChanged();
    }

    private String mobId(boolean boss, int index) {
        String theme = data.getStringOr("theme", "crypt");
        if (boss) return switch (theme) {
            case "frost" -> "minecraft:stray";
            case "sands" -> "minecraft:husk";
            case "roots" -> "minecraft:witch";
            default -> depth() == 4 ? "minecraft:vindicator" : "minecraft:skeleton";
        };
        String[] pool = switch (theme) {
            case "frost" -> new String[]{"stray", "zombie", "skeleton"};
            case "sands" -> new String[]{"husk", "spider", "skeleton"};
            case "roots" -> new String[]{"spider", "zombie", "witch"};
            default -> new String[]{"zombie", "skeleton", "spider"};
        };
        return "minecraft:" + pool[(index + depth()) % pool.length];
    }

    private Mob spawn(ServerLevel world, ServerPlayer target, boolean boss, int index) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mobId(boss,index)));
        if (type == null || !(type.create(world, EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob)) return null;
        BlockPos center = point("center");
        mob.setPos(center.getX()+0.5, center.getY(), center.getZ()+0.5);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(center), EntitySpawnReason.MOB_SUMMONED, null);
        BlockPos stand = null;
        // Keep all spawns inside this room and away from its controller/reward/shaft.
        for (int dx=-2; dx<=2 && stand==null; dx++) {
            for (int dz=-2; dz<=2; dz++) {
                BlockPos candidate = center.offset(dx,0,dz);
                AABB box = mob.getBoundingBox().move(candidate.getX()+0.5-mob.getX(), candidate.getY()-mob.getY(), candidate.getZ()+0.5-mob.getZ());
                if (!world.getBlockState(candidate.below()).isFaceSturdy(world,candidate.below(),Direction.UP)
                    || !world.getFluidState(candidate).isEmpty() || !world.noCollision(box)
                    || !world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,box).isEmpty()) continue;
                stand=candidate; break;
            }
        }
        if (stand == null) return null;
        mob.setPos(stand.getX()+0.5,stand.getY(),stand.getZ()+0.5);
        mob.addTag(HOME+worldPosition.getX()+","+worldPosition.getY()+","+worldPosition.getZ());
        if (boss) {
            mob.addTag(BOSS);
            mob.setCustomName(Component.translatable("mmorpg.delve."+data.getStringOr("theme","crypt")+".boss."+depth()));
            mob.setCustomNameVisible(true);
        }
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        mob.setDropChance(EquipmentSlot.HEAD,0.0F);
        if (!world.addFreshEntity(mob)) return null;
        var unit = Load.Unit(mob);
        if (unit != null) {
            unit.setLevel(data.getIntOr("base_level",1) + (depth()-1)*4 + (boss ? 2 : 0));
            if (boss) unit.setRarity(IRarity.BOSS_ID);
            unit.forceRecalculateStats();
        }
        var health=mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(health.getBaseValue() * (1.0 + depth()*0.15) * (boss ? 2.5 : 1.0));
        var attack=mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(attack.getBaseValue() * (1.0 + depth()*0.1));
        mob.setHealth(mob.getMaxHealth());
        mob.setTarget(target);
        mob.setAggressive(true);
        return mob;
    }

    private void fight(ServerLevel world, ServerPlayer target) {
        for (UUID id : active) {
            if (!(world.getEntity(id) instanceof Mob mob) || !mob.isAlive()) continue;
            if (mob.getItemBySlot(EquipmentSlot.HEAD).isDamageableItem()) mob.getItemBySlot(EquipmentSlot.HEAD).setDamageValue(0);
            if (!(mob.getTarget() instanceof Player current) || !current.isAlive()
                || current.isCreative() || current.isSpectator() || !room().intersects(current.getBoundingBox())) {
                mob.setTarget(target);
                mob.setAggressive(true);
            }
            if (!mob.entityTags().contains(BOSS)) continue;
            // Tell the party before the pulse; the effect still needs line of sight.
            if (world.getGameTime()%160==0) {
                tell(world,"warning");
                pulseDue=world.getGameTime()+20;
            }
            if (pulseDue>=0 && world.getGameTime()>=pulseDue) {
                pulseDue=-1;
                if (mob.hasLineOfSight(target) && mob.distanceToSqr(target)<64) {
                    TowerAbility ability = switch (data.getStringOr("theme","crypt")) {
                        case "frost" -> TowerAbility.FROST_NOVA;
                        case "sands" -> TowerAbility.KNOCKBACK_SLAM;
                        case "roots" -> TowerAbility.POISON_CLOUD;
                        default -> TowerAbility.CURSE_OF_WEAKNESS;
                    };
                    ability.run(world,mob,target,TowerThemes.PLAINS,depth());
                }
            }
        }
    }

    private void unlockReward(ServerLevel world) {
        BlockPos pos=point("reward");
        // A one-shot replacement, so relogging or reopening a room cannot refill it.
        if (!world.getBlockState(pos).is(SlashBlocks.DELVE_SEAL.get())) return;
        world.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            String table = guardian() ? "delve_"+depth() : kind().equals("rest") ? "delve_rest" : "delve_supplies";
            chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("mmorpg:chests/"+table)),world.getRandom().nextLong());
            chest.setChanged();
        }
    }

    public void interact(Player player) {
        if (!(level instanceof ServerLevel world) || !(player instanceof ServerPlayer serverPlayer)) return;
        if (!configured()) return;
        if (kind().equals("rest") || kind().equals("final") && data.getBooleanOr("cleared",false)) {
            boolean enemies = !world.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(12),
                mob -> mob.isAlive() && mob.entityTags().stream().anyMatch(tag -> tag.startsWith(HOME))).isEmpty();
            if (enemies) {
                player.sendSystemMessage(Component.translatable("mmorpg.delve.exit_combat")); return;
            }
            BlockPos exit=point("exit");
            // May load the entrance chunk, but never place blocks or teleport into terrain.
            world.getChunk(exit);
            AABB box=player.getBoundingBox().move(exit.getX()+0.5-player.getX(),exit.getY()-player.getY(),exit.getZ()+0.5-player.getZ());
            if (!world.noCollision(box) || !world.getBlockState(exit.below()).isFaceSturdy(world,exit.below(),Direction.UP)
                || !world.getFluidState(exit).isEmpty()) {
                player.sendSystemMessage(Component.translatable("mmorpg.delve.exit_blocked")); return;
            }
            serverPlayer.teleportTo(exit.getX()+0.5,exit.getY(),exit.getZ()+0.5);
            serverPlayer.resetFallDistance();
            serverPlayer.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            player.sendSystemMessage(Component.translatable("mmorpg.delve.returned"));
        } else {
            player.sendSystemMessage(Component.translatable("mmorpg.delve.status",depth(), data.getIntOr("wave",0),active.size()));
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        var entity=event.getEntityLiving();
        if (entity == null || !(entity.level() instanceof ServerLevel world)) return;
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(HOME)) continue;
            String[] xyz=tag.substring(HOME.length()).split(",");
            if (xyz.length!=3) continue;
            try {
                BlockPos pos=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
                if (world.getBlockEntity(pos) instanceof DelveMarkerBlockEntity marker) {
                    marker.active.remove(entity.getUUID()); marker.missing.remove(entity.getUUID()); marker.setChanged();
                }
            } catch (NumberFormatException ignored) { }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        data.putString("active",String.join(";",active.stream().map(UUID::toString).toList()));
        output.store("AgeOfEpochData",CompoundTag.CODEC,data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        data=input.read("AgeOfEpochData",CompoundTag.CODEC).orElseGet(CompoundTag::new);
        active.clear(); missing.clear(); warmup=0; pulseDue=-1;
        for (String id : data.getStringOr("active","").split(";")) {
            try { active.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
        }
    }
}
