package com.example.examplemod;

import com.example.examplemod.block.BrokenMachineBlock;
import com.example.examplemod.block.CrusherBlock;
import com.example.examplemod.block.entity.CrusherBlockEntity;
import com.example.examplemod.client.CrusherScreen;
import com.example.examplemod.item.LifeMemoryItem;
import com.example.examplemod.menu.CrusherMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Once Factory — 一度きりの工場
 *
 * 機械には寿命がある。
 * 効率で競う土俵は存在しない。
 * 使い切ったら、次の工場を設計せよ。
 */
@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // === Blocks ===
    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher", CrusherBlock::new);
    public static final RegistryObject<Block> BROKEN_MACHINE = BLOCKS.register("broken_machine", BrokenMachineBlock::new);

    // === Items ===
    public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher",
            () -> new BlockItem(CRUSHER.get(), new Item.Properties()));
    public static final RegistryObject<Item> BROKEN_MACHINE_ITEM = ITEMS.register("broken_machine",
            () -> new BlockItem(BROKEN_MACHINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> LIFE_MEMORY = ITEMS.register("life_memory", LifeMemoryItem::new);

    // === Block Entity ===
    public static final RegistryObject<BlockEntityType<CrusherBlockEntity>> CRUSHER_BE =
            BLOCK_ENTITIES.register("crusher",
                    () -> BlockEntityType.Builder.of(CrusherBlockEntity::new, CRUSHER.get()).build(null));

    // === Menu ===
    public static final RegistryObject<MenuType<CrusherMenu>> CRUSHER_MENU =
            MENUS.register("crusher_menu", () -> IForgeMenuType.create(CrusherMenu::new));

    // === Creative Tab ===
    public static final RegistryObject<CreativeModeTab> ONCE_FACTORY_TAB = CREATIVE_MODE_TABS.register("once_factory_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.examplemod.once_factory"))
                    .icon(() -> CRUSHER_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(CRUSHER_ITEM.get());
                        output.accept(BROKEN_MACHINE_ITEM.get());
                        output.accept(LIFE_MEMORY.get());
                    })
                    .build());

    public ExampleMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Once Factory loaded — machines will die. Design the next one.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting with Once Factory. Remember: nothing lasts forever.");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(CRUSHER_MENU.get(), CrusherScreen::new);
            });
            LOGGER.info("Once Factory client ready.");
        }
    }
}
