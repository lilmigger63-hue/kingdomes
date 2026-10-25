package com.kingdomsofminecraft.campaign;

/** Immutable strategic clock. No Minecraft objects or loaded chunks are retained. */
public record CampaignState(long worldSeed, long simulationStep) {
    public CampaignState {
        if (simulationStep < 0) throw new IllegalArgumentException("Negative campaign time");
    }

    public CampaignState advance() {
        return new CampaignState(worldSeed, Math.incrementExact(simulationStep));
    }
}
