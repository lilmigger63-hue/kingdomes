package dev.kingdoms.livingsettlements;

import java.util.List;

/** Pure evaluation of a complete square grid of surface samples, eight blocks apart. */
public final class SiteAssessment {
    public enum Reason { SUITABLE, INCOMPLETE, CLAIMED, WET_OR_UNSAFE, TOO_STEEP }
    public record Surface(int height, boolean fluid, boolean unsafe, boolean claimed) {}
    public record Result(Reason reason, int samples, int relief) {
        public boolean suitable() { return reason == Reason.SUITABLE; }
    }
    private SiteAssessment() {}

    public static Result evaluate(List<Surface> samples, int expected) {
        int side = (int) Math.sqrt(expected);
        if (expected < 1 || side * side != expected || samples.size() != expected) {
            return new Result(Reason.INCOMPLETE, samples.size(), 0);
        }
        int minimum = samples.stream().mapToInt(Surface::height).min().orElseThrow();
        int maximum = samples.stream().mapToInt(Surface::height).max().orElseThrow();
        int relief = maximum - minimum;
        if (samples.stream().anyMatch(Surface::claimed)) {
            return new Result(Reason.CLAIMED, samples.size(), relief);
        }
        if (samples.stream().anyMatch(s -> s.fluid() || s.unsafe())) {
            return new Result(Reason.WET_OR_UNSAFE, samples.size(), relief);
        }
        for (int i = 0; i < samples.size(); i++) {
            int height = samples.get(i).height();
            if ((i % side > 0 && Math.abs(height - samples.get(i - 1).height()) > 6)
                    || (i >= side && Math.abs(height - samples.get(i - side).height()) > 6)) {
                return new Result(Reason.TOO_STEEP, samples.size(), relief);
            }
        }
        return new Result(relief > 12 ? Reason.TOO_STEEP : Reason.SUITABLE, samples.size(), relief);
    }
}
