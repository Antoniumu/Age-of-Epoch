package com.robertx22.age_of_exile.airship.adventure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Personal pre-expedition council; no world move or claim of reaching an unexplored location. */
public final class Council {
    private Council() {}
    public record Option(int action,String label,boolean enabled) {}
    public record Speaker(String npc,int action,String text) {}
    public static final String START="Прежде чем выбирать путь, я хочу услышать команду. Не обязательно спрашивать каждого, но два разных взгляда помогут нам понять цену решения. Затем возвращайся ко мне. Мы выберем, что проверять первым.";
    public static final String CLOSE="Мы выполнили первое обещание выбранного плана. Это ещё не прибытие и не ответ обо всех пропавших. Но теперь в журнале есть действие, за которое мы готовы отвечать вместе.";
    public static final String MAP="Я сверила направление с тем, что мы действительно измерили. Неизвестные участки оставила пустыми. Если откроется новая улика, карта изменится вместе с нами.";
    public static final List<Speaker> SPEAKERS=List.of(
        new Speaker("mara",61,"На карте есть маршрут и есть то, что мы пока о нём не знаем. Я отмечу оба. Пустота на бумаге честнее дороги, которую никто из нас не проверял."),
        new Speaker("tik",62,"Приёмник настроен, но я не обещаю, что пойму любой сигнал. Зато обещаю показать тебе запись целиком, даже если в ней будет что-то, чего я испугался."),
        new Speaker("quartermaster",63,"Запасы рассчитаны не на красивый отчёт, а на людей. Если дорога займёт дольше, придётся делиться. Я лучше заранее скажу об этом, чем потом буду искать виноватых."),
        new Speaker("oren",64,"Я проверю крепление якоря ещё раз. Не потому, что тебе не доверяю. Просто когда я знаю, как он держится, мне легче думать о тех, кто будет на палубе."),
        new Speaker("veya",65,"Разведка начинается с пути назад. Если увидишь только дорогу вперёд, ещё раз осмотрись. Я пойду рядом, но решение повернуть не сделает нас слабее."),
        new Speaker("selin",66,"Я подготовила место для отдыха, но не могу решить за тебя, когда оно понадобится. Обещай лишь одно: если станет тяжело, не скрывай это ради красивой истории."),
        new Speaker("lysander",67,"Я записал имена тех, кто помог нам подготовиться. Не все хотят стать героями песни, и это надо уважать. Иногда достаточно, чтобы их работа не исчезла без следа.")
    );
    public static boolean started(AdventureData d){return d.facts.contains("council.started");}
    public static boolean finished(AdventureData d){return d.facts.contains("council.closed");}
    public static int heard(AdventureData d){return (int)SPEAKERS.stream().filter(s->d.facts.contains("council.heard."+s.npc())).count();}
    public static String choice(AdventureData d){for(String x:List.of("map","signal","aid"))if(d.facts.contains("council.choice."+x))return x;return "";}
    public static List<Option> options(AdventureData d,String npc,int copper,int bread){
        var out=new ArrayList<Option>();if(!Voyage.finished(d)||finished(d))return out;
        if(!started(d)){if(npc.equals("captain"))out.add(new Option(60,"Собрать мнения перед новым маршрутом",true));return out;}
        String decision=choice(d);
        if(decision.isEmpty()){
            for(var s:SPEAKERS)if(s.npc().equals(npc)&&!d.facts.contains("council.heard."+npc))out.add(new Option(s.action(),"Выслушать мнение о следующем пути",true));
            if(npc.equals("captain")){
                boolean enough=heard(d)>=2;
                out.add(new Option(70,"Выбрать карту • поймать новую рыбу для сверки",enough));
                out.add(new Option(71,"Выбрать сигнал • 2 медных слитка Тику",enough));
                out.add(new Option(72,"Выбрать помощь • 4 хлеба Селин",enough));
            }
            return out;
        }
        if(!d.facts.contains("council.fulfilled")){
            if(decision.equals("map")&&npc.equals("mara"))out.add(new Option(73,"Сверить новую рыбу • "+d.progress.getOrDefault("council.fish",0)+"/1",d.progress.getOrDefault("council.fish",0)>=1));
            if(decision.equals("signal")&&npc.equals("tik"))out.add(new Option(73,"Передать Тику 2 медных слитка • "+Math.min(2,copper)+"/2",copper>=2));
            if(decision.equals("aid")&&npc.equals("selin"))out.add(new Option(73,"Передать Селин 4 хлеба • "+Math.min(4,bread)+"/4",bread>=4));
        }else if(npc.equals("captain"))out.add(new Option(74,"Завершить совет и записать выполненное обещание",true));
        return out;
    }
    public static boolean fish(AdventureData d,String item,int amount,String event){
        if(!choice(d).equals("map")||d.facts.contains("council.fulfilled")||item==null||amount<=0||event==null||event.isBlank()||event.length()>160||!Set.of("minecraft:cod","minecraft:salmon","minecraft:pufferfish","minecraft:tropical_fish").contains(item))return false;
        if(d.progress.getOrDefault("council.fish",0)>=1||!d.events.add("council.fish:"+event))return false;
        d.progress.put("council.fish",1);return true;
    }
    /** Successful action 73 authorizes exactly one server-side resource debit, if applicable. */
    public static String act(AdventureData d,String npc,int action,int copper,int bread){
        if(options(d,npc,copper,bread).stream().noneMatch(o->o.action()==action&&o.enabled()))return "";
        if(action==60){d.facts.add("council.started");return START;}
        for(var s:SPEAKERS)if(s.action()==action){d.facts.add("council.heard."+npc);return s.text();}
        if(action>=70&&action<=72){
            String decision=action==70?"map":action==71?"signal":"aid";d.facts.add("council.choice."+decision);d.facts.add("council.promise.pending");
            return switch(decision){
                case "map"->"Ты выбрал сначала сверить карту: после решения поймай одну новую рыбу и вернись к Маре. Это проверка воды, а не доказательство судьбы пропавших.";
                case "signal"->"Ты выбрал сначала проверить приёмник: принеси Тику два медных слитка. Он сохранит и непонятные участки записи, не выдавая их за ответ.";
                default->"Ты выбрал сначала подготовить помощь: передай Селин четыре хлеба для общего запаса. Пока неизвестно, кто ждёт помощи и где его искать.";
            };
        }
        if(action==73){
            d.facts.remove("council.promise.pending");d.facts.add("council.fulfilled");AdventureEngine.bond(d,npc,"council."+choice(d),5,4,6);
            return switch(choice(d)){
                case "map"->MAP;
                case "signal"->"Тик: Приёмник подготовлен. Два слитка пошли на контакты и запасную пластину. Запись остаётся неполной, а неизвестный голос — неизвестным.";
                default->"Селин: Четыре хлеба добавлены в общий запас. Это не доказательство, что мы нашли выживших. Зато помощь будет готова, если мы их встретим.";
            };
        }
        if(action==74){d.facts.add("council.closed");return CLOSE;}
        return "";
    }
    public static String objective(AdventureData d){
        if(!started(d))return "После подготовки обсуди с капитаном следующий план.";
        if(finished(d))return "Совет завершён: первое обещание выполнено. Следующая экспедиция пока не началась.";
        if(choice(d).isEmpty())return "Совет • выслушано "+Math.min(2,heard(d))+"/2 разных мнений; затем выбери один план у капитана.";
        if(d.facts.contains("council.fulfilled"))return "Обещание выполнено • подведи итог у капитана.";
        return switch(choice(d)){case "map"->"План карты • новая рыба "+d.progress.getOrDefault("council.fish",0)+"/1; затем вернись к Маре.";case "signal"->"План сигнала • передай Тику 2 медных слитка.";default->"План помощи • передай Селин 4 хлеба.";};
    }
    public static List<String> journal(AdventureData d){
        var out=new ArrayList<String>();if(!started(d))return out;out.add(objective(d));
        for(var s:SPEAKERS)if(d.facts.contains("council.heard."+s.npc()))out.add(com.robertx22.age_of_exile.airship.AirshipStory.name(s.npc())+" • "+s.text());
        if(!choice(d).isEmpty())out.add("Выбранный план: "+switch(choice(d)){case "map"->"сверка карты";case "signal"->"проверка сигнала";default->"запас помощи";}+(d.facts.contains("council.fulfilled")?" • обещание выполнено":" • обещание открыто"));
        return out;
    }
    public static String voiceKey(AdventureData d,String npc,int action){
        if(action==60&&npc.equals("captain")&&started(d))return "captain_council";
        if(action==74&&npc.equals("captain")&&finished(d))return "captain_close";
        if(action==73&&npc.equals("mara")&&choice(d).equals("map")&&d.facts.contains("council.fulfilled"))return "mara_followup";
        for(var s:SPEAKERS)if(s.action()==action&&s.npc().equals(npc)&&d.facts.contains("council.heard."+npc))return npc+"_council";
        return "";
    }
    public static String voiceText(String key){
        if(key.equals("captain_council"))return START;if(key.equals("captain_close"))return CLOSE;if(key.equals("mara_followup"))return MAP;
        return SPEAKERS.stream().filter(s->(s.npc()+"_council").equals(key)).map(Speaker::text).findFirst().orElse("");
    }
}
