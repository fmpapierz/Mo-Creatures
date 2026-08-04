package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;

/**
 * Port of the legacy {@code MoCEntityJellyFish}. A small aquatic creature with several colour variants.
 */
public class MoCEntityJellyFish extends MoCAquatic {

    /** Synched bioluminescence flag: a jellyfish self-illuminates only at night (legacy setGlowing(!isDaytime)). */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> GLOWING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    MoCEntityJellyFish.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Ticks remaining before this jellyfish can sting again, to avoid spamming nearby entities. */
    private int stingCooldown;

    public MoCEntityJellyFish(EntityType<? extends MoCEntityJellyFish> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GLOWING, false);
    }

    /** True at night — the client renders the bell as a self-illuminating (emissive) bioluminescent glow. */
    public boolean isGlowingNow() {
        return this.entityData.get(GLOWING);
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        // Glow only at night (legacy setGlowing(!worldObj.isDaytime())); re-checked a few times a second.
        if (this.tickCount % 20 == 0) {
            boolean glow = !level.isBrightOutside();
            if (glow != isGlowingNow()) {
                this.entityData.set(GLOWING, glow);
            }
        }
        if (this.stingCooldown > 0) {
            this.stingCooldown--;
            return;
        }
        // Legacy conditional sting (poisoncounter > 250 && difficultySetting > 0): a jellyfish poisons only
        // nearby PLAYERS, only off Peaceful, only every ~250 ticks, and NEVER a player riding a boat. Legacy
        // additionally required the victim to be IN WATER (entityplayertarget.isInWater()) — a jellyfish cannot
        // sting someone standing dry on a dock even though its 2-block reach touches them. It is a poison touch
        // — no direct damage.
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return;
        }
        java.util.List<net.minecraft.world.entity.player.Player> players = level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class, this.getBoundingBox().inflate(2.0D),
                p -> p.isInWater() && !p.isCreative() && !p.isSpectator()
                        && !(p.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat));
        if (!players.isEmpty()) {
            for (net.minecraft.world.entity.player.Player p : players) {
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.POISON, 120, 0), this);
            }
            this.stingCooldown = 250;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 20) {
                setTypeMoC(1);
            } else if (i <= 40) {
                setTypeMoC(2);
            } else if (i <= 65) {
                setTypeMoC(3);
            } else if (i <= 80) {
                setTypeMoC(4);
            } else {
                setTypeMoC(5);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("jellyfishb.png");
            case 3 -> modelTexture("jellyfishc.png");
            case 4 -> modelTexture("jellyfishd.png");
            case 5 -> modelTexture("jellyfishe.png");
            case 6 -> modelTexture("jellyfishf.png");
            case 7 -> modelTexture("jellyfishg.png");
            case 8 -> modelTexture("jellyfishh.png");
            case 9 -> modelTexture("jellyfishi.png");
            case 10 -> modelTexture("jellyfishj.png");
            case 11 -> modelTexture("jellyfishk.png");
            case 12 -> modelTexture("jellyfishl.png");
            default -> modelTexture("jellyfisha.png");
        };
    }
}
