package com.aoironeon1898.caelum.common.base;

import com.aoironeon1898.caelum.common.kernel.BatchController;
import com.aoironeon1898.caelum.common.kernel.IBatchTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CaelumBlockEntity extends BlockEntity implements IBatchTask {

    private float tickAccumulator = 0.0f;
    private static final int MAX_SIMULATION_STEPS = 10;

    // エネルギー機能はオプショナルに変更（パイプには不要なため）
    protected CustomEnergyStorage energyStorage;
    protected LazyOptional<IEnergyStorage> energyCap;

    // ★重要: コンストラクタから tier 引数を削除
    public CaelumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // エネルギーの初期化は onLoad や コンストラクタ内での判定に委ねる
        // ここではnullのままにしておき、必要なクラスだけが初期化する
    }

    // ★抽象メソッド: 全サブクラスは自分のTierを返すこと
    public abstract MachineTier getMachineTier();

    // ★抽象メソッド: ロジック実行 (引数あり版に統一)
    protected abstract boolean runLogic(float efficiencyMultiplier);

    // --- Kernel Integration ---

    @Override
    public boolean isInvalidOrUnloaded() {
        return this.isRemoved() || (level != null && !level.isLoaded(this.worldPosition));
    }

    @Override
    public boolean shouldRun() {
        return true;
    }

    @Override
    public void accumulateAndRun(float kernelMultiplier) {
        this.tickAccumulator += kernelMultiplier;

        // 自分のTierから速度倍率を取得
        float machineSpeed = getMachineTier().getSpeedMultiplier();

        int steps = 0;
        while (this.tickAccumulator >= 1.0f && steps < MAX_SIMULATION_STEPS) {

            // ★統一されたメソッド呼び出し
            boolean workDone = runLogic(machineSpeed);

            this.tickAccumulator -= 1.0f;
            steps++;

            if (!workDone) {
                this.tickAccumulator = 0;
                break;
            }
        }

        if (this.tickAccumulator > MAX_SIMULATION_STEPS) {
            this.tickAccumulator = 0;
        }
    }

    // --- Lifecycle ---

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            BatchController.register(this, level, worldPosition);
        }
        // エネルギーが必要なマシンならここで初期化してもよい
        if (this.energyStorage == null && getMachineTier().getEnergyCapacity() > 0) {
            initEnergy();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            BatchController.unregister(level, worldPosition);
        }
        if (energyCap != null) energyCap.invalidate();
    }

    protected void initEnergy() {
        this.energyStorage = new CustomEnergyStorage(getMachineTier().getEnergyCapacity());
        this.energyCap = LazyOptional.of(() -> energyStorage);
    }

    // --- Capability & Energy ---

    public class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity) { super(capacity); }
        public void setEnergy(int energy) { this.energy = energy; setChanged(); }
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
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && energyCap != null) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    // --- Persistence ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (energyStorage != null) {
            tag.putInt("Energy", energyStorage.getEnergyStored());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (energyStorage != null) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        } else if (tag.contains("Energy") && getMachineTier().getEnergyCapacity() > 0) {
            // ロード時にエネルギーデータがあり、まだ初期化されていなければ初期化
            initEnergy();
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}