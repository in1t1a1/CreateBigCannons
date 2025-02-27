package rbasamoyai.createbigcannons.cannon_control.cannon_mount;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.cannon_control.ControlPitchContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.AutocannonBlock;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBlock;
import rbasamoyai.createbigcannons.index.CBCBlocks;

public class CannonMountBlockEntity extends KineticBlockEntity implements IDisplayAssemblyExceptions, ControlPitchContraption.Block {

	private static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

	private AssemblyException lastException = null;
	protected PitchOrientedContraptionEntity mountedContraption;
	private boolean running;

	private float cannonYaw;
	private float cannonPitch;
	private float prevYaw;
	private float prevPitch;
	private float clientYawDiff;
	private float clientPitchDiff;

	float yawSpeed;


	public CannonMountBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		if (CBCBlocks.CANNON_MOUNT.has(state)) {
			this.cannonYaw = state.getValue(HORIZONTAL_FACING).toYRot();
		}
		this.setLazyTickRate(3);
	}

	@Override
	public BlockState getControllerState() {
		return this.getBlockState();
	}

	@Override
	protected AABB createRenderBoundingBox() {
		// TODO: based on state for things like upside down mounts
		return new AABB(this.getBlockPos()).expandTowards(0, 2, 0);
	}

	@Override
	public void tick() {
		super.tick();

		if (this.mountedContraption != null) {
			if (!this.mountedContraption.isAlive()) {
				this.mountedContraption = null;
			}
		}

		this.prevYaw = this.cannonYaw;
		this.prevPitch = this.cannonPitch;

		boolean flag = this.mountedContraption != null && this.mountedContraption.canBeTurnedByController(this);

		if (this.getLevel().isClientSide) {
			this.clientYawDiff = flag ? this.clientYawDiff * 0.5f : 0;
			this.clientPitchDiff = flag ? this.clientPitchDiff * 0.5f : 0;
		}

		if (!this.running && !this.isVirtual()) {
			if (CBCBlocks.CANNON_MOUNT.has(this.getBlockState())) {
				this.cannonYaw = this.getBlockState().getValue(HORIZONTAL_FACING).toYRot();
				this.prevYaw = this.cannonYaw;
				this.cannonPitch = 0;
				this.prevPitch = 0;
			}
			return;
		}

		if (!(this.mountedContraption != null && this.mountedContraption.isStalled()) && flag) {
			float yawSpeed = this.getAngularSpeed(this::getYawSpeed, this.clientYawDiff);
			float pitchSpeed = this.getAngularSpeed(this::getSpeed, this.clientPitchDiff);
			float newYaw = this.cannonYaw + yawSpeed;
			float newPitch = this.cannonPitch + pitchSpeed;
			this.cannonYaw = newYaw % 360.0f;

			if (this.mountedContraption == null) {
				this.cannonPitch = 0.0f;
			} else {
				Direction dir = this.getContraptionDirection();
				boolean flag1 = (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) == (dir.getAxis() == Direction.Axis.X);
				float cu = flag1 ? this.getMaxElevate() : this.getMaxDepress();
				float cd = flag1 ? -this.getMaxDepress() : -this.getMaxElevate();
				this.cannonPitch = Mth.clamp(newPitch % 360.0f, cd, cu);
			}
		}

		this.applyRotation();
	}

	private float getMaxDepress() {
		return this.mountedContraption.maximumDepression();
	}

	private float getMaxElevate() {
		return this.mountedContraption.maximumElevation();
	}

	public boolean isRunning() {
		return this.running;
	}

	protected void applyRotation() {
		if (this.mountedContraption == null) return;
		if (!this.mountedContraption.canBeTurnedByController(this)) {
			this.cannonPitch = this.mountedContraption.getXRot();
			this.cannonYaw = this.mountedContraption.getYRot();
		}
		this.mountedContraption.pitch = this.cannonPitch;
		this.mountedContraption.yaw = this.cannonYaw;
	}

	public void applyHandRotation() {
		if (this.mountedContraption == null) return;
		this.cannonPitch = this.mountedContraption.pitch;
		this.cannonYaw = this.mountedContraption.yaw;
		this.prevPitch = this.cannonPitch;
		this.prevYaw = this.cannonYaw;
	}

	public void onRedstoneUpdate(boolean assemblyPowered, boolean prevAssemblyPowered, boolean firePowered, boolean prevFirePowered, int firePower) {
		if (assemblyPowered != prevAssemblyPowered) {
			this.getLevel().setBlock(this.worldPosition, this.getBlockState().setValue(CannonMountBlock.ASSEMBLY_POWERED, assemblyPowered), 3);
			if (assemblyPowered) {
				try {
					this.assemble();
					this.lastException = null;
				} catch (AssemblyException e) {
					this.lastException = e;
					this.sendData();
				}
			} else {
				this.disassemble();
				this.sendData();
			}
		}
		if (firePowered != prevFirePowered) {
			this.getLevel().setBlock(this.worldPosition, this.getBlockState().setValue(CannonMountBlock.FIRE_POWERED, firePowered), 3);
		}
		if (this.running && this.mountedContraption != null && this.getLevel() instanceof ServerLevel slevel) {
			((AbstractMountedCannonContraption) this.mountedContraption.getContraption()).onRedstoneUpdate(slevel, this.mountedContraption, firePowered != prevFirePowered, firePower, this);
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (this.running && this.mountedContraption != null) {
			this.sendData();
		}
	}

	public float getPitchOffset(float partialTicks) {
		if (this.isVirtual())
			return Mth.lerp(partialTicks + 0.5f, this.prevPitch, this.cannonPitch);
		if (this.mountedContraption == null || this.mountedContraption.isStalled() || !this.running)
			partialTicks = 0;
		float aSpeed = this.getAngularSpeed(this::getSpeed, this.clientPitchDiff);
		return Mth.lerp(partialTicks, this.cannonPitch, this.cannonPitch + aSpeed);
	}

	public void setPitch(float pitch) {
		this.cannonPitch = pitch;
	}

	public float getYawSpeed() {
		return this.overStressed ? 0 : this.getTheoreticalYawSpeed();
	}

	public float getTheoreticalYawSpeed() {
		return this.yawSpeed;
	}

	public float getYawOffset(float partialTicks) {
		if (this.isVirtual())
			return Mth.lerp(partialTicks + 0.5f, this.prevYaw, this.cannonYaw);
		if (this.mountedContraption == null || this.mountedContraption.isStalled() || !this.running)
			partialTicks = 0;
		float aSpeed = this.getAngularSpeed(this::getYawSpeed, this.clientYawDiff);
		return Mth.lerp(partialTicks, this.cannonYaw, this.cannonYaw + aSpeed);
	}

	public void setYaw(float yaw) {
		this.cannonYaw = yaw;
	}

	public Direction getContraptionDirection() {
		return this.mountedContraption == null ? Direction.NORTH : ((AbstractMountedCannonContraption) this.mountedContraption.getContraption()).initialOrientation();
	}

	public float getAngularSpeed(Supplier<Float> sup, float clientDiff) {
		float speed = convertToAngular(sup.get()) * 0.125f;
		if (sup.get() == 0) {
			speed = 0;
		}
		if (this.getLevel().isClientSide) {
			speed *= ServerSpeedProvider.get();
			speed += clientDiff / 3.0f;
		}
		return speed;
	}

	protected void assemble() throws AssemblyException {
		if (!CBCBlocks.CANNON_MOUNT.has(this.getBlockState())) return;
		BlockPos assemblyPos = this.worldPosition.above(2);
		if (this.getLevel().isOutsideBuildHeight(assemblyPos)) {
			throw cannonBlockOutsideOfWorld(assemblyPos);
		}

		AbstractMountedCannonContraption mountedCannon = this.getContraption(assemblyPos);
		if (mountedCannon == null || !mountedCannon.assemble(this.getLevel(), assemblyPos)) return;
		Direction facing = this.getBlockState().getValue(CannonMountBlock.HORIZONTAL_FACING);
		Direction facing1 = mountedCannon.initialOrientation();
		if (facing.getAxis() != facing1.getAxis() && facing1.getAxis().isHorizontal()) return;
		this.running = true;

		mountedCannon.removeBlocksFromWorld(this.getLevel(), BlockPos.ZERO);
		PitchOrientedContraptionEntity contraptionEntity = PitchOrientedContraptionEntity.create(this.getLevel(), mountedCannon, facing1, this);
		this.mountedContraption = contraptionEntity;
		this.resetContraptionToOffset();
		this.getLevel().addFreshEntity(contraptionEntity);

		this.sendData();

		AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(this.getLevel(), this.worldPosition);
	}

	private AbstractMountedCannonContraption getContraption(BlockPos pos) {
		net.minecraft.world.level.block.Block block = this.getLevel().getBlockState(pos).getBlock();
		if (block instanceof BigCannonBlock) return new MountedBigCannonContraption();
		if (block instanceof AutocannonBlock) return new MountedAutocannonContraption();
		return null;
	}

	public void disassemble() {
		if (!this.running && this.mountedContraption == null) return;
		if (this.mountedContraption != null) {
			this.resetContraptionToOffset();
			this.mountedContraption.save(new CompoundTag()); // Crude refresh of block data
			this.mountedContraption.disassemble();
			AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(this.getLevel(), this.worldPosition);
		}

		this.running = false;

		if (this.remove) {
			CBCBlocks.CANNON_MOUNT.get().playerWillDestroy(this.getLevel(), this.worldPosition, this.getBlockState(), null);
		}
	}

	protected void resetContraptionToOffset() {
		if (this.mountedContraption == null) return;
		this.cannonPitch = 0;
		this.cannonYaw = this.getContraptionDirection().toYRot();
		this.prevPitch = this.cannonPitch;
		this.prevYaw = this.cannonYaw;

		this.mountedContraption.pitch = this.cannonPitch;
		this.mountedContraption.yaw = this.cannonYaw;
		this.mountedContraption.prevPitch = this.mountedContraption.pitch;
		this.mountedContraption.prevYaw = this.mountedContraption.yaw;

		this.mountedContraption.setXRot(this.cannonPitch);
		this.mountedContraption.setYRot(this.cannonYaw);
		this.mountedContraption.xRotO = this.mountedContraption.getXRot();
		this.mountedContraption.yRotO = this.mountedContraption.getYRot();

		Vec3 vec = Vec3.atBottomCenterOf((this.worldPosition.above(2)));
		this.mountedContraption.setPos(vec);
	}

	@Override
	public float calculateStressApplied() {
		if (this.running && this.mountedContraption != null) {
			AbstractMountedCannonContraption contraption = (AbstractMountedCannonContraption) this.mountedContraption.getContraption();
			return contraption.getWeightForStress();
		}
		return 0.0f;
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.putBoolean("Running", this.running);
		tag.putFloat("CannonYaw", this.cannonYaw);
		tag.putFloat("CannonPitch", this.cannonPitch);
		tag.putFloat("YawSpeed", this.yawSpeed);
		AssemblyException.write(tag, this.lastException);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		boolean oldRunning = this.running;
		this.running = tag.getBoolean("Running");
		this.cannonYaw = tag.getFloat("CannonYaw");
		this.cannonPitch = tag.getFloat("CannonPitch");
		this.lastException = AssemblyException.read(tag);
		this.yawSpeed = tag.getFloat("YawSpeed");

		if (!clientPacket) return;

		if (!oldRunning) {
			int x = 0;
		}

		if (this.running) {
			if (oldRunning && (this.mountedContraption == null || !this.mountedContraption.isStalled())) {
				this.clientYawDiff = AngleHelper.getShortestAngleDiff(this.prevYaw, this.cannonYaw);
				this.clientPitchDiff = AngleHelper.getShortestAngleDiff(this.prevPitch, this.cannonPitch);
				this.prevYaw = this.cannonYaw;
				this.prevPitch = this.cannonPitch;
			}
		} else {
			this.mountedContraption = null;
		}
	}

	@Override
	public void remove() {
		this.remove = true;
		if (!this.getLevel().isClientSide) this.disassemble();
		super.remove();
	}

	@Override
	public boolean isAttachedTo(AbstractContraptionEntity entity) {
		return this.mountedContraption == entity;
	}

	@Override
	public void attach(PitchOrientedContraptionEntity contraption) {
		if (!(contraption.getContraption() instanceof AbstractMountedCannonContraption)) return;
		this.mountedContraption = contraption;
		if (!this.getLevel().isClientSide) {
			this.running = true;
			this.sendData();
		}
	}

	@Override
	public void onStall() {
		if (!this.getLevel().isClientSide) this.sendData();
	}

	@Override
	public BlockPos getControllerBlockPos() {
		return this.worldPosition;
	}

	@Override
	public BlockPos getDismountPositionForContraption(PitchOrientedContraptionEntity poce) {
		return this.worldPosition.relative(this.mountedContraption.getInitialOrientation().getOpposite()).above();
	}

	@Override
	public AssemblyException getLastAssemblyException() {
		return this.lastException;
	}

	public static AssemblyException cannonBlockOutsideOfWorld(BlockPos pos) {
		return new AssemblyException(Component.translatable("exception." + CreateBigCannons.MOD_ID + ".cannon_mount.cannonBlockOutsideOfWorld", pos.getX(), pos.getY(), pos.getZ()));
	}

	public Vec3 getInteractionLocation() {
		return this.mountedContraption != null && this.mountedContraption.getContraption() instanceof AbstractMountedCannonContraption cannon
			? cannon.getInteractionVec(this.mountedContraption) : Vec3.atCenterOf(this.worldPosition);
	}

	@Nullable
	public PitchOrientedContraptionEntity getContraption() {
		return this.mountedContraption;
	}

}
