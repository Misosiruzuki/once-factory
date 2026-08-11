package com.example.examplemod.block.entity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.CrusherBlock;
import com.example.examplemod.capability.RelativeSide;
import com.example.examplemod.capability.SlotRestrictedHandler;
import com.example.examplemod.menu.CrusherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Crusher — first concrete FiniteMachineBlockEntity. */
public class CrusherBlockEntity extends FiniteMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int MAX_PROGRESS = 100;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_INPUT;
        }
    };

    private LazyOptional<IItemHandler> lazyFull = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyInput = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyOutput = LazyOptional.empty();

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleMod.CRUSHER_BE.get(), pos, state);
    }

    @Override
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected int getMaxProgress() {
        return MAX_PROGRESS;
    }

    @Override
    protected boolean canProcess() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;
        ItemStack result = getResultFor(input.getItem());
        if (result.isEmpty()) return false;
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void processCompleted() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack result = getResultFor(input.getItem());
        if (result.isEmpty()) return;
        itemHandler.extractItem(SLOT_INPUT, 1, false);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    @Override
    protected void updateDegradationState() {
        if (level == null || level.isClientSide) return;
        float ratio = getLifeRatio();
        int stage = ratio > 0.66f ? 0 : (ratio > 0.33f ? 1 : 2);
        BlockState current = level.getBlockState(worldPosition);
        if (current.getBlock() instanceof CrusherBlock
                && current.getValue(CrusherBlock.DEGRADATION) != stage) {
            level.setBlock(worldPosition, current.setValue(CrusherBlock.DEGRADATION, stage), 3);
        }
    }

    @Override
    protected void onMachineBroken() {
        if (level != null && !level.isClientSide) {
            drops();
            level.setBlock(worldPosition, ExampleMod.BROKEN_MACHINE.get().defaultBlockState(), 3);
        }
    }

    @Nullable
    @Override
    protected DirectionProperty getFacingProperty() {
        return CrusherBlock.FACING;
    }

    @Override
    protected LazyOptional<IItemHandler> getItemCapability(@Nullable Direction side) {
        if (side == null) return lazyFull;
        RelativeSide rel = relativeSide(side);
        return switch (rel) {
            case TOP, BACK, LEFT, RIGHT -> lazyInput;
            case BOTTOM -> lazyOutput;
            case FRONT -> LazyOptional.empty();
        };
    }

    @Override
    protected void onLoadItems() {
        lazyFull = LazyOptional.of(() -> itemHandler);
        lazyInput = LazyOptional.of(() ->
                new SlotRestrictedHandler(itemHandler, SLOT_INPUT, SlotRestrictedHandler.Access.INSERT_ONLY));
        lazyOutput = LazyOptional.of(() ->
                new SlotRestrictedHandler(itemHandler, SLOT_OUTPUT, SlotRestrictedHandler.Access.EXTRACT_ONLY));
    }

    @Override
    protected void invalidateItemCaps() {
        lazyFull.invalidate();
        lazyInput.invalidate();
        lazyOutput.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.examplemod.crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrusherMenu(id, inv, this, getContainerData());
    }

    private ItemStack getResultFor(Item item) {
        if (item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE) return new ItemStack(Items.RAW_IRON, 2);
        if (item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE) return new ItemStack(Items.RAW_GOLD, 2);
        if (item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE) return new ItemStack(Items.RAW_COPPER, 3);
        if (item == Items.COAL_ORE || item == Items.DEEPSLATE_COAL_ORE) return new ItemStack(Items.COAL, 3);
        if (item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE) return new ItemStack(Items.DIAMOND, 2);
        if (item == Items.COBBLESTONE) return new ItemStack(Items.GRAVEL, 1);
        if (item == Items.GRAVEL) return new ItemStack(Items.SAND, 1);
        return ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity be) {
        FiniteMachineBlockEntity.serverTick(level, pos, state, be);
    }
}
