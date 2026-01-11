package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.content.tools.WrenchItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.aoironeon1898.caelum.common.registries.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
    // レジストリ（台帳）の作成
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Caelum.MODID);

    // --- 純粋なアイテムだけを登録します ---

    // 1. アストラルインゴット
    public static final RegistryObject<Item> ASTRAL_INGOT = ITEMS.register("astral_ingot",
            () -> new Item(new Item.Properties()));

    // 2. アストラルダスト
    public static final RegistryObject<Item> ASTRAL_DUST = ITEMS.register("astral_dust",
            () -> new Item(new Item.Properties()));

    // 3. ロジックチップ
    public static final RegistryObject<Item> LOGIC_CHIP = ITEMS.register("logic_chip",
            () -> new Item(new Item.Properties()));

    // 4. アストラル原石（ブロックではなくアイテムなのでここに書く）
    public static final RegistryObject<Item> RAW_ASTRAL_ORE = ITEMS.register("raw_astral_ore",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COMPOSITE_PIPE = ITEMS.register("composite_pipe",
            () -> new BlockItem(ModBlocks.COMPOSITE_PIPE.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAELUM_WRENCH = ITEMS.register("caelum_wrench",
            () -> new WrenchItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}