package drzhark.mocreatures.client.model;

import java.util.Set;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Elephant model, converted faithfully from the legacy {@code MoCModelElephant} ({@code ModelBase}).
 * All geometry, texture offsets and part rotations are preserved, including the equipment geometry
 * (harness, garment skirt, howdah / mammoth platform and storage chests) that the previous port had
 * dropped. The legacy per-frame procedural animation (trunk chains, ear/tail wiggle) is reproduced as
 * a head-tracking + leg-swing gait plus a basic trunk/tail sway, and the equipment parts are shown or
 * hidden from {@link #setupAnim} according to the render-state equip flags.
 *
 * <p>The real texture sheet for this model is {@code 128 x 256}; that size is what makes the harness
 * and howdah UVs (which live in the lower half of the sheet, V &gt; 128) map correctly.
 *
 * <p>Equipment visibility (mirrors the legacy {@code render()} gating, remapped onto the modern
 * {@code armorStage} / {@code hasChest} render-state fields):
 * <ul>
 *   <li>harness blanket + belts &mdash; {@code armorStage >= 1}</li>
 *   <li>garment skirt &mdash; {@code armorStage >= 2} (the garment turns an Indian elephant into type 5)</li>
 *   <li>howdah cabin (type 5) / mammoth fort platform (type 3 or 4) &mdash; {@code armorStage >= 3}</li>
 *   <li>storage chests, bedrolls &amp; blankets &mdash; {@code hasChest}</li>
 * </ul>
 * The tusks are always present (their legacy age/level gating is not carried in the render state).
 */
public class MoCModelElephant extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // Face selectors for the flat (zero-thickness) tusk-armour fins: only the WEST side of each fin
    // is painted on the sheet, so under the culled render type each fin box is split into a painted
    // face plus an opposite face re-aimed (via a shifted texOffs) at the same painted tile.
    private static final Set<Direction> WEST_FACE = Set.of(Direction.WEST);
    private static final Set<Direction> EAST_FACE = Set.of(Direction.EAST);

    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart headBump;
    private final ModelPart chin;
    private final ModelPart lowerLip;
    private final ModelPart back;
    private final ModelPart leftSmallEar;
    private final ModelPart leftBigEar;
    private final ModelPart rightSmallEar;
    private final ModelPart rightBigEar;
    private final ModelPart hump;
    private final ModelPart body;
    private final ModelPart skirt;
    private final ModelPart rightTuskA;
    private final ModelPart rightTuskB;
    private final ModelPart rightTuskC;
    private final ModelPart rightTuskD;
    private final ModelPart leftTuskA;
    private final ModelPart leftTuskB;
    private final ModelPart leftTuskC;
    private final ModelPart leftTuskD;
    private final ModelPart trunkA;
    private final ModelPart trunkB;
    private final ModelPart trunkC;
    private final ModelPart trunkD;
    private final ModelPart trunkE;
    private final ModelPart frontRightUpperLeg;
    private final ModelPart frontRightLowerLeg;
    private final ModelPart frontLeftUpperLeg;
    private final ModelPart frontLeftLowerLeg;
    private final ModelPart backRightUpperLeg;
    private final ModelPart backRightLowerLeg;
    private final ModelPart backLeftUpperLeg;
    private final ModelPart backLeftLowerLeg;
    private final ModelPart tailRoot;
    private final ModelPart tail;
    private final ModelPart tailPlush;

    // ---- Tusk armour sets (ported from legacy; visibility gated by render-state tusks tier) ----
    // Wood (tier 1)
    private final ModelPart tuskLW1;
    private final ModelPart tuskLW2;
    private final ModelPart tuskLW3;
    private final ModelPart tuskLW4;
    private final ModelPart tuskLW5;
    private final ModelPart tuskRW1;
    private final ModelPart tuskRW2;
    private final ModelPart tuskRW3;
    private final ModelPart tuskRW4;
    private final ModelPart tuskRW5;
    // Iron (tier 2)
    private final ModelPart tuskLI1;
    private final ModelPart tuskLI2;
    private final ModelPart tuskLI3;
    private final ModelPart tuskLI4;
    private final ModelPart tuskLI5;
    private final ModelPart tuskRI1;
    private final ModelPart tuskRI2;
    private final ModelPart tuskRI3;
    private final ModelPart tuskRI4;
    private final ModelPart tuskRI5;
    // Diamond (tier 3)
    private final ModelPart tuskLD1;
    private final ModelPart tuskLD2;
    private final ModelPart tuskLD3;
    private final ModelPart tuskLD4;
    private final ModelPart tuskLD5;
    private final ModelPart tuskRD1;
    private final ModelPart tuskRD2;
    private final ModelPart tuskRD3;
    private final ModelPart tuskRD4;
    private final ModelPart tuskRD5;

    // ---- Equipment parts (toggled by the equip flags in setupAnim) ----
    // Harness (armorStage >= 1)
    private final ModelPart harnessBlanket;
    private final ModelPart harnessUpperBelt;
    private final ModelPart harnessLowerBelt;
    // Howdah cabin (type 5, armorStage >= 3)
    private final ModelPart cabinPillow;
    private final ModelPart cabinLeftRail;
    private final ModelPart cabin;
    private final ModelPart cabinRightRail;
    private final ModelPart cabinBackRail;
    private final ModelPart cabinRoof;
    // Mammoth fort platform (type 3/4, armorStage >= 3)
    private final ModelPart fortNeckBeam;
    private final ModelPart fortBackBeam;
    private final ModelPart fortFloor1;
    private final ModelPart fortFloor2;
    private final ModelPart fortFloor3;
    private final ModelPart fortBackWall;
    private final ModelPart fortBackLeftWall;
    private final ModelPart fortBackRightWall;
    // Storage chests (hasChest)
    private final ModelPart storageRightBedroll;
    private final ModelPart storageLeftBedroll;
    private final ModelPart storageFrontRightChest;
    private final ModelPart storageBackRightChest;
    private final ModelPart storageFrontLeftChest;
    private final ModelPart storageBackLeftChest;
    private final ModelPart storageRightBlankets;
    private final ModelPart storageLeftBlankets;
    private final ModelPart storageUpLeft;
    private final ModelPart storageUpRight;

    public MoCModelElephant(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.neck = root.getChild("neck");
        this.headBump = root.getChild("head_bump");
        this.chin = root.getChild("chin");
        this.lowerLip = root.getChild("lower_lip");
        this.back = root.getChild("back");
        this.leftSmallEar = root.getChild("left_small_ear");
        this.leftBigEar = root.getChild("left_big_ear");
        this.rightSmallEar = root.getChild("right_small_ear");
        this.rightBigEar = root.getChild("right_big_ear");
        this.hump = root.getChild("hump");
        this.body = root.getChild("body");
        this.skirt = root.getChild("skirt");
        this.rightTuskA = root.getChild("right_tusk_a");
        this.rightTuskB = root.getChild("right_tusk_b");
        this.rightTuskC = root.getChild("right_tusk_c");
        this.rightTuskD = root.getChild("right_tusk_d");
        this.leftTuskA = root.getChild("left_tusk_a");
        this.leftTuskB = root.getChild("left_tusk_b");
        this.leftTuskC = root.getChild("left_tusk_c");
        this.leftTuskD = root.getChild("left_tusk_d");
        this.trunkA = root.getChild("trunk_a");
        this.trunkB = root.getChild("trunk_b");
        this.trunkC = root.getChild("trunk_c");
        this.trunkD = root.getChild("trunk_d");
        this.trunkE = root.getChild("trunk_e");
        this.frontRightUpperLeg = root.getChild("front_right_upper_leg");
        this.frontRightLowerLeg = root.getChild("front_right_lower_leg");
        this.frontLeftUpperLeg = root.getChild("front_left_upper_leg");
        this.frontLeftLowerLeg = root.getChild("front_left_lower_leg");
        this.backRightUpperLeg = root.getChild("back_right_upper_leg");
        this.backRightLowerLeg = root.getChild("back_right_lower_leg");
        this.backLeftUpperLeg = root.getChild("back_left_upper_leg");
        this.backLeftLowerLeg = root.getChild("back_left_lower_leg");
        this.tailRoot = root.getChild("tail_root");
        this.tail = root.getChild("tail");
        this.tailPlush = root.getChild("tail_plush");

        this.tuskLW1 = root.getChild("tusk_lw1");
        this.tuskLW2 = root.getChild("tusk_lw2");
        this.tuskLW3 = root.getChild("tusk_lw3");
        this.tuskLW4 = root.getChild("tusk_lw4");
        this.tuskLW5 = root.getChild("tusk_lw5");
        this.tuskRW1 = root.getChild("tusk_rw1");
        this.tuskRW2 = root.getChild("tusk_rw2");
        this.tuskRW3 = root.getChild("tusk_rw3");
        this.tuskRW4 = root.getChild("tusk_rw4");
        this.tuskRW5 = root.getChild("tusk_rw5");
        this.tuskLI1 = root.getChild("tusk_li1");
        this.tuskLI2 = root.getChild("tusk_li2");
        this.tuskLI3 = root.getChild("tusk_li3");
        this.tuskLI4 = root.getChild("tusk_li4");
        this.tuskLI5 = root.getChild("tusk_li5");
        this.tuskRI1 = root.getChild("tusk_ri1");
        this.tuskRI2 = root.getChild("tusk_ri2");
        this.tuskRI3 = root.getChild("tusk_ri3");
        this.tuskRI4 = root.getChild("tusk_ri4");
        this.tuskRI5 = root.getChild("tusk_ri5");
        this.tuskLD1 = root.getChild("tusk_ld1");
        this.tuskLD2 = root.getChild("tusk_ld2");
        this.tuskLD3 = root.getChild("tusk_ld3");
        this.tuskLD4 = root.getChild("tusk_ld4");
        this.tuskLD5 = root.getChild("tusk_ld5");
        this.tuskRD1 = root.getChild("tusk_rd1");
        this.tuskRD2 = root.getChild("tusk_rd2");
        this.tuskRD3 = root.getChild("tusk_rd3");
        this.tuskRD4 = root.getChild("tusk_rd4");
        this.tuskRD5 = root.getChild("tusk_rd5");

        this.harnessBlanket = root.getChild("harness_blanket");
        this.harnessUpperBelt = root.getChild("harness_upper_belt");
        this.harnessLowerBelt = root.getChild("harness_lower_belt");

        this.cabinPillow = root.getChild("cabin_pillow");
        this.cabinLeftRail = root.getChild("cabin_left_rail");
        this.cabin = root.getChild("cabin");
        this.cabinRightRail = root.getChild("cabin_right_rail");
        this.cabinBackRail = root.getChild("cabin_back_rail");
        this.cabinRoof = root.getChild("cabin_roof");

        this.fortNeckBeam = root.getChild("fort_neck_beam");
        this.fortBackBeam = root.getChild("fort_back_beam");
        this.fortFloor1 = root.getChild("fort_floor1");
        this.fortFloor2 = root.getChild("fort_floor2");
        this.fortFloor3 = root.getChild("fort_floor3");
        this.fortBackWall = root.getChild("fort_back_wall");
        this.fortBackLeftWall = root.getChild("fort_back_left_wall");
        this.fortBackRightWall = root.getChild("fort_back_right_wall");

        this.storageRightBedroll = root.getChild("storage_right_bedroll");
        this.storageLeftBedroll = root.getChild("storage_left_bedroll");
        this.storageFrontRightChest = root.getChild("storage_front_right_chest");
        this.storageBackRightChest = root.getChild("storage_back_right_chest");
        this.storageFrontLeftChest = root.getChild("storage_front_left_chest");
        this.storageBackLeftChest = root.getChild("storage_back_left_chest");
        this.storageRightBlankets = root.getChild("storage_right_blankets");
        this.storageLeftBlankets = root.getChild("storage_left_blankets");
        this.storageUpLeft = root.getChild("storage_up_left");
        this.storageUpRight = root.getChild("storage_up_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(60, 0).addBox(-5.5F, -6.0F, -8.0F, 11.0F, 15.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.1745329F, 0.0F, 0.0F));

        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(46, 48).addBox(-4.95F, -6.0F, -8.0F, 10.0F, 14.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -10.0F, -0.2617994F, 0.0F, 0.0F));

        root.addOrReplaceChild("head_bump",
                CubeListBuilder.create().texOffs(104, 41).addBox(-3.0F, -9.0F, -6.0F, 6.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.1745329F, 0.0F, 0.0F));

        root.addOrReplaceChild("chin",
                CubeListBuilder.create().texOffs(86, 56).addBox(-1.5F, -6.0F, -10.7F, 3.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 2.054118F, 0.0F, 0.0F));

        root.addOrReplaceChild("lower_lip",
                CubeListBuilder.create().texOffs(80, 65).addBox(-2.0F, -2.0F, -14.0F, 4.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 1.570796F, 0.0F, 0.0F));

        root.addOrReplaceChild("back",
                CubeListBuilder.create().texOffs(0, 48).addBox(-5.0F, -10.0F, -10.0F, 10.0F, 2.0F, 26.0F),
                PartPose.offset(0.0F, -4.0F, -3.0F));

        root.addOrReplaceChild("left_small_ear",
                CubeListBuilder.create().texOffs(102, 0).addBox(2.0F, -8.0F, -5.0F, 8.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.1745329F, -0.5235988F, 0.5235988F));

        root.addOrReplaceChild("left_big_ear",
                CubeListBuilder.create().texOffs(102, 0).addBox(2.0F, -8.0F, -5.0F, 12.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.1745329F, -0.5235988F, 0.5235988F));

        root.addOrReplaceChild("right_small_ear",
                CubeListBuilder.create().texOffs(106, 15).addBox(-10.0F, -8.0F, -5.0F, 8.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.1745329F, 0.5235988F, -0.5235988F));

        root.addOrReplaceChild("right_big_ear",
                CubeListBuilder.create().texOffs(102, 15).addBox(-14.0F, -8.0F, -5.0F, 12.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.1745329F, 0.5235988F, -0.5235988F));

        root.addOrReplaceChild("hump",
                CubeListBuilder.create().texOffs(88, 30).addBox(-6.0F, -2.0F, -3.0F, 12.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, -13.0F, -5.5F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -10.0F, -10.0F, 16.0F, 20.0F, 28.0F),
                PartPose.offset(0.0F, -2.0F, -3.0F));

        root.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(28, 94).addBox(-8.0F, -10.0F, -6.0F, 16.0F, 28.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, -3.0F, 1.570796F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_tusk_a",
                CubeListBuilder.create().texOffs(2, 60).addBox(-3.8F, -3.5F, -19.0F, 2.0F, 2.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 1.22173F, 0.0F, 0.1745329F));

        root.addOrReplaceChild("right_tusk_b",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.8F, 6.2F, -24.2F, 2.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, 0.1745329F));

        root.addOrReplaceChild("right_tusk_c",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3.8F, 17.1F, -21.9F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, 0.1745329F));

        root.addOrReplaceChild("right_tusk_d",
                CubeListBuilder.create().texOffs(14, 18).addBox(-3.8F, 25.5F, -14.5F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, 0.1745329F));

        root.addOrReplaceChild("left_tusk_a",
                CubeListBuilder.create().texOffs(2, 48).addBox(1.8F, -3.5F, -19.0F, 2.0F, 2.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 1.22173F, 0.0F, -0.1745329F));

        root.addOrReplaceChild("left_tusk_b",
                CubeListBuilder.create().texOffs(0, 9).addBox(1.8F, 6.2F, -24.2F, 2.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, -0.1745329F));

        root.addOrReplaceChild("left_tusk_c",
                CubeListBuilder.create().texOffs(0, 18).addBox(1.8F, 17.1F, -21.9F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, -0.1745329F));

        root.addOrReplaceChild("left_tusk_d",
                CubeListBuilder.create().texOffs(14, 18).addBox(1.8F, 25.5F, -14.5F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, -0.1745329F));

        root.addOrReplaceChild("trunk_a",
                CubeListBuilder.create().texOffs(0, 76).addBox(-4.0F, -2.5F, -18.0F, 8.0F, 7.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -22.46667F, 1.570796F, 0.0F, 0.0F));

        root.addOrReplaceChild("trunk_b",
                CubeListBuilder.create().texOffs(0, 93).addBox(-3.0F, -2.5F, -7.0F, 6.0F, 5.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 6.5F, -22.5F, 1.658063F, 0.0F, 0.0F));

        root.addOrReplaceChild("trunk_c",
                CubeListBuilder.create().texOffs(0, 105).addBox(-2.5F, -2.0F, -4.0F, 5.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 13.0F, -22.0F, 1.919862F, 0.0F, 0.0F));

        root.addOrReplaceChild("trunk_d",
                CubeListBuilder.create().texOffs(0, 114).addBox(-2.0F, -1.5F, -5.0F, 4.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, -21.5F, 2.216568F, 0.0F, 0.0F));

        root.addOrReplaceChild("trunk_e",
                CubeListBuilder.create().texOffs(0, 122).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 19.5F, -19.0F, 2.530727F, 0.0F, 0.0F));

        root.addOrReplaceChild("front_right_upper_leg",
                CubeListBuilder.create().texOffs(100, 109).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 12.0F, 7.0F),
                PartPose.offset(-4.6F, 4.0F, -9.6F));

        root.addOrReplaceChild("front_right_lower_leg",
                CubeListBuilder.create().texOffs(100, 73).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 10.0F, 7.0F),
                PartPose.offset(-4.6F, 14.0F, -9.6F));

        root.addOrReplaceChild("front_left_upper_leg",
                CubeListBuilder.create().texOffs(100, 90).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 12.0F, 7.0F),
                PartPose.offset(4.6F, 4.0F, -9.6F));

        root.addOrReplaceChild("front_left_lower_leg",
                CubeListBuilder.create().texOffs(72, 73).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 10.0F, 7.0F),
                PartPose.offset(4.6F, 14.0F, -9.6F));

        root.addOrReplaceChild("back_right_upper_leg",
                CubeListBuilder.create().texOffs(72, 109).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 12.0F, 7.0F),
                PartPose.offset(-4.6F, 4.0F, 11.6F));

        root.addOrReplaceChild("back_right_lower_leg",
                CubeListBuilder.create().texOffs(100, 56).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 10.0F, 7.0F),
                PartPose.offset(-4.6F, 14.0F, 11.6F));

        root.addOrReplaceChild("back_left_upper_leg",
                CubeListBuilder.create().texOffs(72, 90).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 12.0F, 7.0F),
                PartPose.offset(4.6F, 4.0F, 11.6F));

        root.addOrReplaceChild("back_left_lower_leg",
                CubeListBuilder.create().texOffs(44, 77).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 10.0F, 7.0F),
                PartPose.offset(4.6F, 14.0F, 11.6F));

        root.addOrReplaceChild("tail_root",
                CubeListBuilder.create().texOffs(20, 105).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 15.0F, 0.296706F, 0.0F, 0.0F));

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(20, 117).addBox(-1.0F, 9.7F, -0.2F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 15.0F, 0.1134464F, 0.0F, 0.0F));

        root.addOrReplaceChild("tail_plush",
                CubeListBuilder.create().texOffs(26, 76).addBox(-1.5F, 15.5F, -0.7F, 3.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 15.0F, 0.1134464F, 0.0F, 0.0F));

        // ---- Tusk armour sets (ported verbatim from legacy: texOffs + boxes + poses). All share the
        // head pivot (0, -10, -16.5). Only the matching tier's 10 cubes render at once (see setupAnim). ----
        // Wood (tier 1)
        root.addOrReplaceChild("tusk_lw1",
                CubeListBuilder.create().texOffs(56, 166).addBox(1.3F, 5.5F, -24.2F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_lw2",
                CubeListBuilder.create().texOffs(60, 158).addBox(1.29F, 16.5F, -21.9F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_lw3",
                CubeListBuilder.create().texOffs(58, 149).addBox(1.3F, 24.9F, -15.5F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_lw4",
                CubeListBuilder.create()
                        .texOffs(46, 164).addBox(2.7F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, WEST_FACE)
                        .texOffs(41, 164).addBox(2.7F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_lw5",
                CubeListBuilder.create()
                        .texOffs(52, 192).addBox(2.7F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, WEST_FACE)
                        .texOffs(44, 192).addBox(2.7F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_rw1",
                CubeListBuilder.create().texOffs(56, 166).addBox(-4.3F, 5.5F, -24.2F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rw2",
                CubeListBuilder.create().texOffs(60, 158).addBox(-4.29F, 16.5F, -21.9F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rw3",
                CubeListBuilder.create().texOffs(58, 149).addBox(-4.3F, 24.9F, -15.5F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rw4",
                CubeListBuilder.create()
                        .texOffs(46, 157).addBox(-2.8F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, WEST_FACE)
                        .texOffs(41, 157).addBox(-2.8F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rw5",
                CubeListBuilder.create()
                        .texOffs(52, 199).addBox(-2.8F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, WEST_FACE)
                        .texOffs(44, 199).addBox(-2.8F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, 0.1745329F));

        // Iron (tier 2)
        root.addOrReplaceChild("tusk_li1",
                CubeListBuilder.create().texOffs(108, 180).addBox(1.3F, 5.5F, -24.2F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_li2",
                CubeListBuilder.create().texOffs(112, 172).addBox(1.29F, 16.5F, -21.9F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_li3",
                CubeListBuilder.create().texOffs(110, 163).addBox(1.3F, 24.9F, -15.5F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_li4",
                CubeListBuilder.create()
                        .texOffs(96, 175).addBox(2.7F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, WEST_FACE)
                        .texOffs(91, 175).addBox(2.7F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_li5",
                CubeListBuilder.create()
                        .texOffs(112, 209).addBox(2.7F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, WEST_FACE)
                        .texOffs(104, 209).addBox(2.7F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_ri1",
                CubeListBuilder.create().texOffs(108, 180).addBox(-4.3F, 5.5F, -24.2F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_ri2",
                CubeListBuilder.create().texOffs(112, 172).addBox(-4.29F, 16.5F, -21.9F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_ri3",
                CubeListBuilder.create().texOffs(110, 163).addBox(-4.3F, 24.9F, -15.5F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_ri4",
                CubeListBuilder.create()
                        .texOffs(96, 163).addBox(-2.8F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, WEST_FACE)
                        .texOffs(91, 163).addBox(-2.8F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_ri5",
                CubeListBuilder.create()
                        .texOffs(112, 216).addBox(-2.8F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, WEST_FACE)
                        .texOffs(104, 216).addBox(-2.8F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, 0.1745329F));

        // Diamond (tier 3)
        root.addOrReplaceChild("tusk_ld1",
                CubeListBuilder.create().texOffs(108, 207).addBox(1.3F, 5.5F, -24.2F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_ld2",
                CubeListBuilder.create().texOffs(112, 199).addBox(1.29F, 16.5F, -21.9F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_ld3",
                CubeListBuilder.create().texOffs(110, 190).addBox(1.3F, 24.9F, -15.5F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_ld4",
                CubeListBuilder.create()
                        .texOffs(86, 175).addBox(2.7F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, WEST_FACE)
                        .texOffs(81, 175).addBox(2.7F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_ld5",
                CubeListBuilder.create()
                        .texOffs(112, 225).addBox(2.7F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, WEST_FACE)
                        .texOffs(104, 225).addBox(2.7F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("tusk_rd1",
                CubeListBuilder.create().texOffs(108, 207).addBox(-4.3F, 5.5F, -24.2F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.6981317F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rd2",
                CubeListBuilder.create().texOffs(112, 199).addBox(-4.29F, 16.5F, -21.9F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rd3",
                CubeListBuilder.create().texOffs(110, 190).addBox(-4.3F, 24.9F, -15.5F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rd4",
                CubeListBuilder.create()
                        .texOffs(86, 163).addBox(-2.8F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, WEST_FACE)
                        .texOffs(81, 163).addBox(-2.8F, 14.5F, -21.9F, 0.0F, 7.0F, 5.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, 0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("tusk_rd5",
                CubeListBuilder.create()
                        .texOffs(112, 232).addBox(-2.8F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, WEST_FACE)
                        .texOffs(104, 232).addBox(-2.8F, 22.9F, -17.5F, 0.0F, 7.0F, 8.0F, EAST_FACE),
                PartPose.offsetAndRotation(0.0F, -10.0F, -16.5F, -0.3490659F, 0.0F, 0.1745329F));

        // ---- Harness (armorStage >= 1) ----
        root.addOrReplaceChild("harness_blanket",
                CubeListBuilder.create().texOffs(0, 196).addBox(-8.5F, -2.0F, -3.0F, 17.0F, 14.0F, 18.0F),
                PartPose.offset(0.0F, -13.2F, -3.5F));
        root.addOrReplaceChild("harness_upper_belt",
                CubeListBuilder.create().texOffs(70, 196).addBox(-8.5F, 0.5F, -2.0F, 17.0F, 10.0F, 2.0F),
                PartPose.offset(0.0F, -2.0F, -2.5F));
        root.addOrReplaceChild("harness_lower_belt",
                CubeListBuilder.create().texOffs(70, 196).addBox(-8.5F, 0.5F, -2.5F, 17.0F, 10.0F, 2.0F),
                PartPose.offset(0.0F, -2.0F, 7.0F));

        // ---- Howdah cabin (type 5, armorStage >= 3) ----
        // Howdah cabin enlarged ~1.3x wider/deeper and taller so a seated player fits inside (was too
        // narrow and short). The size comes from ModelPart.x/y/zScale set in setupAnim() (which scales the
        // cube AROUND its pivot); we must NOT use PartPose.scaled() here because that also multiplies the
        // pivot translation (y*1.4) and would launch the cabin into the air. The walls' pivot is raised to
        // model Y -43 so that with yScale 1.4 the cube's far (floor) end lands back at model Y -15
        // (world 2.5, the seat) while the roof rises to -43 (world 4.2) for headroom.
        root.addOrReplaceChild("cabin_pillow",
                CubeListBuilder.create().texOffs(76, 146).addBox(-6.5F, 0.0F, -6.5F, 13.0F, 4.0F, 13.0F),
                PartPose.offset(0.0F, -16.0F, 2.0F));
        root.addOrReplaceChild("cabin_left_rail",
                CubeListBuilder.create().texOffs(56, 147).addBox(-7.0F, 0.0F, 7.0F, 14.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -29.0F, 1.5F, 0.0F, 1.570796F, 0.0F));
        root.addOrReplaceChild("cabin",
                CubeListBuilder.create().texOffs(0, 128).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 20.0F, 14.0F),
                PartPose.offset(0.0F, -43.0F, 2.0F));
        root.addOrReplaceChild("cabin_right_rail",
                CubeListBuilder.create().texOffs(56, 147).addBox(-7.0F, 0.0F, 7.0F, 14.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -29.0F, 1.5F, 0.0F, -1.570796F, 0.0F));
        root.addOrReplaceChild("cabin_back_rail",
                CubeListBuilder.create().texOffs(56, 147).addBox(-7.0F, 0.0F, 7.0F, 14.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -29.0F, 1.5F));
        root.addOrReplaceChild("cabin_roof",
                CubeListBuilder.create().texOffs(56, 128).addBox(-7.5F, 0.0F, -7.5F, 15.0F, 4.0F, 15.0F),
                // Pivot one unit below the walls' top edge (-43) so the roof slab's top face does not sit
                // coplanar with the walls' top face — that coincidence was the red-roof z-fighting flicker.
                PartPose.offset(0.0F, -42.0F, 2.0F));

        // ---- Mammoth fort platform (type 3/4, armorStage >= 3) ----
        root.addOrReplaceChild("fort_neck_beam",
                CubeListBuilder.create().texOffs(26, 180).addBox(-12.0F, 0.0F, -20.5F, 24.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -16.0F, 10.0F));
        root.addOrReplaceChild("fort_back_beam",
                CubeListBuilder.create().texOffs(26, 180).addBox(-12.0F, 0.0F, 0.0F, 24.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -16.0F, 10.0F));
        root.addOrReplaceChild("fort_floor1",
                CubeListBuilder.create().texOffs(0, 176).addBox(-0.5F, -20.0F, -6.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, -16.0F, 10.0F, 1.570796F, 0.0F, 1.570796F));
        root.addOrReplaceChild("fort_floor2",
                CubeListBuilder.create().texOffs(0, 176).addBox(-0.5F, -12.0F, -6.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, -16.0F, 10.0F, 1.570796F, 0.0F, 1.570796F));
        root.addOrReplaceChild("fort_floor3",
                CubeListBuilder.create().texOffs(0, 176).addBox(-0.5F, -4.0F, -6.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, -16.0F, 10.0F, 1.570796F, 0.0F, 1.570796F));
        root.addOrReplaceChild("fort_back_wall",
                CubeListBuilder.create().texOffs(0, 176).addBox(-5.0F, -6.2F, -6.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, -16.0F, 10.0F, 0.0F, 1.570796F, 0.0F));
        root.addOrReplaceChild("fort_back_left_wall",
                CubeListBuilder.create().texOffs(0, 176).addBox(6.0F, -6.0F, -7.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offset(0.0F, -16.0F, 10.0F));
        root.addOrReplaceChild("fort_back_right_wall",
                CubeListBuilder.create().texOffs(0, 176).addBox(-7.0F, -6.0F, -7.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offset(0.0F, -16.0F, 10.0F));

        // ---- Storage chests / bedrolls / blankets (hasChest) ----
        root.addOrReplaceChild("storage_right_bedroll",
                CubeListBuilder.create().texOffs(90, 231).addBox(-2.5F, 8.0F, -8.0F, 3.0F, 3.0F, 16.0F),
                PartPose.offsetAndRotation(-9.0F, -10.2F, 1.0F, 0.0F, 0.0F, 0.418879F));
        root.addOrReplaceChild("storage_left_bedroll",
                CubeListBuilder.create().texOffs(90, 231).addBox(-0.5F, 8.0F, -8.0F, 3.0F, 3.0F, 16.0F),
                PartPose.offsetAndRotation(9.0F, -10.2F, 1.0F, 0.0F, 0.0F, -0.418879F));
        root.addOrReplaceChild("storage_front_right_chest",
                CubeListBuilder.create().texOffs(76, 208).addBox(-3.5F, 0.0F, -5.0F, 5.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(-11.0F, -1.2F, -4.5F, 0.0F, 0.0F, -0.2617994F));
        root.addOrReplaceChild("storage_back_right_chest",
                CubeListBuilder.create().texOffs(76, 208).addBox(-3.5F, 0.0F, -5.0F, 5.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(-11.0F, -1.2F, 6.5F, 0.0F, 0.0F, -0.2617994F));
        root.addOrReplaceChild("storage_front_left_chest",
                CubeListBuilder.create().texOffs(76, 226).addBox(-1.5F, 0.0F, -5.0F, 5.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(11.0F, -1.2F, -4.5F, 0.0F, 0.0F, 0.2617994F));
        root.addOrReplaceChild("storage_back_left_chest",
                CubeListBuilder.create().texOffs(76, 226).addBox(-1.5F, 0.0F, -5.0F, 5.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(11.0F, -1.2F, 6.5F, 0.0F, 0.0F, 0.2617994F));
        root.addOrReplaceChild("storage_right_blankets",
                CubeListBuilder.create().texOffs(0, 228).addBox(-4.5F, -1.0F, -7.0F, 5.0F, 10.0F, 14.0F),
                PartPose.offset(-9.0F, -10.2F, 1.0F));
        root.addOrReplaceChild("storage_left_blankets",
                CubeListBuilder.create().texOffs(38, 228).addBox(-0.5F, -1.0F, -7.0F, 5.0F, 10.0F, 14.0F),
                PartPose.offset(9.0F, -10.2F, 1.0F));
        root.addOrReplaceChild("storage_up_left",
                CubeListBuilder.create().texOffs(76, 226).addBox(6.5F, 1.0F, -14.0F, 5.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -16.0F, 10.0F, 0.0F, 0.0F, -0.3839724F));
        root.addOrReplaceChild("storage_up_right",
                CubeListBuilder.create().texOffs(76, 208).addBox(-11.5F, 1.0F, -14.0F, 5.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -16.0F, 10.0F, 0.0F, 0.0F, 0.3839724F));

        return LayerDefinition.create(mesh, 128, 256);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        float headYaw = state.yRot;
        if (headYaw > 20.0F) headYaw = 20.0F;
        if (headYaw < -20.0F) headYaw = -20.0F;
        float headYRot = headYaw * DEG_TO_RAD;

        float headPitch = state.xRot;
        if (headPitch < 0.0F) headPitch = 0.0F;
        float headXRot = headPitch * DEG_TO_RAD;

        // gait
        float rLegXRot = Mth.cos((limbSwing * 0.6662F) + (float) Math.PI) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;

        int type = state.typeMoC;

        this.head.xRot = (-10.0F * DEG_TO_RAD) + headXRot;
        this.head.yRot = headYRot;
        this.headBump.xRot = this.head.xRot;
        this.headBump.yRot = this.head.yRot;
        // The head bump is a mammoth-only feature (legacy renders it only for types 3 and 4).
        this.headBump.visible = (type == 3 || type == 4);

        this.rightTuskA.yRot = headYRot;
        this.leftTuskA.yRot = headYRot;
        this.rightTuskA.xRot = (70.0F * DEG_TO_RAD) + headXRot;
        this.leftTuskA.xRot = (70.0F * DEG_TO_RAD) + headXRot;

        this.chin.yRot = headYRot;
        this.chin.xRot = (113.0F * DEG_TO_RAD) + headXRot;
        this.lowerLip.yRot = headYRot;
        this.lowerLip.xRot = (85.0F * DEG_TO_RAD) + headXRot;

        // ears track head + a small periodic idle flap (faithful to the legacy ear wiggle: the ears
        // splay outward on a slow cosine cycle). Right ears open with +earFlap, left with -earFlap so
        // they flap symmetrically outward.
        float earFlap = Mth.cos(state.ageInTicks * 0.5F) * 0.35F;
        this.rightBigEar.yRot = (30.0F * DEG_TO_RAD) + headYRot + earFlap;
        this.rightSmallEar.yRot = (30.0F * DEG_TO_RAD) + headYRot + earFlap;
        this.leftBigEar.yRot = (-30.0F * DEG_TO_RAD) + headYRot - earFlap;
        this.leftSmallEar.yRot = (-30.0F * DEG_TO_RAD) + headYRot - earFlap;
        this.rightBigEar.xRot = (-10.0F * DEG_TO_RAD) + headXRot;
        this.rightSmallEar.xRot = (-10.0F * DEG_TO_RAD) + headXRot;
        this.leftBigEar.xRot = (-10.0F * DEG_TO_RAD) + headXRot;
        this.leftSmallEar.xRot = (-10.0F * DEG_TO_RAD) + headXRot;

        // Only one ear pair renders at a time (the legacy render() chooses by type): the African
        // elephant (type 1) has the big flapping ears, every other elephant/mammoth uses the small
        // ears. Showing both pairs at once made them overlap and z-fight.
        boolean bigEars = (type == 1);
        this.leftBigEar.visible = bigEars;
        this.rightBigEar.visible = bigEars;
        this.leftSmallEar.visible = !bigEars;
        this.rightSmallEar.visible = !bigEars;

        // trunk sway
        //
        // The trunk is a 5-segment chain that hangs forward/down from the FRONT of the head.
        // In the legacy model the segments are not real children of the head; instead each
        // segment's pivot is recomputed every frame from its parent's pivot + rotation via
        // adjustAllRotationPoints (TrunkA off Head, TrunkB off TrunkA, ...). The static pivots
        // baked into the mesh (Y = 6.5, 13, 16, 19.5) are authoring placeholders that the legacy
        // code overwrites; rendering them as-is drops the trunk far below the body (the bug).
        // We restore the authored rest pivots, set the rotations, then re-chain them exactly
        // like the legacy model so the trunk extends from the head.
        // Idle trunk swing (faithful to the legacy "random Trunk animation"): the trunk curls up more
        // as the elephant walks (from the limb-swing speed) and adds a slow idle sway when standing.
        // The magnitude propagates DECREASINGLY down the 5 segments via the legacy factors
        // (1.5x, 3x, 4.5x, 6x). trunkXRot is in degrees, matching the legacy formulas.
        float trunkXRot;
        if (limbAmount > 0.5F) {
            // Walking fast: a stronger cyclic swing driven by the age timer.
            trunkXRot = Mth.cos(state.ageInTicks * 0.35F) * 4.0F;
        } else {
            // Standing / slow: a gentle idle sway, plus an occasional trunk RAISE that trumpets up and
            // settles back (approximating the legacy "random Trunk animation" without an entity counter —
            // a smooth ~2s lift every ~10s, phased per-elephant by its own age timer).
            float base = (limbAmount * 50.0F) + Mth.cos(state.ageInTicks * 0.1F) * 1.5F;
            float phase = (state.ageInTicks % 200.0F) / 200.0F; // 0..1 over ~10s
            float raise = phase < 0.2F ? Mth.sin((phase / 0.2F) * (float) Math.PI) * 12.0F : 0.0F;
            trunkXRot = base + raise;
        }

        float trunkARotX = 90.0F - trunkXRot;
        if (trunkARotX < 85.0F) trunkARotX = 85.0F;

        this.trunkA.yRot = headYRot;
        this.trunkA.xRot = (trunkARotX * DEG_TO_RAD) + headXRot;
        this.trunkB.yRot = headYRot;
        this.trunkB.xRot = ((95.0F - trunkXRot * 1.5F) * DEG_TO_RAD) + headXRot;
        this.trunkC.yRot = headYRot;
        this.trunkC.xRot = ((110.0F - trunkXRot * 3.0F) * DEG_TO_RAD) + headXRot;
        this.trunkD.yRot = headYRot;
        this.trunkD.xRot = ((127.0F - trunkXRot * 4.5F) * DEG_TO_RAD) + headXRot;
        this.trunkE.yRot = headYRot;
        this.trunkE.xRot = ((145.0F - trunkXRot * 6.0F) * DEG_TO_RAD) + headXRot;

        // Restore authored rest pivots (the chain below mutates pivots, so we must reset each
        // frame). Y values match the mesh definition; Z values match the legacy per-frame overrides.
        this.trunkA.x = 0.0F; this.trunkA.y = -3.0F;  this.trunkA.z = -22.5F;
        this.trunkB.x = 0.0F; this.trunkB.y = 6.5F;   this.trunkB.z = -22.5F;
        this.trunkC.x = 0.0F; this.trunkC.y = 13.0F;  this.trunkC.z = -22.5F;
        this.trunkD.x = 0.0F; this.trunkD.y = 16.0F;  this.trunkD.z = -21.5F;
        this.trunkE.x = 0.0F; this.trunkE.y = 19.5F;  this.trunkE.z = -19.0F;

        // Chain the trunk segments off the head and off each other (matches legacy).
        adjustAllRotationPoints(this.trunkA, this.head);
        adjustAllRotationPoints(this.trunkB, this.trunkA);
        adjustAllRotationPoints(this.trunkC, this.trunkB);
        adjustAllRotationPoints(this.trunkD, this.trunkC);
        adjustAllRotationPoints(this.trunkE, this.trunkD);

        // legs
        this.frontRightUpperLeg.xRot = rLegXRot;
        this.frontLeftUpperLeg.xRot = lLegXRot;
        this.backLeftUpperLeg.xRot = rLegXRot;
        this.backRightUpperLeg.xRot = lLegXRot;

        this.frontRightLowerLeg.xRot = rLegXRot;
        this.frontLeftLowerLeg.xRot = lLegXRot;
        this.backLeftLowerLeg.xRot = rLegXRot;
        this.backRightLowerLeg.xRot = lLegXRot;

        // tusks track head (default state)
        this.leftTuskB.yRot = headYRot;
        this.leftTuskC.yRot = headYRot;
        this.leftTuskD.yRot = headYRot;
        this.rightTuskB.yRot = headYRot;
        this.rightTuskC.yRot = headYRot;
        this.rightTuskD.yRot = headYRot;
        this.leftTuskB.xRot = (40.0F * DEG_TO_RAD) + headXRot;
        this.leftTuskC.xRot = (10.0F * DEG_TO_RAD) + headXRot;
        this.leftTuskD.xRot = (-20.0F * DEG_TO_RAD) + headXRot;
        this.rightTuskB.xRot = (40.0F * DEG_TO_RAD) + headXRot;
        this.rightTuskC.xRot = (10.0F * DEG_TO_RAD) + headXRot;
        this.rightTuskD.xRot = (-20.0F * DEG_TO_RAD) + headXRot;

        // ---- Tusk armour sets ----
        // Every tusk-set cube tracks the head (yaw + the legacy per-cube pitch offsets). Then exactly
        // one tier's 10 cubes is shown (1 wood / 2 iron / 3 diamond); tier 0 shows none. The base tusk
        // extensions (B/C/D) are hidden while an armour set is worn, so the sets sit in their place.
        int tusks = state.tusks;
        applyTuskSetAngles(this.tuskLW1, this.tuskLW2, this.tuskLW3, this.tuskLW4, this.tuskLW5, headYRot, headXRot);
        applyTuskSetAngles(this.tuskRW1, this.tuskRW2, this.tuskRW3, this.tuskRW4, this.tuskRW5, headYRot, headXRot);
        applyTuskSetAngles(this.tuskLI1, this.tuskLI2, this.tuskLI3, this.tuskLI4, this.tuskLI5, headYRot, headXRot);
        applyTuskSetAngles(this.tuskRI1, this.tuskRI2, this.tuskRI3, this.tuskRI4, this.tuskRI5, headYRot, headXRot);
        applyTuskSetAngles(this.tuskLD1, this.tuskLD2, this.tuskLD3, this.tuskLD4, this.tuskLD5, headYRot, headXRot);
        applyTuskSetAngles(this.tuskRD1, this.tuskRD2, this.tuskRD3, this.tuskRD4, this.tuskRD5, headYRot, headXRot);

        boolean woodTusks = (tusks == 1);
        boolean ironTusks = (tusks == 2);
        boolean diamondTusks = (tusks == 3);
        setTuskSetVisible(this.tuskLW1, this.tuskLW2, this.tuskLW3, this.tuskLW4, this.tuskLW5, woodTusks);
        setTuskSetVisible(this.tuskRW1, this.tuskRW2, this.tuskRW3, this.tuskRW4, this.tuskRW5, woodTusks);
        setTuskSetVisible(this.tuskLI1, this.tuskLI2, this.tuskLI3, this.tuskLI4, this.tuskLI5, ironTusks);
        setTuskSetVisible(this.tuskRI1, this.tuskRI2, this.tuskRI3, this.tuskRI4, this.tuskRI5, ironTusks);
        setTuskSetVisible(this.tuskLD1, this.tuskLD2, this.tuskLD3, this.tuskLD4, this.tuskLD5, diamondTusks);
        setTuskSetVisible(this.tuskRD1, this.tuskRD2, this.tuskRD3, this.tuskRD4, this.tuskRD5, diamondTusks);

        // Hide the bare tusk extensions when an armour set replaces them (the base tusk root A stays).
        boolean bareTusks = (tusks == 0);
        this.leftTuskB.visible = bareTusks;
        this.leftTuskC.visible = bareTusks;
        this.leftTuskD.visible = bareTusks;
        this.rightTuskB.visible = bareTusks;
        this.rightTuskC.visible = bareTusks;
        this.rightTuskD.visible = bareTusks;

        // tail: droop scales with walk speed, plus a subtle idle side-to-side yaw flick (legacy tail
        // wiggle, cos(t*0.4)*1.3 but softened here so it doesn't fight the walk animation).
        float tailMov = limbAmount * 0.9F;
        if (tailMov < 0.0F) tailMov = 0.0F;
        float tailFlick = Mth.cos(state.ageInTicks * 0.4F) * 0.15F;
        this.tailRoot.yRot = tailFlick;
        this.tail.yRot = tailFlick;
        this.tailPlush.yRot = tailFlick;
        this.tailRoot.xRot = (17.0F * DEG_TO_RAD) + tailMov;
        this.tail.xRot = (6.5F * DEG_TO_RAD) + tailMov;
        this.tailPlush.xRot = (6.5F * DEG_TO_RAD) + tailMov;

        // ---- Equipment sway (faithful to the legacy per-frame jiggle) ----
        this.storageLeftBedroll.xRot = lLegXRot / 10.0F;
        this.storageFrontLeftChest.xRot = lLegXRot / 5.0F;
        this.storageBackLeftChest.xRot = lLegXRot / 5.0F;
        this.storageRightBedroll.xRot = rLegXRot / 10.0F;
        this.storageFrontRightChest.xRot = rLegXRot / 5.0F;
        this.storageBackRightChest.xRot = rLegXRot / 5.0F;
        this.fortNeckBeam.zRot = lLegXRot / 50.0F;
        this.fortBackBeam.zRot = lLegXRot / 50.0F;
        this.fortBackRightWall.zRot = lLegXRot / 50.0F;
        this.fortBackLeftWall.zRot = lLegXRot / 50.0F;
        this.fortBackWall.xRot = -lLegXRot / 50.0F;

        // ---- Equipment visibility, gated on the render-state equip flags ----
        int stage = state.armorStage;

        boolean harness = stage >= 1;
        this.harnessBlanket.visible = harness;
        this.harnessUpperBelt.visible = harness;
        this.harnessLowerBelt.visible = harness;

        // The garment skirt is the type-5 (garmented Indian) overlay; mammoths (type 3/4) wear the
        // skirt as part of their base body, so it stays visible for them regardless of stage.
        boolean garment = stage >= 2;
        this.skirt.visible = (type == 3 || type == 4) || garment;

        boolean platform = stage >= 3;
        // The howdah cabin renders only on the decorated type 5 sheet (the only elephant texture that
        // actually paints the garment/cabin art). Equipping a garment or howdah converts the elephant to
        // type 5, so every non-mammoth elephant reaches this state; mammoths (3/4) use the fort platform.
        boolean cabinVisible = platform && type == 5;
        this.cabinPillow.visible = cabinVisible;
        this.cabinLeftRail.visible = cabinVisible;
        this.cabin.visible = cabinVisible;
        this.cabinRightRail.visible = cabinVisible;
        this.cabinBackRail.visible = cabinVisible;
        this.cabinRoof.visible = cabinVisible;
        // Enlarge the cabin in place: scale the cubes around their pivots (does NOT move the pivots, so
        // the cabin stays seated on the elephant's back). Walls grow 1.4x tall (floor anchored at the
        // raised pivot), everything 1.3x wider/deeper.
        this.cabin.xScale = 1.3F; this.cabin.yScale = 1.4F; this.cabin.zScale = 1.3F;
        this.cabinRoof.xScale = 1.3F; this.cabinRoof.zScale = 1.3F;
        this.cabinPillow.xScale = 1.3F; this.cabinPillow.zScale = 1.3F;
        this.cabinLeftRail.xScale = 1.3F; this.cabinLeftRail.zScale = 1.3F;
        this.cabinRightRail.xScale = 1.3F; this.cabinRightRail.zScale = 1.3F;
        this.cabinBackRail.xScale = 1.3F; this.cabinBackRail.zScale = 1.3F;

        // The mammoth fort platform belongs only to the Songhua mammoth (type 4); the legacy render()
        // gates the whole fort under type == 4 (type 3 mammoths never receive it).
        boolean fortVisible = platform && type == 4;
        this.fortNeckBeam.visible = fortVisible;
        this.fortBackBeam.visible = fortVisible;
        this.fortFloor1.visible = fortVisible;
        this.fortFloor2.visible = fortVisible;
        this.fortFloor3.visible = fortVisible;
        this.fortBackWall.visible = fortVisible;
        this.fortBackLeftWall.visible = fortVisible;
        this.fortBackRightWall.visible = fortVisible;

        boolean chest = state.hasChest;
        this.storageRightBedroll.visible = chest;
        this.storageLeftBedroll.visible = chest;
        this.storageFrontRightChest.visible = chest;
        this.storageBackRightChest.visible = chest;
        this.storageFrontLeftChest.visible = chest;
        this.storageBackLeftChest.visible = chest;
        this.storageRightBlankets.visible = chest;
        this.storageLeftBlankets.visible = chest;
        // The two raised storage crates sit on the platform, so only show them when both a chest is
        // installed and the platform is present to carry them.
        this.storageUpLeft.visible = chest && fortVisible;
        this.storageUpRight.visible = chest && fortVisible;
    }

    /**
     * Recomputes {@code target}'s pivot point so it hangs off the end of {@code origin}, following
     * {@code origin}'s current rotation. Ported verbatim from the legacy
     * {@code MoCModelElephant.adjustAllRotationPoints} so the trunk chain attaches to the head and
     * curls forward/down instead of rendering at its static (placeholder) pivot. The vertical
     * distance between the two parts' authored pivots is used as the segment length.
     */
    /**
     * Applies the legacy tusk-armour-set pose to one 5-cube tusk (cube pitch offsets 40/10/-20/10/-20
     * degrees, all tracking the head yaw + pitch). Matches the legacy per-cube {@code rotateAngleX}.
     */
    private static void applyTuskSetAngles(ModelPart c1, ModelPart c2, ModelPart c3, ModelPart c4, ModelPart c5,
                                           float headYRot, float headXRot) {
        c1.yRot = headYRot;
        c2.yRot = headYRot;
        c3.yRot = headYRot;
        c4.yRot = headYRot;
        c5.yRot = headYRot;
        c1.xRot = (40.0F * DEG_TO_RAD) + headXRot;
        c2.xRot = (10.0F * DEG_TO_RAD) + headXRot;
        c3.xRot = (-20.0F * DEG_TO_RAD) + headXRot;
        c4.xRot = (10.0F * DEG_TO_RAD) + headXRot;
        c5.xRot = (-20.0F * DEG_TO_RAD) + headXRot;
    }

    private static void setTuskSetVisible(ModelPart c1, ModelPart c2, ModelPart c3, ModelPart c4, ModelPart c5,
                                          boolean visible) {
        c1.visible = visible;
        c2.visible = visible;
        c3.visible = visible;
        c4.visible = visible;
        c5.visible = visible;
    }

    private static void adjustAllRotationPoints(ModelPart target, ModelPart origin) {
        float distanceY = Math.abs(target.y - origin.y);

        target.y = origin.y + Mth.sin(origin.xRot) * distanceY;
        target.z = origin.z - Mth.cos(origin.yRot) * (Mth.cos(origin.xRot) * distanceY);
        target.x = origin.x - Mth.sin(origin.yRot) * (Mth.cos(origin.xRot) * distanceY);
    }
}
