package com.aoironeon1898.caelum.common.content.machines.tile;

import com.aoironeon1898.caelum.common.content.machines.menus.StellarCrusherMenu;
import com.aoironeon1898.caelum.common.content.machines.recipes.StellarCrusherRecipe;
import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StellarCrusherBlockEntity extends BlockEntity implements MenuProvider {

    // 定数定義
    private static final int MACHINE_TIER = 1;
    private static final int ENERGY_CAPACITY = 60000;
    private static final int ENERGY_TRANSFER = 200;
    private static final int ENERGY_PER_TICK = 40;

    // スロット管理 (0: 入力, 1: 出力)
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // エネルギー管理
    private final ModEnergyStorage energyStorage = new ModEnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER) {
        @Override
        public void onEnergyChanged() {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;

    public StellarCrusherBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.STELLAR_CRUSHER_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> StellarCrusherBlockEntity.this.energyStorage.getEnergyStored();
                    case 1 -> StellarCrusherBlockEntity.this.energyStorage.getMaxEnergyStored();
                    case 2 -> StellarCrusherBlockEntity.this.progress;
                    case 3 -> StellarCrusherBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> StellarCrusherBlockEntity.this.energyStorage.setEnergy(pValue);
                    case 2 -> StellarCrusherBlockEntity.this.progress = pValue;
                    case 3 -> StellarCrusherBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.caelum.stellar_crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new StellarCrusherMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("energy", energyStorage.getEnergyStored());
        pTag.putInt("stellar_crusher.progress", progress);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        energyStorage.setEnergy(pTag.getInt("energy"));
        progress = pTag.getInt("stellar_crusher.progress");
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // --- Tick 処理 ---
    public static void tick(Level level, BlockPos pos, BlockState state, StellarCrusherBlockEntity pEntity) {
        if (level.isClientSide()) {
            return;
        }

        // ★★★ デバッグ用：常にエネルギーをMAXにする ★★★
        pEntity.energyStorage.setEnergy(pEntity.energyStorage.getMaxEnergyStored());

        if (hasRecipe(pEntity)) {
            pEntity.energyStorage.extractEnergy(ENERGY_PER_TICK, false);
            pEntity.progress++;
            setChanged(level, pos, state);

            if (pEntity.progress >= pEntity.maxProgress) {
                craftItem(pEntity);
            }
        } else {
            pEntity.resetProgress();
            setChanged(level, pos, state);
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private static boolean hasRecipe(StellarCrusherBlockEntity pEntity) {
        Level level = pEntity.level;
        SimpleContainer inventory = new SimpleContainer(pEntity.itemHandler.getSlots());
        for (int i = 0; i < pEntity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, pEntity.itemHandler.getStackInSlot(i));
        }

        Optional<StellarCrusherRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(StellarCrusherRecipe.Type.INSTANCE, inventory, level);

        if (recipe.isPresent()) {
            StellarCrusherRecipe r = recipe.get();

            // 個数チェック
            boolean hasEnoughItems = inventory.getItem(0).getCount() >= r.getCount(0);
            // Tierチェック (スペルミス修正済み)
            boolean hasCorrectTier = MACHINE_TIER >= r.getTier();

            if (hasEnoughItems && hasCorrectTier &&
                    canInsertAmountIntoOutputSlot(inventory) &&
                    canInsertItemIntoOutputSlot(inventory, r.getResultItem(level.registryAccess()))) {

                pEntity.maxProgress = r.getProcessTime();
                return true;
            }
        }

        return false;
    }

    private static void craftItem(StellarCrusherBlockEntity pEntity) {
        Level level = pEntity.level;
        SimpleContainer inventory = new SimpleContainer(pEntity.itemHandler.getSlots());
        for (int i = 0; i < pEntity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, pEntity.itemHandler.getStackInSlot(i));
        }

        Optional<StellarCrusherRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(StellarCrusherRecipe.Type.INSTANCE, inventory, level);

        if (recipe.isPresent()) {
            StellarCrusherRecipe r = recipe.get();
            ItemStack result = r.getResultItem(level.registryAccess());

            // 入力アイテムをレシピ指定数分消費
            pEntity.itemHandler.extractItem(0, r.getCount(0), false);

            // 出力スロットに合成
            pEntity.itemHandler.setStackInSlot(1, new ItemStack(result.getItem(),
                    pEntity.itemHandler.getStackInSlot(1).getCount() + result.getCount()));

            pEntity.resetProgress();
        }
    }

    private static boolean canInsertItemIntoOutputSlot(SimpleContainer inventory, ItemStack stack) {
        return inventory.getItem(1).getItem() == stack.getItem() || inventory.getItem(1).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleContainer inventory) {
        return inventory.getItem(1).getMaxStackSize() > inventory.getItem(1).getCount();
    }

    // カスタムエネルギー用クラス
    public static class ModEnergyStorage extends EnergyStorage {
        public ModEnergyStorage(int capacity, int maxTransfer) {
            super(capacity, maxTransfer, maxTransfer, 0);
        }
        public void setEnergy(int energy) { this.energy = energy; }
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) onEnergyChanged();
            return received;
        }
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) onEnergyChanged();
            return extracted;
        }
        public void onEnergyChanged() { }
    }
}