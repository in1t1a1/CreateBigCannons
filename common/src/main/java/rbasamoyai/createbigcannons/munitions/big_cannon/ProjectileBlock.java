package rbasamoyai.createbigcannons.munitions.big_cannon;

import javax.annotation.Nullable;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBlock;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

public abstract class ProjectileBlock extends DirectionalBlock implements IWrenchable, BigCannonMunitionBlock {

	private final VoxelShaper shapes;

	public ProjectileBlock(Properties properties) {
		super(properties.pushReaction(PushReaction.NORMAL));
		this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.UP));
		this.shapes = this.makeShapes();
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	protected VoxelShaper makeShapes() {
		VoxelShape base = box(3, 0, 3, 13, 16, 13);
		return new AllShapes.Builder(base).forDirectional();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Player player = context.getPlayer();
		Direction facing = context.getClickedFace();
		boolean flag = player != null && player.isShiftKeyDown();
		BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos().relative(facing.getOpposite()));

		if (clickedState.getBlock() instanceof BigCannonBlock cblock
			&& cblock.getFacing(clickedState).getAxis() == facing.getAxis()
			&& !flag) {
			facing = facing.getOpposite();
		}
		return this.defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
		return this.shapes.get(state.getValue(FACING));
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	public abstract AbstractCannonProjectile getProjectile(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity blockEntity);

	@Override
	public boolean canBeLoaded(BlockState state, Direction.Axis facing) {
		return state.getValue(FACING).getAxis() == facing;
	}

	@Override
	public BlockState onCannonRotate(BlockState oldState, Direction.Axis rotationAxis, Rotation rotation) {
		Direction facing = oldState.getValue(BlockStateProperties.FACING);
		for (int i = 0; i < rotation.ordinal(); ++i) {
			facing = facing.getClockWise(rotationAxis);
		}
		return oldState.setValue(BlockStateProperties.FACING, facing);
	}

	@Override
	public StructureBlockInfo getHandloadingInfo(ItemStack stack, BlockPos localPos, Direction cannonOrientation) {
		BlockState state = this.defaultBlockState().setValue(FACING, cannonOrientation);
		CompoundTag baseTag = stack.getOrCreateTag();
		if (baseTag.contains("BlockEntityTag")) {
			CompoundTag tag = baseTag.getCompound("BlockEntityTag").copy();
			tag.remove("x");
			tag.remove("y");
			tag.remove("z");
			return new StructureBlockInfo(localPos, state, tag);
		}
		return new StructureBlockInfo(localPos, state, null);
	}

	@Override
	public ItemStack getExtractedItem(StructureBlockInfo info) {
		ItemStack stack = new ItemStack(this);
		if (info.nbt() != null) {
			stack.getOrCreateTag().put("BlockEntityTag", info.nbt());
		}
		return stack;
	}

	public abstract EntityType<?> getAssociatedEntityType();

}
