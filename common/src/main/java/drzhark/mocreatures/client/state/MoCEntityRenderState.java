package drzhark.mocreatures.client.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Shared render state for Mo'Creatures entities. Carries the per-entity texture (selected from the
 * entity's {@code typeMoC}) so a single generic renderer can serve creatures with many colour variants.
 */
public class MoCEntityRenderState extends LivingEntityRenderState {
    public Identifier texture;
    public int typeMoC;
    public boolean baby;
    /** Whether a rideable creature is wearing a saddle. */
    public boolean saddled;
    /** Elephant equipment stage (0 bare, 1 harness, 2 garment, 3 howdah/platform) + chest. */
    public int armorStage;
    public boolean hasChest;
    /** Elephant tusk armour tier (0 none, 1 wood, 2 iron, 3 diamond) — selects the tusk model set. */
    public int tusks;
    /** Horse armour tier (0 none, 1 metal, 2 gold, 3 diamond, 4 crystal). */
    public int horseArmor;
    /** Werewolf day form: true -> render the human biped model + skin, false -> the beast model. */
    public boolean humanForm;
    /** Airborne flyers (wyvern): true -> flap wings + tuck legs instead of the walking gait. */
    public boolean flying;
    /** Golem cube presence bit-mask: bit {@code i} set iff anatomical cube {@code i} is present. */
    public int golemCubeMask;
    /** Golem life-cycle state (0 spawned, 1 summoning, 2 complete, 3 half-life, 4 dying). */
    public int golemState;
    /** Golem is inhaling a nearby vacuumed rock: splays its front chest cubes open. */
    public boolean golemOpenChest;
    /** Golem is in its rock-throwing windup: swings arms/legs forward. */
    public boolean golemThrowing;
    /** Fish bowl held fishy type (1-10) for the fish-bowl model; 0 when no fish is held. */
    public int fishBowlType;
    /** Fish bowl swim turn angle in degrees (0-360); circles the shown fish inside the bowl. */
    public float fishBowlRotation;
    /** MoC networked age 0-100 ("edad"); drives continuous size growth. 100 = full grown. */
    public int moCAge;
    /** True once flagged adult (clamps size to full regardless of moCAge). */
    public boolean adult;
    /** Per-species size multiplier on top of the age curve (crab 0.7x, donkey 0.9x); 1.0 = normal. */
    public float sizeFactor = 1.0F;
    /** Ostrich worn-helmet tier (0 none .. 12), coloured flag (0 none .. 16), and chest/saddlebag flag. */
    public int ostrichHelmet;
    public int ostrichFlagColor;
    public boolean ostrichChested;
    /** Bear pose state: 0 on all fours, 1 reared/standing, 2 sitting. */
    public int bearState;
    /** Turtle render flags: flipped onto its back / withdrawn into its shell. */
    public boolean turtleUpsideDown;
    public boolean turtleHiding;
    /** Turtle flip animation progress 0..1 (0 upright, 1 fully on its back) for the smooth roll. */
    public float turtleFlipProgress;
    /** Crocodile: swimming (legs streamline back) / biting (jaws agape on caught prey). */
    public boolean crocInWater;
    public boolean crocBiting;
    /** Ostrich head-in-sand hiding pose (legacy getHiding). */
    public boolean ostrichHiding;
    /** Scorpion sting-strike pose (tail arches forward while attacking/poisoning). */
    public boolean scorpionAttacking;
    /** Werewolf hunched aggressive crouch while charging a target. */
    public boolean werewolfHunched;
    /** Crab raises both claws defensively while fleeing or pinching back. */
    public boolean crabClawsUp;
    /** Big cat drops its lower jaw for a roar/bite. */
    public boolean bigcatJawOpen;
    /** Melee swing progress 0..1 (from getAttackAnim); drives attack-lunge poses (e.g. the wraith). */
    public float attackSwing;
    /** Horse is grazing (head lowered to the ground) / rearing up on its hind legs. */
    public boolean horseEating;
    public boolean horseRearing;
    /** Cobra (snake type 6) is flaring its hood at a nearby player. */
    public boolean snakeHoodFlared;
    /** Litter box has been used (dirty) — render the used-litter cube instead of the clean one. */
    public boolean litterBoxUsed;
    /** Kitty-bed dye colour (0 = plain/untinted, 1..16 = DyeColor id + 1) — tints the placed bed. */
    public int kittyBedColour;
    /** Jellyfish is glowing (night) — render its bell emissive/full-bright instead of the plain grey tint. */
    public boolean jellyfishGlowing;
    /** Jellyfish is beached (out of water, on the ground) — render it flopped on its side. */
    public boolean jellyfishBeached;
    /** Wyvern worn barding tier (0 none, 1 iron, 2 gold, 3 diamond) + storage-bag chest flag. */
    public int wyvernArmor;
    public boolean wyvernChested;
    /** Wyvern is sitting (grounded rest pose). */
    public boolean wyvernSitting;
    /** Scorpion is a female carrying young — render the on-back baby cluster. */
    public boolean scorpionHasBabies;
    /** Crocodile is in its death-roll spin (drives the barrel-roll render). */
    public boolean crocRolling;
    /** Kitty pose state: sitting flag + full kitty FSM state (0 calm/1 hungry/2 playing/3 sleeping). */
    public boolean kittySitting;
    public int kittyState;
    public boolean kittyTamed;
    /**
     * Per-cube block render states for the golem's per-block cube rendering: index {@code i} holds the
     * moving-block state for cube {@code i}, or {@code null} for an empty cube. Built each frame from the
     * golem's synched block ids in {@code MoCGolemRenderer.extractRenderState}.
     */
    public net.minecraft.client.renderer.block.MovingBlockRenderState[] golemCubeBlocks;

    /** Manticore wing beat in progress (legacy {@code wingFlapCounter != 0}) — full-amplitude wing sweep. */
    public boolean manticoreFlapping;
    /** Manticore is off the ground — wings unfold and cruise, and all four legs tuck back. */
    public boolean manticoreAirborne;
    /** Manticore scorpion tail is mid-strike (legacy {@code swingingTail()}) — the barb whips forward. */
    public boolean manticoreStinging;

    /** Riding something (legacy {@code getRidingEntity() != null}) — drives seated rider leg poses. */
    public boolean riding;
    /** Sprinting (legacy {@code isSprinting}) — drives the silver skeleton's forward charge lean. */
    public boolean sprinting;
    /** Silver skeleton per-arm katana swing counters (legacy attackCounterLeft/Right): 0 idle, 1..10 swinging. */
    public int silverSkeletonLeftSwing;
    public int silverSkeletonRightSwing;
    /** Mole burrow state (0 outside, 1 digging in, 2 underground, 3 peek-a-boo) — drives the sink + pitch. */
    public int moleState;
    /** Mini golem has an attack target — swaps its head and body to the red-hot skins. */
    public boolean miniGolemAngry;
    /** Mini golem is hoisting a ripped-up block overhead — both arms swing straight up. */
    public boolean miniGolemHasRock;
    /**
     * The block the mini golem is hoisting, drawn as a real full-size block above its head by
     * {@code MoCMiniGolemRenderer}; {@code null} when it is empty-handed.
     */
    public net.minecraft.client.renderer.block.MovingBlockRenderState miniGolemHeldBlock;
}
