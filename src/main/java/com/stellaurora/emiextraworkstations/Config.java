package com.stellaurora.emiextraworkstations;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = EmiExtraWorkstations.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_COLUMNS = BUILDER
            .comment("The maximum number of columns for the workstations in LEFT/RIGHT mode.")
            .defineInRange("maxColumns", 2, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue MAX_ROWS = BUILDER
            .comment("The maximum number of rows for the workstations in BOTTOM mode.")
            .defineInRange("maxRows", 2, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}