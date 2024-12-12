package io.github.headlesshq.mcrt;

#if MC_VER == MC_1_7_10 || MC_VER == MC_1_8_9 || MC_VER == MC_1_12_2
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;

/**
 * Because unimined 1.7.10 produces weird classes like {@code net.minecraft.world.WorldSettings$GameType}, which do not work properly.
 */
public class WorldCreator extends GuiCreateWorld {
    public WorldCreator(GuiScreen guiScreen) {
        super(guiScreen);
    }

    public void createNewWorld() {
        actionPerformed(new GuiButton(0, 0, 0, "")); // <- id 0, loads world
    }

}
#else
@SuppressWarnings("unused")
public class WorldCreator {

}
#endif
