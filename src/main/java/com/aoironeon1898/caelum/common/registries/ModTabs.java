package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Caelum.MODID);

    public static final RegistryObject<CreativeModeTab> CAELUM_TAB = CREATIVE_MODE_TABS.register("caelum_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.ASTRAL_INGOT.get()))
                    .title(Component.translatable("creativetab.caelum_tab"))
                    .displayItems((pParameters, pOutput) -> {

                        // --- アイテム類 (ModItems から取得) ---
                        // これらは純粋なアイテムなので ModItems のままでOK
                        pOutput.accept(ModItems.RAW_ASTRAL_ORE.get());
                        pOutput.accept(ModItems.ASTRAL_INGOT.get());
                        //pOutput.accept(ModItems.ASTRAL_DUST.get());
                        //pOutput.accept(ModItems.LOGIC_CHIP.get());

                        // --- ブロック類 (ModBlocks から取得) ---
                        // ★ ここを修正しました！ ModItems ではなく ModBlocks を使うように変更
                        pOutput.accept(ModBlocks.ASTRAL_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_ASTRAL_ORE.get());
                        pOutput.accept(ModBlocks.STELLAR_FURNACE.get());
                        pOutput.accept(ModBlocks.STELLAR_SYNTHESIZER.get());

                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}