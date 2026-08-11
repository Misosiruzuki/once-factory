package com.oncefactory;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = OnceFactory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue DEFAULT_MACHINE_LIFE = BUILDER
            .comment("Initial life of newly placed finite machines")
            .defineInRange("defaultMachineLife", 64, 1, 10000);

    private static final ForgeConfigSpec.BooleanValue ALLOW_COAL_REPAIR = BUILDER
            .comment("Whether coal can slightly restore life")
            .define("allowCoalRepair", false);

    private static final ForgeConfigSpec.BooleanValue REQUIRE_ENERGY = BUILDER
            .comment("When true, processing requires FE. Independent from life.")
            .define("energy.requireEnergy", true);

    private static final ForgeConfigSpec.IntValue ENERGY_CAPACITY = BUILDER
            .comment("Machine FE capacity")
            .defineInRange("energy.capacity", 10000, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ENERGY_MAX_RECEIVE = BUILDER
            .comment("Max FE received per tick")
            .defineInRange("energy.maxReceive", 200, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ENERGY_PER_TICK = BUILDER
            .comment("FE consumed per tick while processing")
            .defineInRange("energy.perTick", 20, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int defaultMachineLife;
    public static boolean allowCoalRepair;
    public static boolean requireEnergy;
    public static int energyCapacity;
    public static int energyMaxReceive;
    public static int energyPerTick;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        defaultMachineLife = DEFAULT_MACHINE_LIFE.get();
        allowCoalRepair = ALLOW_COAL_REPAIR.get();
        requireEnergy = REQUIRE_ENERGY.get();
        energyCapacity = ENERGY_CAPACITY.get();
        energyMaxReceive = ENERGY_MAX_RECEIVE.get();
        energyPerTick = ENERGY_PER_TICK.get();
    }
}
