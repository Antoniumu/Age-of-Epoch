package com.robertx22.age_of_exile.airship;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Bounded cardinal search on one floor. This computes candidates; it never moves an entity. */
public final class CrewFloorSearch {
    private CrewFloorSearch() {}
    public record Cell(int x,int z) {}
    public record Result(Set<Cell> cells,boolean truncated) {}
    public static Result search(Cell origin,int radius,int maximum,Predicate<Cell> safe){
        if(radius<0||maximum<1||!safe.test(origin))return new Result(Set.of(),false);
        var seen=new LinkedHashSet<Cell>();var queue=new ArrayDeque<Cell>();seen.add(origin);queue.add(origin);
        int[][] deltas={{1,0},{-1,0},{0,1},{0,-1}};
        while(!queue.isEmpty()){
            Cell current=queue.removeFirst();
            for(int[] delta:deltas){
                long x=(long)current.x()+delta[0],z=(long)current.z()+delta[1];
                if(x<Integer.MIN_VALUE||x>Integer.MAX_VALUE||z<Integer.MIN_VALUE||z>Integer.MAX_VALUE||
                   Math.abs(x-origin.x())>radius||Math.abs(z-origin.z())>radius)continue;
                var next=new Cell((int)x,(int)z);
                if(seen.contains(next)||!safe.test(next))continue;
                if(seen.size()>=maximum)return new Result(Set.copyOf(seen),true);
                seen.add(next);queue.addLast(next);
            }
        }
        return new Result(Set.copyOf(seen),false);
    }
}
