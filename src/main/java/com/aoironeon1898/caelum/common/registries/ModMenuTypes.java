package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.menus.StellarFurnaceMenu;
import com.aoironeon1898.caelum.common.menus.StellarSynthesizerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Caelum.MODID);

    // ★ メニューの登録
    // IForgeMenuType.create を使うことで、ブロックの位置情報などをクライアントに送れます
    public static final RegistryObject<MenuType<StellarFurnaceMenu>> STELLAR_FURNACE_MENU =
            MENUS.register("stellar_furnace_menu",
                    () -> IForgeMenuType.create(StellarFurnaceMenu::new));

    public static final RegistryObject<MenuType<StellarSynthesizerMenu>> STELLAR_SYNTHESIZER_MENU =
            MENUS.register("stellar_synthesizer_menu",
                    () -> IForgeMenuType.create(StellarSynthesizerMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}