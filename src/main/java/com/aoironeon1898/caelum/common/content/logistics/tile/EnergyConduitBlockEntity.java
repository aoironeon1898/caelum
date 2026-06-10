package com.aoironeon1898.caelum.common.content.logistics.tile;

import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Energy Conduit — 沈黙する電力線材。
 *
 * 設計方針：内部に小さなバッファ（1000FE）を持ち、毎tickで隣接ブロックから
 * 引き、隣接ブロックへ流す。GridGraphには載せず単純なnearest-neighborルーティング。
 * Phase 2でグリッド化検討。
 */
public class EnergyConduitBlockEntity extends BlockEntity {

    // 転送800/t は燃焼セルの出力(800)と同等。バッファ4000で短いリレー遅延を吸収。
    private static final int BUFFER_CAPACITY = 4000;
    private static final int TRANSFER_RATE = 800;

    private final EnergyStorage buffer = new EnergyStorage(BUFFER_CAPACITY, TRANSFER_RATE, TRANSFER_RATE) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int r = super.receiveEnergy(maxReceive, simulate);
            if (r > 0 && !simulate) setChanged();
            return r;
        }
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int r = super.extractEnergy(maxExtract, simulate);
            if (r > 0 && !simulate) setChanged();
            return r;
        }
    };

    private final LazyOptional<IEnergyStorage> bufferCap = LazyOptional.of(() -> buffer);

    public EnergyConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CONDUIT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyConduitBlockEntity be) {
        if (level.isClientSide) return;

        // 1. 隣接の電力源から吸い出し（自分のバッファに余裕がある分だけ）
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null || neighbor instanceof EnergyConduitBlockEntity) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(src -> {
                if (!src.canExtract()) return;
                int room = be.buffer.receiveEnergy(TRANSFER_RATE, true);
                if (room <= 0) return;
                int pulled = src.extractEnergy(room, false);
                if (pulled > 0) be.buffer.receiveEnergy(pulled, false);
            });
        }

        // 2. 隣接の受電可能ブロックへ吐き出し（他の Conduit へも流す＝ネットワーク伝播）
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(dst -> {
                if (!dst.canReceive()) return;
                int avail = be.buffer.extractEnergy(TRANSFER_RATE, true);
                if (avail <= 0) return;
                int pushed = dst.receiveEnergy(avail, false);
                if (pushed > 0) be.buffer.extractEnergy(pushed, false);
            });
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return bufferCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        bufferCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", buffer.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Energy")) {
            int e = tag.getInt("Energy");
            buffer.receiveEnergy(e, false);
        }
    }
}
