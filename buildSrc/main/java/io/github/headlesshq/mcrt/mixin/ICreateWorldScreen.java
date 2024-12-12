package io.github.headlesshq.mcrt.mixin;

#if MC_VER != MC_1_7_10 && MC_VER != MC_1_8_9 && MC_VER != MC_1_12_2
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreateWorldScreen.class)
public interface ICreateWorldScreen {
    @Invoker("onCreate")
    void invokeOnCreate();

}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("unused")
@Mixin(Minecraft.class) // dummy because this is still in the mixin config
public interface ICreateWorldScreen {

}
#endif
