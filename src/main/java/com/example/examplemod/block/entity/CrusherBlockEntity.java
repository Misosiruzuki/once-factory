package com.example.examplemod.block.entity;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.CrusherBlock;
import com.example.examplemod.capability.ReceiveOnlyEnergy;
import com.example.examplemod.capability.RelativeSide;
import com.example.examplemod.capability.SlotRestrictedHandler;
import com.example.examplemod.energy.MachineEnergyStorage;
import com.example.examplemod.menu.CrusherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrusherBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int MAX_PROGRESS = 100;
    public static final int DEFAULT_MAX_LIFE = 64;

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

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(10000, 200, 0, this::setChanged);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyInputHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyOutputHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyReceive = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyBlocked = LazyOptional.empty();

    private int progress = 0;
    private int remainingLife;
    private int maxLife;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> remainingLife;
                case 2 -> maxLife;
                case 3 -> energyStorage.getEnergyStored();
                case 4 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> remainingLife = value;
                case 2 -> maxLife = value;
                case 3 -> energyStorage.setEnergy(value);
                case 4 -> energyStorage.setCapacity(value);
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleMod.CRUSHER_BE.get(), pos, state);
        int life = Config.defaultMachineLife > 0 ? Config.defaultMachineLife : DEFAULT_MAX_LIFE;
        this.maxLife = life;
        this.remainingLife = life;
    }

    private void ensureEnergyConfig() {
        int cap = Config.energyCapacity > 0 ? Config.energyCapacity : 10000;
        if (energyStorage.getMaxEnergyStored() != cap) {
            energyStorage.setCapacity(cap);
        }
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

    public MachineEnergyStorage getEnergyStorage() { return energyStorage; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }

    public boolean hasPowerForTick() {
        if (!Config.requireEnergy) return true;
        int cost = Math.max(0, Config.energyPerTick);
        return cost == 0 || energyStorage.hasAtLeast(cost);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.examplemod.crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrusherMenu(id, inv, this, this.data);
    }

    /**
     * Side IO (relative to FACING):
     * TOP/BACK/LEFT/RIGHT = item input; BOTTOM = item output; FRONT = no item IO.
     * All except FRONT accept FE; FRONT blocks energy.
     */
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return lazyItemHandler.cast();
            }
            RelativeSide rel = relativeSide(side);
            return switch (rel) {
                case TOP, BACK, LEFT, RIGHT -> lazyInputHandler.cast();
                case BOTTOM -> lazyOutputHandler.cast();
                case FRONT -> LazyOptional.empty();
            };
        }
        if (cap == ForgeCapabilities.ENERGY) {
            if (side == null) {
                return lazyEnergyReceive.cast();
            }
            RelativeSide rel = relativeSide(side);
            return switch (rel) {
                case FRONT -> lazyEnergyBlocked.cast();
                case TOP, BOTTOM, BACK, LEFT, RIGHT -> lazyEnergyReceive.cast();
            };
        }
        return super.getCapability(cap, side);
    }

    private RelativeSide relativeSide(Direction absolute) {
        Direction facing = Direction.NORTH;
        BlockState state = getBlockState();
        if (state.hasProperty(CrusherBlock.FACING)) {
            facing = state.getValue(CrusherBlock.FACING);
        }
        return RelativeSide.from(facing, absolute);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyInputHandler = LazyOptional.of(() ->
                new SlotRestrictedHandler(itemHandler, SLOT_INPUT, SlotRestrictedHandler.Access.INSERT_ONLY));
        lazyOutputHandler = LazyOptional.of(() ->
                new SlotRestrictedHandler(itemHandler, SLOT_OUTPUT, SlotRestrictedHandler.Access.EXTRACT_ONLY));
        lazyEnergyReceive = LazyOptional.of(() -> new ReceiveOnlyEnergy(energyStorage, true));
        lazyEnergyBlocked = LazyOptional.of(() -> new ReceiveOnlyEnergy(energyStorage, false));
        ensureEnergyConfig();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyInputHandler.invalidate();
        lazyOutputHandler.invalidate();
        lazyEnergyReceive.invalidate();
        lazyEnergyBlocked.invalidate();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("RemainingLife", remainingLife);
        tag.putInt("MaxLife", maxLife);
        tag.put("Energy", energyStorage.serialize());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        remainingLife = tag.getInt("RemainingLife");
        maxLife = tag.contains("MaxLife") ? tag.getInt("MaxLife") : DEFAULT_MAX_LIFE;
        if (tag.contains("Energy")) {
            energyStorage.deserialize(tag.getCompound("Energy"));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity be) {
        if (level.isClientSide) return;
        be.ensureEnergyConfig();

        boolean canWork = be.hasRecipe() && be.remainingLife > 0 && be.hasPowerForTick();

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

            if (be.progress >= MAX_PROGRESS) {
                be.craftItem();
                be.progress = 0;
                be.setChanged();
            }

            if (be.remainingLife <= 0) {
                be.breakMachine();
            }
        } else if (be.progress != 0) {
            be.progress = 0;
            be.setChanged();
        }
    }

    private boolean hasRecipe() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;
        ItemStack result = getResultFor(input.getItem());
        if (result.isEmpty()) return false;
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craftItem() {
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

    private void updateDegradationState() {
        if (level == null || level.isClientSide) return;
        float ratio = getLifeRatio();
        int stage = ratio > 0.66f ? 0 : (ratio > 0.33f ? 1 : 2);
        BlockState current = level.getBlockState(worldPosition);
        if (current.getBlock() instanceof CrusherBlock && current.getValue(CrusherBlock.DEGRADATION) != stage) {
            level.setBlock(worldPosition, current.setValue(CrusherBlock.DEGRADATION, stage), 3);
        }
    }

    private void breakMachine() {
        if (level != null && !level.isClientSide) {
            drops();
            level.setBlock(worldPosition, ExampleMod.BROKEN_MACHINE.get().defaultBlockState(), 3);
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
