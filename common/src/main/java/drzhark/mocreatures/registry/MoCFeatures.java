package drzhark.mocreatures.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.worldgen.MoCOgrePortalFeature;
import drzhark.mocreatures.worldgen.MoCWyvernPortalFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Mo'Creatures worldgen {@link Feature} registrations (registry {@code minecraft:worldgen/feature}).
 *
 * <p>{@link #WYVERN_PORTAL} is the legacy quartz portal-frame landmark that builds a single frame
 * near the Wyvern Lair dimension origin. Its configured feature id is {@code mocreatures:wyvern_portal}.</p>
 */
public final class MoCFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(MoCreatures.MOD_ID, Registries.FEATURE);

    public static final RegistrySupplier<Feature<?>> WYVERN_PORTAL =
            FEATURES.register("wyvern_portal",
                    () -> new MoCWyvernPortalFeature(NoneFeatureConfiguration.CODEC));

    /** Dark twin of the wyvern frame for the Ogre Lair origin (configured feature id {@code mocreatures:ogre_portal}). */
    public static final RegistrySupplier<Feature<?>> OGRE_PORTAL =
            FEATURES.register("ogre_portal",
                    () -> new MoCOgrePortalFeature(NoneFeatureConfiguration.CODEC));

    private MoCFeatures() {}
}
