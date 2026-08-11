package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Port of the legacy {@code MoCEntityMediumFish} — the mod's edible mid-sized food fish.
 *
 * <p>Legacy split this one creature across four classes: the shared {@code MoCEntityMediumFish} held every
 * behaviour, and three one-line subclasses ({@code MoCEntitySalmon}, {@code MoCEntityCod},
 * {@code MoCEntityBass}) existed only to pin {@code setType()}, {@code getTexture()} and
 * {@code getEggNumber()}. Following the port's merge convention (see {@link MoCEntityRay}, which covers both
 * the mantaray and the stingray), all three are one registered entity here, told apart by
 * {@link #getTypeMoC()}: 1 salmon, 2 cod, 3 bass — exactly the ids the legacy subclasses set, so the legacy
 * egg ids (70 + type - 1) and {@code fishNames} ordering still line up.</p>
 *
 * <p>The registry id is deliberately {@code mocreatures:medium_fish} rather than per-species ids: 26.2 ships
 * its own {@code minecraft:cod} and {@code minecraft:salmon}, and although the {@code mocreatures} namespace
 * would keep them technically distinct, one merged id avoids two entities in the same world both called
 * "Cod".</p>
 *
 * <p>Behaviour, all from the 12.0.5 source: 8 HP, a skittish 2-block flee from anything bigger than itself
 * (suppressed once tamed), continuous growth from a fingerling to twice its hitbox length, a self-healing
 * tamed fish, and a death drop of one raw cod 70% of the time. A medium fish is <em>not</em> hand-tameable:
 * the only tamed one in legacy came from hatching a medium-fish egg.</p>
 */
public class MoCEntityMediumFish extends MoCAquatic {

    /** Legacy {@code MoCEntitySalmon} — {@code setType(1)}, {@code getEggNumber() == 70}. */
    public static final int TYPE_SALMON = 1;
    /** Legacy {@code MoCEntityCod} — {@code setType(2)}, {@code getEggNumber() == 71}. */
    public static final int TYPE_COD = 2;
    /** Legacy {@code MoCEntityBass} — {@code setType(3)}, {@code getEggNumber() == 72}. */
    public static final int TYPE_BASS = 3;

    /** Legacy {@code getMaxEdad() == 120} (MoCEntityMediumFish:182-184): the age at which the fish matures. */
    private static final int MAX_AGE = 120;

    public MoCEntityMediumFish(EntityType<? extends MoCEntityMediumFish> type, Level level) {
        super(type, level);
        // Legacy ctor (MoCEntityMediumFish:21-25): setSize(0.6F, 0.3F) — the hitbox now lives on the
        // EntityType builder — plus setEdad(30 + rand(70)), i.e. a population spawning anywhere from
        // fingerling (30) to nearly grown (99).
        //
        // The adult flag has to be cleared explicitly: legacy MoCEntityAquatic registered its ADULT
        // datawatcher as FALSE (MoCEntityAquatic:106) so every aquatic began life immature, whereas the
        // port's MoCAquatic defaults it to TRUE. Without this a medium fish would spawn flagged adult, the
        // legacy growth loop (which is gated on !getIsAdult()) would never run, and every fish would be
        // frozen at its spawn size forever.
        setAdult(false);
        setMoCAge(30 + this.random.nextInt(70));
    }

    /**
     * Legacy {@code applyEntityAttributes} (MoCEntityMediumFish:52-57): 8 max health.
     *
     * <p>Legacy also set MOVEMENT_SPEED to 0.5, but every aquatic in the port runs at 1.0 (dolphin, fishy,
     * ray, shark all do) because water drag damps a WaterBoundPathNavigation swim far harder than the legacy
     * hand-rolled move helper did; keeping 0.5 here would leave the medium fish drifting at half the pace of
     * the fishy it shares a pond with. No attack damage: the legacy medium fish never attacks anything.</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy initEntityAI (MoCEntityMediumFish:41-50) task 3:
        //   new EntityAIFleeFromEntityMoC(this, e -> e.height > 0.6F && e.width > 0.3F, 2.0F, 0.6D, 1.5D)
        // — the fish bolts from anything physically bigger than itself (players, squid, dolphins, sharks,
        // any land animal that wades in) but ignores its own kind and other minnows, since a medium fish is
        // 0.6 wide x 0.3 tall and so fails its own predicate. The trigger radius really is only 2 blocks:
        // legacy let you get right up to a fish before it darted. Walk 0.6 / sprint 1.5 speeds are legacy's.
        //
        // Legacy isNotScared() == getIsTamed() (MoCEntityMediumFish:186-189) is what turns the flee off: a
        // tamed fish stops running from its owner, so both canUse and canContinueToUse gate on it. The type
        // is not known until selectType()/finalizeSpawn runs, but tameness can change at any time, hence the
        // live check rather than a constructor-time decision.
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, LivingEntity.class,
                other -> other.getBbHeight() > 0.6F && other.getBbWidth() > 0.3F,
                2.0F, 0.6D, 1.5D, EntitySelector.NO_CREATIVE_OR_SPECTATOR) {
            @Override
            public boolean canUse() {
                return !getIsTamed() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !getIsTamed() && super.canContinueToUse();
            }
        });
        // Legacy task 5 was EntityAIWanderMoC2(this, 1.0D, 50). MoCAquatic.registerGoals already installs
        // RandomSwimmingGoal(this, 1.0D, 10) at priority 4, which is the same "pick a random reachable point
        // and swim there" wander at the same speed with a shorter re-roll interval, so it is not duplicated
        // here — a second wander goal at a lower priority would simply never win the MOVE flag.
    }

    /**
     * Legacy per-tick server logic, from {@code MoCEntityMediumFish.onLivingUpdate}:83-96 and the shared
     * growth loop in {@code MoCEntityAquatic.onLivingUpdate}:410-416.
     *
     * <p>Growth is implemented here rather than declared in {@code MoCBehavior}'s growth table because
     * {@code MoCAquatic} never calls {@code MoCBehavior.tickGrowth} (only {@code MoCAnimal} does), so a
     * {@code grow(...)} entry in the spec would be inert. {@code MoCEntityShark} solves it the same way.</p>
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy MoCEntityAquatic:410 — "fixes tiny creatures spawned by error". An age of 0 would render
        // the fish at scale 0 (see getSizeFactor), so a fish that somehow arrives ageless is snapped to
        // nearly grown instead of being invisible.
        if (getMoCAge() == 0) {
            setMoCAge(MAX_AGE - 10);
        }
        // Legacy MoCEntityAquatic:411-416: a non-adult ages on a 1-in-300 tick roll and matures at
        // getMaxEdad(), which the medium fish raises from the aquatic default of 100 to 120.
        if (!getIsAdult() && this.random.nextInt(300) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= MAX_AGE) {
                setAdult(true);
            }
        }
        // Legacy MoCEntityMediumFish:87-91: a TAMED fish repairs itself — a 1-in-100 tick roll restores it
        // to full health — so a pet fish knocked about in its bowl or by a passing mob recovers on its own.
        // Wild fish get no such regeneration.
        if (getIsTamed() && this.random.nextInt(100) == 0 && getHealth() < getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    /**
     * Legacy {@code MoCEntityMediumFish.onLivingUpdate}:92-95 — out of water the fish freezes its facing
     * ({@code prevRenderYawOffset = renderYawOffset = rotationYaw = prevRotationYaw} and
     * {@code rotationPitch = prevRotationPitch}). A beached fish is rendered rolled onto its side, and
     * without this pin it would keep swivelling on the sand as the (now unreachable) navigation target
     * dragged its yaw around. Also stands in for the legacy {@code isMovementCeased() == !isInWater()}:
     * the port's WaterBoundPathNavigation cannot path on land anyway, so a stranded fish simply stops.
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.isInWater()) {
            this.setYRot(this.yRotO);
            this.setXRot(this.xRotO);
            this.yBodyRot = this.yBodyRotO;
            this.yHeadRot = this.yHeadRotO;
        }
    }

    /**
     * Legacy {@code selectType} (MoCEntityMediumFish:59-64): {@code rand.nextInt(fishNames.length) + 1},
     * i.e. a flat one-in-three between salmon, cod and bass. Legacy reached the same result through three
     * separate registered entities each with its own spawn entry of identical weight.
     */
    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(this.random.nextInt(3) + 1);
        }
    }

    /**
     * Legacy death drop ({@code dropFewItems}, MoCEntityMediumFish:66-77): a 100-sided roll, under 70 drops
     * exactly one raw fish, otherwise {@code rand.nextInt(2)} = 0-or-1 species egg. The two are mutually
     * exclusive, so ~15% of kills leave an egg and ~15% leave nothing at all. {@code Items.FISH} with
     * metadata 0 is 26.2's {@code Items.COD}.
     *
     * <p>CROSS-FILE need: the egg half cannot fire yet. The port's egg system only knows the composite ids
     * legacy assigned to species that were already ported — {@code MoCEntityEgg.setEggType} maps 1-11, 21-33,
     * 41-45 and 50-54, and anything else falls through to "ostrich". Handing it 70/71/72 today would drop an
     * item named "Spoiled Egg" that hatches an ostrich out of a fish, so the egg branch deliberately drops
     * nothing until {@code MoCEntityEgg} gains a {@code TYPE_MEDIUM_FISH} mapping for 70-72 (plus the
     * matching {@code getEggType} re-encode, {@code MoCThrownEggItem.eggColour}/{@code hasOwnName} entries
     * and three {@code item.mocreatures.mocegg.7x} lang keys). {@link #eggNumber()} below already returns the
     * right legacy id for whoever wires that up. Note this is also the ONLY legacy route to a tamed medium
     * fish, since the species has no feed-to-tame interaction.</p>
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        // Server-admin loot suppression, mirroring the guard at the top of MoCBehavior.dropLoot. Only the
        // blanket flag applies: destroyPassiveDrops is scoped to MoCAnimal, and a medium fish is a MoCAquatic.
        if (drzhark.mocreatures.config.MoCConfig.get().destroyDrops) {
            return;
        }
        if (this.random.nextInt(100) < 70) {
            spawnAtLocation(level, new ItemStack(Items.COD, 1));
        }
    }

    /**
     * The legacy composite egg id for this fish's species: {@code MoCEntitySalmon.getEggNumber() == 70},
     * {@code MoCEntityCod == 71}, {@code MoCEntityBass == 72} — the block {@code MoCItemEgg} reserved as
     * {@code 70 .. 70 + fishNames.length}. Kept (and kept correct) for the egg wiring described above.
     */
    public int eggNumber() {
        int type = getTypeMoC();
        return 69 + (type >= TYPE_SALMON && type <= TYPE_BASS ? type : TYPE_SALMON);
    }

    /**
     * Legacy {@code getSizeFactor() == getEdad() * 0.01F} (MoCEntityMediumFish:98-101). A medium fish renders
     * at exactly its age as a percentage: freshly spawned (age 30-99) it is 0.30x-0.99x, and fully grown
     * (age 120) it is 1.20x. This is load-bearing rather than cosmetic here — the model is roughly 26 px
     * (1.6 blocks) from nose to tail fin, so a flat 1.0x would put a two-block fish in the water the instant
     * it spawned, and the visible fingerling-to-adult growth is the whole point of the 30-120 age range.
     *
     * <p>{@code MoCMobRenderer.scale} multiplies this factor by the port's shared age curve
     * ({@code adult ? 1 : 0.5 + 0.5 * min(age,100)/100}), which is the generic stand-in for the per-species
     * legacy curves. Dividing that curve back out here reproduces the legacy factor exactly without touching
     * the shared renderer every other creature uses. The divisor is never zero (it bottoms out at 0.5).</p>
     */
    @Override
    public float getSizeFactor() {
        float legacy = getMoCAge() * 0.01F;
        float sharedCurve = getIsAdult() ? 1.0F : 0.5F + 0.5F * Math.min(getMoCAge(), 100) / 100.0F;
        return legacy / sharedCurve;
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case TYPE_COD -> modelTexture("mediumfish_cod.png");
            case TYPE_BASS -> modelTexture("mediumfish_bass.png");
            // Legacy createEntity() also fell back to the salmon for any unrecognised type.
            default -> modelTexture("mediumfish_salmon.png");
        };
    }
}
