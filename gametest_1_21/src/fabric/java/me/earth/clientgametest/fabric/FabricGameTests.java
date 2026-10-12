package me.earth.clientgametest.fabric;

import me.earth.clientgametest.GameTestLogic;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Registered through the "fabric-gametest" entrypoint of the fabric-gametest-api.
 */
public class FabricGameTests {
    @GameTest(template = GameTestLogic.TEMPLATE)
    public void dummyGameTest(GameTestHelper context) {
        GameTestLogic.dummyGameTest(context);
    }

}
