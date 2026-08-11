package com.oncefactory.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LifeMemoryItem extends Item {

    public static final int LIFE_BONUS = 16;

    public LifeMemoryItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.oncefactory.life_memory"));
        tooltip.add(Component.translatable("tooltip.oncefactory.life_memory_bonus", LIFE_BONUS));
    }
}
