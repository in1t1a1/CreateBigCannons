package rbasamoyai.createbigcannons.cannon_control.carriage;

import java.util.List;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import rbasamoyai.createbigcannons.index.CBCBlockEntities;

public class CannonCarriageBlock extends Block implements IWrenchable, IBE<CannonCarriageBlockEntity> {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty SADDLED = BooleanProperty.create("saddled");

	public CannonCarriageBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any().setValue(SADDLED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING)
			.add(SADDLED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction dir = context.getHorizontalDirection();
		return super.getStateForPlacement(context).setValue(FACING, context.getPlayer().isShiftKeyDown() ? dir.getOpposite() : dir);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		if (!level.isClientSide && level.getBlockEntity(context.getClickedPos()) instanceof CannonCarriageBlockEntity carriage) {
			carriage.tryAssemble();
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
		ItemStack stack = player.getItemInHand(hand);
		if (state.getValue(SADDLED)) {
			if (stack.isEmpty()) {
				level.setBlock(pos, state.setValue(SADDLED, false), 11);
				if (!level.isClientSide) {
					ItemStack resultStack = Items.SADDLE.getDefaultInstance();
					if (!player.addItem(resultStack) && !player.isCreative()) {
						ItemEntity item = player.drop(resultStack, false);
						if (item != null) {
							item.setNoPickUpDelay();
							item.setTarget(player.getUUID());
						}
					}
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		} else {
			if (stack.is(Items.SADDLE)) {
				if (!level.isClientSide && !player.isCreative()) stack.shrink(1);
				level.setBlock(pos, state.setValue(SADDLED, true), 11);
				level.playSound(player, pos, SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 0.5F, 1.0F);
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> loot = super.getDrops(state, params);
		if (state.getValue(SADDLED)) loot.add(Items.SADDLE.getDefaultInstance());
		return loot;
	}

	@Override
	public Class<CannonCarriageBlockEntity> getBlockEntityClass() {
		return CannonCarriageBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CannonCarriageBlockEntity> getBlockEntityType() {
		return CBCBlockEntities.CANNON_CARRIAGE.get();
	}

}
