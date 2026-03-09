package com.stellaurora.emiextraworkstations;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EmiExtraWorkstations.MODID)
public class EmiExtraWorkstations
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "emiextraworkstations";

    public EmiExtraWorkstations() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
