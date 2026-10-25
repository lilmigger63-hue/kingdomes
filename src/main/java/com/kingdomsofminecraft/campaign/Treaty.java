package com.kingdomsofminecraft.campaign;

/** One canonical unordered diplomatic pair. Absent pairs are at peace. */
public record Treaty(int first, int second, boolean war, long changedAt) {
    public Treaty {
        if (first < 0 || second <= first || changedAt < 0)
            throw new IllegalArgumentException("Invalid diplomatic pair or time");
    }
}
