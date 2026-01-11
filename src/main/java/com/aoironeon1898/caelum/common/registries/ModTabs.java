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
                        // 1. Resources (素材)
                        pOutput.accept(ModItems.RAW_ASTRAL_ORE.get());
                        pOutput.accept(ModItems.ASTRAL_INGOT.get());
                        pOutput.accept(ModItems.ASTRAL_DUST.get());
                        pOutput.accept(ModItems.LOGIC_CHIP.get());

                        // 2. Blocks (自然生成ブロック)
                        pOutput.accept(ModBlocks.ASTRAL_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_ASTRAL_ORE.get());

                        // 3. Machines (機械)
                        pOutput.accept(ModBlocks.STELLAR_FURNACE.get());
                        pOutput.accept(ModBlocks.STELLAR_CRUSHER.get());
                        pOutput.accept(ModBlocks.STELLAR_SYNTHESIZER.get());

                        // 4. Logistics (物流)
                        pOutput.accept(ModBlocks.COMPOSITE_PIPE.get());

                        //5. tools
                        pOutput.accept(ModItems.CAELUM_WRENCH.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}