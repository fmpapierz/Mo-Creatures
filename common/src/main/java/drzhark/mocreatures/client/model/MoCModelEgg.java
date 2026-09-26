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

/**
 * The laid egg, exactly as 12.0.5 {@code MoCModelEgg} (and 5.1.5) built it: a 3x3x3 core with a one-pixel bump on
 * top, underneath and on two sides, so it reads as a small rounded egg about a quarter of a block across. It sits off
 * the model origin just as legacy's did, and on legacy's texture sheet: 12.0.5 commented out its
 * {@code textureWidth/Height} lines, which leaves 1.12 {@code ModelBase}'s default 64x32, the size of
 * {@code egg.png}. Every face of it is painted opaque, so the culled render type hides nothing.
 */
public class MoCModelEgg extends EntityModel<MoCEntityRenderState> {

    public MoCModelEgg(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("egg1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F)); // core
        root.addOrReplaceChild("egg2", CubeListBuilder.create().texOffs(10, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.5F, 19.5F, 0.5F)); // top
        root.addOrReplaceChild("egg3", CubeListBuilder.create().texOffs(30, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.5F, 22.5F, 0.5F)); // bottom
        root.addOrReplaceChild("egg4", CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(-0.5F, 20.5F, 0.5F)); // side
        root.addOrReplaceChild("egg5", CubeListBuilder.create().texOffs(18, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(2.5F, 20.5F, 0.5F)); // side
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
    }
}
