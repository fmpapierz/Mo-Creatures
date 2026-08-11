package drzhark.mocreatures.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import drzhark.mocreatures.MoCreatures;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorType;

/** All Mo'Creatures items: spawn eggs + equipment/materials/food + block items. Generated. */
public final class MoCItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MoCreatures.MOD_ID, Registries.ITEM);

    private MoCItems() {}

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, name));
    }

    public static final RegistrySupplier<SpawnEggItem> BUNNY_SPAWN_EGG = ITEMS.register("bunny_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.BUNNY.get()).setId(itemKey("bunny_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> BEAR_SPAWN_EGG = ITEMS.register("bear_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.BEAR.get()).setId(itemKey("bear_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> BIG_CAT_SPAWN_EGG = ITEMS.register("big_cat_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.BIG_CAT.get()).setId(itemKey("big_cat_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> BIRD_SPAWN_EGG = ITEMS.register("bird_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.BIRD.get()).setId(itemKey("bird_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> BOAR_SPAWN_EGG = ITEMS.register("boar_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.BOAR.get()).setId(itemKey("boar_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> CROCODILE_SPAWN_EGG = ITEMS.register("crocodile_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.CROCODILE.get()).setId(itemKey("crocodile_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> DEER_SPAWN_EGG = ITEMS.register("deer_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.DEER.get()).setId(itemKey("deer_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> DUCK_SPAWN_EGG = ITEMS.register("duck_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.DUCK.get()).setId(itemKey("duck_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> ELEPHANT_SPAWN_EGG = ITEMS.register("elephant_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.ELEPHANT.get()).setId(itemKey("elephant_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> FOX_SPAWN_EGG = ITEMS.register("fox_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.FOX.get()).setId(itemKey("fox_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> GOAT_SPAWN_EGG = ITEMS.register("goat_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.GOAT.get()).setId(itemKey("goat_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> HORSE_SPAWN_EGG = ITEMS.register("horse_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.HORSE.get()).setId(itemKey("horse_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> KITTY_SPAWN_EGG = ITEMS.register("kitty_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.KITTY.get()).setId(itemKey("kitty_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> KOMODO_SPAWN_EGG = ITEMS.register("komodo_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.KOMODO.get()).setId(itemKey("komodo_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> MOUSE_SPAWN_EGG = ITEMS.register("mouse_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.MOUSE.get()).setId(itemKey("mouse_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> OSTRICH_SPAWN_EGG = ITEMS.register("ostrich_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.OSTRICH.get()).setId(itemKey("ostrich_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> PET_SCORPION_SPAWN_EGG = ITEMS.register("pet_scorpion_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.PET_SCORPION.get()).setId(itemKey("pet_scorpion_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> SNAKE_SPAWN_EGG = ITEMS.register("snake_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.SNAKE.get()).setId(itemKey("snake_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> TURKEY_SPAWN_EGG = ITEMS.register("turkey_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.TURKEY.get()).setId(itemKey("turkey_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> TURTLE_SPAWN_EGG = ITEMS.register("turtle_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.TURTLE.get()).setId(itemKey("turtle_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> WYVERN_SPAWN_EGG = ITEMS.register("wyvern_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.WYVERN.get()).setId(itemKey("wyvern_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> CRAB_SPAWN_EGG = ITEMS.register("crab_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.CRAB.get()).setId(itemKey("crab_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> MAGGOT_SPAWN_EGG = ITEMS.register("maggot_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.MAGGOT.get()).setId(itemKey("maggot_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> SNAIL_SPAWN_EGG = ITEMS.register("snail_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.SNAIL.get()).setId(itemKey("snail_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> DOLPHIN_SPAWN_EGG = ITEMS.register("dolphin_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.DOLPHIN.get()).setId(itemKey("dolphin_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> FISHY_SPAWN_EGG = ITEMS.register("fishy_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.FISHY.get()).setId(itemKey("fishy_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> JELLYFISH_SPAWN_EGG = ITEMS.register("jellyfish_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.JELLYFISH.get()).setId(itemKey("jellyfish_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> RAY_SPAWN_EGG = ITEMS.register("ray_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.RAY.get()).setId(itemKey("ray_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> SHARK_SPAWN_EGG = ITEMS.register("shark_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.SHARK.get()).setId(itemKey("shark_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> BEE_SPAWN_EGG = ITEMS.register("bee_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.BEE.get()).setId(itemKey("bee_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> BUTTERFLY_SPAWN_EGG = ITEMS.register("butterfly_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.BUTTERFLY.get()).setId(itemKey("butterfly_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> DRAGONFLY_SPAWN_EGG = ITEMS.register("dragonfly_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.DRAGONFLY.get()).setId(itemKey("dragonfly_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> FIREFLY_SPAWN_EGG = ITEMS.register("firefly_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.FIREFLY.get()).setId(itemKey("firefly_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> FLY_SPAWN_EGG = ITEMS.register("fly_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.FLY.get()).setId(itemKey("fly_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> CRICKET_SPAWN_EGG = ITEMS.register("cricket_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.CRICKET.get()).setId(itemKey("cricket_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> ROACH_SPAWN_EGG = ITEMS.register("roach_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.ROACH.get()).setId(itemKey("roach_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> GOLEM_SPAWN_EGG = ITEMS.register("golem_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.GOLEM.get()).setId(itemKey("golem_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> OGRE_SPAWN_EGG = ITEMS.register("ogre_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.OGRE.get()).setId(itemKey("ogre_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> RAT_SPAWN_EGG = ITEMS.register("rat_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.RAT.get()).setId(itemKey("rat_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> SCORPION_SPAWN_EGG = ITEMS.register("scorpion_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.SCORPION.get()).setId(itemKey("scorpion_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> WILD_WOLF_SPAWN_EGG = ITEMS.register("wild_wolf_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.WILD_WOLF.get()).setId(itemKey("wild_wolf_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> WEREWOLF_SPAWN_EGG = ITEMS.register("werewolf_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.WEREWOLF.get()).setId(itemKey("werewolf_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> WRAITH_SPAWN_EGG = ITEMS.register("wraith_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.WRAITH.get()).setId(itemKey("wraith_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> HELL_RAT_SPAWN_EGG = ITEMS.register("hell_rat_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.HELL_RAT.get()).setId(itemKey("hell_rat_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> FLAME_WRAITH_SPAWN_EGG = ITEMS.register("flame_wraith_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.FLAME_WRAITH.get()).setId(itemKey("flame_wraith_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> HORSE_MOB_SPAWN_EGG = ITEMS.register("horse_mob_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.HORSE_MOB.get()).setId(itemKey("horse_mob_spawn_egg"))));
    // ------------------------------------------- spawn eggs for the creatures ported from 12.0.5
    public static final RegistrySupplier<SpawnEggItem> ANT_SPAWN_EGG = ITEMS.register("ant_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.ANT.get()).setId(itemKey("ant_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> RACCOON_SPAWN_EGG = ITEMS.register("raccoon_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.RACCOON.get()).setId(itemKey("raccoon_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> MOLE_SPAWN_EGG = ITEMS.register("mole_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.MOLE.get()).setId(itemKey("mole_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> ENT_SPAWN_EGG = ITEMS.register("ent_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.ENT.get()).setId(itemKey("ent_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> SMALL_FISH_SPAWN_EGG = ITEMS.register("small_fish_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.SMALL_FISH.get()).setId(itemKey("small_fish_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> MEDIUM_FISH_SPAWN_EGG = ITEMS.register("medium_fish_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.MEDIUM_FISH.get()).setId(itemKey("medium_fish_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> SILVER_SKELETON_SPAWN_EGG = ITEMS.register("silver_skeleton_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.SILVER_SKELETON.get()).setId(itemKey("silver_skeleton_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> MINI_GOLEM_SPAWN_EGG = ITEMS.register("mini_golem_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.MINI_GOLEM.get()).setId(itemKey("mini_golem_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> MANTICORE_SPAWN_EGG = ITEMS.register("manticore_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.MANTICORE.get()).setId(itemKey("manticore_spawn_egg"))));
    public static final RegistrySupplier<SpawnEggItem> MANTICORE_PET_SPAWN_EGG = ITEMS.register("manticore_pet_spawn_egg",
            () -> new SpawnEggItem(new Item.Properties().spawnEgg(MoCEntities.MANTICORE_PET.get()).setId(itemKey("manticore_pet_spawn_egg"))));
    public static final RegistrySupplier<Item> AMULETBONE = ITEMS.register("amuletbone",
            () -> new Item(new Item.Properties().setId(itemKey("amuletbone"))));
    // Full special-horse amulets store the captured horse (coat + name) and release it on use, returning the
    // matching empty amulet. The empty amulets capture a matching horse via MoCEntityHorse.mobInteract.
    public static final RegistrySupplier<Item> AMULETBONEFULL = ITEMS.register("amuletbonefull",
            () -> new drzhark.mocreatures.item.MoCSpecialAmuletItem(new Item.Properties().stacksTo(1).setId(itemKey("amuletbonefull")), AMULETBONE));
    public static final RegistrySupplier<Item> AMULETFAIRY = ITEMS.register("amuletfairy",
            () -> new Item(new Item.Properties().setId(itemKey("amuletfairy"))));
    public static final RegistrySupplier<Item> AMULETFAIRYFULL = ITEMS.register("amuletfairyfull",
            () -> new drzhark.mocreatures.item.MoCSpecialAmuletItem(new Item.Properties().stacksTo(1).setId(itemKey("amuletfairyfull")), AMULETFAIRY));
    public static final RegistrySupplier<Item> AMULETGHOST = ITEMS.register("amuletghost",
            () -> new Item(new Item.Properties().setId(itemKey("amuletghost"))));
    public static final RegistrySupplier<Item> AMULETGHOSTFULL = ITEMS.register("amuletghostfull",
            () -> new drzhark.mocreatures.item.MoCSpecialAmuletItem(new Item.Properties().stacksTo(1).setId(itemKey("amuletghostfull")), AMULETGHOST));
    public static final RegistrySupplier<Item> AMULETPEGASUS = ITEMS.register("amuletpegasus",
            () -> new Item(new Item.Properties().setId(itemKey("amuletpegasus"))));
    public static final RegistrySupplier<Item> AMULETPEGASUSFULL = ITEMS.register("amuletpegasusfull",
            () -> new drzhark.mocreatures.item.MoCSpecialAmuletItem(new Item.Properties().stacksTo(1).setId(itemKey("amuletpegasusfull")), AMULETPEGASUS));
    public static final RegistrySupplier<Item> ARMORDIAMOND = ITEMS.register("armordiamond",
            () -> new Item(new Item.Properties().setId(itemKey("armordiamond"))));
    public static final RegistrySupplier<Item> ARMORGOLD = ITEMS.register("armorgold",
            () -> new Item(new Item.Properties().setId(itemKey("armorgold"))));
    public static final RegistrySupplier<Item> ARMORMETAL = ITEMS.register("armormetal",
            () -> new Item(new Item.Properties().setId(itemKey("armormetal"))));
    public static final RegistrySupplier<Item> BIGCATCLAW = ITEMS.register("bigcatclaw",
            () -> new Item(new Item.Properties().setId(itemKey("bigcatclaw"))));
    public static final RegistrySupplier<Item> BO = ITEMS.register("bo",
            () -> new Item(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("bo"))));
    public static final RegistrySupplier<Item> BUILDERHAMMER = ITEMS.register("builderhammer",
            () -> new drzhark.mocreatures.item.MoCBuilderHammerItem(new Item.Properties().setId(itemKey("builderhammer"))));
    public static final RegistrySupplier<Item> BUILDERHAMMER2 = ITEMS.register("builderhammer2",
            () -> new drzhark.mocreatures.item.MoCBuilderHammerItem(new Item.Properties().setId(itemKey("builderhammer2"))));
    public static final RegistrySupplier<Item> CHITIN = ITEMS.register("chitin",
            () -> new Item(new Item.Properties().setId(itemKey("chitin"))));
    public static final RegistrySupplier<Item> CHITINBLACK = ITEMS.register("chitinblack",
            () -> new Item(new Item.Properties().setId(itemKey("chitinblack"))));
    public static final RegistrySupplier<Item> CHITINFROST = ITEMS.register("chitinfrost",
            () -> new Item(new Item.Properties().setId(itemKey("chitinfrost"))));
    public static final RegistrySupplier<Item> CHITINNETHER = ITEMS.register("chitinnether",
            () -> new Item(new Item.Properties().setId(itemKey("chitinnether"))));
    public static final RegistrySupplier<Item> CRABCOOKED = ITEMS.register("crabcooked",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()).setId(itemKey("crabcooked"))));
    public static final RegistrySupplier<Item> CRABRAW = ITEMS.register("crabraw",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0), 0.8F)).build()).setId(itemKey("crabraw"))));
    public static final RegistrySupplier<Item> CREATUREPEDIA = ITEMS.register("creaturepedia",
            () -> new drzhark.mocreatures.item.MoCCreaturePediaItem(new Item.Properties().setId(itemKey("creaturepedia"))));
    public static final RegistrySupplier<Item> ELEPHANTCHEST = ITEMS.register("elephantchest",
            () -> new Item(new Item.Properties().setId(itemKey("elephantchest"))));
    public static final RegistrySupplier<Item> ELEPHANTGARMENT = ITEMS.register("elephantgarment",
            () -> new Item(new Item.Properties().setId(itemKey("elephantgarment"))));
    public static final RegistrySupplier<Item> ELEPHANTHARNESS = ITEMS.register("elephantharness",
            () -> new Item(new Item.Properties().setId(itemKey("elephantharness"))));
    public static final RegistrySupplier<Item> ELEPHANTHOWDAH = ITEMS.register("elephanthowdah",
            () -> new Item(new Item.Properties().setId(itemKey("elephanthowdah"))));
    public static final RegistrySupplier<Item> ESSENCEDARKNESS = ITEMS.register("essencedarkness",
            () -> new Item(new Item.Properties().setId(itemKey("essencedarkness"))));
    public static final RegistrySupplier<Item> ESSENCEFIRE = ITEMS.register("essencefire",
            () -> new Item(new Item.Properties().setId(itemKey("essencefire"))));
    public static final RegistrySupplier<Item> ESSENCELIGHT = ITEMS.register("essencelight",
            () -> new Item(new Item.Properties().setId(itemKey("essencelight"))));
    public static final RegistrySupplier<Item> ESSENCEUNDEAD = ITEMS.register("essenceundead",
            () -> new Item(new Item.Properties().setId(itemKey("essenceundead"))));
    public static final RegistrySupplier<Item> FISHNET = ITEMS.register("fishnet",
            () -> new Item(new Item.Properties().setId(itemKey("fishnet"))));
    public static final RegistrySupplier<Item> FISHNETFULL = ITEMS.register("fishnetfull",
            () -> new Item(new Item.Properties().setId(itemKey("fishnetfull"))));
    // ---- fish bowl (12 variants): empty, water-filled, and one per captured fishy type (1-10) ----
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_EMPTY = ITEMS.register("fishbowl_empty",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_empty")), drzhark.mocreatures.item.MoCFishBowlItem.Role.EMPTY, 0));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_WATER = ITEMS.register("fishbowl_water",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_water")), drzhark.mocreatures.item.MoCFishBowlItem.Role.WATER, 11));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_1 = ITEMS.register("fishbowl_1",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_1")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 1));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_2 = ITEMS.register("fishbowl_2",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_2")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 2));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_3 = ITEMS.register("fishbowl_3",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_3")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 3));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_4 = ITEMS.register("fishbowl_4",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_4")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 4));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_5 = ITEMS.register("fishbowl_5",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_5")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 5));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_6 = ITEMS.register("fishbowl_6",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_6")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 6));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_7 = ITEMS.register("fishbowl_7",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_7")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 7));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_8 = ITEMS.register("fishbowl_8",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_8")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 8));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_9 = ITEMS.register("fishbowl_9",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_9")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 9));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFishBowlItem> FISHBOWL_10 = ITEMS.register("fishbowl_10",
            () -> new drzhark.mocreatures.item.MoCFishBowlItem(new Item.Properties().stacksTo(16).setId(itemKey("fishbowl_10")), drzhark.mocreatures.item.MoCFishBowlItem.Role.FISH, 10));
    public static final RegistrySupplier<Item> FUR = ITEMS.register("fur",
            () -> new Item(new Item.Properties().setId(itemKey("fur"))));
    // Fur and hide armor are IRON-tier: legacy furARMOR/hideARMOR were both EnumHelper.addArmorMaterial(..,15,{2,6,5,2},12)
    // — identical to crocARMOR (the reptile set) and to modern IRON's {2,6,5,2} reduction. Not LEATHER ({1,3,2,1}), which
    // would roughly halve the protection DrZhark intended.
    // The material is MoCArmorMaterials.FUR rather than vanilla ArmorMaterials.IRON: the numbers are the same, but an
    // ArmorMaterial also carries the EquipmentAsset key that picks the worn texture, and IRON's is minecraft:iron —
    // which is why every Mo'Creatures set used to render as iron plate on the player. See MoCArmorMaterials.
    public static final RegistrySupplier<Item> FURBOOTS = ITEMS.register("furboots",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.FUR, ArmorType.BOOTS).setId(itemKey("furboots"))));
    public static final RegistrySupplier<Item> FURCHEST = ITEMS.register("furchest",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.FUR, ArmorType.CHESTPLATE).setId(itemKey("furchest"))));
    public static final RegistrySupplier<Item> FURHELMET = ITEMS.register("furhelmet",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.FUR, ArmorType.HELMET).setId(itemKey("furhelmet"))));
    public static final RegistrySupplier<Item> FURLEGS = ITEMS.register("furlegs",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.FUR, ArmorType.LEGGINGS).setId(itemKey("furlegs"))));
    public static final RegistrySupplier<Item> HAYSTACK = ITEMS.register("haystack",
            () -> new Item(new Item.Properties().setId(itemKey("haystack"))));
    public static final RegistrySupplier<Item> HEARTDARKNESS = ITEMS.register("heartdarkness",
            () -> new Item(new Item.Properties().setId(itemKey("heartdarkness"))));
    public static final RegistrySupplier<Item> HEARTFIRE = ITEMS.register("heartfire",
            () -> new Item(new Item.Properties().setId(itemKey("heartfire"))));
    public static final RegistrySupplier<Item> HEARTUNDEAD = ITEMS.register("heartundead",
            () -> new Item(new Item.Properties().setId(itemKey("heartundead"))));
    public static final RegistrySupplier<Item> HIDE = ITEMS.register("hide",
            () -> new Item(new Item.Properties().setId(itemKey("hide"))));
    public static final RegistrySupplier<Item> HIDEBOOTS = ITEMS.register("hideboots",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.HIDE, ArmorType.BOOTS).setId(itemKey("hideboots"))));
    public static final RegistrySupplier<Item> HIDECHEST = ITEMS.register("hidechest",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.HIDE, ArmorType.CHESTPLATE).setId(itemKey("hidechest"))));
    public static final RegistrySupplier<Item> HIDEHELMET = ITEMS.register("hidehelmet",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.HIDE, ArmorType.HELMET).setId(itemKey("hidehelmet"))));
    public static final RegistrySupplier<Item> HIDELEGS = ITEMS.register("hidelegs",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.HIDE, ArmorType.LEGGINGS).setId(itemKey("hidelegs"))));
    public static final RegistrySupplier<Item> HORSEARMORCRYSTAL = ITEMS.register("horsearmorcrystal",
            () -> new Item(new Item.Properties().setId(itemKey("horsearmorcrystal"))));
    public static final RegistrySupplier<Item> HORSESADDLE = ITEMS.register("horsesaddle",
            () -> new Item(new Item.Properties().setId(itemKey("horsesaddle"))));
    public static final RegistrySupplier<Item> KATANA = ITEMS.register("katana",
            () -> new Item(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("katana"))));
    public static final RegistrySupplier<Item> KEY = ITEMS.register("key",
            () -> new Item(new Item.Properties().setId(itemKey("key"))));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFurnitureItem> KITTYBED = ITEMS.register("kittybed",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed")), MoCEntities.KITTY_BED));
    // The 16 dyed kitty beds are placeable furniture too (legacy: one damage-keyed kittybed item; here split
    // into 16 named items). Each carries its dye colour = DyeColor id + 1, stamped onto the placed bed so it
    // renders tinted and hands the matching coloured item back on pickaxe pickup.
    public static final RegistrySupplier<Item> KITTYBED_BLACK = ITEMS.register("kittybed_black",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_black")), MoCEntities.KITTY_BED, 16));
    public static final RegistrySupplier<Item> KITTYBED_BLUE = ITEMS.register("kittybed_blue",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_blue")), MoCEntities.KITTY_BED, 12));
    public static final RegistrySupplier<Item> KITTYBED_BROWN = ITEMS.register("kittybed_brown",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_brown")), MoCEntities.KITTY_BED, 13));
    public static final RegistrySupplier<Item> KITTYBED_CYAN = ITEMS.register("kittybed_cyan",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_cyan")), MoCEntities.KITTY_BED, 10));
    public static final RegistrySupplier<Item> KITTYBED_GRAY = ITEMS.register("kittybed_gray",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_gray")), MoCEntities.KITTY_BED, 8));
    public static final RegistrySupplier<Item> KITTYBED_GREEN = ITEMS.register("kittybed_green",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_green")), MoCEntities.KITTY_BED, 14));
    public static final RegistrySupplier<Item> KITTYBED_LIGHT_BLUE = ITEMS.register("kittybed_light_blue",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_light_blue")), MoCEntities.KITTY_BED, 4));
    public static final RegistrySupplier<Item> KITTYBED_LIME = ITEMS.register("kittybed_lime",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_lime")), MoCEntities.KITTY_BED, 6));
    public static final RegistrySupplier<Item> KITTYBED_MAGENTA = ITEMS.register("kittybed_magenta",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_magenta")), MoCEntities.KITTY_BED, 3));
    public static final RegistrySupplier<Item> KITTYBED_ORANGE = ITEMS.register("kittybed_orange",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_orange")), MoCEntities.KITTY_BED, 2));
    public static final RegistrySupplier<Item> KITTYBED_PINK = ITEMS.register("kittybed_pink",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_pink")), MoCEntities.KITTY_BED, 7));
    public static final RegistrySupplier<Item> KITTYBED_PURPLE = ITEMS.register("kittybed_purple",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_purple")), MoCEntities.KITTY_BED, 11));
    public static final RegistrySupplier<Item> KITTYBED_RED = ITEMS.register("kittybed_red",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_red")), MoCEntities.KITTY_BED, 15));
    public static final RegistrySupplier<Item> KITTYBED_SILVER = ITEMS.register("kittybed_silver",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_silver")), MoCEntities.KITTY_BED, 9));
    public static final RegistrySupplier<Item> KITTYBED_WHITE = ITEMS.register("kittybed_white",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_white")), MoCEntities.KITTY_BED, 1));
    public static final RegistrySupplier<Item> KITTYBED_YELLOW = ITEMS.register("kittybed_yellow",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittybed_yellow")), MoCEntities.KITTY_BED, 5));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCFurnitureItem> KITTYLITTER = ITEMS.register("kittylitter",
            () -> new drzhark.mocreatures.item.MoCFurnitureItem(new Item.Properties().setId(itemKey("kittylitter")), MoCEntities.LITTER_BOX));
    public static final RegistrySupplier<Item> MAMMOTHPLATFORM = ITEMS.register("mammothplatform",
            () -> new Item(new Item.Properties().setId(itemKey("mammothplatform"))));
    public static final RegistrySupplier<Item> MEDALLION = ITEMS.register("medallion",
            () -> new Item(new Item.Properties().setId(itemKey("medallion"))));
    public static final RegistrySupplier<Item> MOCEGG = ITEMS.register("mocegg",
            () -> new drzhark.mocreatures.item.MoCThrownEggItem(new Item.Properties().stacksTo(16).setId(itemKey("mocegg"))));
    /** Duck egg: a throwable egg (chance to hatch a duckling) that also crafts like a chicken egg. */
    public static final RegistrySupplier<Item> DUCK_EGG = ITEMS.register("duck_egg",
            () -> new drzhark.mocreatures.item.MoCDuckEggItem(new Item.Properties().stacksTo(16).setId(itemKey("duck_egg"))));
    public static final RegistrySupplier<Item> NETHERCANNON = ITEMS.register("nethercannon",
            () -> new drzhark.mocreatures.item.MoCNetherCannonItem(new Item.Properties().setId(itemKey("nethercannon"))));
    public static final RegistrySupplier<Item> NUNCHAKU = ITEMS.register("nunchaku",
            () -> new Item(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("nunchaku"))));
    public static final RegistrySupplier<Item> OMELET = ITEMS.register("omelet",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()).setId(itemKey("omelet"))));
    public static final RegistrySupplier<Item> OSTRICHCOOKED = ITEMS.register("ostrichcooked",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()).setId(itemKey("ostrichcooked"))));
    public static final RegistrySupplier<Item> OSTRICHRAW = ITEMS.register("ostrichraw",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0), 0.8F)).build()).setId(itemKey("ostrichraw"))));
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCAmuletItem> PETAMULET = ITEMS.register("petamulet",
            () -> new drzhark.mocreatures.item.MoCAmuletItem(new Item.Properties().setId(itemKey("petamulet"))));
    public static final RegistrySupplier<Item> PETAMULETFULL = ITEMS.register("petamuletfull",
            () -> new Item(new Item.Properties().setId(itemKey("petamuletfull"))));
    public static final RegistrySupplier<Item> PETFOOD = ITEMS.register("petfood",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()).setId(itemKey("petfood"))));
    public static final RegistrySupplier<Item> RATBURGER = ITEMS.register("ratburger",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()).setId(itemKey("ratburger"))));
    public static final RegistrySupplier<Item> RATCOOKED = ITEMS.register("ratcooked",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()).setId(itemKey("ratcooked"))));
    public static final RegistrySupplier<Item> RATRAW = ITEMS.register("ratraw",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0), 0.8F)).build()).setId(itemKey("ratraw"))));
    public static final RegistrySupplier<Item> RECORDSHUFFLE = ITEMS.register("recordshuffle",
            () -> new Item(new Item.Properties().stacksTo(1)
                    .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "shuffle")))
                    .setId(itemKey("recordshuffle"))));
    public static final RegistrySupplier<Item> REPTILEBOOTS = ITEMS.register("reptileboots",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.REPTILE, ArmorType.BOOTS).setId(itemKey("reptileboots"))));
    public static final RegistrySupplier<Item> REPTILEHELMET = ITEMS.register("reptilehelmet",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.REPTILE, ArmorType.HELMET).setId(itemKey("reptilehelmet"))));
    public static final RegistrySupplier<Item> REPTILEHIDE = ITEMS.register("reptilehide",
            () -> new Item(new Item.Properties().setId(itemKey("reptilehide"))));
    public static final RegistrySupplier<Item> REPTILELEGS = ITEMS.register("reptilelegs",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.REPTILE, ArmorType.LEGGINGS).setId(itemKey("reptilelegs"))));
    public static final RegistrySupplier<Item> REPTILEPLATE = ITEMS.register("reptileplate",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.REPTILE, ArmorType.CHESTPLATE).setId(itemKey("reptileplate"))));
    public static final RegistrySupplier<Item> ROPE = ITEMS.register("rope",
            () -> new drzhark.mocreatures.item.MoCRopeItem(new Item.Properties().setId(itemKey("rope"))));
    public static final RegistrySupplier<Item> SAI = ITEMS.register("sai",
            () -> new Item(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("sai"))));
    public static final RegistrySupplier<Item> SCORPBOOTSCAVE = ITEMS.register("scorpbootscave",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_CAVE, ArmorType.BOOTS).setId(itemKey("scorpbootscave"))));
    public static final RegistrySupplier<Item> SCORPBOOTSDIRT = ITEMS.register("scorpbootsdirt",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_DIRT, ArmorType.BOOTS).setId(itemKey("scorpbootsdirt"))));
    public static final RegistrySupplier<Item> SCORPBOOTSFROST = ITEMS.register("scorpbootsfrost",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_FROST, ArmorType.BOOTS).setId(itemKey("scorpbootsfrost"))));
    public static final RegistrySupplier<Item> SCORPBOOTSNETHER = ITEMS.register("scorpbootsnether",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_NETHER, ArmorType.BOOTS).setId(itemKey("scorpbootsnether"))));
    public static final RegistrySupplier<Item> SCORPHELMETCAVE = ITEMS.register("scorphelmetcave",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_CAVE, ArmorType.HELMET).setId(itemKey("scorphelmetcave"))));
    public static final RegistrySupplier<Item> SCORPHELMETDIRT = ITEMS.register("scorphelmetdirt",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_DIRT, ArmorType.HELMET).setId(itemKey("scorphelmetdirt"))));
    public static final RegistrySupplier<Item> SCORPHELMETFROST = ITEMS.register("scorphelmetfrost",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_FROST, ArmorType.HELMET).setId(itemKey("scorphelmetfrost"))));
    public static final RegistrySupplier<Item> SCORPHELMETNETHER = ITEMS.register("scorphelmetnether",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_NETHER, ArmorType.HELMET).setId(itemKey("scorphelmetnether"))));
    public static final RegistrySupplier<Item> SCORPLEGSCAVE = ITEMS.register("scorplegscave",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_CAVE, ArmorType.LEGGINGS).setId(itemKey("scorplegscave"))));
    public static final RegistrySupplier<Item> SCORPLEGSDIRT = ITEMS.register("scorplegsdirt",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_DIRT, ArmorType.LEGGINGS).setId(itemKey("scorplegsdirt"))));
    public static final RegistrySupplier<Item> SCORPLEGSFROST = ITEMS.register("scorplegsfrost",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_FROST, ArmorType.LEGGINGS).setId(itemKey("scorplegsfrost"))));
    public static final RegistrySupplier<Item> SCORPLEGSNETHER = ITEMS.register("scorplegsnether",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_NETHER, ArmorType.LEGGINGS).setId(itemKey("scorplegsnether"))));
    public static final RegistrySupplier<Item> SCORPPLATECAVE = ITEMS.register("scorpplatecave",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_CAVE, ArmorType.CHESTPLATE).setId(itemKey("scorpplatecave"))));
    public static final RegistrySupplier<Item> SCORPPLATEDIRT = ITEMS.register("scorpplatedirt",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_DIRT, ArmorType.CHESTPLATE).setId(itemKey("scorpplatedirt"))));
    public static final RegistrySupplier<Item> SCORPPLATEFROST = ITEMS.register("scorpplatefrost",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_FROST, ArmorType.CHESTPLATE).setId(itemKey("scorpplatefrost"))));
    public static final RegistrySupplier<Item> SCORPPLATENETHER = ITEMS.register("scorpplatenether",
            () -> new Item(new Item.Properties().humanoidArmor(MoCArmorMaterials.SCORPION_NETHER, ArmorType.CHESTPLATE).setId(itemKey("scorpplatenether"))));
    // Scorpion stings/swords apply their elemental on-hit effect (legacy MoCItemWeapon): dirt=poison,
    // frost=slowdown, nether=fire, cave=confusion. Legacy stings were GOLD-tier + fragile:
    // new MoCItemWeapon(EnumToolMaterial.GOLD, type, true) => maxDamage = GOLD.getMaxUses() = 32, and the
    // fragile flag made hitEntity wear 10 durability per hit (32/10 => shatters on the 4th hit, surviving ~3).
    // Faithful port: GOLD.sword already sets durability 32; overriding the WEAPON component to Weapon(10) makes
    // ItemStack.postHurtEnemy wear 10 per successful hit. (The four scorpion SWORDS below are IRON, non-fragile: 1/hit.)
    public static final RegistrySupplier<Item> SCORPSTINGCAVE = ITEMS.register("scorpstingcave",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.GOLD, 3.0F, -2.4F).component(DataComponents.WEAPON, new Weapon(10)).setId(itemKey("scorpstingcave")), drzhark.mocreatures.item.MoCWeaponItem.Effect.CONFUSION));
    public static final RegistrySupplier<Item> SCORPSTINGDIRT = ITEMS.register("scorpstingdirt",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.GOLD, 3.0F, -2.4F).component(DataComponents.WEAPON, new Weapon(10)).setId(itemKey("scorpstingdirt")), drzhark.mocreatures.item.MoCWeaponItem.Effect.POISON));
    public static final RegistrySupplier<Item> SCORPSTINGFROST = ITEMS.register("scorpstingfrost",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.GOLD, 3.0F, -2.4F).component(DataComponents.WEAPON, new Weapon(10)).setId(itemKey("scorpstingfrost")), drzhark.mocreatures.item.MoCWeaponItem.Effect.SLOWNESS));
    public static final RegistrySupplier<Item> SCORPSTINGNETHER = ITEMS.register("scorpstingnether",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.GOLD, 3.0F, -2.4F).component(DataComponents.WEAPON, new Weapon(10)).setId(itemKey("scorpstingnether")), drzhark.mocreatures.item.MoCWeaponItem.Effect.FIRE));
    public static final RegistrySupplier<Item> SCORPSWORDCAVE = ITEMS.register("scorpswordcave",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("scorpswordcave")), drzhark.mocreatures.item.MoCWeaponItem.Effect.CONFUSION));
    public static final RegistrySupplier<Item> SCORPSWORDDIRT = ITEMS.register("scorpsworddirt",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("scorpsworddirt")), drzhark.mocreatures.item.MoCWeaponItem.Effect.POISON));
    public static final RegistrySupplier<Item> SCORPSWORDFROST = ITEMS.register("scorpswordfrost",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("scorpswordfrost")), drzhark.mocreatures.item.MoCWeaponItem.Effect.SLOWNESS));
    public static final RegistrySupplier<Item> SCORPSWORDNETHER = ITEMS.register("scorpswordnether",
            () -> new drzhark.mocreatures.item.MoCWeaponItem(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("scorpswordnether")), drzhark.mocreatures.item.MoCWeaponItem.Effect.FIRE));
    public static final RegistrySupplier<Item> SCROLLOFFREEDOM = ITEMS.register("scrolloffreedom",
            () -> new drzhark.mocreatures.item.MoCScrollItem(drzhark.mocreatures.item.MoCScrollItem.Mode.FREEDOM, new Item.Properties().setId(itemKey("scrolloffreedom"))));
    public static final RegistrySupplier<Item> SCROLLOFOWNER = ITEMS.register("scrollofowner",
            () -> new drzhark.mocreatures.item.MoCScrollItem(drzhark.mocreatures.item.MoCScrollItem.Mode.OWNER, new Item.Properties().setId(itemKey("scrollofowner"))));
    public static final RegistrySupplier<Item> SCROLLOFSALE = ITEMS.register("scrollofsale",
            () -> new drzhark.mocreatures.item.MoCScrollItem(drzhark.mocreatures.item.MoCScrollItem.Mode.SALE, new Item.Properties().setId(itemKey("scrollofsale"))));
    public static final RegistrySupplier<Item> SHARKSWORD = ITEMS.register("sharksword",
            () -> new Item(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("sharksword"))));
    public static final RegistrySupplier<Item> SHARKTEETH = ITEMS.register("sharkteeth",
            () -> new Item(new Item.Properties().setId(itemKey("sharkteeth"))));
    public static final RegistrySupplier<Item> SILVERSWORD = ITEMS.register("silversword",
            () -> new Item(new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).setId(itemKey("silversword"))));
    public static final RegistrySupplier<Item> STAFF = ITEMS.register("staff",
            () -> new Item(new Item.Properties().setId(itemKey("staff"))));
    public static final RegistrySupplier<Item> STAFF2 = ITEMS.register("staff2",
            () -> new Item(new Item.Properties().setId(itemKey("staff2"))));
    public static final RegistrySupplier<Item> STAFF3 = ITEMS.register("staff3",
            () -> new Item(new Item.Properties().setId(itemKey("staff3"))));
    // Staffs are durability items (legacy setMaxDamage): each teleport/portal jump wears the staff by 1
    // (see MoCStaffPortalItem / MoCStaffTeleportItem). Legacy: Staff of Portal breaks after 3 jumps
    // (setMaxDamage(3)), Staff of Teleport after 128 blinks (setMaxDamage(128)).
    public static final RegistrySupplier<drzhark.mocreatures.item.MoCStaffPortalItem> STAFFPORTAL = ITEMS.register("staffportal",
            () -> new drzhark.mocreatures.item.MoCStaffPortalItem(new Item.Properties().durability(3).setId(itemKey("staffportal"))));
    public static final RegistrySupplier<Item> STAFFTELEPORT = ITEMS.register("staffteleport",
            () -> new drzhark.mocreatures.item.MoCStaffTeleportItem(new Item.Properties().durability(128).setId(itemKey("staffteleport"))));
    public static final RegistrySupplier<Item> SUGARLUMP = ITEMS.register("sugarlump",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()).setId(itemKey("sugarlump"))));
    public static final RegistrySupplier<Item> TURKEYCOOKED = ITEMS.register("turkeycooked",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()).setId(itemKey("turkeycooked"))));
    public static final RegistrySupplier<Item> TURKEYRAW = ITEMS.register("turkeyraw",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0), 0.8F)).build()).setId(itemKey("turkeyraw"))));
    public static final RegistrySupplier<Item> TURTLERAW = ITEMS.register("turtleraw",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()).setId(itemKey("turtleraw"))));
    public static final RegistrySupplier<Item> TURTLESOUP = ITEMS.register("turtlesoup",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build())
                    .usingConvertsTo(Items.BOWL).stacksTo(1).setId(itemKey("turtlesoup"))));
    // Tusk-armour items are durability items: wear accumulates as an elephant bulldozes and is stored on
    // the item's damage value, so a partly-worn tusk set can be removed and re-fitted (legacy tuskUses:
    // wood 59 / iron 250 / diamond 1000 uses before it shatters).
    public static final RegistrySupplier<Item> TUSKSDIAMOND = ITEMS.register("tusksdiamond",
            () -> new Item(new Item.Properties().setId(itemKey("tusksdiamond")).durability(1000)));
    public static final RegistrySupplier<Item> TUSKSIRON = ITEMS.register("tusksiron",
            () -> new Item(new Item.Properties().setId(itemKey("tusksiron")).durability(250)));
    public static final RegistrySupplier<Item> TUSKSWOOD = ITEMS.register("tuskswood",
            () -> new Item(new Item.Properties().setId(itemKey("tuskswood")).durability(59)));
    public static final RegistrySupplier<Item> UNICORNHORN = ITEMS.register("unicornhorn",
            () -> new Item(new Item.Properties().setId(itemKey("unicornhorn"))));
    // The whip is a durability item (legacy setMaxDamage(24)): each successful crack wears it by 1
    // (see MoCWhipItem). 24 cracks before it breaks.
    public static final RegistrySupplier<Item> WHIP = ITEMS.register("whip",
            () -> new drzhark.mocreatures.item.MoCWhipItem(new Item.Properties().durability(24).setId(itemKey("whip"))));
    public static final RegistrySupplier<Item> WOOLBALL = ITEMS.register("woolball",
            () -> new drzhark.mocreatures.item.MoCWoolBallItem(new Item.Properties().setId(itemKey("woolball"))));

    // ---- block items ----
    public static final RegistrySupplier<BlockItem> DIRT_WYVERN_LAIR_ITEM = ITEMS.register("dirt_wyvern_lair",
            () -> new BlockItem(MoCBlocks.DIRT_WYVERN_LAIR.get(), new Item.Properties().setId(itemKey("dirt_wyvern_lair"))));
    public static final RegistrySupplier<BlockItem> GRASS_WYVERN_LAIR_ITEM = ITEMS.register("grass_wyvern_lair",
            () -> new BlockItem(MoCBlocks.GRASS_WYVERN_LAIR.get(), new Item.Properties().setId(itemKey("grass_wyvern_lair"))));
    public static final RegistrySupplier<BlockItem> STONE_WYVERN_LAIR_ITEM = ITEMS.register("stone_wyvern_lair",
            () -> new BlockItem(MoCBlocks.STONE_WYVERN_LAIR.get(), new Item.Properties().setId(itemKey("stone_wyvern_lair"))));
    public static final RegistrySupplier<BlockItem> LOG_WYVERN_LAIR_ITEM = ITEMS.register("log_wyvern_lair",
            () -> new BlockItem(MoCBlocks.LOG_WYVERN_LAIR.get(), new Item.Properties().setId(itemKey("log_wyvern_lair"))));
    public static final RegistrySupplier<BlockItem> LEAVES_WYVERN_LAIR_ITEM = ITEMS.register("leaves_wyvern_lair",
            () -> new BlockItem(MoCBlocks.LEAVES_WYVERN_LAIR.get(), new Item.Properties().setId(itemKey("leaves_wyvern_lair"))));
    public static final RegistrySupplier<BlockItem> PLANKS_WYVERN_LAIR_ITEM = ITEMS.register("planks_wyvern_lair",
            () -> new BlockItem(MoCBlocks.PLANKS_WYVERN_LAIR.get(), new Item.Properties().setId(itemKey("planks_wyvern_lair"))));
    public static final RegistrySupplier<BlockItem> TALL_GRASS_WYVERN_LAIR_ITEM = ITEMS.register("tall_grass_wyvern_lair",
            () -> new BlockItem(MoCBlocks.TALL_GRASS_WYVERN_LAIR.get(), new Item.Properties().setId(itemKey("tall_grass_wyvern_lair"))));
    public static final RegistrySupplier<BlockItem> DIRT_OGRE_LAIR_ITEM = ITEMS.register("dirt_ogre_lair",
            () -> new BlockItem(MoCBlocks.DIRT_OGRE_LAIR.get(), new Item.Properties().setId(itemKey("dirt_ogre_lair"))));
    public static final RegistrySupplier<BlockItem> GRASS_OGRE_LAIR_ITEM = ITEMS.register("grass_ogre_lair",
            () -> new BlockItem(MoCBlocks.GRASS_OGRE_LAIR.get(), new Item.Properties().setId(itemKey("grass_ogre_lair"))));
    public static final RegistrySupplier<BlockItem> STONE_OGRE_LAIR_ITEM = ITEMS.register("stone_ogre_lair",
            () -> new BlockItem(MoCBlocks.STONE_OGRE_LAIR.get(), new Item.Properties().setId(itemKey("stone_ogre_lair"))));
    public static final RegistrySupplier<BlockItem> LOG_OGRE_LAIR_ITEM = ITEMS.register("log_ogre_lair",
            () -> new BlockItem(MoCBlocks.LOG_OGRE_LAIR.get(), new Item.Properties().setId(itemKey("log_ogre_lair"))));
    public static final RegistrySupplier<BlockItem> LEAVES_OGRE_LAIR_ITEM = ITEMS.register("leaves_ogre_lair",
            () -> new BlockItem(MoCBlocks.LEAVES_OGRE_LAIR.get(), new Item.Properties().setId(itemKey("leaves_ogre_lair"))));
    public static final RegistrySupplier<BlockItem> PLANKS_OGRE_LAIR_ITEM = ITEMS.register("planks_ogre_lair",
            () -> new BlockItem(MoCBlocks.PLANKS_OGRE_LAIR.get(), new Item.Properties().setId(itemKey("planks_ogre_lair"))));
    public static final RegistrySupplier<BlockItem> TALL_GRASS_OGRE_LAIR_ITEM = ITEMS.register("tall_grass_ogre_lair",
            () -> new BlockItem(MoCBlocks.TALL_GRASS_OGRE_LAIR.get(), new Item.Properties().setId(itemKey("tall_grass_ogre_lair"))));
}
