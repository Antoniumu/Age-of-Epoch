package com.robertx22.age_of_exile.airship;

import java.util.function.BiPredicate;

/** Bounded, read-only check: never assume an unloaded chunk contains no crew. */
final class CrewMigrationSafety {
    private CrewMigrationSafety() {}
    static boolean allChunksLoaded(int minX,int minZ,int maxX,int maxZ,BiPredicate<Integer,Integer> loaded){
        if(minX>maxX||minZ>maxZ||loaded==null)return false;
        int firstX=Math.floorDiv(minX,16),lastX=Math.floorDiv(maxX,16);
        int firstZ=Math.floorDiv(minZ,16),lastZ=Math.floorDiv(maxZ,16);
        for(int x=firstX;x<=lastX;x++)for(int z=firstZ;z<=lastZ;z++)if(!loaded.test(x,z))return false;
        return true;
    }
}
