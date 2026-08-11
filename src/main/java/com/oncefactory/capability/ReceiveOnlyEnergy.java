package com.oncefactory.capability;

import net.minecraftforge.energy.IEnergyStorage;

public class ReceiveOnlyEnergy implements IEnergyStorage {

    private final IEnergyStorage backing;
    private final boolean canReceive;

    public ReceiveOnlyEnergy(IEnergyStorage backing, boolean canReceive) {
        this.backing = backing;
        this.canReceive = canReceive;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive) return 0;
        return backing.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return backing.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return backing.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return canReceive && backing.canReceive();
    }
}
