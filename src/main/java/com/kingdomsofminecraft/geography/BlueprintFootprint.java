package com.kingdomsofminecraft.geography;

/** Inclusive, unrotated blueprint bounds. Limits cap synchronous read-only work. */
public record BlueprintFootprint(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
    public BlueprintFootprint {
        long x=(long)maxX-minX+1,y=(long)maxY-minY+1,z=(long)maxZ-minZ+1;
        if(x<1 || y<1 || z<1 || x>64 || y>64 || z>64 || x*y*z>65536 || minY==Integer.MIN_VALUE)
            throw new IllegalArgumentException("Blueprint exceeds the 64-block axis / 65,536-block preflight budget");
    }
    public static BlueprintFootprint at(int x,int y,int z,int offsetX,int offsetY,int offsetZ,int anchorX,int anchorY,int anchorZ) {
        if(x<1 || y<1 || z<1 || x>64 || y>64 || z>64 || (long)x*y*z>65536
                || offsetX<0 || offsetY<0 || offsetZ<0 || offsetX>=x || offsetY>=y || offsetZ>=z)
            throw new IllegalArgumentException("Invalid or oversized blueprint dimensions/anchor");
        try {
            int lowX=Math.subtractExact(anchorX,offsetX),lowY=Math.subtractExact(anchorY,offsetY),lowZ=Math.subtractExact(anchorZ,offsetZ);
            return new BlueprintFootprint(lowX,lowY,lowZ,Math.addExact(lowX,x-1),Math.addExact(lowY,y-1),Math.addExact(lowZ,z-1));
        } catch(ArithmeticException error){throw new IllegalArgumentException("Blueprint coordinates overflow",error);}
    }
    public int volume(){return (maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);}
    public int minChunkX(){return Math.floorDiv(minX,16);}
    public int maxChunkX(){return Math.floorDiv(maxX,16);}
    public int minChunkZ(){return Math.floorDiv(minZ,16);}
    public int maxChunkZ(){return Math.floorDiv(maxZ,16);}
    public static boolean validReference(String pack,String path) {
        if(pack==null || pack.isBlank() || pack.length()>128 || pack.chars().anyMatch(Character::isISOControl)
                || path==null || path.length()>256 || !path.endsWith(".blueprint") || path.startsWith("/")
                || !path.matches("[A-Za-z0-9_ ./-]+"))return false;
        for(String part:path.split("/",-1))if(part.isEmpty() || part.equals(".") || part.equals(".."))return false;
        return true;
    }
}
