package com.robertx22.age_of_exile.airship;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrewRoutePlannerTest {
    private static CrewRoutePlanner.Cell c(int x,int z){return new CrewRoutePlanner.Cell(x,z);}
    @Test void shortestCardinalPathAvoidsObstaclesAndSweptCollision(){
        var floor=Set.of(c(0,0),c(1,0),c(0,1),c(1,1),c(2,1),c(2,0));
        var r=CrewRoutePlanner.find(c(0,0),c(2,0),3,30,floor::contains,(a,b)->!a.equals(c(1,0))||!b.equals(c(2,0)));
        assertEquals(List.of(c(0,0),c(1,0),c(1,1),c(2,1),c(2,0)),r.path());
        assertFalse(r.truncated());assertThrows(UnsupportedOperationException.class,()->r.path().clear());
    }
    @Test void noDiagonalOrUnsafeStartAndGoal(){
        var floor=Set.of(c(0,0),c(1,1));
        assertTrue(CrewRoutePlanner.find(c(0,0),c(1,1),3,20,floor::contains,(a,b)->true).path().isEmpty());
        assertTrue(CrewRoutePlanner.find(c(0,0),c(3,3),3,20,floor::contains,(a,b)->true).path().isEmpty());
        assertEquals(List.of(c(0,0)),CrewRoutePlanner.find(c(0,0),c(0,0),0,1,floor::contains,(a,b)->true).path());
    }
    @Test void exhaustionAndBoundsNeverMasqueradeAsRoutes(){
        var r=CrewRoutePlanner.find(c(0,0),c(4,0),4,3,x->true,(a,b)->true);
        assertTrue(r.truncated());assertTrue(r.path().isEmpty());
        var boundary=CrewRoutePlanner.find(c(Integer.MAX_VALUE,0),c(Integer.MIN_VALUE,0),3,100,x->true,(a,b)->true);
        assertFalse(boundary.truncated());assertTrue(boundary.path().isEmpty());
    }
}
