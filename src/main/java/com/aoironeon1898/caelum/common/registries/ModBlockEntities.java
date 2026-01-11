package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
// マシンのインポートパスはご提示のファイルに合わせました
import com.aoironeon1898.caelum.common.content.machines.tile.StellarCrusherBlockEntity;
import com.aoironeon1898.caelum.common.content.machines.tile.StellarFurnaceBlockEntity;
import com.aoironeon1898.caelum.common.content.machines.tile.StellarSynthesizerBlockEntity;
// パイプのインポート (tileパッケージ)
import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Caelum.MODID);

    // =================================================================
    // Section: Machines (加工機械)
    // =================================================================

    public static final RegistryObject<BlockEntityType<StellarFurnaceBlockEntity>> STELLAR_FURNACE_BE =
            BLOCK_ENTITIES.register("stellar_furnace_be",
                    () -> BlockEntityType.Builder.of(StellarFurnaceBlockEntity::new,
                            ModBlocks.STELLAR_FURNACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<StellarSynthesizerBlockEntity>> STELLAR_SYNTHESIZER_BE =
            BLOCK_ENTITIES.register("stellar_synthesizer_be",
                    () -> BlockEntityType.Builder.of(StellarSynthesizerBlockEntity::new,
                            ModBlocks.STELLAR_SYNTHESIZER.get()).build(null));

    public static final RegistryObject<BlockEntityType<StellarCrusherBlockEntity>> STELLAR_CRUSHER_BE =
            BLOCK_ENTITIES.register("stellar_crusher_be",
                    () -> BlockEntityType.Builder.of(StellarCrusherBlockEntity::new,
                            ModBlocks.STELLAR_CRUSHER.get()).build(null));

    // =================================================================
    // Section: Logistics (物流システム)
    // =================================================================

    // ★修正: 変数名を COMPOSITE_PIPE_BE から COMPOSITE_PIPE に変更
    // (CompositePipeBlockEntity.java のコンストラクタが呼んでいる名前と一致させる)
    public static final RegistryObject<BlockEntityType<CompositePipeBlockEntity>> COMPOSITE_PIPE =
            BLOCK_ENTITIES.register("composite_pipe",
                    () -> BlockEntityType.Builder.of(CompositePipeBlockEntity::new,
                            ModBlocks.COMPOSITE_PIPE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}