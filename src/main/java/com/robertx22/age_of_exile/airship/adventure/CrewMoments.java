package com.robertx22.age_of_exile.airship.adventure;

/** Small, private, one-time character moments. Neither answer represents a correct way to be a friend. */
public final class CrewMoments {
    private CrewMoments() {}
    public record Moment(String prompt,String first,String second,String firstReply,String secondReply) {}
    public static Moment moment(String npc){return switch(npc){
        case "captain"->new Moment("Капитан перебирает снасти. «В следующий раз у воды посидим молча или поговорим? Я и так, и так могу».","Посидеть молча","Поговорить у воды","Капитан: Договорились. Удочку возьму запасную, если захочешь попробовать.","Капитан: Договорились. Только не спрашивай, сколько рыбы я упустил в прошлый раз.");
        case "mara"->new Moment("Мара показывает незаконченный угол карты. «Здесь пока пусто. Сначала отметим тропу или места для отдыха?»","Отметить тропу","Отметить места отдыха","Мара: Тропу — так тропу. Возьму тонкое перо, здесь узко.","Мара: Хорошо, начнём с привалов. Брам оценит, если рядом найдётся вода.");
        case "quartermaster"->new Moment("Брам раскладывает посуду. «Когда соберёмся за столом, хлеб подогреть или оставить свежим?»","Подогреть хлеб","Оставить свежим","Брам: Подогреем. Только проследи за печью, пока я позову остальных.","Брам: Свежим тоже хорошо. Тогда достану масло, если Тик его ещё не нашёл.");
        case "oren"->new Moment("Орен держит деревянную заготовку. «Я хочу сделать Тику фигурку. Как думаешь, птичку или маленький корабль?»","Предложить птичку","Предложить корабль","Птичку? Хорошо. А крылья попробую сделать подвижными. Надеюсь, Тику понравится.","Корабль? Хорошо. Придётся сделать его маленьким, чтобы Тик мог держать его в лапках.");
        case "veya"->new Moment("Вея раскладывает два обрывка карты. «Когда пойдём смотреть местность, тебе интереснее лес или берег?»","Выбрать лес","Выбрать берег","Вея: Лес, значит. Если заблудимся, скажем Маре, что изучали деревья.","Вея: Берег? Отлично. Только учти: я всегда проверяю, что лежит за следующим поворотом.");
        case "selin"->new Moment("Селин заглядывает в кастрюлю. «Поможешь с ужином? Попробуй и скажи: добавить соли или оставить как есть?»","Добавить соли","Оставить как есть","Селин: Совсем немного? Ладно, доверюсь тебе. В прошлый раз у меня получилась морская вода.","Селин: Оставим. Приятно, когда кто-то останавливает меня до того, как я всё пересолю.");
        case "lysander"->new Moment("Лисандр закрывает тетрадь. «Если ты расскажешь мне историю, можно будет её записать? Или лучше оставить только между нами?»","Можно записать","Оставить между нами","Лисандр: Запишу, если захочешь рассказать. И покажу тебе прежде, чем кто-то другой прочтёт.","Лисандр: Хорошо. Тетрадь останется закрытой. Можем поговорить без неё.");
        case "tik"->new Moment("Тик раскладывает инструменты. «Я хотел сделать ящик получше. Нарисуем на крышке ключ или шестерёнку?»","Нарисовать ключ","Нарисовать шестерёнку","Ключ! А внутри будет место для запасного. Орену пока не показывай, это сюрприз.","Шестерёнку! Сделаю побольше, чтобы никто не перепутал мой ящик с обычным.");
        default->null;};}
    private static String prefix(String npc){return "crew_moment:"+npc+":";}
    public static int chosen(AdventureData d,String npc){
        if(moment(npc)==null)return -1;
        return d.facts.contains(prefix(npc)+"0")?0:d.facts.contains(prefix(npc)+"1")?1:-1;
    }
    public static boolean available(AdventureData d,String npc){
        var q=AdventureCatalog.personal(npc);
        return q!=null && moment(npc)!=null && d.completed.contains(q.id()) && d.facts.contains("memory:"+npc+":confidence") && chosen(d,npc)<0;
    }
    public static String choose(AdventureData d,String npc,int choice){
        if((choice!=0&&choice!=1)||!available(d,npc))return "";
        d.facts.add(prefix(npc)+choice);
        AdventureEngine.bond(d,npc,"crew_moment",5,10,5);
        var m=moment(npc);return choice==0?m.firstReply():m.secondReply();
    }
    /** Recordings exist for both choices of Oren and Tik; other characters remain text-only. */
    public static String voiceKey(AdventureData d,String npc,int action){
        if(action!=90&&action!=91)return "";
        int choice=action-90;if(chosen(d,npc)!=choice)return "";
        return switch(npc){
            case "oren"->choice==0?"oren_moment_bird":"oren_moment_ship";
            case "tik"->choice==0?"tik_moment_key":"tik_moment_gear";
            default->"";
        };
    }
    /** Persisted callback, shown on later visits, not a repeated reward. */
    public static String remembered(AdventureData d,String npc){
        int c=chosen(d,npc);if(c<0)return "";
        var m=moment(npc);return c==0?m.firstReply():m.secondReply();
    }
}
