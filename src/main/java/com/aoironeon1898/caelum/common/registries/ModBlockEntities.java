package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
// ★追加: 複合パイプのBlockEntityをインポート
import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.content.machines.entities.StellarCrusherBlockEntity;
import com.aoironeon1898.caelum.common.content.machines.entities.StellarFurnaceBlockEntity;
import com.aoironeon1898.caelum.common.content.machines.entities.StellarSynthesizerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Caelum.MODID);


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

    // ★追加: 複合パイプの登録
    // ModBlocks.COMPOSITE_PIPE と紐付けます
    public static final RegistryObject<BlockEntityType<CompositePipeBlockEntity>> COMPOSITE_PIPE =
            BLOCK_ENTITIES.register("composite_pipe",
                    () -> BlockEntityType.Builder.of(
                            CompositePipeBlockEntity::new,
                            ModBlocks.COMPOSITE_PIPE.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}