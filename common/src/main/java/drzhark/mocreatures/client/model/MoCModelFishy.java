package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Fishy model, converted faithfully from the legacy {@code MoCModelFishy} ({@code ModelBase}).
 * Body and tail carry a fixed pitch; the tail wags with the swim animation.
 */
public class MoCModelFishy extends EntityModel<MoCEntityRenderState> {

    private static final float PITCH = 0.7853981F;

    private final ModelPart body;
    private final ModelPart tail;

    public MoCModelFishy(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 1, 5, 5),
                PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, PITCH, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(12, 0).addBox(0.0F, 0.0F, 0.0F, 1, 3, 3),
                PartPose.offsetAndRotation(0.0F, 18.7F, 6.0F, PITCH, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        this.tail.yRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
    }
}
