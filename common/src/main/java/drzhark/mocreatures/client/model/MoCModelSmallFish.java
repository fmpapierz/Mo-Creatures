package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.Set;

/**
 * Small fish model, converted faithfully from the legacy {@code MoCModelSmallFish} ({@code ModelBase}).
 * One mesh serves all eight species (anchovy / angelfish / angler / clownfish / goldfish / hippotang /
 * manderin / piranha) — legacy rendered every part for every one of them and told them apart purely by
 * texture ({@code MoCModelSmallFish.render}:80-92 has no per-type branches at all), so this model has no
 * per-type part visibility either.
 *
 * <p><b>Why the two wrapper parts.</b> The legacy mesh is laid out along the <em>X</em> axis — the head end
 * (BodyRomboid) sits at x -4 and the tail at x +1.3 — which is why the legacy entity returned
 * {@code yawRotationOffset() == 90} and the renderer spun the whole model with
 * {@code glRotatef(f, 0, -1, 0)} ({@code MoCRenderMoC.adjustYaw}:148-153) before drawing it. 26.2 models are
 * built facing -Z, so that same -90 degree yaw lives on the {@code fish} wrapper here. Nested inside it,
 * {@code offset} carries the legacy {@code glTranslatef(xOffset, yOffset, zOffset)} that
 * {@code MoCModelSmallFish.render} applied around the part list — the fish body is drawn a little below the
 * entity origin so it sits centred in its 0.3-block hitbox rather than hovering above it. Nesting reproduces
 * the legacy matrix order exactly (rotate, then translate, then draw the parts).</p>
 *
 * <p>The remaining legacy transform, the {@code rollRotationOffset() == -90} that lays a beached fish on its
 * side, is applied one level further out in {@code MoCSmallFishRenderer.setupRotations} — it has to pivot
 * about the entity's own origin, which is above this model's root, so it cannot live on a part.</p>
 *
 * <p>Texture size stays 32x32 as legacy declared it even though the shipped PNGs are 64x64: the sheets are
 * 2x upscales of a 32x32 layout, so the UVs are only correct against the legacy 32x32 declaration.</p>
 */
public class MoCModelSmallFish extends EntityModel<MoCEntityRenderState> {

    /** Legacy -45 degree roll that turns the flat body/tail quads into diamonds ({@code -0.7853982F}). */
    private static final float DIAMOND = -0.7853982F;
    /** Legacy MidBodyFin base yaw, +45 degrees ({@code 0.7853982F}); the fin swings around this. */
    private static final float MID_FIN_YAW = 0.7853982F;
    /** Legacy {@code getAdjustedYOffset()} while swimming: 0.3 blocks -> 4.8 model units (model +Y is down). */
    private static final float IN_WATER_Y = 4.8F;
    /** Legacy {@code getAdjustedYOffset()} out of water: 0.5 blocks, which drops the body flat onto the ground. */
    private static final float BEACHED_Y = 8.0F;
    /** Legacy {@code getAdjustedZOffset()} out of water: 0.1 blocks; 0 while swimming. */
    private static final float BEACHED_Z = 1.6F;
    /** Legacy in-water yaw sway amplitude in degrees ({@code MoCEntityAquatic.yawRotationOffset}:995-1001). */
    private static final float SWAY_DEGREES = 8.0F;

    // The pelvic plane is split into two single-face boxes (painted DOWN face + a re-aimed UP face sampling
    // the same painted tile) so the culled render type keeps it visible from both sides.
    private static final Set<Direction> DOWN_ONLY = Set.of(Direction.DOWN);
    private static final Set<Direction> UP_ONLY = Set.of(Direction.UP);

    private final ModelPart fish;
    private final ModelPart offset;
    private final ModelPart midBodyFin;
    private final ModelPart lowerFinB;
    private final ModelPart tail;

