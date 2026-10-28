package com.robertx22.age_of_exile.airship;
import java.util.List;

/** One subtitle cue per independently generated audio clip: no guessed word timestamps. */
public final class AirshipVoiceScript {
    private AirshipVoiceScript() {}
    public record Cue(String sound,String text,int durationMillis) {}
    public static List<Cue> cues(int stage) { return switch(stage) {
        case 0->List.of(
            new Cue("captain_intro_a","Я капитан «Последнего Рассвета». Тридцать лет я веду этот корабль сквозь разломы. Если ищешь убежище — поднимайся. Если ответы — придётся поработать.",11680),
            new Cue("captain_intro_b","Найди Мару у картографического стола. Скажи, что пришёл снизу. И запомни: если ночью услышишь знакомый голос за бортом — не отвечай.",11420));
        case 3->List.of(new Cue("captain_welcome","Значит, с командой познакомился. Хорошо. Мы держимся не за прошлое, а друг за друга. Добро пожаловать на «Последний Рассвет».",9590));
        case 4->List.of(new Cue("captain_echoes","В последнем переходе мы потеряли путевой колокол. Его эхо осталось в старых тайниках. Мара умеет читать такие следы. Помоги ей — и мы найдём дорогу домой.",11200));
        case 7->List.of(new Cue("captain_choice","Ты слышал этот зов и всё-таки вернулся. Теперь решай: рассказать команде всё, что узнал, или оставить опасное знание на мостике. Я выслушаю тебя.",12500));
        case 10->List.of(new Cue("captain_return","Колокол отозвался. Но дорогу открыл не он. Её открыли твои поступки. Теперь на картах Мары есть место, которого вчера ещё не существовало.",13080));
        case 11->List.of(new Cue("captain_finale","Мы не вернём мир таким, каким его помним. Зато можем построить новый. Пока этот корабль держится в небе, у нас есть завтрашний день. И теперь он есть у тебя.",12530));
        default->List.of(new Cue("captain_idle","Сначала закончи дело, за которое взялся. Доверие команды не покупают красивыми словами. Его собирают по крупицам, когда никто не смотрит.",13640));}; }
}
