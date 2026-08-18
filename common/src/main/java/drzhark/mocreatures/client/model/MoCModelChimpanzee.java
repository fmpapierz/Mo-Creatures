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
 * Chimpanzee model — an original mesh (the species was designed but never released upstream, so there
 * is no legacy {@code ModelBase} to convert). Authored at adult size on a 64x64 {@code chimpanzee.png}
 * sheet against the 0.9 x 1.1 hitbox; growth scaling comes from the generic renderer via
 * {@code state.moCAge}/{@code state.adult}.
 *
 * <p>Anatomy notes (what makes it read as a chimp and not a generic quadruped):</p>
 * <ul>
 *   <li><b>Knuckle-walker stance.</b> The torso is one box pitched -30 degrees so the shoulders ride
 *       high (back line at ~17 px) and the rump drops onto a separate level hip box (~6 px lower). The
 *       arms hang 16 px from shoulder to ground while the hind legs are only 9 px — the long-arm /
 *       short-leg mismatch IS the silhouette, so neither pair should be resized independently.</li>
 *   <li><b>Arms end in knuckle fists.</b> Each arm part carries a second, chunkier 4x4x4 cube at the
 *       wrist that juts <em>forward</em> of the arm line (curled fingers planted knuckles-down). The
 *       fist is a cube of the arm part, not a child, so it swings rigidly with the shoulder.</li>
 *   <li><b>Head.</b> A near-cubic skull with a large bare face, a heavy 6x2x2 brow ridge proud of the
 *       top-front edge, a 4x3x3 muzzle pushed 2.5 px forward, and two flat 1-px ear discs that stick
 *       out sideways from the upper half of the skull. Brow/muzzle/ears are cubes of the head part and
 *       track the look direction for free. No tail (apes have none) — the hip box caps the rear.</li>
 * </ul>
 *
 * <p>Every cube is at least 1 px on all axes and no two faces are coplanar (the renderer draws
 * unculled): overlapping cubes always differ in every shared plane by at least 0.4 px.</p>
 */
public class MoCModelChimpanzee extends EntityModel<MoCEntityRenderState> {

    /** Degrees-to-radians divisor, matching the other MoC models (legacy {@code radianF}). */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart armRight;
    private final ModelPart armLeft;
    private final ModelPart legRight;
    private final ModelPart legLeft;

    public MoCModelChimpanzee(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.armRight = root.getChild("arm_right");
        this.armLeft = root.getChild("arm_left");
        this.legRight = root.getChild("leg_right");
        this.legLeft = root.getChild("leg_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ----------------------------------------------------------------------------------- head
        // Pivot at the skull centre so look pitch/yaw rotate naturally. Skull top sits at world y=5
        // (19 px tall overall). Brow rides 1 px proud of the top-front edge, the muzzle protrudes
        // 2.5 px forwards low on the face, and the ear discs poke 0.8 px out of each temple, kept
        // 0.2-0.5 px clear of every skull plane so nothing z-fights.
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -3.5F, -5.5F, 7.0F, 7.0F, 6.0F)     // skull
                        .texOffs(46, 0).addBox(-2.0F, -0.5F, -8.0F, 4.0F, 3.0F, 3.0F)    // muzzle
                        .texOffs(46, 7).addBox(-3.0F, -4.0F, -6.5F, 6.0F, 2.0F, 2.0F)    // brow ridge
                        .texOffs(39, 26).addBox(-4.3F, -3.2F, -2.6F, 1.0F, 4.0F, 3.0F)   // ear right
                        .texOffs(48, 26).addBox(3.3F, -3.2F, -2.6F, 1.0F, 4.0F, 3.0F),   // ear left
                PartPose.offset(0.0F, 8.5F, -3.0F));

