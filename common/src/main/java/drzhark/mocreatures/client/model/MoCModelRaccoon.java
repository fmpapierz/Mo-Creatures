package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Raccoon model, converted faithfully from the legacy {@code MoCModelRaccoon} ({@code ModelBase},
 * 1.12.2). Every cube keeps its original texture offset, box origin/size and rotation point on the
 * 64x64 {@code raccoon.png} sheet, and the two-segment legs / two-segment tail keep their legacy gait.
 *
 * <p>Two legacy details are worth recording:</p>
 * <ul>
 *   <li>The cheek "sideburns" are <em>children of the head</em> (legacy {@code Head.addChild(...)} at
 *       {@code MoCModelRaccoon:57}/{@code :63}). Their explicit {@code render()} calls are commented out
 *       in the legacy {@code render()} precisely because a 1.12 {@code ModelRenderer} draws its children
 *       automatically — rendering them again would have double-drawn them. They are therefore modelled
 *       here as real child parts of {@code head}, so they inherit the head's look-at rotation for free
 *       (which is what the commented-out {@code RightSideburn.rotateAngleX = Head.rotateAngleX} block at
 *       {@code MoCModelRaccoon:197-200} was trying to do by hand).</li>
 *   <li>The legs are built from three <em>sibling</em> parts each (upper {@code A}, lower {@code B} and a
 *       foot) that all share one rotation point rather than being parented to one another, so each
 *       segment carries its own baked-in rest angle and is animated separately. That is reproduced
 *       exactly rather than "fixed" into a proper joint chain, because the rest angles and box origins
 *       were authored against that flat layout.</li>
 * </ul>
 */
public class MoCModelRaccoon extends EntityModel<MoCEntityRenderState> {

    /** Legacy {@code radianF = 57.29578F}: the model's degrees-to-radians divisor. */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart earRight;
    private final ModelPart earLeft;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart frontLegRightA;
    private final ModelPart frontLegRightB;
    private final ModelPart frontFootRight;
    private final ModelPart frontLegLeftA;
    private final ModelPart frontLegLeftB;
    private final ModelPart frontFootLeft;
    private final ModelPart rearLegRightA;
    private final ModelPart rearLegRightB;
    private final ModelPart rearFootRight;
    private final ModelPart rearLegLeftA;
    private final ModelPart rearLegLeftB;
    private final ModelPart rearFootLeft;

