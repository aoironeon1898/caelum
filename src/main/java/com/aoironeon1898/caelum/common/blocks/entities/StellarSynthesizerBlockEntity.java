package com.aoironeon1898.caelum.common.blocks.entities;

import com.aoironeon1898.caelum.common.blocks.StellarSynthesizerBlock;
import com.aoironeon1898.caelum.common.menus.StellarSynthesizerMenu;
import com.aoironeon1898.caelum.common.recipes.StellarInfuserRecipe;
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

public class StellarSynthesizerBlockEntity extends BlockEntity implements MenuProvider {

    // ★ Machine Tier (Set to 2 when making Tier 2 machines)
    private static final int MACHINE_TIER = 1;

    private static final int ENERGY_CAPACITY = 60000;
    private static final int ENERGY_TRANSFER = 200;
    private static final int ENERGY_PER_TICK = 60;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

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

    public StellarSynthesizerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.STELLAR_SYNTHESIZER_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> StellarSynthesizerBlockEntity.this.energyStorage.getEnergyStored();
                    case 1 -> StellarSynthesizerBlockEntity.this.energyStorage.getMaxEnergyStored();
                    case 2 -> StellarSynthesizerBlockEntity.this.progress;
                    case 3 -> StellarSynthesizerBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> StellarSynthesizerBlockEntity.this.energyStorage.setEnergy(pValue);
                    case 2 -> StellarSynthesizerBlockEntity.this.progress = pValue;
                    case 3 -> StellarSynthesizerBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StellarSynthesizerBlockEntity pEntity) {
        if (level.isClientSide()) {
            return;
        }

        // ★★★ DEBUG: Constantly refill energy to max ★★★
        pEntity.energyStorage.setEnergy(pEntity.energyStorage.getMaxEnergyStored());
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

        // 1. Get Recipe
        Optional<StellarInfuserRecipe> recipe = getCurrentRecipe(pEntity);

        // 2. Recipe Validity Check
        boolean hasRecipe = false;
        if (recipe.isPresent()) {
            StellarInfuserRecipe r = recipe.get();

            // ★ Tier Check: Only works if the machine tier is high enough for the recipe
            if (r.getTier() <= MACHINE_TIER) {

                // Item Count Check
                boolean hasEnoughItems =
                        pEntity.itemHandler.getStackInSlot(0).getCount() >= r.getCount(0) &&
                                pEntity.itemHandler.getStackInSlot(1).getCount() >= r.getCount(1);

                if (hasEnoughItems) {
                    // Output Slot Check
                    hasRecipe = canInsertAmountIntoOutputSlot(pEntity.itemHandler) &&
                            canInsertItemIntoOutputSlot(pEntity.itemHandler, r.getResultItem(level.registryAccess()));
                }
            }
        }

        boolean hasEnergy = pEntity.energyStorage.getEnergyStored() >= ENERGY_PER_TICK;
        boolean isWorking = hasRecipe && hasEnergy;

        // 3. Block Appearance Update (Lit state)
        if (state.hasProperty(StellarSynthesizerBlock.LIT)) {
            boolean currentLit = state.getValue(StellarSynthesizerBlock.LIT);
            if (currentLit != isWorking) {
                level.setBlock(pos, state.setValue(StellarSynthesizerBlock.LIT, isWorking), 3);
            }
        }

        // 4. Processing Logic
        if (isWorking) {
            pEntity.maxProgress = recipe.get().getProcessTime();
            pEntity.energyStorage.extractEnergy(ENERGY_PER_TICK, false);
            pEntity.progress++;
            setChanged(level, pos, state);

            if (pEntity.progress >= pEntity.maxProgress) {
                craftItem(pEntity);
            }
        } else {
            if (pEntity.progress > 0) {
                pEntity.resetProgress();
                setChanged(level, pos, state);
            }
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private static Optional<StellarInfuserRecipe> getCurrentRecipe(StellarSynthesizerBlockEntity entity) {
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
        for (int i = 0; i < entity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }
        return entity.level.getRecipeManager()
                .getRecipeFor(ModRecipes.STELLAR_INFUSING_TYPE.get(), inventory, entity.level);
    }

    private static void craftItem(StellarSynthesizerBlockEntity entity) {
        Optional<StellarInfuserRecipe> recipe = getCurrentRecipe(entity);

        // ★ Tier Check
        if (recipe.isPresent() && recipe.get().getTier() <= MACHINE_TIER) {
            StellarInfuserRecipe r = recipe.get();
            ItemStack result = r.getResultItem(entity.level.registryAccess());

            ItemStack output = entity.itemHandler.getStackInSlot(2);
            ItemStack input1 = entity.itemHandler.getStackInSlot(0);
            ItemStack input2 = entity.itemHandler.getStackInSlot(1);

            if (output.isEmpty()) {
                entity.itemHandler.setStackInSlot(2, result.copy());
            } else {
                output.grow(result.getCount());
            }

            // Consume Items (based on recipe count)
            input1.shrink(r.getCount(0));
            input2.shrink(r.getCount(1));

            entity.resetProgress();
        }
    }

    private static boolean canInsertItemIntoOutputSlot(ItemStackHandler inventory, ItemStack result) {
        return inventory.getStackInSlot(2).getItem() == result.getItem() || inventory.getStackInSlot(2).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(ItemStackHandler inventory) {
        return inventory.getStackInSlot(2).getMaxStackSize() > inventory.getStackInSlot(2).getCount();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.caelum.stellar_synthesizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new StellarSynthesizerMenu(pContainerId, pPlayerInventory, this, this.data);
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
        pTag.putInt("stellar_synthesizer.progress", progress);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        energyStorage.setEnergy(pTag.getInt("energy"));
        progress = pTag.getInt("stellar_synthesizer.progress");
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public static class ModEnergyStorage extends EnergyStorage {
        public ModEnergyStorage(int capacity, int maxTransfer) {
            super(capacity, maxTransfer, maxTransfer, 0);
        }
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
        public void onEnergyChanged() { }
    }
}