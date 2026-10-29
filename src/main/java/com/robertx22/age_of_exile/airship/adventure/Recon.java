package com.robertx22.age_of_exile.airship.adventure;

import java.util.ArrayList;
import java.util.List;

/** A personal field survey, not a fabricated harbour or a move of the shared world hub. */
public final class Recon {
    private Recon() {}
    public record Option(int action,String label,boolean enabled) {}
    public record Brief(String npc,int action,String text) {}
    public static final String CAPTAIN_START="Корабль пока остаётся здесь. Поговори с Веей о разведке, а потом отправляйся осмотреть местность. Вернёшься — доложишь мне.";
    public static final String CAPTAIN_FINISH="Вея получила оба наблюдения? Хорошо. Теперь у нас есть ещё кое-какие сведения для маршрута. Спасибо, что вернулся с ними.";
    public static final String VEYA_START="После того как примешь поручение, найди два разных биома в обычном мире, не ближе 128 блоков от корабля. Мне нужны новые наблюдения. Посмотришь оба — возвращайся ко мне.";
    public static final String VEYA_FINISH="Два разных биома, отлично. Я запишу, что ты видел, и передам сведения Маре. Теперь расскажи капитану, что вернулся.";
    public static final List<Brief> BRIEFS=List.of(
        new Brief("mara",84,"Запоминай, какая там местность. Если не знаешь, что видишь, так и скажи — потом вместе разберёмся. Я добавлю наблюдения к карте."),
        new Brief("quartermaster",85,"Возьми еды с запасом и проверь снаряжение перед выходом. Я не хочу потом искать твой второй ботинок по всему миру."),
        new Brief("oren",86,"Я буду у якоря, когда вернёшься. Если захочешь передохнуть перед докладом, заходи. Плед всё ещё на месте."),
        new Brief("selin",87,"Перед выходом проверь, есть ли у тебя еда и чем перевязать рану. И возвращайся, если почувствуешь себя плохо. Маршрут никуда не денется."),
        new Brief("lysander",88,"Можешь потом рассказать мне, что увидел? Интересны даже мелочи. А если что-то не захочешь записывать в журнал, просто скажи."),
        new Brief("tik",89,"Приёмник проверил, но источник голоса всё ещё не нашёл. Если услышишь что-нибудь странное, запомни, где и когда. И на знакомый голос не отвечай.")
    );
    public static boolean started(AdventureData d){return d.facts.contains("recon.started");}
    public static boolean accepted(AdventureData d){return d.facts.contains("recon.accepted");}
    public static boolean reported(AdventureData d){return d.facts.contains("recon.reported");}
    public static boolean finished(AdventureData d){return d.facts.contains("recon.finished");}
    public static int count(AdventureData d){return (int)Math.min(2,d.facts.stream().filter(x->x.startsWith("recon.biome:")).count());}
    public static List<Option> options(AdventureData d,String npc){
        var out=new ArrayList<Option>();if(!Council.finished(d)||finished(d))return out;
        if(!started(d)){if(npc.equals("captain"))out.add(new Option(80,"Обсудить разведку после совета",true));return out;}
        if(npc.equals("veya")&&!reported(d)){
            if(!accepted(d))out.add(new Option(81,"Принять разведку двух разных биомов",true));
            else out.add(new Option(82,"Доложить Вее • "+count(d)+"/2",count(d)>=2));
        }
        if(npc.equals("captain")&&reported(d))out.add(new Option(83,"Подвести итог разведки",true));
        for(var b:BRIEFS)if(npc.equals(b.npc())&&!d.facts.contains("recon.brief."+npc))out.add(new Option(b.action(),"Спросить о предстоящей разведке",true));
        return out;
    }
    /** The adapter supplies server-observed dimension and distance; no client progress packets. */
    public static boolean observe(AdventureData d,String biome,boolean overworld,boolean hubIdle,double distanceSquared){
        if(!accepted(d)||reported(d)||!overworld||!hubIdle||!Double.isFinite(distanceSquared)||distanceSquared<128*128||biome==null||biome.length()>160||!biome.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||count(d)>=2)return false;
        return d.facts.add("recon.biome:"+biome);
    }
    public static String act(AdventureData d,String npc,int action){
        if(options(d,npc).stream().noneMatch(o->o.action()==action&&o.enabled()))return "";
        if(action==80){d.facts.add("recon.started");return CAPTAIN_START;}
        if(action==81){d.facts.add("recon.accepted");return VEYA_START;}
        if(action==82){d.facts.add("recon.reported");AdventureEngine.bond(d,"veya","recon.return",8,5,8);return VEYA_FINISH;}
        if(action==83){d.facts.add("recon.finished");AdventureEngine.bond(d,"captain","recon.report",4,2,6);return CAPTAIN_FINISH;}
        for(var b:BRIEFS)if(b.action()==action){d.facts.add("recon.brief."+npc);return b.text();}
        return "";
    }
    public static String context(AdventureData d){
        return switch(Council.choice(d)){
            case "map"->"На совете ты выбрал сверку карты; рыба проверена Марой. Теперь названия реальных биомов дополнят карту, но не докажут судьбу ожидавших.";
            case "signal"->"На совете ты выбрал проверку сигнала; Тик подготовил приёмник. Наблюдения помогут уточнять местность, но источник голоса ещё неизвестен.";
            case "aid"->"На совете ты выбрал запас помощи; Селин подготовила хлеб. Наблюдения помогут искать путь, но не подтверждают, что выжившие найдены.";
            default->"Разведка начинается только после завершённого совета.";
        };
    }
    public static String objective(AdventureData d){
        if(!started(d))return "После совета обсуди разведку с капитаном.";
        if(finished(d))return "Разведка завершена • два наблюдения записаны. Экспедиция к гавани ещё не началась.";
        if(reported(d))return "Разведка • отчёт передан Вее; подведи итог у капитана.";
        if(!accepted(d))return "Разведка • поговори с Веей и явно прими поручение.";
        return "Разведка • "+count(d)+"/2 разных биомов обычного мира в 128+ блоках от корабля; затем вернись к Вее.";
    }
    public static List<String> journal(AdventureData d){
        var out=new ArrayList<String>();if(!started(d))return out;out.add(objective(d));out.add(context(d));
        d.facts.stream().filter(x->x.startsWith("recon.biome:")).sorted().forEach(x->out.add("Наблюдение • "+x.substring("recon.biome:".length())));
        return out;
    }
    public static String voiceKey(AdventureData d,String npc,int action){
        if(action==80&&npc.equals("captain")&&started(d))return "captain_recon_start";
        if(action==81&&npc.equals("veya")&&accepted(d))return "veya_recon_start";
        if(action==82&&npc.equals("veya")&&reported(d))return "veya_recon_finish";
        if(action==83&&npc.equals("captain")&&finished(d))return "captain_recon_finish";
        for(var b:BRIEFS)if(b.npc().equals(npc)&&b.action()==action&&d.facts.contains("recon.brief."+npc))return npc+"_recon";
        return "";
    }
    public static String voiceText(String key){
        return switch(key){case "captain_recon_start"->CAPTAIN_START;case "captain_recon_finish"->CAPTAIN_FINISH;case "veya_recon_start"->VEYA_START;case "veya_recon_finish"->VEYA_FINISH;default->BRIEFS.stream().filter(b->(b.npc()+"_recon").equals(key)).map(Brief::text).findFirst().orElse("");};
    }
}
