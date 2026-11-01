package com.robertx22.age_of_exile.airship;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrewMigrationSafetyTest {
    @Test void coversEveryChunkIncludingNegativeCoordinatesAndBoundaries(){
        var visited=new HashSet<String>();
        assertTrue(CrewMigrationSafety.allChunksLoaded(-17,-1,16,16,(x,z)->{visited.add(x+","+z);return true;}));
        assertEquals(12,visited.size());
        assertTrue(visited.contains("-2,-1"));assertTrue(visited.contains("1,1"));
    }
    @Test void missingOrInvalidBoundsFailClosed(){
        assertFalse(CrewMigrationSafety.allChunksLoaded(-17,-1,16,16,(x,z)->x!=0||z!=0));
        assertFalse(CrewMigrationSafety.allChunksLoaded(1,0,0,1,(x,z)->true));
        assertFalse(CrewMigrationSafety.allChunksLoaded(0,0,0,0,null));
    }
}
