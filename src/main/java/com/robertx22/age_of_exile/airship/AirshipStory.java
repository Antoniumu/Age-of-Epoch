package com.robertx22.age_of_exile.airship;

/** Shared, Minecraft-independent first chapter. IDs are stable save/network keys. */
public final class AirshipStory {
    private AirshipStory() {}
    public static final String[] IDS = {"captain", "mara", "quartermaster"};
    public static int index(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }
    public static String name(String id) {
        return switch (id) {
            case "captain" -> "Капитан • Последний Рассвет";
            case "mara" -> "Мара • Картограф";
            case "quartermaster" -> "Брам • Квартирмейстер";
            default -> "Неизвестный";
        };
    }
    public static String owner(int stage) { return stage == 2 ? "mara" : "captain"; }
    public static String objective(int stage) {
        return switch (stage) {
            case 1 -> "Найди Мару у картографического стола и поговори с ней.";
            case 2 -> "Поговори с Брамом в кормовой части корабля.";
            case 3 -> "Вернись к капитану и доложи о готовности.";
            case 4 -> "Первое поручение выполнено. Ты принят на борт.";
            default -> "Поднимись на дирижабль и поговори с капитаном.";
        };
    }
    public static String dialogue(String id, int stage) {
        return switch (id) {
            case "captain" -> stage == 0
                ? "Я капитан «Последнего Рассвета». Если ищешь убежище — поднимайся. Если ответы — придётся поработать. Найди Мару у картографического стола. Скажи, что пришёл снизу. Она поймёт."
                : stage == 3 ? "Мара показала тебе маршрут, Брам объяснил правила. Хорошо. На этом корабле мы держимся не за прошлое, а друг за друга. Добро пожаловать в команду."
                : stage == 4 ? "Теперь ты один из нас. Осмотрись. Когда придёт время следующего перехода, я тебя позову."
                : "Сначала познакомься с командой. Мара у картографического стола, Брам — на корме. Я буду здесь.";
            case "mara" -> stage == 1
                ? "Капитан прислал? Значит, решил дать тебе шанс. Эти линии на карте — не границы королевств. Это шрамы. Мы ищем путь между ними. Но прежде загляни к Браму. Без его разрешения с палубы не сходят."
                : "Карты меняются после каждого разлома. Если увидишь на земле что-то, чего здесь нет, запомни дорогу назад.";
            case "quartermaster" -> stage == 2
                ? "Новенький? Слушай внимательно. Два удара колокола — возвращайся на борт. При переходе держись подальше от борта. А теперь доложи капитану. Он ждёт."
                : "Припасы любят счёт. Люди — порядок. Держись этих двух правил и на корабле приживёшься.";
            default -> "";
        };
    }
    public static int advance(String id, int stage) {
        if (stage == 0 && id.equals("captain")) return 1;
        if (stage == 1 && id.equals("mara")) return 2;
        if (stage == 2 && id.equals("quartermaster")) return 3;
        if (stage == 3 && id.equals("captain")) return 4;
        return stage;
    }
}
