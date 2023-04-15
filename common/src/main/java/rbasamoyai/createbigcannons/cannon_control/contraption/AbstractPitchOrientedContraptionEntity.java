package rbasamoyai.createbigcannons.cannon_control.contraption;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.components.actors.SeatEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.OrientedContraptionEntity;
import com.simibubi.create.foundation.utility.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.cannon_control.ControlPitchContraption;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.AbstractCannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBlock;
import rbasamoyai.createbigcannons.cannons.big_cannons.IBigCannonBlockEntity;
import rbasamoyai.createbigcannons.index.CBCEntityTypes;
import rbasamoyai.createbigcannons.manualloading.HandloadingTool;
import rbasamoyai.createbigcannons.multiloader.NetworkPlatform;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonMunitionBlock;
import rbasamoyai.createbigcannons.network.ClientboundUpdateContraptionPacket;

public abstract class AbstractPitchOrientedContraptionEntity extends OrientedContraptionEntity {

	private BlockPos controllerPos;
	private boolean updatesOwnRotation;

	protected AbstractPitchOrientedContraptionEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	public static AbstractPitchOrientedContraptionEntity create(Level level, Contraption contraption, Direction initialOrientation, boolean updatesOwnRotation) {
		AbstractPitchOrientedContraptionEntity entity = CBCEntityTypes.PITCH_ORIENTED_CONTRAPTION.create(level);

		entity.setContraption(contraption);
		entity.setInitialOrientation(initialOrientation);
		entity.startAtInitialYaw();
		entity.updatesOwnRotation = updatesOwnRotation;
		return entity;
	}
	
	public static AbstractPitchOrientedContraptionEntity create(Level level, Contraption contraption, Direction initialOrientation, ControlPitchContraption.Block block) {
		AbstractPitchOrientedContraptionEntity poce = create(level, contraption, initialOrientation, true);
		poce.controllerPos = block.getControllerBlockPos();
		return poce;
	}

	@Override
	protected void readAdditional(CompoundTag compound, boolean spawnPacket) {
		super.readAdditional(compound, spawnPacket);
		if (compound.contains("ControllerRelative")) this.controllerPos = NbtUtils.readBlockPos(compound.getCompound("ControllerRelative")).offset(this.blockPosition());
		else if (this.level.getBlockEntity(this.blockPosition().below(2)) instanceof ControlPitchContraption.Block controller && !this.isPassenger()) {
			// Legacy, cannon mount
			this.controllerPos = controller.getControllerBlockPos();
		}
		this.updatesOwnRotation = compound.getBoolean("UpdatesOwnRotation");
	}

	@Override
	protected void writeAdditional(CompoundTag compound, boolean spawnPacket) {
		super.writeAdditional(compound, spawnPacket);
		if (this.controllerPos != null) compound.put("ControllerRelative", NbtUtils.writeBlockPos(controllerPos.subtract(blockPosition())));
		compound.putBoolean("UpdatesOwnRotation", this.updatesOwnRotation);
	}

	protected ControlPitchContraption getController() {
		if (this.controllerPos != null) {
			if (!this.level.isLoaded(this.controllerPos)) return null;
			return this.level.getBlockEntity(this.controllerPos) instanceof ControlPitchContraption controller ? controller : null;
		}
		return this.getVehicle() instanceof ControlPitchContraption controller ? controller : null;
	}

	@Override
	protected void tickContraption() {
		super.tickContraption();

		if (this.updatesOwnRotation) {
			this.prevYaw = this.yaw;
			this.prevPitch = this.pitch;
		}

		this.contraption.anchor = this.blockPosition();
		if (this.contraption instanceof AbstractMountedCannonContraption mounted) mounted.tick(this.level, this);

		ControlPitchContraption controller = this.getController();
		if (controller == null) {
			if (!this.level.isClientSide) this.disassemble();
			return;
		}
		if (!controller.isAttachedTo(this)) {
			controller.attach(this);
			if (this.level.isClientSide) this.setPos(this.getX(), this.getY(), this.getZ());
		}
	}

	public void handleAnimation() {
		if (this.contraption instanceof AbstractMountedCannonContraption mounted) mounted.animate();
	}

