package dev.kingdoms.livingsettlements.gametest;

import dev.kingdoms.livingsettlements.ColonyIntegration;
import dev.kingdoms.livingsettlements.GovernorSavedData;
import dev.kingdoms.livingsettlements.SiteAssessment;
import dev.kingdoms.livingsettlements.TerrainSurvey;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;
import java.util.function.Consumer;

/** Only on the gameTest source set; not included in the distributed addon. */
@Mod(SettlementGameTests.MOD_ID)
public final class SettlementGameTests {
    public static boolean mayorTestComplete;
    static final String MOD_ID = "living_settlements_tests";
    private final DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, MOD_ID);

    public SettlementGameTests(IEventBus bus) {
        functions.register("commands", () -> SettlementGameTests::commands);
        functions.register("unloaded_survey", () -> SettlementGameTests::unloadedSurvey);
        functions.register("natural_settlement", () -> NaturalSettlementGameTest::run);
        functions.register("water_budget", () -> NaturalSettlementGameTest::waterBudget);
        functions.register("village_selection", () -> VillageReplacementGameTest::run);
        functions.register("village_colony", () -> NaturalSettlementGameTest::village);
        functions.register("village_fallback", () -> NaturalSettlementGameTest::fallback);
        functions.register("terrain_compatibility", () -> VillageReplacementGameTest::terrain);
        functions.register("autonomous_mayor", () -> MayorGameTest::run);
        functions.register("evolving_world", () -> EvolutionGameTest::run);
        functions.register("adaptive_unloaded", () -> EvolutionGameTest::unloaded);
        functions.register("queued_edit_protection", () -> EvolutionGameTest::editedBuilding);
        functions.register("blocked_infrastructure", () -> EvolutionGameTest::blockedInfrastructure);
        functions.register(bus);
        bus.addListener(this::registerTests);
    }
    private void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("empty"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : List.of("commands", "unloaded_survey")) {
            event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                new TestData<>(environment, id("empty"), 100, 0, true)));
        }
        event.registerTest(id("natural_settlement"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("natural_settlement")),
            new TestData<>(environment, id("empty"), 40000, 0, true)));
        event.registerTest(id("water_budget"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("water_budget")),
            new TestData<>(environment, id("empty"), 100, 0, true)));
        event.registerTest(id("village_selection"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("village_selection")),
            new TestData<>(environment, id("empty"), 1000, 0, true)));
        event.registerTest(id("village_colony"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("village_colony")),
            new TestData<>(environment, id("empty"), 40000, 0, true)));
        event.registerTest(id("village_fallback"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("village_fallback")),
            new TestData<>(environment, id("empty"), 40000, 0, true)));
        event.registerTest(id("terrain_compatibility"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("terrain_compatibility")),
            new TestData<>(environment, id("empty"), 1000, 0, true)));
        event.registerTest(id("autonomous_mayor"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("autonomous_mayor")),
            new TestData<>(environment, id("empty"), 60000, 0, true)));
        event.registerTest(id("evolving_world"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("evolving_world")),
            new TestData<>(environment, id("empty"), 120000, 0, true)));
        for(String name:List.of("adaptive_unloaded","queued_edit_protection","blocked_infrastructure"))
            event.registerTest(id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),new TestData<>(environment,id("empty"),60000,0,true)));
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    private static void commands(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var commands = server.getCommands().getDispatcher();
        var source = server.createCommandSourceStack();
        try {
            helper.assertTrue(commands.execute("settlements", source) == 1,
                Component.literal("Addon help must execute on the real server command dispatcher"));
            int before = GovernorSavedData.get(helper.getLevel()).assignments().size();
            try {
                commands.execute("settlements governor enable", source);
                helper.fail(Component.literal("Console must not authorize colony automation without a player"));
            } catch (CommandSyntaxException expected) {
                helper.assertTrue(GovernorSavedData.get(helper.getLevel()).assignments().size() == before,
                    Component.literal("Rejected authorization must not create saved governor settings"));
            }
        } catch (CommandSyntaxException ex) {
            helper.fail(Component.literal("Addon command is missing: " + ex.getMessage()));
        }
        helper.succeed();
    }
    private static void unloadedSurvey(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos distant = new BlockPos(-1_000_000, 70, -1_000_000);
        helper.assertTrue(!ColonyIntegration.loaded(level, distant), Component.literal("Test probe must start unloaded"));
        var result = TerrainSurvey.survey(level, distant);
        helper.assertTrue(result.reason() == SiteAssessment.Reason.INCOMPLETE,
            Component.literal("Unloaded terrain must produce an incomplete survey"));
        helper.assertTrue(!ColonyIntegration.loaded(level, distant),
            Component.literal("Survey must not load negative-coordinate chunks"));
        helper.succeed();
    }
}
