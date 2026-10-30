package com.robertx22.age_of_exile.airship;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CrewFloorSearchTest {
    private static CrewFloorSearch.Cell cell(int x,int z){return new CrewFloorSearch.Cell(x,z);}
    @Test void neverCrossesWallsOrMovesDiagonally(){
        var safe=Set.of(cell(0,0),cell(1,0),cell(2,1),cell(2,2));
        var found=CrewFloorSearch.search(cell(0,0),4,81,safe::contains);
        assertEquals(Set.of(cell(0,0),cell(1,0)),found.cells());assertFalse(found.truncated());
    }
    @Test void startsOnlyOnSafeFloorAndStaysInBounds(){
        assertTrue(CrewFloorSearch.search(cell(0,0),4,81,c->false).cells().isEmpty());
        var found=CrewFloorSearch.search(cell(0,0),1,81,c->true);
        assertEquals(9,found.cells().size());assertFalse(found.truncated());
        assertTrue(found.cells().stream().allMatch(c->Math.abs(c.x())<=1&&Math.abs(c.z())<=1));
    }
    @Test void marksLimitWithoutUnsafeOverflow(){
        var found=CrewFloorSearch.search(cell(Integer.MAX_VALUE,0),4,4,c->true);
        assertEquals(4,found.cells().size());assertTrue(found.truncated());
        assertTrue(found.cells().stream().allMatch(c->(long)Integer.MAX_VALUE-c.x()>=0));
        assertThrows(UnsupportedOperationException.class,()->found.cells().add(cell(1,1)));
    }
    @Test void checksTheWholeFootprintIncludingLargeActorsAndNegativeCoordinates(){
        var tiles=Set.of(cell(-2,-2),cell(-2,-1),cell(-1,-2),cell(-1,-1));
        assertTrue(CrewFloorSearch.supported(-1.8,-1.8,-.2,-.2,tiles::contains));
        assertFalse(CrewFloorSearch.supported(-1.8,-1.8,.2,-.2,tiles::contains));
        assertTrue(CrewFloorSearch.supported(-1,-1,0,0,Set.of(cell(-1,-1))::contains));
        assertFalse(CrewFloorSearch.supported(Double.NaN,0,1,1,c->true));
        assertFalse(CrewFloorSearch.supported(0,0,5,1,c->true));
        assertFalse(CrewFloorSearch.supported(0,0,0,1,c->true));
    }
}
