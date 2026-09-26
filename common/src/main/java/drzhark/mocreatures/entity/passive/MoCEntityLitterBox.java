package drzhark.mocreatures.entity.passive;

import java.util.List;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.entity.monster.MoCEntityOgre;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Port of the legacy {@code MoCEntityLitterBox}. A static piece of placeable furniture the kitty
 * uses. It has no goals and cannot be pushed; it drops to the ground where it is placed and then stays
 * put (the same furniture physics as {@code MoCEntityKittyBed}, see there). It carries a synched
 * {@code used} flag: once a kitty uses it the box becomes dirty (rendered with the "used" litter) and
 * stays dirty until it is cleaned.
 * Faithful to the legacy entity it restores the signature behaviours of a dirty box:
 *
 * <ul>
 *   <li>while dirty it smoulders (smoke particles), lures every hostile mob within a 12x4x12 box
 *       onto itself (defusing creepers so they home in without exploding), and auto-cleans after
 *       ~5000 ticks;</li>
 *   <li>right-click with <b>sand</b> to scoop it clean (consuming one sand);</li>
 *   <li>right-click with any <b>pickaxe</b> to pick the box back up as its item;</li>
 *   <li>right-click empty-handed (or with anything else) to carry the box on your head / set it down;</li>
 *   <li>hit it (left-click) to break it: it drops its item, or simply vanishes in creative.</li>
 * </ul>
 */
public class MoCEntityLitterBox extends MoCAnimal {

    private static final EntityDataAccessor<Boolean> USED =
            SynchedEntityData.defineId(MoCEntityLitterBox.class, EntityDataSerializers.BOOLEAN);

    /** Server-side festering timer; a dirty box auto-cleans once this passes 5000 (legacy littertime). */
    private int littertime;

    /**
     * No {@code setNoAi(true)}: in 26.2 a NoAI mob never runs {@code travel}, so it never falls (mc262-ref
     * LivingEntity.java:3110 + Mob.java:1301-1302) — the box hung wherever it was placed or set down, exactly
     * like the kitty bed. See {@code MoCEntityKittyBed}'s constructor.
     */
    public MoCEntityLitterBox(EntityType<? extends MoCEntityLitterBox> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                // Legacy MoCEntityLitterBox.getMaxHealth() == 20 (matches the kitty bed's furniture toughness).
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** Static furniture: no goals at all. */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(USED, false);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("litterbox.png");
    }

    /** Whether the litter box is dirty and needs cleaning. */
    public boolean getUsed() {
        return this.entityData.get(USED);
    }

    public void setUsed(boolean used) {
        this.entityData.set(USED, used);
    }

