package com.robertx22.age_of_exile.airship;

/** Three authored chapters. Stable stages preserve the original 0..4 introduction save data. */
public final class AirshipStory {
    private AirshipStory() {}
    public static final String[] IDS={"captain","mara","quartermaster"};
    public static final int LAST_STAGE=11;
    public enum Goal { NONE, CHESTS, KILLS, DUNGEON, COMPLETE }
    public static int index(String id) { for(int i=0;i<IDS.length;i++) if(IDS[i].equals(id)) return i;return -1; }
    public static String name(String id) { return switch(id) {
        case "captain"->"Капитан • Последний Рассвет";case "mara"->"Мара • Картограф";
        case "quartermaster"->"Брам • Квартирмейстер";default->"Неизвестный";}; }
    public static String actor(int stage) { return switch(stage) {
        case 0,3,4,7,10->"captain";case 1,5,8->"mara";case 2,6,9->"quartermaster";default->"";}; }
    public static String owner(int stage) { return switch(stage) {
        case 2,6,9->"mara";case 7,10->"quartermaster";default->"captain";}; }
    public static String chapter(int stage) { return stage<=3?"I • Люди над бездной":stage<=7?"II • Эхо путевого колокола":"III • Карта завтрашнего дня"; }
    public static String objective(int stage) { return switch(stage) {
        case 1->"Поговори с Марой у картографического стола.";
        case 2->"Познакомься с Брамом на нижней палубе.";
        case 3->"Вернись к капитану: команда ждёт твоего решения.";
        case 4->"Поговори с капитаном о пропавшем путевом колоколе.";
        case 5->"Открой 3 новых RPG-сундука с добычей и вернись к Маре.";
        case 6->"Победи 10 противников после поручения и доложи Браму.";
        case 7->"Вернись к капитану и реши, кому доверить правду о зове.";
        case 8->"Обсуди с Марой путь к сердцу разлома.";
        case 9->"Заверши 1 обычное подземелье мода и доложи Браму.";
        case 10->"Вернись к капитану. Путевой колокол снова звучит.";
        case 11->"История первого перехода завершена. Помоги экипажу заслужить общее завтра.";
        default->"Поднимись на борт и поговори с капитаном.";}; }
    public static Goal goal(int stage) { return switch(stage) {case 5->Goal.CHESTS;case 6->Goal.KILLS;case 9->Goal.DUNGEON;case 11->Goal.COMPLETE;default->Goal.NONE;}; }
    public static int target(int stage) { return switch(stage) {case 5->3;case 6->10;case 9->1;default->0;}; }
    public static int advance(String id,int stage) { return stage>=0 && stage<LAST_STAGE && actor(stage).equals(id)?stage+1:stage; }
    public static String dialogue(String id,int stage) {
        if(id.equals("captain")) return String.join(" ",AirshipVoiceScript.cues(stage).stream().map(AirshipVoiceScript.Cue::text).toList());
        if(id.equals("mara")) return switch(stage) {
            case 1->"Капитан прислал? Значит, дал тебе шанс. Эти линии — не границы королевств. Это шрамы между эпохами. Но карту не прочитать в одиночку. Загляни к Браму. Пусть объяснит, как возвращаться живым.";
            case 5->"Вот они, три отголоска. Колокол не исчез: его звон рассыпался по тайникам старого мира. Каждый найденный сундук вернул нам кусочек пути. Теперь Браму нужна безопасная тропа. Помоги расчистить её от противников.";
            case 8->"За разломом нет чудовища, которое всё исправит своим поражением. Там память о нашей первой ошибке. Пройди обычное подземелье: его сердце оставит след на твоей силе. Брам сумеет настроить по нему якорь.";
            default->"Я доверяю не красивым картам, а тем, кто возвращается и исправляет неточности. Если хочешь помочь лично мне, найди новые тайники: у каждого своя память.";};
        if(id.equals("quartermaster")) return switch(stage) {
            case 2->"Новенький? Два удара колокола — возвращайся на борт. Не трать всю добычу на первую красивую вещь. Проверяй уровень, свойства и свою сборку. А теперь доложи капитану: он ждёт не героя, а человека слова.";
            case 6->"Десять угроз меньше. Неплохое начало. Убийства и добыча — не цель нашей дороги, но без них корабль не выживет. Я слышал зов вместе с тобой. Расскажи капитану правду. Он должен знать, что мы не одни.";
            case 9->"Ты вернулся из подземелья. Вещи можно потерять, но этот опыт уже твой. Якорь принял твой след. Иди к капитану. Сегодня мы идём не на знакомый голос — на собственный свет.";
            default->"Припасы любят счёт, люди — порядок. Нужно личное поручение? Мне нужны шестнадцать железных слитков. Не для блеска на борту — для ремонта тех мест, о которых пассажиры не спрашивают.";};
        return "";
    }
    public static String reputationLabel(int value) { return value>=40?"Свой человек":value>=20?"Надёжный товарищ":value>=10?"Доверие":value>0?"Знакомство":"Чужак"; }
    public static String favor(String id) { return switch(id) {case "captain"->"Победи 5 новых противников для дозора.";case "mara"->"Открой 2 новых RPG-сундука с добычей.";default->"Принеси 16 железных слитков для ремонта.";}; }
}
