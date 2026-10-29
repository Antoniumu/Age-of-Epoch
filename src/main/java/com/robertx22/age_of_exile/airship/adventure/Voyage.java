package com.robertx22.age_of_exile.airship.adventure;

import java.util.ArrayList;
import java.util.List;

/** Post-investigation preparation. Two different real server activities, not five mandatory collections. */
public final class Voyage {
    private Voyage() {}
    public enum Metric { PROFESSION, RUNEWORD, DUNGEON, MISSION, TOWER }
    public record Route(String id,String npc,int action,Metric metric,String title,String request,String debrief) {}
    public record Option(int action,String label,boolean enabled) {}
    public static final String START="Теперь ты знаешь, что случилось. Нам нужно подготовиться к следующему переходу. Поговори с командой и выбери два способа подготовки. Если с кем-то не согласен, так и скажи.";
    public static final String FINISH="Мы проверили оба выбранных способа. Я записал результаты и тех, кто помогал. Перед переходом ещё раз посмотрим, достаточно ли этого. Если нет — остановимся.";
    public static final String OUTRO="На карте я оставила выбранные способы и пустые места рядом. Мы проверили часть возможностей, а не всё на свете. Если кто-нибудь назовёт эту карту безошибочной, покажи ему мои исправления.";
    public static final String BRAM="Выбирай не самый красивый способ, а тот, для которого найдётся страховка. Ремесло не заменит разведку, а победа в подземелье не накормит людей. Два разных подхода лучше одного обещания на все случаи.";
    public static final String LYSANDER="Я запишу не только выбранные ответы, но и возражения. Человек, который не согласился идти, не становится врагом истории. Оставим ему место за столом. И не будем сочинять согласие тех, кого ещё не спросили.";
    public static final List<Route> ROUTES=List.of(
        new Route("craft","mara",41,Metric.PROFESSION,"Воспроизводимая работа",
            "Начнём с того, что можно воспроизвести. Примени одну из профессий после принятия поручения. Мне важен не новый значок в коллекции, а работа, которую ты действительно выполнишь сейчас.",
            "Мара: Работа подтверждена. На карту заношу воспроизводимый способ подготовки, не обещание, что ремесло устранит любой риск."),
        new Route("runes","tik",42,Metric.RUNEWORD,"Пределы настройки",
            "Собери одно рабочее рунное слово после поручения. Я проверю сам результат, не список найденных рун. И подпишу пределы настройки. На этот раз слово «надёжно» не будет означать «я очень надеюсь».",
            "Тик: Рабочее рунное слово подтверждено. Настройку записал. За пределами этой проверки я не буду выдавать предположение за гарантию."),
        new Route("scout","veya",43,Metric.DUNGEON,"Вернуться с опытом",
            "Мой способ — вернуться из обычного подземелья с новым опытом, а не со старым рассказом о победе. Прими поручение прежде, чем отправишься. Если сегодня не готов, выбери другой путь. Я не стану называть это трусостью.",
            "Вея: Ты вернулся. Это важнее красивого отчёта. Опыт похода есть; дорогу в неизвестное он не превращает в прогулку."),
        new Route("help","selin",44,Metric.MISSION,"Граница обещания",
            "Выполни одну божественную миссию после принятия поручения. Не потому, что чей-то ответ нужно признать единственно верным. Мне важно увидеть, какую помощь ты выберешь и где проведёшь границу обещаниям.",
            "Селин: Миссия выполнена. Помощь была настоящей. Но один поступок не обязывает тебя принимать все ответы того, кто дал поручение."),
        new Route("stability","oren",45,Metric.TOWER,"След устойчивости",
            "Я могу сверить устойчивость по следу башенного стража. Если выберешь этот путь, победи одного настоящего стража башни после поручения. Но не ходи туда, чтобы доказать мне свою смелость. Другие способы тоже годятся.",
            "Орен: След стража подтверждён. Есть с чем сравнивать нагрузку. И всё же, если попрошу остановиться, давай остановимся.")
    );
    private static String accepted(Route r){return "voyage.accept."+r.id();}
    private static String done(Route r){return "voyage.done."+r.id();}
    private static String progress(Route r){return "voyage.progress."+r.id();}
    public static boolean started(AdventureData d){return d.facts.contains("voyage.started");}
    public static boolean finished(AdventureData d){return d.facts.contains("voyage.finished");}
    public static int completed(AdventureData d){return (int)ROUTES.stream().filter(r->d.facts.contains(done(r))).count();}
    public static int active(AdventureData d){return (int)ROUTES.stream().filter(r->d.facts.contains(accepted(r))&&!d.facts.contains(done(r))).count();}
    public static List<Option> options(AdventureData d,String npc){
        var out=new ArrayList<Option>();if(!d.facts.contains("truth.shared"))return out;
        if(!started(d)){if(npc.equals("captain"))out.add(new Option(40,"Начать подготовку • выбрать два способа",true));return out;}
        if(finished(d)){if(npc.equals("mara")&&!d.facts.contains("voyage.outro"))out.add(new Option(49,"Обсудить пределы новой карты",true));return out;}
        for(var r:ROUTES)if(r.npc().equals(npc)&&!d.facts.contains(done(r))){
            boolean a=d.facts.contains(accepted(r));int n=d.progress.getOrDefault(progress(r),0);
            out.add(new Option(r.action(),(a?"Доложить "+n+"/1 • ":"Принять • ")+r.title(),a?n>=1:active(d)<2));
            if(a)out.add(new Option(r.action()+10,"Отказаться от этого способа; выбрать другой",true));
        }
        if(npc.equals("captain"))out.add(new Option(48,"Подвести итоги подготовки • "+Math.min(2,completed(d))+"/2",completed(d)>=2&&active(d)==0));
        if(npc.equals("quartermaster")&&!d.facts.contains("voyage.brief.bram"))out.add(new Option(46,"Спросить Брама о страховке",true));
        if(npc.equals("lysander")&&!d.facts.contains("voyage.brief.lysander"))out.add(new Option(47,"Сохранить возражения, а не сочинять согласие",true));
        return out;
    }
    /** The only progress entry point; called by native server callbacks, never a C2S metric payload. */
    public static boolean record(AdventureData d,Metric metric,String subject,String event){
        if(!started(d)||finished(d)||metric==null||subject==null||subject.isBlank()||subject.length()>160||event==null||event.isBlank()||event.length()>160)return false;
        for(var r:ROUTES)if(r.metric()==metric&&d.facts.contains(accepted(r))&&!d.facts.contains(done(r))&&d.progress.getOrDefault(progress(r),0)==0){
            if(!d.events.add("voyage."+r.id()+":"+event))return false;
            d.progress.put(progress(r),1);d.facts.add("voyage.observed."+r.id()+":"+subject);return true;
        }
        return false;
    }
    public static String act(AdventureData d,String npc,int action){
        if(options(d,npc).stream().noneMatch(o->o.action()==action&&o.enabled()))return "";
        if(action==40){d.facts.add("voyage.started");return START;}
        if(action==46){d.facts.add("voyage.brief.bram");return BRAM;}
        if(action==47){d.facts.add("voyage.brief.lysander");return LYSANDER;}
        if(action==48){d.facts.add("voyage.finished");AdventureEngine.bond(d,"captain","voyage.prepared",5,3,8);return FINISH;}
        if(action==49){d.facts.add("voyage.outro");return OUTRO;}
        for(var r:ROUTES){
            if(action==r.action()+10){
                d.facts.remove(accepted(r));d.progress.remove(progress(r));
                d.events.removeIf(s->s.startsWith("voyage."+r.id()+":"));d.facts.removeIf(s->s.startsWith("voyage.observed."+r.id()+":"));
                return "Отложено: "+r.title()+". Слот подготовки свободен; при повторном принятии понадобится новое действие. Отказ не уменьшает отношения.";
            }
            if(action==r.action()){
                if(d.facts.add(accepted(r)))return r.request();
                d.facts.add(done(r));AdventureEngine.bond(d,npc,"voyage."+r.id(),8,3,8);return r.debrief();
            }
        }
        return "";
    }
    public static String objective(AdventureData d){
        if(!started(d))return "После раскрытия правды обсуди с капитаном способы подготовки.";
        if(finished(d))return "Подготовка завершена • проверено "+completed(d)+" разных способов. Следующая экспедиция ещё не началась.";
        return "Подготовка • "+Math.min(2,completed(d))+"/2 разных способов; активно "+active(d)+"/2. От ненужного поручения можно отказаться у его автора.";
    }
    public static List<String> activeGoals(AdventureData d){
        var out=new ArrayList<String>();
        if(started(d)&&!finished(d))for(var r:ROUTES)if(d.facts.contains(accepted(r))&&!d.facts.contains(done(r)))out.add("ПОДГОТОВКА • "+r.title()+" • "+d.progress.getOrDefault(progress(r),0)+"/1");return out;
    }
    public static List<String> journal(AdventureData d){
        var out=new ArrayList<String>();if(!started(d))return out;
        out.add(objective(d));for(var r:ROUTES)if(d.facts.contains(accepted(r)))out.add((d.facts.contains(done(r))?"✓ ":"• ")+r.title()+" • "+d.progress.getOrDefault(progress(r),0)+"/1 • "+com.robertx22.age_of_exile.airship.AirshipStory.name(r.npc()).split(" • ")[0]);return out;
    }
    public static String voiceText(String key){
        return switch(key){
            case "captain_voyage_start"->START;case "captain_voyage_finish"->FINISH;
            case "mara_voyage_outro"->OUTRO;case "quartermaster_voyage_brief"->BRAM;case "lysander_voyage_brief"->LYSANDER;
            default->ROUTES.stream().filter(r->(r.npc()+"_voyage_route").equals(key)).map(Route::request).findFirst().orElse("");
        };
    }
    public static String voiceKey(AdventureData d,String npc,int action){
        if(action==40&&npc.equals("captain")&&started(d))return "captain_voyage_start";
        if(action==48&&npc.equals("captain")&&finished(d))return "captain_voyage_finish";
        if(action==49&&npc.equals("mara")&&d.facts.contains("voyage.outro"))return "mara_voyage_outro";
        if(action==46&&npc.equals("quartermaster")&&d.facts.contains("voyage.brief.bram"))return "quartermaster_voyage_brief";
        if(action==47&&npc.equals("lysander")&&d.facts.contains("voyage.brief.lysander"))return "lysander_voyage_brief";
        for(var r:ROUTES)if(r.action()==action&&r.npc().equals(npc)&&d.facts.contains(accepted(r))&&!d.facts.contains(done(r)))return npc+"_voyage_route";
        return "";
    }
}
