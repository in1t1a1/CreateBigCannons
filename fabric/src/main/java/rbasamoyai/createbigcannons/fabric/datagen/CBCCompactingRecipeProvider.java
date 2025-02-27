package rbasamoyai.createbigcannons.fabric.datagen;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.data.recipe.ProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import rbasamoyai.createbigcannons.CBCTags;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.datagen.CBCDatagenCommon;
import rbasamoyai.createbigcannons.index.CBCBlocks;
import rbasamoyai.createbigcannons.index.CBCFluids;
import rbasamoyai.createbigcannons.index.CBCItems;

public class CBCCompactingRecipeProvider extends ProcessingRecipeGen {

	public CBCCompactingRecipeProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	protected IRecipeTypeInfo getRecipeType() {
		return AllRecipeTypes.COMPACTING;
	}

	GeneratedRecipe

		PACKED_GUNPOWDER = create(CreateBigCannons.resource("packed_gunpowder"), b -> b.require(CBCTags.CBCItemTags.GUNPOWDER)
		.require(CBCTags.CBCItemTags.GUNPOWDER)
		.require(CBCTags.CBCItemTags.GUNPOWDER)
		.output(CBCItems.PACKED_GUNPOWDER.get())),

	FORGE_CAST_IRON_INGOT = create(CreateBigCannons.resource("forge_cast_iron_ingot"), b -> b.require(AllTags.forgeFluidTag("molten_cast_iron"), 90 * CBCDatagenCommon.FLUID_MULTIPLIER)
		.output(CBCItems.CAST_IRON_INGOT.get())),

	FORGE_CAST_IRON_NUGGET = create(CreateBigCannons.resource("forge_cast_iron_nugget"), b -> b.require(AllTags.forgeFluidTag("molten_cast_iron"), 10 * CBCDatagenCommon.FLUID_MULTIPLIER)
		.output(CBCItems.CAST_IRON_NUGGET.get())),

	FORGE_BRONZE_INGOT = create(CreateBigCannons.resource("forge_bronze_ingot"), b -> b
		.withCondition(DefaultResourceConditions.itemTagsPopulated(CBCTags.CBCItemTags.INGOT_BRONZE))
		.require(AllTags.forgeFluidTag("molten_bronze"), 90 * CBCDatagenCommon.FLUID_MULTIPLIER)
		.output(1, new ResourceLocation("alloyed", "bronze_ingot"), 1)),

	FORGE_STEEL_INGOT = create(CreateBigCannons.resource("forge_steel_ingot"), b -> b
		.withCondition(DefaultResourceConditions.itemTagsPopulated(CBCTags.CBCItemTags.INGOT_STEEL))
		.require(AllTags.forgeFluidTag("molten_steel"), 90 * CBCDatagenCommon.FLUID_MULTIPLIER)
		.output(1, new ResourceLocation("alloyed", "steel_ingot"), 1)),

	FORGE_NETHERSTEEL_INGOT = create(CreateBigCannons.resource("forge_nethersteel_ingot"), b -> b.require(CBCFluids.MOLTEN_NETHERSTEEL.get(), 90 * CBCDatagenCommon.FLUID_MULTIPLIER)
		.output(CBCItems.NETHERSTEEL_INGOT.get())),

	FORGE_NETHERSTEEL_NUGGET = create(CreateBigCannons.resource("forge_nethersteel_nugget"), b -> b.require(CBCFluids.MOLTEN_NETHERSTEEL.get(), 10 * CBCDatagenCommon.FLUID_MULTIPLIER)
		.output(CBCItems.NETHERSTEEL_NUGGET.get())),

	// The following are reimplemented from Create Deco
	IRON_TO_CAST_IRON_INGOT = create(CreateBigCannons.resource("iron_to_cast_iron_ingot"), b -> b.require(Items.IRON_INGOT)
		.requiresHeat(HeatCondition.HEATED)
		.output(CBCItems.CAST_IRON_INGOT.get())),

	IRON_TO_CAST_IRON_BLOCK = create(CreateBigCannons.resource("iron_to_cast_iron_block"), b -> b.require(Items.IRON_BLOCK)
		.requiresHeat(HeatCondition.HEATED)
		.output(CBCBlocks.CAST_IRON_BLOCK.get()));

}
