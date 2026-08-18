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
 * Kitty model, rebuilt faithfully from the original 12.0.5 {@code MoCModelKitty}
 * ({@code ModelBase}). This is the model the {@code pussycat*} textures are authored for.
 *
 * <p>The legacy model never calls {@code setTextureSize}/{@code func_78787_b}, so it inherits
 * {@code ModelBase}'s default texture size of <b>64&times;32</b>; every {@code texOffs} below is
 * expressed in that 64&times;32 UV space. (The shipped {@code pussycat*.png} files are 128&times;64
 * HD textures, i.e. a 2&times; sheet that Minecraft maps the 64&times;32 UVs onto — so the layer
 * definition MUST stay 64&times;32 for the face / eyes to land on the right pixels.)
 *
 * <p>Geometry, texture offsets, mirror flags and rotation points are taken verbatim from the
 * decompiled constructor. The legacy rotation points already place the model on the ground, so no
 * artificial vertical offset is applied. The legacy model is flat (no nested parents); the head
 * parts and the head cube all share rotation point {@code (0, 0, -2)} and the renderer rotated each
 * of them by the same look angles, which we reproduce in {@link #setupAnim}.
 *
 * <p>The legacy renderer hid the collar / whiskers based on the kitty's tamed state
 * ({@code kittystate}); that state is not carried in {@link MoCEntityRenderState}, so those parts
 * are always present here (matching the other generic-rendered Mo'Creatures models).
 */
public class MoCModelKitty extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;        // legacy headParts[9]
    private final ModelPart earRight;    // legacy headParts[0]
    private final ModelPart earLeft;     // legacy headParts[1]
    private final ModelPart browRight;   // legacy headParts[2]
    private final ModelPart browLeft;    // legacy headParts[3]
    private final ModelPart cheekRight;  // legacy headParts[4]
    private final ModelPart cheekLeft;   // legacy headParts[5]
    private final ModelPart nose;        // legacy headParts[6]
    private final ModelPart collar;      // legacy headParts[7]
    private final ModelPart whiskers;    // legacy headParts[8]
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public MoCModelKitty(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.earRight = root.getChild("ear_right");
        this.earLeft = root.getChild("ear_left");
        this.browRight = root.getChild("brow_right");
        this.browLeft = root.getChild("brow_left");
        this.cheekRight = root.getChild("cheek_right");
        this.cheekLeft = root.getChild("cheek_left");
        this.nose = root.getChild("nose");
        this.collar = root.getChild("collar");
        this.whiskers = root.getChild("whiskers");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The legacy model's pivots place the lowest foot box at y=9 (leg pivot y=3 + box height 6),
        // 15px above the modern ground plane at y=24 (the old base renderer translated the whole model
        // down to ground it). We bake that grounding in by adding a uniform +15 to every part's pivot Y
        // so the feet bottom out at y=24, matching the grounded MoCModelBear / MoCModelBigCat.

        // ---- Head cube (legacy headParts[9]); carries the cat face and eyes ----
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(1, 1).addBox(-2.5F, -3.0F, -4.0F, 5.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Ears (legacy headParts[0] / [1]) ----
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, -5.0F, -3.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(16, 0).mirror().addBox(1.0F, -5.0F, -3.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Brows (legacy headParts[2] / [3]) ----
        root.addOrReplaceChild("brow_right",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.5F, -4.0F, -3.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));
        root.addOrReplaceChild("brow_left",
                CubeListBuilder.create().texOffs(20, 0).mirror().addBox(0.5F, -4.0F, -3.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Cheeks (legacy headParts[4] / [5]) ----
        root.addOrReplaceChild("cheek_right",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, -1.5F, -5.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));
        root.addOrReplaceChild("cheek_left",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(1.0F, -1.5F, -5.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Nose / muzzle (legacy headParts[6]) ----
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(21, 6).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Collar / medallion (legacy headParts[7]; tamed-only, hidden by default) ----
        root.addOrReplaceChild("collar",
                CubeListBuilder.create().texOffs(50, 0).addBox(-2.5F, 0.5F, -1.0F, 5.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Whiskers (legacy headParts[8]; special-state-only, hidden by default) ----
        root.addOrReplaceChild("whiskers",
                CubeListBuilder.create().texOffs(60, 0).addBox(-1.5F, -2.0F, -4.1F, 3.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Body ----
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.5F, -2.0F, 0.0F, 5.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));

        // ---- Front legs (arms) ----
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-1.5F, 18.0F, -1.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 9).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(1.5F, 18.0F, -1.0F));

        // ---- Hind legs ----
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(8, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-1.5F, 18.0F, 7.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(8, 9).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(1.5F, 18.0F, 7.0F));

        // ---- Tail ----
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(16, 9).mirror().addBox(-0.5F, -8.0F, -1.0F, 1.0F, 8.0F, 1.0F),
                PartPose.offset(0.0F, 14.5F, 7.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // Collar / medallion is a tamed-only decoration. The legacy renderer only drew headParts[7]
        // when the kitty was tamed (kittystate > 2); reproduce that with the dedicated kittyTamed flag.
        this.collar.visible = state.kittyTamed;
        // Whiskers were only ever drawn in the special legacy kittystate == 12 pose (a held/lifted state
        // that has no equivalent in the new FSM and no driving flag in MoCEntityRenderState), so keep the
        // opaque whiskers box hidden — it would otherwise sit at z=-4.1 directly in front of the eyes.
        // (Legacy held/lifted pose is intentionally not ported; see class javadoc.)
        this.whiskers.visible = false;

        // Head tracking: the legacy renderer rotated the head cube by the look angles and then
        // copied those angles onto every other head part (headParts[0..8]).
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;

        this.head.yRot = headYaw;
        this.head.xRot = headPitch;
        ModelPart[] headParts = {this.earRight, this.earLeft, this.browRight, this.browLeft,
                this.cheekRight, this.cheekLeft, this.nose, this.collar, this.whiskers};
        for (ModelPart part : headParts) {
            part.yRot = headYaw;
            part.xRot = headPitch;
        }

        // A sitting/curled pose is used both when explicitly sitting and when asleep (kittyState 3).
        boolean sitting = state.kittySitting || state.kittyState == 3;

        if (sitting) {
            // ---- Legacy sitting pose (MoCModelKitty.render, isSitting branch) ----
            // The kitty drops onto its haunches: all four limbs fold forward flat under the body
            // (rotateAngleX = -pi/2) and the hind legs splay out very slightly (+/-0.1 rad yaw),
            // while the tail curls up over the back (rotateAngleX = -2.3, no z-sway).
            float fold = -1.570796F;      // legacy f6 = -pi/2
            this.rightArm.xRot = fold;
            this.leftArm.xRot = fold;
            this.rightLeg.xRot = fold;
            this.leftLeg.xRot = fold;
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.rightLeg.yRot = 0.1F;
            this.leftLeg.yRot = -0.1F;

            this.tail.xRot = -2.3F;       // curled up over the back
            this.tail.zRot = 0.0F;
            return;
        }

        // Walking gait (legacy setRotationAngles).
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 2.0F * limbAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbAmount * 0.5F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;

        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        // Tail droops slightly and sways with the rear gait.
        this.tail.xRot = -0.5F;
        this.tail.zRot = this.leftLeg.xRot * 0.625F;
    }
}
