package com.robertx22.age_of_exile.airship.adventure;

import java.util.List;

/** Player-owned conversation about a shared crew relationship, not a simulated NPC meeting. */
public final class CrewWorkshop {
    private CrewWorkshop() {}
    private static final String START="workshop:asked", KEY="workshop:talk_key", FIGURE="workshop:talk_figure",
        CONSENT="workshop:consent", PRIVATE="workshop:private", OREN="workshop:oren_informed", DONE="workshop:finished";
    public static final String OREN_START="Я всё ещё хочу сделать Тику фигурку. Но он в последнее время какой-то напряжённый. Если будешь с ним говорить, спроси, всё ли в порядке. Только не заставляй рассказывать, ладно?";
    public static final String TIK_KEY="Ключ? Я немного погнул защёлку, когда искал его. Орен ещё не знает. Я скажу ему сам, только сначала соберусь с мыслями. Не надо говорить за меня.";
    public static final String TIK_FIGURE="Фигурку для меня? Вот это да. Слушай, я ещё защёлку погнул, пока искал ключ. Хочу сам сказать об этом Орену. Только дай мне немного времени.";
    public static final String OREN_REPLY="Понятно. Не буду торопить Тика. Пусть подойдёт, когда будет готов, а там вместе посмотрим на защёлку. Спасибо, что предупредил.";
    public record Option(int action,String label) {}
    private static boolean spoke(AdventureData d){return d.facts.contains(KEY)||d.facts.contains(FIGURE);}
    public static boolean eligible(AdventureData d){
        var tik=AdventureCatalog.personal("tik");
        return CrewMoments.chosen(d,"oren")>=0 && tik!=null && d.completed.contains(tik.id());
    }
    public static List<Option> options(AdventureData d,String npc){
        if(!eligible(d)||d.facts.contains(DONE))return List.of();
        if(!d.facts.contains(START))return npc.equals("oren")?List.of(new Option(92,"Спросить Орена о фигурке для Тика")):List.of();
        if(!spoke(d))return npc.equals("tik")?List.of(new Option(93,"Спросить, как дела с ключом"),new Option(94,"Сначала упомянуть фигурку")):List.of();
        if(!d.facts.contains(CONSENT))return npc.equals("tik")?List.of(new Option(96,"Спросить, можно ли предупредить Орена"),new Option(95,"Ничего не передавать без просьбы")):List.of();
        if(!d.facts.contains(OREN))return npc.equals("oren")?List.of(new Option(97,"Передать то, на что Тик согласился")):List.of();
        if(!d.facts.contains(DONE))return npc.equals("tik")?List.of(new Option(98,"Сказать Тику, что Орен готов выслушать")):List.of();
        return List.of();
    }
    public static String act(AdventureData d,String npc,int action){
        if(options(d,npc).stream().noneMatch(o->o.action()==action))return "";
        return switch(action){
            case 92->{d.facts.add(START);yield OREN_START;}
            case 93->{d.facts.add(KEY);yield TIK_KEY;}
            case 94->{d.facts.add(FIGURE);yield TIK_FIGURE;}
            case 95->{d.facts.add(PRIVATE);d.facts.add(DONE);AdventureEngine.bond(d,"tik","workshop.private",3,5,3);
                yield "Тик: Спасибо. Я сам поговорю с Ореном, когда буду готов. Только не рассказывай пока про защёлку, ладно?";}
            case 96->{d.facts.add(CONSENT);yield "Тик: Можешь сказать ему про защёлку и что я сам подойду. Остальное я объясню сам.";}
            case 97->{d.facts.add(OREN);yield OREN_REPLY;}
            case 98->{d.facts.add(DONE);AdventureEngine.bond(d,"oren","workshop",3,5,3);AdventureEngine.bond(d,"tik","workshop",3,5,3);
                yield "Тик: Спасибо, что спросил меня сначала. Я поговорю с Ореном сам. И про фигурку потом тоже спрошу.";}
            default->"";
        };
    }
    public static String objective(AdventureData d){
        if(CrewMoments.chosen(d,"oren")<0)return "";
        if(!eligible(d))return "Орен и Тик • выполни личное поручение Тика, затем вернись к Орену.";
        if(d.facts.contains(PRIVATE))return "Орен и Тик • ты оставил разговор о защёлке Тику. Он сам решит, когда подойти к Орену.";
        if(d.facts.contains(DONE))return "Орен и Тик • Тик собирается сам поговорить с Ореном. Совместная встреча ещё впереди.";
        if(!d.facts.contains(START))return "Орен и Тик • спроси Орена о фигурке.";
        if(!spoke(d)||!d.facts.contains(CONSENT))return "Орен и Тик • поговори с Тиком и спроси, что можно передать.";
        if(!d.facts.contains(OREN))return "Орен и Тик • передай Орену только то, на что Тик согласился.";
        return "Орен и Тик • расскажи Тику об ответе Орена.";
    }
    public static String voiceKey(int action){return switch(action){case 92->"oren_workshop_start";case 93->"tik_workshop_key";case 94->"tik_workshop_figure";case 97->"oren_workshop_reply";default->"";};}
    public static String voiceText(int action){return switch(action){case 92->OREN_START;case 93->TIK_KEY;case 94->TIK_FIGURE;case 97->OREN_REPLY;default->"";};}
}
