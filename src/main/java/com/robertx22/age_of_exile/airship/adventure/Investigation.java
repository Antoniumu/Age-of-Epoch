package com.robertx22.age_of_exile.airship.adventure;

import java.util.ArrayList;
import java.util.List;

/** A personal evidence ledger. Public here means this character's crew scene, never a world-wide unlock. */
public final class Investigation {
    private Investigation() {}
    public static final String ANALYSIS="Первая фраза совпадает с предупреждением капитана. Но это не доказывает, что за бортом живой человек. Отделим то, что слышали, от того, что хотим услышать. Попроси Тика проверить второй источник.";
    public static final String CONFESSION="Тридцать лет назад я перевёл резерв якоря на наш корабль. Мы ушли, а у другого причала не осталось возможности перейти. Я не знаю, сколько людей выжило. И я слишком долго об этом молчал.";
    public record Option(int action,String label) {}
    public static boolean started(AdventureData d){return d.facts.contains("investigation.analysis");}
    public static boolean decided(AdventureData d){return d.facts.contains("truth.shared")||d.facts.contains("truth.public")||d.facts.contains("truth.review")||d.facts.contains("truth.mediator");}
    public static List<Option> options(AdventureData d,String npc){
        var list=new ArrayList<Option>();
        if(d.callDecision.isEmpty())return list;
        if(npc.equals("mara")&&!started(d))list.add(new Option(30,"Сопоставить услышанное предупреждение"));
        if(!started(d))return list;
        if(npc.equals("tik")&&!d.facts.contains("evidence.device"))list.add(new Option(31,"Восстановить приёмник • 4 медных слитка"));
        if(npc.equals("lysander")&&!d.facts.contains("evidence.witness"))list.add(new Option(32,"Спросить, что он видел у старого причала"));
        if(npc.equals("oren")&&evidenceCount(d)==3&&!d.facts.contains("evidence.crosscheck"))list.add(new Option(33,"Сверить три свидетельства с якорем"));
        if(npc.equals("captain")&&d.facts.contains("evidence.crosscheck")&&!d.facts.contains("testimony.captain"))list.add(new Option(34,"Спросить о цене первого спасения"));
        if(npc.equals("captain")&&d.facts.contains("testimony.captain")&&!decided(d)){
            list.add(new Option(35,"Открыть проверенные сведения экипажу"));
            list.add(new Option(36,"Сначала техническая проверка; затем общий разговор"));
            list.add(new Option(37,"Попросить Лисандра подготовить общий разговор"));
        }
        if(npc.equals("mara")&&d.facts.contains("truth.review"))list.add(new Option(38,"Закончить проверку и выполнить обещание рассказать"));
        if(npc.equals("lysander")&&d.facts.contains("truth.mediator"))list.add(new Option(39,"Провести подготовленный разговор с экипажем"));
        return list;
    }
    public static int evidenceCount(AdventureData d){
        int n=0;for(String key:List.of("evidence.water","evidence.device","evidence.witness"))if(d.facts.contains(key))n++;return n;
    }
    public static String fish(AdventureData d,String subject,int amount,String event){
        if(subject==null||!started(d)||d.facts.contains("evidence.water")||amount<=0||event==null||event.isBlank()||event.length()>160||!java.util.Set.of("minecraft:cod","minecraft:salmon","minecraft:pufferfish","minecraft:tropical_fish").contains(subject))return "";
        if(!d.events.add("investigation.fish:"+event))return "";
        int n=(int)Math.min(3,(long)d.progress.getOrDefault("investigation.water",0)+amount);d.progress.put("investigation.water",n);
        if(n<3)return "";
        d.facts.add("evidence.water");
        return "Среди улова обнаружена промокшая маршрутная метка. На ней два причала и одна линия резерва. Это след маршрута, не доказательство того, что все пассажиры погибли.";
    }
    /** Adapter supplies actual inventory; only a successful action authorizes consumption. */
    public static String act(AdventureData d,String npc,int action,int copper){
        if(options(d,npc).stream().noneMatch(o->o.action()==action))return "";
        switch(action){
            case 30->{d.facts.add("investigation.analysis");d.facts.add("evidence.warning_overlap");return ANALYSIS;}
            case 31->{
                if(copper<4)return "";
                d.facts.add("evidence.device");
                String trace=switch(d.callDecision){case "answer"->"Второй канал повторяет именно твой ответ. Это подтверждает реакцию на обращение, но не личность собеседника.";case "silence"->"Без ответа повторяется одна и та же пауза. Первый слой ведёт себя как запись; о втором мы пока знаем меньше.";default->"В сохранённом фрагменте два слоя: повтор предупреждения и другой несущий сигнал. Мы можем сравнить их отдельно.";};
                d.facts.add("evidence.trace."+d.callDecision);return "Тик: Приёмник восстановлен. "+trace;
            }
            case 32->{d.facts.add("evidence.witness");return "Лисандр: Я видел тот причал позже, уже без корабля. На досках оставались имена ожидавших перехода. Это свидетельство о тех, кого ждали, не список погибших. Не превращай мои слова в то, чего я не видел.";}
            case 33->{d.facts.add("evidence.crosscheck");return "Орен: Метка показывает два причала, приёмник — общий резерв, свидетель — оставшихся ждать. На старом ограничителе якоря есть ручной перевод резерва. Спроси капитана, кто его сделал. Сам якорь намерений не объяснит.";}
            case 34->{d.facts.add("testimony.captain");return CONFESSION;}
            case 35->{
                d.facts.add("truth.public");d.facts.add("truth.shared");
                AdventureEngine.bond(d,"mara","truth.public",8,0,6);AdventureEngine.bond(d,"captain","truth.public",0,0,5);
                AdventureEngine.bond(d,"quartermaster","truth.public",-3,0,2);
                return "Проверенные сведения открыты команде в твоей истории. Мара предлагает сверять маршрут вместе. Брам возражает: разговор нужен был до решения о риске. Капитан больше не остаётся единственным хранителем этой тайны.";
            }
            case 36->{
                d.facts.add("truth.review");d.facts.add("promise.return_to_crew");
                AdventureEngine.bond(d,"quartermaster","truth.review",4,0,4);AdventureEngine.bond(d,"mara","truth.review",-3,0,0);
                return "Ты обязался вернуться к общему разговору после технической проверки. Брам готовит безопасный порядок проверки; Мара не принимает осторожность как бессрочное право молчать. Обещание остаётся в журнале: закончи проверку вместе с Марой.";
            }
            case 37->{
                d.facts.add("truth.mediator");d.facts.add("promise.mediated_talk");AdventureEngine.bond(d,"lysander","truth.mediator",4,5,2);
                return "Лисандр согласен подготовить разговор, но не сочинять оправдание капитану. Пока разговор не проведён, команда ещё не получила проверенные сведения. Вернись к Лисандру, чтобы выполнить обещание.";
            }
            case 38->{
                d.facts.remove("truth.review");d.facts.add("truth.review_completed");d.facts.add("truth.shared");d.facts.remove("promise.return_to_crew");
                AdventureEngine.bond(d,"mara","truth.promise_kept",8,3,5);
                return "Мара отделяет измерения от предположений. Вместе вы сообщаете экипажу проверенные сведения. Обещание выполнено; осторожность не стала новой тайной.";
            }
            case 39->{
                d.facts.remove("truth.mediator");d.facts.add("truth.mediator_completed");d.facts.add("truth.shared");d.facts.remove("promise.mediated_talk");
                AdventureEngine.bond(d,"lysander","truth.promise_kept",4,5,5);
                return "Лисандр начинает не с оправдания, а с известных имён и пробелов в свидетельствах. Ты и капитан отвечаете на вопросы. Обещанный разговор состоялся; согласия со всеми решениями он не гарантирует.";
            }
            default->{return "";}
        }
    }
    public static List<String> journal(AdventureData d){
        var entries=new ArrayList<String>();
        if(started(d))entries.add("УЛИКИ • Предупреждение совпало дословно; это ещё не доказывает присутствие человека.");
        if(d.facts.contains("evidence.water"))entries.add("Маршрутная метка • два причала, одна линия резерва. Не список погибших.");
        if(d.facts.contains("evidence.device"))entries.add("Приёмник • "+switch(d.callDecision){case "answer"->"второй канал повторил твой ответ, личность источника неизвестна.";case "silence"->"пауза первого слоя неизменна; поведение второго пока не установлено.";default->"разделены повтор предупреждения и другой несущий сигнал.";});
        if(d.facts.contains("evidence.witness"))entries.add("Лисандр • видел позднее оставленные на причале имена. Не видел судьбу ожидавших.");
        if(d.facts.contains("evidence.crosscheck"))entries.add("Орен • обнаружен ручной перевод резерва; механизм не объясняет намерений человека.");
        if(d.facts.contains("testimony.captain"))entries.add("Показания капитана • "+CONFESSION);
        return entries;
    }
    public static String objective(AdventureData d){
        if(!d.facts.contains("call.heard"))return "Голос за бортом • послушай сигнал с капитаном";
        if(d.callDecision.isEmpty())return "Голос за бортом • реши, отвечать ли на обращение";
        if(!started(d))return "Расследование • сопоставь услышанное с Марой";
        if(d.facts.contains("truth.shared"))return "Расследование первого спасения завершено. Сведения открыты; судьба оставшихся пока неизвестна.";
        if(d.facts.contains("truth.review"))return "ОБЕЩАНИЕ • вернись к Маре и проведи общий разговор";
        if(d.facts.contains("truth.mediator"))return "ОБЕЩАНИЕ • проведи подготовленный разговор с Лисандром";
        if(!d.facts.contains("evidence.crosscheck"))return "Три следа • "+evidenceCount(d)+"/3: рыбалка "+d.progress.getOrDefault("investigation.water",0)+"/3; приёмник Тика; свидетельство Лисандра"+(evidenceCount(d)==3?" • сверь с Ореном":"");
        if(!d.facts.contains("testimony.captain"))return "Цена первого спасения • задай капитану прямой вопрос";
        return "Цена первого спасения • выбери, как сообщить проверенные сведения экипажу";
    }
    public static String reaction(AdventureData d,String npc){
        if(d.facts.contains("truth.shared"))return switch(npc){case "mara"->"Теперь маршрут будем проверять вместе. И ошибки тоже.";case "quartermaster"->"Правда не заменяет страховку. Перед следующим риском поговорим заранее.";case "captain"->"Ты знаешь, что я сделал. Теперь вправе не соглашаться со мной.";default->"Проверенные сведения больше не остаются тайной мостика.";};
        if(d.facts.contains("truth.review"))return npc.equals("mara")?"Ты обещал вернуться к общему разговору. Я помню.":"Техническая проверка не отменяет обещанного разговора.";
        if(d.facts.contains("truth.mediator"))return "Лисандр готовит общий разговор. Он ещё не состоялся.";
        return "";
    }
}
