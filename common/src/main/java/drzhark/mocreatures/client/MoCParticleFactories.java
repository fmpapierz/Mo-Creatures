package drzhark.mocreatures.client;

import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import drzhark.mocreatures.registry.MoCParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Client-side particle providers for the custom Mo'Creatures FX particles registered in
 * {@link MoCParticles}. Restores the legacy {@code MoCEntityFXStar / FXUndead / FXVanish}
 * visuals as simple sprite particles.
 *
 * <p>Each type is bound to a {@link SimpleAnimatedParticle} (the vanilla base for
 * single-sprite, camera-facing, animated particles). The provider lambda receives an
 * {@code ExtendedSpriteSet} (a {@link SpriteSet}) resolved from the particle atlas via the
 * matching {@code assets/mocreatures/particles/*.json} definition, mirroring how the vanilla
 * simple particle providers pick their sprite.</p>
 *
 * <p>Wired (client-only) from {@code MoCreaturesClient.init()} via {@link #register()}.</p>
 */
public final class MoCParticleFactories {

    private MoCParticleFactories() {}

    public static void register() {
        // FX_STAR: bright, near-weightless sparkle that drifts and fades (release burst).
        ParticleProviderRegistry.register(MoCParticles.FX_STAR,
                sprites -> new FxProvider(sprites, 0.996F, 0xFFFFFF, 12));
        // FX_UNDEAD: sickly greenish wisp with a slight downward pull (ghost/undead FX).
        ParticleProviderRegistry.register(MoCParticles.FX_UNDEAD,
                sprites -> new FxProvider(sprites, 0.900F, 0x88CC66, 16));
        // FX_VANISH: pale puff that lingers briefly then fades (capture puff).
        ParticleProviderRegistry.register(MoCParticles.FX_VANISH,
                sprites -> new FxProvider(sprites, 0.960F, 0xEEEEFF, 14));
    }

    /**
     * Simple sprite particle provider mirroring vanilla's single-sprite providers. Reuses the
     * {@link SpriteSet} handed in by the registry (its sprite is resolved from the particle's
     * JSON texture list) and produces a lightweight {@link SimpleAnimatedParticle}.
     */
    private record FxProvider(SpriteSet sprites, float friction, int color, int lifetime)
            implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       RandomSource random) {
            // SimpleAnimatedParticle is abstract only in declaration (getLayer() is implemented by
            // the base); an anonymous subclass gives us a concrete, sprite-backed particle and a
            // scope in which the protected 'friction' field is reachable.
            float f = friction;
            SimpleAnimatedParticle particle = new SimpleAnimatedParticle(level, x, y, z, sprites, 0.0F) {
                {
                    this.friction = f;
                }
            };
            particle.setColor(color);
            particle.setLifetime(lifetime);
            particle.setSpriteFromAge(sprites);
            particle.scale(1.0F);
            // Carry the spawn velocity supplied by sendParticles(...).
            particle.setParticleSpeed(xSpeed, ySpeed, zSpeed);
            return particle;
        }
    }
}
