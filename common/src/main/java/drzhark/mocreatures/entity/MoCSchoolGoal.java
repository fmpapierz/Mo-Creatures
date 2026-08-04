package drzhark.mocreatures.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight "swim toward nearby fish of the same type" goal for Mo'Creatures fish
 * (e.g. {@code MoCEntityFishy}).
 *
 * <p>Vanilla's {@code FollowFlockLeaderGoal} only accepts {@code AbstractSchoolingFish}, so this
 * custom goal reproduces a simple schooling feel for arbitrary {@link PathfinderMob} water animals:
 * every so often it scans for living neighbours of the exact same class within a small radius,
 * computes their average position, and — if this fish has drifted too far from that centre — paths
 * back toward the group. Lone or edge fish therefore rejoin, while fish already inside the school
 * are left alone.</p>
 *
 * <p>The scan is throttled by a decrementing cooldown ({@link #SCAN_MIN}..{@link #SCAN_MAX} ticks)
 * so it never runs the neighbour lookup every tick.</p>
 */
public class MoCSchoolGoal extends Goal {

    /** Minimum ticks between neighbour scans. */
    private static final int SCAN_MIN = 40;
    /** Extra random jitter (0..SCAN_JITTER-1) added to the minimum, giving a 40..80 tick interval. */
    private static final int SCAN_JITTER = 41;
    /** Radius (blocks) around the fish searched for same-type neighbours. */
    private static final double RADIUS = 8.0D;
    /** How far (blocks) the fish must be from the school centre before it bothers to rejoin. */
    private static final double REJOIN_DISTANCE = 3.0D;
    private static final double REJOIN_DISTANCE_SQR = REJOIN_DISTANCE * REJOIN_DISTANCE;
    /** Path speed modifier used when swimming back toward the group. */
    private static final double SPEED = 1.0D;

    private final PathfinderMob fish;

    /** Decrementing cooldown so we don't scan for neighbours every tick. */
    private int nextScan;

    /** Stored school centre (average neighbour position) chosen in {@link #canUse()}. */
    private double centreX;
    private double centreY;
    private double centreZ;
    private boolean hasCentre;

    public MoCSchoolGoal(PathfinderMob fish) {
        this.fish = fish;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Throttle: only actually scan once the cooldown elapses.
        if (this.nextScan > 0) {
            this.nextScan--;
            return false;
        }
        this.nextScan = SCAN_MIN + this.fish.getRandom().nextInt(SCAN_JITTER);

        List<? extends Entity> neighbours = this.fish.level().getEntitiesOfClass(
                this.fish.getClass(),
                this.fish.getBoundingBox().inflate(RADIUS),
                e -> e != this.fish && e.isAlive());

        // Need at least two other fish to form a school worth joining.
        if (neighbours.size() < 2) {
            return false;
        }

        double sumX = 0.0D;
        double sumY = 0.0D;
        double sumZ = 0.0D;
        for (Entity e : neighbours) {
            sumX += e.getX();
            sumY += e.getY();
            sumZ += e.getZ();
        }
        int count = neighbours.size();
        Vec3 centre = new Vec3(sumX / count, sumY / count, sumZ / count);
        this.centreX = centre.x;
        this.centreY = centre.y;
        this.centreZ = centre.z;
        this.hasCentre = true;

        // Only rejoin if we've drifted farther than REJOIN_DISTANCE from the group's centre.
        return centre.distanceToSqr(this.fish.getX(), this.fish.getY(), this.fish.getZ()) > REJOIN_DISTANCE_SQR;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.fish.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (this.hasCentre) {
            this.fish.getNavigation().moveTo(this.centreX, this.centreY, this.centreZ, SPEED);
        }
    }

    @Override
    public void stop() {
        this.hasCentre = false;
    }
}
