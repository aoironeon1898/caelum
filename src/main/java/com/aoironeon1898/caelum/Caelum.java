package com.aoironeon1898.caelum;

import com.aoironeon1898.caelum.common.network.ModMessages; // ★ここ重要
import com.aoironeon1898.caelum.common.registries.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Caelum.MODID) // ここで MODID を使っています
public class Caelum {
    // ★修正: 変数名を 'MOD_ID' から 'MODID' に変更しました
    // これで ClientModEvents からのエラーが消えます
    public static final String MODID = "caelum";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Caelum() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // --- レジストリ登録 ---
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // --- ★重要: ネットワークの登録 ---
        // PacketHandler ではなく、Screen側で使っている ModMessages を登録します
        ModMessages.register();

        // --- イベントリスナー ---
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 共通セットアップ
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // クリエイティブタブ
    }
}