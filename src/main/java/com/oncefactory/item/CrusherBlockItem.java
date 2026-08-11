package com.oncefactory.item;

import com.oncefactory.Config;
import com.oncefactory.block.entity.CrusherBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrusherBlockItem extends BlockItem {

    public static final String TAG_INITIAL_LIFE = "InitialLife";

    public CrusherBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static int baseLife() {
        return Config.defaultMachineLife > 0
                ? Config.defaultMachineLife
                : CrusherBlockEntity.DEFAULT_MAX_LIFE;
    }

    public static int getInitialLife(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_INITIAL_LIFE)) {
            return Math.max(1, tag.getInt(TAG_INITIAL_LIFE));
        }
        return baseLife();
    }

    public static void setInitialLife(ItemStack stack, int life) {
        stack.getOrCreateTag().putInt(TAG_INITIAL_LIFE, Math.max(1, life));
    }

    public static ItemStack withLifeBonus(int memoryCount) {
        ItemStack stack = new ItemStack(com.oncefactory.OnceFactory.CRUSHER_ITEM.get());
        int life = baseLife() + Math.max(0, memoryCount) * LifeMemoryItem.LIFE_BONUS;
        setInitialLife(stack, life);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int life = getInitialLife(stack);
        tooltip.add(Component.translatable("tooltip.oncefactory.crusher_life", life));
        if (stack.hasTag() && stack.getTag().contains(TAG_INITIAL_LIFE)) {
            int bonus = life - baseLife();
            if (bonus > 0) {
                tooltip.add(Component.translatable("tooltip.oncefactory.crusher_life_bonus", bonus));
            }
        }
    }
}
