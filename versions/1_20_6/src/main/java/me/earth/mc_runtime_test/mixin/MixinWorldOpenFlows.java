package me.earth.mc_runtime_test.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = WorldOpenFlows.class, priority = 1001)
public class MixinWorldOpenFlows {

    @ModifyVariable(
            method = "confirmWorldCreation",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private static Lifecycle modifyLifecycle(Lifecycle lifecycle) {
        return Lifecycle.stable();
    }
}
