package rbasamoyai.createbigcannons.index;

import java.util.function.Supplier;

import com.simibubi.create.foundation.particle.ICustomParticleData;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import rbasamoyai.createbigcannons.cannon_control.effects.CannonPlumeParticleData;
import rbasamoyai.createbigcannons.cannon_control.effects.CannonSmokeParticleData;
import rbasamoyai.createbigcannons.multiloader.IndexPlatform;
import rbasamoyai.createbigcannons.multiloader.ParticlesPlatform;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidBlobParticleData;

public enum CBCParticleTypes {

	CANNON_PLUME(CannonPlumeParticleData::new),
	FLUID_BLOB(FluidBlobParticleData::new),
	CANNON_SMOKE(CannonSmokeParticleData::new);

	private final ParticleEntry<?> entry;

	<D extends ParticleOptions> CBCParticleTypes(Supplier<ICustomParticleData<D>> typeFactory) {
		String name = Lang.asId(name());
		entry = new ParticleEntry<>(name, typeFactory);
	}

	public static void register() {
		IndexPlatform.registerDeferredParticles();
	}

	@Environment(EnvType.CLIENT)
	public static void registerFactories() {
		ParticleEngine particles = Minecraft.getInstance().particleEngine;
		for (CBCParticleTypes particle : values())
			particle.entry.registerFactory(particles);
	}

	public ParticleType<?> get() {
		return entry.object;
	}

	public String parameter() {
		return entry.name;
	}

	private static class ParticleEntry<D extends ParticleOptions> {
		private final String name;
		private final Supplier<? extends ICustomParticleData<D>> typeFactory;
		private final ParticleType<D> object;

		public ParticleEntry(String name, Supplier<? extends ICustomParticleData<D>> typeFactory) {
			this.name = name;
			this.typeFactory = typeFactory;

			object = this.typeFactory.get().createType();
			IndexPlatform.registerDeferredParticleType(this.name, this.object);
		}

        @Environment(EnvType.CLIENT)
        public void registerFactory(ParticleEngine particles) {
            ParticlesPlatform.register(typeFactory.get(), object, particles);
        }
    }

}
