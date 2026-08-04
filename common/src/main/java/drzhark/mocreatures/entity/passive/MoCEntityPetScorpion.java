package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.entity.MoCBehavior;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityPetScorpion}. A tameable pet scorpion with five variants
 * (dirt, cave, nether, frost, undead), each with a saddled texture.
 */
public class MoCEntityPetScorpion extends MoCAnimal {

    /** Synched: this pet scorpion is a female carrying young and will drop babies on death. */
    private static final EntityDataAccessor<Boolean> HAS_BABIES =
            SynchedEntityData.defineId(MoCEntityPetScorpion.class, EntityDataSerializers.BOOLEAN);

    static {
        // Legacy pet scorpions are a HOSTILE mob (MoCEntityPetScorpion.findPlayerToAttack + attackEntity):
        // an untamed ADULT hunts nearby players at night and stings on contact (the sting proc lives in
        // doHurtTarget below), retaliates when struck, and adults never flee (onLivingUpdate forced
        // fleeingTick=0). The shared MoCAnimal.registerGoals installs the MeleeAttackGoal (which drives our
        // doHurtTarget), the untamed-adult NearestAttackableTargetGoal + HurtByTargetGoal, and drops the
        // PanicGoal ONLY when the creature's behaviour spec is wildHostile. Without it the whole sting is dead
        // code and untamed adults panic-flee instead of attacking. MoCBehavior owns the reg() table (a sibling
        // file), so we flip the pet_scorpion spec's wildHostile flag here — the exact equivalent of appending
        // .hostile() to reg("pet_scorpion") — before any pet scorpion is constructed (registerGoals runs in
        // the Mob constructor, i.e. after this class-init static block).
        MoCBehavior.get("pet_scorpion").wildHostile = true;
    }

