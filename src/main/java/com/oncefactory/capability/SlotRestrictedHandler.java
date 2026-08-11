package com.oncefactory.capability;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class SlotRestrictedHandler implements IItemHandlerModifiable {

    public enum Access { INSERT_ONLY, EXTRACT_ONLY, BOTH }

    private final IItemHandlerModifiable parent;
    private final int slot;
    private final Access access;

    public SlotRestrictedHandler(IItemHandlerModifiable parent, int slot, Access access) {
        this.parent = parent;
        this.slot = slot;
        this.access = access;
    }

    @Override
    public int getSlots() { return 1; }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return parent.getStackInSlot(this.slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (access == Access.EXTRACT_ONLY) return stack;
        return parent.insertItem(this.slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (access == Access.INSERT_ONLY) return ItemStack.EMPTY;
        return parent.extractItem(this.slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return parent.getSlotLimit(this.slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (access == Access.EXTRACT_ONLY) return false;
        return parent.isItemValid(this.slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        parent.setStackInSlot(this.slot, stack);
    }
}
