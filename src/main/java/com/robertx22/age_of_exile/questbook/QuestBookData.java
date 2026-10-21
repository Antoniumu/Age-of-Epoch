package com.robertx22.age_of_exile.questbook;

import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

import java.util.HashSet;

/** Persistent per-character progress for the in-game quest book. */
@Storable
public class QuestBookData {

    @Store
    public int mobKills = 0;
    @Store
    public int chestsLooted = 0;
    @Store
    public int standardDungeonsCompleted = 0;
    @Store
    public int infiniteDungeonsCompleted = 0;

    @Store
    public HashSet<String> manualTasks = new HashSet<>();
    @Store
    public HashSet<String> professionsUsed = new HashSet<>();
    @Store
    public HashSet<String> gemVariants = new HashSet<>();
    @Store
    public HashSet<String> runes = new HashSet<>();
    @Store
    public HashSet<String> runewords = new HashSet<>();
    @Store
    public HashSet<String> uniqueGears = new HashSet<>();
    @Store
    public HashSet<String> missionGods = new HashSet<>();
    @Store
    public HashSet<String> infiniteDungeonLayouts = new HashSet<>();
    @Store
    public HashSet<String> towerBosses = new HashSet<>();
    @Store
    public HashSet<String> towerThemes = new HashSet<>();
    @Store
    public HashSet<String> simulationGuides = new HashSet<>();

    @Store
    public boolean platinumMode = false;
    @Store
    public boolean platinumAwarded = false;
}
