package com.aoironeon1898.caelum.common.content.machines.tile;

import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Combustion Cell — Phase 0 の発電機。
 *
 * 設計：「燃焼しない燃焼」。鉄の箱の中で燃料が「目覚める」だけ、炎は見えない。
 * 燃料1スロット、内部FE 50,000。
 * 燃焼中は 20 FE/tick を生成、6面に最大 200 FE/tick で出力可能。
 * GUIは現状なし。Phase 1完成時に正式GUI追加予定。
 * - 上面から hopper でfuel自動投入可
 * - 隣接の EnergyStorage / EnergyConduit にFE伝達
 */
public class CombustionCellBlockEntity extends BlockEntity {

    // 発電 80 FE/t = 機械2台(40FE)分、またはSynthesizer(60FE)1台＋余剰。
    // 容量10万で複数機械の供給ハブとして機能。出力800で4台同時供給可。
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int ENERGY_TRANSFER = 800;
    private static final int ENERGY_PER_TICK = 80;

    private final ItemStackHandler fuelInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return ForgeHooks.getBurnTime(stack, null) > 0;
        }
    };

    // 上面からのみ自動挿入を許可するラッパー（hopper連携想定）
    private final IItemHandler topInsertOnly = new IItemHandler() {
        @Override public int getSlots() { return fuelInventory.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return fuelInventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return fuelInventory.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // 抽出禁止（燃料は引き戻せない）
        }
        @Override public int getSlotLimit(int slot) { return fuelInventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return fuelInventory.isItemValid(slot, stack); }
    };

    // 発電専用ストレージ：外部からの受電は拒否(maxReceive=0)、出力のみ許可。
    // 発電は generate() で内部energyを直接増やす（receiveEnergyはcanReceive=falseで常に0を返すため）。
    private final GeneratorEnergyStorage energyStorage = new GeneratorEnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER);

    public class GeneratorEnergyStorage extends EnergyStorage {
        public GeneratorEnergyStorage(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract, 0);
        }
        /** 発電：内部バッファを直接増やす。受入れた量を返す。 */
        public int generate(int amount) {
            int accepted = Math.min(amount, capacity - energy);
            if (accepted > 0) {
                energy += accepted;
                setChanged();
            }
            return accepted;
        }
        public void setEnergy(int e) { this.energy = Math.max(0, Math.min(capacity, e)); }
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int r = super.extractEnergy(maxExtract, simulate);
            if (r > 0 && !simulate) setChanged();
            return r;
        }
    }

    private LazyOptional<IItemHandler> fuelCap = LazyOptional.of(() -> topInsertOnly);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyStorage);

    private int burnTimeRemaining = 0;
    private int currentItemBurnTime = 0;

    public CombustionCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_CELL_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionCellBlockEntity be) {
        if (level.isClientSide) return;

        // 1. 燃焼中ならエネルギー生成
        if (be.burnTimeRemaining > 0) {
            // バッファに空きがある分だけ発電。あふれる分はロスではなく燃焼を遅らせる方針（メンテフリー思想）
            int accepted = be.energyStorage.generate(ENERGY_PER_TICK);
            if (accepted > 0) {
                be.burnTimeRemaining--;
                be.setChanged();
            }
            // バッファ満杯のときは燃料を消費しない＝静寂に「待つ」
        } else {
            // 2. 燃料が残ってなければ新規燃焼開始
            ItemStack fuel = be.fuelInventory.getStackInSlot(0);
            if (!fuel.isEmpty() && be.energyStorage.getEnergyStored() < ENERGY_CAPACITY) {
                int burnTime = ForgeHooks.getBurnTime(fuel, null);
                if (burnTime > 0) {
                    be.burnTimeRemaining = burnTime;
                    be.currentItemBurnTime = burnTime;
                    ItemStack container = fuel.getCraftingRemainingItem();
                    fuel.shrink(1);
                    if (fuel.isEmpty() && !container.isEmpty()) {
                        be.fuelInventory.setStackInSlot(0, container);
                    }
                    be.setChanged();
                }
            }
        }

        // 3. 隣接ブロックへ電力を吐き出す（Conduitや他のEnergyStorage含む）
        if (be.energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null) continue;
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(dst -> {
                    if (!dst.canReceive()) return;
                    int avail = be.energyStorage.extractEnergy(ENERGY_TRANSFER, true);
                    if (avail <= 0) return;
                    int pushed = dst.receiveEnergy(avail, false);
                    if (pushed > 0) be.energyStorage.extractEnergy(pushed, false);
                });
            }
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            // 上面と上から落とすhopperのみ燃料挿入を許可
            if (side == Direction.UP || side == null) return fuelCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fuelCap.invalidate();
        energyCap.invalidate();
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(1);
        inv.setItem(0, fuelInventory.getStackInSlot(0));
        Containers.dropContents(level, worldPosition, inv);
    }

    public boolean isBurning() { return burnTimeRemaining > 0; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getEnergyCapacity() { return ENERGY_CAPACITY; }
    public ItemStack getFuelStack() { return fuelInventory.getStackInSlot(0); }

    // 右クリックで燃料投入（GUIなしの簡易UI）
    public boolean tryInsertFuel(ItemStack hand) {
        if (hand.isEmpty()) return false;
        if (ForgeHooks.getBurnTime(hand, null) <= 0) return false;
        ItemStack remainder = fuelInventory.insertItem(0, hand.copy(), false);
        int consumed = hand.getCount() - remainder.getCount();
        if (consumed > 0) {
            hand.shrink(consumed);
            return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fuel", fuelInventory.serializeNBT());
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("BurnTime", burnTimeRemaining);
        tag.putInt("CurrentBurn", currentItemBurnTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Fuel")) fuelInventory.deserializeNBT(tag.getCompound("Fuel"));
        if (tag.contains("Energy")) energyStorage.setEnergy(tag.getInt("Energy"));
        burnTimeRemaining = tag.getInt("BurnTime");
        currentItemBurnTime = tag.getInt("CurrentBurn");
    }
}
