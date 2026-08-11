package com.example.examplemod.block.entity;

import com.example.examplemod.Config;
import com.example.examplemod.capability.ReceiveOnlyEnergy;
import com.example.examplemod.capability.RelativeSide;
import com.example.examplemod.energy.MachineEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for once-only machines: life, FE, tick template, sided energy.
 * Subclasses own slots, recipes, item-side IO, degradation, and death block.
 */
public abstract class FiniteMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int DEFAULT_MAX_LIFE = 64;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_LIFE = 1;
    public static final int DATA_MAX_LIFE = 2;
    public static final int DATA_ENERGY = 3;
    public static final int DATA_ENERGY_CAP = 4;
    public static final int DATA_COUNT = 5;

    private final MachineEnergyStorage energyStorage;
    private LazyOptional<IEnergyStorage> lazyEnergyReceive = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyBlocked = LazyOptional.empty();

    private int progress;
    private int remainingLife;
    private int maxLife;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_LIFE -> remainingLife;
                case DATA_MAX_LIFE -> maxLife;
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAP -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_LIFE -> remainingLife = value;
                case DATA_MAX_LIFE -> maxLife = value;
                case DATA_ENERGY -> energyStorage.setEnergy(value);
                case DATA_ENERGY_CAP -> energyStorage.setCapacity(value);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected FiniteMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        int life = Config.defaultMachineLife > 0 ? Config.defaultMachineLife : DEFAULT_MAX_LIFE;
        this.maxLife = life;
        this.remainingLife = life;
        int capacity = Config.energyCapacity > 0 ? Config.energyCapacity : 10000;
        int maxReceive = Config.energyMaxReceive > 0 ? Config.energyMaxReceive : 200;
        this.energyStorage = new MachineEnergyStorage(capacity, maxReceive, 0, this::setChanged);
    }

    protected abstract int getMaxProgress();
    protected abstract boolean canProcess();
    protected abstract void processCompleted();
    protected abstract void updateDegradationState();
    protected abstract void onMachineBroken();
    public abstract ItemStackHandler getItemHandler();

    protected LazyOptional<IItemHandler> getItemCapability(@Nullable Direction side) {
        return LazyOptional.of(this::getItemHandler);
    }

    protected Direction getFacing() {
        BlockState state = getBlockState();
        DirectionProperty facingProp = getFacingProperty();
        if (facingProp != null && state.hasProperty(facingProp)) {
            return state.getValue(facingProp);
        }
        return Direction.NORTH;
    }

    @Nullable
    protected abstract DirectionProperty getFacingProperty();

    protected boolean canReceiveEnergy(RelativeSide side) {
        return side != RelativeSide.FRONT;
    }

    public void setInitialLife(int life) {
        this.maxLife = Math.max(1, life);
        this.remainingLife = this.maxLife;
        setChanged();
        updateDegradationState();
    }

    public int getRemainingLife() { return remainingLife; }
    public int getMaxLife() { return maxLife; }

    public float getLifeRatio() {
        return maxLife <= 0 ? 0f : (float) remainingLife / (float) maxLife;
    }

    public int getProgress() { return progress; }
    public ContainerData getContainerData() { return data; }
    public MachineEnergyStorage getEnergyStorage() { return energyStorage; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }

    public boolean hasPowerForTick() {
        if (!Config.requireEnergy) return true;
        int cost = Math.max(0, Config.energyPerTick);
        return cost == 0 || energyStorage.hasAtLeast(cost);
    }

    protected RelativeSide relativeSide(Direction absolute) {
        return RelativeSide.from(getFacing(), absolute);
    }

    private void ensureEnergyConfig() {
        int cap = Config.energyCapacity > 0 ? Config.energyCapacity : 10000;
        if (energyStorage.getMaxEnergyStored() != cap) {
            energyStorage.setCapacity(cap);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return getItemCapability(side).cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            if (side == null) {
                return lazyEnergyReceive.cast();
            }
            RelativeSide rel = relativeSide(side);
            return canReceiveEnergy(rel) ? lazyEnergyReceive.cast() : lazyEnergyBlocked.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergyReceive = LazyOptional.of(() -> new ReceiveOnlyEnergy(energyStorage, true));
        lazyEnergyBlocked = LazyOptional.of(() -> new ReceiveOnlyEnergy(energyStorage, false));
        ensureEnergyConfig();
        onLoadItems();
    }

    protected void onLoadItems() {}

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyReceive.invalidate();
        lazyEnergyBlocked.invalidate();
        invalidateItemCaps();
    }

    protected void invalidateItemCaps() {}

    public void drops() {
        ItemStackHandler handler = getItemHandler();
        SimpleContainer inventory = new SimpleContainer(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            inventory.setItem(i, handler.getStackInSlot(i));
        }
        if (level != null) {
            Containers.dropContents(level, worldPosition, inventory);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", getItemHandler().serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("RemainingLife", remainingLife);
        tag.putInt("MaxLife", maxLife);
        tag.put("Energy", energyStorage.serialize());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        getItemHandler().deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        remainingLife = tag.getInt("RemainingLife");
        maxLife = tag.contains("MaxLife") ? tag.getInt("MaxLife") : DEFAULT_MAX_LIFE;
        if (tag.contains("Energy")) {
            energyStorage.deserialize(tag.getCompound("Energy"));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FiniteMachineBlockEntity be) {
        if (level.isClientSide) return;
        be.ensureEnergyConfig();

        boolean canWork = be.canProcess() && be.remainingLife > 0 && be.hasPowerForTick();

        if (canWork) {
            int cost = Math.max(0, Config.energyPerTick);
            if (Config.requireEnergy && cost > 0) {
                int used = be.energyStorage.consume(cost, false);
                if (used < cost) {
                    be.progress = 0;
                    be.setChanged();
                    return;
                }
            }

            be.progress++;

            if (be.progress % 20 == 0) {
                be.remainingLife = Math.max(0, be.remainingLife - 1);
                be.setChanged();
                be.updateDegradationState();
            }

            if (be.progress >= be.getMaxProgress()) {
                be.processCompleted();
                be.progress = 0;
                be.setChanged();
            }

            if (be.remainingLife <= 0) {
                be.onMachineBroken();
            }
        } else if (be.progress != 0) {
            be.progress = 0;
            be.setChanged();
        }
    }
}
