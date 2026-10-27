package dev.kingdoms.livingsettlements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-dimension authorization only; MineColonies remains owner of all colony state. */
public final class GovernorSavedData extends SavedData {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(text -> {
        try { return DataResult.success(UUID.fromString(text)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "Invalid governor authorizer UUID"); }
    }, UUID::toString);

    public record Assignment(int colonyId, UUID authorizer, int maximumLevel) {
        public static final Codec<Assignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("colony_id").forGetter(Assignment::colonyId),
            UUID_CODEC.fieldOf("authorizer").forGetter(Assignment::authorizer),
            Codec.intRange(1, 5).fieldOf("maximum_level").forGetter(Assignment::maximumLevel)
        ).apply(instance, Assignment::new));
        public Assignment {
            if (colonyId < 1 || authorizer == null || maximumLevel < 1 || maximumLevel > 5) {
                throw new IllegalArgumentException("Invalid governor assignment");
            }
        }
    }

    public static final Codec<GovernorSavedData> CODEC = Assignment.CODEC.listOf()
        .fieldOf("governors").codec().flatXmap(entries -> {
            if (entries.stream().map(Assignment::colonyId).distinct().count() != entries.size()) {
                return DataResult.error(() -> "Duplicate governor colony assignment");
            }
            return DataResult.success(new GovernorSavedData(entries));
        }, data -> DataResult.success(List.copyOf(data.assignments.values())));
    private static final SavedDataType<GovernorSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(LivingSettlements.MOD_ID, "governors"),
        () -> new GovernorSavedData(List.of()), CODEC, null);

    private final Map<Integer, Assignment> assignments = new HashMap<>();
    // Transient reports and cooldowns never become another colony database.
    final Map<Integer, String> reports = new HashMap<>();
    final Map<Long, Long> retryAfter = new HashMap<>();

    GovernorSavedData(List<Assignment> entries) {
        entries.forEach(entry -> assignments.put(entry.colonyId(), entry));
    }

    public static GovernorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
    public Collection<Assignment> assignments() { return List.copyOf(assignments.values()); }
    public Assignment assignment(int id) { return assignments.get(id); }
    public void enable(int id, UUID authorizer, int maximumLevel) {
        assignments.put(id, new Assignment(id, authorizer, maximumLevel));
        reports.put(id, "Enabled; waiting for next governor cycle");
        setDirty();
    }
    public void disable(int id) {
        if (assignments.remove(id) != null) { setDirty(); }
        reports.remove(id);
    }
}
