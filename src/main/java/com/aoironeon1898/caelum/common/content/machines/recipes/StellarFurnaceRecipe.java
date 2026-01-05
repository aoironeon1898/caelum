package com.aoironeon1898.caelum.common.content.machines.recipes;

import com.aoironeon1898.caelum.Caelum;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class StellarFurnaceRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    // ★追加: 入力アイテムの個数リスト
    private final NonNullList<Integer> recipeCounts;
    private final int cookingTime;
    private final int tier;

    public StellarFurnaceRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems, NonNullList<Integer> recipeCounts, int cookingTime, int tier) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        this.recipeCounts = recipeCounts;
        this.cookingTime = cookingTime;
        this.tier = tier;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public int getTier() {
        return tier;
    }

    // ★追加: 必要な個数を取得するメソッド
    public int getCount(int index) {
        if (index >= 0 && index < recipeCounts.size()) {
            return recipeCounts.get(index);
        }
        return 1; // デフォルトは1個
    }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if(pLevel.isClientSide()) {
            return false;
        }
        // ここではアイテムの種類だけ簡易チェック (個数チェックはBlockEntityで行う)
        return recipeItems.get(0).test(pContainer.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<StellarFurnaceRecipe> {
        private Type() { }
        public static final Type INSTANCE = new Type();
        public static final String ID = "stellar_smelting";
    }

    public static class Serializer implements RecipeSerializer<StellarFurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(Caelum.MODID, "stellar_smelting");

        @Override
        public StellarFurnaceRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));

            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);
            // ★ 個数リストの初期化 (デフォルト1個)
            NonNullList<Integer> counts = NonNullList.withSize(1, 1);

            for (int i = 0; i < inputs.size(); i++) {
                JsonElement element = ingredients.get(i);

                // 1. Ingredient読み込み
                inputs.set(i, Ingredient.fromJson(element));

                // 2. count読み込み
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("count")) {
                        counts.set(i, GsonHelper.getAsInt(obj, "count"));
                    }
                }
            }

            int cookingTime = GsonHelper.getAsInt(pSerializedRecipe, "cookingtime", 72);
            int tier = GsonHelper.getAsInt(pSerializedRecipe, "tier", 1);

            return new StellarFurnaceRecipe(pRecipeId, output, inputs, counts, cookingTime, tier);
        }

        @Override
        public @Nullable StellarFurnaceRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            int size = pBuffer.readInt();
            NonNullList<Ingredient> inputs = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                inputs.set(i, Ingredient.fromNetwork(pBuffer));
            }

            // ★ ネットワークから個数読み込み
            NonNullList<Integer> counts = NonNullList.withSize(size, 1);
            for (int i = 0; i < size; i++) {
                counts.set(i, pBuffer.readInt());
            }

            ItemStack output = pBuffer.readItem();
            int cookingTime = pBuffer.readInt();
            int tier = pBuffer.readInt();

            return new StellarFurnaceRecipe(pRecipeId, output, inputs, counts, cookingTime, tier);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, StellarFurnaceRecipe pRecipe) {
            pBuffer.writeInt(pRecipe.recipeItems.size());
            for (Ingredient ingredient : pRecipe.recipeItems) {
                ingredient.toNetwork(pBuffer);
            }

            // ★ ネットワークへ個数書き込み
            for (Integer count : pRecipe.recipeCounts) {
                pBuffer.writeInt(count);
            }

            pBuffer.writeItemStack(pRecipe.getResultItem(null), false);
            pBuffer.writeInt(pRecipe.cookingTime);
            pBuffer.writeInt(pRecipe.tier);
        }
    }
}