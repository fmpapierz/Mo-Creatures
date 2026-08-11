package drzhark.mocreatures;

import com.mojang.logging.LogUtils;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.registry.MoCBlocks;
import drzhark.mocreatures.registry.MoCCreativeTabs;
import drzhark.mocreatures.registry.MoCDataComponents;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import drzhark.mocreatures.registry.MoCSpawns;
import org.slf4j.Logger;

/**
 * Common (loader-agnostic) entrypoint for DrZhark's Mo'Creatures on Minecraft 26.2.
 * Each platform entrypoint calls {@link #init()}. Registration is performed through
 * Architectury's cross-loader {@code DeferredRegister}s in the {@code registry} package.
 */
public final class MoCreatures {
    public static final String MOD_ID = "mocreatures";
    public static final Logger LOGGER = LogUtils.getLogger();

    private MoCreatures() {}

    public static void init() {
        MoCConfig.load();

        MoCDataComponents.COMPONENTS.register();
        MoCSounds.SOUND_EVENTS.register();
        MoCEntities.ENTITIES.register();
        MoCBlocks.BLOCKS.register();
        MoCItems.ITEMS.register();
        MoCCreativeTabs.TABS.register();
        drzhark.mocreatures.registry.MoCParticles.PARTICLES.register();
        drzhark.mocreatures.registry.MoCFeatures.FEATURES.register();

        registerEntityAttributes();
        MoCSpawns.register();
        // Biome spawn lists are wired per loader, not here: Fabric applies them through Architectury's
        // BiomeModifications, while on NeoForge biome modifiers are a datapack registry that Architectury
        // registers a serializer for but ships no entry for, so the NeoForge module supplies its own
        // BiomeModifier. Both feed MoCSpawns.addBiomeSpawns, so the rules themselves stay shared.
        drzhark.mocreatures.spawn.MoCMobCap.register();
        drzhark.mocreatures.network.MoCNetwork.init();

        // Cross-loader /moc admin command. The register lambda receives the brigadier
        // CommandDispatcher<CommandSourceStack>, a CommandBuildContext and a Commands.CommandSelection;
        // MoCCommand only needs the dispatcher to build its tree.
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register(
                (dispatcher, registration, selection) -> drzhark.mocreatures.command.MoCCommand.register(dispatcher));

        // NOTE: there is deliberately no PLAYER_QUIT hook for carried pets. Carrying does not use the
        // vanilla passenger system (a player cannot be a vehicle on a 26.2 server — EntityType.PLAYER is
        // noSave(), so Entity.startRiding rejects it), so a carried pet is an ordinary world entity that
        // saves normally and keeps a persisted claim on its carrier's UUID. Dropping it on logout would
        // discard that claim, which is exactly the "quit to title and the bunny falls off" bug; instead
        // MoCAnimal re-attaches it the moment the carrier is back in the world.

        // Client-only rendering registration; only loads MoCreaturesClient on the physical client.
        EnvExecutor.runInEnv(Env.CLIENT, () -> drzhark.mocreatures.client.MoCreaturesClient::init);

        LOGGER.info("Mo'Creatures (Architectury multi-loader, MC 26.2) initialized with {} creatures", 56);
    }

