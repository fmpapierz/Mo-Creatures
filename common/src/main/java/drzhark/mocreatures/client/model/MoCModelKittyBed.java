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
 * Kitty bed model, converted faithfully from the legacy {@code MoCModelKittyBed} ({@code ModelBase},
 * 64x32 texture). Geometry and texture offsets are preserved; only the scaffolding is modern. The
 * bed is static furniture, so {@link #setupAnim} does nothing beyond the base call.
 */
public class MoCModelKittyBed extends EntityModel<MoCEntityRenderState> {

    public MoCModelKittyBed(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("table_l",
                CubeListBuilder.create().texOffs(30, 8).addBox(-8.0F, 0.0F, 7.0F, 16, 6, 1),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("table_r",
                CubeListBuilder.create().texOffs(30, 8).addBox(-8.0F, 18.0F, -8.0F, 16, 6, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("table_b",
                CubeListBuilder.create().texOffs(30, 0).addBox(-8.0F, -3.0F, 0.0F, 16, 6, 1),
                PartPose.offsetAndRotation(8.0F, 21.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
        root.addOrReplaceChild("food_t",
                CubeListBuilder.create().texOffs(14, 0).addBox(1.0F, 1.0F, 1.0F, 4, 1, 4),
                PartPose.offset(-16.0F, 22.0F, 0.0F));
        root.addOrReplaceChild("food_tray_side",
                CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, 21.0F, 5.0F, 5, 3, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("food_tray_side_b",
                CubeListBuilder.create().texOffs(0, 0).addBox(-15.0F, 21.0F, 0.0F, 5, 3, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("food_tray_side_c",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -1.0F, 0.0F, 5, 3, 1),
                PartPose.offsetAndRotation(-16.0F, 22.0F, 2.0F, 0.0F, 1.5708F, 0.0F));
        root.addOrReplaceChild("food_tray_side_d",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -1.0F, 0.0F, 5, 3, 1),
                PartPose.offsetAndRotation(-11.0F, 22.0F, 3.0F, 0.0F, 1.5708F, 0.0F));
        root.addOrReplaceChild("pet_food",
                CubeListBuilder.create().texOffs(0, 9).addBox(0.0F, 0.0F, 0.0F, 4, 1, 4),
                PartPose.offset(-15.0F, 21.0F, 1.0F));
        root.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(16, 15).addBox(-10.0F, 0.0F, -7.0F, 16, 1, 14),
                PartPose.offset(2.0F, 23.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
    }
}
