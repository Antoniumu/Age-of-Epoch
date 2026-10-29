package com.robertx22.age_of_exile.airship;

/** Three authored chapters. Stable stages preserve the original 0..4 introduction save data. */
public final class AirshipStory {
    private AirshipStory() {}
    public static final String[] IDS={"captain","mara","quartermaster","oren","veya","selin","lysander","tik"};
    public static boolean hasFavor(String id) { int i=index(id);return i>=0 && i<3; }
    public static final int LAST_STAGE=11;
    public enum Goal { NONE, CHESTS, KILLS, DUNGEON, COMPLETE }
    public static int index(String id) { for(int i=0;i<IDS.length;i++) if(IDS[i].equals(id)) return i;return -1; }
    public static String name(String id) { return switch(id) {
        case "captain"->"Капитан • Последний Рассвет";case "mara"->"Мара • Картограф";
        case "quartermaster"->"Брам • Квартирмейстер";
        case "oren"->"Орен Тихий • Хранитель якоря";case "veya"->"Вея «Искра» • Разведчица";
        case "selin"->"Селин Валь • Целительница";case "lysander"->"Лисандр Рэй • Хранитель историй";case "tik"->"Тик • Старший помощник";default->"Неизвестный";}; }
    public static String actor(int stage) { return switch(stage) {
        case 0,3,4,7,10->"captain";case 1,5,8->"mara";case 2,6,9->"quartermaster";default->"";}; }
    public static String owner(int stage) { return switch(stage) {
        case 2,6,9->"mara";case 7,10->"quartermaster";default->"captain";}; }
    public static String chapter(int stage) { return stage<=3?"I • Люди над бездной":stage<=7?"II • Эхо путевого колокола":"III • Карта завтрашнего дня"; }
    public static String objective(int stage) { return switch(stage) {
        case 1->"Поговори с Марой у картографического стола.";
        case 2->"Познакомься с Брамом на нижней палубе.";
        case 3->"Вернись к капитану и расскажи о знакомстве.";
        case 4->"Поговори с капитаном о пропавшем путевом колоколе.";
        case 5->"Открой 3 новых RPG-сундука с добычей и вернись к Маре.";
        case 6->"Победи 10 противников после поручения и доложи Браму.";
        case 7->"Вернись к капитану и обсуди, кому рассказать о находках.";
        case 8->"Обсуди с Марой путь к сердцу разлома.";
        case 9->"Заверши 1 обычное подземелье мода и доложи Браму.";
        case 10->"Вернись к капитану и обсуди поиски колокола.";
        case 11->"Первые поручения выполнены. Поговори с экипажем о дальнейших делах.";
        default->"Поднимись на борт и поговори с капитаном.";}; }
    public static Goal goal(int stage) { return switch(stage) {case 5->Goal.CHESTS;case 6->Goal.KILLS;case 9->Goal.DUNGEON;case 11->Goal.COMPLETE;default->Goal.NONE;}; }
    public static int target(int stage) { return switch(stage) {case 5->3;case 6->10;case 9->1;default->0;}; }
    public static int advance(String id,int stage) { return stage>=0 && stage<LAST_STAGE && actor(stage).equals(id)?stage+1:stage; }
    public static String dialogue(String id,int stage) {
        if(id.equals("captain")) return String.join(" ",AirshipVoiceScript.cues(stage).stream().map(AirshipVoiceScript.Cue::text).toList());
        if(id.equals("mara")) return switch(stage) {
            case 1->"Ты с земли? Я Мара, отвечаю за карты. Капитан говорил, что заметил сигнал. Мы ищем дорогу после последнего перехода, а эти линии показывают разломы между эпохами. Если хочешь остаться, загляни к Браму на нижнюю палубу. Он расскажет, что нужно знать перед вылазкой.";
            case 5->"Три сундука, хорошо. Я попробую сопоставить находки с тем, что нам известно о колоколе. А ты поговори с Брамом: он просил помочь расчистить дорогу от противников.";
            case 8->"Про разлом мы до сих пор многого не знаем. Браму нужен след твоей силы для настройки якоря. Пройди обычное подземелье, а потом поговори с ним.";
            default->"Если найдёшь новый тайник, расскажи мне, где он был. Я отмечу его на карте. Только не удивляйся, если попрошу описать дорогу дважды.";};
        if(id.equals("quartermaster")) return switch(stage) {
            case 2->"Новенький? Я Брам, слежу за припасами. Запомни: два удара колокола — возвращайся на борт. Добычу сразу не трать: сначала проверь уровень вещи, свойства и подходит ли она твоей сборке. Теперь доложи капитану, что мы познакомились.";
            case 6->"Десять противников. Хорошая работа, теперь дорога спокойнее. Мара продолжает разбираться с находками из тайников. Поговори с капитаном: ему нужно решить, что рассказать команде.";
            case 9->"Вернулся из подземелья? Хорошо. Якорь принял твой след, настройка готова. Иди к капитану, он ждёт.";
            default->"Для ремонта нужны шестнадцать железных слитков. Если принесёшь, займусь креплениями на нижней палубе. Там давно пора всё проверить.";};
        return AirshipCrewVoice.dialogue(id);
    }
    public static String reputationLabel(int value) { return value>=40?"Свой человек":value>=20?"Надёжный товарищ":value>=10?"Доверие":value>0?"Знакомство":"Чужак"; }
    public static String favor(String id) { return switch(id) {case "captain"->"Победи 5 новых противников для дозора.";case "mara"->"Открой 2 новых RPG-сундука с добычей.";default->"Принеси 16 железных слитков для ремонта.";}; }
}
