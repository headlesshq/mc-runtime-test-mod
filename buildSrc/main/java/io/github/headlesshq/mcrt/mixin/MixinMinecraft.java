package io.github.headlesshq.mcrt.mixin;

#if MC_VER != MC_1_7_10 && MC_VER != MC_1_8_9 && MC_VER != MC_1_12_2
// everything >= 1.16.5
import io.github.headlesshq.mcrt.McGameTestRunner;
import io.github.headlesshq.mcrtapi.McRuntimeTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.MultipleTestTracker;
#if MC_VER == MC_1_16_5 || MC_VER == MC_1_17_1
import org.apache.logging.log4j.Logger;
#else
import org.slf4j.Logger;
#endif
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"CommentedOutCode", "StringConcatenationArgumentToLogCall"})
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public ClientLevel level;
    @Shadow @Nullable public Screen screen;
    @Shadow @Nullable private IntegratedServer singleplayerServer;
    @Shadow private volatile boolean running;

    @Unique
    private boolean mcRuntimeTest$startedLoadingSPWorld = false;
    @Unique
    private boolean mcRuntimeTest$worldCreationStarted = false;
    @Unique
    private MultipleTestTracker mcRuntimeTest$testTracker = null;

    @Shadow
    public abstract @Nullable Overlay getOverlay();

    @Shadow public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreenHook(Screen screen, CallbackInfo ci) {
        if (!McRuntimeTest.screenHook()) {
            return;
        }

        if (screen instanceof ErrorScreen) {
            running = false;
            throw new RuntimeException("Error Screen " + screen);
        } else if (screen instanceof DeathScreen && player != null) {
            player.respawn();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHook(CallbackInfo ci) throws ExecutionException, InterruptedException, TimeoutException {
        if (!McRuntimeTest.tickHook()) {
            return;
        }

        if (getOverlay() == null) {
            if (!mcRuntimeTest$startedLoadingSPWorld && getOverlay() == null) {
                #if MC_VER == MC_1_16_5 || MC_VER == MC_1_17_1
                setScreen(CreateWorldScreen.create(new SelectWorldScreen(new TitleScreen())));
                #elif MC_VER == MC_1_18_2
                setScreen(CreateWorldScreen.createFresh(new SelectWorldScreen(new TitleScreen())));
                #else
                CreateWorldScreen.openFresh(Minecraft.class.cast(this), null);
                #endif
                mcRuntimeTest$startedLoadingSPWorld = true;
            } else if (!mcRuntimeTest$worldCreationStarted && screen instanceof ICreateWorldScreen) {
                ((ICreateWorldScreen) screen).invokeOnCreate();
                mcRuntimeTest$worldCreationStarted = true;
            }
        } else {
            LOGGER.info("Waiting for overlay to disappear...");
        }

        if (player != null && level != null) {
            if (screen == null) {
                if (!level.getChunk(SectionPos.blockToSectionCoord((int) player.getX()), SectionPos.blockToSectionCoord((int) player.getZ())).isEmpty()) {
                    if (player.tickCount < 100) {
                        LOGGER.info("Waiting " + (100 - player.tickCount) + " ticks before testing...");
                    } else if (mcRuntimeTest$testTracker == null) {
                        if (McRuntimeTest.RUN_GAME_TESTS) {
                            LOGGER.info("Running game tests...");
                            mcRuntimeTest$testTracker = McGameTestRunner.runGameTests(player.getUUID(), Objects.requireNonNull(singleplayerServer));
                        } else {
                            LOGGER.info("Successfully finished.");
                            running = false;
                        }
                    } else if (mcRuntimeTest$testTracker.isDone()) {
                        if (mcRuntimeTest$testTracker.getFailedRequiredCount() > 0
                                || mcRuntimeTest$testTracker.getFailedOptionalCount() > 0 && McRuntimeTest.GAME_TESTS_FAIL_ON_OPTIONAL) {
                            System.exit(-1);
                        }

                        running = false;
                    } else {
                        LOGGER.info("Waiting for GameTest: " + mcRuntimeTest$testTracker.getProgressBar());
                    }
                } else {
                    LOGGER.info("Players chunk not yet loaded, " + player + ": cores: " + Runtime.getRuntime().availableProcessors()
                            + ", server running: " + (singleplayerServer == null ? "null" : singleplayerServer.isRunning()));
                }
            } else {
                LOGGER.info("Screen not yet null: " + screen);
                if (McRuntimeTest.CLOSE_ANY_SCREEN || McRuntimeTest.CLOSE_CREATE_WORLD_SCREEN && screen instanceof CreateWorldScreen) {
                    LOGGER.info("Closing screen");
                    setScreen(null);
                }
            }
        } else {
            LOGGER.info("Waiting for player to load...");
        }
    }

}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public class MixinMinecraft {

}
#endif
