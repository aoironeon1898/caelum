package com.aoironeon1898.caelum.common.kernel;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.logic.grid.GridTopologyManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ★重要: Bus = Forge になっています。ClientModEventsとはバスが違います。
@Mod.EventBusSubscriber(modid = Caelum.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CaelumEvents {

    private static int debugTimer = 0;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // サーバー側のTickの終わり(END)に処理を実行
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
            if (event.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {

                // --- 動作確認用ログ (200tick = 10秒ごとに表示) ---
                debugTimer++;
                if (debugTimer >= 200) {
                    // System.out.println("DEBUG: Caelum Tick is running at " + serverLevel.dimension().location());
                    debugTimer = 0;
                }

                // 1. ネットワーク（グラフ）の更新
                GridTopologyManager.tick(serverLevel);

                // 2. パイプ・マシンの実行 (BatchController)
                BatchController.tickLevel(serverLevel);
            }
        }
    }
}