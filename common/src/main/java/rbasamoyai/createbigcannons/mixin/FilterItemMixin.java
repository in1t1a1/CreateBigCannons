package rbasamoyai.createbigcannons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.logistics.filter.FilterItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import rbasamoyai.createbigcannons.index.CBCBlocks;
import rbasamoyai.createbigcannons.munitions.autocannon.ammo_container.AutocannonAmmoContainerItem;
import rbasamoyai.createbigcannons.munitions.big_cannon.propellant.BigCartridgeBlockItem;

@Mixin(FilterItem.class)
public class FilterItemMixin {

	@Inject(method = "test(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Z)Z",
		at = @At("HEAD"), cancellable = true)
	private static void createbigcannons$test(Level level, ItemStack stack, ItemStack filter, boolean matchNbt, CallbackInfoReturnable<Boolean> cir) {
		if (filter.isEmpty() || matchNbt) return;
		if (CBCBlocks.BIG_CARTRIDGE.is(filter.getItem()) && BigCartridgeBlockItem.getPower(filter) == 0) {
			cir.setReturnValue(CBCBlocks.BIG_CARTRIDGE.is(stack.getItem()) && BigCartridgeBlockItem.getPower(stack) == 0);
			return;
		}
		if (filter.getItem() instanceof AutocannonAmmoContainerItem && AutocannonAmmoContainerItem.getTotalAmmoCount(filter) == 0) {
			cir.setReturnValue(stack.getItem() instanceof AutocannonAmmoContainerItem && AutocannonAmmoContainerItem.getTotalAmmoCount(stack) == 0);
			return;
		}
	}

}
