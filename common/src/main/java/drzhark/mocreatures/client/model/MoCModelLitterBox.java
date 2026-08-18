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
 * Litter box model, converted faithfully from the legacy {@code MoCModelLitterBox} ({@code ModelBase},
 * 64x32 texture). Geometry and texture offsets are preserved. The clean litter cube ({@code litter}) is
 * swapped for the dirty {@code litter_used} cube once the box has been used (legacy {@code usedlitter}),
 * driven by {@code state.litterBoxUsed}.
 */
public class MoCModelLitterBox extends EntityModel<MoCEntityRenderState> {

    private final ModelPart litter;
    private final ModelPart litterUsed;

    public MoCModelLitterBox(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.litter = root.getChild("litter");
        this.litterUsed = root.getChild("litter_used");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("table1",
                CubeListBuilder.create().texOffs(30, 0).addBox(-8.0F, 0.0F, 7.0F, 16, 6, 1),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("table3",
                CubeListBuilder.create().texOffs(30, 0).addBox(-8.0F, 18.0F, -8.0F, 16, 6, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("table2",
                CubeListBuilder.create().texOffs(30, 0).addBox(-8.0F, -3.0F, 0.0F, 16, 6, 1),
                PartPose.offsetAndRotation(8.0F, 21.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
        root.addOrReplaceChild("litter",
                CubeListBuilder.create().texOffs(0, 15).addBox(0.0F, 0.0F, 0.0F, 16, 2, 14),
                PartPose.offset(-8.0F, 21.0F, -7.0F));
        // Dirty litter (legacy LitterUsed), shown once the box has been used.
        root.addOrReplaceChild("litter_used",
                CubeListBuilder.create().texOffs(16, 15).addBox(0.0F, 0.0F, 0.0F, 16, 2, 14),
                PartPose.offset(-8.0F, 21.0F, -7.0F));
        root.addOrReplaceChild("table4",
                CubeListBuilder.create().texOffs(30, 0).addBox(-8.0F, -3.0F, 0.0F, 16, 6, 1),
                PartPose.offsetAndRotation(-9.0F, 21.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
        root.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(16, 15).addBox(-10.0F, 0.0F, -7.0F, 16, 1, 14),
                PartPose.offset(2.0F, 23.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        // Swap clean litter for dirty once the box has been used (legacy usedlitter flag).
        this.litter.visible = !state.litterBoxUsed;
        this.litterUsed.visible = state.litterBoxUsed;
    }
}
