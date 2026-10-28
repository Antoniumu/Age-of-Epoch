package com.robertx22.age_of_exile.airship.adventure;
import java.util.List;
/** First authored steps only, not a claim that all eight complete personal campaigns exist. */
public final class AdventureCatalog {
    private AdventureCatalog() {}
    public enum Metric { FISH, BIOME, SAPLING, DELIVERY }
    public record Quest(String id,String npc,String title,Metric metric,String item,int target,String objective,String request,String thanks) {}
    public static final List<Quest> QUESTS=List.of(
        new Quest("captain.quiet_water","captain","Тихая вода",Metric.FISH,"",3,"рыбы, пойманные после принятия",
            "Дозор умеет ждать опасность. А я, кажется, разучился ждать без неё. Поймай три рыбы. Когда вернёшься, поговорим не о службе.",
            "Ты не стал торопить воду. Я тридцать лет тороплю людей, будто от этого можно успеть спасти всех. Спасибо, что сегодня не попросил у меня очередной приказ."),
        new Quest("mara.unfinished_map","mara","Незаконченная карта",Metric.BIOME,"",3,"разных биома после принятия",
            "Мне нужны наблюдения из трёх разных биомов. Не идеальная карта — честная. Если местность не совпала с ожиданиями, это тоже результат.",
            "Когда-то я стёрла ошибку с карты, прежде чем признаться в ней людям. Теперь оставляю исправления видимыми. С твоей помощью здесь будет не красивая ложь, а дорога."),
        new Quest("bram.shared_table","quartermaster","Не только припасы",Metric.DELIVERY,"minecraft:bread",8,"хлеба для общего стола",
            "Восемь хлебов для общего стола. Да, мог бы выдать сухой паёк. Но люди должны иногда есть вместе, а не по очереди между тревогами.",
            "Раньше я записывал только, сколько порций осталось. Сегодня запишу, кто пришёл к столу. Это разные списки. Спасибо."),
        new Quest("oren.new_roots","oren","Новые корни",Metric.SAPLING,"",4,"посаженных саженца после принятия",
            "Четыре саженца. Посади их там, где смогут вырасти. Большое дерево начинается тихо. Мне нравится, что не всё полезное надо делать силой.",
            "Ты оставил после себя то, что будет расти. Я всё думаю, что мои руки годятся только удерживать тяжёлое. Может, я просто давно не начинал с малого.")
    );
    public static Quest find(String id){return QUESTS.stream().filter(q->q.id.equals(id)).findFirst().orElse(null);}
    public static Quest personal(String npc){return QUESTS.stream().filter(q->q.npc.equals(npc)).findFirst().orElse(null);}
}
