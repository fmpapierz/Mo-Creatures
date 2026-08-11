package drzhark.mocreatures.entity.passive;

import java.util.List;

import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityEnt} ({@code entity/passive/MoCEntityEnt.java}). The Ent is a walking
 * tree — 1.4 blocks wide and a full <b>7 blocks tall</b>, with a 2-block step height so it strides over
 * fences and terraces rather than pathing round them ({@code MoCEntityEnt}:38-42).
 *
 * <p>Two sub-types, selected 50/50 on spawn and driving both skin and drops
 * ({@code MoCEntityEnt.selectType}:62-66):</p>
 * <ul>
 *   <li><b>1 — oak</b> ({@code ent_oak.png}): drops oak logs / oak saplings, plants oak saplings.</li>
 *   <li><b>2 — birch</b> ({@code ent_birch.png}): drops birch logs / birch saplings, plants birch saplings.</li>
 * </ul>
 *
 * <p><b>Why it is <em>not</em> simply a hostile mob.</b> The Ent has a {@code MeleeAttackGoal} but no target
 * goal of its own: legacy never gave it {@code findPlayerToAttack} and never installed a hurt-by-target
 * task. The one and only way it acquires a target is
 * {@code attackEntityFrom} — and only when a player strikes it <b>with an axe</b>
 * ({@code MoCEntityEnt}:80-101). Cut one down and it fights back; hit it with anything else and it does not
 * even notice.</p>
 *
 * <p><b>Axe-or-fire damage immunity.</b> The same method makes the Ent immune to <em>everything else</em>:
 * a player's sword, arrows, other mobs, fall damage and drowning all return {@code false} without taking a
 * point off it. Only an axe swung by a player and fire damage get through — a tree is felled with an axe or
 * burned down, nothing else. This immunity is reproduced verbatim in {@link #hurtServer}.</p>
 *
 * <p><b>Gardening.</b> Roughly once every 500 ticks while it has no target, the Ent tends the ground it
 * stands on ({@code plantOnFertileGround}:178-227): bare dirt under its feet is turned to grass, and on
 * grass it scatters flowers, tall grass, ferns, mushrooms and saplings of its own species across the 3x3
 * around it. It also calls small animals to it roughly once every 100 ticks ({@code atractCritter}:156-176),
 * so a wandering Ent collects a little procession of critters and leaves a trail of meadow behind it.</p>
 *
 * @see drzhark.mocreatures.client.model.MoCModelEnt
 */
public class MoCEntityEnt extends MoCAnimal {

    /** Legacy {@code selectType}: sub-type 1 = oak, 2 = birch. */
    public static final int TYPE_OAK = 1;
    public static final int TYPE_BIRCH = 2;

    public MoCEntityEnt(EntityType<? extends MoCEntityEnt> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy {@code applyEntityAttributes}:52-59 — 40 HP, 3 attack damage, 0.2 movement speed. The
     * {@code stepHeight = 2F} from the constructor ({@code MoCEntityEnt}:41) becomes the 26.2
     * {@code STEP_HEIGHT} attribute, which is the modern home of that field.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.STEP_HEIGHT, 2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy isNotScared() == true (:311-314): the Ent's fleeingTick was zeroed every tick, so it never
        // ran away. Strip the PanicGoal MoCAnimal installs for ordinary animals — a struck Ent stands and
        // (if the blow came from an axe) swings back.
        this.goalSelector.removeAllGoals(g -> g instanceof PanicGoal);
        // Legacy initEntityAI:47 — EntityAIAttackMelee(this, 1.0D, true) at priority 5. Deliberately WITHOUT
        // any target goal: the Ent never hunts and never auto-retaliates. hurtServer() is the sole source of
        // a target, and only for an axe-wielding player (see the class javadoc).
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        // Legacy priorities 1 (swimming), 6 (EntityAIWanderMoC2) and 7 (watch player) are already covered by
        // MoCAnimal's shared FloatGoal / WaterAvoidingRandomStrollGoal / LookAtPlayerGoal set.
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // Legacy: setType(rand.nextInt(2) + 1) -> 1 (oak) or 2 (birch), 50/50.
            setTypeMoC(this.random.nextInt(2) + 1);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case TYPE_BIRCH -> modelTexture("ent_birch.png");
            default -> modelTexture("ent_oak.png");
        };
    }

    // ------------------------------------------------------------------------------------- damage

    /**
     * Legacy {@code attackEntityFrom}:80-101, reproduced exactly.
     *
     * <p>An Ent is a tree: it is felled with an axe or burned, and is otherwise <b>immune</b>. A player
     * holding an axe deals normal damage and — unless the Ent is tamed or the world is Peaceful
     * ({@code MoCEntityAnimal.shouldAttackPlayers}:1167-1169) — becomes its target, which is the ONLY way
     * this creature ever acquires one. Fire damage also lands. Every other source (swords, arrows, other
     * mobs, fall, drowning, suffocation, cactus, magic) is refused outright.</p>
     *
     * <p>One deliberate addition: sources tagged {@code bypasses_invulnerability} (the {@code /kill}
     * command and the void) are let through. Legacy 1.12.2 did not need this because {@code /kill} went
     * through {@code onKillCommand()} rather than {@code attackEntityFrom}; in 26.2
     * {@code LivingEntity.kill} routes through this method, so without the exception an Ent could never be
     * removed by an operator or by falling out of the world.</p>
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            // Legacy read ep.inventory.getCurrentItem() -> the main hand. ItemTags.AXES is checked alongside
            // `instanceof AxeItem` so modded axes that do not extend AxeItem still fell an Ent.
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof AxeItem || held.is(ItemTags.AXES)) {
                if (!getIsTamed() && level.getDifficulty() != Difficulty.PEACEFUL) {
                    setTarget(player);
                }
                return super.hurtServer(level, source, amount);
            }
        }
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        return false;
    }

    /**
     * Legacy {@code applyEnchantments}:304-309: whenever the Ent's swipe connects it plays the "smack" sound
     * and hurls the victim with {@code MoCTools.bigsmack(this, entity, 1F)} — force 1.0, which is a huge
     * launch: the victim's momentum is halved, then it is thrown directly away from the Ent and one full
     * unit straight up. (In 1.12 {@code applyEnchantments} was invoked by {@code attackEntityAsMob} only on
     * a landed hit, so the shove is gated on {@code super.doHurtTarget} returning true here.)
     *
     * <p>Legacy reused the goat's smack sound ({@code MoCSoundEvents.ENTITY_GOAT_SMACK}) for it, hence
     * {@code MoCSounds.GOATSMACK} below rather than an Ent-specific sound.</p>
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            this.playSound(MoCSounds.GOATSMACK.get(), 1.0F,
                    1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            bigSmack(target, 1.0F);
        }
        return hit;
    }

    /**
     * Legacy {@code MoCTools.bigsmack}: halve the victim's momentum, then shove it directly away from this
     * entity by {@code force} horizontally and {@code force} upward (the vertical component is clamped to
     * {@code force}).
     */
    private void bigSmack(Entity victim, float force) {
        double dx = victim.getX() - this.getX();
        double dz = victim.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.01D) {
            // Legacy jittered the offset when the two entities were exactly stacked, to avoid a divide by ~0.
            dx = (Math.random() - Math.random()) * 0.01D;
            dz = (Math.random() - Math.random()) * 0.01D;
            dist = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        }
        net.minecraft.world.phys.Vec3 halved = victim.getDeltaMovement().scale(0.5D);
        victim.setDeltaMovement(halved.x + (dx / dist) * force,
                Math.min(force, halved.y + force),
                halved.z + (dz / dist) * force);
        victim.hurtMarked = true; // 26.2 syncs impulses to the client via hurtMarked
    }

    /** Legacy {@code canBePushed()} returns false (:288-291) — an Ent is a tree; nothing shoulders it aside. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * Legacy {@code canTriggerWalking()} returns false (:316-319). In 1.12 that flag suppressed the walking
     * step sound; 26.2 has no such flag, so the closest faithful equivalent is a no-op
     * {@code playStepSound} — a creature made of living wood pads about silently.
     */
    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        // intentionally silent
    }

    // ------------------------------------------------------------------------------------ sounds

    /*
     * Legacy referenced ENTITY_ENT_AMBIENT / _DEATH / _HURT ("entgrunt" / "entdying" / "enthurt"), but every
     * one of those was flagged `// TODO` in MoCSoundEvents (:411-413) and 12.0.5 ships NEITHER a sounds.json
     * entry NOR an .ogg for any of them — the Ent was already silent in 1.12.2. Rather than register three
     * sound events that resolve to nothing (and log a missing-sound warning on every client), the port leaves
     * them out; the only sound an Ent actually makes is the smack in doHurtTarget above. If Ent audio is ever
     * authored, add the three events to MoCSounds/sounds.json and override the three hooks below.
     */
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    // ------------------------------------------------------------------------------------- drops

    /**
     * Legacy {@code dropFewItems}:103-121 — exactly ONE of three stacks per kill, each of
     * {@code rand.nextInt(12) + 4} = 4-15 items:
     * <ul>
     *   <li>1/3 logs of the Ent's own wood (oak for type 1, birch for type 2),</li>
     *   <li>1/3 sticks,</li>
     *   <li>1/3 saplings of the Ent's own wood.</li>
     * </ul>
     * The legacy metadata {@code typ} (0 = oak, 2 = birch) is what selected the wood; here it maps to the
     * distinct 26.2 items.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        // Honour the server-admin loot suppression flags the shared MoCBehavior.dropLoot applies, so an Ent
        // does not sidestep destroyDrops / destroyPassiveDrops with its hand-rolled drop.
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        if (cfg.destroyDrops || cfg.destroyPassiveDrops) {
            return;
        }
        boolean birch = getTypeMoC() == TYPE_BIRCH;
        int qty = this.random.nextInt(12) + 4;
        int roll = this.random.nextInt(3);
        if (roll == 0) {
            spawnAtLocation(level, new ItemStack(birch ? Items.BIRCH_LOG : Items.OAK_LOG, qty));
        } else if (roll == 1) {
            spawnAtLocation(level, new ItemStack(Items.STICK, qty));
        } else {
            spawnAtLocation(level, new ItemStack(birch ? Items.BIRCH_SAPLING : Items.OAK_SAPLING, qty));
        }
    }

    // ------------------------------------------------------------------------------- ambient behaviour

    /**
     * Legacy {@code onLivingUpdate}:138-151 — the two server-side ambient rolls: plant something roughly
     * every 500 ticks while the Ent is not fighting, and call critters over roughly every 100 ticks.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (getTarget() == null && this.random.nextInt(500) == 0) {
            plantOnFertileGround(level);
        }
        if (this.random.nextInt(100) == 0) {
            attractCritters();
        }
    }

    /**
     * Legacy {@code atractCritter}:153-176 — "Makes small creatures follow the Ent". Every small, untamed,
     * idle animal within (8, 3, 8) of the Ent is sent walking towards it, up to {@code rand.nextInt(3) + 1}
     * of them per call, so an Ent wanders the forest trailed by a handful of bunnies, ducks and mice.
     *
     * <p><b>Deviation:</b> legacy also called {@code entityanimal.setAttackTarget(this)}. In 1.12 the
     * Mo'Creatures {@code entityToAttack} field doubled as "the thing I walk towards" for passive animals,
     * so that call was how the critter followed. In 26.2 {@code setTarget} means genuinely hostile intent,
     * and setting it would make any small creature that owns a {@code MeleeAttackGoal} (a Mo'Creatures
     * {@code wildHostile} species, a modded critter) actually attack the Ent. The follow is therefore
     * expressed purely as navigation, which is what the legacy line after it — {@code getNavigator().setPath(
     * pathToEntityLiving(this))} — did anyway.</p>
     */
    private void attractCritters() {
        int limit = this.random.nextInt(3) + 1;
        int called = 0;
        List<Animal> nearby = this.level().getEntitiesOfClass(Animal.class,
                getBoundingBox().inflate(8.0D, 3.0D, 8.0D), a -> a != this);
        for (Animal animal : nearby) {
            // Legacy gate: an EntityAnimal strictly smaller than 0.6 in BOTH width and height, with no target
            // of its own, and not tamed (MoCTools.isTamed: a vanilla tameable that is tamed, or anything
            // carrying an Owner/Tamed tag -> the Mo'Creatures getIsTamed flag here).
            if (animal.getBbWidth() >= 0.6F || animal.getBbHeight() >= 0.6F) {
                continue;
            }
            if (animal.getTarget() != null || isCritterTamed(animal)) {
                continue;
            }
            animal.getNavigation().moveTo(this, 1.0D);
            if (++called > limit) {
                return;
            }
        }
    }

    /** Legacy {@code MoCTools.isTamed}:1597-1612 — a tamed vanilla pet, or a tamed Mo'Creatures creature. */
    private static boolean isCritterTamed(Animal animal) {
        return (animal instanceof TamableAnimal tamable && tamable.isTame())
                || (animal instanceof IMoCEntity moc && moc.getIsTamed());
    }

    /**
     * Legacy {@code plantOnFertileGround}:178-227 — the Ent's gardening pass.
     *
     * <ul>
     *   <li>Standing on bare <b>dirt</b>: the dirt under its feet becomes a grass block. Nothing else
     *       happens this pass — the Ent prepares the soil first and plants on a later roll.</li>
     *   <li>Standing on <b>grass</b> with air at foot level: it picks one random plant (see
     *       {@link #plantToBePlanted}) and tries to place it in each of the nine cells of the 3x3 around
     *       itself, each cell independently on a {@code 1-in-3} roll — or {@code 1-in-10} when the pick came
     *       up a sapling, so an Ent seeds far more meadow than forest.</li>
     * </ul>
     *
     * <p><b>Deviations.</b> (1) Legacy asked Forge for permission with a {@code BlockEvent.BreakEvent} fired
     * through a fake player; the loader-neutral equivalent in this port is the vanilla {@code mobGriefing}
     * game rule, which is exactly the "may this mob change blocks" switch that protection mods hook.
     * (2) A {@code canSurvive} check is added before each placement: 1.12 happily wrote a flower onto sand
     * and let the next block update pop it as an item, which in 26.2 just litters the ground with item
     * entities. (3) The six two-block-tall {@code double_plant} variants legacy addressed by metadata
     * (sunflower, lilac, tall grass, large fern, rose bush, peony) are placed with
     * {@code DoublePlantBlock.placeAt} so both halves are written — a lone lower half would break instantly.</p>
     *
     * @return whether the Ent found ground it could work with
     */
    private boolean plantOnFertileGround(ServerLevel level) {
        // Legacy: the "may this mob change blocks here" gate (a fake-player BreakEvent in 1.12).
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }
        BlockPos pos = BlockPos.containing(getX(), getY(), getZ());
        Block blockUnderFeet = level.getBlockState(pos.below()).getBlock();
        Block blockOnFeet = level.getBlockState(pos).getBlock();

        if (blockUnderFeet == Blocks.DIRT) {
            level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }

        if (blockUnderFeet == Blocks.GRASS_BLOCK && blockOnFeet == Blocks.AIR) {
            BlockState plant = plantToBePlanted();
            // Legacy: saplings are rolled at 1-in-10 per cell, everything else at 1-in-3, so a pass that came
            // up "sapling" seeds at most a tree or two rather than a nine-sapling thicket.
            int plantChance = (plant.is(Blocks.OAK_SAPLING) || plant.is(Blocks.BIRCH_SAPLING)) ? 10 : 3;
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    BlockPos target = pos.offset(x, 0, z);
                    if (this.random.nextInt(plantChance) != 0 || !level.getBlockState(target).isAir()) {
                        continue;
                    }
                    if (!plant.canSurvive(level, target)) {
                        continue; // see deviation (2) above
                    }
                    if (plant.getBlock() instanceof DoublePlantBlock) {
                        if (level.getBlockState(target.above()).isAir()) {
                            DoublePlantBlock.placeAt(level, plant, target, Block.UPDATE_ALL);
                        }
                    } else {
                        level.setBlock(target, plant, Block.UPDATE_ALL);
                    }
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Legacy {@code getBlockStateToBePlanted}:229-286 — one random plant, weighted out of 20 by the legacy
     * numeric block ids and metadata:
     * <ul>
     *   <li>8/20 — id 31 {@code tallgrass}, meta 1 or 2: short grass or fern.</li>
     *   <li>3/20 — id 175 {@code double_plant}, meta 0-5: sunflower, lilac, tall grass, large fern,
     *       rose bush, peony.</li>
     *   <li>3/20 — id 37 {@code yellow_flower}: dandelion.</li>
     *   <li>3/20 — id 38 {@code red_flower}, meta 0-8: poppy, blue orchid, allium, azure bluet and the four
     *       tulips, oxeye daisy.</li>
     *   <li>1/20 — id 39: brown mushroom. 1/20 — id 40: red mushroom.</li>
     *   <li>1/20 — id 6 {@code sapling}, meta 0 or 2: a sapling of the Ent's <em>own</em> species (legacy set
     *       meta 2 for a type-2 Ent), so a birch Ent seeds birch and an oak Ent seeds oak.</li>
     * </ul>
     */
    private BlockState plantToBePlanted() {
        return switch (this.random.nextInt(20)) {
            // 0-7: tallgrass meta rand.nextInt(2) + 1 -> 1 = grass, 2 = fern
            case 0, 1, 2, 3, 4, 5, 6, 7 ->
                    (this.random.nextInt(2) == 0 ? Blocks.SHORT_GRASS : Blocks.FERN).defaultBlockState();
            // 8-10: double_plant meta rand.nextInt(6) -> 0..5
            case 8, 9, 10 -> switch (this.random.nextInt(6)) {
                case 0 -> Blocks.SUNFLOWER.defaultBlockState();
                case 1 -> Blocks.LILAC.defaultBlockState();
                case 2 -> Blocks.TALL_GRASS.defaultBlockState();
                case 3 -> Blocks.LARGE_FERN.defaultBlockState();
                case 4 -> Blocks.ROSE_BUSH.defaultBlockState();
                default -> Blocks.PEONY.defaultBlockState();
            };
            // 11-13: dandelion
            case 11, 12, 13 -> Blocks.DANDELION.defaultBlockState();
            // 14-16: red_flower meta rand.nextInt(9) -> 0..8
            case 14, 15, 16 -> switch (this.random.nextInt(9)) {
                case 0 -> Blocks.POPPY.defaultBlockState();
                case 1 -> Blocks.BLUE_ORCHID.defaultBlockState();
                case 2 -> Blocks.ALLIUM.defaultBlockState();
                case 3 -> Blocks.AZURE_BLUET.defaultBlockState();
                case 4 -> Blocks.RED_TULIP.defaultBlockState();
                case 5 -> Blocks.ORANGE_TULIP.defaultBlockState();
                case 6 -> Blocks.WHITE_TULIP.defaultBlockState();
                case 7 -> Blocks.PINK_TULIP.defaultBlockState();
                default -> Blocks.OXEYE_DAISY.defaultBlockState();
            };
            case 17 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
            case 18 -> Blocks.RED_MUSHROOM.defaultBlockState();
            // 19: a sapling matching this Ent's own wood
            case 19 -> (getTypeMoC() == TYPE_BIRCH ? Blocks.BIRCH_SAPLING : Blocks.OAK_SAPLING).defaultBlockState();
            default -> Blocks.SHORT_GRASS.defaultBlockState();
        };
    }
}
