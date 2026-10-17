package me.earth.clientgametest.neoforge;

import me.earth.clientgametest.GameTestLogic;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Discovered by NeoForge's annotation scan in GameTestHooks.registerGametests().
 * Template resolves to "clientgametest:empty".
 */
@GameTestHolder("clientgametest")
@PrefixGameTestTemplate(false)
public class NeoForgeGameTests {
    @GameTest(template = "empty")
    public static void dummyGameTest(GameTestHelper context) {
        GameTestLogic.dummyGameTest(context);
    }

}
