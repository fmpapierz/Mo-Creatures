package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Snail model, converted faithfully from the legacy {@code MoCModelSnail} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; only the scaffolding is modern.
 */
public class MoCModelSnail extends EntityModel<MoCEntityRenderState> {

    private final ModelPart head;
    private final ModelPart antenna;
    private final ModelPart body;
    private final ModelPart shellUp;
    private final ModelPart tail;

    public MoCModelSnail(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.antenna = root.getChild("antenna");
        this.body = root.getChild("body");
        this.shellUp = root.getChild("shell_up");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 6).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 21.8F, -1.0F, -0.4537856F, 0.0F, 0.0F));
        root.addOrReplaceChild("antenna",
                CubeListBuilder.create().texOffs(8, 0).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 19.4F, -1.0F, 0.0523599F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        root.addOrReplaceChild("shell_up",
                CubeListBuilder.create().texOffs(12, 0).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 22.3F, -0.2F, 0.2268928F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(1, 2).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 23.0F, 3.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float ageInTicks = state.ageInTicks;
        float limbAmount = state.walkAnimationSpeed;

        float tailMov = Mth.cos(ageInTicks * 0.3F) * 0.8F;
        if (limbAmount < 0.1F) {
            tailMov = 0.0F;
        }
        this.tail.z = 2.0F + tailMov;
        this.shellUp.xRot = 0.2268928F + (tailMov / 10.0F);
    }
}
