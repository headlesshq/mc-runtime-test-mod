package me.earth.mc_runtime_test.forge.mixin;

import me.earth.mc_runtime_test.McRuntimeTest;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeGameTestHooks.class, remap = false)
public abstract class MixinForgeGameTestHooks {
    /**
     * Forge neither registers gametests (RegisterGameTestsEvent, @GameTestHolder)
     * nor ticks the GameTestTicker in production, which is what mc-runtime-test runs.
     * https://github.com/headlesshq/mc-runtime-test/issues/119
     */
    @Inject(method = "isGametestEnabled", at = @At("HEAD"), cancellable = true)
    private static void isGametestEnabledHook(CallbackInfoReturnable<Boolean> cir) {
        if (McRuntimeTest.RUN_GAME_TESTS) {
            cir.setReturnValue(true);
        }
    }

}
