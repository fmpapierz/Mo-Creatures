package drzhark.mocreatures.entity;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;

/** Keeps a Mo'Creatures creature stationary while it is sitting (toggled by the whip). */
public class MoCSitGoal extends Goal {

    private final MoCAnimal mob;

    public MoCSitGoal(MoCAnimal mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return mob.isSitting() && !mob.isVehicle() && !mob.isPassenger();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isSitting();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        mob.setJumping(false);
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
    }
}
