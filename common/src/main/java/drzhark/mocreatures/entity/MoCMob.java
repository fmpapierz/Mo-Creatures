package drzhark.mocreatures.entity;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base class for hostile Mo'Creatures mobs on Minecraft 26.2 / NeoForge — the modern equivalent of
 * the legacy {@code MoCEntityMob}. Carries the same shared Mo'Creatures state as {@link MoCAnimal}
 * (type / tamed / owner / adult / age) but extends vanilla {@link Monster} with standard target +
 * melee AI.
 */
public abstract class MoCMob extends Monster implements IMoCEntity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(MoCMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED =
            SynchedEntityData.defineId(MoCMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ADULT =
            SynchedEntityData.defineId(MoCMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(MoCMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER =
            SynchedEntityData.defineId(MoCMob.class, EntityDataSerializers.STRING);

    protected MoCMob(EntityType<? extends MoCMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
        builder.define(TAMED, false);
        builder.define(ADULT, true);
        builder.define(AGE, 50);
        builder.define(OWNER, "");
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public int getTypeMoC() {
        return this.entityData.get(TYPE);
    }

    @Override
    public void setTypeMoC(int type) {
        this.entityData.set(TYPE, type);
    }

    @Override
    public boolean getIsTamed() {
        return this.entityData.get(TAMED);
    }

    @Override
    public void setTamed(boolean tamed) {
        this.entityData.set(TAMED, tamed);
    }

    @Override
    public boolean getIsAdult() {
        return this.entityData.get(ADULT);
    }

    @Override
    public void setAdult(boolean adult) {
        this.entityData.set(ADULT, adult);
    }

    @Override
    public int getMoCAge() {
        return this.entityData.get(AGE);
    }

    @Override
    public void setMoCAge(int age) {
        this.entityData.set(AGE, age);
    }

    @Override
    public String getOwnerName() {
        return this.entityData.get(OWNER);
    }

    @Override
    public void setOwnerName(String name) {
        this.entityData.set(OWNER, name == null ? "" : name);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TypeMoC", getTypeMoC());
        output.putBoolean("Tamed", getIsTamed());
        output.putBoolean("Adult", getIsAdult());
        output.putInt("AgeMoC", getMoCAge());
        output.putString("OwnerName", getOwnerName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setTypeMoC(input.getIntOr("TypeMoC", getTypeMoC()));
        setTamed(input.getBooleanOr("Tamed", false));
        setAdult(input.getBooleanOr("Adult", true));
        setMoCAge(input.getIntOr("AgeMoC", 50));
        setOwnerName(input.getStringOr("OwnerName", ""));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        selectType();
        return data;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.damagesource.DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        MoCBehavior.dropLoot(this, level, MoCBehavior.of(this));
        // Chance (config monsterEggDropChance %) to drop this monster's own spawn egg on death (legacy
        // monster-egg-on-death). Only when slain by a player, to avoid a farmable trickle.
        int chance = drzhark.mocreatures.config.MoCConfig.get().monsterEggDropChance;
        if (chance > 0 && hitByPlayer && this.random.nextInt(100) < chance) {
            net.minecraft.world.item.SpawnEggItem.byId(this.getType()).ifPresent(
                    holder -> spawnAtLocation(level, new net.minecraft.world.item.ItemStack(holder.value())));
        }
    }

    // ------------------------------------------------------- signature monster behaviours (overridable)

    /** Undead/ethereal mobs that catch fire in daylight under open sky. */
    protected boolean burnsInDaylight() {
        return false;
    }

    /** Applied to a victim on a successful melee hit (poison stings, fiery bites, ...). */
    protected void applyHitEffects(net.minecraft.world.entity.LivingEntity target) {
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof net.minecraft.world.entity.LivingEntity victim) {
            applyHitEffects(victim);
        }
        return hit;
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        if (burnsInDaylight() && !this.isOnFire() && level.isBrightOutside()
                && level.canSeeSky(this.blockPosition()) && !this.isInWaterOrRain()
                && this.random.nextInt(20) == 0) {
            // Legacy burned only on a random proc (rand vs. daylight brightness), not every tick, so
            // undead flare up occasionally rather than continuously re-igniting an 8s fire every tick.
            this.igniteForSeconds(8.0F);
        }
    }

    protected static Identifier modelTexture(String file) {
        return Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "textures/models/" + file);
    }
}
