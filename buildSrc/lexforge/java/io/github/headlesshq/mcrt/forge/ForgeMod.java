package io.github.headlesshq.mcrt.forge;

#if MC_VER == MC_1_7_10
import cpw.mods.fml.common.Mod;
#else
import net.minecraftforge.fml.common.Mod;
#endif

#if MC_VER == MC_1_7_10 || MC_VER == MC_1_8_9 || MC_VER == MC_1_12_2
@Mod(modid = "mc_runtime_test")
#else
@Mod("mc_runtime_test")
#endif
public class ForgeMod {

}