    private static void registerEntityAttributes() {
        // ------------------------------------------------ ported from Mo'Creatures 12.0.5
        EntityAttributeRegistry.register(MoCEntities.ANT, drzhark.mocreatures.entity.passive.MoCEntityAnt::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.RACCOON, drzhark.mocreatures.entity.passive.MoCEntityRaccoon::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.MOLE, drzhark.mocreatures.entity.passive.MoCEntityMole::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.ENT, drzhark.mocreatures.entity.passive.MoCEntityEnt::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.SMALL_FISH, drzhark.mocreatures.entity.passive.MoCEntitySmallFish::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.MEDIUM_FISH, drzhark.mocreatures.entity.passive.MoCEntityMediumFish::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.SILVER_SKELETON, drzhark.mocreatures.entity.monster.MoCEntitySilverSkeleton::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.MINI_GOLEM, drzhark.mocreatures.entity.monster.MoCEntityMiniGolem::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.MANTICORE, drzhark.mocreatures.entity.monster.MoCEntityManticore::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.MANTICORE_PET, drzhark.mocreatures.entity.passive.MoCEntityManticorePet::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.BUNNY, drzhark.mocreatures.entity.passive.MoCEntityBunny::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.BEAR, drzhark.mocreatures.entity.passive.MoCEntityBear::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.BIG_CAT, drzhark.mocreatures.entity.passive.MoCEntityBigCat::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.BIRD, drzhark.mocreatures.entity.passive.MoCEntityBird::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.BOAR, drzhark.mocreatures.entity.passive.MoCEntityBoar::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.CROCODILE, drzhark.mocreatures.entity.passive.MoCEntityCrocodile::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.DEER, drzhark.mocreatures.entity.passive.MoCEntityDeer::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.DUCK, drzhark.mocreatures.entity.passive.MoCEntityDuck::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.ELEPHANT, drzhark.mocreatures.entity.passive.MoCEntityElephant::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.FOX, drzhark.mocreatures.entity.passive.MoCEntityFox::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.GOAT, drzhark.mocreatures.entity.passive.MoCEntityGoat::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.HORSE, drzhark.mocreatures.entity.passive.MoCEntityHorse::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.KITTY, drzhark.mocreatures.entity.passive.MoCEntityKitty::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.KOMODO, drzhark.mocreatures.entity.passive.MoCEntityKomodo::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.MOUSE, drzhark.mocreatures.entity.passive.MoCEntityMouse::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.OSTRICH, drzhark.mocreatures.entity.passive.MoCEntityOstrich::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.PET_SCORPION, drzhark.mocreatures.entity.passive.MoCEntityPetScorpion::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.SNAKE, drzhark.mocreatures.entity.passive.MoCEntitySnake::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.TURKEY, drzhark.mocreatures.entity.passive.MoCEntityTurkey::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.TURTLE, drzhark.mocreatures.entity.passive.MoCEntityTurtle::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.WYVERN, drzhark.mocreatures.entity.passive.MoCEntityWyvern::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.CRAB, drzhark.mocreatures.entity.passive.MoCEntityCrab::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.MAGGOT, drzhark.mocreatures.entity.passive.MoCEntityMaggot::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.SNAIL, drzhark.mocreatures.entity.passive.MoCEntitySnail::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.DOLPHIN, drzhark.mocreatures.entity.passive.MoCEntityDolphin::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.FISHY, drzhark.mocreatures.entity.passive.MoCEntityFishy::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.JELLYFISH, drzhark.mocreatures.entity.passive.MoCEntityJellyFish::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.RAY, drzhark.mocreatures.entity.passive.MoCEntityRay::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.SHARK, drzhark.mocreatures.entity.passive.MoCEntityShark::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.BEE, drzhark.mocreatures.entity.passive.MoCEntityBee::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.BUTTERFLY, drzhark.mocreatures.entity.passive.MoCEntityButterfly::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.DRAGONFLY, drzhark.mocreatures.entity.passive.MoCEntityDragonfly::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.FIREFLY, drzhark.mocreatures.entity.passive.MoCEntityFirefly::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.FLY, drzhark.mocreatures.entity.passive.MoCEntityFly::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.CRICKET, drzhark.mocreatures.entity.passive.MoCEntityCricket::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.ROACH, drzhark.mocreatures.entity.passive.MoCEntityRoach::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.GOLEM, drzhark.mocreatures.entity.monster.MoCEntityGolem::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.OGRE, drzhark.mocreatures.entity.monster.MoCEntityOgre::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.RAT, drzhark.mocreatures.entity.monster.MoCEntityRat::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.SCORPION, drzhark.mocreatures.entity.monster.MoCEntityScorpion::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.WILD_WOLF, drzhark.mocreatures.entity.monster.MoCEntityWWolf::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.WEREWOLF, drzhark.mocreatures.entity.monster.MoCEntityWerewolf::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.WRAITH, drzhark.mocreatures.entity.monster.MoCEntityWraith::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.HELL_RAT, drzhark.mocreatures.entity.monster.MoCEntityHellRat::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.FLAME_WRAITH, drzhark.mocreatures.entity.monster.MoCEntityFlameWraith::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.HORSE_MOB, drzhark.mocreatures.entity.monster.MoCEntityHorseMob::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.EGG, drzhark.mocreatures.entity.passive.MoCEntityEgg::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.KITTY_BED, drzhark.mocreatures.entity.passive.MoCEntityKittyBed::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.LITTER_BOX, drzhark.mocreatures.entity.passive.MoCEntityLitterBox::createAttributes);
        EntityAttributeRegistry.register(MoCEntities.FISH_BOWL, drzhark.mocreatures.entity.passive.MoCEntityFishBowl::createAttributes);
    }
}
