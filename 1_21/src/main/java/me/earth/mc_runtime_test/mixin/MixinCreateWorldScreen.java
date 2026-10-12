package me.earth.mc_runtime_test.mixin;

import me.earth.mc_runtime_test.McRuntimeTest;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen {
    /**
     * Skips the "Experimental Settings" confirmation screen, which nobody would ever click away.
     * Minecraft marks every datapack registry entry that does not come from a vanilla pack as experimental,
     * on Forge this means that any mod adding e.g. a damage type or a custom datapack registry triggers the screen.
     * https://github.com/headlesshq/mc-runtime-test/issues/120
     */
    @ModifyArg(
        method = "onCreate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;confirmWorldCreation(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V"),
        index = 4)
    private boolean skipWarningsHook(boolean skipWarnings) {
        return skipWarnings || McRuntimeTest.tickHook();
    }

}
