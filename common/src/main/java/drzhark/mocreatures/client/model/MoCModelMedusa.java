package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Medusa (pass 3, to the user's art direction): clean smooth boxes. A scaled hood frames the pale
 * face; THREE skinny segmented snakes rise from the crown and four short skinny ones hang at the
 * SIDES of the head, every snake head carrying variant-coloured eye pixels; the arms are smooth
 * uniform rectangles (no pauldrons, hands painted); the chest reads as a hanging bust — a low
 * protruding skin box with shadow under it — over the metal wrap band; the abdomen below is
 * scaled. The trunk is a stepped stack of smooth ring segments dropping to the ground, and the
 * tapering tail runs BEHIND her along the ground with a gentle S-curve.
 *
 * <p>Authored at 2x pixel scale (ground plane y = 48) on a 256x256 sheet, shrunk to world size by
 * a bare {@code pose.scaled(0.5F)} mesh transform for double texel density (the stock
 * {@code MeshTransformer.scaling} would sink a 2x model 12px into the floor — see
 * {@code MoCModelMinotaur}).
 *
 * <p>Structure: hood + all seven snakes are children of {@code head}; the trunk is a parent chain
 * serpent1..serpent4 with the tail (tail1 -> tail2 -> tail_tip) off serpent4, its S-curve applied
 * in {@link #setupAnim} (pose resets wipe baked rotations). Every box is at least 1px thick;
 * overlapping rest-pose faces are never coplanar.
 */
public class MoCModelMedusa extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /** Stalk part names: 3 crown snakes (rising), then 4 hanging side snakes. */
    private static final String[] STALK_NAMES = {
            "stalk_crown_c", "stalk_crown_l", "stalk_crown_r",
            "stalk_hang_fl", "stalk_hang_fr", "stalk_hang_bl", "stalk_hang_br"};
    /** Baked roll: crown near-upright; hanging snakes splay outward from the hood sides. */
    private static final float[] STALK_BASE_Z = {0.0F, -0.08F, 0.08F, 0.26F, -0.26F, 0.38F, -0.38F};
    /** Baked pitch: crown vertical-ish; hanging snakes tip slightly forward. */
    private static final float[] STALK_BASE_X = {-0.04F, -0.02F, -0.02F, 0.10F, 0.10F, 0.04F, 0.04F};
    /** Baked yaw of the tail run behind her (tail1, tail2, tip): a gentle S-curve. */
    private static final float[] TAIL_SWEEP = {0.15F, -0.22F, 0.18F};

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    /** Vertical trunk stack, top to bottom. */
    private final ModelPart[] trunk = new ModelPart[4];
    /** Tail run behind: tail1, tail2, tip. */
    private final ModelPart[] tail = new ModelPart[3];
    /** The seven hair snakes (children of the head), in {@link #STALK_NAMES} order. */
    private final ModelPart[] stalks = new ModelPart[7];

    public MoCModelMedusa(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        for (int i = 0; i < STALK_NAMES.length; i++) {
            this.stalks[i] = this.head.getChild(STALK_NAMES[i]);
        }
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.trunk[0] = root.getChild("serpent1");
        for (int i = 1; i < 4; i++) {
            this.trunk[i] = this.trunk[i - 1].getChild("serpent" + (i + 1));
        }
        this.tail[0] = this.trunk[3].getChild("tail1");
        this.tail[1] = this.tail[0].getChild("tail2");
        this.tail[2] = this.tail[1].getChild("tail_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- Head (pivot 0,-4,0): 14px skull under a scaled hood — crown slab, back slab, side
        // curtains past the jaw. The face is the skull's bare front.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0F, -13.5F, -7.0F, 14.0F, 14.0F, 14.0F)     // skull
                        .texOffs(56, 0).addBox(-9.0F, -15.5F, -8.0F, 18.0F, 4.0F, 18.0F)     // hood crown
                        .texOffs(128, 0).addBox(-9.0F, -11.9F, 4.6F, 18.0F, 14.0F, 4.0F)     // hood back
                        .texOffs(174, 0).addBox(-9.8F, -12.1F, -6.4F, 3.0F, 16.0F, 14.0F)    // hood right
                        .texOffs(210, 0).addBox(6.8F, -12.1F, -6.4F, 3.0F, 16.0F, 14.0F),    // hood left
                PartPose.offset(0.0F, -4.0F, 0.0F));

        // Crown snakes: three skinny segmented columns rising from the crown, each with a small
        // head whose front face carries the eyes.
        head.addOrReplaceChild("stalk_crown_c", CubeListBuilder.create()
                        .texOffs(188, 30).addBox(-1.0F, -11.0F, -1.0F, 2.0F, 11.0F, 2.0F)
                        .texOffs(198, 30).addBox(-1.5F, -13.6F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, -14.8F, 0.4F));
        head.addOrReplaceChild("stalk_crown_l", CubeListBuilder.create()
                        .texOffs(188, 30).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 9.0F, 2.0F)
                        .texOffs(198, 30).addBox(-1.5F, -11.6F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(4.6F, -14.8F, 1.6F));
        head.addOrReplaceChild("stalk_crown_r", CubeListBuilder.create()
                        .texOffs(188, 30).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 9.0F, 2.0F)
                        .texOffs(198, 30).addBox(-1.5F, -11.6F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-4.6F, -14.8F, 1.6F));

        // Hanging side snakes: short skinny columns growing DOWNWARD from the hood sides, heads at
        // jaw height with the eyes forward.
        head.addOrReplaceChild("stalk_hang_fl", CubeListBuilder.create()
                        .texOffs(212, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F)
                        .texOffs(222, 30).addBox(-1.5F, 5.4F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(10.3F, -8.5F, -4.5F));
        head.addOrReplaceChild("stalk_hang_fr", CubeListBuilder.create()
                        .texOffs(212, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F)
                        .texOffs(222, 30).addBox(-1.5F, 5.4F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-10.3F, -8.5F, -4.5F));
        head.addOrReplaceChild("stalk_hang_bl", CubeListBuilder.create()
                        .texOffs(212, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                        .texOffs(222, 30).addBox(-1.5F, 4.4F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(10.5F, -5.0F, 1.5F));
        head.addOrReplaceChild("stalk_hang_br", CubeListBuilder.create()
                        .texOffs(212, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                        .texOffs(222, 30).addBox(-1.5F, 4.4F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-10.5F, -5.0F, 1.5F));

        // ---- Torso (pivot 0,-4,0 = the neck): smooth chest with a LOW protruding bust (hanging
        // read comes from the under-shadow in the texture), the metal wrap band under it, then the
        // scaled abdomen sinking into the trunk hips.
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 30).addBox(-8.0F, 0.0F, -5.0F, 16.0F, 12.0F, 10.0F)      // chest
                        .texOffs(52, 30).addBox(-6.0F, 4.0F, -7.4F, 12.0F, 6.0F, 4.0F)       // hanging bust
                        .texOffs(88, 30).addBox(-8.5F, 10.2F, -5.4F, 17.0F, 5.0F, 11.0F)     // metal band
                        .texOffs(144, 30).addBox(-6.5F, 14.5F, -4.4F, 13.0F, 10.0F, 8.0F),   // scaled abdomen
                PartPose.offset(0.0F, -4.0F, 0.0F));

        // ---- Arms: smooth uniform rectangles, hands painted on the lower rows. No pauldrons.
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 52).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 22.0F, 5.0F),
                PartPose.offset(-10.5F, -2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(24, 52).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 22.0F, 5.0F),
                PartPose.offset(10.5F, -2.0F, 0.0F));

        // ---- Trunk: a stepped stack of smooth ring segments, each jogging fore/aft of its
        // parent, dropping exactly to the ground plane at y 48.
        PartDefinition s1 = root.addOrReplaceChild("serpent1",
                CubeListBuilder.create().texOffs(0, 80).addBox(-8.5F, 0.0F, -7.0F, 17.0F, 9.0F, 14.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        PartDefinition s2 = s1.addOrReplaceChild("serpent2",
                CubeListBuilder.create().texOffs(72, 80).addBox(-8.0F, -0.5F, -7.2F, 16.0F, 9.0F, 13.0F),
                PartPose.offset(0.0F, 8.5F, 1.2F));
        PartDefinition s3 = s2.addOrReplaceChild("serpent3",
                CubeListBuilder.create().texOffs(140, 80).addBox(-7.5F, -0.5F, -5.2F, 15.0F, 9.0F, 12.0F),
                PartPose.offset(0.0F, 8.0F, -1.8F));
        PartDefinition s4 = s3.addOrReplaceChild("serpent4",
                CubeListBuilder.create().texOffs(0, 105).addBox(-7.0F, -0.5F, -6.0F, 14.0F, 7.0F, 11.0F),
                PartPose.offset(0.0F, 8.0F, 0.8F));

        // Tail: runs BEHIND her along the ground, tapering over two segments to a tip; the gentle
        // S-curve is applied in setupAnim (TAIL_SWEEP).
        PartDefinition t1 = s4.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(60, 105).addBox(-3.5F, -3.0F, -1.0F, 7.0F, 6.0F, 13.0F),
                PartPose.offset(0.0F, 3.5F, 4.0F));
        PartDefinition t2 = t1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(104, 105).addBox(-2.5F, -2.0F, -0.5F, 5.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 1.0F, 11.4F));
        t2.addOrReplaceChild("tail_tip",
                CubeListBuilder.create().texOffs(140, 105).addBox(-1.5F, -1.2F, -0.5F, 3.0F, 2.5F, 10.0F),
                PartPose.offset(0.0F, 0.5F, 10.9F));

        // 2x authoring (ground y=48) on a 256 sheet; bare 0.5 scale restores world size with
        // double texel density.
        return LayerDefinition.create(mesh, 256, 256)
                .apply(m -> m.transformed(pose -> pose.scaled(0.5F)));
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float age = state.ageInTicks;

        // Head tracks the look; hood and snakes are its children and follow for free.
        this.head.xRot = headPitch;
        this.head.yRot = headYaw;

        // Two-arm strike (0 at rest, 1 mid-swing) with the torso leaning into it at the neck.
        float smash = Mth.sin(state.attackSwing * (float) Math.PI);
        this.body.xRot = 0.10F * smash;

        // Arms: subtle opposed gait swing, an idle breathing splay, and the shared strike lunge.
        float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.4F * limbAmount;
        this.rightArm.xRot = armSwing - smash * 1.9F;
        this.leftArm.xRot = -armSwing - smash * 1.9F;
        this.leftArm.zRot = (Mth.cos(age * 0.09F) * 0.05F) - 0.05F;
        this.rightArm.zRot = -(Mth.cos(age * 0.09F) * 0.05F) + 0.05F;

        // Hair snakes: baked lean + independent slow weave, phase-offset so the nest never moves
        // in unison; the hanging side snakes sway a touch wider than the crown ones.
        for (int i = 0; i < this.stalks.length; i++) {
            float amp = i < 3 ? 0.07F : 0.11F;
            this.stalks[i].zRot = STALK_BASE_Z[i] + amp * Mth.sin(age * 0.11F + i * 0.9F);
            this.stalks[i].xRot = STALK_BASE_X[i] + 0.06F * Mth.cos(age * 0.13F + i * 1.3F);
        }

        // Trunk: gentle phase-offset yaw sway plus the snake model's travelling wave while moving.
        float w = 1.5F;
        float t = limbSwing / 2.0F;
        for (int i = 0; i < this.trunk.length; i++) {
            this.trunk[i].yRot = 0.045F * Mth.sin(age * 0.08F + i * 0.8F)
                    + 0.11F * Mth.sin(w * t - 0.5F * i) * limbAmount;
            this.trunk[i].zRot = 0.018F * Mth.sin(age * 0.06F + i * 0.7F);
        }
        // Tail behind: baked S-curve (re-applied every frame around the pose reset) + wave.
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i].yRot = TAIL_SWEEP[i]
                    + 0.05F * Mth.sin(age * 0.07F + i)
                    + 0.15F * Mth.sin(w * t - 0.45F * i) * limbAmount;
        }
    }
}
