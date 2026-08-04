package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import net.minecraft.resources.Identifier;
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

    public MoCEntityFishy(EntityType<? extends MoCEntityFishy> type, Level level) {
        super(type, level);
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
