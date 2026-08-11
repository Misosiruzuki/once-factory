package com.example.examplemod.block.entity;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.CrusherBlock;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 一度きりの粉砕機の核。
 *
 * - 処理中に寿命が少しずつ減る
 * - 寿命が尽きると壊れた機械に変わる
 * - 見た目劣化は寿命比率で 0/1/2 の3段階
 */
public class CrusherBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int MAX_PROGRESS = 100; // 処理完了までのtick目安
    public static final int DEFAULT_MAX_LIFE = 64;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

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
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> remainingLife = value;
                case 2 -> maxLife = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleMod.CRUSHER_BE.get(), pos, state);
        int life = Config.defaultMachineLife > 0 ? Config.defaultMachineLife : DEFAULT_MAX_LIFE;
        this.maxLife = life;
        this.remainingLife = life;
    }

    /** クラフト時に寿命の記憶を使った場合など、外部から初期寿命を上書きする */
    public void setInitialLife(int life) {
        this.maxLife = Math.max(1, life);
        this.remainingLife = this.maxLife;
        setChanged();
        updateDegradationState();
    }

    public int getRemainingLife() {
        return remainingLife;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public float getLifeRatio() {
        return maxLife <= 0 ? 0f : (float) remainingLife / (float) maxLife;
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

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
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
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        remainingLife = tag.getInt("RemainingLife");
        maxLife = tag.contains("MaxLife") ? tag.getInt("MaxLife") : DEFAULT_MAX_LIFE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity be) {
        if (level.isClientSide) return;

        if (be.hasRecipe() && be.remainingLife > 0) {
            be.progress++;
            // 処理中に寿命を少しずつ消費（MAX_PROGRESS 回で roughly 1 寿命相当になるよう調整）
            // ここでは進捗 1 につき寿命を確率的ではなく、一定間隔で減らす
            if (be.progress % 20 == 0) { // 約1秒ごとに1消費
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
        } else {
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

    /** 簡易ハードコードレシピ。後で RecipeType に置き換え可能 */
    private ItemStack getResultFor(Item item) {
        if (item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE) {
            return new ItemStack(Items.RAW_IRON, 2);
        }
        if (item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE) {
            return new ItemStack(Items.RAW_GOLD, 2);
        }
        if (item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE) {
            return new ItemStack(Items.RAW_COPPER, 3);
        }
        if (item == Items.COAL_ORE || item == Items.DEEPSLATE_COAL_ORE) {
            return new ItemStack(Items.COAL, 3);
        }
        if (item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE) {
            return new ItemStack(Items.DIAMOND, 2);
        }
        if (item == Items.COBBLESTONE) {
            return new ItemStack(Items.GRAVEL, 1);
        }
        if (item == Items.GRAVEL) {
            return new ItemStack(Items.SAND, 1);
        }
        return ItemStack.EMPTY;
    }

    private void updateDegradationState() {
        if (level == null || level.isClientSide) return;
        float ratio = getLifeRatio();
        int stage;
        if (ratio > 0.66f) {
            stage = 0; // 新品
        } else if (ratio > 0.33f) {
            stage = 1; // 使用中
        } else {
            stage = 2; // 危険
        }
        BlockState current = level.getBlockState(worldPosition);
        if (current.getBlock() instanceof CrusherBlock && current.getValue(CrusherBlock.DEGRADATION) != stage) {
            level.setBlock(worldPosition, current.setValue(CrusherBlock.DEGRADATION, stage), 3);
        }
    }

    private void breakMachine() {
        if (level != null && !level.isClientSide) {
            // 中身をドロップしてから壊れた機械に置換
            drops();
            BlockState broken = ExampleMod.BROKEN_MACHINE.get().defaultBlockState();
            level.setBlock(worldPosition, broken, 3);
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
