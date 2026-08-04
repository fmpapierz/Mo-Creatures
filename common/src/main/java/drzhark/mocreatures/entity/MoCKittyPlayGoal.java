package drzhark.mocreatures.entity;

import java.util.EnumSet;
import java.util.List;

import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A tamed kitty chases and bats a nearby wool ball (dropped by the Wool Ball item). */
public class MoCKittyPlayGoal extends Goal {

    private final MoCAnimal kitty;
    private ItemEntity ball;
    private int playTime;

    public MoCKittyPlayGoal(MoCAnimal kitty) {
        this.kitty = kitty;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private ItemEntity findBall() {
        AABB box = kitty.getBoundingBox().inflate(8.0D);
        List<ItemEntity> items = kitty.level().getEntitiesOfClass(ItemEntity.class, box,
                e -> e.isAlive() && e.getItem().is(MoCItems.WOOLBALL.get()));
        ItemEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ItemEntity e : items) {
            double d = kitty.distanceToSqr(e);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        return nearest;
    }

    @Override
    public boolean canUse() {
        if (!kitty.getIsTamed() || kitty.isVehicle() || kitty.isPassenger()) {
            return false;
        }
        this.ball = findBall();
        return ball != null;
    }

    @Override
    public boolean canContinueToUse() {
        return ball != null && ball.isAlive() && kitty.getIsTamed() && playTime < 300;
    }

    @Override
    public void start() {
        this.playTime = 0;
    }

    @Override
    public void stop() {
        this.ball = null;
        this.playTime = 0;
        kitty.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (ball == null) {
            return;
        }
        playTime++;
        kitty.getLookControl().setLookAt(ball, 30.0F, 30.0F);
        if (kitty.distanceToSqr(ball) > 1.5D) {
            kitty.getNavigation().moveTo(ball, 1.2D);
        } else {
            kitty.getNavigation().stop();
            if (kitty.getRandom().nextInt(8) == 0) {
                Vec3 dir = ball.position().subtract(kitty.position()).normalize();
                ball.setDeltaMovement(dir.x * 0.3D, 0.25D, dir.z * 0.3D);
                if (kitty.onGround()) {
                    kitty.setDeltaMovement(kitty.getDeltaMovement().x, 0.3D, kitty.getDeltaMovement().z);
                }
            }
        }
    }
}