    // ------------------------------------------------------------------------------------- tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // Clear any grass or flower the box came to rest in, so it doesn't poke up through the litter.
        if (this.onGround()) {
            breakPlantsInside((ServerLevel) this.level());
        }
        // Legacy MoCEntityLitterBox.onUpdate: while dirty the box smoulders, and the stench draws in
        // every hostile mob within a 12x4x12 box, pointing them at the litter box itself. Creepers are
        // defused (swell dir -1) so they home in without blowing the box apart.
        if (getUsed() && this.level() instanceof ServerLevel serverLevel) {
            this.littertime++;
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            List<Monster> mobs = serverLevel.getEntitiesOfClass(Monster.class,
                    this.getBoundingBox().inflate(12.0D, 4.0D, 12.0D));
            for (Monster mob : mobs) {
                mob.setTarget(this);
                if (mob instanceof Creeper creeper) {
                    creeper.setSwellDir(-1);
                }
                // Legacy cleared the ogre's pendingSmashAttack so a lured ogre abandons its in-progress
                // smash and lumbers over to the box instead of pulverising where it stands.
                if (mob instanceof MoCEntityOgre ogre) {
                    ogre.resetSmash();
                }
            }
        }
        // Auto-clean once the litter has festered long enough (legacy littertime > 5000).
        if (this.littertime > 5000) {
            setUsed(false);
            this.littertime = 0;
        }
    }

    // ------------------------------------------------------------------------------- interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        final boolean server = !this.level().isClientSide();

        // Any pickaxe -> pick the litter box back up as its item and remove it (legacy dropped
        // MoCreatures.litterbox, the same item that places it back down).
        if (stack.is(ItemTags.PICKAXES)) {
            if (server) {
                ItemStack drop = new ItemStack(MoCItems.KITTYLITTER.get());
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
                this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                        ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }
        // Sand -> scoop the box clean, consuming one sand (legacy required Block.sand specifically; red
        // sand does not clean it).
        if (stack.is(Items.SAND)) {
            if (server) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                setUsed(false);
                this.littertime = 0;
            }
            return InteractionResult.SUCCESS;
        }
        // Otherwise: carry the litter box on the player's head, or set it back down (legacy mount toggle).
        if (server) {
            // Shared carry toggle. The furniture used the same "ride the player" idiom the pets did, and it
            // was broken for exactly the same reason: a player cannot be a vehicle on a 26.2 server.
            toggleCarry(player, false);
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------------------------- damage

    /**
     * Legacy {@code MoCEntityLitterBox.attackEntityFrom} returned {@code false}: no damage destroys the box —
     * which, notably, keeps the lured-in hostile mobs from destroying the box they home in on. The one
     * exception is {@code BYPASSES_INVULNERABILITY} damage, i.e. {@code /kill} and the void, which the blanket
     * {@code false} used to block as well (see {@code MoCEntityKittyBed.hurtServer}). Players break it through
     * {@link #skipAttackInteraction}.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
    }

    /**
     * A player's hit breaks the box and drops the litter box item (nothing in creative) — the struck-target
     * hook vanilla paintings use, so a sword sweep or a lured mob never breaks it. Legacy's only removal was
     * the pickaxe right-click, still in {@link #mobInteract}. See {@code MoCEntityKittyBed.skipAttackInteraction}.
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!(attacker instanceof Player player)) {
            return false;
        }
        if (this.level() instanceof ServerLevel serverLevel && !this.isRemoved()
                && serverLevel.mayInteract(player, this.blockPosition())) {
            if (!player.hasInfiniteMaterials()) {
                this.spawnAtLocation(serverLevel, new ItemStack(MoCItems.KITTYLITTER.get()));
            }
            this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                    ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            this.discard();
        }
        return true;
    }

    // ---------------------------------------------------------------------------------- movement

    /**
     * Legacy {@code MoCEntityLitterBox.move} (12.0.5:155-162) ignored every move while the box sat on the
     * ground with {@code staticLitter} on. Same gate as {@code MoCEntityKittyBed.move}: a grounded box keeps
     * only the downward part of its own motion, so it still settles and still drops if its floor is removed.
     */
    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && drzhark.mocreatures.config.MoCConfig.get().staticLitter && this.onGround()) {
            this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
            delta = new Vec3(0.0D, Math.min(0.0D, delta.y), 0.0D);
        }
        super.move(type, delta);
    }

    /** Legacy {@code fall()} was empty (12.0.5:99-101). */
    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    /** Legacy {@code canBreatheUnderwater() == true} (12.0.5:89-92). */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    // ------------------------------------------------------------------------------ push behaviour

    // Static furniture isn't shoved around by other entities (legacy staticLitter, default on). Setting
    // staticLitter=false lets the litter box be pushed like a loose entity.
    @Override
    public boolean isPushable() {
        return !drzhark.mocreatures.config.MoCConfig.get().staticLitter;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    // ------------------------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Used", getUsed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setUsed(input.getBooleanOr("Used", false));
        // Boxes placed by the earlier build were saved with NoAI:1b and would reload frozen in mid-air
        // (mc262-ref Mob.java:388-389 / :408). The box has no AI to disable, so clear it.
        this.setNoAi(false);
    }
}
