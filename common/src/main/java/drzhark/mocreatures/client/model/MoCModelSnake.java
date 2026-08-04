package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Snake model, converted faithfully from the legacy {@code MoCModelSnake} ({@code ModelBase}).
 *
 * <p>The legacy model built a {@code bodySnake[40]} array of segments procedurally and animated a
 * travelling sine wave through GL11 matrix pushes in {@code render()}; that per-segment matrix work
 * cannot be expressed through the modern {@link PartPose}/{@link ModelPart} pipeline, so the side
 * wave is dropped. The discrete geometry (body segments, head, nose, tongue, teeth, tail) is preserved
 * and the body still travels a per-segment sine yaw wave with the head tracking the look direction. The
 * type-6 cobra hood flare IS ported (the Wing1..5 L/R hood cubes, shown when {@code state.snakeHoodFlared}
 * — a cobra rearing at a nearby player). The type-7 rattlesnake rattle IS ported: the {@code tail} cube is
 * shown only for a rattlesnake and raised toward vertical with a fast shake (legacy {@code Tail.rotateAngleX}).
 * A subtle jaw split and tongue flick are ported from the legacy {@code getfMouth()}/{@code getfTongue()}
 * poses, synthesised from {@code ageInTicks}. NOT ported: the near-player head-rearing lift (that legacy pose
 * was driven by per-segment GL matrix work).
 */
