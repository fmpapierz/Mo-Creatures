package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Simple egg model — a small rounded box resting on the ground, textured from {@code egg.png}.
 */
public class MoCModelEgg extends EntityModel<MoCEntityRenderState> {

    public MoCModelEgg(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Egg body sitting on the ground (feet at model Y 24), ~5 wide x 7 tall x 5 deep.
        root.addOrReplaceChild("egg",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 17.0F, -2.5F, 5.0F, 7.0F, 5.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
    }
}
