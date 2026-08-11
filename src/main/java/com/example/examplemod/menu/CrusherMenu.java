package com.example.examplemod.menu;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.entity.CrusherBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class CrusherMenu extends AbstractContainerMenu {

    public final CrusherBlockEntity blockEntity;
    private final ContainerData data;

    public CrusherMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new net.minecraft.world.inventory.SimpleContainerData(3));
    }

    public CrusherMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ExampleMod.CRUSHER_MENU.get(), id);
        checkContainerSize(((CrusherBlockEntity) entity).getItemHandler(), 2);
        this.blockEntity = (CrusherBlockEntity) entity;
        this.data = data;

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, 0, 56, 35)); // input
            this.addSlot(new SlotItemHandler(handler, 1, 116, 35)); // output
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int max = CrusherBlockEntity.MAX_PROGRESS;
        int arrowSize = 24;
        return max != 0 && progress != 0 ? progress * arrowSize / max : 0;
    }

    public int getRemainingLife() {
        return data.get(1);
    }

    public int getMaxLife() {
        return data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        // 0,1 = machine slots; 2-28 = player inv; 29-37 = hotbar
        if (index < 2) {
            if (!this.moveItemStackTo(sourceStack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(sourceStack, 0, 1, false)) { // only to input
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ExampleMod.CRUSHER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
