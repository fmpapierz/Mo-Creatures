package drzhark.mocreatures.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Central, data-driven behaviour table for every Mo'Creatures creature, ported faithfully from the
 * legacy 1.12.2 source (foods, taming mechanism, heal foods, breeding, rideability, milking, baby
 * scaling and death drops). The four base entity classes look their entry up by registry id, so the
 * per-creature classes stay focused on models/attributes/AI while the shared interaction rules live
 * here. Items are held as suppliers because Mo'Creatures items are resolved lazily after registration.
 */
public final class MoCBehavior {

    /** How a creature is tamed, faithful to the legacy mechanic. */
    public enum Tame {
        /** Not tameable. */            NONE,
        /** Right-click feed its food. */ FEED,
        /** Right-click to pick up / carry (tames an untamed one). */ PICKUP,
        /** Right-click to pick up / carry ONLY when already tamed — never tames a wild one (legacy snake:
         *  MoCEntitySnake.interact returns immediately for a wild snake, so only an egg-hatched tamed snake
         *  can be carried). Handled in MoCAnimal.mobInteract's pick-up branch. */ PICKUP_TAMED,
        /** Give it a Medallion. */      MEDALLION
    }

    public record Drop(Supplier<Item> item, int min, int max, float chance) {}

    public static final class Spec {
        public Tame tame = Tame.NONE;
        public List<Supplier<Item>> foods = List.of();
        public List<Supplier<Item>> healFoods = null; // null -> same as foods
        public boolean canBreed = false;
        public boolean rideable = false;
        public boolean rideNeedsSaddle = false;
        public boolean milkable = false;
        public boolean babyScales = false;
        /** Wild (untamed, adult) members hunt players and retaliate — Mo'Creatures' dangerous fauna. */
        public boolean wildHostile = false;
        /** Drop this creature's single item the faithful vanilla way: {@code rand.nextInt(3)} copies (0-2,
         *  including 0), reproducing legacy EntityLiving.dropFewItems for the plain single-item mobs whose
         *  legacy class does NOT override dropFewItems (rat / wild_wolf / wraith / flame_wraith / turtle).
         *  When set, the generic loop ignores min/max/chance and drops 0-2. Only use for single-drop specs. */
        public boolean vanillaDrop = false;
        /**
         * This creature can be broken in: legacy let you saddle and mount it while still WILD, and it then
         * bucked and threw the rider until a temper roll made it submit ({@code MoCEntityAnimal:1096-1142}).
         * Only the horse, the wyvern and the dolphin worked this way — every other rideable creature had to be
         * tamed before it could be mounted at all, so this stays off by default.
         */
        public boolean rideTames = false;
        public List<Drop> drops = new ArrayList<>();

        Spec tame(Tame t) { this.tame = t; return this; }
        Spec food(Supplier<Item>... f) { this.foods = List.of(f); return this; }
        Spec heal(Supplier<Item>... f) { this.healFoods = List.of(f); return this; }
        Spec breed() { this.canBreed = true; return this; }
        Spec ride(boolean needsSaddle) { this.rideable = true; this.rideNeedsSaddle = needsSaddle; return this; }
        Spec rideTames() { this.rideTames = true; return this; }
        Spec milk() { this.milkable = true; return this; }
        Spec baby() { this.babyScales = true; return this; }
        Spec hostile() { this.wildHostile = true; return this; }
        Spec vanilla() { this.vanillaDrop = true; return this; }
        Spec drop(Supplier<Item> item, int min, int max, float chance) {
            this.drops.add(new Drop(item, min, max, chance)); return this;
        }
        public List<Supplier<Item>> healOrFood() { return healFoods != null ? healFoods : foods; }
    }

    private static final Spec DEFAULT = new Spec();
    private static final Map<String, Spec> M = new HashMap<>();

    public static Spec get(String id) { return M.getOrDefault(id, DEFAULT); }

    /** Look up the behaviour spec for a live entity by its registry id. */
    public static Spec of(net.minecraft.world.entity.Entity e) {
        return get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
    }

