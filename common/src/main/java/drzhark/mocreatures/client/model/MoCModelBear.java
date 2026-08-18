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
import net.minecraft.util.Mth;

/**
 * Bear model, converted faithfully from the legacy {@code MoCModelBear} ({@code ModelBase}).
 * Geometry and texture offsets for the on-fours pose are preserved; the walking gait is ported.
 */
public class MoCModelBear extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart mouth;
    private final ModelPart mouthOpen;
    private final ModelPart snout;
    private final ModelPart earLeft;
    private final ModelPart earRight;
    private final ModelPart neck;
    private final ModelPart abdomen;
    private final ModelPart torso;
    private final ModelPart tail;
    private final ModelPart legFL1;
    private final ModelPart legFL2;
    private final ModelPart legFL3;
    private final ModelPart legFR1;
    private final ModelPart legFR2;
    private final ModelPart legFR3;
    private final ModelPart legRL1;
    private final ModelPart legRL2;
    private final ModelPart legRL3;
    private final ModelPart legRR1;
    private final ModelPart legRR2;
    private final ModelPart legRR3;

    // --- Standing (bearState == 1) parts
    private final ModelPart bHead;
    private final ModelPart bSnout;
    private final ModelPart bMouth;
    private final ModelPart bMouthOpen;
    private final ModelPart bNeck;
    private final ModelPart bEarLeft;
    private final ModelPart bEarRight;
    private final ModelPart bTorso;
    private final ModelPart bAbdomen;
    private final ModelPart bTail;
    private final ModelPart bLegFL1;
    private final ModelPart bLegFL2;
    private final ModelPart bLegFL3;
    private final ModelPart bLegFR1;
    private final ModelPart bLegFR2;
    private final ModelPart bLegFR3;
    private final ModelPart bLegRL1;
    private final ModelPart bLegRL2;
    private final ModelPart bLegRL3;
    private final ModelPart bLegRR1;
    private final ModelPart bLegRR2;
    private final ModelPart bLegRR3;

    // --- Sitting (bearState == 2) parts
    private final ModelPart cHead;
    private final ModelPart cSnout;
    private final ModelPart cMouth;
    private final ModelPart cMouthOpen;
    private final ModelPart cEarLeft;
    private final ModelPart cEarRight;
    private final ModelPart cNeck;
    private final ModelPart cTorso;
    private final ModelPart cAbdomen;
    private final ModelPart cTail;
    private final ModelPart cLegFL1;
    private final ModelPart cLegFL2;
    private final ModelPart cLegFL3;
    private final ModelPart cLegFR1;
    private final ModelPart cLegFR2;
    private final ModelPart cLegFR3;
    private final ModelPart cLegRL1;
    private final ModelPart cLegRL2;
    private final ModelPart cLegRL3;
    private final ModelPart cLegRR1;
    private final ModelPart cLegRR2;
    private final ModelPart cLegRR3;

    public MoCModelBear(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.mouth = root.getChild("mouth");
        this.mouthOpen = root.getChild("mouth_open");
        this.snout = root.getChild("snout");
        this.earLeft = root.getChild("ear_left");
        this.earRight = root.getChild("ear_right");
        this.neck = root.getChild("neck");
        this.abdomen = root.getChild("abdomen");
        this.torso = root.getChild("torso");
        this.tail = root.getChild("tail");
        this.legFL1 = root.getChild("leg_fl1");
        this.legFL2 = root.getChild("leg_fl2");
        this.legFL3 = root.getChild("leg_fl3");
        this.legFR1 = root.getChild("leg_fr1");
        this.legFR2 = root.getChild("leg_fr2");
        this.legFR3 = root.getChild("leg_fr3");
        this.legRL1 = root.getChild("leg_rl1");
        this.legRL2 = root.getChild("leg_rl2");
        this.legRL3 = root.getChild("leg_rl3");
        this.legRR1 = root.getChild("leg_rr1");
        this.legRR2 = root.getChild("leg_rr2");
        this.legRR3 = root.getChild("leg_rr3");

        this.bHead = root.getChild("b_head");
        this.bSnout = root.getChild("b_snout");
        this.bMouth = root.getChild("b_mouth");
        this.bMouthOpen = root.getChild("b_mouth_open");
        this.bNeck = root.getChild("b_neck");
        this.bEarLeft = root.getChild("b_ear_left");
        this.bEarRight = root.getChild("b_ear_right");
        this.bTorso = root.getChild("b_torso");
        this.bAbdomen = root.getChild("b_abdomen");
        this.bTail = root.getChild("b_tail");
        this.bLegFL1 = root.getChild("b_leg_fl1");
        this.bLegFL2 = root.getChild("b_leg_fl2");
        this.bLegFL3 = root.getChild("b_leg_fl3");
        this.bLegFR1 = root.getChild("b_leg_fr1");
        this.bLegFR2 = root.getChild("b_leg_fr2");
        this.bLegFR3 = root.getChild("b_leg_fr3");
        this.bLegRL1 = root.getChild("b_leg_rl1");
        this.bLegRL2 = root.getChild("b_leg_rl2");
        this.bLegRL3 = root.getChild("b_leg_rl3");
        this.bLegRR1 = root.getChild("b_leg_rr1");
        this.bLegRR2 = root.getChild("b_leg_rr2");
        this.bLegRR3 = root.getChild("b_leg_rr3");

        this.cHead = root.getChild("c_head");
        this.cSnout = root.getChild("c_snout");
        this.cMouth = root.getChild("c_mouth");
        this.cMouthOpen = root.getChild("c_mouth_open");
        this.cEarLeft = root.getChild("c_ear_left");
        this.cEarRight = root.getChild("c_ear_right");
        this.cNeck = root.getChild("c_neck");
        this.cTorso = root.getChild("c_torso");
        this.cAbdomen = root.getChild("c_abdomen");
        this.cTail = root.getChild("c_tail");
        this.cLegFL1 = root.getChild("c_leg_fl1");
        this.cLegFL2 = root.getChild("c_leg_fl2");
        this.cLegFL3 = root.getChild("c_leg_fl3");
        this.cLegFR1 = root.getChild("c_leg_fr1");
        this.cLegFR2 = root.getChild("c_leg_fr2");
        this.cLegFR3 = root.getChild("c_leg_fr3");
        this.cLegRL1 = root.getChild("c_leg_rl1");
        this.cLegRL2 = root.getChild("c_leg_rl2");
        this.cLegRL3 = root.getChild("c_leg_rl3");
        this.cLegRR1 = root.getChild("c_leg_rr1");
        this.cLegRR2 = root.getChild("c_leg_rr2");
        this.cLegRR3 = root.getChild("c_leg_rr3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(19, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -10.0F, 0.1502636F, 0.0F, 0.0F));
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(24, 21).addBox(-1.5F, 6.0F, -6.8F, 3.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -10.0F, -0.0068161F, 0.0F, 0.0F));
        // On-fours open maw: legacy MoCModelBear.MouthOpen (attack). Same head pivot/texOffs as 'mouth'.
        root.addOrReplaceChild("mouth_open",
                CubeListBuilder.create().texOffs(24, 21).addBox(-1.5F, 4.0F, -9.5F, 3.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -10.0F, 0.534236F, 0.0F, 0.0F));
        root.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(23, 13).addBox(-2.0F, 3.0F, -8.0F, 4.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -10.0F, 0.1502636F, 0.0F, 0.0F));
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(40, 0).addBox(2.0F, -2.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -10.0F, 0.1502636F, -0.3490659F, 0.1396263F));
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(16, 0).addBox(-5.0F, -2.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -10.0F, 0.1502636F, 0.3490659F, -0.1396263F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(18, 28).addBox(-3.5F, 0.0F, -7.0F, 7.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, -5.0F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(13, 62).addBox(-4.5F, 0.0F, 0.0F, 9.0F, 11.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, 5.0F, -0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(12, 42).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 5.0F, -5.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(26, 83).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 8.466666F, 12.0F, 0.4363323F, 0.0F, 0.0F));

        root.addOrReplaceChild("leg_fl1",
                CubeListBuilder.create().texOffs(40, 22).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(4.0F, 10.0F, -4.0F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_fl2",
                CubeListBuilder.create().texOffs(46, 35).addBox(-2.0F, 7.0F, 0.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(4.0F, 10.0F, -4.0F));
        root.addOrReplaceChild("leg_fl3",
                CubeListBuilder.create().texOffs(46, 45).addBox(-2.0F, 12.0F, -1.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offset(4.0F, 10.0F, -4.0F));

        root.addOrReplaceChild("leg_fr1",
                CubeListBuilder.create().texOffs(4, 22).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(-4.0F, 10.0F, -4.0F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_fr2",
                CubeListBuilder.create().texOffs(2, 35).addBox(-2.0F, 7.0F, 0.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-4.0F, 10.0F, -4.0F));
        root.addOrReplaceChild("leg_fr3",
                CubeListBuilder.create().texOffs(0, 45).addBox(-2.0F, 12.0F, -1.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offset(-4.0F, 10.0F, -4.0F));

        root.addOrReplaceChild("leg_rl1",
                CubeListBuilder.create().texOffs(34, 83).addBox(-1.5F, 0.0F, -2.5F, 4.0F, 8.0F, 6.0F),
                PartPose.offsetAndRotation(3.5F, 11.0F, 9.0F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_rl2",
                CubeListBuilder.create().texOffs(41, 97).addBox(-2.0F, 6.0F, -1.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(3.5F, 11.0F, 9.0F));
        root.addOrReplaceChild("leg_rl3",
                CubeListBuilder.create().texOffs(44, 107).addBox(-2.0F, 11.0F, -2.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offset(3.5F, 11.0F, 9.0F));

        root.addOrReplaceChild("leg_rr1",
                CubeListBuilder.create().texOffs(10, 83).addBox(-2.5F, 0.0F, -2.5F, 4.0F, 8.0F, 6.0F),
                PartPose.offsetAndRotation(-3.5F, 11.0F, 9.0F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_rr2",
                CubeListBuilder.create().texOffs(7, 97).addBox(-2.0F, 6.0F, -1.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-3.5F, 11.0F, 9.0F));
        root.addOrReplaceChild("leg_rr3",
                CubeListBuilder.create().texOffs(2, 107).addBox(-2.0F, 11.0F, -2.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offset(-3.5F, 11.0F, 9.0F));

        // ---------------------------------------------------------------
        // Standing pose (bearState == 1) -- B* parts
        // ---------------------------------------------------------------
        root.addOrReplaceChild("b_head",
                CubeListBuilder.create().texOffs(19, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 5.0F, -0.0242694F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_snout",
                CubeListBuilder.create().texOffs(23, 13).addBox(-2.0F, 2.5F, -8.5F, 4.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 5.0F, -0.0242694F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_mouth",
                CubeListBuilder.create().texOffs(24, 21).addBox(-1.5F, 5.5F, -8.0F, 3.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 5.0F, -0.08726F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_mouth_open",
                CubeListBuilder.create().texOffs(24, 21).addBox(-1.5F, 3.5F, -11.0F, 3.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 5.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_neck",
                CubeListBuilder.create().texOffs(18, 28).addBox(-3.5F, 0.0F, -7.0F, 7.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 11.0F, -1.336881F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_ear_left",
                CubeListBuilder.create().texOffs(40, 0).addBox(2.0F, -2.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 5.0F, -0.0242694F, -0.3490659F, 0.1396263F));
        root.addOrReplaceChild("b_ear_right",
                CubeListBuilder.create().texOffs(16, 0).addBox(-5.0F, -2.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 5.0F, -0.0242694F, 0.3490659F, -0.1396263F));
        root.addOrReplaceChild("b_torso",
                CubeListBuilder.create().texOffs(12, 42).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, 12.3F, -1.396263F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_abdomen",
                CubeListBuilder.create().texOffs(13, 62).addBox(-4.5F, 0.0F, 0.0F, 9.0F, 11.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 14.0F, -1.570796F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_tail",
                CubeListBuilder.create().texOffs(26, 83).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 12.46667F, 12.6F, 0.3619751F, 0.0F, 0.0F));

        root.addOrReplaceChild("b_leg_fl1",
                CubeListBuilder.create().texOffs(40, 22).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(5.0F, -1.0F, 6.0F, 0.2617994F, 0.0F, -0.2617994F));
        root.addOrReplaceChild("b_leg_fl2",
                CubeListBuilder.create().texOffs(46, 35).addBox(0.0F, 5.0F, 3.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(5.0F, -1.0F, 6.0F, -0.5576792F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_leg_fl3",
                CubeListBuilder.create().texOffs(46, 45).addBox(0.1F, -7.0F, -14.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(5.0F, -1.0F, 6.0F, 2.007645F, 0.0F, 0.0F));

        root.addOrReplaceChild("b_leg_fr1",
                CubeListBuilder.create().texOffs(4, 22).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(-5.0F, -1.0F, 6.0F, 0.2617994F, 0.0F, 0.2617994F));
        root.addOrReplaceChild("b_leg_fr2",
                CubeListBuilder.create().texOffs(2, 35).addBox(-4.0F, 5.0F, 3.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-5.0F, -1.0F, 6.0F, -0.5576792F, 0.0F, 0.0F));
        root.addOrReplaceChild("b_leg_fr3",
                CubeListBuilder.create().texOffs(0, 45).addBox(-4.1F, -7.0F, -14.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-5.0F, -1.0F, 6.0F, 2.007129F, 0.0F, 0.0F));

        root.addOrReplaceChild("b_leg_rl1",
                CubeListBuilder.create().texOffs(34, 83).addBox(-1.5F, 0.0F, -2.5F, 4.0F, 8.0F, 6.0F),
                PartPose.offsetAndRotation(3.0F, 11.0F, 9.0F, -0.5235988F, -0.2617994F, 0.0F));
        root.addOrReplaceChild("b_leg_rl2",
                CubeListBuilder.create().texOffs(41, 97).addBox(-1.3F, 6.0F, -3.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(3.0F, 11.0F, 9.0F, 0.0F, -0.2617994F, 0.0F));
        root.addOrReplaceChild("b_leg_rl3",
                CubeListBuilder.create().texOffs(44, 107).addBox(-1.2F, 11.0F, -4.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(3.0F, 11.0F, 9.0F, 0.0F, -0.2617994F, 0.0F));

        root.addOrReplaceChild("b_leg_rr1",
                CubeListBuilder.create().texOffs(10, 83).addBox(-2.5F, 0.0F, -2.5F, 4.0F, 8.0F, 6.0F),
                PartPose.offsetAndRotation(-3.0F, 11.0F, 9.0F, -0.1745329F, 0.2617994F, 0.0F));
        root.addOrReplaceChild("b_leg_rr2",
                CubeListBuilder.create().texOffs(7, 97).addBox(-2.4F, 6.0F, -1.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-3.0F, 11.0F, 9.0F, 0.0F, 0.2617994F, 0.0F));
        root.addOrReplaceChild("b_leg_rr3",
                CubeListBuilder.create().texOffs(2, 107).addBox(-2.5F, 11.0F, -2.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-3.0F, 11.0F, 9.0F, 0.0F, 0.2617994F, 0.0F));

        // ---------------------------------------------------------------
        // Sitting pose (bearState == 2) -- C* parts
        // ---------------------------------------------------------------
        root.addOrReplaceChild("c_head",
                CubeListBuilder.create().texOffs(19, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -3.5F, 0.1502636F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_snout",
                CubeListBuilder.create().texOffs(23, 13).addBox(-2.0F, 3.0F, -8.5F, 4.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -3.5F, 0.1502636F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_mouth",
                CubeListBuilder.create().texOffs(24, 21).addBox(-1.5F, 6.0F, -7.0F, 3.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -3.5F, -0.0068161F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_mouth_open",
                CubeListBuilder.create().texOffs(24, 21).addBox(-1.5F, 5.5F, -9.0F, 3.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -3.5F, 0.3665191F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_ear_left",
                CubeListBuilder.create().texOffs(40, 0).addBox(2.0F, -2.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -3.5F, 0.1502636F, -0.3490659F, 0.1396263F));
        root.addOrReplaceChild("c_ear_right",
                CubeListBuilder.create().texOffs(16, 0).addBox(-5.0F, -2.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -3.5F, 0.1502636F, 0.3490659F, -0.1396263F));
        root.addOrReplaceChild("c_neck",
                CubeListBuilder.create().texOffs(18, 28).addBox(-3.5F, 0.0F, -7.0F, 7.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 5.8F, 3.4F, -0.3316126F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_torso",
                CubeListBuilder.create().texOffs(12, 42).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 5.8F, 3.4F, -0.9712912F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_abdomen",
                CubeListBuilder.create().texOffs(13, 62).addBox(-4.5F, 0.0F, 0.0F, 9.0F, 11.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, 9.0F, -1.570796F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_tail",
                CubeListBuilder.create().texOffs(26, 83).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 21.46667F, 8.0F, 0.4363323F, 0.0F, 0.0F));

        root.addOrReplaceChild("c_leg_fl1",
                CubeListBuilder.create().texOffs(40, 22).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(4.0F, 10.0F, 0.0F, -0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_leg_fl2",
                CubeListBuilder.create().texOffs(46, 35).addBox(-2.0F, 0.0F, -1.2F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(4.0F, 17.0F, -2.0F, -0.3490659F, 0.0F, 0.2617994F));
        root.addOrReplaceChild("c_leg_fl3",
                CubeListBuilder.create().texOffs(46, 45).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(2.5F, 22.0F, -4.0F, 0.0F, 0.1745329F, 0.0F));

        root.addOrReplaceChild("c_leg_fr1",
                CubeListBuilder.create().texOffs(4, 22).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(-4.0F, 10.0F, 0.0F, -0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("c_leg_fr2",
                CubeListBuilder.create().texOffs(2, 35).addBox(-2.0F, 0.0F, -1.2F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-4.0F, 17.0F, -2.0F, -0.3490659F, 0.0F, -0.2617994F));
        root.addOrReplaceChild("c_leg_fr3",
                CubeListBuilder.create().texOffs(0, 45).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-2.5F, 22.0F, -4.0F, 0.0F, -0.1745329F, 0.0F));

        root.addOrReplaceChild("c_leg_rl1",
                CubeListBuilder.create().texOffs(34, 83).addBox(-1.5F, 0.0F, -2.5F, 4.0F, 8.0F, 6.0F),
                PartPose.offsetAndRotation(3.0F, 21.0F, 5.0F, -1.396263F, -0.3490659F, 0.3490659F));
        root.addOrReplaceChild("c_leg_rl2",
                CubeListBuilder.create().texOffs(41, 97).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(5.2F, 22.5F, -1.0F, -1.570796F, 0.0F, 0.3490659F));
        root.addOrReplaceChild("c_leg_rl3",
                CubeListBuilder.create().texOffs(44, 107).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(5.5F, 22.0F, -6.0F, -1.375609F, 0.0F, 0.3490659F));

        root.addOrReplaceChild("c_leg_rr1",
                CubeListBuilder.create().texOffs(10, 83).addBox(-2.5F, 0.0F, -2.5F, 4.0F, 8.0F, 6.0F),
                PartPose.offsetAndRotation(-3.0F, 21.0F, 5.0F, -1.396263F, 0.3490659F, -0.3490659F));
        root.addOrReplaceChild("c_leg_rr2",
                CubeListBuilder.create().texOffs(7, 97).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-5.2F, 22.5F, -1.0F, -1.570796F, 0.0F, -0.3490659F));
        root.addOrReplaceChild("c_leg_rr3",
                CubeListBuilder.create().texOffs(2, 107).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-5.5F, 22.0F, -6.0F, -1.375609F, 0.0F, -0.3490659F));

        // The bear textures ship padded to a 256x256 square; declaring a 128x128 UV space maps the
        // legacy 64x128-authored offsets 2x-uniform onto that sheet (matching the ostrich convention).
        // Declaring the legacy 64x128 here stretches U 4x / V 2x = the glitched black-and-white mess.
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        int bearState = state.bearState;
        float xAngle = state.xRot * DEG_TO_RAD;
        float yAngle = state.yRot * DEG_TO_RAD;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float lLegRotX = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;
        float rLegRotX = Mth.cos((limbSwing * 0.6662F) + 3.141593F) * 0.8F * limbAmount;

        // Visibility: only the active pose's part set draws. During a melee swing the bear opens its maw
        // (swaps the closed mouth cube for the *_mouth_open cube) and, when reared, swings its forepaws down.
        boolean onFours = (bearState == 0);
        boolean standing = (bearState == 1);
        boolean sitting = (bearState == 2);
        float maul = Mth.sin(state.attackSwing * (float) Math.PI); // 0 at rest, 1 mid-swing
        boolean maw = state.attackSwing > 0.05F;

        this.head.visible = onFours;
        this.mouth.visible = onFours && !maw;
        this.mouthOpen.visible = onFours && maw;
        this.snout.visible = onFours;
        this.earLeft.visible = onFours;
        this.earRight.visible = onFours;
        this.neck.visible = onFours;
        this.abdomen.visible = onFours;
        this.torso.visible = onFours;
        this.tail.visible = onFours;
        this.legFL1.visible = onFours;
        this.legFL2.visible = onFours;
        this.legFL3.visible = onFours;
        this.legFR1.visible = onFours;
        this.legFR2.visible = onFours;
        this.legFR3.visible = onFours;
        this.legRL1.visible = onFours;
        this.legRL2.visible = onFours;
        this.legRL3.visible = onFours;
        this.legRR1.visible = onFours;
        this.legRR2.visible = onFours;
        this.legRR3.visible = onFours;

        this.bHead.visible = standing;
        this.bSnout.visible = standing;
        this.bMouth.visible = standing && !maw;
        this.bMouthOpen.visible = standing && maw;
        this.bNeck.visible = standing;
        this.bEarLeft.visible = standing;
        this.bEarRight.visible = standing;
        this.bTorso.visible = standing;
        this.bAbdomen.visible = standing;
        this.bTail.visible = standing;
        this.bLegFL1.visible = standing;
        this.bLegFL2.visible = standing;
        this.bLegFL3.visible = standing;
        this.bLegFR1.visible = standing;
        this.bLegFR2.visible = standing;
        this.bLegFR3.visible = standing;
        this.bLegRL1.visible = standing;
        this.bLegRL2.visible = standing;
        this.bLegRL3.visible = standing;
        this.bLegRR1.visible = standing;
        this.bLegRR2.visible = standing;
        this.bLegRR3.visible = standing;

        this.cHead.visible = sitting;
        this.cSnout.visible = sitting;
        this.cMouth.visible = sitting && !maw;
        this.cMouthOpen.visible = sitting && maw;
        this.cEarLeft.visible = sitting;
        this.cEarRight.visible = sitting;
        this.cNeck.visible = sitting;
        this.cTorso.visible = sitting;
        this.cAbdomen.visible = sitting;
        this.cTail.visible = sitting;
        this.cLegFL1.visible = sitting;
        this.cLegFL2.visible = sitting;
        this.cLegFL3.visible = sitting;
        this.cLegFR1.visible = sitting;
        this.cLegFR2.visible = sitting;
        this.cLegFR3.visible = sitting;
        this.cLegRL1.visible = sitting;
        this.cLegRL2.visible = sitting;
        this.cLegRL3.visible = sitting;
        this.cLegRR1.visible = sitting;
        this.cLegRR2.visible = sitting;
        this.cLegRR3.visible = sitting;

        if (bearState == 0) {
            this.head.xRot = 0.1502636F + xAngle;
            this.head.yRot = yAngle;
            this.snout.xRot = 0.1502636F + xAngle;
            this.snout.yRot = yAngle;
            this.mouth.xRot = -0.0068161F + xAngle;
            this.mouth.yRot = yAngle;
            this.earLeft.xRot = 0.1502636F + xAngle;
            this.earLeft.yRot = -0.3490659F + yAngle;
            this.earRight.xRot = 0.1502636F + xAngle;
            this.earRight.yRot = 0.3490659F + yAngle;

            this.legFL1.xRot = 0.2617994F + lLegRotX;
            this.legFL2.xRot = lLegRotX;
            this.legFL3.xRot = lLegRotX;

            this.legRR1.xRot = -0.1745329F + lLegRotX;
            this.legRR2.xRot = lLegRotX;
            this.legRR3.xRot = lLegRotX;

            this.legFR1.xRot = 0.2617994F + rLegRotX;
            this.legFR2.xRot = rLegRotX;
            this.legFR3.xRot = rLegRotX;

            this.legRL1.xRot = -0.1745329F + rLegRotX;
            this.legRL2.xRot = rLegRotX;
            this.legRL3.xRot = rLegRotX;

            this.tail.zRot = lLegRotX * 0.2F;
        } else if (bearState == 1) {
            // Standing: head/snout/mouth/ears track look with INVERTED pitch.
            this.bHead.xRot = -0.0242694F - xAngle;
            this.bHead.yRot = yAngle;
            this.bSnout.xRot = -0.0242694F - xAngle;
            this.bSnout.yRot = yAngle;
            this.bMouth.xRot = -0.08726F - xAngle;
            this.bMouth.yRot = yAngle;
            this.bEarLeft.xRot = -0.0242694F - xAngle;
            this.bEarLeft.yRot = -0.3490659F + yAngle;
            this.bEarRight.xRot = -0.0242694F - xAngle;
            this.bEarRight.yRot = 0.3490659F + yAngle;

            // Arm breathing movement.
            float breathing = Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
            this.bLegFR1.zRot = 0.2617994F + breathing;
            this.bLegFR2.zRot = breathing;
            this.bLegFR3.zRot = breathing;
            this.bLegFL1.zRot = -0.2617994F - breathing;
            this.bLegFL2.zRot = -breathing;
            this.bLegFL3.zRot = -breathing;

            // Front-arm base pitch, plus a two-forepaw maul swing driven by the melee swing progress: the
            // reared bear rocks its forearms up and slams them down at the target (legacy attack swing).
            this.bLegFL1.xRot = 0.2617994F - maul * 1.3F;
            this.bLegFL2.xRot = -0.5576792F;
            this.bLegFL3.xRot = 2.007645F;
            this.bLegFR1.xRot = 0.2617994F - maul * 1.3F;
            this.bLegFR2.xRot = -0.5576792F;
            this.bLegFR3.xRot = 2.007645F;

            // Rear legs walk gait.
            this.bLegRR1.xRot = -0.1745329F + lLegRotX;
            this.bLegRR2.xRot = lLegRotX;
            this.bLegRR3.xRot = lLegRotX;
            this.bLegRL1.xRot = -0.5235988F + rLegRotX;
            this.bLegRL2.xRot = rLegRotX;
            this.bLegRL3.xRot = rLegRotX;
        } else {
            // Sitting: head/snout/mouth/ears track look (normal pitch); legs are static.
            this.cHead.xRot = 0.1502636F + xAngle;
            this.cHead.yRot = yAngle;
            this.cSnout.xRot = 0.1502636F + xAngle;
            this.cSnout.yRot = yAngle;
            this.cMouth.xRot = -0.0068161F + xAngle;
            this.cMouth.yRot = yAngle;
            this.cEarLeft.xRot = 0.1502636F + xAngle;
            this.cEarLeft.yRot = -0.3490659F + yAngle;
            this.cEarRight.xRot = 0.1502636F + xAngle;
            this.cEarRight.yRot = 0.3490659F + yAngle;
            // Legacy never wags the sitting tail (CTail is baked-static); c_tail keeps its baked pose only.
        }
    }
}
