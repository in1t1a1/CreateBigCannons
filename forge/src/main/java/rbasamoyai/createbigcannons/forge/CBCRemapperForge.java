package rbasamoyai.createbigcannons.forge;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.index.CBCBlocks;
import rbasamoyai.createbigcannons.index.CBCItems;

import java.util.HashMap;
import java.util.Map;

import static rbasamoyai.createbigcannons.CreateBigCannons.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CBCRemapperForge {

	private static final Map<String, ResourceLocation> REMAP = new HashMap<>();

	static {
		REMAP.put("cannon_cast_wand", CBCItems.CANNON_CRAFTING_WAND.getId());
		REMAP.put("nether_gunmetal_cannon_barrel", CBCBlocks.NETHERSTEEL_CANNON_BARREL.getId());
		REMAP.put("nether_gunmetal_cannon_chamber", CBCBlocks.NETHERSTEEL_CANNON_CHAMBER.getId());
		REMAP.put("nether_gunmetal_screw_breech", CBCBlocks.NETHERSTEEL_SCREW_BREECH.getId());
		REMAP.put("unbored_sliding_breech_cast_mould", CBCBlocks.SLIDING_BREECH_CAST_MOULD.getId());
	}

	@SubscribeEvent
	public static void onRemapItem(MissingMappingsEvent event) {
		for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings((ResourceKey<? extends Registry<Item>>) event.getKey(), MOD_ID)) {
			ResourceLocation key = mapping.getKey();
			String path = key.getPath();
			ResourceLocation remapLoc = REMAP.get(path);
			if (remapLoc == null) continue;
			Item remapped = BuiltInRegistries.ITEM.get(remapLoc);
			if (remapped == null) continue;
			CreateBigCannons.LOGGER.warn("Remapping item '{}' to '{}'", key, remapLoc);
			try {
				mapping.remap(remapped);
			} catch (Throwable t) {
				CreateBigCannons.LOGGER.warn("Remapping item '{}' to '{}' failed: {}", key, remapLoc, t);
			}
		}
	}

	@SubscribeEvent
	public static void onRemapBlock(MissingMappingsEvent event) {
		for (MissingMappingsEvent.Mapping<Block> mapping : event.getMappings((ResourceKey<? extends Registry<Block>>) event.getKey(), MOD_ID)) {
			ResourceLocation key = mapping.getKey();
			String path = key.getPath();
			ResourceLocation remapLoc = REMAP.get(path);
			if (remapLoc == null) continue;
			Block remapped = BuiltInRegistries.BLOCK.get(remapLoc);
			if (remapped == null) continue;
			CreateBigCannons.LOGGER.warn("Remapping block '{}' to '{}'", key, remapLoc);
			try {
				mapping.remap(remapped);
			} catch (Throwable t) {
				CreateBigCannons.LOGGER.warn("Remapping block '{}' to '{}' failed: {}", key, remapLoc, t);
			}
		}
	}

}