        // ---------------------------------------------------------------------------- neck + torso
        // The neck shares the torso's -30 degree pitch and is never animated: it is the wedge that
        // hides the seam between the high chest and the skull rear when the head yaws.
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(27, 0).addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, -4.5F, -0.5235988F, 0.0F, 0.0F));
        // Torso pitched -30 degrees: chest front rides at world y ~7-12, the spine slopes down and
        // back to the rump at ~12-18. This is the knuckle-walk lean the whole model hangs off.
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 14).addBox(-4.0F, -3.0F, -1.0F, 8.0F, 6.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, -3.0F, -0.5235988F, 0.0F, 0.0F));
        // Level hip box capping the sloped torso rear (no tail — this is the whole backside). It is
        // 1 px narrower than the chest so no side plane is shared with the torso.
        root.addOrReplaceChild("hips",
                CubeListBuilder.create().texOffs(39, 14).addBox(-3.5F, -3.0F, -2.5F, 7.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 15.0F, 5.5F));

        // -------------------------------------------------------------- arms (long, knuckle fists)
        // Shoulder pivots high on the chest sides (world y=9). Arm shaft reaches 13 px down; the
        // fist cube overlaps the shaft's last pixel and carries the hand to the ground at y=24 —
        // 16 px of arm against 9 px of leg. The fist is wider than the shaft (4 vs 3) and juts
        // 1.1 px forward: knuckles curled under, planted ahead of the arm line.
        root.addOrReplaceChild("arm_right", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F)   // shaft
                        .texOffs(0, 49).addBox(-2.0F, 11.0F, -2.6F, 4.0F, 4.0F, 4.0F),   // knuckle fist
                PartPose.offset(-5.0F, 9.0F, -2.0F));
        root.addOrReplaceChild("arm_left", CubeListBuilder.create()
                        .texOffs(13, 32).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F)
                        .texOffs(17, 49).addBox(-2.0F, 11.0F, -2.6F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(5.0F, 9.0F, -2.0F));

        // ------------------------------------------------------------------- hind legs (short) + feet
        // Hip pivots on the hip box sides. The 8-px shank stops 1 px short of the ground; the long
        // flat foot overlaps that last pixel, reaches the ground at y=24 and extends 2 px forward of
        // the shank (an ape's long grasping foot). Foot is wider (4 vs 3) so no plane is shared.
        root.addOrReplaceChild("leg_right", CubeListBuilder.create()
                        .texOffs(26, 32).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F)   // shank
                        .texOffs(34, 49).addBox(-2.0F, 5.5F, -3.5F, 4.0F, 3.0F, 4.0F),   // foot
                PartPose.offset(-2.5F, 15.5F, 5.5F));
        root.addOrReplaceChild("leg_left", CubeListBuilder.create()
                        .texOffs(39, 34).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F)
                        .texOffs(34, 57).addBox(-2.0F, 5.5F, -3.5F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(2.5F, 15.5F, 5.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // Head tracking (state yaw/pitch arrive in degrees, like every MoC model) plus a gentle idle
        // bob on the pitch — a slow, small nod so the chimp never sits perfectly still.
        this.head.yRot = state.yRot * DEG_TO_RAD;
        this.head.xRot = state.xRot * DEG_TO_RAD + Mth.sin(state.ageInTicks * 0.06F) * 0.04F;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        // Knuckle-walk gait: diagonal pairs (right arm with left leg), like the raccoon, but the
        // arms swing a wider arc (0.75 vs 0.5 rad) and lead the stride by a small phase advance —
        // a knuckle-walker plants the fist a beat before the diagonal hind foot follows.
        float armPhase = limbSwing + 0.4F;
        float armSwingRight = Mth.cos(armPhase + 3.141593F) * 0.75F * limbAmount;
        float armSwingLeft = Mth.cos(armPhase) * 0.75F * limbAmount;
        float legSwingRight = Mth.cos(limbSwing) * 0.5F * limbAmount;
        float legSwingLeft = Mth.cos(limbSwing + 3.141593F) * 0.5F * limbAmount;

        this.armRight.xRot = armSwingRight;
        this.armLeft.xRot = armSwingLeft;
        this.legRight.xRot = legSwingRight;
        this.legLeft.xRot = legSwingLeft;

        // Attack: a two-armed overhead flail. attackSwing runs 0..1 over the swing, sin(x*PI) peaks
        // mid-swing, so both arms whip up ~97 degrees and flare slightly outwards, then fall back.
        // super.setupAnim() restored zRot to 0, so the flare needs no manual reset.
        float flail = Mth.sin(state.attackSwing * (float) Math.PI);
        this.armRight.xRot -= flail * 1.7F;
        this.armLeft.xRot -= flail * 1.7F;
        this.armRight.zRot = flail * 0.35F;
        this.armLeft.zRot = -flail * 0.35F;
    }
}