    public MoCModelRaccoon(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.snout = root.getChild("snout");
        this.earRight = root.getChild("ear_right");
        this.earLeft = root.getChild("ear_left");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_b");
        this.frontLegRightA = root.getChild("front_leg_right_a");
        this.frontLegRightB = root.getChild("front_leg_right_b");
        this.frontFootRight = root.getChild("front_foot_right");
        this.frontLegLeftA = root.getChild("front_leg_left_a");
        this.frontLegLeftB = root.getChild("front_leg_left_b");
        this.frontFootLeft = root.getChild("front_foot_left");
        this.rearLegRightA = root.getChild("rear_leg_right_a");
        this.rearLegRightB = root.getChild("rear_leg_right_b");
        this.rearFootRight = root.getChild("rear_foot_right");
        this.rearLegLeftA = root.getChild("rear_leg_left_a");
        this.rearLegLeftB = root.getChild("rear_leg_left_b");
        this.rearFootLeft = root.getChild("rear_foot_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ------------------------------------------------------------------ head + masked cheeks
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(38, 21).addBox(-4.0F, -3.5F, -6.5F, 8.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 17.0F, -4.0F));
        // Child cubes of the head (legacy Head.addChild): the flared cheek tufts, splayed +/-30 degrees.
        head.addOrReplaceChild("sideburn_right",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(-2.5F, 0.5F, -3.2F, 0.0F, -0.5235988F, 0.0F));
        head.addOrReplaceChild("sideburn_left",
                CubeListBuilder.create().texOffs(0, 40).addBox(0.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(2.5F, 0.5F, -3.2F, 0.0F, 0.5235988F, 0.0F));

        root.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(24, 25).addBox(-1.5F, -0.5F, -10.5F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, -4.0F));
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(24, 22).addBox(-4.0F, -5.5F, -3.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 17.0F, -4.0F));
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(24, 18).addBox(1.0F, -5.5F, -3.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 17.0F, -4.0F));

        // -------------------------------------------------------------------------- neck + trunk
        // The neck is fixed at -25.5 degrees (legacy -0.4461433F) and never animated: it is the wedge
        // that hides the seam between the low-slung body and the head.
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(46, 4).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 17.0F, -4.0F, -0.4461433F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 12.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // -------------------------------------------------------------------- two-segment ringed tail
        // Both segments share the rump pivot and carry different rest pitches (-116 / -96.8 degrees), which
        // is what gives the raccoon its distinctive tail held up and curved back over the body.
        root.addOrReplaceChild("tail_a",
                CubeListBuilder.create().texOffs(0, 3).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 16.5F, 6.5F, -2.024582F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_b",
                CubeListBuilder.create().texOffs(24, 3).addBox(-1.5F, -11.0F, 0.3F, 3.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 16.5F, 6.5F, -1.689974F, 0.0F, 0.0F));

        // ------------------------------------------------------------------------------ front legs
        root.addOrReplaceChild("front_leg_right_a",
                CubeListBuilder.create().texOffs(36, 0).addBox(-4.0F, -1.0F, -1.0F, 2.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, -4.0F, 0.5205006F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_leg_right_b",
                CubeListBuilder.create().texOffs(46, 11).addBox(-3.5F, 1.0F, 2.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, -4.0F, -0.3717861F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_foot_right",
                CubeListBuilder.create().texOffs(46, 0).addBox(-4.0F, 5.0F, -1.0F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, -4.0F));
        root.addOrReplaceChild("front_leg_left_a",
                CubeListBuilder.create().texOffs(36, 8).addBox(2.0F, -1.0F, -1.0F, 2.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, -4.0F, 0.5205006F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_leg_left_b",
                CubeListBuilder.create().texOffs(54, 11).addBox(1.5F, 1.0F, 2.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, -4.0F, -0.3717861F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_foot_left",
                CubeListBuilder.create().texOffs(46, 0).addBox(1.0F, 5.0F, -1.0F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, -4.0F));

        // ------------------------------------------------------------------------------- rear legs
        root.addOrReplaceChild("rear_leg_right_a",
                CubeListBuilder.create().texOffs(12, 18).addBox(-5.0F, -2.0F, -3.0F, 2.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 4.0F, 0.9294653F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_leg_right_b",
                CubeListBuilder.create().texOffs(0, 27).addBox(-4.5F, 2.0F, -5.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 4.0F, 0.9294653F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_foot_right",
                CubeListBuilder.create().texOffs(46, 0).addBox(-5.0F, 5.0F, -2.0F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("rear_leg_left_a",
                CubeListBuilder.create().texOffs(0, 18).addBox(3.0F, -2.0F, -3.0F, 2.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 4.0F, 0.9294653F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_leg_left_b",
                CubeListBuilder.create().texOffs(10, 27).addBox(2.5F, 2.0F, -5.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 4.0F, 0.9294653F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_foot_left",
                CubeListBuilder.create().texOffs(46, 0).addBox(2.0F, 5.0F, -2.0F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, 4.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // Head tracking: legacy divided the incoming DEGREE yaw/pitch by radianF (57.29578) to get radians.
        // The snout and both ears are siblings of the head in the legacy mesh, so they must be steered by
        // hand to the same angles; the sideburns are real children here and follow automatically.
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;
        this.head.yRot = headYaw;
        this.head.xRot = headPitch;
        this.snout.yRot = headYaw;
        this.snout.xRot = headPitch;
        this.earRight.yRot = headYaw;
        this.earRight.xRot = headPitch;
        this.earLeft.yRot = headYaw;
        this.earLeft.xRot = headPitch;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        // Legacy gait: one pair of legs swings on cos(limbSwing), the other on cos(limbSwing + PI) so the
        // raccoon walks diagonally (front-right with rear-left, front-left with rear-right). Note the legacy
        // deliberate cross-over — RightRearLegA uses the LEFT phase and LeftRearLegA the RIGHT phase.
        float swingRight = Mth.cos(limbSwing + 3.141593F) * 0.8F * limbAmount;
        float swingLeft = Mth.cos(limbSwing) * 0.8F * limbAmount;

        // Rest angles are re-applied every frame on top of the swing (legacy setRotationAngles overwrote the
        // constructor's rest pitches rather than adding to them): 30 degrees for the front uppers, 53 for the
        // rear uppers and -21 for the front lowers.
        this.frontLegRightA.xRot = (30.0F * DEG_TO_RAD) + swingRight;
        this.frontLegLeftA.xRot = (30.0F * DEG_TO_RAD) + swingLeft;
        this.rearLegRightA.xRot = (53.0F * DEG_TO_RAD) + swingLeft;
        this.rearLegLeftA.xRot = (53.0F * DEG_TO_RAD) + swingRight;

        this.frontLegRightB.xRot = (-21.0F * DEG_TO_RAD) + swingRight;
        this.frontFootRight.xRot = swingRight;
        this.frontLegLeftB.xRot = (-21.0F * DEG_TO_RAD) + swingLeft;
        this.frontFootLeft.xRot = swingLeft;

        this.rearLegRightB.xRot = (53.0F * DEG_TO_RAD) + swingLeft;
        this.rearFootRight.xRot = swingLeft;
        this.rearLegLeftB.xRot = (53.0F * DEG_TO_RAD) + swingRight;
        this.rearFootLeft.xRot = swingRight;

        // The tail sways side to side (yaw, not pitch) at the slower 0.6662 frequency while walking; both
        // segments share the angle so the ringed tail stays straight as it swings.
        this.tailA.yRot = Mth.cos(limbSwing * 0.6662F) * 0.7F * limbAmount;
        this.tailB.yRot = this.tailA.yRot;

        // The neck and the body are never animated in the legacy model, so they are not held as fields:
        // super.setupAnim() has already restored every part (including the neck's baked -25.5 degree pitch)
        // to its authored rest pose before the assignments above.
    }
}
