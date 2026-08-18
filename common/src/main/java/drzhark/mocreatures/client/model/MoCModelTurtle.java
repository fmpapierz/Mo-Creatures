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
 * Turtle model, converted faithfully from the legacy {@code MoCModelTurtle} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the walking leg/tail gait is kept.
 */
public class MoCModelTurtle extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart shell;
    private final ModelPart shellUp;
    private final ModelPart shellTop;
    private final ModelPart belly;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart head;
    private final ModelPart tail;

    public MoCModelTurtle(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.shell = root.getChild("shell");
        this.shellUp = root.getChild("shell_up");
        this.shellTop = root.getChild("shell_top");
        this.belly = root.getChild("belly");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, 0.0F, 0.0F, 9.0F, 1.0F, 9.0F),
                PartPose.offset(-4.5F, 19.0F, -4.5F));
        root.addOrReplaceChild("shell_up",
                CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, 0.0F, 0.0F, 8.0F, 2.0F, 8.0F),
                PartPose.offset(-4.0F, 17.0F, -4.0F));
        root.addOrReplaceChild("shell_top",
                CubeListBuilder.create().texOffs(40, 10).addBox(0.0F, 0.0F, 0.0F, 6.0F, 1.0F, 6.0F),
                PartPose.offset(-3.0F, 16.0F, -3.0F));
        root.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, 0.0F, 0.0F, 8.0F, 1.0F, 8.0F),
                PartPose.offset(-4.0F, 20.0F, -4.0F));
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(3.5F, 20.0F, -3.5F));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-3.5F, 20.0F, -3.5F));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(3.5F, 20.0F, 3.5F));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-3.5F, 20.0F, 3.5F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(10, 0).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 20.0F, -4.5F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 5).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 21.0F, 4.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        if (state.turtleUpsideDown) {
            // Flipped onto its back: legs and tail flail helplessly instead of walking. Legacy drove this
            // from the hurt swing-progress; reproduced deterministically from ageInTicks (not on the state).
            float f26 = Mth.clamp(Mth.sin(state.ageInTicks * 0.3F) * 0.6F, -1.6F, 1.6F);
            this.leg1.xRot = -f26;
            this.leg4.xRot = -f26;
            this.leg2.xRot = f26;
            this.leg3.xRot = f26;
            this.tail.yRot = -f26;
        } else {
            this.leg1.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
            this.leg2.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
            this.leg3.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
            this.leg4.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
            this.tail.yRot = Mth.cos(limbSwing * 0.6662F) * 0.7F * limbAmount;
        }

        if (state.turtleHiding) {
            // Retract head, legs and tail into the shell. Legacy pulls the pivots inward (a translation of
            // the rotation points), NOT a rotation. Head/legs draw pulled up and toward the shell centre.
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.setPos(0.0F, 19.5F, -1.0F);
            this.leg1.setPos(2.9F, 18.5F, -2.9F);
            this.leg2.setPos(-2.9F, 18.5F, -2.9F);
            this.leg3.setPos(2.9F, 18.5F, 2.9F);
            this.leg4.setPos(-2.9F, 18.5F, 2.9F);
            this.tail.setPos(0.0F, 21.0F, 2.0F);
        } else {
            this.head.xRot = state.xRot * DEG_TO_RAD;
            this.head.yRot = state.yRot * DEG_TO_RAD;
            this.head.setPos(0.0F, 20.0F, -4.5F);
            this.leg1.setPos(3.5F, 20.0F, -3.5F);
            this.leg2.setPos(-3.5F, 20.0F, -3.5F);
            this.leg3.setPos(3.5F, 20.0F, 3.5F);
            this.leg4.setPos(-3.5F, 20.0F, 3.5F);
            this.tail.setPos(0.0F, 21.0F, 4.0F);
        }
    }
}