    public static boolean matches(List<Supplier<Item>> list, net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Supplier<Item> s : list) {
            if (stack.is(s.get())) return true;
        }
        return false;
    }

    /** Spawn this creature's faithful death drops. */
    /**
     * Builds a {@code mocegg} carrying a legacy composite egg id in CUSTOM_DATA, so it displays with its real
     * name and hatches the right species. Without this the drop is a blank "Spoiled Egg".
     */
    private static net.minecraft.world.item.ItemStack eggStack(int eggType) {
        return drzhark.mocreatures.item.MoCThrownEggItem.createEgg(eggType);
    }

    public static void dropLoot(net.minecraft.world.entity.Mob e, net.minecraft.server.level.ServerLevel level, Spec spec) {
        // Server-admin loot suppression (legacy destroyDrops / destroyPassiveDrops): drop nothing when the
        // matching flag is set. destroyPassiveDrops targets only passive creatures (MoCAnimal).
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        if (cfg.destroyDrops
                || (cfg.destroyPassiveDrops && e instanceof drzhark.mocreatures.entity.MoCAnimal)) {
            return;
        }

        // ---------------------------------------------------------------------------------------------
        // Type/name/age-keyed death drops, ported from the per-species legacy getDropItemId() overrides.
        // The reg() drop list can only express type-agnostic loot, so the variant creatures resolve their
        // single legacy drop here (branching on the live entity) and return before the generic loop below.
        // Creatures without a special case fall straight through to that loop.
        // ---------------------------------------------------------------------------------------------
        net.minecraft.util.RandomSource rand = e.getRandom();

        // Skeleton / undead / bat / nightmare horse mobs (legacy MoCEntityHorseMob.getDropItemId; the single
        // shared flag is rand.nextInt(5)==0, i.e. a 0.2 chance for the "special" drop).
        if (e instanceof drzhark.mocreatures.entity.monster.MoCEntityHorseMob hm) {
            boolean flag = rand.nextInt(5) == 0;
            int t = hm.getTypeMoC();
            net.minecraft.world.item.Item drop;
            if (t == 26) {                                   // skeleton horse -> bone only
                drop = Items.BONE;
            } else if (t == 23 || t == 24 || t == 25) {      // undead horses -> heartundead @0.2 else rotten flesh
                drop = flag ? MoCItems.HEARTUNDEAD.get() : Items.ROTTEN_FLESH;
            } else if (t == 32) {                            // bat horse -> heartdarkness @0.2 else leather
                drop = flag ? MoCItems.HEARTDARKNESS.get() : Items.LEATHER;
            } else if (t == 38) {                            // nightmare -> heartfire only in the Nether, else leather
                drop = (flag && level.dimension() == net.minecraft.world.level.Level.NETHER)
                        ? MoCItems.HEARTFIRE.get() : Items.LEATHER;
            } else {
                drop = Items.LEATHER;
            }
            // Legacy MoCEntityHorseMob does NOT override dropFewItems, so vanilla drops 0-2 copies of the id.
            int n = rand.nextInt(3);
            for (int i = 0; i < n; i++) {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(drop, 1));
            }
            return;
        }

        // Horses (legacy getDropItemId): the DEFAULT drop for almost every coat is 0-2 leather — including
        // zebra(60)/zorse(61)/donkey(65)/mule(66)/zonky(67)/bug-horse(30) and a unicorn(36)/fairy(50-59)/
        // nightmare(38)/bat-horse(32) whose 1-in-4 signature roll (in MoCEntityHorse.dropCustomDeathLoot)
        // FAILED. Only the coats whose legacy getDropItemId NEVER falls through to leather leave a signature
        // item alone: ghost(21/22 -> ghast tear), undead(23/24/25 -> heartundead/rotten flesh),
        // skeleton(26 -> bone) and pegasus(39/40 -> feather). The horse drops those signatures itself in
        // dropCustomDeathLoot, so suppress leather ONLY for that whitelist; resolve leather here and return so
        // the generic spec loop (whose "horse" spec still lists leather for readability) never double-drops it.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse horse) {
            int t = horse.getTypeMoC();
            // Suppress the generic 0-2 leather for every NON-ordinary coat whose leather is instead emitted by
            // MoCEntityHorse.dropCustomDeathLoot (undead 21-26, bat 32, unicorn 36, nightmare 38, pegasi 39/40,
            // fairies 50-59) so those coats never double-drop leather. Ordinary coats stay unsuppressed.
            boolean suppressLeather = (t == 21 || t == 22 || t == 23 || t == 24 || t == 25
                    || t == 26 || t == 32 || t == 36 || t == 38 || t == 39 || t == 40
                    || (t >= 50 && t < 60));
            if (!suppressLeather) {
                int n = rand.nextInt(3); // 0-2, matching legacy vanilla dropFewItems of the leather default
                for (int i = 0; i < n; i++) {
                    e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(Items.LEATHER, 1));
                }
            }
            return;
        }

        // Ogres (legacy MoCEntityOgre.getDropItemId fed through vanilla dropFewItems: 0-2 copies of one item).
        // Green ogres (type<3) drop obsidian, fire ogres (3-4) drop heartfire at 1/4 chance (else the fire
        // block, which yields no item), and cave ogres (>4) drop diamond.
        if (e instanceof drzhark.mocreatures.entity.monster.MoCEntityOgre ogre) {
            int t = ogre.getTypeMoC();
            int n = rand.nextInt(3);                         // vanilla dropFewItems copy count: 0-2
            net.minecraft.world.item.Item drop = null;
            if (t < 3) {
                drop = Items.OBSIDIAN;
            } else if (t < 5) {
                if (rand.nextInt(4) == 0) drop = MoCItems.HEARTFIRE.get();
            } else {
                drop = Items.DIAMOND;
            }
            if (drop != null && n > 0) {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(drop, n));
            }
            return;
        }

        // Hell rats (legacy MoCEntityHellRat.getDropItemId): the id is the FIRE block 1-in-3 kills — which
        // yields no usable item — and redstone the other 2-in-3. MoCEntityHellRat does NOT override
        // dropFewItems, so vanilla drops 0-2 copies of whatever id was chosen (0-2 fire = nothing, else 0-2
        // redstone), letting ~1/3 of kills leave nothing.
        if (e instanceof drzhark.mocreatures.entity.monster.MoCEntityHellRat) {
            if (rand.nextInt(3) != 0) {                       // not the fire-block (no-item) branch
                int n = rand.nextInt(3);                     // vanilla dropFewItems copy count: 0-2
                for (int i = 0; i < n; i++) {
                    e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(Items.REDSTONE, 1));
                }
            }
            return;
        }

        // Ostriches, including the transformed fire/black/undead/unicorn variants (legacy
        // MoCEntityOstrich.getDropItemId; special chance rand.nextInt(3)==0). Exactly one item drops per kill:
        // the essence heart / unicorn horn OR ostrich meat (never both), and the undead type never drops meat.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityOstrich ostrich) {
            boolean flag = rand.nextInt(3) == 0;
            int t = ostrich.getTypeMoC();
            net.minecraft.world.item.Item drop;
            if (flag && t == 8) {                            // unicorn ostrich -> unicorn horn
                drop = MoCItems.UNICORNHORN.get();
            } else if (t == 5 && flag) {                     // fire ostrich -> heartfire
                drop = MoCItems.HEARTFIRE.get();
            } else if (t == 6 && flag) {                     // black-wyvern ostrich -> heartdarkness
                drop = MoCItems.HEARTDARKNESS.get();
            } else if (t == 7) {                             // undead ostrich -> heartundead @1/3 else rotten flesh
                drop = flag ? MoCItems.HEARTUNDEAD.get() : Items.ROTTEN_FLESH;
            } else {
                drop = MoCItems.OSTRICHRAW.get();
            }
            // Legacy MoCEntityOstrich does NOT override dropFewItems, so vanilla drops 0-2 copies of the id.
            int n = rand.nextInt(3);
            for (int i = 0; i < n; i++) {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(drop, 1));
            }
            return;
        }

        // Pet scorpions (legacy MoCEntityPetScorpion.getDropItemId; sting/chitin coin-flip is rand.nextInt(2)==0):
        // babies drop silk, undead (type 5) drop rotten flesh, and each adult drops its type's sting or chitin 50/50.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityPetScorpion scorp) {
            net.minecraft.world.item.Item drop;
            if (!scorp.getIsAdult()) {
                drop = Items.STRING;                         // silk
            } else {
                boolean flag = rand.nextInt(2) == 0;
                drop = switch (scorp.getTypeMoC()) {
                    case 1 -> flag ? MoCItems.SCORPSTINGDIRT.get() : MoCItems.CHITIN.get();
                    case 2 -> flag ? MoCItems.SCORPSTINGCAVE.get() : MoCItems.CHITINBLACK.get();
                    case 3 -> flag ? MoCItems.SCORPSTINGNETHER.get() : MoCItems.CHITINNETHER.get();
                    case 4 -> flag ? MoCItems.SCORPSTINGFROST.get() : MoCItems.CHITINFROST.get();
                    case 5 -> Items.ROTTEN_FLESH;            // undead
                    default -> Items.STRING;
                };
            }
            // Legacy MoCEntityPetScorpion does NOT override dropFewItems, so vanilla drops 0-2 copies.
            int n = rand.nextInt(3);
            for (int i = 0; i < n; i++) {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(drop, 1));
            }
            return;
        }

        // Ninja-Turtle-named turtles drop their signature weapon instead of turtle meat (legacy
        // MoCEntityTurtle.getDropItemId); any other turtle falls through to the generic turtle-meat drop.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityTurtle
                && e.hasCustomName() && e.getCustomName() != null) {
            String name = e.getCustomName().getString().toLowerCase(java.util.Locale.ROOT).trim();
            net.minecraft.world.item.Item weapon = switch (name) {
                case "donatello" -> MoCItems.BO.get();
                case "leonardo" -> MoCItems.KATANA.get();
                case "rafael", "raphael" -> MoCItems.SAI.get();
                case "michelangelo", "michaelangelo" -> MoCItems.NUNCHAKU.get();
                default -> null;
            };
            if (weapon != null) {
                // Legacy MoCEntityTurtle does NOT override dropFewItems, so vanilla drops 0-2 copies.
                int n = rand.nextInt(3);
                for (int i = 0; i < n; i++) {
                    e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(weapon, 1));
                }
                return;                                      // suppress the normal turtle-meat drop
            }
        }

        // Sharks (legacy MoCEntityShark.dropFewItems): 90% drop 1-3 shark teeth (and nothing else); otherwise
        // — ONLY on non-peaceful difficulty AND an old shark (edad > 150) — drop rand.nextInt(3) = 0-2 eggs.
        // Teeth and egg are mutually exclusive, teeth only 90% of the time, and the egg branch can drop nothing.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityShark shark) {
            int i = rand.nextInt(100);
            if (i < 90) {
                int j = rand.nextInt(3) + 1;                 // 1-3 shark teeth
                for (int l = 0; l < j; l++) {
                    e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(MoCItems.SHARKTEETH.get(), 1));
                }
            } else if (level.getDifficulty().getId() > 0 && shark.getMoCAge() > 150) {
                int k = rand.nextInt(3);                     // 0-2 eggs
                for (int i1 = 0; i1 < k; i1++) {
                    e.spawnAtLocation(level, eggStack(11)); // legacy shark-egg meta
                }
            }
            return;
        }

        // Fishy (legacy MoCEntityFishy.dropFewItems): 70% drop exactly 1 raw fish; otherwise drop
        // rand.nextInt(2) = 0-or-1 fishy egg. Mutually exclusive (never both), ~15% of kills drop nothing.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityFishy fishy) {
            int i = rand.nextInt(100);
            if (i < 70) {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(Items.COD, 1));
            } else {
                int j = rand.nextInt(2);
                for (int k = 0; k < j; k++) {
                    e.spawnAtLocation(level, eggStack(Math.max(1, Math.min(10, fishy.getTypeMoC()))));
                }
            }
            return;
        }

        // Komodo (legacy MoCEntityKomodo.dropFewItems:216-232): an adult (edad > 90) on a 1-in-5 roll drops
        // 1-2 Komodo Dragon Eggs (meta 33); on every other kill it drops exactly one reptile hide instead.
        // The two are mutually exclusive — a komodo never drops both.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityKomodo komodo) {
            if (komodo.getMoCAge() > 90 && rand.nextInt(5) == 0) {
                int n = rand.nextInt(2) + 1;                 // 1-2 eggs
                for (int i = 0; i < n; i++) {
                    e.spawnAtLocation(level, eggStack(33));
                }
            } else {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(MoCItems.REPTILEHIDE.get(), 1));
            }
            return;
        }

        // Turkey (legacy MoCEntityTurkey.getDropItemId picks ONE item per kill via a coin-flip, then vanilla
        // dropFewItems drops 0-2 copies of that single item): rand.nextInt(2)==0 ? raw turkey : feather, 0-2.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityTurkey) {
            net.minecraft.world.item.Item drop = rand.nextInt(2) == 0 ? MoCItems.TURKEYRAW.get() : Items.FEATHER;
            int n = rand.nextInt(3);
            for (int i = 0; i < n; i++) {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(drop, 1));
            }
            return;
        }

        // Boar (legacy MoCEntityBoar.getDropItemId picks ONE item per kill, then vanilla dropFewItems 0-2
        // copies): rand.nextInt(2)==0 ? raw porkchop : animal hide, then 0-2 copies of that single item.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityBoar) {
            net.minecraft.world.item.Item drop = rand.nextInt(2) == 0 ? Items.PORKCHOP : MoCItems.HIDE.get();
            int n = rand.nextInt(3);
            for (int i = 0; i < n; i++) {
                e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(drop, 1));
            }
            return;
        }

        // A tamed kitty always returns its Medallion on death, so the player never permanently loses the item
        // they spent to tame it (legacy MoCEntityKitty.onDeath). Additive: kitties have no other death drop.
        if (e instanceof drzhark.mocreatures.entity.passive.MoCEntityKitty kitty && kitty.getIsTamed()) {
            e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(MoCItems.MEDALLION.get(), 1));
        }

        for (Drop d : spec.drops) {
            // Faithful vanilla dropFewItems (rat / wild_wolf / wraith / flame_wraith / turtle): the legacy
            // class does NOT override dropFewItems, so it drops rand.nextInt(3) = 0-2 copies of its single id,
            // including ZERO (~1/3 of kills). No chance gate and no min-1 floor. Scoped to vanillaDrop specs
            // so the chance/min/max behaviour of every other creature is left exactly as-is.
            if (spec.vanillaDrop) {
                int n = rand.nextInt(3);
                for (int i = 0; i < n; i++) {
                    e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(d.item().get(), 1));
                }
                continue;
            }
            if (e.getRandom().nextFloat() > d.chance()) continue; // chance() gates whether it drops at all
            int n = d.min();
            if (d.max() > d.min()) n += e.getRandom().nextInt(d.max() - d.min() + 1);
            if (n <= 0) n = 1; // once the chance passes, always drop at least one
            e.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(d.item().get(), n));
        }
    }

    private static Spec reg(String id) { Spec s = new Spec(); M.put(id, s); return s; }

    // ---- vanilla item suppliers (eager constants are fine) ----
    private static Supplier<Item> v(Item it) { return () -> it; }
    // ---- mod item suppliers (lazy: resolved after registration) ----

    static {
        // =========================================================== PASSIVE LAND (MoCAnimal)
        reg("bunny").tame(Tame.PICKUP).heal(v(Items.CARROT)).baby();
        // Legacy interact() only tames the PANDA (type 3), by sugar-lump or reed; brown/black/polar bears
        // stay wild. That panda-only taming lives in MoCEntityBear.mobInteract (which returns before falling
        // through), so the generic FEED taming is disabled here to stop it taming any fed bear. Legacy
        // isMyHealFood is panda + reed (sugar cane), so that is the only heal food.
        reg("bear").tame(Tame.NONE).heal(v(Items.SUGAR_CANE)).baby().hostile()
                .vanilla().drop(() -> MoCItems.HIDE.get(), 0, 2, 1.0F);
        reg("big_cat").tame(Tame.MEDALLION).heal(v(Items.PORKCHOP), v(Items.COD)).ride(true).baby().hostile()
                .vanilla().drop(() -> MoCItems.BIGCATCLAW.get(), 0, 2, 1.0F);
        // Bird taming is a two-stage seed flow handled in MoCEntityBird (peck scattered seeds -> pre-tamed,
        // then hand-feed to tame), so it is NOT auto-tamed by the generic pickup/feed path.
        reg("bird").tame(Tame.NONE).food(v(Items.WHEAT_SEEDS)).heal(v(Items.WHEAT_SEEDS))
                .vanilla().drop(v(Items.FEATHER), 0, 2, 1.0F);
        // Boar death drop picks ONE item per kill (raw porkchop OR animal hide, 50/50) then drops 0-2 copies,
        // resolved per entity in dropLoot (boar branch), so no type-agnostic .drop() spec here.
        reg("boar").baby().hostile();
        reg("crocodile").baby().hostile().vanilla().drop(() -> MoCItems.REPTILEHIDE.get(), 0, 2, 1.0F);
        reg("deer").baby().drop(() -> MoCItems.FUR.get(), 1, 1, 1.0F);
        reg("duck").drop(v(Items.FEATHER), 1, 1, 1.0F);
        // Legacy elephants cannot be tamed by a single feed and never tame as adults: interact() only accepts
        // an UNTAMED, NON-ADULT elephant, accumulating temper (cake +2 / sugar-lump +1, full-heal each feed)
        // and taming only once temper>=10 — never setting adult. That temper flow lives in
        // MoCEntityElephant, so the generic FEED taming is disabled here (heal foods are unchanged).
        reg("elephant").tame(Tame.NONE)
                .heal(v(Items.BAKED_POTATO), v(Items.BREAD), () -> MoCItems.HAYSTACK.get()).ride(true).baby()
                .drop(() -> MoCItems.HIDE.get(), 1, 3, 1.0F);
        reg("fox").tame(Tame.FEED).food(() -> MoCItems.TURKEYRAW.get()).heal(() -> MoCItems.RATRAW.get()).baby()
                .drop(() -> MoCItems.FUR.get(), 1, 1, 1.0F);
        // Legacy goats cannot breed (createChild() returns null, isMyAphrodisiac() is false) — no BreedGoal.
        // Their tame/heal food set is broader than this list (any edible; see MoCEntityGoat's food override).
        reg("goat").tame(Tame.FEED).food(v(Items.WHEAT), v(Items.WHEAT_SEEDS), v(Items.SUGAR), v(Items.CAKE), v(Items.EGG))
                .milk().baby().vanilla().drop(v(Items.LEATHER), 0, 2, 1.0F);
        // Legacy heal foods are the MOD's sugar lump and haystack, not vanilla sugar and a vanilla hay bale.
        reg("horse").tame(Tame.FEED).food(v(Items.APPLE), v(Items.GOLDEN_APPLE))
                .heal(v(Items.WHEAT), () -> MoCItems.SUGARLUMP.get(), v(Items.BREAD), v(Items.APPLE),
                        v(Items.GOLDEN_APPLE), () -> MoCItems.HAYSTACK.get())
                .breed().ride(true).rideTames().drop(v(Items.LEATHER), 0, 2, 1.0F);
        reg("kitty").tame(Tame.MEDALLION).heal(v(Items.COD), v(Items.COOKED_COD), v(Items.CAKE)).baby();
        // Legacy komodos are NOT hand-tameable: the only tamed komodo is one hatched from a Komodo Dragon Egg
        // (MoCEntityEgg). Raw rat / raw turkey stay as heal foods for an already-tamed one. The death drop is
        // resolved in dropLoot (egg or reptile hide, mutually exclusive), so no .drop() spec here.
        reg("komodo").tame(Tame.NONE).heal(() -> MoCItems.RATRAW.get(), () -> MoCItems.TURKEYRAW.get())
                .ride(true).baby().hostile();
        reg("mouse").tame(Tame.PICKUP).vanilla().drop(v(Items.WHEAT_SEEDS), 0, 2, 1.0F);
        // Death drop is type-keyed (meat, or a fire/darkness/undead heart or unicorn horn for transformed
        // types 5-8) and resolved per entity in dropLoot; see the ostrich branch there.
        // Legacy MoCEntityOstrich.isMyHealFood == isItemEdible: ANY vanilla food item, ANY seeds, plus
        // wheat/sugar/cake/egg, fully heals a tamed, hurt ostrich. A fixed supplier list can't express "any
        // ItemFood", so this heal list enumerates the common vanilla foods + the seeds + wheat/sugar/cake/egg
        // as a faithful stopgap. See CROSS-FILE need: a truly "any edible" heal wants MoCEntityOstrich to
        // override the heal-food test with a FOOD-component check, exactly like MoCEntityGoat.isGoatEdible.
        // Legacy ostriches are NOT hand-tameable either: the only tamed ostrich comes from a STOLEN ostrich egg
        // (composite id 31 — a wild laid egg, 30, becomes stolen when a player picks it up or places it). The
        // full feed list stays as heal food for an already-tamed bird.
        reg("ostrich").tame(Tame.NONE)
                .heal(v(Items.WHEAT), v(Items.WHEAT_SEEDS), v(Items.MELON_SEEDS), v(Items.PUMPKIN_SEEDS),
                        v(Items.BEETROOT_SEEDS), v(Items.SUGAR), v(Items.CAKE), v(Items.EGG),
                        v(Items.BREAD), v(Items.APPLE), v(Items.GOLDEN_APPLE), v(Items.CARROT), v(Items.GOLDEN_CARROT),
                        v(Items.POTATO), v(Items.BAKED_POTATO), v(Items.BEETROOT), v(Items.COOKIE), v(Items.MELON_SLICE),
                        v(Items.COOKED_BEEF), v(Items.COOKED_PORKCHOP), v(Items.COOKED_CHICKEN), v(Items.COOKED_MUTTON),
                        v(Items.COOKED_RABBIT), v(Items.COOKED_COD), v(Items.COOKED_SALMON))
                .ride(true).baby();
        // Death drop is type/age-keyed (silk for babies, per-type sting/chitin 50/50, rotten flesh for undead)
        // and resolved per entity in dropLoot; see the pet-scorpion branch there.
        reg("pet_scorpion").tame(Tame.PICKUP).heal(() -> MoCItems.RATRAW.get(), () -> MoCItems.RATCOOKED.get())
                .ride(true).baby();
        // Legacy wild snakes CANNOT be tamed or picked up: MoCEntitySnake.interact returns immediately for an
        // untamed snake, so a right-click is ignored entirely; only an egg-hatched TAMED snake can be carried.
        // PICKUP_TAMED carries a tamed snake without ever taming a wild one (see MoCAnimal.mobInteract).
        reg("snake").tame(Tame.PICKUP_TAMED).heal(() -> MoCItems.RATRAW.get()).baby().hostile();
        // Turkey death drop picks ONE item per kill (raw turkey OR feather, 50/50) then drops 0-2 copies,
        // resolved per entity in dropLoot (turkey branch), so no type-agnostic .drop() spec here.
        reg("turkey").tame(Tame.FEED).food(v(Items.MELON_SEEDS)).heal(v(Items.PUMPKIN_SEEDS));
        // Legacy turtles are NOT tamed by hand-feeding: a wild turtle is tamed only by walking to and eating a
        // melon/reed EntityItem dropped on the ground, and a right-click flips it upside-down (see MoCEntityTurtle).
        // So taming is NONE here; the TMNT-named signature-weapon drop is resolved in dropLoot. A TAMED turtle
        // IS hand-fed to full health with a melon slice or sugar cane (legacy isMyHealFood: reed | melon), so
        // register those heal foods for the tamed-heal branch. The plain turtle-meat drop is 0-2 (vanilla
        // dropFewItems: MoCEntityTurtle does not override it).
        reg("turtle").tame(Tame.NONE).heal(v(Items.MELON_SLICE), v(Items.SUGAR_CANE)).baby().vanilla()
                .drop(() -> MoCItems.TURTLERAW.get(), 0, 2, 1.0F);
        // Legacy wyverns have no feed-to-tame interaction — rat/turkey are heal food only, on an already-tamed
        // wyvern. Wild wyverns become tamed by being ridden/named (base tameWithName), not by feeding, so the
        // generic FEED taming is disabled here (the raw rat/turkey heal foods are kept).
        // Legacy wyverns have no feed-taming: you saddle and mount a WILD one and break it in (rideTames).
        reg("wyvern").tame(Tame.NONE)
                .ride(true).rideTames().heal(() -> MoCItems.RATRAW.get(), () -> MoCItems.TURKEYRAW.get()).baby();
        reg("crab").baby().drop(() -> MoCItems.CRABRAW.get(), 1, 1, 1.0F);
        reg("maggot").vanilla().drop(v(Items.SLIME_BALL), 0, 2, 1.0F);
        reg("snail").vanilla().drop(v(Items.SLIME_BALL), 0, 2, 1.0F);
        reg("cricket");
        reg("roach").food(v(Items.ROTTEN_FLESH)); // attraction only, NOT tameable

        // =========================================================== AQUATIC (MoCAquatic)
        reg("dolphin").tame(Tame.FEED).food(v(Items.APPLE), v(Items.GOLDEN_APPLE))
                .heal(v(Items.COD), v(Items.COOKED_COD)).breed().ride(false).rideTames().baby()
                .vanilla().drop(v(Items.COD), 0, 2, 1.0F);
        // Fishy death drop is a mutually-exclusive 70%-raw-fish-else-rand(2)-egg roll, resolved per entity in
        // dropLoot (fishy branch), so no type-agnostic .drop() spec here.
        reg("fishy").baby();
        reg("jellyfish").baby().drop(v(Items.SLIME_BALL), 0, 1, 0.5F);
        reg("ray").ride(false).baby();
        // Shark death drop is a mutually-exclusive 90%-teeth-else-difficulty/age-gated-egg roll, resolved per
        // entity in dropLoot (shark branch), so no type-agnostic .drop() spec here.
        reg("shark").baby().hostile();

        // =========================================================== FLYING INSECTS (MoCFlyingInsect)
        reg("bee").food(v(Items.POPPY), v(Items.DANDELION));      // attraction only
        reg("butterfly").food(v(Items.POPPY), v(Items.DANDELION)); // attraction only
        reg("dragonfly");
        reg("firefly");
        reg("fly").food(v(Items.ROTTEN_FLESH));                   // attraction only

        // =========================================================== MONSTERS (MoCMob) - hostile, not tameable
        reg("golem"); // drops its absorbed blocks in legacy; no plain item drop
        // Legacy getDropItemId() is strictly variant-keyed: green ogres (type<3) drop obsidian, fire ogres
        // (type 3-4) drop a heartfire at 1/4 chance (else the fire block, no item), and cave ogres (type>4)
        // drop a diamond — 0-2 copies via vanilla dropFewItems. Resolved per entity in dropLoot (ogre branch).
        reg("ogre");
        // rat / wild_wolf / wraith / flame_wraith: a single item via vanilla dropFewItems (0-2 copies, 0
        // possible) — legacy classes do not override dropFewItems. .vanilla() drives the 0-2 count in dropLoot.
        reg("rat").vanilla().drop(() -> MoCItems.RATRAW.get(), 0, 2, 1.0F);
        reg("scorpion").baby().drop(() -> MoCItems.CHITIN.get(), 0, 1, 0.5F)
                .drop(v(Items.STRING), 0, 1, 0.5F);
        reg("wild_wolf").vanilla().drop(() -> MoCItems.FUR.get(), 0, 2, 1.0F);
        reg("werewolf").drop(v(Items.GOLDEN_APPLE), 0, 1, 0.2F);
        reg("wraith").vanilla().drop(v(Items.GUNPOWDER), 0, 2, 1.0F);
        // Hell rats resolve their drop per entity in dropLoot (fire-block 1/3 -> nothing, else 0-2 redstone);
        // see the hell-rat branch there.
        reg("hell_rat");
        reg("flame_wraith").vanilla().drop(v(Items.REDSTONE), 0, 2, 1.0F);
        // Legacy getDropItemId() returns a single variant-keyed item: skeleton(26)->bone; undead(23/24/25)->
        // heartundead @0.2 else rotten flesh; bat(32)->heartdarkness @0.2 else leather; nightmare(38)->heartfire
        // only in the Nether else leather; otherwise leather. Resolved per entity in dropLoot (horse-mob branch).
        reg("horse_mob");
    }

    private MoCBehavior() {}
}
