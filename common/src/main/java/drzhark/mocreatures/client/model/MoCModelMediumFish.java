package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Medium fish (salmon / cod / bass) model, converted faithfully from the legacy {@code MoCModelMediumFish}
 * ({@code ModelBase}). Geometry, texture offsets and every animation are preserved: the tail and tail fin
 * sweep with the swim, the pectoral fins paddle on a slow timer, and the lower jaw gulps continuously.
 *
 * <p>Two pieces of legacy scaffolding needed a home in the modern pipeline:</p>
 * <ul>
 *   <li><b>The body offset.</b> Legacy's {@code render()} wrapped the whole model in
 *       {@code GL11.glTranslatef(getAdjustedXOffset(), getAdjustedYOffset(), getAdjustedZOffset())} —
 *       0 / 0.5 / 0 blocks while submerged and 0 / 1.0 / 0.2 out of water, dropping a beached fish to the
 *       ground and nudging it clear of the block face. That translate happened in the model's own local
 *       space, <em>inside</em> the renderer's rotations, so it is reproduced here on a {@link #fish} pivot
 *       part that parents all sixteen cubes rather than in the renderer (where the same numbers would be
 *       applied in the rotated frame and push the fish sideways instead of down).</li>
 *   <li><b>The base orientation.</b> The legacy geometry is laid out along the X axis — nose at x=-11, tail
 *       fin out at x=+16 — and was swung into place by {@code MoCEntityMediumFish.yawRotationOffset()},
 *       which returned 90 degrees plus the shared aquatic swim-wiggle. That is a renderer-space rotation, so
 *       it stays in the renderer; see {@code MoCMediumFishRenderer}.</li>
 * </ul>
 */
public class MoCModelMediumFish extends EntityModel<MoCEntityRenderState> {

    /** Legacy pectoral-fin rest angle, {@code 0.8726646F} (50 degrees) out from the body. */
    private static final float PECTORAL_REST = 0.8726646F;
    /** Legacy lower-jaw rest angle, {@code 0.3346075F}. */
    private static final float MOUTH_REST = 0.3346075F;
    /** Legacy jaw-tip rest angle, {@code -0.7132579F}. */
    private static final float MOUTH_TIP_REST = -0.7132579F;

    /**
     * Pivot parenting every cube. Legacy applied {@code getAdjustedYOffset()} / {@code getAdjustedZOffset()}
     * as a whole-model translate; here it is this pivot's position, in pixels (1 block = 16 px).
     */
    private final ModelPart fish;
    private final ModelPart tail;
    private final ModelPart tailFin;
    private final ModelPart rightPectoralFin;
    private final ModelPart leftPectoralFin;
    private final ModelPart mouthBottom;
    private final ModelPart mouthBottomB;