    public MoCModelSmallFish(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.fish = root.getChild("fish");
        this.offset = this.fish.getChild("offset");
        this.midBodyFin = this.offset.getChild("mid_body_fin");
        this.lowerFinB = this.offset.getChild("lower_fin_b");
        this.tail = this.offset.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Legacy yawRotationOffset() == 90, applied by MoCRenderMoC as glRotatef(90, 0, -1, 0) — a -90 degree
        // turn about +Y, which swings the X-aligned legacy mesh onto 26.2's -Z facing.
        PartDefinition fish = root.addOrReplaceChild("fish", CubeListBuilder.create(),
                PartPose.rotation(0.0F, -Mth.HALF_PI, 0.0F));
        // Legacy MoCModelSmallFish.render's glTranslatef(xOffset, yOffset, zOffset) around the part list.
        PartDefinition offset = fish.addOrReplaceChild("offset", CubeListBuilder.create(),
                PartPose.offset(0.0F, IN_WATER_Y, 0.0F));

        offset.addOrReplaceChild("body_flat",
                CubeListBuilder.create().texOffs(0, 2).addBox(0.0F, -1.5F, -1.0F, 5.0F, 3.0F, 2.0F),
                PartPose.offset(-3.0F, 15.0F, 0.0F));
        offset.addOrReplaceChild("body_romboid",
                CubeListBuilder.create().texOffs(0, 7).addBox(0.0F, 0.0F, -0.5F, 4.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 15.0F, 0.0F, 0.0F, 0.0F, DIAMOND));
        offset.addOrReplaceChild("mid_body_fin",
                CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -0.5F, 0.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(-3.0F, 15.0F, 0.0F, 0.0F, MID_FIN_YAW, 0.0F));
        offset.addOrReplaceChild("upper_fin_a",
                CubeListBuilder.create().texOffs(10, 0).addBox(-0.5F, -1.3F, -0.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(-0.65F, 13.5F, 0.0F));
        offset.addOrReplaceChild("upper_fin_b",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 13.5F, 0.0F));
        // Zero-thickness dorsal sail (legacy 8x3x0), exactly as vanilla's tropical-fish fins are built.
        offset.addOrReplaceChild("upper_fin_c",
                CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -2.0F, 0.0F, 8.0F, 3.0F, 0.0F),
                PartPose.offset(0.0F, 13.5F, 0.0F));
        offset.addOrReplaceChild("lower_fin_a",
                CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -0.3F, -0.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(-0.65F, 17.2F, 0.0F));
        // Zero-height pelvic plane (legacy 5x0x6); it flaps with the fin animation below.
        offset.addOrReplaceChild("lower_fin_b",
                CubeListBuilder.create().texOffs(0, 21).addBox(0.0F, 0.0F, -3.0F, 5.0F, 0.0F, 6.0F, DOWN_ONLY)
                        .texOffs(-5, 21).addBox(0.0F, 0.0F, -3.0F, 5.0F, 0.0F, 6.0F, UP_ONLY),
                PartPose.offset(-3.0F, 16.0F, 0.0F));
        offset.addOrReplaceChild("lower_fin_c",
                CubeListBuilder.create().texOffs(16, 18).addBox(-5.0F, 0.0F, 0.0F, 8.0F, 3.0F, 0.0F),
                PartPose.offset(0.0F, 15.5F, 0.0F));
        offset.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(10, 7).addBox(0.0F, 0.0F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.3F, 15.0F, 0.0F, 0.0F, 0.0F, DIAMOND));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // Legacy MoCEntitySmallFish.getAdjustedYOffset/getAdjustedZOffset:145-150 and 219-225: a beached fish is
        // dropped lower (so it lies ON the ground once MoCSmallFishRenderer rolls it onto its side) and nudged
        // 0.1 blocks along its own long axis.
        boolean inWater = state.isInWater;
        this.offset.y = inWater ? IN_WATER_Y : BEACHED_Y;
        this.offset.z = inWater ? 0.0F : BEACHED_Z;

        // Legacy MoCEntitySmallFish.yawRotationOffset:157-164 — 90 degrees flat when beached, and while
        // swimming the base 90 plus MoCEntityAquatic's lazy 8-degree sideways sway (sin(ticks * 0.5) * 8),
        // which legacy only applied while the fish was actually moving.
        float sway = (inWater && state.walkAnimationSpeed > 0.0F)
                ? Mth.sin(state.ageInTicks * 0.5F) * SWAY_DEGREES
                : 0.0F;
        this.fish.yRot = -Mth.HALF_PI - (sway * Mth.DEG_TO_RAD);

        // Legacy setRotationAngles:101-108: the tail wags with the swim stroke while the mid-body and pelvic
        // fins idle on a slow, swim-independent cosine.
        float tailMov = Mth.cos(state.walkAnimationPos * 0.8F) * state.walkAnimationSpeed * 0.6F;
        float finMov = Mth.cos(state.ageInTicks * 0.4F) * 0.2F;
        this.tail.yRot = tailMov;
        this.midBodyFin.yRot = MID_FIN_YAW + finMov;
        this.lowerFinB.zRot = finMov;
    }
}
