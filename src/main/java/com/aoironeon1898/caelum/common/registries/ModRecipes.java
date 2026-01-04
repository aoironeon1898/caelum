package com.aoironeon1898.caelum.common.registries;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.recipes.StellarCrusherRecipe;
import com.aoironeon1898.caelum.common.recipes.StellarFurnaceRecipe;
import com.aoironeon1898.caelum.common.recipes.StellarInfuserRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Caelum.MODID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Caelum.MODID);

    // ★ レシピの読み込み機（Serializer）の登録
    public static final RegistryObject<RecipeSerializer<StellarFurnaceRecipe>> STELLAR_SMELTING_SERIALIZER =
            SERIALIZERS.register("stellar_smelting", () -> StellarFurnaceRecipe.Serializer.INSTANCE);

    // ★ レシピの種類（Type）の登録
    public static final RegistryObject<RecipeType<StellarFurnaceRecipe>> STELLAR_SMELTING_TYPE =
            TYPES.register("stellar_smelting", () -> StellarFurnaceRecipe.Type.INSTANCE);

    // ★追加: 注入機のレシピシリアライザー
    public static final RegistryObject<RecipeSerializer<StellarInfuserRecipe>> STELLAR_INFUSING_SERIALIZER =
            SERIALIZERS.register("stellar_infusing", () -> StellarInfuserRecipe.Serializer.INSTANCE);

    // ★追加: 注入機のレシピタイプ
    public static final RegistryObject<RecipeType<StellarInfuserRecipe>> STELLAR_INFUSING_TYPE =
            TYPES.register("stellar_infusing", () -> StellarInfuserRecipe.Type.INSTANCE);

    public static final RegistryObject<RecipeSerializer<StellarCrusherRecipe>> STELLAR_CRUSHING_SERIALIZER =
            SERIALIZERS.register("stellar_crushing", () -> StellarCrusherRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<StellarCrusherRecipe>> STELLAR_CRUSHING_TYPE =
            TYPES.register("stellar_crushing", () -> StellarCrusherRecipe.Type.INSTANCE);

    
    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}