    public MoCModelMediumFish(ModelPart root) {
        super(root);
        this.fish = root.getChild("fish");
        this.tail = this.fish.getChild("tail");
        this.tailFin = this.fish.getChild("tail_fin");
        this.rightPectoralFin = this.fish.getChild("right_pectoral_fin");
        this.leftPectoralFin = this.fish.getChild("left_pectoral_fin");
        this.mouthBottom = this.fish.getChild("mouth_bottom");
        this.mouthBottomB = this.fish.getChild("mouth_bottom_b");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition fish = mesh.getRoot().addOrReplaceChild("fish", CubeListBuilder.create(), PartPose.ZERO);

        fish.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 10).addBox(-5.0F, 0.0F, -1.5F, 5.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-8.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.4461433F));
        fish.addOrReplaceChild("lower_head",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -3.0F, -1.5F, 4.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-8.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.3346075F));
        fish.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(14, 17).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-11.0F, 8.2F, 0.0F, 0.0F, 0.0F, 1.412787F));
        fish.addOrReplaceChild("mouth_bottom",
                CubeListBuilder.create().texOffs(16, 10).addBox(-2.0F, -0.4F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-11.5F, 10.0F, 0.0F, 0.0F, 0.0F, MOUTH_REST));
        fish.addOrReplaceChild("mouth_bottom_b",
                CubeListBuilder.create().texOffs(16, 13).addBox(-1.5F, -2.4F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-11.5F, 10.0F, 0.0F, 0.0F, 0.0F, MOUTH_TIP_REST));
        fish.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -3.0F, -2.0F, 9.0F, 6.0F, 4.0F),
                PartPose.offset(-8.0F, 9.0F, 0.0F));
        fish.addOrReplaceChild("back_up",
                CubeListBuilder.create().texOffs(26, 0).addBox(0.0F, 0.0F, -1.5F, 8.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(1.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.1858931F));
        fish.addOrReplaceChild("back_down",
                CubeListBuilder.create().texOffs(26, 6).addBox(0.0F, -3.0F, -1.5F, 8.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(1.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1919862F));
        fish.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(48, 0).addBox(0.0F, -1.5F, -1.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(8.0F, 9.0F, 0.0F));
        // Flat (zero-depth) fin quads, exactly as legacy authored them.
        fish.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(48, 5).addBox(3.0F, -5.3F, 0.0F, 5.0F, 11.0F, 0.0F),
                PartPose.offset(8.0F, 9.0F, 0.0F));
        fish.addOrReplaceChild("right_pectoral_fin",
                CubeListBuilder.create().texOffs(28, 12).addBox(0.0F, -2.0F, 0.0F, 5.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-6.5F, 10.0F, 2.0F, 0.0F, -PECTORAL_REST, 0.185895F));
        fish.addOrReplaceChild("left_pectoral_fin",
                CubeListBuilder.create().texOffs(38, 12).addBox(0.0F, -2.0F, 0.0F, 5.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-6.5F, 10.0F, -2.0F, 0.0F, PECTORAL_REST, 0.1858931F));
        fish.addOrReplaceChild("upper_fin",
                CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, -4.0F, 0.0F, 15.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-7.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.1047198F));
        fish.addOrReplaceChild("lower_fin",
                CubeListBuilder.create().texOffs(46, 20).addBox(0.0F, 0.0F, 0.0F, 9.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1858931F));
        fish.addOrReplaceChild("right_lower_fin",
                CubeListBuilder.create().texOffs(28, 16).addBox(0.0F, 0.0F, 0.0F, 9.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-7.0F, 12.0F, 1.0F, 0.5235988F, 0.0F, 0.0F));
        fish.addOrReplaceChild("left_lower_fin",
                CubeListBuilder.create().texOffs(46, 16).addBox(0.0F, 0.0F, 0.0F, 9.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-7.0F, 12.0F, -1.0F, -0.5235988F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // Legacy render(): glTranslatef(getAdjustedXOffset(), getAdjustedYOffset(), getAdjustedZOffset()).
        // X is always 0. Y is 0.5 blocks submerged / 1.0 out of water (MoCEntityMediumFish:103-109) and Z is
        // 0 submerged / 0.2 out of water (:138-144), converted to the pivot's pixel units (x16). Legacy
        // tested the Y offset with isInsideOfMaterial(WATER) and the Z offset with !isInWater(); 26.2's
        // render state carries the single isInWater flag, so both use it — the two only differ for a fish
        // touching the surface, where the offsets are a fraction of a block apart anyway.
        boolean beached = !state.isInWater;
        this.fish.y = beached ? 16.0F : 8.0F;
        this.fish.z = beached ? 3.2F : 0.0F;

        // Legacy setRotationAngles (MoCModelMediumFish:147-165). f = limb swing, f1 = limb swing amount,
        // f2 = the animation timer.
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float timer = state.ageInTicks;

        // Tail sweep, driven by how fast the fish is actually swimming.
        float tailMov = Mth.cos(limbSwing * 0.6662F) * limbAmount * 0.6F;
        this.tail.yRot = tailMov;
        this.tailFin.yRot = tailMov;

        // Pectoral fins paddle open and closed on the free-running timer, so they keep working even when the
        // fish is hovering in place.
        float finMov = Mth.cos(timer * 0.2F) * 0.4F;
        this.leftPectoralFin.yRot = PECTORAL_REST + finMov;
        this.rightPectoralFin.yRot = -PECTORAL_REST - finMov;

        // Continuous gulping of the lower jaw and its tip.
        float mouthMov = Mth.cos(timer * 0.3F) * 0.2F;
        this.mouthBottom.zRot = MOUTH_REST + mouthMov;
        this.mouthBottomB.zRot = MOUTH_TIP_REST + mouthMov;
    }
}