public class MoCModelSnake extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = 1F / 57.29578F;
    private static final int BODYPARTS = 40;

    private final ModelPart[] bodySnake = new ModelPart[BODYPARTS];
    /** Cobra-hood cubes flanking the neck (legacy Wing1L..5L / Wing1R..5R); shown flared for a type-6 cobra. */
    private final ModelPart[] hoodL = new ModelPart[5];
    private final ModelPart[] hoodR = new ModelPart[5];
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart lNose;
    private final ModelPart teethUR;
    private final ModelPart teethUL;
    private final ModelPart tongue;
    private final ModelPart tongue1;
    private final ModelPart tongue0;
    private final ModelPart tail;

    public MoCModelSnake(ModelPart root) {
        super(root);
        for (int i = 0; i < BODYPARTS; i++) {
            this.bodySnake[i] = root.getChild("body" + i);
        }
        for (int i = 0; i < 5; i++) {
            this.hoodL[i] = root.getChild("hood_l" + (i + 1));
            this.hoodR[i] = root.getChild("hood_r" + (i + 1));
        }
        this.head = root.getChild("head");
        this.nose = root.getChild("nose");
        this.lNose = root.getChild("l_nose");
        this.teethUR = root.getChild("teeth_ur");
        this.teethUL = root.getChild("teeth_ul");
        this.tongue = root.getChild("tongue");
        this.tongue1 = root.getChild("tongue1");
        this.tongue0 = root.getChild("tongue0");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        final float fsegm = 1F / 8F;
        final float fsep = -1.6F;
        float flength = 0.0F;

        for (int i = 0; i < BODYPARTS; i++) {
            flength = ((BODYPARTS / 2) - i) * fsep;

            float factor;
            float fport = ((i + 1F) / BODYPARTS);
            if (fport < fsegm) {
                factor = -0.20F;
            } else if (fport < (fsegm * 2F)) {
                factor = -0.15F;
            } else if (fport < (fsegm * 4F)) {
                factor = 0.0F;
            } else if (fport < (fsegm * 6F)) {
                factor = 0.0F;
            } else if (fport < (fsegm * 7F)) {
                factor = -0.15F;
            } else {
                factor = -0.2F;
            }

            int j = (i % 2 == 0) ? 0 : 4;
            root.addOrReplaceChild("body" + i,
                    CubeListBuilder.create().texOffs(8, j)
                            .addBox(-1F, -0.5F, 0F, 2, 2, 2, new CubeDeformation(factor)),
                    PartPose.offset(0F, 23F, flength));
        }

        // Cobra hood cubes (legacy Wing1..5 L/R): 2x2x2 cubes flanking the neck at the body1..5 positions,
        // their baked X offsets forming the flared hood shape. Hidden unless a type-6 cobra rears at a player.
        float[] hoodZ = new float[5];
        for (int n = 1; n <= 5; n++) {
            hoodZ[n - 1] = ((BODYPARTS / 2) - n) * fsep;
        }
        // Left/right baked X offsets per pair (from the legacy addBox origins).
        float[] lx = {0.0F, 0.5F, 1.0F, 0.5F, 0.0F};
        float[] rx = {-2.0F, -2.5F, -3.0F, -2.5F, -2.0F};
        int[][] uv = {{8, 4}, {8, 4}, {16, 4}, {16, 8}, {16, 8}};
        for (int n = 0; n < 5; n++) {
            root.addOrReplaceChild("hood_l" + (n + 1),
                    CubeListBuilder.create().texOffs(uv[n][0], uv[n][1]).addBox(lx[n], -0.5F, 0F, 2, 2, 2),
                    PartPose.offset(0F, 23F, hoodZ[n]));
            root.addOrReplaceChild("hood_r" + (n + 1),
                    CubeListBuilder.create().texOffs(uv[n][0], uv[n][1]).addBox(rx[n], -0.5F, 0F, 2, 2, 2),
                    PartPose.offset(0F, 23F, hoodZ[n]));
        }

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 0.5F, -1.0F, 1, 1, 5),
                PartPose.offset(0F, 23F, flength));

        flength = (BODYPARTS / 2) * fsep;

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -0.5F, -2F, 2, 2, 2),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -0.3F, -4F, 1, 1, 2),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("l_nose",
                CubeListBuilder.create().texOffs(22, 0).addBox(-0.5F, 0.3F, -4F, 1, 1, 2),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("teeth_ur",
                CubeListBuilder.create().texOffs(46, 0).addBox(-0.4F, 0.3F, -3.8F, 0, 1, 1),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("teeth_ul",
                CubeListBuilder.create().texOffs(44, 0).addBox(0.4F, 0.3F, -3.8F, 0, 1, 1),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("tongue",
                CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0.5F, -6F, 1, 0, 3),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("tongue1",
                CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0.5F, -5F, 1, 0, 3),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("tongue0",
                CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0.25F, -4F, 1, 0, 3),
                PartPose.offset(0F, 23F, flength));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float rAX = state.xRot * DEG_TO_RAD;
        float rAY = state.yRot * DEG_TO_RAD;

        this.head.xRot = rAX;
        this.head.yRot = rAY;
        this.bodySnake[0].xRot = rAX * 0.95F;
        this.bodySnake[1].xRot = rAX * 0.90F;
        this.bodySnake[2].xRot = rAX * 0.85F;
        this.bodySnake[3].xRot = rAX * 0.80F;
        this.bodySnake[4].xRot = rAX * 0.75F;

        // Mouth split + tongue flick. The legacy model drove these from networked getfMouth()/getfTongue()
        // timers; those aren't carried on the render state, so we synthesise them from ageInTicks:
        //  - fMouth: a small, mostly-closed jaw split that opens periodically (legacy split nose up / lNose down).
        //  - fTongue (f8 = cos(fTongue*10)/40 in legacy): a subtle tongue-tip flick offset.
        float age = state.ageInTicks;
        // periodic gentle jaw open (0..~0.18 rad), spends most of the cycle near closed.
        float fMouth = 0.09F * (1F - Mth.cos(age * 0.18F)) * 0.5F;
        // tongue flick, matching the legacy cos(.)/40 amplitude but on a faster, offset phase so it darts.
        float fTongue = Mth.cos(age * 0.6F) / 40F;

        this.nose.xRot = this.head.xRot - fMouth;
        this.lNose.xRot = this.head.xRot + fMouth;
        this.tongue1.xRot = this.head.xRot + fTongue;
        this.tongue.xRot = this.head.xRot + fTongue;
        this.tongue0.xRot = this.lNose.xRot + fTongue;
        this.teethUR.xRot = this.head.xRot - fMouth;
        this.teethUL.xRot = this.head.xRot - fMouth;

        this.bodySnake[0].yRot = rAY * 0.85F;
        this.bodySnake[1].yRot = rAY * 0.65F;
        this.bodySnake[2].yRot = rAY * 0.45F;
        this.bodySnake[3].yRot = rAY * 0.25F;
        this.bodySnake[4].yRot = rAY * 0.10F;

        this.nose.yRot = this.head.yRot;
        this.tongue.yRot = this.head.yRot;
        this.tongue0.yRot = this.head.yRot;
        this.tongue1.yRot = this.head.yRot;
        this.lNose.yRot = this.head.yRot;
        this.teethUR.yRot = this.head.yRot;
        this.teethUL.yRot = this.head.yRot;

        // travelling side wave from the legacy render() loop, applied as a per-segment yaw
        float w = 1.5F;
        float t = state.walkAnimationPos / 2F;
        for (int i = 5; i < BODYPARTS; i++) {
            this.bodySnake[i].yRot = 0.1F * Mth.sin(w * t - 0.3F * i) * state.walkAnimationSpeed;
        }

        // Cobra hood: shown only when a type-6 cobra rears at a nearby player (legacy nearplayer && typeI==6).
        // Each hood cube tracks its neighbouring neck segment (body1..5) so the flare turns with the head.
        boolean hood = state.snakeHoodFlared;
        for (int n = 0; n < 5; n++) {
            this.hoodL[n].visible = hood;
            this.hoodR[n].visible = hood;
            if (hood) {
                float yr = this.bodySnake[n + 1].yRot;
                float xr = this.bodySnake[n + 1].xRot;
                this.hoodL[n].yRot = yr;
                this.hoodR[n].yRot = yr;
                this.hoodL[n].xRot = xr;
                this.hoodR[n].xRot = xr;
            }
        }

        // Rattle: the legacy model only rendered the Tail cube for a rattlesnake (typeI == 7) and, when
        // it was rattling, raised it toward vertical with a fast ±20° shake:
        //   Tail.rotateAngleX = ((cos(f3 * 10F) * 20F) + 90F) / 57.29578F.
        // The port had made 'tail' an always-on root child, so every snake variant showed a rattle. Gate it
        // to type 7 and drive the raised-and-shaking pose off ageInTicks (no networked rattle timer here).
        boolean rattlesnake = state.typeMoC == 7;
        this.tail.visible = rattlesnake;
        if (rattlesnake) {
            this.tail.xRot = ((Mth.cos(age * 1.4F) * 20F) + 90F) * DEG_TO_RAD;
        }
    }
}
