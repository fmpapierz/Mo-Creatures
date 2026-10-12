package dev.architectury.registry.client.particle;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): answered from Forge's RegisterParticleProvidersEvent. */
public final class ParticleProviderRegistry {
    private ParticleProviderRegistry() {}

    public static <T extends ParticleOptions> void register(RegistrySupplier<? extends ParticleType<T>> type,
            DeferredParticleProvider<T> provider) {
        RegisterParticleProvidersEvent.BUS.addListener(event ->
                event.registerSpriteSet(type.get(), sprites -> provider.create(new Sprites(sprites))));
    }

    /** Architectury's sprite set: a plain SpriteSet here. */
    public interface ExtendedSpriteSet extends SpriteSet {
    }

    @FunctionalInterface
    public interface DeferredParticleProvider<T extends ParticleOptions> {
        ParticleProvider<T> create(ExtendedSpriteSet spriteSet);
    }

    private record Sprites(SpriteSet delegate) implements ExtendedSpriteSet {
        @Override
        public TextureAtlasSprite get(int index, int max) {
            return this.delegate.get(index, max);
        }

        @Override
        public TextureAtlasSprite get(RandomSource random) {
            return this.delegate.get(random);
        }

        @Override
        public TextureAtlasSprite first() {
            return this.delegate.first();
        }
    }
}
