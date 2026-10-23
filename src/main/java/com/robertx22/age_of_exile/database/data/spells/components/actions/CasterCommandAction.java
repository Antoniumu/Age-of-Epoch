package com.robertx22.age_of_exile.database.data.spells.components.actions;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.library_of_exile.utils.CommandUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Collection;

import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.COMMAND;

public class CasterCommandAction extends SpellAction {

    public CasterCommandAction() {
        super(Arrays.asList(COMMAND));
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        try {

            if (ctx.world.isClientSide()) {
                return;
            }

            String command = data.get(COMMAND);
            if (command == null || command.isBlank()) {
                return;
            }

            Entity entity = ctx.caster;
            String normalized = command.trim();
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1).trim();
            }

            // Directly fulfill "give @s <item> [count]" in Java so inventory/offhand/command quirks never lose items
            if (entity instanceof Player player && normalized.startsWith("give @s ")) {
                String[] parts = normalized.split("\\s+");
                if (parts.length >= 3) {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(parts[2]));
                    int count = 1;
                    if (parts.length >= 4) {
                        try {
                            count = Integer.parseInt(parts[3]);
                        } catch (NumberFormatException ignored) {
                            count = 1;
                        }
                    }
                    if (item != null && item != Items.AIR && count > 0) {
                        int maxStack = new ItemStack(item).getMaxStackSize();
                        int remaining = count;
                        while (remaining > 0) {
                            int batch = Math.min(remaining, maxStack);
                            PlayerUtils.giveItem(new ItemStack(item, batch), player);
                            remaining -= batch;
                        }
                        return;
                    }
                }
            }

            CommandSourceStack source = CommandUtils.getCommandSource(entity);

            ctx.caster.level()
                .getServer()
                .getCommands()
                .performPrefixedCommand(source, normalized);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public MapHolder create(String command) {
        MapHolder d = new MapHolder();
        d.type = GUID();
        d.put(COMMAND, command);
        return d;
    }

    @Override
    public String GUID() {
        return "caster_command";
    }
}
