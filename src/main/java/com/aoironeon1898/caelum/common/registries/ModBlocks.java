package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.blocks.StellarFurnaceBlock;
import com.aoironeon1898.caelum.common.blocks.StellarSynthesizerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor; // 追加
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Caelum.MODID);

    // --- ブロックの登録 ---

    public static final RegistryObject<Block> ASTRAL_ORE = registerBlock("astral_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_ASTRAL_ORE = registerBlock("deepslate_astral_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(4.5f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> STELLAR_FURNACE = registerBlock("stellar_furnace",
            () -> new StellarFurnaceBlock(BlockBehaviour.Properties.of() // ★ copyをやめて of() にする
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(1.5f)
                    .noOcclusion()
            ));

    public static final RegistryObject<Block> STELLAR_SYNTHESIZER = registerBlock("stellar_synthesizer",
            () -> new StellarSynthesizerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(1.5f)
                    .noOcclusion()
            ));

    // --- 登録用のヘルパーメソッド ---

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        // ★ ModItems.ITEMS が null でないことを前提にしています
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}