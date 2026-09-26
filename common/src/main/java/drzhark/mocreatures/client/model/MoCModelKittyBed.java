package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.renderer.MoCKittyBedRenderer;
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
 * 64x32 texture). Geometry and texture offsets are preserved; only the scaffolding is modern.
 *
 * <p>Legacy drew the bed as TWO models over the one {@code fullkittybed.png}: this frame (walls, plank floor,
 * food tray, milk / pet food) in the main pass, and {@code MoCModelKittyBed2} — a single 16x3x14 "Sheet" —
 * in a second, wool-tinted pass (12.0.5 {@code MoCRenderKittyBed.LayerMoCKittyBed}:53-60). The first port
 * kept only the frame, so the wool never existed to be coloured, and because the floor plank's top face is
 * transparent in the texture (its top UV, u30-46/v15-29, is an empty region: the sheet is what covers it)
 * a placed bed was an empty wooden frame with the ground showing through. The sheet is restored as
 * {@link Sheet}, which {@code MoCKittyBedRenderer} draws in its own tinted layer exactly like legacy.</p>
 *
 * <p>{@link #setupAnim} restores legacy {@code render()}'s conditional parts (12.0.5
 * {@code MoCModelKittyBed.java}:83-103): the food tray is hidden while the bed is carried ({@code pickedUp}),
 * and the milk / pet-food fill is drawn only when the bed actually holds it. The port used to draw the pet
 * food permanently and had dropped the {@code Milk} cube altogether.</p>
 */
public class MoCModelKittyBed extends EntityModel<MoCEntityRenderState> {

    private final ModelPart foodTray;
    private final ModelPart foodTraySide;
    private final ModelPart foodTraySideB;
    private final ModelPart foodTraySideC;
    private final ModelPart foodTraySideD;
    private final ModelPart milk;
    private final ModelPart petFood;

    public MoCModelKittyBed(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.foodTray = root.getChild("food_t");
        this.foodTraySide = root.getChild("food_tray_side");
        this.foodTraySideB = root.getChild("food_tray_side_b");
        this.foodTraySideC = root.getChild("food_tray_side_c");
        this.foodTraySideD = root.getChild("food_tray_side_d");
        this.milk = root.getChild("milk");
        this.petFood = root.getChild("pet_food");
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
        // Legacy Milk (12.0.5 MoCModelKittyBed.java:71-73): the same 4x1x4 slot in the tray as the pet food,
        // skinned with the cream texels at (14, 9) instead of the kibble at (0, 9).
        root.addOrReplaceChild("milk",
                CubeListBuilder.create().texOffs(14, 9).addBox(0.0F, 0.0F, 0.0F, 4, 1, 4),
                PartPose.offset(-15.0F, 21.0F, 1.0F));
        root.addOrReplaceChild("pet_food",
                CubeListBuilder.create().texOffs(0, 9).addBox(0.0F, 0.0F, 0.0F, 4, 1, 4),
                PartPose.offset(-15.0F, 21.0F, 1.0F));
        root.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(16, 15).addBox(-10.0F, 0.0F, -7.0F, 16, 1, 14),
                PartPose.offset(2.0F, 23.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * Legacy {@code render()} drew the frame and floor always, but the food tray only while the bed was not
     * {@code pickedUp}, and within it the milk only {@code if (hasMilk)} and the pet food only
     * {@code if (hasFood)} (12.0.5 {@code MoCModelKittyBed.java}:84-101). {@code pickedUp} was true while the
     * bed rode the player; its 26.2 equivalent is {@code state.riding}, which {@code MoCMobRenderer} sets for a
     * carried creature. The milk/food flags come from {@link MoCKittyBedRenderer.KittyBedRenderState}.
     *
     * <p>Legacy lowered the fill as {@code milklevel} rose, but {@code milklevel} was a server-only field that
     * was never synced, so on the client it was always 0 and the fill always sat at its full height (Y 21) —
     * which is what the part's rest pose already is.</p>
     *
     * <p>{@code Model.resetPose()} restores the pose but NOT {@code ModelPart.visible}, so every toggled part is
     * assigned on every frame.</p>
     */
    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        MoCKittyBedRenderer.KittyBedRenderState bed =
                state instanceof MoCKittyBedRenderer.KittyBedRenderState kb ? kb : null;
        boolean onGround = !state.riding;
        boolean hasMilk = bed != null && bed.hasMilk;
        boolean hasFood = bed != null && bed.hasFood;
        this.foodTray.visible = onGround;
        this.foodTraySide.visible = onGround;
        this.foodTraySideB.visible = onGround;
        this.foodTraySideC.visible = onGround;
        this.foodTraySideD.visible = onGround;
        this.milk.visible = onGround && hasMilk;
        // Milk and food are mutually exclusive in play (each clears the other); should edited NBT ever set both,
        // draw only the milk rather than two coplanar cubes fighting over the same slot.
        this.petFood.visible = onGround && hasFood && !hasMilk;
    }

    /**
     * The dyed sheet — legacy {@code MoCModelKittyBed2} (12.0.5 {@code MoCModelKittyBed2.java}:16-18): one
     * 16x3x14 box at texture offset (0, 15), whose top face (u14-30, v15-29) and open-side face (u0-14,
     * v29-32) are the white wool in {@code fullkittybed.png}; its other faces are transparent there, so they
     * never fight the walls and floor they share planes with. It is a separate model, not a part of the frame,
     * because it is drawn in its own pass with the wool tint while the frame stays untinted — both submits
     * share one render state, so a part toggled per pass inside a single model could not tell them apart.
     */
    public static final class Sheet extends EntityModel<MoCEntityRenderState> {

        public Sheet(ModelPart root) {
            super(root, RenderTypes::entityCutoutCull);
        }

        public static LayerDefinition createSheetLayer() {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("sheet",
                    CubeListBuilder.create().texOffs(0, 15).addBox(0.0F, 0.0F, 0.0F, 16, 3, 14),
                    PartPose.offset(-8.0F, 21.0F, -7.0F));
            return LayerDefinition.create(mesh, 64, 32);
        }
    }
}
