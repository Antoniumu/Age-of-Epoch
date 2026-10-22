package com.robertx22.age_of_exile.questbook;

import java.util.HashSet;

/** Persistent per-character progress for the in-game quest book. */
public class QuestBookData {

    public int mobKills = 0;
    public int chestsLooted = 0;
    public int delvesCompleted = 0;
    public int standardDungeonsCompleted = 0;
    public int infiniteDungeonsCompleted = 0;

    public HashSet<String> completedQuests = new HashSet<>();
    public HashSet<String> manualTasks = new HashSet<>();
    public HashSet<String> professionsUsed = new HashSet<>();
    public HashSet<String> gemVariants = new HashSet<>();
    public HashSet<String> runes = new HashSet<>();
    public HashSet<String> runewords = new HashSet<>();
    public HashSet<String> uniqueGears = new HashSet<>();
    public HashSet<String> missionGods = new HashSet<>();
    public HashSet<String> infiniteDungeonLayouts = new HashSet<>();
    public HashSet<String> towerBosses = new HashSet<>();
    public HashSet<String> towerThemes = new HashSet<>();
    public HashSet<String> simulationGuides = new HashSet<>();

    public boolean platinumMode = false;
    public boolean platinumAwarded = false;
}
