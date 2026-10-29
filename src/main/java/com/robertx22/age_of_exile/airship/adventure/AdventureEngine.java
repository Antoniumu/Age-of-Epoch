package com.robertx22.age_of_exile.airship.adventure;
import com.robertx22.age_of_exile.airship.AirshipData;
import com.robertx22.age_of_exile.airship.AirshipStory;
/** Pure transitions. Only server adapters may supply world events and inventory counts. */
public final class AdventureEngine {
    private AdventureEngine() {}
    public static void migrate(AirshipData legacy){
        AdventureData d=legacy.adventure;if(d.version>0)return;
        for(String id:AirshipStory.IDS){d.trust.put(id,Math.min(20,legacy.trust(id)));d.respect.put(id,Math.min(20,legacy.trust(id)));}
        for(int stage=0;stage<Math.min(11,legacy.stage);stage++)d.facts.add("memory:"+AirshipStory.actor(stage)+":legacy.stage."+stage);
        for(String id:AirshipStory.IDS)if((legacy.favorCompleted & (1<<AirshipStory.index(id)))!=0)d.facts.add("memory:"+id+":legacy.favor");
        if(legacy.stage==11)d.facts.add("legacy.introduction_complete");
        d.version=1; // Never touch old stage, choice, balances or favor baselines.
    }
    public static boolean accept(AdventureData d,String id){return AdventureCatalog.find(id)!=null && !d.completed.contains(id) && d.accepted.add(id);}
    public static int progress(AdventureData d,AdventureCatalog.Quest q,int inventory){return Math.clamp(q.metric()==AdventureCatalog.Metric.DELIVERY?inventory:d.progress.getOrDefault(q.id(),0),0,q.target());}
    public static boolean record(AdventureData d,AdventureCatalog.Metric metric,String subject,int amount,String eventId){
        if(subject==null||metric==null||metric==AdventureCatalog.Metric.DELIVERY||amount<=0||eventId==null||eventId.isBlank()||eventId.length()>160)return false;
        boolean changed=false;
        for(var q:AdventureCatalog.QUESTS){
            if(q.metric()!=metric||!d.accepted.contains(q.id())||d.completed.contains(q.id())||d.progress.getOrDefault(q.id(),0)>=q.target())continue;
            if(metric==AdventureCatalog.Metric.FISH && !java.util.Set.of("minecraft:cod","minecraft:salmon","minecraft:pufferfish","minecraft:tropical_fish").contains(subject))continue;
            String unique=q.id()+":"+(metric==AdventureCatalog.Metric.BIOME?subject:eventId);
            if(subject==null||subject.isBlank()||!d.events.add(unique))continue;
            d.progress.put(q.id(),(int)Math.min(q.target(),(long)d.progress.getOrDefault(q.id(),0)+(metric==AdventureCatalog.Metric.FISH?amount:1)));changed=true;
        }
        return changed;
    }
    public static boolean finish(AdventureData d,String id,int inventory){
        var q=AdventureCatalog.find(id);if(q==null||!d.accepted.contains(id)||d.completed.contains(id)||progress(d,q,inventory)<q.target())return false;
        d.completed.add(id);bond(d,q.npc(),"quest:"+id,20,15,20);return true;
    }
    public static boolean bond(AdventureData d,String npc,String memory,int trust,int close,int respect){
        if(AirshipStory.index(npc)<0||!d.facts.add("memory:"+npc+":"+memory))return false;
        add(d.trust,npc,trust);add(d.closeness,npc,close);add(d.respect,npc,respect);return true;
    }
    private static void add(java.util.Map<String,Integer> map,String key,int amount){map.put(key,(int)Math.clamp((long)map.getOrDefault(key,0)+amount,0,100));}
    public static int heart(AdventureData d,String npc){
        var q=AdventureCatalog.personal(npc);
        int cap=q!=null&&d.completed.contains(q.id())?50:25;
        if(d.facts.contains("memory:"+npc+":confidence"))cap=75;
        if(d.facts.contains("personal_finale:"+npc))cap=100;
        return Math.min(cap,(d.trust.getOrDefault(npc,0)+d.closeness.getOrDefault(npc,0)+d.respect.getOrDefault(npc,0))/3);
    }
    public static boolean witnessCall(AdventureData d){return d.facts.add("call.heard");}
    public static boolean chooseCall(AdventureData d,String choice){
        if(!d.facts.contains("call.heard")||!d.callDecision.isEmpty()||!java.util.Set.of("silence","answer","record").contains(choice))return false;
        d.callDecision=choice;d.facts.add("call."+choice);return true;
    }
    public static String callSummary(AdventureData d){
        if(!d.facts.contains("call.heard"))return "На мостике доступно расследование: «Голос за бортом».";
        return switch(d.callDecision){case "answer"->"Ты ответил. Сигнал повторил твой ответ: источник знает, что его слышат.";case "silence"->"Ты промолчал. Обращение повторилось с тем же интервалом, словно запись.";case "record"->"Ты попросил сохранить сигнал. Тик заметил два наложенных источника; запись ещё не расшифрована.";default->"Сигнал услышан. Вернись к капитану и реши, как на него отреагировать.";};
    }
    public static String confidence(String npc){return switch(npc){
        case "captain"->"После вахты всё ещё хожу по палубе, проверяю, все ли на местах. Сесть и отдохнуть почему-то труднее. Посидишь со мной немного?";
        case "mara"->"Здесь пока оставлю пустое место: я не знаю, что там. Раньше, наверное, нарисовала бы что-нибудь наугад. Не бери с меня пример.";
        case "quartermaster"->"Я отложил порцию для того, кто сегодня не пришёл к столу. Разогреем позже, если проголодается.";
        case "oren"->"Если станет тяжело, я скажу, что мне нужен перерыв. А ты напомни, если я опять стану упрямиться. Договорились?";
        case "veya"->"Если задержусь к ужину, сходи меня поискать, ладно? Обычно я отшучиваюсь, но мне правда спокойнее, когда кто-то знает, куда я ушла.";
        case "selin"->"Я опять сказала, что уже ела, да? Не ела. Если заметишь такое в следующий раз, напомни мне. Только не при всей команде, пожалуйста.";
        case "lysander"->"Кстати, если расскажешь что-то только для меня, я это никуда не запишу. Можешь сказать прямо — я не обижусь.";
        case "tik"->"Я всё ещё не отдал ключ Орену. То занят, то его нет рядом... Нет, это я прячусь. Давай сначала тебе скажу: я потерял ключ. Ну вот.";
        default->"Эта личная история ещё не открыта.";
    };}
    public static String greeting(AdventureData d,String npc){
        int h=heart(d,npc);boolean warm=h>=20;
        return switch(npc){
            case "captain"->warm?"Садись. Сегодня мне нужно твоё мнение, не отчёт.":"Вернулся? Я слушаю.";
            case "mara"->warm?"Хорошо, что ты здесь. Покажу карту вместе с исправлениями.":"Есть наблюдения? Не приукрашивай.";
            case "quartermaster"->warm?"Для тебя место за столом найдётся. Работа подождёт.":"Как дорога? Припасы посчитаем потом.";
            case "oren"->warm?"Побудешь рядом? Можно ничего не говорить.":"Здравствуй. Тебе помочь?";
            case "veya"->warm?"А я уже собиралась тебя искать. Не привыкай!":"Ну что, какие новости с земли?";
            case "selin"->warm?"Сегодня можно не быть полезным. Просто посиди со мной.":"Как ты себя чувствуешь? Только честно.";
            case "lysander"->warm?"Сегодня твоя история. Я попробую просто послушать.":"Расскажешь, что видел? Без украшений, обещаю.";
            case "tik"->warm?"Свой сигнал. Рад видеть. Это не запись!":"Сигнал принят. С возвращением.";
            default->"С возвращением.";
        };
    }
}