	public float maximumDepression() { return this.contraption instanceof AbstractMountedCannonContraption cannon ? cannon.maximumDepression(this.getController()) : 90f; }
	public float maximumElevation() { return this.contraption instanceof AbstractMountedCannonContraption cannon ? cannon.maximumElevation(this.getController()) : 90f; }

	@Override
	protected void onContraptionStalled() {
		ControlPitchContraption controller = this.getController();
		if (controller != null) controller.onStall();
		super.onContraptionStalled();
	}

	@Override
	public ContraptionRotationState getRotationState() {
		return new CBCContraptionRotationState(this);
	}

	@Override
	protected boolean updateOrientation(boolean rotationLock, boolean wasStalled, Entity riding, boolean isOnCoupling) {
		return false;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		float prevYaw = this.yaw;
		super.onSyncedDataUpdated(key);
		this.yaw = prevYaw;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void applyLocalTransforms(PoseStack stack, float partialTicks) {
		float initialYaw = this.getInitialYaw();
		float pitch = this.getViewXRot(partialTicks);
		float yaw = this.getViewYRot(partialTicks) + initialYaw;
		
		stack.translate(-0.5f, 0.0f, -0.5f);
		
		TransformStack tstack = TransformStack.cast(stack)
				.nudge(this.getId())
				.centre()
				.rotateY(yaw);
		
		if (this.getInitialOrientation().getAxis() == Direction.Axis.X) {
			tstack.rotateZ(pitch);
		} else {
			tstack.rotateX(pitch);
		}
		tstack.unCentre();
	}
	
	@Override
	public Vec3 applyRotation(Vec3 localPos, float partialTicks) {
		localPos = VecHelper.rotate(localPos, this.getViewXRot(partialTicks), this.getInitialOrientation().getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		localPos = VecHelper.rotate(localPos, this.getViewYRot(partialTicks), Direction.Axis.Y);
		localPos = VecHelper.rotate(localPos, this.getInitialYaw(), Direction.Axis.Y);
		return localPos;
	}
	
	@Override
	public Vec3 reverseRotation(Vec3 localPos, float partialTicks) {
		localPos = VecHelper.rotate(localPos, -this.getInitialYaw(), Direction.Axis.Y);
		localPos = VecHelper.rotate(localPos, -this.getViewYRot(partialTicks), Direction.Axis.Y);
		localPos = VecHelper.rotate(localPos, -this.getViewXRot(partialTicks), this.getInitialOrientation().getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return localPos;
	}

	public float getRotationCoefficient() {
		return this.contraption instanceof AbstractMountedCannonContraption cannon ? Math.max(1 / cannon.getWeightForStress(), 0.1f) : 0.1f;
	}

	@Override
	public void onPassengerTurned(Entity entity) {
		if (this.contraption instanceof AbstractMountedCannonContraption cannon && cannon.canBeTurnedByPassenger(entity)) {
			Direction dir = this.getInitialOrientation();
			boolean flag = (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) == (dir.getAxis() == Direction.Axis.X);
			this.prevPitch = flag ? -entity.xRotO : entity.xRotO;
			this.pitch = flag ? -entity.getXRot() : entity.getXRot();
			this.prevYaw = entity.yRotO;
			this.yaw = entity.getYRot();

			entity.setYBodyRot(entity.getYRot());
			if (CBCEntityTypes.CANNON_CARRIAGE.is(this.getVehicle())) {
				this.getVehicle().onPassengerTurned(this);
			} else if (this.getController() instanceof AbstractCannonMountBlockEntity mount) {
				mount.applyHandRotation();
				this.xRotO = this.prevPitch;
				this.setXRot(this.pitch);
				this.yRotO = this.prevYaw;
				this.setYRot(this.yaw);
			}
		}
	}

	@Override
	public void addSittingPassenger(Entity passenger, int seatIndex) {
		if (passenger instanceof Mob mob && mob.getLeashHolder() instanceof Player player) {
			this.addSittingPassenger(player, seatIndex);
		}
		super.addSittingPassenger(passenger, seatIndex);
	}

	@Override
	protected void addPassenger(Entity entity) {
		super.addPassenger(entity);
		Direction dir = this.getInitialOrientation();
		boolean flag = (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) == (dir.getAxis() == Direction.Axis.X);

		entity.setXRot(flag ? -this.pitch : this.prevPitch);
		entity.setYRot(this.yaw);
		entity.xRotO = flag ? -this.prevPitch : this.prevPitch;
		entity.yRotO = this.prevYaw;
	}

	@Override
	public void positionRider(Entity passenger) {
		if (!this.hasPassenger(passenger)) return;
		Vec3 transformedVector = this.getPassengerPosition(passenger, 1);
		if (transformedVector == null) return;
		passenger.setPos(transformedVector.x,
				transformedVector.y + SeatEntity.getCustomEntitySeatOffset(passenger) - 1 / 8f, transformedVector.z);
	}

	@Override
	public Vec3 getPassengerPosition(Entity passenger, float partialTicks) {
		if (passenger != this.getControllingPassenger() || !(this.contraption instanceof AbstractMountedCannonContraption cannon))
			return super.getPassengerPosition(passenger, partialTicks);

		BlockPos seat = cannon.getSeatPos(passenger);
		if (seat == null) return null;
		return this.toGlobalVector(Vec3.atLowerCornerOf(seat)
				.add(.5, 1, .5), partialTicks)
				.subtract(0, passenger.getEyeHeight(), 0);
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity entityLiving) {
		ControlPitchContraption controller = this.getController();
		Vec3 superResult = super.getDismountLocationForPassenger(entityLiving); // Call to process other stuff
		return controller != null ? Vec3.atCenterOf(controller.getDismountPositionForContraption(this)) : superResult;
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
	}

	public BlockPos getSeatPos(Entity passenger) {
		return ((AbstractMountedCannonContraption) this.contraption).getSeatPos(passenger);
	}

	@Nullable
	@Override
	public Entity getControllingPassenger() {
		return this.getFirstPassenger() instanceof Player player ? player : null;
	}

	public boolean canBeTurnedByController(ControlPitchContraption control) {
		return this.contraption instanceof AbstractMountedCannonContraption cannon && cannon.canBeTurnedByController(control);
	}

	public void tryFiringShot() {
		if (this.contraption instanceof AbstractMountedCannonContraption cannon && this.level instanceof ServerLevel slevel) cannon.fireShot(slevel, this);
	}

	@Override
	public boolean handlePlayerInteraction(Player player, BlockPos localPos, Direction side, InteractionHand interactionHand) {
		if (this.contraption instanceof MountedBigCannonContraption cannon) {
			BlockEntity be = this.contraption.presentTileEntities.get(localPos);
			StructureBlockInfo info = this.contraption.getBlocks().get(localPos);

			if (info.state.getBlock() instanceof BigCannonBlock cBlock
					&& cBlock.getFacing(info.state).getAxis() == side.getAxis()
					&& be instanceof IBigCannonBlockEntity cbe
					&& !cbe.cannonBehavior().isConnectedTo(side)) {
				ItemStack stack = player.getItemInHand(interactionHand);
				if (Block.byItem(stack.getItem()) instanceof BigCannonMunitionBlock munition) {
					StructureBlockInfo loadInfo = munition.getHandloadingInfo(stack, localPos, side);
					if (!this.level.isClientSide && cbe.cannonBehavior().tryLoadingBlock(loadInfo)) {
						CompoundTag tag = be.saveWithFullMetadata();
						tag.remove("x");
						tag.remove("y");
						tag.remove("z");
						StructureBlockInfo newInfo = new StructureBlockInfo(info.pos, info.state, tag);
						this.contraption.getBlocks().put(info.pos, newInfo);
						NetworkPlatform.sendToClientTracking(new ClientboundUpdateContraptionPacket(this, info.pos, newInfo), this);

						SoundType sound = loadInfo.state.getSoundType();
						this.level.playSound(null, player.blockPosition(), sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
						if (!player.isCreative()) stack.shrink(1);
					}
					return true;
				}
				if (stack.getItem() instanceof HandloadingTool tool && !player.getCooldowns().isOnCooldown(stack.getItem())) {
					tool.onUseOnCannon(player, this.level, localPos, side, cannon);
					return true;
				}
			}
		}

		return super.handlePlayerInteraction(player, localPos, side, interactionHand);
	}

}
