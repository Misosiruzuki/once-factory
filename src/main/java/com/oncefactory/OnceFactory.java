package com.oncefactory;

import com.oncefactory.block.BrokenMachineBlock;
import com.oncefactory.block.CrusherBlock;
import com.oncefactory.block.entity.CrusherBlockEntity;
import com.oncefactory.client.CrusherScreen;
import com.oncefactory.item.CrusherBlockItem;
import com.oncefactory.item.LifeMemoryItem;
import com.oncefactory.recipe.CrusherWithMemoryRecipe;
import com.oncefactory.menu.CrusherMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
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

@Mod(OnceFactory.MODID)
public class OnceFactory {

    public static final String MODID = "oncefactory";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher", CrusherBlock::new);
    public static final RegistryObject<Block> BROKEN_MACHINE = BLOCKS.register("broken_machine", BrokenMachineBlock::new);

    public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher",
            () -> new CrusherBlockItem(CRUSHER.get(), new Item.Properties()));
    public static final RegistryObject<Item> BROKEN_MACHINE_ITEM = ITEMS.register("broken_machine",
            () -> new BlockItem(BROKEN_MACHINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> LIFE_MEMORY = ITEMS.register("life_memory", LifeMemoryItem::new);

    public static final RegistryObject<BlockEntityType<CrusherBlockEntity>> CRUSHER_BE =
            BLOCK_ENTITIES.register("crusher",
                    () -> BlockEntityType.Builder.of(CrusherBlockEntity::new, CRUSHER.get()).build(null));

    public static final RegistryObject<MenuType<CrusherMenu>> CRUSHER_MENU =
            MENUS.register("crusher_menu", () -> IForgeMenuType.create(CrusherMenu::new));

    public static final RegistryObject<RecipeSerializer<CrusherWithMemoryRecipe>> CRUSHER_WITH_MEMORY_SERIALIZER =
            RECIPE_SERIALIZERS.register("crusher_with_memory",
                    () -> new SimpleCraftingRecipeSerializer<>(CrusherWithMemoryRecipe::new));

    public static final RegistryObject<CreativeModeTab> ONCE_FACTORY_TAB = CREATIVE_MODE_TABS.register("once_factory_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.oncefactory.once_factory"))
                    .icon(() -> CRUSHER_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(CRUSHER_ITEM.get());
                        output.accept(BROKEN_MACHINE_ITEM.get());
                        output.accept(LIFE_MEMORY.get());
                    })
                    .build());

    public OnceFactory(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
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
