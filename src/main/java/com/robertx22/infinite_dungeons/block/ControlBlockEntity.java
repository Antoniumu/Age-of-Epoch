package com.robertx22.infinite_dungeons.block;

import com.robertx22.forge_compat.blocks.ITickableBlockEntity;
import com.robertx22.forge_compat.network.ClientboundTitleCompatPacket;
import com.robertx22.infinite_dungeons.components.MobIDCap;
import com.robertx22.infinite_dungeons.components.PlayerIDCap;
import com.robertx22.infinite_dungeons.database.CompletitionScore;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonSpawn;
import com.robertx22.infinite_dungeons.main.DungeonBlockEntities;
import com.robertx22.library_of_exile.utils.LoadSave;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ControlBlockEntity extends BlockEntity implements ITickableBlockEntity {

    public ControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ControlBlockData data = new ControlBlockData();

    public ControlBlockEntity() {
        this(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
    }

    public ControlBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonBlockEntities.CONTROL_BLOCK.get(), pos, state);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void tick() {
        try {

            if (level.isClientSide()) {
                return;
            }

            if (data != null) {

                data.ticks++;

                if (data.ticks % 40 == 0) {

                    if (data.quest_finished) {
                        if (!data.cleared) {
                            if (this.getAllDungeonMobsAlive()
                                .size() == 0) {
                                this.data.cleared = true;
                            }
                        }
                    }

                    for (Player p : getAllPlayers()) {
                        ServerPlayer sp = (ServerPlayer) p;
                        PlayerIDCap.get(sp).data.buy_history.shop_list = this.data.getDungeonLayout()
                            .getShopList()
                            .GUID();
                        PlayerIDCap.get(sp)
                            .syncToClient(sp);
                        ClientboundBlockEntityDataPacket pkt = getUpdatePacket();
                        if (pkt != null) {
                            sp.connection.send(pkt);
                        }
                    }
                }

                if (getAllPlayers().isEmpty()) {
                    data.no_players_nearby_ticks++;

                    if (data.no_players_nearby_ticks > 20 * 5) {
                        this.destroyDungeon();
                        return;
                    }
                }

                if (data.cleared) {
                    data.ticks_until_destruction--;

                    if (data.ticks_until_destruction % 100 == 0 || (data.ticks_until_destruction % 20 == 0 && data.ticks_until_destruction < 100)) {
                        int sec = data.ticks_until_destruction / 20;
                        sendMessage(Component.literal(sec + "s until Self Destruction"));
                    }
                    if (data.ticks_until_destruction < 1) {
                        this.destroyDungeon();

                    }
                    return;
                }

                if (getAllDungeonMobsAlive()
                    .size() != 0) {
                    data.no_mobs_alive_ticks = 0;
                } else {
                    data.no_mobs_alive_ticks++;

                }

                data.getDungeonLayout()
                    .getDungeonType()
                    .onControlBlockTick(this);

                for (DungeonModifier x : data.getModifiers()) {
                    x.onControlBlockTick(this);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            this.sendMessage(Component.literal("Error in Dungeon Control Block code, destroying dungeon :("));
            this.destroyDungeon();
            this.setRemoved();
        }

    }

    public void spawnMob() {

        List<DungeonSpawn> list = data.getDungeonLayout().spawns.stream()
            .filter(x -> x.canSpawn(data))
            .collect(Collectors.toList());

        DungeonSpawn spawn = RandomUtils.weightedRandom(list);

        EntityType type = spawn.getEntityType();

        BlockPos pos = getRandomPosition(type);

        LivingEntity en = (LivingEntity) type.create(level, EntitySpawnReason.STRUCTURE);
        en.setPos(pos.getX(), pos.getY(), pos.getZ());

        if (en instanceof Mob mob) {
            mob.finalizeSpawn((ServerLevelAccessor) level, ((ServerLevel) level).getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        }

        for (DungeonModifier x : data.getModifiers()) {
            x.onMobSpawn(en);
        }

        MobIDCap.get(en).data.dungeon_uuid = this.data.dungeon_uuid;
        MobIDCap.get(en).data.control_block_pos = getBlockPos();

        for (String mod : spawn.modifiers) {
            try {
                DungeonModifier m = DungeonsDB.DungeonModifiers()
                    .get(mod);
                m.onMobSpawn(en);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        data.mobs_spawned++;
        level.addFreshEntity(en);

        applyDifficultyMods(en);

    }

    private void applyDifficultyMods(LivingEntity en) {

        DungeonDifficulty diff = data.getDifficulty();

        AttributeModifier m2 = new AttributeModifier(
            Identifier.fromNamespaceAndPath("infinite_dungeons", "72738c54-5357-11ec-bf63-0242ac130002"),
            diff.mob_hp_multi - 1F,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        en.getAttribute(Attributes.MAX_HEALTH)
            .addPermanentModifier(m2);

        en.heal(500000);

    }

    public void destroyDungeon() {

        try {
            for (Player x : getAllPlayers()) {
                x.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 30));
            }

            if (this.level instanceof ServerLevel sl) {
                this.getAllDungeonMobsAlive()
                    .forEach(x -> x.kill(sl));
            }

            BlockPos.betweenClosed(data.dungeon_boundary_1, data.dungeon_boundary_0)
                .forEach(x -> {
                    if (level.getBlockEntity(x) != null) {
                        this.level.destroyBlock(x, true);
                    } else {
                        this.level.setBlockAndUpdate(x, Blocks.AIR.defaultBlockState());
                    }
                });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<LivingEntity> getAllDungeonMobsAlive() {
        BlockPos p = this.getBlockPos();
        AABB box = new AABB(p.getX() - 1, p.getY() - 1, p.getZ() - 1, p.getX() + 1, p.getY() + 1, p.getZ() + 1);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box
            .inflate(40));
        list.removeIf(x -> !isInsideDungeon(x.blockPosition()));
        list.removeIf(x -> !MobIDCap.get(x)
            .isDungeonMob());
        return list;
    }

    public Boolean isInsideDungeon(BlockPos pos) {
        return pos.getX() >= data.dungeon_boundary_0.getX() &&
            pos.getY() >= data.dungeon_boundary_0.getY() &&
            pos.getZ() >= data.dungeon_boundary_0.getZ() &&
            pos.getX() <= data.dungeon_boundary_1.getX() &&
            pos.getY() <= data.dungeon_boundary_1.getY() &&
            pos.getZ() <= data.dungeon_boundary_1.getZ();
    }

    public List<Player> getAllPlayers() {
        List<Player> list = (List<Player>) (List<?>) level.players()
            .stream()
            .filter(x -> {
                return isInsideDungeon(x.blockPosition());
            })
            .collect(Collectors.toList());
        return list;
    }

    public void spawnRewardsAndFinish(CompletitionScore score) {

        if (!this.data.quest_finished) {
            this.data.quest_finished = true;

            BlockPos pos = data.chests_pos.get(0);
            if (data.getDungeonLayout().score_configs.containsKey(score)) {
                spawnChestWithLoot(pos, data.getDungeonLayout().score_configs.get(score)
                    .getLootTable());

                Villager trader = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
                trader.setPos(pos.getX() + 1, pos.getY(), pos.getZ());
                level.addFreshEntity(trader);
                trader.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 1000, 10));
                trader.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 1000, 10));

            }

            finishMessage(score);
        }

    }

    public static String STAR = "\u272B";

    public void finishMessage(CompletitionScore score) {

        Component text = Component.literal("Dungeon Cleared.").withStyle(ChatFormatting.GREEN);

        if (score == CompletitionScore.FAIL) {
            text = Component.literal("Dungeon Failed.").withStyle(ChatFormatting.RED);
        }

        sendTitleMessage(text);

        if (score != CompletitionScore.FAIL) {
            String stars = "";

            for (int i = 0; i <= score.tier; i++) {
                stars += STAR;
            }
            Component scoretxt = Component.literal("Score: " + stars).withStyle(score.format);
            sendTitleMessage(scoretxt, ClientboundTitleCompatPacket.Type.SUBTITLE);
        }
    }

    public void spawnChestWithLoot(BlockPos pos, Identifier loottable) {

        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 0);
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(pos);
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, loottable);
        chest.setLootTable(key, level.getRandom().nextLong());

    }

    public static boolean canPlaceMob(Level world, EntityType type, BlockPos p) {
        AABB box = type.getSpawnAABB(p.getX(), p.getY(), p.getZ());
        boolean can = world.noCollision(box);
        return can;
    }

    public BlockPos getRandomPosition() {
        return getRandomPosition(null);
    }

    public BlockPos getRandomPosition(EntityType type) {

        BlockPos start = data.dungeon_boundary_0;
        BlockPos end = data.dungeon_boundary_1;

        List<Player> players = getAllPlayers();

        BlockPos pos = null;

        while (pos == null) {

            BlockPos trypos = new BlockPos(
                RandomRange(start.getX(), end.getX(), level.getRandom()),
                RandomRange(start.getY(), end.getY(), level.getRandom()),
                RandomRange(start.getZ(), end.getZ(), level.getRandom())
            );

            if (players.stream()
                .anyMatch(x -> x.blockPosition()
                    .distManhattan(trypos) < 8)) {
                continue;
            }

            if (type == null) {
                if (level.getBlockState(trypos)
                    .isAir()) {
                    pos = trypos;
                }
            } else {
                if (canPlaceMob(level, type, trypos)) {
                    pos = trypos;
                }
            }
        }
        return pos;
    }

    public static int RandomRange(int min, int max, RandomSource rand) {
        try {
            int reamin = Math.min(min, max);
            int realmax = Math.max(min, max);
            int result = rand.nextInt(realmax - reamin + 1);
            return Mth.clamp(result + reamin, reamin, realmax);
        } catch (Exception var4) {
            var4.printStackTrace();
            return 0;
        }
    }

    public static int RandomRange(int min, int max, Random rand) {
        try {

            int reamin = Math.min(min, max);
            int realmax = Math.max(min, max);

            int result = rand.nextInt(realmax - reamin + 1);
            return Mth.clamp(result + reamin, reamin, realmax);
        } catch (Exception var4) {
            var4.printStackTrace();
            return 0;
        }
    }

    public void sendTitleMessage(Component txt) {
        sendTitleMessage(txt, ClientboundTitleCompatPacket.Type.TITLE);
    }

    public void sendTitleMessage(Component txt, ClientboundTitleCompatPacket.Type type) {
        getAllPlayers().forEach(x -> {
            ServerPlayer sp = (ServerPlayer) x;
            new ClientboundTitleCompatPacket(type, txt).sendTo(sp);
        });
    }

    public void sendMessage(Component txt) {
        getAllPlayers().forEach(x -> {
            x.sendSystemMessage(txt);
        });
    }

    public boolean hasNearbyPlayers() {
        return !getAllPlayers().isEmpty();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag nbt = new CompoundTag();
        this.save(nbt);
        output.store("AgeOfEpochData", CompoundTag.CODEC, nbt);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("AgeOfEpochData", CompoundTag.CODEC).ifPresent(nbt -> this.load(this.getBlockState(), nbt));
    }

    public void load(BlockState state, CompoundTag nbt) {
        try {
            this.data = LoadSave.Load(ControlBlockData.class, new ControlBlockData(), nbt, "dungeon");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CompoundTag save(CompoundTag nbt) {
        try {
            LoadSave.Save(data, nbt, "dungeon");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nbt;
    }

}
