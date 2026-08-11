package com.example.examplemod.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.EnergyStorage;

/**
 * Machine FE buffer. RF-compatible units. Energy does not restore life.
 */
public class MachineEnergyStorage extends EnergyStorage {

    @FunctionalInterface
    public interface ChangeListener {
        void onEnergyChanged();
    }

    private final ChangeListener listener;

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract, ChangeListener listener) {
        super(capacity, maxReceive, maxExtract);
        this.listener = listener;
    }

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this(capacity, maxReceive, maxExtract, () -> {});
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) {
            onEnergyChanged();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) {
            onEnergyChanged();
        }
        return extracted;
    }

    /** Internal consume (machine self-use, ignores maxExtract). */
    public int consume(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        int can = Math.min(amount, this.energy);
        if (!simulate && can > 0) {
            this.energy -= can;
            onEnergyChanged();
        }
        return can;
    }

    public boolean hasAtLeast(int amount) {
        return this.energy >= amount;
    }

    public void setEnergy(int value) {
        this.energy = Math.max(0, Math.min(value, capacity));
        onEnergyChanged();
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        }
        onEnergyChanged();
    }

    protected void onEnergyChanged() {
        if (listener != null) {
            listener.onEnergyChanged();
        }
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", this.energy);
        tag.putInt("Capacity", this.capacity);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        if (tag.contains("Capacity")) {
            this.capacity = Math.max(1, tag.getInt("Capacity"));
        }
        this.energy = Math.max(0, Math.min(tag.getInt("Energy"), this.capacity));
    }
}
