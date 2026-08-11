package drzhark.mocreatures.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import drzhark.mocreatures.client.renderer.MoCMobRenderer;
import drzhark.mocreatures.registry.MoCEntities;

/**
 * Client registration: model-layer definitions + entity renderers, via Architectury's
 * cross-loader client registries. Invoked (client-only) from {@code MoCreatures.init()}. Generated.
 */
public final class MoCreaturesClient {

    private MoCreaturesClient() {}

    public static void init() {
        EntityModelLayerRegistry.register(MoCModelLayers.BUNNY, drzhark.mocreatures.client.model.MoCModelBunny::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.BEAR, drzhark.mocreatures.client.model.MoCModelBear::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.BIG_CAT, drzhark.mocreatures.client.model.MoCModelBigCat::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.BIRD, drzhark.mocreatures.client.model.MoCModelBird::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.BOAR, drzhark.mocreatures.client.model.MoCModelBoar::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.CROCODILE, drzhark.mocreatures.client.model.MoCModelCrocodile::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.DEER, drzhark.mocreatures.client.model.MoCModelDeer::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.DUCK, drzhark.mocreatures.client.model.MoCModelDuck::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.ELEPHANT, drzhark.mocreatures.client.model.MoCModelElephant::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.FOX, drzhark.mocreatures.client.model.MoCModelFox::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.GOAT, drzhark.mocreatures.client.model.MoCModelGoat::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.HORSE, drzhark.mocreatures.client.model.MoCModelHorse::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.KITTY, drzhark.mocreatures.client.model.MoCModelKitty::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.KOMODO, drzhark.mocreatures.client.model.MoCModelKomodo::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.MOUSE, drzhark.mocreatures.client.model.MoCModelMouse::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.OSTRICH, drzhark.mocreatures.client.model.MoCModelOstrich::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.PET_SCORPION, drzhark.mocreatures.client.model.MoCModelPetScorpion::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.SNAKE, drzhark.mocreatures.client.model.MoCModelSnake::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.TURKEY, drzhark.mocreatures.client.model.MoCModelTurkey::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.TURTLE, drzhark.mocreatures.client.model.MoCModelTurtle::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.WYVERN, drzhark.mocreatures.client.model.MoCModelWyvern::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.CRAB, drzhark.mocreatures.client.model.MoCModelCrab::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.MAGGOT, drzhark.mocreatures.client.model.MoCModelMaggot::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.SNAIL, drzhark.mocreatures.client.model.MoCModelSnail::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.DOLPHIN, drzhark.mocreatures.client.model.MoCModelDolphin::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.FISHY, drzhark.mocreatures.client.model.MoCModelFishy::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.JELLYFISH, drzhark.mocreatures.client.model.MoCModelJellyFish::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.RAY, drzhark.mocreatures.client.model.MoCModelRay::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.SHARK, drzhark.mocreatures.client.model.MoCModelShark::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.BEE, drzhark.mocreatures.client.model.MoCModelBee::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.BUTTERFLY, drzhark.mocreatures.client.model.MoCModelButterfly::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.DRAGONFLY, drzhark.mocreatures.client.model.MoCModelDragonfly::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.FIREFLY, drzhark.mocreatures.client.model.MoCModelFirefly::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.FLY, drzhark.mocreatures.client.model.MoCModelFly::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.CRICKET, drzhark.mocreatures.client.model.MoCModelCricket::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.ROACH, drzhark.mocreatures.client.model.MoCModelRoach::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.GOLEM, drzhark.mocreatures.client.model.MoCModelGolem::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.OGRE, drzhark.mocreatures.client.model.MoCModelOgre::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.RAT, drzhark.mocreatures.client.model.MoCModelRat::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.SCORPION, drzhark.mocreatures.client.model.MoCModelScorpion::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.WILD_WOLF, drzhark.mocreatures.client.model.MoCModelWWolf::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.WEREWOLF, drzhark.mocreatures.client.model.MoCModelWerewolf::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.WEREHUMAN, drzhark.mocreatures.client.model.MoCModelWereHuman::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.EGG, drzhark.mocreatures.client.model.MoCModelEgg::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.KITTY_BED, drzhark.mocreatures.client.model.MoCModelKittyBed::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.LITTER_BOX, drzhark.mocreatures.client.model.MoCModelLitterBox::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.FISH_BOWL, drzhark.mocreatures.client.model.MoCModelFishBowl::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.WRAITH, drzhark.mocreatures.client.model.MoCModelWraith::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.HELL_RAT, drzhark.mocreatures.client.model.MoCModelHellRat::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.FLAME_WRAITH, drzhark.mocreatures.client.model.MoCModelFlameWraith::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.HORSE_MOB, drzhark.mocreatures.client.model.MoCModelHorseMob::createBodyLayer);

        EntityModelLayerRegistry.register(MoCModelLayers.ANT, drzhark.mocreatures.client.model.MoCModelAnt::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.RACCOON, drzhark.mocreatures.client.model.MoCModelRaccoon::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.MOLE, drzhark.mocreatures.client.model.MoCModelMole::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.ENT, drzhark.mocreatures.client.model.MoCModelEnt::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.SMALL_FISH, drzhark.mocreatures.client.model.MoCModelSmallFish::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.MEDIUM_FISH, drzhark.mocreatures.client.model.MoCModelMediumFish::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.SILVER_SKELETON, drzhark.mocreatures.client.model.MoCModelSilverSkeleton::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.MINI_GOLEM, drzhark.mocreatures.client.model.MoCModelMiniGolem::createBodyLayer);
        EntityModelLayerRegistry.register(MoCModelLayers.MANTICORE, drzhark.mocreatures.client.model.MoCModelManticore::createBodyLayer);

        EntityRendererRegistry.register(MoCEntities.BUNNY, context -> new MoCMobRenderer<>(context, MoCModelLayers.BUNNY, drzhark.mocreatures.client.model.MoCModelBunny::new, 0.4F));
        EntityRendererRegistry.register(MoCEntities.BEAR, context -> new MoCMobRenderer<>(context, MoCModelLayers.BEAR, drzhark.mocreatures.client.model.MoCModelBear::new, 0.45F));
        EntityRendererRegistry.register(MoCEntities.BIG_CAT, context -> new MoCMobRenderer<>(context, MoCModelLayers.BIG_CAT, drzhark.mocreatures.client.model.MoCModelBigCat::new, 0.45F));
        EntityRendererRegistry.register(MoCEntities.BIRD, context -> new MoCMobRenderer<>(context, MoCModelLayers.BIRD, drzhark.mocreatures.client.model.MoCModelBird::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.BOAR, context -> new MoCMobRenderer<>(context, MoCModelLayers.BOAR, drzhark.mocreatures.client.model.MoCModelBoar::new, 0.45F));
        EntityRendererRegistry.register(MoCEntities.CROCODILE, context -> new MoCMobRenderer<>(context, MoCModelLayers.CROCODILE, drzhark.mocreatures.client.model.MoCModelCrocodile::new, 1.0F));
        EntityRendererRegistry.register(MoCEntities.DEER, context -> new MoCMobRenderer<>(context, MoCModelLayers.DEER, drzhark.mocreatures.client.model.MoCModelDeer::new, 0.45F));
        EntityRendererRegistry.register(MoCEntities.DUCK, context -> new MoCMobRenderer<>(context, MoCModelLayers.DUCK, drzhark.mocreatures.client.model.MoCModelDuck::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.ELEPHANT, context -> new MoCMobRenderer<>(context, MoCModelLayers.ELEPHANT, drzhark.mocreatures.client.model.MoCModelElephant::new, 0.55F));
        EntityRendererRegistry.register(MoCEntities.FOX, context -> new MoCMobRenderer<>(context, MoCModelLayers.FOX, drzhark.mocreatures.client.model.MoCModelFox::new, 0.45F));
        EntityRendererRegistry.register(MoCEntities.GOAT, context -> new MoCMobRenderer<>(context, MoCModelLayers.GOAT, drzhark.mocreatures.client.model.MoCModelGoat::new, 0.7F));
        EntityRendererRegistry.register(MoCEntities.HORSE, context -> new drzhark.mocreatures.client.renderer.MoCHorseRenderer(context));
        EntityRendererRegistry.register(MoCEntities.KITTY, context -> new MoCMobRenderer<>(context, MoCModelLayers.KITTY, drzhark.mocreatures.client.model.MoCModelKitty::new, 0.35F));
        EntityRendererRegistry.register(MoCEntities.KOMODO, context -> new MoCMobRenderer<>(context, MoCModelLayers.KOMODO, drzhark.mocreatures.client.model.MoCModelKomodo::new, 0.8F));
        EntityRendererRegistry.register(MoCEntities.MOUSE, context -> new MoCMobRenderer<>(context, MoCModelLayers.MOUSE, drzhark.mocreatures.client.model.MoCModelMouse::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.OSTRICH, context -> new MoCMobRenderer<>(context, MoCModelLayers.OSTRICH, drzhark.mocreatures.client.model.MoCModelOstrich::new, 0.5F));
        EntityRendererRegistry.register(MoCEntities.PET_SCORPION, context -> new MoCMobRenderer<>(context, MoCModelLayers.PET_SCORPION, drzhark.mocreatures.client.model.MoCModelPetScorpion::new, 0.7F));
        EntityRendererRegistry.register(MoCEntities.SNAKE, context -> new MoCMobRenderer<>(context, MoCModelLayers.SNAKE, drzhark.mocreatures.client.model.MoCModelSnake::new, 0.7F));
        EntityRendererRegistry.register(MoCEntities.TURKEY, context -> new MoCMobRenderer<>(context, MoCModelLayers.TURKEY, drzhark.mocreatures.client.model.MoCModelTurkey::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.TURTLE, context -> new drzhark.mocreatures.client.renderer.MoCTurtleRenderer(context));
        EntityRendererRegistry.register(MoCEntities.WYVERN, context -> new MoCMobRenderer<>(context, MoCModelLayers.WYVERN, drzhark.mocreatures.client.model.MoCModelWyvern::new, 0.95F));
        EntityRendererRegistry.register(MoCEntities.CRAB, context -> new MoCMobRenderer<>(context, MoCModelLayers.CRAB, drzhark.mocreatures.client.model.MoCModelCrab::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.MAGGOT, context -> new MoCMobRenderer<>(context, MoCModelLayers.MAGGOT, drzhark.mocreatures.client.model.MoCModelMaggot::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.SNAIL, context -> new MoCMobRenderer<>(context, MoCModelLayers.SNAIL, drzhark.mocreatures.client.model.MoCModelSnail::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.DOLPHIN, context -> new MoCMobRenderer<>(context, MoCModelLayers.DOLPHIN, drzhark.mocreatures.client.model.MoCModelDolphin::new, 0.75F));
        EntityRendererRegistry.register(MoCEntities.FISHY, context -> new MoCMobRenderer<>(context, MoCModelLayers.FISHY, drzhark.mocreatures.client.model.MoCModelFishy::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.JELLYFISH, context -> new drzhark.mocreatures.client.renderer.MoCJellyFishRenderer(context));
        EntityRendererRegistry.register(MoCEntities.RAY, context -> new MoCMobRenderer<>(context, MoCModelLayers.RAY, drzhark.mocreatures.client.model.MoCModelRay::new, 0.9F));
        EntityRendererRegistry.register(MoCEntities.SHARK, context -> new MoCMobRenderer<>(context, MoCModelLayers.SHARK, drzhark.mocreatures.client.model.MoCModelShark::new, 0.75F));
        EntityRendererRegistry.register(MoCEntities.BEE, context -> new MoCMobRenderer<>(context, MoCModelLayers.BEE, drzhark.mocreatures.client.model.MoCModelBee::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.BUTTERFLY, context -> new MoCMobRenderer<>(context, MoCModelLayers.BUTTERFLY, drzhark.mocreatures.client.model.MoCModelButterfly::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.DRAGONFLY, context -> new MoCMobRenderer<>(context, MoCModelLayers.DRAGONFLY, drzhark.mocreatures.client.model.MoCModelDragonfly::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.FIREFLY, context -> new drzhark.mocreatures.client.renderer.MoCFireflyRenderer(context));
        EntityRendererRegistry.register(MoCEntities.FLY, context -> new MoCMobRenderer<>(context, MoCModelLayers.FLY, drzhark.mocreatures.client.model.MoCModelFly::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.CRICKET, context -> new MoCMobRenderer<>(context, MoCModelLayers.CRICKET, drzhark.mocreatures.client.model.MoCModelCricket::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.ROACH, context -> new MoCMobRenderer<>(context, MoCModelLayers.ROACH, drzhark.mocreatures.client.model.MoCModelRoach::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.GOLEM, drzhark.mocreatures.client.renderer.MoCGolemRenderer::new);
        EntityRendererRegistry.register(MoCEntities.OGRE, context -> new MoCMobRenderer<>(context, MoCModelLayers.OGRE, drzhark.mocreatures.client.model.MoCModelOgre::new, 0.95F));
        EntityRendererRegistry.register(MoCEntities.RAT, context -> new MoCMobRenderer<>(context, MoCModelLayers.RAT, drzhark.mocreatures.client.model.MoCModelRat::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.SCORPION, context -> new MoCMobRenderer<>(context, MoCModelLayers.SCORPION, drzhark.mocreatures.client.model.MoCModelScorpion::new, 0.7F));
        EntityRendererRegistry.register(MoCEntities.WILD_WOLF, context -> new MoCMobRenderer<>(context, MoCModelLayers.WILD_WOLF, drzhark.mocreatures.client.model.MoCModelWWolf::new, 0.45F));
        EntityRendererRegistry.register(MoCEntities.WEREWOLF, context -> new drzhark.mocreatures.client.renderer.MoCWerewolfRenderer(context));
        EntityRendererRegistry.register(MoCEntities.WRAITH, context -> new drzhark.mocreatures.client.renderer.MoCWraithRenderer(context));
        EntityRendererRegistry.register(MoCEntities.HELL_RAT, context -> new MoCMobRenderer<>(context, MoCModelLayers.HELL_RAT, drzhark.mocreatures.client.model.MoCModelHellRat::new, 0.35F));
        EntityRendererRegistry.register(MoCEntities.FLAME_WRAITH, context -> new drzhark.mocreatures.client.renderer.MoCFlameWraithRenderer(context));
        EntityRendererRegistry.register(MoCEntities.HORSE_MOB, context -> new MoCMobRenderer<>(context, MoCModelLayers.HORSE_MOB, drzhark.mocreatures.client.model.MoCModelHorseMob::new, 0.7F));
        EntityRendererRegistry.register(MoCEntities.ROCK, context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context));
        EntityRendererRegistry.register(MoCEntities.THROWN_EGG, context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context));
        EntityRendererRegistry.register(MoCEntities.THROWN_DUCK_EGG, context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context));
        EntityRendererRegistry.register(MoCEntities.THROWABLE_ROCK, context -> new drzhark.mocreatures.client.renderer.MoCThrowableRockRenderer(context));
        EntityRendererRegistry.register(MoCEntities.EGG, context -> new MoCMobRenderer<>(context, MoCModelLayers.EGG, drzhark.mocreatures.client.model.MoCModelEgg::new, 0.2F));
        EntityRendererRegistry.register(MoCEntities.KITTY_BED, context -> new drzhark.mocreatures.client.renderer.MoCKittyBedRenderer(context, MoCModelLayers.KITTY_BED, drzhark.mocreatures.client.model.MoCModelKittyBed::new, 0.0F));
        EntityRendererRegistry.register(MoCEntities.LITTER_BOX, context -> new MoCMobRenderer<>(context, MoCModelLayers.LITTER_BOX, drzhark.mocreatures.client.model.MoCModelLitterBox::new, 0.0F));
        EntityRendererRegistry.register(MoCEntities.FISH_BOWL, context -> new MoCMobRenderer<>(context, MoCModelLayers.FISH_BOWL, drzhark.mocreatures.client.model.MoCModelFishBowl::new, 0.3F));


        // ------------------------------------------------------ ported from Mo'Creatures 12.0.5
        EntityRendererRegistry.register(MoCEntities.ANT, context -> new MoCMobRenderer<>(context, MoCModelLayers.ANT, drzhark.mocreatures.client.model.MoCModelAnt::new, 0.3F));
        EntityRendererRegistry.register(MoCEntities.RACCOON, context -> new MoCMobRenderer<>(context, MoCModelLayers.RACCOON, drzhark.mocreatures.client.model.MoCModelRaccoon::new, 0.4F));
        EntityRendererRegistry.register(MoCEntities.MOLE, context -> new MoCMobRenderer<>(context, MoCModelLayers.MOLE, drzhark.mocreatures.client.model.MoCModelMole::new, 0.4F));
        EntityRendererRegistry.register(MoCEntities.ENT, context -> new MoCMobRenderer<>(context, MoCModelLayers.ENT, drzhark.mocreatures.client.model.MoCModelEnt::new, 0.5F));
        EntityRendererRegistry.register(MoCEntities.SMALL_FISH, drzhark.mocreatures.client.renderer.MoCSmallFishRenderer::new);
        EntityRendererRegistry.register(MoCEntities.MEDIUM_FISH, drzhark.mocreatures.client.renderer.MoCMediumFishRenderer::new);
        EntityRendererRegistry.register(MoCEntities.SILVER_SKELETON, context -> new MoCMobRenderer<>(context, MoCModelLayers.SILVER_SKELETON, drzhark.mocreatures.client.model.MoCModelSilverSkeleton::new, 0.6F));
        EntityRendererRegistry.register(MoCEntities.MINI_GOLEM, drzhark.mocreatures.client.renderer.MoCMiniGolemRenderer::new);
        EntityRendererRegistry.register(MoCEntities.MANTICORE, context -> new MoCMobRenderer<>(context, MoCModelLayers.MANTICORE, drzhark.mocreatures.client.model.MoCModelManticore::new, 0.7F));
        EntityRendererRegistry.register(MoCEntities.MANTICORE_PET, context -> new MoCMobRenderer<>(context, MoCModelLayers.MANTICORE, drzhark.mocreatures.client.model.MoCModelManticore::new, 0.7F));

        // Pet HUD overlay (legacy displayPetName / displayPetHealth): draws a crosshair-target
        // readout for the local player's tamed Mo'Creatures pets.
        MoCPetHud.register();

        // Settings keybind (legacy F9 MoCSettings): registers the mapping + opens MoCSettingsScreen.
        MoCKeyMappings.register();

        // Custom particle providers (legacy FX star / undead / vanish).
        MoCParticleFactories.register();
    }
}
