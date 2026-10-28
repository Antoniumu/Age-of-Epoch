package com.robertx22.age_of_exile.airship.adventure;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import java.util.HashMap;
import java.util.HashSet;
/** Character-owned facts; no world coordinates and no client-reported completion. */
@Storable
public class AdventureData {
    @Store public int version=0;
    @Store public HashSet<String> accepted=new HashSet<>(),completed=new HashSet<>(),facts=new HashSet<>(),events=new HashSet<>();
    @Store public HashMap<String,Integer> progress=new HashMap<>(),trust=new HashMap<>(),closeness=new HashMap<>(),respect=new HashMap<>();
    @Store public String callDecision="",lastInvestigationLine="";
}
