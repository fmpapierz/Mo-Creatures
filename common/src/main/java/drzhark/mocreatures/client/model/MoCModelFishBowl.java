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
 * Fish bowl model, converted faithfully from the legacy {@code MoCModelFishBowl} ({@code ModelBase},
 * 128x64 texture). The bowl always shows its rim + glass; the water is shown for any filled bowl
 * (type 1-11, matching the legacy {@code typeI > 0} check), and exactly one of the ten fish (a body +
 * a tail part) is shown — the one matching the held type (only for fish types 1-10, so a water-only
 * type-11 bowl shows water but no fish). Geometry and texture offsets are preserved from the legacy model.
 *
 * <p>Rendered with a translucent render type so the (semi-transparent) water and glass let the fish show
 * through, matching the legacy look. The water part is added <em>last</em> so it is submitted after the
 * fish: with depth-write on, the fish depth is laid down first and the translucent water then blends over
 * it (near wall over the fish, far wall correctly occluded by it). Fully-transparent glass faces are
 * discarded by the translucent shader (alpha &lt; 0.1), so they never wall off the interior.
 */
public class MoCModelFishBowl extends EntityModel<MoCEntityRenderState> {

    private static final int NUMBER_FISH = 10;

    private final ModelPart water;
    private final ModelPart[] body = new ModelPart[NUMBER_FISH];
    private final ModelPart[] tail = new ModelPart[NUMBER_FISH];

    public MoCModelFishBowl(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.water = root.getChild("water");
        for (int i = 0; i < NUMBER_FISH; i++) {
            this.body[i] = root.getChild("body" + i);
            this.tail[i] = root.getChild("tail" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("bowl_tap",
                CubeListBuilder.create().texOffs(64, 24).addBox(-7.5F, -7.5F, -7.5F, 11, 2, 11),
                PartPose.offset(2.0F, 15.0F, 2.0F));
        root.addOrReplaceChild("bowl",
                CubeListBuilder.create().texOffs(0, 33).addBox(-8.0F, -8.0F, -6.0F, 16, 15, 16),
                PartPose.offset(0.0F, 17.0F, -2.0F));

        // Fish are added BEFORE the (translucent) water so they render first and the water blends over them.
        for (int i = 0; i < NUMBER_FISH; i++) {
            int xText = (i < 5) ? i * 20 : i * 20 - 100;
            int yText = (i < 5) ? 0 : 10;
            root.addOrReplaceChild("body" + i,
                    CubeListBuilder.create().texOffs(xText, yText).addBox(3.0F, -4.0F, -4.0F, 1, 5, 5),
                    PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.7853982F, 0.0F, 0.0F));
            root.addOrReplaceChild("tail" + i,
                    CubeListBuilder.create().texOffs(xText + 12, yText).addBox(2.9F, 0.0F, 0.0F, 1, 3, 3),
                    PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.7853982F, 0.0F, 0.0F));
        }

        root.addOrReplaceChild("water",
                CubeListBuilder.create().texOffs(64, 38).addBox(-7.5F, -7.5F, -7.5F, 15, 11, 15),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        int type = state.fishBowlType; // 1-10 when a fish is held, else 0
        boolean hasFish = type >= 1 && type <= NUMBER_FISH;
        // Legacy MoCModelFishBowl rendered Water whenever typeI > 0, so a water-only bowl
        // (display type 11) also shows its water; the fish body/tail parts stay gated on
        // hasFish (types 1-10), leaving a type-11 water bowl with water but no fish.
        this.water.visible = type > 0;
        // Legacy swim: the shown fish circles the bowl by turning about its centre pivot. The rotation is
        // in degrees; 57.29578 = 180/pi converts to the radians the model part expects (faithful to
        // MoCModelFishBowl.setRotationAngles). The 45-degree nose-down pitch (xRot) is kept each frame.
        float swimYaw = state.fishBowlRotation / 57.29578F;
        for (int i = 0; i < NUMBER_FISH; i++) {
            boolean show = hasFish && (i == type - 1);
            this.body[i].visible = show;
            this.tail[i].visible = show;
            if (show) {
                this.body[i].xRot = 0.7853982F;
                this.tail[i].xRot = 0.7853982F;
                this.body[i].yRot = swimYaw;
                this.tail[i].yRot = swimYaw;
            }
        }
    }
}
