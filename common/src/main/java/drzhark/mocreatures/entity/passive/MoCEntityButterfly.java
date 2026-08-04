package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCFlyingInsect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Port of the legacy {@code MoCEntityButterfly}. A small flying insect with ten butterfly/moth
 * variants.
 *
 * <p>Legacy flavour restored here:
 * <ul>
 *   <li>Per-type render size ({@link #getSizeFactor()}): the seven butterflies (types 1-7) render
 *       at 0.7x, the three moths (types 8-10) at full size, each moth with a subtle spread so the
 *       night fliers aren't uniform (legacy had a flat 0.7f/1.0f split with no moth variation).</li>
 *   <li>Light attraction ({@code isAttractedToLight() = getType() > 7}): the moth variants are drawn
 *       toward nearby block-light sources (torches, glowstone, lanterns, fireflies) at night,
 *       mirroring the legacy behaviour where moths swarmed light after dark.</li>
 * </ul>
 */
public class MoCEntityButterfly extends MoCFlyingInsect {

    /** Throttles the (moderately expensive) nearby-light scan to a few times a second. */
    private int lightSeekCooldown;

    public MoCEntityButterfly(EntityType<? extends MoCEntityButterfly> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.50D);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(this.random.nextInt(10) + 1);
        }
    }

    /**
     * Legacy {@code isAttractedToLight()}: only the moth variants (types 8-10) chase light.
     */
    private boolean isAttractedToLight() {
        return getTypeMoC() > 7;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Moths swarm light after dark (legacy isAttractedToLight). Steer toward the brightest
        // nearby block-light source within ~8 blocks. Butterflies (types 1-7) ignore this.
        if (isAttractedToLight() && !level.isBrightOutside()) {
            if (--this.lightSeekCooldown <= 0) {
                this.lightSeekCooldown = 10;
                BlockPos target = findNearbyLight(level, 8);
                if (target != null) {
                    this.getMoveControl().setWantedPosition(
                            target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                }
            }
        }
    }

    /**
     * Scans a cube around the butterfly for the block emitting the most block-light (a torch,
     * lantern, glowstone, etc.), preferring the brightest and, on ties, the closest. Returns
     * {@code null} if nothing lit is nearby.
     */
    private BlockPos findNearbyLight(ServerLevel level, int range) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        int bestLight = 8; // ignore ambient gloom; only chase genuine light sources
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    int light = level.getBrightness(LightLayer.BLOCK, cursor);
                    if (light < bestLight) {
                        continue;
                    }
                    double distSq = origin.distSqr(cursor);
                    if (light > bestLight || distSq < bestDistSq) {
                        bestLight = light;
                        bestDistSq = distSq;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    /**
     * Per-type render size (legacy {@code getSizeFactor}): butterflies (types 1-7) at 0.7x, moths
     * (types 8-10) at full size. Each moth gets a subtle spread so the three night fliers read as
     * distinct sizes rather than identical.
     */
    @Override
    public float getSizeFactor() {
        return switch (getTypeMoC()) {
            case 8 -> 0.95F; // mothcamptogrammabilineata
            case 9 -> 1.05F; // mothidiaaemula
            case 10 -> 1.0F; // moththyatirabatis
            default -> 0.7F; // butterflies (types 1-7)
        };
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("bfagalaisurticae.png");
            case 2 -> modelTexture("bfargyreushyperbius.png");
            case 3 -> modelTexture("bfathymanefte.png");
            case 4 -> modelTexture("bfcatopsiliapomona.png");
            case 5 -> modelTexture("bfmorphopeleides.png");
            case 6 -> modelTexture("bfvanessaatalanta.png");
            case 7 -> modelTexture("bfpierisrapae.png");
            case 8 -> modelTexture("mothcamptogrammabilineata.png");
            case 9 -> modelTexture("mothidiaaemula.png");
            case 10 -> modelTexture("moththyatirabatis.png");
            default -> modelTexture("bfpierisrapae.png");
        };
    }
}
