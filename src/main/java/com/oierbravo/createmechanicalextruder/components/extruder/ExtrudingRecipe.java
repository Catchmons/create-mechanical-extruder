package com.oierbravo.createmechanicalextruder.components.extruder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.oierbravo.createmechanicalextruder.CreateMechanicalExtruder;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.recipe.IRecipeTypeInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExtrudingRecipe implements Recipe<SimpleContainer>, IRecipeTypeInfo {
    private ResourceLocation id;
    private NonNullList<Ingredient> itemIngredients;
    private NonNullList<FluidIngredient> fluidIngredients;
    private ItemStack catalyst;

    private ItemStack result;
    public ExtrudingRecipe(ExtrudingRecipeBuilder.ExtrudingRecipeParams params) {
        this.id = params.id;
        this.result = params.result;
        this.itemIngredients = params.itemIngredients;
        this.fluidIngredients = params.fluidIngredients;
        this.catalyst = params.catalyst;
    }

    /*public static IRecipeTypeInfo getTypeInfo() {
        return new IRecipeTypeInfo() {
            @Override
            public ResourceLocation getId() {
                return Serializer.ID;
            }

            @Override
            public <T extends RecipeSerializer<?>> T getSerializer() {
                return Serializer;
            }



            @Override
            public <T extends RecipeType<?>> T getType() {
                return Type.class;
            }
        };
    }*/

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        return false;
    }

    public static boolean match(ExtruderTileEntity extruderTileEntity, ExtrudingRecipe recipe) {
        List<ItemStack> stack = extruderTileEntity.getItemStacks();

        if(recipe.getItemIngredients().size() != extruderTileEntity.getItemStacks().size())
            return false;
        if(!recipe.getItemIngredients().isEmpty()
                && !extruderTileEntity.getItemStacks().stream().allMatch(itemStack -> hasItemStack(itemStack,recipe.getItemIngredients() ))
        ) return false;

        List<FluidStack> fluids = extruderTileEntity.getFluidStacks();
        if(recipe.getFluidIngredients().size() != extruderTileEntity.getFluidStacks().size())
            return false;
        if(!recipe.getFluidIngredients().isEmpty()
                && !extruderTileEntity.getFluidStacks().stream().allMatch(fluidStack -> hasFluidStack(fluidStack,recipe.getFluidIngredients() ))
        ) return false;

        if(!recipe.catalyst.isEmpty() && !recipe.catalyst.is(extruderTileEntity.getCatalystItem()))
            return false;
        return true;
    }
    private static boolean hasItemStack(ItemStack itemStack,List<Ingredient> itemIngredients){
        if(itemStack.isEmpty())
            return false;
        boolean hasItemStack = false;
        for(int i = 0; i< itemIngredients.size();i++){
            if(itemIngredients.get(i).test(itemStack))
                hasItemStack = true;
        }
        return hasItemStack;
    }
    private static boolean hasFluidStack(FluidStack fluidStack, List<FluidIngredient> fluidIngredients){
        if(fluidStack.isEmpty())
            return false;
        boolean hasFluidStack = false;
        for(int i = 0; i< fluidIngredients.size();i++){
            if(fluidIngredients.get(i).test(fluidStack))
                hasFluidStack = true;
        }
        return hasFluidStack;
    }
    public List<Ingredient> getItemIngredients() {
        return itemIngredients;
    }

    public List<FluidIngredient> getFluidIngredients() {
        return fluidIngredients;
    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer) {
        return getResultItem();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }



    @Override
    public ItemStack getResultItem() {
        return result;
    }

    public ItemStack getCatalyst() {
        return catalyst;
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
    /*public static class TypeInfo implements IRecipeTypeInfo {
        @Override
        public ResourceLocation getId() {
            return Serializer.ID;
        }

        @Override
        public <T extends RecipeSerializer<?>> T getSerializer() {
            return  Serializer.class;
        }

        @Override
        public <T extends RecipeType<?>> T getType() {
            return null;
        }
    }*/
    public static class Type implements RecipeType<ExtrudingRecipe> {
        private Type() { }
        public static final Type INSTANCE = new Type();
        public static final String ID = "extruding";
    }
    public static class Serializer implements RecipeSerializer<ExtrudingRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID =
                new ResourceLocation(CreateMechanicalExtruder.MODID,"extruding");

        @Override
        public ExtrudingRecipe fromJson(ResourceLocation id, JsonObject json) {
            ExtrudingRecipeBuilder builder = new ExtrudingRecipeBuilder(id);
            NonNullList<Ingredient> itemIngredients = NonNullList.create();
            NonNullList<FluidIngredient> fluidIngredients = NonNullList.create();
            ItemStack result = ItemStack.EMPTY;
            ItemStack catalyst = ItemStack.EMPTY;


            for (JsonElement je : GsonHelper.getAsJsonArray(json, "ingredients")) {
                if (FluidIngredient.isFluidIngredient(je))
                    fluidIngredients.add(FluidIngredient.deserialize(je));
                else
                    itemIngredients.add(Ingredient.fromJson(je));
            }
            result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            if(GsonHelper.isValidNode(json,"catalyst")){
                catalyst = ShapedRecipe.itemStackFromJson( GsonHelper.getAsJsonObject(json, "catalyst"));
            }

            builder.withItemIngredients(itemIngredients)
                    .withSingleItemOutput(result)
                    .withFluidIngredients(fluidIngredients)
                    .withCatalyst(catalyst);
            return builder.build();
        }

        @Override
        public ExtrudingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ExtrudingRecipeBuilder builder = new ExtrudingRecipeBuilder(id);
            NonNullList<Ingredient> itemIngredients = NonNullList.create();
            NonNullList<FluidIngredient> fluidIngredients = NonNullList.create();
            ItemStack result = ItemStack.EMPTY;
            ItemStack catalyst = ItemStack.EMPTY;


            int size = buffer.readVarInt();
            for (int i = 0; i < size; i++)
                itemIngredients.add(Ingredient.fromNetwork(buffer));

            size = buffer.readVarInt();
            for (int i = 0; i < size; i++)
                fluidIngredients.add(FluidIngredient.read(buffer));

            result = buffer.readItem();
            catalyst = buffer.readItem();


            builder.withItemIngredients(itemIngredients)
                    .withSingleItemOutput(result)
                    .withFluidIngredients(fluidIngredients)
                    .withCatalyst(catalyst);
            return builder.build();
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ExtrudingRecipe recipe) {
            NonNullList<Ingredient> itemIngredients = recipe.itemIngredients;
            NonNullList<FluidIngredient> fluidIngredients = recipe.fluidIngredients;
            ItemStack result = recipe.result;
            ItemStack catalyst = recipe.catalyst;



            buffer.writeVarInt(itemIngredients.size());
            itemIngredients.forEach(i -> i.toNetwork(buffer));
            buffer.writeVarInt(fluidIngredients.size());
            fluidIngredients.forEach(i -> i.write(buffer));

            buffer.writeItemStack(result, false);
            buffer.writeItemStack(catalyst, false);
        }


        @Override
        public RecipeSerializer<?> setRegistryName(ResourceLocation name) {
            return INSTANCE;
        }

        @Nullable
        @Override
        public ResourceLocation getRegistryName() {
            return ID;
        }

        @Override
        public Class<RecipeSerializer<?>> getRegistryType() {
            return Serializer.castClass(RecipeSerializer.class);
        }

        @SuppressWarnings("unchecked") // Need this wrapper, because generics
        private static <G> Class<G> castClass(Class<?> cls) {
            return (Class<G>)cls;
        }
    }

}