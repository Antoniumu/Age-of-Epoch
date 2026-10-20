package com.robertx22.divine_missions.mission_gen;

import java.util.HashMap;

public class WorthsData {

    public int totalReduced = 0;

    public HashMap<String, Integer> map = new HashMap<>();

    public int getWorth(String id) {
        return map.getOrDefault(id, 0);
    }

    public void reduce(String id, Integer worth) {
        totalReduced += worth;
        map.put(id, map.getOrDefault(id, 0) - worth);
    }

    public void add(String id, Integer worth) {
        map.put(id, map.getOrDefault(id, 0) + worth);
    }

    public void add(HashMap<String, Integer> worths, int count, float multi) {
        worths.entrySet()
            .forEach(x -> {
                add(x.getKey(), (int) (x.getValue() * multi * count));
            });
    }
}
