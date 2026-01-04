package com.aoironeon1898.caelum.common.recipes;

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

public class StellarCrusherRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    private final NonNullList<Integer> recipeCounts;
    private final int processTime;
    private final int tier;

    public StellarCrusherRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems,
                                NonNullList<Integer> recipeCounts, int processTime, int tier) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        this.recipeCounts = recipeCounts;
        this.processTime = processTime;
        this.tier = tier;
    }

    public int getProcessTime() { return processTime; }
    public int getTier() { return tier; }
    public int getCount(int index) { return recipeCounts.get(index); }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if(pLevel.isClientSide()) return false;
        return recipeItems.get(0).test(pContainer.getItem(0));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() { return recipeItems; }

    @Override
    public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) { return output.copy(); }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) { return true; }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) { return output.copy(); }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<StellarCrusherRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "stellar_crushing";
    }

    public static class Serializer implements RecipeSerializer<StellarCrusherRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Caelum.MODID, "stellar_crushing");

        @Override
        public StellarCrusherRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));
            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");

            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);
            NonNullList<Integer> counts = NonNullList.withSize(1, 1);

            for (int i = 0; i < inputs.size(); i++) {
                JsonElement element = ingredients.get(i);
                inputs.set(i, Ingredient.fromJson(element));
                if (element.isJsonObject() && element.getAsJsonObject().has("count")) {
                    counts.set(i, GsonHelper.getAsInt(element.getAsJsonObject(), "count"));
                }
            }

            int processTime = GsonHelper.getAsInt(pSerializedRecipe, "process_time", 72);
            int tier = GsonHelper.getAsInt(pSerializedRecipe, "tier", 1);

            return new StellarCrusherRecipe(pRecipeId, output, inputs, counts, processTime, tier);
        }

        @Override
        public @Nullable StellarCrusherRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            int size = pBuffer.readInt();
            NonNullList<Ingredient> inputs = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) inputs.set(i, Ingredient.fromNetwork(pBuffer));

            NonNullList<Integer> counts = NonNullList.withSize(size, 1);
            for (int i = 0; i < size; i++) counts.set(i, pBuffer.readInt());

            ItemStack output = pBuffer.readItem();
            int processTime = pBuffer.readInt();
            int tier = pBuffer.readInt();

            return new StellarCrusherRecipe(pRecipeId, output, inputs, counts, processTime, tier);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, StellarCrusherRecipe pRecipe) {
            pBuffer.writeInt(pRecipe.recipeItems.size());
            for (Ingredient ingredient : pRecipe.recipeItems) ingredient.toNetwork(pBuffer);
            for (Integer count : pRecipe.recipeCounts) pBuffer.writeInt(count);
            pBuffer.writeItemStack(pRecipe.output, false);
            pBuffer.writeInt(pRecipe.processTime);
            pBuffer.writeInt(pRecipe.tier);
        }
    }
}