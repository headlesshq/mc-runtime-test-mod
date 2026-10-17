package me.earth.clientgametest.forge;

import me.earth.clientgametest.GameTestLogic;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Discovered by Forge's annotation scan in ForgeGameTestHooks.registerGametests().
 * Templates containing a ':' are used verbatim by Forge.
 */
@GameTestHolder("clientgametest")
public class ForgeGameTests {
    @GameTest(template = GameTestLogic.TEMPLATE)
    public static void dummyGameTest(GameTestHelper context) {
        GameTestLogic.dummyGameTest(context);
    }

}
