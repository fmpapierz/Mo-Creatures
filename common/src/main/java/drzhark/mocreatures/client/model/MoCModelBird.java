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
 * Bird model, converted faithfully from the legacy {@code MoCModelBird} ({@code ModelBase}).
 * The legacy constructor added a fixed y-offset of {@code byte0 = 16} to every rotation point;
 * that offset is baked into each {@link PartPose}.
 */
public class MoCModelBird extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart body;
    private final ModelPart leftleg;
    private final ModelPart rightleg;
    private final ModelPart rwing;
    private final ModelPart lwing;
    private final ModelPart tail;

    public MoCModelBird(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.beak = root.getChild("beak");
        this.body = root.getChild("body");
        this.leftleg = root.getChild("leftleg");
        this.rightleg = root.getChild("rightleg");
        this.rwing = root.getChild("rwing");
        this.lwing = root.getChild("lwing");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // legacy used a y-offset of byte0 = 16 added to every setRotationPoint y
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3.0F, -2.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(14, 0).addBox(-0.5F, -1.5F, -3.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 9).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 1.047198F, 0.0F, 0.0F));
        root.addOrReplaceChild("leftleg",
                CubeListBuilder.create().texOffs(26, 0).addBox(-1.0F, 0.0F, -4.0F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-2.0F, 19.0F, 1.0F));
        root.addOrReplaceChild("rightleg",
                CubeListBuilder.create().texOffs(26, 0).addBox(-1.0F, 0.0F, -4.0F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(1.0F, 19.0F, 1.0F));
        root.addOrReplaceChild("rwing",
                CubeListBuilder.create().texOffs(24, 13).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 5.0F, 5.0F),
                PartPose.offset(-2.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("lwing",
                CubeListBuilder.create().texOffs(24, 13).addBox(0.0F, 0.0F, -3.0F, 1.0F, 5.0F, 5.0F),
                PartPose.offset(2.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 23).addBox(-6.0F, 5.0F, 2.0F, 4.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(4.0F, 13.0F, 0.0F, 0.261799F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;
        float ageInTicks = state.ageInTicks;

        this.head.xRot = -(headPitch / 2.0F);
        this.head.yRot = headYaw;
        this.beak.yRot = this.head.yRot;
        this.beak.xRot = this.head.xRot;
        this.leftleg.xRot = Mth.cos(limbSwing * 0.6662F) * limbAmount;
        this.rightleg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbAmount;
        // Wing flap: a bounded cosine oscillation (the raw ageInTicks used before span unbounded, spinning
        // the wings instead of flapping). Approximates the legacy wing-field roll.
        float flap = Mth.cos(ageInTicks * 0.6F) * 0.6F;
        this.rwing.zRot = flap;
        this.lwing.zRot = -flap;
    }
}
