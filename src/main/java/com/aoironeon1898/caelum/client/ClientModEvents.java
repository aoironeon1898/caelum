package com.aoironeon1898.caelum.client;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.client.screens.machines.StellarCrusherScreen;
import com.aoironeon1898.caelum.client.screens.machines.StellarFurnaceScreen;
import com.aoironeon1898.caelum.client.screens.machines.StellarSynthesizerScreen;
import com.aoironeon1898.caelum.client.screens.logistics.CompositePipeScreen;
import com.aoironeon1898.caelum.common.registries.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Caelum.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.STELLAR_FURNACE_MENU.get(), StellarFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.STELLAR_SYNTHESIZER_MENU.get(), StellarSynthesizerScreen::new);
            MenuScreens.register(ModMenuTypes.STELLAR_CRUSHER_MENU.get(), StellarCrusherScreen::new);
            MenuScreens.register(ModMenuTypes.COMPOSITE_PIPE_MENU.get(), CompositePipeScreen::new);
        });
    }
}