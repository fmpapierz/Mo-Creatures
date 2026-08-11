package drzhark.mocreatures.client;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/** Model layer locations for every Mo'Creatures entity model. Generated. */
public final class MoCModelLayers {

    private MoCModelLayers() {}

    public static final ModelLayerLocation BUNNY = layer("bunny");
    public static final ModelLayerLocation BEAR = layer("bear");
    public static final ModelLayerLocation BIG_CAT = layer("big_cat");
    public static final ModelLayerLocation BIRD = layer("bird");
    public static final ModelLayerLocation BOAR = layer("boar");
    public static final ModelLayerLocation CROCODILE = layer("crocodile");
    public static final ModelLayerLocation DEER = layer("deer");
    public static final ModelLayerLocation DUCK = layer("duck");
    public static final ModelLayerLocation ELEPHANT = layer("elephant");
    public static final ModelLayerLocation FOX = layer("fox");
    public static final ModelLayerLocation GOAT = layer("goat");
    public static final ModelLayerLocation HORSE = layer("horse");
    public static final ModelLayerLocation KITTY = layer("kitty");
    public static final ModelLayerLocation KOMODO = layer("komodo");
    public static final ModelLayerLocation MOUSE = layer("mouse");
    public static final ModelLayerLocation OSTRICH = layer("ostrich");
    public static final ModelLayerLocation PET_SCORPION = layer("pet_scorpion");
    public static final ModelLayerLocation SNAKE = layer("snake");
    public static final ModelLayerLocation TURKEY = layer("turkey");
    public static final ModelLayerLocation TURTLE = layer("turtle");
    public static final ModelLayerLocation WYVERN = layer("wyvern");
    public static final ModelLayerLocation CRAB = layer("crab");
    public static final ModelLayerLocation MAGGOT = layer("maggot");
    public static final ModelLayerLocation SNAIL = layer("snail");
    public static final ModelLayerLocation DOLPHIN = layer("dolphin");
    public static final ModelLayerLocation FISHY = layer("fishy");
    public static final ModelLayerLocation JELLYFISH = layer("jellyfish");
    public static final ModelLayerLocation RAY = layer("ray");
    public static final ModelLayerLocation SHARK = layer("shark");
    public static final ModelLayerLocation BEE = layer("bee");
    public static final ModelLayerLocation BUTTERFLY = layer("butterfly");
    public static final ModelLayerLocation DRAGONFLY = layer("dragonfly");
    public static final ModelLayerLocation FIREFLY = layer("firefly");
    public static final ModelLayerLocation FLY = layer("fly");
    public static final ModelLayerLocation CRICKET = layer("cricket");
    public static final ModelLayerLocation ROACH = layer("roach");
    public static final ModelLayerLocation GOLEM = layer("golem");
    public static final ModelLayerLocation OGRE = layer("ogre");
    public static final ModelLayerLocation RAT = layer("rat");
    public static final ModelLayerLocation SCORPION = layer("scorpion");
    public static final ModelLayerLocation WILD_WOLF = layer("wild_wolf");
    public static final ModelLayerLocation WEREWOLF = layer("werewolf");
    public static final ModelLayerLocation WEREHUMAN = layer("werehuman");
    public static final ModelLayerLocation EGG = layer("egg");
    public static final ModelLayerLocation KITTY_BED = layer("kitty_bed");
    public static final ModelLayerLocation LITTER_BOX = layer("litter_box");
    public static final ModelLayerLocation WRAITH = layer("wraith");
    public static final ModelLayerLocation HELL_RAT = layer("hell_rat");
    public static final ModelLayerLocation FLAME_WRAITH = layer("flame_wraith");
    public static final ModelLayerLocation HORSE_MOB = layer("horse_mob");
    public static final ModelLayerLocation FISH_BOWL = layer("fish_bowl");

    public static final ModelLayerLocation ANT = layer("ant");
    public static final ModelLayerLocation RACCOON = layer("raccoon");
    public static final ModelLayerLocation MOLE = layer("mole");
    public static final ModelLayerLocation ENT = layer("ent");
    public static final ModelLayerLocation SMALL_FISH = layer("small_fish");
    public static final ModelLayerLocation MEDIUM_FISH = layer("medium_fish");
    public static final ModelLayerLocation SILVER_SKELETON = layer("silver_skeleton");
    public static final ModelLayerLocation MINI_GOLEM = layer("mini_golem");
    /** Shared by the wild manticore and its tameable pet form. */
    public static final ModelLayerLocation MANTICORE = layer("manticore");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, name), "main");
    }
}
