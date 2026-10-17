package me.earth.mc_runtime_test;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Similar to running the "/test runall" command.
 */
public class McGameTestRunner {
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Default of "/test runall".
     */
    private static final int TESTS_PER_ROW = 8;

    /**
     * Basically what happens in {@link TestCommand} when "runall" is used.
     * We just exit with an error code if a test fails.
     *
     * @param playerUUID the uuid of the player.
     * @param server the server to run the tests on.
     */
    public static @Nullable MultipleTestTracker runGameTests(UUID playerUUID, MinecraftServer server) throws ExecutionException, InterruptedException, TimeoutException {
        return server.submit(() -> {
            Player player = Objects.requireNonNull(server.getPlayerList().getPlayer(playerUUID));
            ServerLevel level = (ServerLevel) player.level();
            GameTestRunner.clearMarkers(level);
            Collection<TestFunction> testFunctions = GameTestRegistry.getAllTestFunctions();
            LOGGER.info("TestFunctions: " + testFunctions);
            if (testFunctions.size() < McRuntimeTest.MIN_GAME_TESTS_TO_FIND) {
                LOGGER.error("Failed to find the minimum amount of gametests, expected " + McRuntimeTest.MIN_GAME_TESTS_TO_FIND + ", but found " + testFunctions.size());
                throw new IllegalStateException("Failed to find the minimum amount of gametests, expected " + McRuntimeTest.MIN_GAME_TESTS_TO_FIND + ", but found " + testFunctions.size());
            } else if (testFunctions.isEmpty()) {
                return null;
            }

            GameTestRegistry.forgetFailedTests();

            Collection<GameTestBatch> batches = GameTestBatchFactory.fromTestFunction(testFunctions, level);
            GameTestRunner gameTestRunner = GameTestRunner.Builder.fromBatches(batches, level)
                    // The default StructureSpawner.NOT_SET never places a structure, so no test would ever start.
                    // https://github.com/headlesshq/mc-runtime-test/issues/121
                    .newStructureSpawner(new StructureGridSpawner(createTestPositionAround(player, level), TESTS_PER_ROW, false))
                    .build();
            gameTestRunner.start();

            MultipleTestTracker multipleTestTracker = new MultipleTestTracker(gameTestRunner.getTestInfos());
            multipleTestTracker.addFailureListener(gameTestInfo -> {
                LOGGER.error("Test failed: " + gameTestInfo);
                if (gameTestInfo.getError() != null) {
                    LOGGER.error(String.valueOf(gameTestInfo), gameTestInfo.getError());
                }

                if (!gameTestInfo.isOptional() || McRuntimeTest.GAME_TESTS_FAIL_ON_OPTIONAL) {
                    System.exit(-1);
                }
            });

            return multipleTestTracker;
        }).get(60, TimeUnit.SECONDS);
    }

    /**
     * Same position as TestCommand.createTestPositionAround uses for "/test runall".
     */
    private static BlockPos createTestPositionAround(Player player, ServerLevel level) {
        BlockPos pos = player.blockPosition();
        int y = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos).getY();
        return new BlockPos(pos.getX(), y + 1, pos.getZ() + 3);
    }

}
