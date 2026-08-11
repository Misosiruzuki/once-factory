package com.oncefactory.recipe;

import com.oncefactory.OnceFactory;
import com.oncefactory.item.CrusherBlockItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CrusherWithMemoryRecipe extends CustomRecipe {

    public CrusherWithMemoryRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int ironSlots = 0, stoneSlots = 0, memorySlots = 0, other = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.IRON_INGOT)) ironSlots++;
            else if (stack.is(Items.SMOOTH_STONE)) stoneSlots++;
            else if (stack.is(OnceFactory.LIFE_MEMORY.get())) memorySlots++;
            else other++;
        }
        return other == 0 && ironSlots >= 6 && stoneSlots >= 3 && memorySlots >= 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int memorySlots = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(OnceFactory.LIFE_MEMORY.get())) memorySlots++;
        }
        return CrusherBlockItem.withLifeBonus(Math.max(1, memorySlots));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 10;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OnceFactory.CRUSHER_WITH_MEMORY_SERIALIZER.get();
    }
}
