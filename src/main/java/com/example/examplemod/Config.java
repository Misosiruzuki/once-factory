package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue DEFAULT_MACHINE_LIFE = BUILDER
            .comment("新規に設置した一度きりの機械の初期寿命（処理中に減っていく値）")
            .defineInRange("defaultMachineLife", 64, 1, 10000);

    private static final ForgeConfigSpec.BooleanValue ALLOW_COAL_REPAIR = BUILDER
            .comment("石炭で寿命をわずかに回復させるか（一度きりの精神を弱める）")
            .define("allowCoalRepair", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int defaultMachineLife;
    public static boolean allowCoalRepair;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        defaultMachineLife = DEFAULT_MACHINE_LIFE.get();
        allowCoalRepair = ALLOW_COAL_REPAIR.get();
    }
}
