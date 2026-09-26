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
 * Bunny model, converted from the legacy {@code MoCModelBunny} ({@code ModelBase}). Every box, texture offset
 * and rotation point, and the legacy leg gait, are kept verbatim. What changed is where the parts hang: they
 * all hang from one {@code hop_pivot} at the ground under the bunny, so the model itself can do the two jobs
 * legacy gave the bunny's RENDERER, which the port had lost. Together with the missing hop impulse on the
 * entity, those losses are why a port bunny "just glides".
 *
 * <p><b>Feet on the ground.</b> Legacy's leg boxes end at y = 21 (rotation point {@code 3 + byte0} = 19,
 * two pixels tall). The renderer draws model y = 24 at the entity's feet (mc262-ref
 * {@code LivingEntityRenderer.submit}:87-90: {@code scale(-1, -1, 1)}, then {@code translate(0, -1.501, 0)}),
 * so drawn as built, the bunny floats three pixels up. Legacy never drew it that way. 12.0.5's
 * {@code MoCRenderBunny.preRenderCallback}:36-39 lowered it by {@code getAdjustedYOffset()} = 0.2 blocks
 * ({@code MoCEntityBunny}:279-281; after the flip, +Y is down), 3.2 px, which puts the feet flush on the
 * ground. 5.1.5's {@code preRenderCallback}:41-52 lowered it 0.1 whenever the bunny was not riding. The port
 * applied neither, so every bunny hovered 3/16 of a block over the grass as it slid along. Here the pivot
 * sits on the ground line and every legacy rotation point is re-expressed relative to legacy's feet line
 * ({@link #legacyPose}), which lowers the rest pose by exactly 3 px.</p>
 *
 * <p><b>The bound.</b> Legacy {@code MoCRenderBunny.rotBunny} (12.0.5:41-51, 5.1.5:54-71) pitched the whole
 * airborne bunny by {@code glRotatef(motionY * 70, -1, 0, 0)}, clamped to ±35°: nose up on the way up, nose
 * down on the way down. It ran from {@code preRenderCallback}, after 1.12's {@code scale(-1, -1, 1)} and
 * before its {@code translate(0, -1.501, 0)}, so it turned the bunny about its feet. Carried through the flip,
 * that is a model-space {@code xRot} of {@code -angle} about the ground line, which is exactly
 * {@code hop_pivot}. The angle can no longer come from {@code motionY}: a 26.2 client never integrates a
 * remote mob's velocity, it only replays interpolated positions, so the vector is not a live number there. It
 * follows the hop clock instead ({@code MoCEntityRenderState.bunnyHop}, from
 * {@code MoCEntityBunny.getHopCompletion}), the way the vanilla rabbit's model follows
 * {@code jumpCompletion}. The clock's rising half gets the nose-up tilt and its falling half the nose-down
 * one, at legacy's own launch angle. The legs add a leap pose legacy did not have. The hind feet swing
 * toe-down through the flight, and the front paws sweep back on the rise and reach forward for the
 * landing. The legacy gait fades out as the bound peaks.</p>
 *
 * <p><b>Carried.</b> A bunny on its carrier's head keeps legacy's un-lowered placement, since
 * {@code MoCAnimal.carriedHeadSink} was tuned against it (and 5.1.5 also skipped its offset while riding). It
 * never pitches, matching rotBunny's {@code getRidingEntity() == null} gate, and its legs stay still. 12.0.5
 * skipped the leg gait for a "bunny hat" ({@code MoCModelBunny}:65, 94-99), and that matters more here: the
 * carried bunny is not a passenger, so it still picks up a walk animation from its carrier's steps (mc262-ref
 * {@code LivingEntity.calculateEntityAnimation}:2642-2649).</p>
 */
public class MoCModelBunny extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float HALF_PI = (float) (Math.PI / 2.0);
    private static final float PI = (float) Math.PI;

    /** The ground line in model space: the renderer draws model y = 24 at the entity's feet. */
    private static final float GROUND_Y = 24.0F;
    /** Bottom of the legacy leg boxes: rotation point {@code 3 + byte0} = 19, plus the boxes' 2 px height. */
    private static final float LEGACY_FEET_Y = 21.0F;
    /**
     * Peak body pitch of a bound, in degrees. Legacy tilted by {@code motionY * 70}, and a 12.0.5 bound leaves
     * the ground at 0.3 ({@code MoCEntityBunny.onUpdate}:196) and lands at about -0.33, so legacy's own tilt
     * at launch and landing was 21-23°. Its ±35° clamp only ever caught a thrown bunny.
     */
    private static final float HOP_PITCH_DEGREES = 21.0F;
    /** Hind feet toe-down swing at the top of a bound (legs stretched through the flight). */
    private static final float HOP_HIND_LEG_DEGREES = 50.0F;
    /** Front paws: swept back while rising, reaching forward for the landing. */
    private static final float HOP_FRONT_LEG_DEGREES = 40.0F;

    private final ModelPart hopPivot;
    private final ModelPart head;
    private final ModelPart earLeft;
    private final ModelPart earRight;
    private final ModelPart cheekLeft;
    private final ModelPart cheekRight;
    private final ModelPart frontLegRight;
    private final ModelPart frontLegLeft;
    private final ModelPart backLegRight;
    private final ModelPart backLegLeft;

    public MoCModelBunny(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.hopPivot = root.getChild("hop_pivot");
        this.head = this.hopPivot.getChild("head");
        this.earLeft = this.hopPivot.getChild("ear_left");
        this.earRight = this.hopPivot.getChild("ear_right");
        this.cheekLeft = this.hopPivot.getChild("cheek_left");
        this.cheekRight = this.hopPivot.getChild("cheek_right");
        this.frontLegRight = this.hopPivot.getChild("front_leg_right");
        this.frontLegLeft = this.hopPivot.getChild("front_leg_left");
        this.backLegRight = this.hopPivot.getChild("back_leg_right");
        this.backLegLeft = this.hopPivot.getChild("back_leg_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The pivot everything hangs from: the ground under the bunny's centre. It is both the rest pose's
        // ground contact and the point legacy rotBunny pitched the whole bunny about.
        PartDefinition hop = root.addOrReplaceChild("hop_pivot", CubeListBuilder.create(),
                PartPose.offset(0.0F, GROUND_Y, 0.0F));

        // legacy used a y-offset of byte0 = 16 added to every setRotationPoint y; the y values below are
        // those legacy rotation points verbatim, made pivot-relative by legacyPose
        hop.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 4.0F, 6.0F),
                legacyPose(0.0F, 15.0F, -4.0F, 0.0F));
        hop.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(14, 0).addBox(-2.0F, -5.0F, -3.0F, 1.0F, 4.0F, 2.0F),
                legacyPose(0.0F, 15.0F, -4.0F, 0.0F));
        hop.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(14, 0).addBox(1.0F, -5.0F, -3.0F, 1.0F, 4.0F, 2.0F),
                legacyPose(0.0F, 15.0F, -4.0F, 0.0F));
        hop.addOrReplaceChild("cheek_left",
                CubeListBuilder.create().texOffs(20, 0).addBox(-4.0F, 0.0F, -3.0F, 2.0F, 3.0F, 2.0F),
                legacyPose(0.0F, 15.0F, -4.0F, 0.0F));
        hop.addOrReplaceChild("cheek_right",
                CubeListBuilder.create().texOffs(20, 0).addBox(2.0F, 0.0F, -3.0F, 2.0F, 3.0F, 2.0F),
                legacyPose(0.0F, 15.0F, -4.0F, 0.0F));
        // body and tail carry a fixed pi/2 pitch (legacy set this every frame)
        hop.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 10).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F),
                legacyPose(0.0F, 16.0F, 0.0F, HALF_PI));
        hop.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                legacyPose(0.0F, 16.0F, 0.0F, HALF_PI));
        hop.addOrReplaceChild("front_leg_right",
                CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                legacyPose(3.0F, 19.0F, -3.0F, 0.0F));
        hop.addOrReplaceChild("front_leg_left",
                CubeListBuilder.create().texOffs(24, 16).addBox(0.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                legacyPose(-3.0F, 19.0F, -3.0F, 0.0F));
        hop.addOrReplaceChild("back_leg_right",
                CubeListBuilder.create().texOffs(16, 24).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                legacyPose(3.0F, 19.0F, 4.0F, 0.0F));
        hop.addOrReplaceChild("back_leg_left",
                CubeListBuilder.create().texOffs(16, 24).addBox(0.0F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                legacyPose(-3.0F, 19.0F, 4.0F, 0.0F));

        // Back faces 1.12 drew with culling off (see MoCBackFaces): the whiskers are painted on the front face of
        // each cheek box only, so from behind and the sides they were seen through the empty faces. Each twin is the
        // named face of that part's box turned to face into the box, with the same corners and texels.
        MoCBackFaces.add(root, "hop_pivot/cheek_left", CubeListBuilder.create()
                .texOffs(20, 2).mirror(true).addBox(-4.0F, 0.0F, -3.0F, 2.0F, 3.0F, 0.0F, MoCBackFaces.SOUTH)); // back of the north face
        MoCBackFaces.add(root, "hop_pivot/cheek_right", CubeListBuilder.create()
                .texOffs(20, 2).mirror(true).addBox(2.0F, 0.0F, -3.0F, 2.0F, 3.0F, 0.0F, MoCBackFaces.SOUTH)); // back of the north face

        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * A legacy rotation point (with legacy's {@code byte0 = 16} already folded into {@code y}) re-expressed
     * relative to {@code hop_pivot}. At rest the pivot sits at {@link #GROUND_Y}, so the part lands
     * {@code GROUND_Y - LEGACY_FEET_Y} = 3 px below where legacy built it, which puts the feet on the ground.
     */
    private static PartPose legacyPose(float x, float y, float z, float xRot) {
        return PartPose.offsetAndRotation(x, y - LEGACY_FEET_Y, z, xRot, 0.0F, 0.0F);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;

        // head + ears + cheeks track the look direction (legacy negates the pitch)
        this.head.xRot = -headPitch;
        this.head.yRot = headYaw;
        this.earLeft.xRot = -headPitch;
        this.earLeft.yRot = headYaw;
        this.earRight.xRot = -headPitch;
        this.earRight.yRot = headYaw;
        this.cheekLeft.xRot = -headPitch;
        this.cheekLeft.yRot = headYaw;
        this.cheekRight.xRot = -headPitch;
        this.cheekRight.yRot = headYaw;

        if (state.riding) {
            // Carried (or riding): legacy's un-lowered placement, level, and no leg motion at all.
            this.hopPivot.y = LEGACY_FEET_Y;
            return;
        }

        // Hop phase p runs 0..1 over one bound, and is 0 when not hopping. "airborne" is 0 at take-off and
        // touch-down and 1 mid-flight. "climb" is positive through the rising half and negative through the
        // falling half, the sign of the motionY that legacy rotBunny read.
        float p = Mth.clamp(state.bunnyHop, 0.0F, 1.0F);
        float airborne = Mth.sin(p * PI);
        float climb = Mth.sin(p * 2.0F * PI);

        // Nose up on the way up and down on the way down, turning about the feet (legacy rotBunny). In model
        // space +Y points down and the head lies toward -Z, so a negative xRot lifts the nose.
        this.hopPivot.xRot = -HOP_PITCH_DEGREES * climb * DEG_TO_RAD;

        // Legacy gait: both front legs in phase, both back legs opposite at 1.2x. It fades out as the bound
        // peaks so the legs hold the leap pose in the air.
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed * (1.0F - airborne);
        float frontGait = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbAmount;
        float backGait = Mth.cos(limbSwing * 0.6662F + PI) * 1.2F * limbAmount;
        // A positive xRot swings a part that hangs below its pivot backward. On the hind feet, which lie
        // forward of their heel pivot, it tips the toe down.
        float frontHop = HOP_FRONT_LEG_DEGREES * climb * DEG_TO_RAD;
        float backHop = HOP_HIND_LEG_DEGREES * airborne * DEG_TO_RAD;
        this.frontLegRight.xRot = frontGait + frontHop;
        this.frontLegLeft.xRot = frontGait + frontHop;
        this.backLegRight.xRot = backGait + backHop;
        this.backLegLeft.xRot = backGait + backHop;
    }
}
