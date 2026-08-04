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
 * Maggot model, converted faithfully from the legacy {@code MoCModelMaggot} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; only the scaffolding is modern.
 */
public class MoCModelMaggot extends EntityModel<MoCEntityRenderState> {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart tailtip;

    public MoCModelMaggot(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.tailtip = root.getChild("tailtip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 23.0F, -2.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, 0.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 23.0F, -2.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 7).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 23.0F, 2.0F));
        root.addOrReplaceChild("tailtip",
                CubeListBuilder.create().texOffs(8, 7).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 23.0F, 4.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        // Legacy setRotationAngles was empty; no animation.
    }
}
