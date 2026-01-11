package com.aoironeon1898.caelum.common.events;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.kernel.BatchController;
import com.aoironeon1898.caelum.common.logic.grid.GridTopologyManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Caelum.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CaelumEvents {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // サーバー側のTick終了時のみ実行
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
            if (event.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // 1. ネットワーク更新
                GridTopologyManager.tick(serverLevel);
                // 2. パイプ処理実行
                BatchController.tickLevel(serverLevel);
            }
        }
    }
}