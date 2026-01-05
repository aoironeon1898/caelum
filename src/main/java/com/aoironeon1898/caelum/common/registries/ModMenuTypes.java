package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.content.logistics.menus.CompositePipeMenu; // ★インポート追加
import com.aoironeon1898.caelum.common.content.machines.menus.StellarCrusherMenu;
import com.aoironeon1898.caelum.common.content.machines.menus.StellarFurnaceMenu;
import com.aoironeon1898.caelum.common.content.machines.menus.StellarSynthesizerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Caelum.MODID);

    // --- 既存の機械 ---
    public static final RegistryObject<MenuType<StellarFurnaceMenu>> STELLAR_FURNACE_MENU =
            MENUS.register("stellar_furnace_menu",
                    () -> IForgeMenuType.create(StellarFurnaceMenu::new));

    public static final RegistryObject<MenuType<StellarSynthesizerMenu>> STELLAR_SYNTHESIZER_MENU =
            MENUS.register("stellar_synthesizer_menu",
                    () -> IForgeMenuType.create(StellarSynthesizerMenu::new));

    public static final RegistryObject<MenuType<StellarCrusherMenu>> STELLAR_CRUSHER_MENU =
            MENUS.register("stellar_crusher_menu",
                    () -> IForgeMenuType.create(StellarCrusherMenu::new));

    public static final RegistryObject<MenuType<CompositePipeMenu>> COMPOSITE_PIPE_MENU =
            MENUS.register("composite_pipe_menu",
                    () -> IForgeMenuType.create(CompositePipeMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}