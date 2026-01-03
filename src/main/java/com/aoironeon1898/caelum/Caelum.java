package com.aoironeon1898.caelum;

import com.aoironeon1898.caelum.common.registries.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Caelum.MODID)
public class Caelum {
    public static final String MODID = "caelum";
    private static final Logger LOGGER = LogUtils.getLogger();

    // ★ 修正ポイント: 引数に context を受け取るように変更
    // これを「コンストラクタ インジェクション（依存性の注入）」と呼びます
    public Caelum(FMLJavaModLoadingContext context) {
        // .get() を使わず、渡された context からバスを取り出す
        IEventBus modEventBus = context.getModEventBus();

        // ★ 各種登録処理（レジストリ）
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModTabs.register(modEventBus);

        // ★ イベントリスナーの登録
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 共通のセットアップ処理
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // クリエイティブタブへの追加処理
    }
}