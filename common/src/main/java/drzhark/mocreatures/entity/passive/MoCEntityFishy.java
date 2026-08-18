package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;

/**
 * Port of the legacy {@code MoCEntityFishy}. A small schooling fish with ten colour variants
 * (the tenth being a piranha).
 */
public class MoCEntityFishy extends MoCAquatic {

    /**
     * Legacy {@code hasEaten} (datawatcher 22): a fed fishy is ready to breed. Note that legacy 5.1.5 defined
     * the flag and the breeding loop that reads it but never set it anywhere, so fishy breeding was
     * unreachable dead code in that build; feeding a tamed adult its heal food supplies the missing step.
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HAS_EATEN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(MoCEntityFishy.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public MoCEntityFishy(EntityType<? extends MoCEntityFishy> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_EATEN, false);
    }

    public boolean getHasEaten() {
        return this.entityData.get(HAS_EATEN);
    }

    public void setHasEaten(boolean eaten) {
        this.entityData.set(HAS_EATEN, eaten);
    }

    /**
     * Feeding a fishy — the step that makes the breeding loop further down reachable at all.
     *
     * <p>Legacy 5.1.5 declared {@code hasEaten} (datawatcher 22) and read it in {@code ReadyforParenting}
     * ({@code MoCEntityFishy}:326-328), but never called {@code setHasEaten(true)} anywhere — the only calls
     * are the two that CLEAR it after a litter is born ({@code MoCEntityFishy}:302-303), and upstream 12.0.5
     * still only clears it ({@code MoCEntityFishy}:180-181). Fishy breeding was therefore unreachable dead
     * code in both builds. The port supplies the missing feed step, using the fishy's heal food
     * ({@code MoCBehavior.reg("fishy").heal(COD, COOKED_COD)}) as what it eats.</p>
     *
     * <p><b>Why this cannot simply delegate to {@link MoCAquatic#mobInteract}.</b> The shared aquatic feed
     * branch ({@code MoCAquatic}:107) is gated on {@code getHealth() < getMaxHealth()} — it exists to heal a
     * hurt dolphin, not to feed a healthy pet. A full-health fishy consequently matched NOTHING: fishy
     * declares no attraction {@code foods} (its spec only sets {@code heal} foods), so the
     * swallow-the-food branch at {@code MoCAquatic}:123 missed as well and the call fell through to
     * {@code Mob.mobInteract}'s {@code PASS}. Two things followed from that PASS. First, {@code HasEaten}
     * could never be set, because the old code here only set it when the super call reported
     * {@code consumesAction()} — so breeding stayed as unreachable as it was in 5.1.5. Second, on a
     * non-Success result the client's {@code Minecraft.startUseItem} ENTITY case breaks out of its switch
     * and goes on to USE the held item, so right-clicking a fishy with raw cod fed the player instead of
     * the fish. That is the reported "can't feed raw cod to fishy".</p>
     *
     * <p>So a TAMED ADULT fishy now eats at ANY health, handled here ahead of the super call. This mirrors
     * {@code MoCAnimal}:445-460, where the big cat is special-cased ahead of the same hurt-only heal branch
     * for exactly this reason (legacy {@code MoCEntityBigCat.interact} fed a cat at full health too). Wild
     * fish and fry are not touched and still fall through to the shared aquatic behaviour, so the dolphin,
     * ray, shark and jellyfish are unaffected.</p>
     */
    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player,
            net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (getIsTamed() && getIsAdult() && drzhark.mocreatures.entity.MoCBehavior.matches(
                drzhark.mocreatures.entity.MoCBehavior.of(this).healOrFood(), stack)) {
            if (!this.level().isClientSide()) {
                // Nothing to gain from a second helping: an already-fed fishy at full health still accepts
                // the click (so the player does not end up eating their own cod, see above) but keeps the
                // fish. Feeding is otherwise a normal consume-one heal, as everywhere else in the mod.
                final boolean gains = !getHasEaten() || getHealth() < getMaxHealth();
                if (gains) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    heal(fishHealAmount(stack));
                    setHasEaten(true);
                    hearts(4);
                }
                this.level().playSound(null, blockPosition(),
                        drzhark.mocreatures.registry.MoCSounds.EATING.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Feeding feedback. {@code MoCAquatic.aquaHearts()} and {@code MoCAnimal.hearts(int)} are both private to
     * their own class, so the same six-line heart burst is repeated here rather than widening either.
     */
    private void hearts(int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(),
                    getY() + getBbHeight() * 0.5D, getZ(), count, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasEaten", getHasEaten());
        output.putInt("GestationTime", this.gestationTime);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        setHasEaten(input.getBooleanOr("HasEaten", false));
        this.gestationTime = input.getIntOr("GestationTime", 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                // Piranha (type 10) bite — legacy dealt 1 point of damage per hit. Attributes are shared
                // across all variants, but only type-10 fishy actually run the attack goals below.
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Simple schooling: rejoin nearby fish of the same type.
        this.goalSelector.addGoal(3, new drzhark.mocreatures.entity.MoCSchoolGoal(this));

        // Piranha (legacy type 10): an aggressive fish that attacks players and small water mobs in
        // water. Only type-10 fishy behave this way; all other variants stay passive schoolers. Because
        // the type isn't known until selectType()/finalizeSpawn runs, every goal below gates on
        // getTypeMoC()==10 in its canUse (mirroring the legacy findPlayerToAttack difficulty/adult/tamed
        // guards) so the same goals are inert on the peaceful colour variants.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.4D, true) {
            @Override
            public boolean canUse() {
                return isPiranhaHostile() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isPiranhaHostile() && super.canContinueToUse();
            }
        });
        // Hunt vulnerable players who are in the water (legacy: closest vulnerable player within 16, in water).
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.player.Player.class, 16, true, false,
                (living, serverLevel) -> isPiranhaHostile() && living.isInWater()) {
            @Override
            public boolean canUse() {
                return isPiranhaHostile() && super.canUse();
            }
        });
        // Also snap at small water mobs (vanilla fish, squid, etc.), but never other Mo'Creatures aquatics —
        // matching the legacy FindTarget exclusion of MoCEntityAquatic / eggs / players.
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.animal.fish.WaterAnimal.class, 16, true, false,
                (living, serverLevel) -> isPiranhaHostile()
                        && !(living instanceof drzhark.mocreatures.entity.MoCAquatic)
                        && living.isInWater()) {
            @Override
            public boolean canUse() {
                return isPiranhaHostile() && super.canUse();
            }
        });
    }

    /**
     * Legacy piranha aggression gate: only a wild (untamed) adult type-10 fishy hunts, and only when the
     * world difficulty is above Peaceful (mirrors the legacy {@code findPlayerToAttack} guards:
     * {@code difficultySetting > 0 && edad >= 100 && type == 10 && !isTamed}).
     */
    private boolean isPiranhaHostile() {
        return getTypeMoC() == 10
                && getIsAdult()
                && !getIsTamed()
                && !this.level().getDifficulty().equals(net.minecraft.world.Difficulty.PEACEFUL);
    }

    /** Legacy {@code MoCEntityFishy.isFisheable()}:224-226 — a wild fishy will bite a fishing rod's bobber. */
    @Override
    protected boolean isFisheable() {
        return !getIsTamed();
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 9) {
                setTypeMoC(1);
            } else if (i <= 19) {
                setTypeMoC(2);
            } else if (i <= 29) {
                setTypeMoC(3);
            } else if (i <= 39) {
                setTypeMoC(4);
            } else if (i <= 49) {
                setTypeMoC(5);
            } else if (i <= 59) {
                setTypeMoC(6);
            } else if (i <= 69) {
                setTypeMoC(7);
            } else if (i <= 79) {
                setTypeMoC(8);
            } else if (i <= 89) {
                setTypeMoC(9);
            } else {
                setTypeMoC(10);
            }
            // Piranhas (type 10) only remain when spawnPiranhas is enabled; otherwise demote to a blue fishy (legacy).
            if (getTypeMoC() == 10 && !drzhark.mocreatures.config.MoCConfig.get().spawnPiranhas) {
                setTypeMoC(1);
            }
        }
    }

    /**
     * Legacy fishy breeding ({@code MoCEntityFishy}:261-320). Two TAMED, ADULT fish of the SAME colour that
     * have both been fed carry a gestation timer; once it fills, a litter of 1-3 fry is born, each tamed to
     * the nearest player within 24 blocks and inheriting the parents' colour. Crowding blocks it: legacy
     * bailed out when more than one other fishy was already in a 4x3x4 box around this one.
     *
     * <p>This is the mechanic that made fishy a breedable, collectable species alongside the fish bowl, and
     * it had no counterpart in the port at all.</p>
     */
    private int gestationTime;

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy growth (MoCEntityFishy.onLivingUpdate:253-260): a fry ages +2 on a 1-in-100 tick and is
        // flagged adult at 100 — precisely the reg("fishy").grow(100, 100, 100).growStep(2) spec. But only
        // MoCAnimal calls MoCBehavior.tickGrowth; MoCAquatic never does, so for an aquatic that spec is inert
        // unless the entity drives it itself (as MoCEntitySmallFish and MoCEntityMediumFish already do).
        // Without this the fry born below (setMoCAge(20), setAdult(false)) stay babies forever, so they can
        // never satisfy readyForParenting and breed in their turn — the loop would only ever close for the
        // founding pair.
        drzhark.mocreatures.entity.MoCBehavior.tickGrowth(this, this.random,
                drzhark.mocreatures.entity.MoCBehavior.of(this));
        if (!readyForParenting(this)) {
            return;
        }
        // Legacy crowding guard: more than one other fishy nearby and nothing happens.
        if (level.getEntitiesOfClass(MoCEntityFishy.class, getBoundingBox().inflate(4.0D, 3.0D, 4.0D),
                f -> f != this).size() > 1) {
            return;
        }
        for (MoCEntityFishy partner : level.getEntitiesOfClass(MoCEntityFishy.class,
                getBoundingBox().inflate(4.0D, 2.0D, 4.0D),
                f -> f != this && f.getTypeMoC() == this.getTypeMoC() && readyForParenting(f))) {
            if (this.random.nextInt(100) == 0) {
                this.gestationTime++;
            }
            if (this.gestationTime <= 50) {
                return;
            }
            Player parent = level.getNearestPlayer(this, 24.0D);
            int litter = this.random.nextInt(3) + 1;
            for (int i = 0; i < litter; i++) {
                MoCEntityFishy fry = drzhark.mocreatures.registry.MoCEntities.FISHY.get()
                        .create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
                if (fry == null) {
                    continue;
                }
                fry.setPos(getX(), getY(), getZ());
                fry.setTypeMoC(getTypeMoC());
                fry.setMoCAge(20);
                fry.setAdult(false);
                if (parent != null && !drzhark.mocreatures.entity.MoCAnimal.exceedsTameCap(fry, parent)) {
                    fry.setTamed(true);
                    fry.setOwnerName(parent.getName().getString());
                }
                level.addFreshEntity(fry);
            }
            level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.CHICKEN_EGG,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                    ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
            setHasEaten(false);
            partner.setHasEaten(false);
            this.gestationTime = 0;
            partner.gestationTime = 0;
            return;
        }
    }

    /** Legacy {@code ReadyforParenting}: tamed, fed and grown. */
    private static boolean readyForParenting(MoCEntityFishy fishy) {
        return fishy.getIsTamed() && fishy.getHasEaten() && fishy.getIsAdult();
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("fishy2.png");
            case 3 -> modelTexture("fishy3.png");
            case 4 -> modelTexture("fishy4.png");
            case 5 -> modelTexture("fishy5.png");
            case 6 -> modelTexture("fishy6.png");
            case 7 -> modelTexture("fishy7.png");
            case 8 -> modelTexture("fishy8.png");
            case 9 -> modelTexture("fishy9.png");
            case 10 -> modelTexture("fishy10.png");
            default -> modelTexture("fishy1.png");
        };
    }
}
