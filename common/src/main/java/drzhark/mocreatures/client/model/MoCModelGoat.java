package drzhark.mocreatures.client.model;

import java.util.Set;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Goat model, converted faithfully from the legacy {@code MoCModelGoat} ({@code ModelBase}).
 * Geometry, texture offsets and the walking gait / head tracking are preserved. Type/age-dependent
 * visibility of horns, udder and goatee (legacy render()) is reproduced in {@link #setupAnim}: does
 * (types 2-4) show an udder + small horns; adult bucks (types 5+) grow the full horn set + goatee; kids
 * are hornless.
 */
public class MoCModelGoat extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // Face selectors for the flat (zero-height) tongue: only its DOWN tile is painted on the sheet,
    // so under the culled render type the box is split into a painted face plus an opposite face
    // re-aimed (via a shifted texOffs) at the same painted tile.
    private static final Set<Direction> DOWN_FACE = Set.of(Direction.DOWN);
    private static final Set<Direction> UP_FACE = Set.of(Direction.UP);

    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart lEar;
    private final ModelPart rEar;
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart tongue;
    private final ModelPart mouth;
    private final ModelPart rHorn1;
    private final ModelPart rHorn2;
    private final ModelPart rHorn3;
    private final ModelPart rHorn4;
    private final ModelPart rHorn5;
    private final ModelPart lHorn1;
    private final ModelPart lHorn2;
    private final ModelPart lHorn3;
    private final ModelPart lHorn4;
    private final ModelPart lHorn5;
    private final ModelPart goatie;
    private final ModelPart neck;
    private final ModelPart tits;

    public MoCModelGoat(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.lEar = root.getChild("l_ear");
        this.rEar = root.getChild("r_ear");
        this.head = root.getChild("head");
        this.nose = root.getChild("nose");
        this.tongue = root.getChild("tongue");
        this.mouth = root.getChild("mouth");
        this.rHorn1 = root.getChild("r_horn1");
        this.rHorn2 = root.getChild("r_horn2");
        this.rHorn3 = root.getChild("r_horn3");
        this.rHorn4 = root.getChild("r_horn4");
        this.rHorn5 = root.getChild("r_horn5");
        this.lHorn1 = root.getChild("l_horn1");
        this.lHorn2 = root.getChild("l_horn2");
        this.lHorn3 = root.getChild("l_horn3");
        this.lHorn4 = root.getChild("l_horn4");
        this.lHorn5 = root.getChild("l_horn5");
        this.goatie = root.getChild("goatie");
        this.neck = root.getChild("neck");
        this.tits = root.getChild("tits");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(2.0F, 17.0F, -6.0F));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(-2.0F, 17.0F, -6.0F));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(-2.0F, 17.0F, 6.0F));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(2.0F, 17.0F, 6.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 8).addBox(-3.0F, -4.0F, -8.0F, 6.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(22, 8).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 10.0F, 8.0F));
        root.addOrReplaceChild("l_ear",
                CubeListBuilder.create().texOffs(52, 8).addBox(1.5F, -2.0F, 0.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("r_ear",
                CubeListBuilder.create().texOffs(52, 8).addBox(-3.5F, -2.0F, 0.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(52, 16).addBox(-1.5F, -2.0F, -2.0F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(52, 10).addBox(-1.5F, -1.0F, -5.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("tongue",
                CubeListBuilder.create()
                        .texOffs(56, 5).addBox(-0.5F, 2.0F, -5.0F, 1.0F, 0.0F, 3.0F, DOWN_FACE)
                        .texOffs(55, 5).addBox(-0.5F, 2.0F, -5.0F, 1.0F, 0.0F, 3.0F, UP_FACE),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(54, 0).addBox(-1.0F, 2.0F, -5.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("r_horn1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3.0F, -0.7F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("r_horn2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.9F, -4.0F, -0.2F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("r_horn3",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.1F, -4.8F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("r_horn4",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.3F, -5.2F, 1.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("r_horn5",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.6F, -4.9F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.15F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("l_horn1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.5F, -3.0F, -0.7F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("l_horn2",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.9F, -4.0F, -0.2F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("l_horn3",
                CubeListBuilder.create().texOffs(0, 0).addBox(1.2F, -4.9F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("l_horn4",
                CubeListBuilder.create().texOffs(0, 0).addBox(1.4F, -5.3F, 1.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("l_horn5",
                CubeListBuilder.create().texOffs(0, 0).addBox(1.7F, -4.9F, 2.1F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.15F)),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("goatie",
                CubeListBuilder.create().texOffs(52, 5).addBox(-0.5F, 3.0F, -4.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 8.0F, -12.0F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(18, 14).addBox(-1.5F, -2.0F, -5.0F, 3.0F, 4.0F, 6.0F, new CubeDeformation(-0.2F)),
                PartPose.offsetAndRotation(0.0F, 11.0F, -8.0F, -24.0F / 57.29578F, 0.0F, 0.0F));
        root.addOrReplaceChild("tits",
                CubeListBuilder.create().texOffs(18, 0).addBox(-2.5F, 0.0F, -2.0F, 5.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 3.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // Type/age-gated features (legacy render(): udder on does 2-4; horns 1-2 on adult goats > type 1;
        // horns 3-5 + goatee on adult bucks > type 4; kids are hornless).
        int t = state.typeMoC;
        boolean adult = state.adult;
        boolean smallHorns = t > 1 && adult;
        boolean bigHorns = t > 4 && adult;
        this.tits.visible = t > 1 && t < 5;
        this.rHorn1.visible = smallHorns;
        this.rHorn2.visible = smallHorns;
        this.lHorn1.visible = smallHorns;
        this.lHorn2.visible = smallHorns;
        this.rHorn3.visible = bigHorns;
        this.rHorn4.visible = bigHorns;
        this.rHorn5.visible = bigHorns;
        this.lHorn3.visible = bigHorns;
        this.lHorn4.visible = bigHorns;
        this.lHorn5.visible = bigHorns;
        this.goatie.visible = bigHorns;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;

        this.leg1.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        this.leg2.xRot = Mth.cos((limbSwing * 0.6662F) + 3.141593F) * 1.4F * limbAmount;
        this.leg3.xRot = Mth.cos((limbSwing * 0.6662F) + 3.141593F) * 1.4F * limbAmount;
        this.leg4.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;

        float baseAngle = (30.0F * DEG_TO_RAD) + (headPitch * DEG_TO_RAD);

        if (netHeadYaw > 20.0F) {
            netHeadYaw = 20.0F;
        }
        if (netHeadYaw < -20.0F) {
            netHeadYaw = -20.0F;
        }
        this.head.yRot = netHeadYaw * DEG_TO_RAD;
        this.neck.xRot = -30.0F * DEG_TO_RAD;
        this.tail.xRot = 90.0F * DEG_TO_RAD;

        this.head.xRot = baseAngle;

        this.lEar.xRot = baseAngle;
        this.rEar.xRot = baseAngle;

        this.nose.xRot = this.head.xRot;
        this.mouth.xRot = this.head.xRot;
        this.tongue.xRot = this.head.xRot;
        this.goatie.xRot = this.head.xRot;
        this.rHorn1.xRot = this.head.xRot;
        this.lHorn1.xRot = this.head.xRot;
        this.rHorn2.xRot = this.head.xRot;
        this.lHorn2.xRot = this.head.xRot;
        this.rHorn3.xRot = this.head.xRot;
        this.lHorn3.xRot = this.head.xRot;
        this.rHorn4.xRot = this.head.xRot;
        this.lHorn4.xRot = this.head.xRot;
        this.rHorn5.xRot = this.head.xRot;
        this.lHorn5.xRot = this.head.xRot;

        this.nose.yRot = this.head.yRot;
        this.mouth.yRot = this.head.yRot;
        this.tongue.yRot = this.head.yRot;
        this.lEar.yRot = this.head.yRot;
        this.rEar.yRot = this.head.yRot;
        this.goatie.yRot = this.head.yRot;
        this.rHorn1.yRot = this.head.yRot;
        this.lHorn1.yRot = this.head.yRot;
        this.rHorn2.yRot = this.head.yRot;
        this.lHorn2.yRot = this.head.yRot;
        this.rHorn3.yRot = this.head.yRot;
        this.lHorn3.yRot = this.head.yRot;
        this.rHorn4.yRot = this.head.yRot;
        this.lHorn4.yRot = this.head.yRot;
        this.rHorn5.yRot = this.head.yRot;
        this.lHorn5.yRot = this.head.yRot;
    }
}
