package drzhark.mocreatures.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import drzhark.mocreatures.MoCreatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** All Mo'Creatures {@link EntityType}s, registered cross-loader via Architectury. Generated. */
public final class MoCEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(MoCreatures.MOD_ID, Registries.ENTITY_TYPE);

    private MoCEntities() {}

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, name));
    }

    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityBunny>> BUNNY = ENTITIES.register("bunny",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityBunny::new, MobCategory.CREATURE).sized(0.6F, 0.6F).clientTrackingRange(10).build(key("bunny")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityBear>> BEAR = ENTITIES.register("bear",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityBear::new, MobCategory.CREATURE).sized(0.9F, 1.3F).clientTrackingRange(10).build(key("bear")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityBigCat>> BIG_CAT = ENTITIES.register("big_cat",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityBigCat::new, MobCategory.CREATURE).sized(0.9F, 1.3F).clientTrackingRange(10).build(key("big_cat")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityBird>> BIRD = ENTITIES.register("bird",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityBird::new, MobCategory.CREATURE).sized(0.4F, 0.3F).clientTrackingRange(10).build(key("bird")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityBoar>> BOAR = ENTITIES.register("boar",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityBoar::new, MobCategory.CREATURE).sized(0.9F, 0.9F).clientTrackingRange(10).build(key("boar")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityCrocodile>> CROCODILE = ENTITIES.register("crocodile",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityCrocodile::new, MobCategory.CREATURE).sized(2.0F, 0.6F).clientTrackingRange(10).build(key("crocodile")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityDeer>> DEER = ENTITIES.register("deer",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityDeer::new, MobCategory.CREATURE).sized(0.9F, 1.3F).clientTrackingRange(10).build(key("deer")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityDuck>> DUCK = ENTITIES.register("duck",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityDuck::new, MobCategory.CREATURE).sized(0.3F, 0.4F).clientTrackingRange(10).build(key("duck")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityElephant>> ELEPHANT = ENTITIES.register("elephant",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityElephant::new, MobCategory.CREATURE).sized(1.1F, 3.0F)
                    // Two howdah seats anchored to the cabin pillow (model Y -16 -> 2.5 blocks above the feet).
                    // Driver centred on the pillow (+z is forward; +0.3 sat at the front edge), second behind.
                    .passengerAttachments(new net.minecraft.world.phys.Vec3(0.0D, 2.5D, -0.1D), new net.minecraft.world.phys.Vec3(0.0D, 2.5D, -0.45D))
                    .clientTrackingRange(10).build(key("elephant")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityFox>> FOX = ENTITIES.register("fox",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityFox::new, MobCategory.CREATURE).sized(0.9F, 1.3F).clientTrackingRange(10).build(key("fox")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityGoat>> GOAT = ENTITIES.register("goat",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityGoat::new, MobCategory.CREATURE).sized(1.4F, 0.9F).clientTrackingRange(10).build(key("goat")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityHorse>> HORSE = ENTITIES.register("horse",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityHorse::new, MobCategory.CREATURE).sized(1.4F, 1.6F).clientTrackingRange(10).build(key("horse")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityKitty>> KITTY = ENTITIES.register("kitty",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityKitty::new, MobCategory.CREATURE).sized(0.7F, 0.5F).clientTrackingRange(10).build(key("kitty")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityKomodo>> KOMODO = ENTITIES.register("komodo",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityKomodo::new, MobCategory.CREATURE).sized(1.6F, 0.5F).clientTrackingRange(10).build(key("komodo")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityMouse>> MOUSE = ENTITIES.register("mouse",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityMouse::new, MobCategory.CREATURE).sized(0.3F, 0.3F).clientTrackingRange(10).build(key("mouse")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityOstrich>> OSTRICH = ENTITIES.register("ostrich",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityOstrich::new, MobCategory.CREATURE).sized(1.0F, 1.6F)
                    // Seat the rider on the saddle (model Y 0.5, feet at ~23 -> ~1.4 above the feet) instead
                    // of the default hitbox-top attachment, which left the player floating slightly high.
                    .passengerAttachments(new net.minecraft.world.phys.Vec3(0.0D, 1.4D, 0.0D))
                    .clientTrackingRange(10).build(key("ostrich")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityPetScorpion>> PET_SCORPION = ENTITIES.register("pet_scorpion",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityPetScorpion::new, MobCategory.CREATURE).sized(1.4F, 0.9F).clientTrackingRange(10).build(key("pet_scorpion")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntitySnake>> SNAKE = ENTITIES.register("snake",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntitySnake::new, MobCategory.CREATURE).sized(1.4F, 0.5F).clientTrackingRange(10).build(key("snake")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityTurkey>> TURKEY = ENTITIES.register("turkey",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityTurkey::new, MobCategory.CREATURE).sized(0.5F, 0.5F).clientTrackingRange(10).build(key("turkey")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityTurtle>> TURTLE = ENTITIES.register("turtle",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityTurtle::new, MobCategory.CREATURE).sized(0.6F, 0.4F).clientTrackingRange(10).build(key("turtle")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityWyvern>> WYVERN = ENTITIES.register("wyvern",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityWyvern::new, MobCategory.CREATURE).sized(1.9F, 1.7F).clientTrackingRange(10).build(key("wyvern")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityCrab>> CRAB = ENTITIES.register("crab",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityCrab::new, MobCategory.CREATURE).sized(0.3F, 0.3F).clientTrackingRange(10).build(key("crab")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityMaggot>> MAGGOT = ENTITIES.register("maggot",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityMaggot::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("maggot")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntitySnail>> SNAIL = ENTITIES.register("snail",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntitySnail::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("snail")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityDolphin>> DOLPHIN = ENTITIES.register("dolphin",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityDolphin::new, MobCategory.WATER_CREATURE).sized(1.5F, 0.8F).clientTrackingRange(10).build(key("dolphin")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityFishy>> FISHY = ENTITIES.register("fishy",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityFishy::new, MobCategory.WATER_CREATURE).sized(0.3F, 0.3F).clientTrackingRange(10).build(key("fishy")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityJellyFish>> JELLYFISH = ENTITIES.register("jellyfish",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityJellyFish::new, MobCategory.WATER_CREATURE).sized(0.3F, 0.5F).clientTrackingRange(10).build(key("jellyfish")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityRay>> RAY = ENTITIES.register("ray",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityRay::new, MobCategory.WATER_CREATURE).sized(1.8F, 0.5F).clientTrackingRange(10).build(key("ray")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityShark>> SHARK = ENTITIES.register("shark",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityShark::new, MobCategory.WATER_CREATURE).sized(1.5F, 0.8F).clientTrackingRange(10).build(key("shark")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityBee>> BEE = ENTITIES.register("bee",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityBee::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("bee")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityButterfly>> BUTTERFLY = ENTITIES.register("butterfly",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityButterfly::new, MobCategory.CREATURE).sized(0.6F, 0.5F).clientTrackingRange(10).build(key("butterfly")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityDragonfly>> DRAGONFLY = ENTITIES.register("dragonfly",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityDragonfly::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("dragonfly")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityFirefly>> FIREFLY = ENTITIES.register("firefly",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityFirefly::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("firefly")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityFly>> FLY = ENTITIES.register("fly",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityFly::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("fly")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityCricket>> CRICKET = ENTITIES.register("cricket",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityCricket::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("cricket")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityRoach>> ROACH = ENTITIES.register("roach",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityRoach::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("roach")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityGolem>> GOLEM = ENTITIES.register("golem",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityGolem::new, MobCategory.MONSTER).sized(1.5F, 4.0F).clientTrackingRange(10).build(key("golem")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityOgre>> OGRE = ENTITIES.register("ogre",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityOgre::new, MobCategory.MONSTER).sized(1.9F, 4.0F).clientTrackingRange(10).build(key("ogre")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityRat>> RAT = ENTITIES.register("rat",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityRat::new, MobCategory.MONSTER).sized(0.5F, 0.5F).clientTrackingRange(10).build(key("rat")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityScorpion>> SCORPION = ENTITIES.register("scorpion",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityScorpion::new, MobCategory.MONSTER).sized(1.4F, 0.9F).clientTrackingRange(10).build(key("scorpion")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityWWolf>> WILD_WOLF = ENTITIES.register("wild_wolf",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityWWolf::new, MobCategory.MONSTER).sized(0.9F, 1.3F).clientTrackingRange(10).build(key("wild_wolf")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityWerewolf>> WEREWOLF = ENTITIES.register("werewolf",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityWerewolf::new, MobCategory.MONSTER).sized(0.9F, 1.3F).clientTrackingRange(10).build(key("werewolf")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityWraith>> WRAITH = ENTITIES.register("wraith",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityWraith::new, MobCategory.MONSTER).sized(1.5F, 1.5F).clientTrackingRange(10).build(key("wraith")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityHellRat>> HELL_RAT = ENTITIES.register("hell_rat",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityHellRat::new, MobCategory.MONSTER).sized(0.7F, 0.7F).clientTrackingRange(10).build(key("hell_rat")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityFlameWraith>> FLAME_WRAITH = ENTITIES.register("flame_wraith",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityFlameWraith::new, MobCategory.MONSTER).sized(1.5F, 1.5F).clientTrackingRange(10).build(key("flame_wraith")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityHorseMob>> HORSE_MOB = ENTITIES.register("horse_mob",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityHorseMob::new, MobCategory.MONSTER).sized(1.4F, 1.6F).clientTrackingRange(10).build(key("horse_mob")));

    // ------------------------------------------------------------ ported from Mo'Creatures 12.0.5
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityAnt>> ANT = ENTITIES.register("ant",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityAnt::new, MobCategory.CREATURE).sized(0.2F, 0.2F).clientTrackingRange(10).build(key("ant")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityRaccoon>> RACCOON = ENTITIES.register("raccoon",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityRaccoon::new, MobCategory.CREATURE).sized(0.5F, 0.6F).clientTrackingRange(10).build(key("raccoon")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityMole>> MOLE = ENTITIES.register("mole",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityMole::new, MobCategory.CREATURE).sized(1.0F, 0.5F).clientTrackingRange(10).build(key("mole")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityEnt>> ENT = ENTITIES.register("ent",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityEnt::new, MobCategory.CREATURE).sized(1.4F, 7.0F).clientTrackingRange(10).build(key("ent")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntitySmallFish>> SMALL_FISH = ENTITIES.register("small_fish",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntitySmallFish::new, MobCategory.WATER_CREATURE).sized(0.3F, 0.3F).clientTrackingRange(10).build(key("small_fish")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityMediumFish>> MEDIUM_FISH = ENTITIES.register("medium_fish",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityMediumFish::new, MobCategory.WATER_CREATURE).sized(0.6F, 0.3F).clientTrackingRange(10).build(key("medium_fish")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntitySilverSkeleton>> SILVER_SKELETON = ENTITIES.register("silver_skeleton",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntitySilverSkeleton::new, MobCategory.MONSTER).sized(0.9F, 1.4F).clientTrackingRange(10).build(key("silver_skeleton")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityMiniGolem>> MINI_GOLEM = ENTITIES.register("mini_golem",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityMiniGolem::new, MobCategory.MONSTER).sized(1.0F, 1.0F).clientTrackingRange(10).build(key("mini_golem")));
    /** Wild Nether manticore. The seat is where legacy {@code getMountedYOffset} put its skeleton rider. */
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.monster.MoCEntityManticore>> MANTICORE = ENTITIES.register("manticore",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.monster.MoCEntityManticore::new, MobCategory.MONSTER).sized(1.4F, 1.6F)
                    .passengerAttachments(new net.minecraft.world.phys.Vec3(0.0D, 1.1D, 0.0D))
                    .clientTrackingRange(10).build(key("manticore")));
    /** Tameable, rideable manticore, hatched from a manticore egg (legacy {@code MoCEntityManticorePet}). */
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityManticorePet>> MANTICORE_PET = ENTITIES.register("manticore_pet",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityManticorePet::new, MobCategory.CREATURE).sized(1.4F, 1.6F)
                    .passengerAttachments(new net.minecraft.world.phys.Vec3(0.0D, 1.35D, 0.0D))
                    .clientTrackingRange(10).build(key("manticore_pet")));
    /** Thrown rock projectile (a {@code ThrowableItemProjectile}, MISC category — not a spawnable mob). Hurled by the golem. */
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.projectile.MoCEntityRock>> ROCK = ENTITIES.register("rock",
            () -> EntityType.Builder.<drzhark.mocreatures.entity.projectile.MoCEntityRock>of(drzhark.mocreatures.entity.projectile.MoCEntityRock::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(key("rock")));
    /** Homing block-carrier the golem vacuums back into itself (a plain MISC {@code Entity} that renders a block). */
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.projectile.MoCEntityThrowableRock>> THROWABLE_ROCK = ENTITIES.register("throwable_rock",
            () -> EntityType.Builder.<drzhark.mocreatures.entity.projectile.MoCEntityThrowableRock>of(drzhark.mocreatures.entity.projectile.MoCEntityThrowableRock::new, MobCategory.MISC).sized(0.98F, 0.98F).clientTrackingRange(6).updateInterval(3).build(key("throwable_rock")));
    /** Thrown {@code mocegg} projectile (a {@code ThrowableItemProjectile}, MISC category — not a spawnable mob). Hatches a random-species egg on impact. */
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.projectile.MoCThrownEgg>> THROWN_EGG = ENTITIES.register("thrown_egg",
            () -> EntityType.Builder.<drzhark.mocreatures.entity.projectile.MoCThrownEgg>of(drzhark.mocreatures.entity.projectile.MoCThrownEgg::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(key("thrown_egg")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.projectile.MoCThrownDuckEgg>> THROWN_DUCK_EGG = ENTITIES.register("thrown_duck_egg",
            () -> EntityType.Builder.<drzhark.mocreatures.entity.projectile.MoCThrownDuckEgg>of(drzhark.mocreatures.entity.projectile.MoCThrownDuckEgg::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(key("thrown_duck_egg")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityEgg>> EGG = ENTITIES.register("egg",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityEgg::new, MobCategory.MISC).sized(0.35F, 0.45F).clientTrackingRange(8).build(key("egg")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityKittyBed>> KITTY_BED = ENTITIES.register("kitty_bed",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityKittyBed::new, MobCategory.MISC).sized(0.98F, 0.35F).clientTrackingRange(8).build(key("kitty_bed")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityLitterBox>> LITTER_BOX = ENTITIES.register("litter_box",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityLitterBox::new, MobCategory.MISC).sized(0.98F, 0.35F).clientTrackingRange(8).build(key("litter_box")));
    public static final RegistrySupplier<EntityType<drzhark.mocreatures.entity.passive.MoCEntityFishBowl>> FISH_BOWL = ENTITIES.register("fish_bowl",
            () -> EntityType.Builder.of(drzhark.mocreatures.entity.passive.MoCEntityFishBowl::new, MobCategory.MISC).sized(0.6F, 0.9F).clientTrackingRange(8).build(key("fish_bowl")));
}
