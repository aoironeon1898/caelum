package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.blocks.entities.StellarFurnaceBlockEntity;
import com.aoironeon1898.caelum.common.blocks.entities.StellarSynthesizerBlockEntity; // ★追加
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Caelum.MODID);

    // 恒星炉
    public static final RegistryObject<BlockEntityType<StellarFurnaceBlockEntity>> STELLAR_FURNACE_BE =
            BLOCK_ENTITIES.register("stellar_furnace_be",
                    () -> BlockEntityType.Builder.of(StellarFurnaceBlockEntity::new,
                            ModBlocks.STELLAR_FURNACE.get()).build(null));

    // ★★★ これを追加！ 星辰の注入器の脳みそ ★★★
    public static final RegistryObject<BlockEntityType<StellarSynthesizerBlockEntity>> STELLAR_SYNTHESIZER_BE =
            BLOCK_ENTITIES.register("stellar_synthesizer_be",
                    () -> BlockEntityType.Builder.of(StellarSynthesizerBlockEntity::new,
                            ModBlocks.STELLAR_SYNTHESIZER.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}