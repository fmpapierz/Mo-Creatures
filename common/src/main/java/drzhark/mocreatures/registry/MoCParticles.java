package drzhark.mocreatures.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import drzhark.mocreatures.MoCreatures;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/**
 * Custom Mo'Creatures particle types, restoring the legacy client FX entities
 * ({@code MoCEntityFXStar}, {@code MoCEntityFXUndead}, {@code MoCEntityFXVanish})
 * as modern {@link SimpleParticleType} particles.
 *
 * <p>Each type is a plain {@link SimpleParticleType}. The vanilla constructor
 * {@code SimpleParticleType(boolean)} is {@code protected}, so it is invoked through
 * an anonymous subclass (this is the standard cross-loader idiom and matches how the
 * vanilla {@code ParticleTypes} registrar builds its simple types). The client-side
 * sprite providers are wired separately in {@code MoCParticleFactories}.</p>
 */
public final class MoCParticles {

    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            DeferredRegister.create(MoCreatures.MOD_ID, Registries.PARTICLE_TYPE);

    private MoCParticles() {}

    /** Sparkle burst emitted on creature release (legacy MoCEntityFXStar). */
    public static final RegistrySupplier<SimpleParticleType> FX_STAR =
            PARTICLES.register("fx_star", () -> new SimpleParticleType(false) {});

    /** Sickly wisp used by undead/ghost effects (legacy MoCEntityFXUndead). */
    public static final RegistrySupplier<SimpleParticleType> FX_UNDEAD =
            PARTICLES.register("fx_undead", () -> new SimpleParticleType(false) {});

    /** Vanish puff emitted on creature capture (legacy MoCEntityFXVanish). */
    public static final RegistrySupplier<SimpleParticleType> FX_VANISH =
            PARTICLES.register("fx_vanish", () -> new SimpleParticleType(false) {});
}
