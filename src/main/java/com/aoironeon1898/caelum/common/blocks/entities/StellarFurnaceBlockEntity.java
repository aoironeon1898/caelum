package com.aoironeon1898.caelum.common.blocks.entities;

import com.aoironeon1898.caelum.common.blocks.StellarFurnaceBlock;
import com.aoironeon1898.caelum.common.menus.StellarFurnaceMenu;
import com.aoironeon1898.caelum.common.recipes.StellarFurnaceRecipe;
import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
import com.aoironeon1898.caelum.common.registries.ModRecipes;
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

public class StellarFurnaceBlockEntity extends BlockEntity implements MenuProvider {

    // 定数定義（管理しやすくするため上部にまとめています）
    private static final int MACHINE_TIER = 1;
    private static final int ENERGY_CAPACITY = 60000;
    private static final int ENERGY_TRANSFER = 200;
    private static final int ENERGY_PER_TICK = 40;

    // アイテムハンドラー（スロット管理）
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // エネルギーハンドラー（電力管理）
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
    private int maxProgress = 72;

    public StellarFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STELLAR_FURNACE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> StellarFurnaceBlockEntity.this.progress;
                    case 1 -> StellarFurnaceBlockEntity.this.maxProgress;
                    case 2 -> StellarFurnaceBlockEntity.this.energyStorage.getEnergyStored();
                    case 3 -> StellarFurnaceBlockEntity.this.energyStorage.getMaxEnergyStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> StellarFurnaceBlockEntity.this.progress = pValue;
                    case 1 -> StellarFurnaceBlockEntity.this.maxProgress = pValue;
                    case 2 -> StellarFurnaceBlockEntity.this.energyStorage.setEnergy(pValue);
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
        return Component.translatable("block.caelum.stellar_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new StellarFurnaceMenu(pContainerId, pPlayerInventory, this, this.data);
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
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt("stellar_furnace.progress", progress);
        nbt.putInt("energy", energyStorage.getEnergyStored());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        progress = nbt.getInt("stellar_furnace.progress");
        energyStorage.setEnergy(nbt.getInt("energy"));
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // ★★★ メイン処理 (Tick) ★★★
    public static void tick(Level level, BlockPos pos, BlockState state, StellarFurnaceBlockEntity pEntity) {
        if (level.isClientSide()) {
            return;
        }

        // ▼▼▼▼▼▼▼▼▼▼ デバッグ用：常にエネルギー満タン ▼▼▼▼▼▼▼▼▼▼
        pEntity.energyStorage.setEnergy(pEntity.energyStorage.getMaxEnergyStored());
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        boolean hasRecipe = hasRecipe(pEntity);
        boolean hasEnergy = pEntity.energyStorage.getEnergyStored() >= ENERGY_PER_TICK;
        boolean isWorking = hasRecipe && hasEnergy;

        // 稼働中ならブロックを光らせる（LITプロパティの更新）
        if (state.hasProperty(StellarFurnaceBlock.LIT)) {
            boolean currentLit = state.getValue(StellarFurnaceBlock.LIT);
            if (currentLit != isWorking) {
                level.setBlock(pos, state.setValue(StellarFurnaceBlock.LIT, isWorking), 3);
            }
        }

        if (isWorking) {
            // エネルギー消費
            pEntity.energyStorage.extractEnergy(ENERGY_PER_TICK, false);
            pEntity.progress++;
            setChanged(level, pos, state);

            if (pEntity.progress >= pEntity.maxProgress) {
                craftItem(pEntity);
            }
        } else {
            // 途中停止したらリセット
            if (pEntity.progress > 0) {
                pEntity.resetProgress();
                setChanged(level, pos, state);
            }
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    // レシピがあるかチェック
    private static boolean hasRecipe(StellarFurnaceBlockEntity entity) {
        Level level = entity.level;
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
        for (int i = 0; i < entity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }

        Optional<StellarFurnaceRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.STELLAR_SMELTING_TYPE.get(), inventory, level);

        if (recipe.isEmpty() || recipe.get().getTier() > MACHINE_TIER) {
            return false;
        }

        if (inventory.getItem(0).getCount() < recipe.get().getCount(0)) {
            return false;
        }

        // 調理時間をレシピから取得してセット
        entity.maxProgress = recipe.get().getCookingTime();

        return canInsertAmountIntoOutputSlot(inventory) &&
                canInsertItemIntoOutputSlot(inventory, recipe.get().getResultItem(level.registryAccess()));
    }

    // アイテム作成処理
    private static void craftItem(StellarFurnaceBlockEntity entity) {
        Level level = entity.level;
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
        for (int i = 0; i < entity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }

        Optional<StellarFurnaceRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.STELLAR_SMELTING_TYPE.get(), inventory, level);

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().getResultItem(level.registryAccess());
            ItemStack output = entity.itemHandler.getStackInSlot(1);
            ItemStack input = entity.itemHandler.getStackInSlot(0);

            if (output.isEmpty()) {
                entity.itemHandler.setStackInSlot(1, result.copy());
            } else {
                output.grow(result.getCount());
            }

            input.shrink(recipe.get().getCount(0));
            entity.resetProgress();
        }
    }

    private static boolean canInsertItemIntoOutputSlot(SimpleContainer inventory, ItemStack result) {
        return inventory.getItem(1).getItem() == result.getItem() || inventory.getItem(1).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleContainer inventory) {
        return inventory.getItem(1).getMaxStackSize() > inventory.getItem(1).getCount();
    }

    // カスタムエネルギーストレージ（変更検知付き）
    public static class ModEnergyStorage extends EnergyStorage {
        public ModEnergyStorage(int capacity, int maxTransfer) {
            super(capacity, maxTransfer, maxTransfer, 0);
        }

        // エネルギーを直接セットするメソッド（デバッグや同期用）
        public void setEnergy(int energy) {
            this.energy = energy;
        }

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

        public void onEnergyChanged() {}
    }
}