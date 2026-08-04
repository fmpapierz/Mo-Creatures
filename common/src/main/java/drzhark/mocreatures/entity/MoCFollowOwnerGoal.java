package drzhark.mocreatures.entity;

import java.util.EnumSet;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;

/**
 * Makes a tamed Mo'Creatures creature follow its owner, like the legacy mod. The owner is matched by
 * the stored owner name (Mo'Creatures tracks ownership by name). Skips while ridden, sitting on a
 * vehicle, or being carried.
 */
public class MoCFollowOwnerGoal extends Goal {

    private final PathfinderMob mob;
    private final IMoCEntity moc;
    private final double speed;
    private final double startDistSqr;
    private final double stopDistSqr;
    private final double teleportDistSqr;
    private final PathNavigation nav;
    private Player owner;
    private int recalc;

    public MoCFollowOwnerGoal(PathfinderMob mob, double speed, float startDist, float stopDist) {
        this.mob = mob;
        this.moc = (IMoCEntity) mob;
        this.speed = speed;
        this.startDistSqr = startDist * startDist;
        this.stopDistSqr = stopDist * stopDist;
        this.teleportDistSqr = 24.0D * 24.0D;
        this.nav = mob.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private Player findOwner() {
        String name = moc.getOwnerName();
        if (name == null || name.isEmpty()) {
            return null;
        }
        Player nearest = mob.level().getNearestPlayer(mob, 32.0D);
        if (nearest != null && nearest.getName().getString().equals(name)) {
            return nearest;
        }
        for (Player p : mob.level().players()) {
            if (p.getName().getString().equals(name) && mob.distanceToSqr(p) < 1024.0D) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean canUse() {
        if (!moc.getIsTamed() || mob.isVehicle() || mob.isPassenger()) {
            return false;
        }
        Player p = findOwner();
        if (p == null || p.isSpectator()) {
            return false;
        }
        if (mob.distanceToSqr(p) < startDistSqr) {
            return false;
        }
        this.owner = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && moc.getIsTamed() && !mob.isVehicle() && !mob.isPassenger()
                && mob.distanceToSqr(owner) > stopDistSqr;
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void stop() {
        owner = null;
        nav.stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
        if (--recalc <= 0) {
            recalc = 10;
            if (mob.distanceToSqr(owner) > teleportDistSqr && !mob.isLeashed()) {
                mob.snapTo(owner.getX(), owner.getY(), owner.getZ(), mob.getYRot(), mob.getXRot());
                nav.stop();
            } else {
                nav.moveTo(owner, speed);
            }
        }
    }
}
