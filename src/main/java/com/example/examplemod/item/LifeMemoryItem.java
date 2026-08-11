package com.example.examplemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 寿命の記憶。
 * 死んだ機械から得られる。
 * 新しい機械をクラフトするときに使うと、初期寿命が少し伸びる。
 */
public class LifeMemoryItem extends Item {

    public static final int LIFE_BONUS = 16; // 1個使うと初期寿命 +16

    public LifeMemoryItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.examplemod.life_memory"));
        tooltip.add(Component.translatable("tooltip.examplemod.life_memory_bonus", LIFE_BONUS));
    }
}
