package com.robertx22.age_of_exile.dimension.packets;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.data.tiers.base.Difficulty;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.dimension.dungeon_data.TeamSize;
import com.robertx22.age_of_exile.dimension.teleporter.TeleportedBlockEntity;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TeamUtils;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Клиент → сервер: «начать подземелье из этого портала, вот с такой сложностью
 * и таким составом отряда».
 *
 * <p>Сложность приходит с клиента, поэтому сервер её валидирует: должна быть
 * зарегистрирована, доступна по уровню и запрошена у реально существующего
 * блока-портала рядом с игроком.</p>
 */
public class StartDungeonPacket extends MyPacket<StartDungeonPacket> {

    BlockPos pos = BlockPos.ZERO;
    TeamSize teamSize = TeamSize.SOLO;
    String diff = "";

    public StartDungeonPacket() {

    }

    public StartDungeonPacket(TeamSize teamSize, BlockPos pos, String diff) {
        this.pos = pos;
        this.teamSize = teamSize;
        this.diff = diff == null ? "" : diff;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "start_dun");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        pos = tag.readBlockPos();
        teamSize = tag.readEnum(TeamSize.class);
        diff = tag.readUtf(100);
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeBlockPos(pos);
        tag.writeEnum(teamSize);
        tag.writeUtf(diff);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        Player player = ctx.getPlayer();

        if (player == null) {
            return;
        }

        if (!ExileDB.Difficulties()
            .isRegistered(diff)) {
            player.sendSystemMessage(Component.literal("Неизвестная сложность подземелья.")
                .withStyle(ChatFormatting.RED));
            return;
        }

        Difficulty difficulty = ExileDB.Difficulties()
            .get(diff);

        int playerLevel = Load.Unit(player)
            .getLevel();

        if (playerLevel < difficulty.req_player_lvl) {
            player.sendSystemMessage(Component.literal("Для этой сложности нужен уровень " + difficulty.req_player_lvl + ".")
                .withStyle(ChatFormatting.RED));
            return;
        }

        if (player.blockPosition()
            .distSqr(pos) > 64) {
            player.sendSystemMessage(Component.literal("Портал слишком далеко.")
                .withStyle(ChatFormatting.RED));
            return;
        }

        BlockEntity tile = player.level()
            .getBlockEntity(pos);

        if (!(tile instanceof TeleportedBlockEntity be) || !be.data.type.isDungeon()) {
            player.sendSystemMessage(Component.literal("Портал не найден.")
                .withStyle(ChatFormatting.RED));
            return;
        }

        if (teamSize.requiredMemberAmount > 1) {
            if (ServerContainer.get().REQUIRE_TEAM_FOR_TEAM_DUNGEONS.get()) {
                if (TeamUtils.getOnlineTeamMembersInRange(player)
                    .size() < teamSize.requiredMemberAmount) {
                    player.sendSystemMessage(Component.literal("Рядом должно быть не менее " + teamSize.requiredMemberAmount + " участников группы для начала подземелья в этом режиме.")
                        .withStyle(ChatFormatting.RED));
                    player.sendSystemMessage(Component.literal("Используйте /age_of_exile teams"));

                    return;
                }
            }
        }

        RPGPlayerData rpg = Load.playerRPGData(player);

        // создаём именно ту сложность, которую выбрал игрок,
        // и заново рандомизируем уровень/мобов/аффиксы
        rpg.createRandomDungeon(difficulty);

        be.data.activated = true;
        be.setChanged();

        rpg.maps.onStartDungeon(player, teamSize, pos, rpg.maps.dungeonData.uuid);

        player.sendSystemMessage(Component.literal("Экспедиция создана: ")
            .append(difficulty.locName())
            .append(Component.literal(", уровень " + rpg.maps.dungeonData.lv))
            .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("Войдите в портал рядом с блоком телепорта, чтобы отправиться внутрь.")
            .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public MyPacket<StartDungeonPacket> newInstance() {
        return new StartDungeonPacket();
    }
}
