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
 * Ant model, converted faithfully from the legacy {@code MoCModelAnt} ({@code ModelBase}). Every box,
 * texture offset, pivot and rest rotation is preserved verbatim; only the scaffolding is modern.
 *
 * <p>The legacy model is <em>flat</em> — all nine parts hang directly off the root and were rendered one
 * after another in {@code render()}, none of them parented to the head or thorax — so the port keeps them
 * as nine root children rather than inventing a hierarchy. Note also that legacy
 * {@code setRotationAngles} touches <b>only</b> the three leg rows: the ant's head, mouth and antennae do
 * NOT track the look-at target the way most Mo'Creatures models do, so no head rotation is applied here
 * either.</p>
 *
 * <p>The texture is declared 32x32 exactly as the legacy model did ({@code textureWidth}/{@code textureHeight}
 * = 32), even though {@code ant.png} ships at 64x64. That is intentional and is the same convention the
 * ported roach model follows: UVs are normalised against the declared size, so a 32-wide layout stretched
 * over a 64px image samples identically to 1.12.</p>
 */
public class MoCModelAnt extends EntityModel<MoCEntityRenderState> {

    /** Legacy rest angles for the three leg rows; the walk gait swings about these. */
    private static final float FRONT_LEGS_REST = -0.6192304F;
    private static final float MID_LEGS_REST = 0.5948578F;
    private static final float REAR_LEGS_REST = 0.9136644F;

    private final ModelPart head;
    private final ModelPart mouth;
    private final ModelPart rightAntenna;
    private final ModelPart leftAntenna;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart midLegs;
    private final ModelPart frontLegs;
    private final ModelPart rearLegs;

    public MoCModelAnt(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.mouth = root.getChild("mouth");
        this.rightAntenna = root.getChild("right_antenna");
        this.leftAntenna = root.getChild("left_antenna");
        this.thorax = root.getChild("thorax");
        this.abdomen = root.getChild("abdomen");
        this.midLegs = root.getChild("mid_legs");
        this.frontLegs = root.getChild("front_legs");
        this.rearLegs = root.getChild("rear_legs");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 21.9F, -1.3F, -2.171231F, 0.0F, 0.0F));

        // Flat 2x1 mandible plate hung off the front of the head.
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(8, 10).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 0.0F),
                PartPose.offsetAndRotation(-1.0F, 22.3F, -1.9F, -0.8286699F, 0.0F, 0.0F));

        // Antennae: flat 1x1 quads splayed +/-45 degrees (0.7853982 rad) and tipped up.
        root.addOrReplaceChild("right_antenna",
                CubeListBuilder.create().texOffs(0, 6).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(-0.5F, 21.7F, -2.3F, -1.041001F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("left_antenna",
                CubeListBuilder.create().texOffs(4, 6).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(0.5F, 21.7F, -2.3F, -1.041001F, -0.7853982F, 0.0F));

        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 1.5F, -1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 20.0F, -0.5F));

        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(8, 1).addBox(-0.5F, -0.2F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 21.5F, 0.3F, 1.706911F, 0.0F, 0.0F));

        // Three flat leg rows (a 2-wide x 3-tall quad each), splayed forward / down / back.
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(4, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, -0.7F, MID_LEGS_REST, 0.0F, 0.0F));
        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, -0.8F, FRONT_LEGS_REST, 0.0F, 0.0F));
        // Legacy reuses the front-leg texture region (0, 8) for the rear row.
        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create().texOffs(0, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, REAR_LEGS_REST, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    /**
     * Legacy {@code setRotationAngles}:90-96. A classic insect tripod gait: the front and rear rows swing
     * together ({@code cos(walkPos + PI) * walkSpeed}) while the middle row swings in antiphase
     * ({@code cos(walkPos) * walkSpeed}), each about its own rest angle. Nothing else animates.
     */
    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float walkPos = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;

        float legMov = Mth.cos(walkPos + (float) Math.PI) * walkSpeed;
        float legMovB = Mth.cos(walkPos) * walkSpeed;

        this.frontLegs.xRot = FRONT_LEGS_REST + legMov;
        this.midLegs.xRot = MID_LEGS_REST + legMovB;
        this.rearLegs.xRot = REAR_LEGS_REST + legMov;
    }
}
