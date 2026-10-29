package com.robertx22.age_of_exile.airship;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Conservative bounded shortest path on a single horizontal plane.
 * This is a planner only: the caller must revalidate the actual actor, footprint,
 * support, other entities and swept collision as the world changes. */
public final class CrewRoutePlanner {
    private CrewRoutePlanner() {}
    public record Cell(int x,int z) {}
    public record Result(List<Cell> path,boolean truncated) {
        public Result { path=List.copyOf(path); }
    }
    private static final int[][] CARDINAL={{1,0},{-1,0},{0,1},{0,-1}};
    public static Result find(Cell start,Cell goal,int radius,int limit,Predicate<Cell> safe,BiPredicate<Cell,Cell> edgeSafe){
        if(start==null||goal==null||safe==null||edgeSafe==null||radius<0||limit<1||!safe.test(start)||!safe.test(goal))return new Result(List.of(),false);
        var queue=new ArrayDeque<Cell>();var previous=new HashMap<Cell,Cell>();
        queue.add(start);previous.put(start,start);
        while(!queue.isEmpty()){
            Cell current=queue.removeFirst();
            if(current.equals(goal)){
                var path=new ArrayList<Cell>();
                while(!current.equals(start)){path.add(current);current=previous.get(current);}
                path.add(start);Collections.reverse(path);return new Result(path,false);
            }
            for(int[] delta:CARDINAL){
                long x=(long)current.x()+delta[0],z=(long)current.z()+delta[1];
                if(x<Integer.MIN_VALUE||x>Integer.MAX_VALUE||z<Integer.MIN_VALUE||z>Integer.MAX_VALUE||
                   Math.abs(x-(long)start.x())>radius||Math.abs(z-(long)start.z())>radius)continue;
                var next=new Cell((int)x,(int)z);
                if(previous.containsKey(next)||!safe.test(next)||!edgeSafe.test(current,next))continue;
                if(previous.size()>=limit)return new Result(List.of(),true);
                previous.put(next,current);queue.addLast(next);
            }
        }
        return new Result(List.of(),false);
    }
}
