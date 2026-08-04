package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCFlyingInsect;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityFirefly}. A tiny glowing flying insect.
 */
public class MoCEntityFirefly extends MoCFlyingInsect {

    /**
     * Synched: the firefly has settled and is no longer airborne. Legacy tracked this as an
     * {@code isFlying} flag toggled by {@code onLivingUpdate} (~1/500 chance to land each tick);
     * modelled here as its inverse so the default freshly-spawned state is flying.
     */
    private static final EntityDataAccessor<Boolean> LANDED =
            SynchedEntityData.defineId(MoCEntityFirefly.class, EntityDataSerializers.BOOLEAN);

    /** Countdown between airborne buzz sounds, mirroring the legacy {@code soundCount} (~20 ticks). */
    private int soundCount;

    public MoCEntityFirefly(EntityType<? extends MoCEntityFirefly> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.30D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LANDED, false);
    }

    /** Whether the firefly has settled on the ground (inverse of the legacy {@code isFlying} flag). */
    public boolean getLanded() {
        return this.entityData.get(LANDED);
    }

    public void setLanded(boolean landed) {
        this.entityData.set(LANDED, landed);
    }

    /** Server-side dwell timer: ticks a landed firefly must stay settled before it may take off again. */
    private int landedTicks;

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (!getLanded()) {
            // While airborne and a player is nearby, buzz the cricketfly loop every ~20 ticks.
            Player near = level.getNearestPlayer(this, 8.0D);
            if (near != null && --this.soundCount <= 0) {
                level.playSound(null, this.blockPosition(), MoCSounds.CRICKETFLY.get(),
                        SoundSource.NEUTRAL, 1.0F, 1.0F);
                this.soundCount = 20;
            }

            // Legacy onLivingUpdate: while flying, a rare ~1/500 chance to settle down and land — and then it
            // STAYS put for a while (the flight nav is halted) rather than instantly relaunching.
            if (this.random.nextInt(500) == 0) {
                setLanded(true);
                this.landedTicks = 80 + this.random.nextInt(160); // rest ~4-12s
                this.getNavigation().stop();
            }
        } else {
            // Settled: keep the flight navigation stopped so it doesn't drift, then take off once the dwell
            // timer expires (a single clean transition — no per-tick land/take-off oscillation).
            this.getNavigation().stop();
            if (--this.landedTicks <= 0) {
                setLanded(false);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Landed", getLanded());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setLanded(in.getBooleanOr("Landed", false));
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("firefly.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.FIREFLY.get();
    }
}