    public MoCEntityPetScorpion(EntityType<? extends MoCEntityPetScorpion> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_BABIES, false);
    }

    /** Whether this pet scorpion is carrying young (and will release babies on death). */
    public boolean getHasBabies() {
        return this.entityData.get(HAS_BABIES);
    }

    public void setHasBabies(boolean flag) {
        this.entityData.set(HAS_BABIES, flag);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        // Legacy: ~1 in 4 pet scorpions is a female carrying young.
        setHasBabies(this.random.nextInt(4) == 0);
        return data;
    }

    /** Spider-style wall climb (legacy {@code isOnLadder() = isCollidedHorizontally}). */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    /**
     * Legacy {@code findPlayerToAttack} let an untamed adult pet scorpion PROACTIVELY hunt players only at
     * NIGHT ({@code worldObj.difficultySetting > 0 && !worldObj.isDaytime() && getIsAdult()}). The shared
     * {@link MoCAnimal#registerGoals()} installs an always-on {@code NearestAttackableTargetGoal<Player>}
     * (gated only on {@code !getIsTamed() && getIsAdult()}), which would let it sting in broad daylight —
     * so veto a fresh daytime player target here. Retaliation still works day or night: legacy
     * {@code attackEntityFrom} set {@code entityToAttack} to whatever struck it regardless of time, so a
     * target that equals {@link #getLastHurtByMob()} (the {@code HurtByTargetGoal} path) is left alone.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player && !getIsTamed() && this.level().isBrightOutside()
                && target != this.getLastHurtByMob()) {
            return;
        }
        super.setTarget(target);
    }

    /**
     * Legacy {@code attackEntity} gap-charge: when grounded with a target 2-6 blocks away, on a 1-in-50 tick
     * the scorpion lunges toward it — horizontally at {@code (delta/dist)*0.5*0.8} plus 0.2 of its current
     * momentum, with {@code motionY = 0.4} — so it can spring at prey and hop small obstacles.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        if (target != null && this.onGround()) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double distSq = (dx * dx) + (dz * dz);
            if (distSq > 4.0D && distSq < 36.0D && this.random.nextInt(50) == 0) {
                double dist = Math.sqrt(distSq);
                net.minecraft.world.phys.Vec3 dm = this.getDeltaMovement();
                this.setDeltaMovement(
                        ((dx / dist) * 0.5D * 0.8D) + (dm.x * 0.2D),
                        0.4D,
                        ((dz / dist) * 0.5D * 0.8D) + (dm.z * 0.2D));
                this.hurtMarked = true; // force a velocity sync so the client sees the lunge
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // Legacy pet scorpion attackEntity dealt exactly 1 damage on a plain sting (and 0 on the
                // status-proc hit); max melee is 1, not 3.
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("scorpioncave.png");
            case 3 -> modelTexture("scorpionnether.png");
            case 4 -> modelTexture("scorpionfrost.png");
            case 5 -> modelTexture("scorpionundead.png");
            default -> modelTexture("scorpiondirt.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.SCORPIONGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.SCORPIONHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.SCORPIONDYING.get();
    }

    /**
     * A pet scorpion's sting, mirroring the legacy {@code attackEntity} proc. On each landed hit there is a
     * ~1-in-5 chance ({@code rand.nextInt(5) == 0}) that the sting applies a type-specific status and deals
     * NO direct damage; every other hit is a plain sting for exactly 1 damage (via {@code super}). Per-type
     * effects match legacy: dirt/cave (&lt;=2) poison, nether (3) fire — players only and never in the Nether,
     * frost (4) slowness; undead (5) has no status effect in legacy.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity victim && this.random.nextInt(5) == 0) {
            // Legacy status-proc branch: apply the per-type effect and deal NO direct damage.
            switch (getTypeMoC()) {
                case 3 -> { // nether scorpion: legacy only sets fire on players, and never in the Nether
                    if (victim instanceof Player && level.dimension() != net.minecraft.world.level.Level.NETHER) {
                        victim.igniteForSeconds(15.0F);
                    }
                }
                case 4 -> // frost scorpion: slows the target
                        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0), this);
                case 5 -> { /* undead scorpion: legacy applies no status effect */ }
                default -> // dirt/cave scorpions (type <= 2): poison the target
                        victim.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0), this);
            }
            level.playSound(null, this.blockPosition(), MoCSounds.SCORPIONSTING.get(), SoundSource.NEUTRAL,
                    1.0F, 1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
            return true;
        }
        // Non-proc hit: a plain sting for exactly 1 melee damage (ATTACK_DAMAGE = 1.0D).
        return super.doHurtTarget(level, target);
    }

    /**
     * On death, a female carrying young ({@link #getHasBabies()}) releases 0-4 wild monster scorpions of
     * the same variant, mirroring the legacy {@code onDeath} brood ({@code rand.nextInt(5)} of
     * {@code MoCEntityScorpion}) — the pet-&gt;wild cross-spawn (wild scorpions in turn brood pet babies).
     */
    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel && getIsAdult() && getHasBabies()) {
            int count = this.random.nextInt(5); // 0-4 babies (legacy rand.nextInt(5))
            for (int i = 0; i < count; i++) {
                drzhark.mocreatures.entity.monster.MoCEntityScorpion baby =
                        new drzhark.mocreatures.entity.monster.MoCEntityScorpion(MoCEntities.SCORPION.get(), serverLevel);
                baby.setPos(this.getX(), this.getY(), this.getZ());
                baby.setYRot(this.getYRot());
                baby.setTypeMoC(getTypeMoC());
                baby.setAdult(false);
                baby.setHealth(baby.getMaxHealth());
                serverLevel.addFreshEntity(baby);
                serverLevel.playSound(null, this.blockPosition(),
                        SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                        1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
            }
        }
        super.die(damageSource);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Babies", getHasBabies());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHasBabies(input.getBooleanOr("Babies", false));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getIsTamed()) {
            // Undead essence (legacy vialundead) turns a pet scorpion into the undead variant (type 5), or fully
            // heals it if already undead; darkness essence (vialdarkness) just fully heals it.
            if (stack.is(MoCItems.ESSENCEUNDEAD.get())) {
                if (!this.level().isClientSide()) {
                    if (getTypeMoC() == 5) {
                        setHealth(getMaxHealth());
                    } else {
                        setTypeMoC(5);
                    }
                    drinkEssence(player, hand, stack);
                }
                return InteractionResult.SUCCESS;
            }
            if (stack.is(MoCItems.ESSENCEDARKNESS.get())) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    setHealth(getMaxHealth());
                    drinkEssence(player, hand, stack);
                    // Legacy vialdarkness on a tamed pet scorpion also lays its breeding egg: a MoCEntityEgg
                    // with composite id getType()+40 (41-45 -> scorpion variants 1-5) at the player's position,
                    // with a small random toss (legacy motionX/Z += (rand-rand)*0.3, motionY += rand*0.05).
                    MoCEntityEgg egg = new MoCEntityEgg(MoCEntities.EGG.get(), serverLevel);
                    egg.setEggType(getTypeMoC() + 40);
                    egg.setPos(player.getX(), player.getY(), player.getZ());
                    egg.setDeltaMovement(
                            (this.random.nextFloat() - this.random.nextFloat()) * 0.3F,
                            this.random.nextFloat() * 0.05F,
                            (this.random.nextFloat() - this.random.nextFloat()) * 0.3F);
                    serverLevel.addFreshEntity(egg);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Consumes one essence vial for a glass bottle, with the drink sound (shared by the transform branches). */
    private void drinkEssence(Player player, InteractionHand hand, ItemStack stack) {
        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, bottle);
        } else {
            player.addItem(bottle);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_DRINK.value(),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
