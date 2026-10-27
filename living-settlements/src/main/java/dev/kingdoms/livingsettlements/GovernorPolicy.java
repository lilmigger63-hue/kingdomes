package dev.kingdoms.livingsettlements;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;

/** Selects work; world access and MineColonies mutations stay in the adapter. */
public final class GovernorPolicy {
    public record Candidate(long position, String type, int level, int maxLevel, boolean loaded) {}
    public record Gate(boolean authorized, boolean centerLoaded, boolean underAttack, boolean workPending) {}
    private GovernorPolicy() {}

    public static Optional<Candidate> choose(List<Candidate> candidates, Gate gate,
            int maximumLevel, Map<String, Integer> priorities) {
        if (maximumLevel < 1 || maximumLevel > 5) {
            throw new IllegalArgumentException("Maximum building level must be between 1 and 5");
        }
        if (!gate.authorized() || !gate.centerLoaded() || gate.underAttack() || gate.workPending()) {
            return Optional.empty();
        }
        return candidates.stream()
            .filter(c -> c.loaded() && c.level() >= 0 && c.level() < Math.min(maximumLevel, c.maxLevel()))
            .min(Comparator.comparingInt(Candidate::level)
                .thenComparingInt(c -> priorities.getOrDefault(c.type(), 100))
                .thenComparingLong(Candidate::position));
    }
}
