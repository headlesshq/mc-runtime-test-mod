package io.github.headlesshq.mcrt.mixin;

#if MC_VER == MC_1_7_10 || MC_VER == MC_1_8_9 || MC_VER == MC_1_12_2
import io.github.headlesshq.mcrt.WorldCreator;
import io.github.headlesshq.mcrtapi.McRuntimeTest;
import net.minecraft.client.Minecraft;
#if MC_VER == MC_1_7_10
import net.minecraft.client.entity.EntityClientPlayerMP;
#elif MC_VER == MC_1_8_9 || MC_VER == MC_1_12_2
import net.minecraft.client.entity.EntityPlayerSP;
#endif
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.integrated.IntegratedServer;
#if MC_VER == MC_1_12_2
import net.minecraft.world.GameType;
#endif
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

// 1.7.10, 1.8.9 1.12.2
@SuppressWarnings({"StringConcatenationArgumentToLogCall", "CommentedOutCode"})
@Mixin(Minecraft.class)
public abstract class MixinLegacyMinecraft {
    // TODO: this makes things ugly, maybe have a third MixinMinecraft just for 1.12.2?
    #if MC_VER == MC_1_7_10 || MC_VER == MC_1_8_9
    @Shadow @Final private static Logger logger;
    @Shadow public WorldClient theWorld;
    @Shadow private IntegratedServer theIntegratedServer;
    #else
    @Shadow @Final private static Logger LOGGER;
    @Shadow private IntegratedServer integratedServer;
    @Shadow public WorldClient world;
    #endif

    @Shadow public GuiScreen currentScreen;
    #if MC_VER == MC_1_7_10
    @Shadow public EntityClientPlayerMP thePlayer;
    #elif MC_VER == MC_1_8_9
    @Shadow public EntityPlayerSP thePlayer;
    #elif MC_VER == MC_1_12_2
    @Shadow public EntityPlayerSP player;
    #endif

    @Shadow
    volatile boolean running;

    @Shadow public abstract void displayGuiScreen(GuiScreen par1);

    #if MC_VER == MC_1_8_9 || MC_VER == MC_1_12_2
    @Shadow public abstract void launchIntegratedServer(String par1, String par2, WorldSettings par3);
    #endif

    @Unique
    private boolean mcRuntimeTest$startedLoadingSPWorld = false;

    @Unique
    #if MC_VER == MC_1_7_10
    private EntityClientPlayerMP mcRuntimeTest$GetPlayer() {
        return thePlayer;
    #elif MC_VER == MC_1_8_9
    private EntityPlayerSP mcRuntimeTest$GetPlayer() {
        return thePlayer;
    #elif MC_VER == MC_1_12_2
    private EntityPlayerSP mcRuntimeTest$GetPlayer() {
        return player;
    #endif
    }

    @Unique
    private Logger mcRuntimeTest$GetLogger() {
        #if MC_VER == MC_1_7_10 || MC_VER == MC_1_8_9
        return logger;
        #else
        return LOGGER;
        #endif
    }

    @Unique
    private WorldClient mcRuntimeTest$GetWorld() {
        #if MC_VER == MC_1_7_10 || MC_VER == MC_1_8_9
        return theWorld;
        #else
        return world;
        #endif
    }

    @Unique
    private IntegratedServer mcRuntimeTest$GetServer() {
        #if MC_VER == MC_1_7_10 || MC_VER == MC_1_8_9
        return theIntegratedServer;
        #else
        return integratedServer;
        #endif
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void displayGuiScreenHook(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (!McRuntimeTest.screenHook()) {
            return;
        }

        if (guiScreenIn instanceof GuiErrorScreen) {
            running = false;
            throw new RuntimeException("Error Screen " + guiScreenIn);
        } else if (guiScreenIn instanceof GuiGameOver && mcRuntimeTest$GetPlayer() != null) {
            mcRuntimeTest$GetPlayer().respawnPlayer();
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void tickHook(CallbackInfo ci) {
        if (!McRuntimeTest.tickHook()) {
            return;
        }

        if (currentScreen instanceof GuiMainMenu) {
            if (!mcRuntimeTest$startedLoadingSPWorld) {
                mc_runtime_test$loadSinglePlayerWorld();
                mcRuntimeTest$startedLoadingSPWorld = true;
            }
        } else {
            mcRuntimeTest$GetLogger().info("Waiting for overlay to disappear...");
        }

        if (mcRuntimeTest$GetPlayer() != null && mcRuntimeTest$GetWorld() != null) {
            if (currentScreen == null) {
                #if MC_VER == MC_1_7_10
                if (!theWorld.getChunkFromBlockCoords((int) thePlayer.posX, (int) thePlayer.posZ).isEmpty()) {
                #elif MC_VER == MC_1_8_9
                if (!theWorld.getChunkFromChunkCoords(((int) thePlayer.posX) >> 4, ((int) thePlayer.posZ) >> 4).isEmpty()) {
                #elif MC_VER == MC_1_12_2
                if (!world.getChunk(((int) player.posX) >> 4, ((int) player.posZ) >> 4).isEmpty()) {
                #endif
                    if (mcRuntimeTest$GetPlayer().ticksExisted < 100) {
                        mcRuntimeTest$GetLogger().info("Waiting " + (100 - mcRuntimeTest$GetPlayer().ticksExisted) + " ticks before testing...");
                    } else {
                        mcRuntimeTest$GetLogger().info("Test successful!");
                        running = false;
                    }
                } else {
                    mcRuntimeTest$GetLogger().info("Players chunk not yet loaded, " + mcRuntimeTest$GetPlayer() + ": cores: " + Runtime.getRuntime().availableProcessors()
                            + ", server running: " + (mcRuntimeTest$GetServer() == null ? "null" : mcRuntimeTest$GetServer().isServerRunning()));
                }
            } else {
                mcRuntimeTest$GetLogger().info("Screen not yet null: " + currentScreen);
            }
        } else {
            mcRuntimeTest$GetLogger().info("Waiting for player to load, screen: " + currentScreen + ", server: " + mcRuntimeTest$GetServer());
        }
    }

    @Unique
    private void mc_runtime_test$loadSinglePlayerWorld() {
        #if MC_VER == MC_1_7_10
        WorldCreator creator = new WorldCreator(new GuiMainMenu());
        displayGuiScreen(creator);
        creator.createNewWorld();
        #elif MC_VER == MC_1_8_9 || MC_VER == MC_1_12_2
        displayGuiScreen(null);
        long seed = (new Random()).nextLong();
            #if MC_VER == MC_1_8_9
            WorldSettings worldSettings = new WorldSettings(seed, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT);
            #elif MC_VER == MC_1_12_2
            WorldSettings worldSettings = new WorldSettings(seed, GameType.SURVIVAL, true, false, WorldType.DEFAULT);
            #endif
        launchIntegratedServer("new_world", "New World", worldSettings);
        #endif
    }

}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class MixinLegacyMinecraft {

}
#endif
