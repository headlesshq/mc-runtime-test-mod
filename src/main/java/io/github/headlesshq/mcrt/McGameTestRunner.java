package io.github.headlesshq.mcrt;

#if MC_VER != MC_1_7_10 && MC_VER != MC_1_8_9 && MC_VER != MC_1_12_2
import io.github.headlesshq.mcrtapi.McRuntimeTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
#if MC_VER == MC_1_16_5 || MC_VER == MC_1_17_1
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
#else
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
#endif

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Similar to running the "/test runall" command.
 */
@SuppressWarnings({"CommentedOutCode", "StringConcatenationArgumentToLogCall", "ExtractMethodRecommender"})
public class McGameTestRunner {
    #if MC_VER == MC_1_16_5 || MC_VER == MC_1_17_1
    private static final Logger LOGGER = LogManager.getLogger();
    #else
    private static final Logger LOGGER = LogUtils.getLogger();
    #endif

    /**
     * Basically what happens in {@link TestCommand} when "runall" is used.
     * We just exit with an error code if a test fails.
     *
     * @param playerUUID the uuid of the player.
     * @param server the server to run the tests on.
     */
    public static MultipleTestTracker runGameTests(UUID playerUUID, MinecraftServer server) throws ExecutionException, InterruptedException, TimeoutException {
        return server.submit(() -> {
            Player player = Objects.requireNonNull(server.getPlayerList().getPlayer(playerUUID));
            #if MC_VER >= MC_1_20_1
            ServerLevel level = (ServerLevel) player.level();
            #else
            ServerLevel level = (ServerLevel) player.level;
            #endif
            GameTestRunner.clearMarkers(level);
            Collection<TestFunction> testFunctions = GameTestRegistry.getAllTestFunctions();
            LOGGER.info("TestFunctions: " + testFunctions);
            if (testFunctions.size() < McRuntimeTest.MIN_GAME_TESTS_TO_FIND) {
                LOGGER.error("Failed to find the minimum amount of gametests, expected " + McRuntimeTest.MIN_GAME_TESTS_TO_FIND + ", but found " + testFunctions.size());
                throw new IllegalStateException("Failed to find the minimum amount of gametests, expected " + McRuntimeTest.MIN_GAME_TESTS_TO_FIND + ", but found " + testFunctions.size());
            }

            GameTestRegistry.forgetFailedTests();

            #if MC_VER >= MC_1_20_6
            Collection<GameTestBatch> batches = GameTestBatchFactory.fromTestFunction(testFunctions, level);
            GameTestRunner gameTestRunner = GameTestRunner.Builder.fromBatches(batches, level).build();
            gameTestRunner.start();

            MultipleTestTracker multipleTestTracker = new MultipleTestTracker(gameTestRunner.getTestInfos());
            #else
            BlockPos pos = createTestPositionAround(player, level);
            Rotation rotation = StructureUtils.getRotationForRotationSteps(0);
                #if MC_VER == MC_1_16_5
                Collection<GameTestInfo> tests = GameTestRunner.runTests(testFunctions, pos, rotation, level, GameTestTicker.singleton, 8);
                #elif MC_VER < MC_1_20_6
                Collection<GameTestInfo> tests = GameTestRunner.runTests(testFunctions, pos, rotation, level, GameTestTicker.SINGLETON, 8);
                #endif
            MultipleTestTracker multipleTestTracker = new MultipleTestTracker(tests);
            #endif
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

    private static BlockPos createTestPositionAround(Player player, ServerLevel level) {
        #if MC_VER > MC_1_18_2
        BlockPos blockPos = player.getOnPos();
        #else
        BlockPos blockPos = new BlockPos(player.position());
        #endif
        int y = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockPos).getY();
        return new BlockPos(blockPos.getX(), y + 1, blockPos.getZ() + 3);
    }

}
#else
public class McGameTestRunner {

}
#endif
