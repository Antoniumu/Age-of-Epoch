package com.robertx22.age_of_exile.airship;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;

/** Operator-only import preview of the user-supplied schematic. Never replaces an existing mooring. */
public final class WingedSailorService {
    private WingedSailorService() {}
    private static final BlockPos LIFT = new BlockPos(57,51,38);
    private static final int[][] CREW={{58,63,67},{59,56,50},{56,51,61}};
    public static int summon(ServerPlayer p) {
        if(!Level.OVERWORLD.equals(p.level().dimension())) return tell(p,"Winged Sailor доступен только в обычном мире.");
        AirshipData d=AirshipService.data(p);
        if(d.summoned) return tell(p,"Причал уже занят твоим кораблём. Для проверки большого корабля используй новый тестовый мир/персонажа.");
        ServerLevel level=(ServerLevel)p.level();
        var template=level.getServer().getStructureTemplateManager().get(Identifier.fromNamespaceAndPath("mmorpg","winged_sailor")).orElse(null);
        if(template==null) return tell(p,"Не найдена структура Winged Sailor.");
        var size=template.getSize();
        if(size.getX()!=117 || size.getY()!=100 || size.getZ()!=89) return tell(p,"Неверный размер структуры; размещение отменено.");
        // Select a dry, head-clear landing near the player before choosing the ship origin.
        BlockPos landing=null;
        for(int radius=0;radius<=8 && landing==null;radius++) for(int dx=-radius;dx<=radius && landing==null;dx++)
            for(int dz=-radius;dz<=radius;dz++) {
                int x=p.blockPosition().getX()+dx,z=p.blockPosition().getZ()+dz;
                if(!level.hasChunkAt(new BlockPos(x,p.blockPosition().getY(),z))) continue;
                int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                BlockPos feet=new BlockPos(x,y,z);
                if(level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() &&
                    level.getBlockState(feet.below()).isSolid() && level.getFluidState(feet.below()).isEmpty()) {
                    landing=feet;break;
                }
            }
        if(landing==null) return tell(p,"Не найдена сухая свободная площадка для спуска рядом с тобой.");
        int ox=landing.getX()-LIFT.getX(),oz=landing.getZ()-LIFT.getZ();
        int top=Integer.MIN_VALUE;
        for(int x=-3;x<size.getX()+3;x++) for(int z=-3;z<size.getZ()+3;z++) {
            BlockPos column=new BlockPos(ox+x,p.blockPosition().getY(),oz+z);
            if(!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column))
                return tell(p,"Для большого корабля нужны загруженные чанки в радиусе 70 блоков. Увеличь дальность, подожди загрузки и повтори.");
            top=Math.max(top,level.getHeight(Heightmap.Types.MOTION_BLOCKING,ox+x,oz+z));
        }
        int base=top+8;
        if(base+size.getY()+3>=level.getMaxY()) return tell(p,"Над этим рельефом корабль не помещается по высоте. Найди равнину или побережье.");
        BlockPos origin=new BlockPos(ox,base,oz);
        for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-3,-3,-3),origin.offset(size.getX()+2,size.getY()+2,size.getZ()+2)))
            if(!level.getBlockState(pos).isAir()) return tell(p,"Объём большого корабля занят. Существующие блоки не изменены.");
        AABB box=new AABB(ox-3,base-3,oz-3,ox+size.getX()+3,base+size.getY()+3,oz+size.getZ()+3);
        if(!level.getEntitiesOfClass(Entity.class,box,e->true).isEmpty()) return tell(p,"В небе есть сущности; размещение отменено.");
        BlockPos lift=origin.offset(LIFT);
        int ground=landing.getY();
        BlockPos floor=new BlockPos(lift.getX(),ground-1,lift.getZ());
        if(!level.getBlockState(floor).isSolid() || !level.getFluidState(floor).isEmpty())
            return tell(p,"Под подъёмником нет безопасной сухой площадки. Отойди от воды или дерева.");
        // Allocate every actor before editing the world; all local spots were checked by the converter.
        ArrayList<LivingEntity> crew=new ArrayList<>();
        for(int i=0;i<CREW.length;i++) {
            Entity e=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("mannequin")).create(level,EntitySpawnReason.COMMAND);
            if(!(e instanceof LivingEntity actor)) return tell(p,"Не удалось создать команду.");
            int[] local=CREW[i];
            actor.snapTo(ox+local[0]+0.5,base+local[1],oz+local[2]+0.5,180F,0F);
            actor.setCustomName(Component.literal(AirshipStory.name(AirshipStory.IDS[i])));
            actor.setCustomNameVisible(false);actor.setSilent(true);
            actor.addTag(AirshipService.TAG);actor.addTag("aoe_npc_"+AirshipStory.IDS[i]);
            crew.add(actor);
        }
        var settings=new StructurePlaceSettings();settings.setKnownShape(true);
        if(!template.placeInWorld(level,origin,origin,settings,level.getRandom(),2)) return tell(p,"Ошибка размещения структуры; проверь журнал сервера.");
        // Validate the actual data-fixed structure, not just the original schematic's palette.
        for(LivingEntity actor:crew) {
            BlockPos feet=actor.blockPosition();
            if(!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir() || !level.getBlockState(feet.below()).isSolid())
                return tell(p,"Структура поставлена, но место NPC после обновления блоков небезопасно. Команда не создана; проверь мир.");
        }
        for(LivingEntity actor:crew) { level.addFreshEntity(actor);AirshipService.configureSkin(level,actor); }
        d.x=lift.getX();d.y=lift.getY()-1;d.z=lift.getZ()-4;d.groundY=ground;d.summoned=true;d.layout="winged_sailor";
        Load.playerRPGData(p).syncToClient(p);
        System.out.println("AIRSHIPTEST WINGED placed origin="+origin+" size="+size+" crew="+crew.size());
        return tell(p,"Winged Sailor на причале. Световой круг: X="+d.x+", Z="+(d.z+4)+". Капитан наверху, Мара у карт, Брам на нижней палубе.");
    }
    private static int tell(ServerPlayer p,String message) { p.sendSystemMessage(Component.literal(message));return 1; }
}
