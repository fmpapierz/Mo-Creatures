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
 * Minotaur: a bipedal bull-man on a 128x128 sheet, built new for the port (no legacy model existed —
 * the mob was never released upstream). Broad humanoid torso with a rear neck hump, bull head with a
 * hanging muzzle, small bovine ears and two three-segment horns that sweep out, up and slightly
 * forward/inward; thick digitigrade-flavoured legs (thigh, rear-set hock, wide forward-set hoof);
 * heavy shoulders with hanging fists; short tail with a hair tuft.
 *
 * <p>Structure notes:</p>
 * <ul>
 *   <li>The horns are real children of {@code Head} (base -&gt; tip -&gt; point chains), so they inherit
 *       the look rotation and the attack horn-toss for free. Their curve comes from baked pivot
 *       rotations, which {@code Model.setupAnim}'s pose reset restores every frame — nothing in
 *       {@link #setupAnim} touches them.</li>
 *   <li>Rendering is back-face culled ({@code entityCutoutCull}): every box is at least 1px on every axis, and no two
 *       rest-pose faces are coplanar where they overlap (joints embed by fractional offsets — e.g.
 *       the upper arm sinks 0.25px into the chest flank, the hoof is wider than the hock).</li>
 *   <li>The digitigrade read is done purely with offset child boxes (hock shifted back, hoof shifted
 *       forward), not baked leg rotations, so the walking gait only ever drives the thigh pivots and
 *       the hooves always land exactly on the ground plane (y = 24).</li>
 * </ul>
 *
 * <p>Rest-pose extents: hooves at y 24, skull top at y -19 (2.7 blocks — matches the 1.4 x 2.6
 * hitbox), horn points to roughly y -24 above it; fists reach x +/-13.25, muzzle front at z -13.5.</p>
 */
public class MoCModelMinotaur extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart rgtArm;
    private final ModelPart lftArm;
    private final ModelPart rgtThigh;
    private final ModelPart lftThigh;
    private final ModelPart tail;

    public MoCModelMinotaur(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("Head");
        this.torso = root.getChild("Torso");
        this.rgtArm = root.getChild("RgtArm");
        this.lftArm = root.getChild("LftArm");
        this.rgtThigh = root.getChild("RgtThigh");
        this.lftThigh = root.getChild("LftThigh");
        this.tail = root.getChild("Tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- Head: skull + muzzle + both ears in one part; the horns hang off it as children so
        // they follow the look direction and the horn-toss. Pivot at the neck (0, -13, -1).
        PartDefinition head = root.addOrReplaceChild("Head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -6.0F, -8.0F, 8.0F, 8.0F, 8.0F)     // skull
                        .texOffs(34, 0).addBox(-3.0F, -2.0F, -12.5F, 6.0F, 5.0F, 5.0F)   // muzzle (hangs 1px below the skull as the jaw)
                        .texOffs(58, 0).addBox(-7.5F, -4.5F, -4.5F, 4.0F, 2.0F, 1.0F)    // right ear (inner end embedded 0.5px in the skull)
                        .texOffs(58, 4).addBox(3.5F, -4.5F, -4.5F, 4.0F, 2.0F, 1.0F),    // left ear
                PartPose.offset(0.0F, -13.0F, -1.0F));

        // Horns: base juts sideways from the skull top (outer end rotated up 20deg), the tip stands
        // up from the base's end leaning 10deg back inward and 15deg forward, and a slim point box
        // continues the curve. All rotations are baked poses; the boxes never touch setupAnim.
        PartDefinition rgtHornBase = head.addOrReplaceChild("RgtHornBase",
                CubeListBuilder.create().texOffs(70, 0).addBox(-4.5F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.5F, -6.0F, -4.0F, 0.0F, 0.0F, 0.3490659F));
        PartDefinition rgtHornTip = rgtHornBase.addOrReplaceChild("RgtHornTip",
                CubeListBuilder.create().texOffs(86, 0).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, -1.0F, 0.0F, 0.2617994F, 0.0F, -0.1745329F));
        rgtHornTip.addOrReplaceChild("RgtHornPoint",
                CubeListBuilder.create().texOffs(106, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.1745329F, 0.0F, -0.1745329F));
        PartDefinition lftHornBase = head.addOrReplaceChild("LftHornBase",
                CubeListBuilder.create().texOffs(70, 5).addBox(-0.5F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.5F, -6.0F, -4.0F, 0.0F, 0.0F, -0.3490659F));
        PartDefinition lftHornTip = lftHornBase.addOrReplaceChild("LftHornTip",
                CubeListBuilder.create().texOffs(96, 0).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, -1.0F, 0.0F, 0.2617994F, 0.0F, 0.1745329F));
        lftHornTip.addOrReplaceChild("LftHornPoint",
                CubeListBuilder.create().texOffs(112, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.1745329F, 0.0F, 0.1745329F));

        // ---- Torso: broad chest slab plus the rear neck hump in one part, so the idle breathing
        // bob moves both together. The hump is rear-biased and pokes 0.5px past the chest's back.
        root.addOrReplaceChild("Torso", CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-9.0F, 0.0F, -5.5F, 18.0F, 11.0F, 11.0F)  // chest (y -13..-2)
                        .texOffs(60, 17).addBox(-5.0F, -2.75F, -2.0F, 10.0F, 5.0F, 8.0F), // neck hump (y -15.75..-10.75)
                PartPose.offset(0.0F, -13.0F, 0.0F));

        // Belly: narrower than the chest, overlaps it by 1px above and swallows the thigh tops below.
        root.addOrReplaceChild("Belly",
                CubeListBuilder.create().texOffs(0, 40).addBox(-7.5F, 0.0F, -4.5F, 15.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, -3.0F, 0.0F));

        // ---- Arms: one part each (shoulder bulge + upper arm + fist) pivoted at the shoulder so
        // the whole limb swings as a unit. The upper arm sinks 0.25px into the chest flank; the fist
        // overlaps the arm's lower end by 0.5px (no coplanar faces at the wrist).
        root.addOrReplaceChild("RgtArm", CubeListBuilder.create()
                        .texOffs(98, 17).addBox(-3.5F, -2.5F, -3.5F, 5.0F, 7.0F, 7.0F)   // shoulder bulge
                        .texOffs(50, 40).addBox(-2.75F, 2.5F, -2.5F, 4.0F, 9.0F, 5.0F)   // upper arm
                        .texOffs(98, 47).addBox(-3.25F, 11.0F, -3.0F, 5.0F, 5.0F, 6.0F), // fist
                PartPose.offset(-10.0F, -11.0F, 0.5F));
        root.addOrReplaceChild("LftArm", CubeListBuilder.create()
                        .texOffs(98, 32).addBox(-1.5F, -2.5F, -3.5F, 5.0F, 7.0F, 7.0F)
                        .texOffs(70, 40).addBox(-1.25F, 2.5F, -2.5F, 4.0F, 9.0F, 5.0F)
                        .texOffs(98, 59).addBox(-1.75F, 11.0F, -3.0F, 5.0F, 5.0F, 6.0F),
                PartPose.offset(10.0F, -11.0F, 0.5F));

        // ---- Legs: thigh -> hock -> hoof chains. The hock box is set back and the hoof set forward
        // for the digitigrade silhouette; only the thigh pivot is animated, so the hoof soles always
        // sit exactly on y = 24. The hoof is 1px wider than the hock on each side (fetlock flare).
        PartDefinition rgtThigh = root.addOrReplaceChild("RgtThigh",
                CubeListBuilder.create().texOffs(0, 58).addBox(-3.0F, 0.0F, -3.5F, 6.0F, 9.0F, 7.0F),
                PartPose.offset(-4.0F, 2.0F, 0.75F));
        PartDefinition rgtHock = rgtThigh.addOrReplaceChild("RgtHock",
                CubeListBuilder.create().texOffs(56, 58).addBox(-2.5F, -1.0F, -3.0F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 9.0F, 1.75F));
        rgtHock.addOrReplaceChild("RgtHoof",
                CubeListBuilder.create().texOffs(0, 75).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 7.0F, -1.5F));
        PartDefinition lftThigh = root.addOrReplaceChild("LftThigh",
                CubeListBuilder.create().texOffs(28, 58).addBox(-3.0F, 0.0F, -3.5F, 6.0F, 9.0F, 7.0F),
                PartPose.offset(4.0F, 2.0F, 0.75F));
        PartDefinition lftHock = lftThigh.addOrReplaceChild("LftHock",
                CubeListBuilder.create().texOffs(78, 58).addBox(-2.5F, -1.0F, -3.0F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 9.0F, 1.75F));
        lftHock.addOrReplaceChild("LftHoof",
                CubeListBuilder.create().texOffs(24, 75).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 7.0F, -1.5F));

        // ---- Tail: short rope angled 30deg back from the rump, with the tuft child folding most of
        // the way back to vertical so it hangs. Rotated at rest, so nothing here can be coplanar.
        PartDefinition tail = root.addOrReplaceChild("Tail",
                CubeListBuilder.create().texOffs(118, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 3.5F, 4.0F, 0.5235988F, 0.0F, 0.0F));
        tail.addOrReplaceChild("TailTuft",
                CubeListBuilder.create().texOffs(104, 8).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.3490659F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        // Head tracks the look, plus the horn-toss on attack: sin(2*pi*t) over the swing dips the
        // head down through the first half and snaps it up past neutral in the second — a gore-and-
        // fling. The horns are children of the head and ride along.
        float toss = Mth.sin(state.attackSwing * ((float) Math.PI * 2.0F)) * 0.7F;
        this.head.xRot = headPitch + toss;
        this.head.yRot = headYaw;

        // Heavy biped gait: the whole leg swings from the hip; hock and hoof keep their baked
        // offsets, so the digitigrade shape is preserved through the stride.
        float rgtLeg = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.8F * limbAmount;
        float lftLeg = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;
        this.rgtThigh.xRot = rgtLeg;
        this.lftThigh.xRot = lftLeg;

        // Arms counter-swing the legs; the attack hurls both fists forward-up alongside the horn
        // toss. The zRot holds the arms slightly out from the flanks and adds the breathing sway.
        float smash = Mth.sin(state.attackSwing * (float) Math.PI); // 0 at rest, 1 mid-swing
        float breath = Mth.cos(ageInTicks * 0.09F) * 0.05F;
        this.rgtArm.xRot = lftLeg * 0.7F - smash * 1.6F;
        this.lftArm.xRot = rgtLeg * 0.7F - smash * 1.6F;
        this.rgtArm.zRot = 0.06F + breath;
        this.lftArm.zRot = -0.06F - breath;

        // Idle breathing: the chest + hump slab rises and falls a fraction of a pixel. The belly is
        // static and overlaps the chest by 1px, so the bob never opens a seam.
        this.torso.y += Mth.sin(ageInTicks * 0.08F) * 0.3F;

        // Tail sway: slow idle swish plus a walk-driven flick; the tuft follows its parent.
        this.tail.zRot = Mth.cos(ageInTicks * 0.1F) * 0.08F
                + Mth.cos(limbSwing * 0.6662F) * 0.2F * limbAmount;
    }
}
