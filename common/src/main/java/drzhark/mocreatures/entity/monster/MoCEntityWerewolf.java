package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityWerewolf}. A hostile shapeshifter with several wolf-coat
 * variants and a day/night transform: by day it reverts to the weak, harmless human form
 * ({@code werehuman} texture, 15 HP, won't hunt); at night it becomes the empowered beast
 * (coat texture by type, 40 HP, faster, stronger). The form is synched so the client renders it.
 */
public class MoCEntityWerewolf extends MoCMob {

    // Synched so the client knows the current form and can pick the right texture. Defined on this
    // class (not MoCMob) to avoid data-accessor id collisions with the base mob fields.
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HUMAN_FORM =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    MoCEntityWerewolf.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    /** Synched hunched-charge flag: true while the beast is stalking a target, for the aggressive crouch pose. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HUNCHED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    MoCEntityWerewolf.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public MoCEntityWerewolf(EntityType<? extends MoCEntityWerewolf> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUMAN_FORM, false);
        builder.define(HUNCHED, false);
    }

    public boolean isHumanForm() {
        return this.entityData.get(HUMAN_FORM);
    }

    public void setHumanForm(boolean humanForm) {
        this.entityData.set(HUMAN_FORM, humanForm);
    }

    /** True while the beast is stalking a target on the ground — the client renders the aggressive hunched crouch. */
    public boolean isHunched() {
        return this.entityData.get(HUNCHED);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HumanForm", isHumanForm());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        setHumanForm(input.getBooleanOr("HumanForm", false));
        // Keep the transient nightForm flag in lock-step with the persisted form and re-apply the form's
        // attributes on load (without snapping health, so the saved HP is preserved) — otherwise a loaded
        // beast/human could run the wrong branch's AI + stats until the next day/night boundary.
        this.nightForm = !isHumanForm();
        applyFormAttributes(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                // Legacy findPlayerToAttack used getClosestVulnerablePlayerToEntity(this, 16D): the beast form
                // only hunted players within 16 blocks. Cap FOLLOW_RANGE at 16 so the inherited
                // NearestAttackableTargetGoal acquires targets at that radius instead of the vanilla default 32.
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int k = this.random.nextInt(100);
            if (k <= 28) {
                setTypeMoC(1);
            } else if (k <= 56) {
                setTypeMoC(2);
            } else if (k <= 85) {
                setTypeMoC(3);
            } else {
                setTypeMoC(4);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        if (isHumanForm()) {
            // Per-type human skins (faithful to the legacy werewolf human form).
            return switch (getTypeMoC()) {
                case 1 -> modelTexture("weredude.png");
                case 2 -> modelTexture("werehuman.png");
                case 3 -> modelTexture("wereoldie.png");
                default -> modelTexture("werewoman.png");
            };
        }
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("blackwerewolf.png");
            case 3 -> modelTexture("timberwerewolf.png"); // timber coat (extracted wolftimber.png from 12.0.5)
            case 4 -> modelTexture(fireFrame()); // fire werewolf: flickers through firewerewolf1-3 when animated
            default -> modelTexture("brownwerewolf.png");
        };
    }

    /**
     * The fire werewolf's flame texture, cycling {@code firewerewolf1..3} for a flickering fire when the
     * {@code animateTextures} config is on (legacy {@code getAnimateTextures}); static frame 1 when off.
     */
    private String fireFrame() {
        if (drzhark.mocreatures.config.MoCConfig.get().animateTextures) {
            return "firewerewolf" + (((this.tickCount / 3) % 3) + 1) + ".png";
        }
        return "firewerewolf1.png";
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        // Legacy getLivingSound() branches on form: the human form idles with the distinct human
        // "werehumangrunt" cue, only the beast form uses "werewolfgrunt". WEREHUMANGRUNT has no
        // registered sound asset yet, so the human form stays silent (null) rather than emitting the
        // beast growl — consistent with getHurtSound()/getDeathSound(), which also switch by form.
        return isHumanForm() ? null : MoCSounds.WEREWOLFGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        // The weak human form yelps in a human voice; the beast snarls (legacy werehumanhurt vs werewolfhurt).
        return isHumanForm() ? MoCSounds.WEREHUMANHURT.get() : MoCSounds.WEREWOLFHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return isHumanForm() ? MoCSounds.WEREHUMANDYING.get() : MoCSounds.WEREWOLFDYING.get();
    }

    @Override
    protected void applyHitEffects(net.minecraft.world.entity.LivingEntity target) {
        if (getTypeMoC() == 4) {
            target.igniteForSeconds(10.0F); // fire werewolf
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Silver-bullet rule: in beast form the werewolf shrugs off non-gold weapons (a player's blow with any
        // non-golden held item is reduced to 1), and only golden tools deal full damage (legacy attackEntityFrom).
        if (!isHumanForm() && source.getEntity() instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            if (held.is(drzhark.mocreatures.registry.MoCItems.SILVERSWORD.get())) {
                // Silver is the werewolf's bane: it bypasses the beast's tough hide and cuts twice as deep.
                amount *= 2.0F;
            } else if (!held.isEmpty()) {
                // Legacy attackEntityFrom: with any held item the damage is FORCED by weapon type — gold hoe=6,
                // shovel=7, pickaxe=8, axe=9, sword=10 (gold is the werewolf-bane), any other held item=1.
                // Bare-handed (empty) leaves the incoming damage unchanged.
                if (held.is(Items.GOLDEN_HOE)) {
                    amount = 6.0F;
                } else if (held.is(Items.GOLDEN_SHOVEL)) {
                    amount = 7.0F;
                } else if (held.is(Items.GOLDEN_PICKAXE)) {
                    amount = 8.0F;
                } else if (held.is(Items.GOLDEN_AXE)) {
                    amount = 9.0F;
                } else if (held.is(Items.GOLDEN_SWORD)) {
                    amount = 10.0F;
                } else {
                    amount = 1.0F;
                }
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        // Legacy onDeath dropped ONLY two getDropItemId() rolls and never called super — no MoCBehavior reg loot
        // (which would add a 20% golden apple even in human form) and no config spawn-egg drop. Deliberately skip
        // super.dropCustomDeathLoot so the drops match legacy exactly: just the two form-keyed tool rolls.
        for (int r = 0; r < 2; r++) {
            spawnAtLocation(level, new ItemStack(werewolfDrop(this.random.nextInt(12))));
        }
    }

    /** Legacy {@code getDropItemId}: human form drops wood tools/sticks; beast form drops iron/stone tools. */
    private Item werewolfDrop(int i) {
        if (isHumanForm()) {
            return switch (i) {
                case 0 -> Items.WOODEN_SHOVEL;
                case 1 -> Items.WOODEN_AXE;
                case 2 -> Items.WOODEN_SWORD;
                case 3 -> Items.WOODEN_HOE;
                case 4 -> Items.WOODEN_PICKAXE;
                default -> Items.STICK;
            };
        }
        return switch (i) {
            case 0 -> Items.IRON_HOE;
            case 1 -> Items.IRON_SHOVEL;
            case 2 -> Items.IRON_AXE;
            case 3 -> Items.IRON_PICKAXE;
            case 4 -> Items.IRON_SWORD;
            case 5 -> Items.STONE_HOE;
            case 6 -> Items.STONE_SHOVEL;
            case 7 -> Items.STONE_AXE;
            case 8 -> Items.STONE_PICKAXE;
            case 9 -> Items.STONE_SWORD;
            default -> Items.GOLDEN_APPLE;
        };
    }

    // ---------------------------------------------------------------- day/night transformation
    // By day the werewolf reverts to its weak, harmless "human" form (15 HP, won't hunt); at night it
    // transforms into the empowered hostile beast (40 HP, faster, stronger). The human form renders a
    // distinct biped model + per-type human skin (MoCModelWereHuman / MoCWerewolfRenderer) and uses
    // human hurt/death sounds; the change is a gradual, probabilistic windup with the weretransform cue.

    private boolean nightForm = true;
    /** Gradual-transform windup (legacy): counts down while the werewolf is mid-change; 0 = settled. */
    private int transformingTicks;
    /** The form the in-progress transform will settle into once the windup finishes. */
    private boolean pendingNightForm = true;

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy despawn-resistance (onLivingUpdate): periodically push back the vanilla despawn/age timer, scaled
        // by difficulty (peaceful=0 → no change), so a werewolf clings to loaded chunks far longer than a normal
        // monster instead of despawning under the usual unpersistent-mob rules.
        if (this.random.nextInt(300) == 0) {
            this.setNoActionTime(Math.max(0, this.getNoActionTime() - (100 * level.getDifficulty().getId())));
        }
        boolean shouldTransform = !level.isBrightOutside();
        // Legacy transform is probabilistic + GRADUAL, not instant at the day/night boundary: while caught in
        // the wrong form the werewolf randomly begins a ~30-tick change (body jitter + self-damage + the
        // weretransform cue), then flips form once the windup completes.
        if (this.transformingTicks == 0 && shouldTransform != nightForm && this.random.nextInt(60) == 0) {
            this.transformingTicks = 30;
            this.pendingNightForm = shouldTransform;
        }
        if (this.transformingTicks > 0) {
            this.transformingTicks--;
            // Jitter the body and bleed a little as the bones reshape.
            this.setDeltaMovement(this.getDeltaMovement().add(
                    (this.random.nextDouble() - 0.5D) * 0.1D, 0.0D, (this.random.nextDouble() - 0.5D) * 0.1D));
            this.hurtMarked = true;
            if (this.transformingTicks == 20) {
                level.playSound(null, blockPosition(), MoCSounds.WERETRANSFORM.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            if (this.transformingTicks == 0) {
                nightForm = this.pendingNightForm;
                applyForm(level);
            }
        }
        if (!nightForm) {
            // Human form: a harmless wanderer that won't chase prey.
            this.setTarget(null);
            if (isHunched()) {
                this.entityData.set(HUNCHED, false);
            }
        } else {
            // Beast form: a hunched charge — occasionally leaps toward a target 2-6 blocks away (legacy: on
            // ground, 1/15 tick, lunge horizontally at 0.5*0.8 with a bit of carried momentum + motionY 0.4).
            net.minecraft.world.entity.LivingEntity target = getTarget();
            // Hunched crouch pose whenever the beast is actively stalking a grounded target.
            boolean hunch = target != null && this.onGround();
            if (hunch != isHunched()) {
                this.entityData.set(HUNCHED, hunch);
            }
            if (target != null && this.onGround()) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double distSq = (dx * dx) + (dz * dz);
                if (distSq > 4.0D && distSq < 36.0D && this.random.nextInt(15) == 0) {
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
    }

    /**
     * Sets the synched form + the form's attributes (beast 40HP/7atk/0.38spd vs human 15/1/0.25). When
     * {@code adjustHealth} is true (a fresh transform or spawn) the health is snapped to the new range;
     * on a NBT load it is false so the saved health is preserved.
     */
    private void applyFormAttributes(boolean adjustHealth) {
        setHumanForm(!nightForm);
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        var atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (hp != null) hp.setBaseValue(nightForm ? 40.0D : 15.0D);
        if (atk != null) atk.setBaseValue(nightForm ? 7.0D : 1.0D);
        if (spd != null) spd.setBaseValue(nightForm ? 0.38D : 0.25D);
        if (adjustHealth) {
            if (nightForm) {
                this.setHealth(this.getMaxHealth());
            } else if (this.getHealth() > 15.0F) {
                this.setHealth(15.0F);
            }
        }
    }

    private void applyForm(net.minecraft.server.level.ServerLevel level) {
        // Transform: set the form + attributes (healing into the new range) with the poof + grunt cue.
        applyFormAttributes(true);
        this.setTarget(null);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                20, 0.4D, 0.6D, 0.4D, 0.02D);
        level.playSound(null, this.blockPosition(), MoCSounds.WEREWOLFGRUNT.get(),
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, nightForm ? 0.8F : 1.2F);
    }

    @Override
    public net.minecraft.world.entity.@Nullable SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.world.entity.@Nullable SpawnGroupData groupData) {
        net.minecraft.world.entity.SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        // Start in the form matching the time of day so a night-spawned werewolf actually has beast stats
        // (otherwise applyForm never ran until a full day/night cycle — it looked like a beast but hit like a human).
        this.nightForm = !level.getLevel().isBrightOutside();
        applyFormAttributes(true);
        return data;
    }
}
