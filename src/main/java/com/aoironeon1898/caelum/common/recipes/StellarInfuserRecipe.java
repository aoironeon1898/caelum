package com.aoironeon1898.caelum.common.recipes;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.registries.ModRecipes;
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

public class StellarInfuserRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    private final NonNullList<Integer> recipeCounts; // 個数リスト
    private final int processTime;
    private final int tier; // ★Tier追加

    public StellarInfuserRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems, NonNullList<Integer> recipeCounts, int processTime, int tier) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        this.recipeCounts = recipeCounts;
        this.processTime = processTime;
        this.tier = tier;
    }

    public int getProcessTime() {
        return processTime;
    }

    // ★Tier取得メソッド
    public int getTier() {
        return tier;
    }

    public int getCount(int index) {
        if (index >= 0 && index < recipeCounts.size()) {
            return recipeCounts.get(index);
        }
        return 1;
    }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if(pLevel.isClientSide()) {
            return false;
        }
        return recipeItems.get(0).test(pContainer.getItem(0)) &&
                recipeItems.get(1).test(pContainer.getItem(1));
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
        return ModRecipes.STELLAR_INFUSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.STELLAR_INFUSING_TYPE.get();
    }

    public static class Type implements RecipeType<StellarInfuserRecipe> {
        private Type() { }
        public static final Type INSTANCE = new Type();
        public static final String ID = "stellar_infusing";
    }

    public static class Serializer implements RecipeSerializer<StellarInfuserRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Caelum.MODID, "stellar_infusing");

        @Override
        public StellarInfuserRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));

            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(2, Ingredient.EMPTY);
            NonNullList<Integer> counts = NonNullList.withSize(2, 1);

            for (int i = 0; i < inputs.size(); i++) {
                JsonElement element = ingredients.get(i);
                inputs.set(i, Ingredient.fromJson(element));

                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("count")) {
                        counts.set(i, GsonHelper.getAsInt(obj, "count"));
                    }
                }
            }

            int processTime = GsonHelper.getAsInt(pSerializedRecipe, "process_time", 78);
            // ★Tier読み込み (デフォルト1)
            int tier = GsonHelper.getAsInt(pSerializedRecipe, "tier", 1);

            return new StellarInfuserRecipe(pRecipeId, output, inputs, counts, processTime, tier);
        }

        @Override
        public @Nullable StellarInfuserRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            int size = pBuffer.readInt(); // 材料の数
            NonNullList<Ingredient> inputs = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                inputs.set(i, Ingredient.fromNetwork(pBuffer));
            }

            NonNullList<Integer> counts = NonNullList.withSize(size, 1);
            for (int i = 0; i < size; i++) {
                counts.set(i, pBuffer.readInt());
            }

            ItemStack output = pBuffer.readItem();
            int processTime = pBuffer.readInt();
            // ★Tier読み込み
            int tier = pBuffer.readInt();

            return new StellarInfuserRecipe(pRecipeId, output, inputs, counts, processTime, tier);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, StellarInfuserRecipe pRecipe) {
            pBuffer.writeInt(pRecipe.recipeItems.size());
            for (Ingredient ing : pRecipe.recipeItems) {
                ing.toNetwork(pBuffer);
            }

            for (Integer count : pRecipe.recipeCounts) {
                pBuffer.writeInt(count);
            }

            pBuffer.writeItemStack(pRecipe.getResultItem(null), false);
            pBuffer.writeInt(pRecipe.processTime);
            // ★Tier書き込み
            pBuffer.writeInt(pRecipe.tier);
        }
    }
}