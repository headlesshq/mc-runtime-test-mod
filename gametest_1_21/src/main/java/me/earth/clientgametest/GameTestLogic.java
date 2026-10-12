package me.earth.clientgametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

/**
 * The actual test, shared by all loaders.
 * Each loader registers it through its own production discovery mechanism,
 * so this mod behaves like a regular mod that ships gametests.
 */
public class GameTestLogic {
    /**
     * Logged when the test runs, CI greps for this to make sure the test really ran.
     */
    public static final String MARKER = "Hi from dummy GameTest!";
    public static final String TEMPLATE = "clientgametest:empty";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void dummyGameTest(GameTestHelper context) {
        LOGGER.info(MARKER);
        context.setBlock(0, 2, 0, Blocks.OBSIDIAN);
        context.succeedWhen(() -> context.assertBlock(new BlockPos(0, 2, 0), (block) -> block == Blocks.OBSIDIAN, "Expect block to be obsidian"));
    }

}
