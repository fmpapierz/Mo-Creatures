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
 * Ostrich model, converted faithfully from the legacy {@code MoCModelOstrich} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the animation reproduces the legacy head-tracking,
 * leg gait and wing flap for the common (non-floating, unsaddled) case.
 */
public class MoCModelOstrich extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float RADIAN_F = 57.29578F;

    private final ModelPart head;
    private final ModelPart uBeak;
    private final ModelPart uBeak2;
    private final ModelPart lBeak;
    private final ModelPart lBeak2;
    private final ModelPart neckU;
    private final ModelPart neckD;
    private final ModelPart neckL;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart lLegA;
    private final ModelPart lLegB;
    private final ModelPart lLegC;
    private final ModelPart lFoot;
    private final ModelPart rLegA;
    private final ModelPart rLegB;
    private final ModelPart rLegC;
    private final ModelPart rFoot;
    private final ModelPart lWingB;
    private final ModelPart lWingC;
    private final ModelPart rWingB;
    private final ModelPart rWingC;
    // Type-specific geometry the legacy model built (ported).
    // Unicorn horn — shown only for type 8.
    private final ModelPart uniHorn;
    // Demon/darkness alternate wings + neck feathers — shown for type 5 or 6 (replacing the normal wing cubes).
    private final ModelPart lWingD;
    private final ModelPart lWingE;
    private final ModelPart rWingD;
    private final ModelPart rWingE;
    private final ModelPart neckUFeather;
    private final ModelPart neckLFeather;
    // Five-segment darkness tail — shown for type 6 (replacing the normal Tail1/Tail2/Tail3).
    private final ModelPart tailpart1;
    private final ModelPart tailpart2;
    private final ModelPart tailpart3;
    private final ModelPart tailpart4;
    private final ModelPart tailpart5;
    // Saddle + neck harness (ported from the legacy model) — shown only when the ostrich is saddled.
    private final ModelPart saddleA;
    private final ModelPart saddleB;
    private final ModelPart saddleC;
    private final ModelPart saddleL;
    private final ModelPart saddleR;
    private final ModelPart saddleL2;
    private final ModelPart saddleR2;
    private final ModelPart neckHarness;
    private final ModelPart neckHarness2;
    // Helmet cubes (shown per state.ostrichHelmet 1-12).
    private final ModelPart helmetLeather;
    private final ModelPart helmetIron;
    private final ModelPart helmetGold;
    private final ModelPart helmetDiamond;
    private final ModelPart helmetHide;
    private final ModelPart helmetNeckHide;
    private final ModelPart helmetHideEar1;
    private final ModelPart helmetHideEar2;
    private final ModelPart helmetFur;
    private final ModelPart helmetNeckFur;
    private final ModelPart helmetFurEar1;
    private final ModelPart helmetFurEar2;
    private final ModelPart helmetReptile;
    private final ModelPart helmetReptileEar1;
    private final ModelPart helmetReptileEar2;
    private final ModelPart helmetGreenChitin;
    private final ModelPart helmetYellowChitin;
    private final ModelPart helmetBlueChitin;
    private final ModelPart helmetBlackChitin;
    private final ModelPart helmetRedChitin;
    // Chest (saddlebag + flagpole) + coloured flag cubes (shown when chested).
    private final ModelPart saddlebag;
    private final ModelPart flagpole;
    private final ModelPart flagOrange;
    private final ModelPart flagPurple;
    private final ModelPart flagLightBlue;
    private final ModelPart flagYellow;
    private final ModelPart flagGreen;
    private final ModelPart flagLightRed;
    private final ModelPart flagDarkGrey;
    private final ModelPart flagGrey;
    private final ModelPart flagCyan;
    private final ModelPart flagDarkPurple;
    private final ModelPart flagDarkBlue;
    private final ModelPart flagBrown;
    private final ModelPart flagDarkGreen;
    private final ModelPart flagRed;
    private final ModelPart flagBlack;
    private final ModelPart flagWhite;

    public MoCModelOstrich(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.uBeak = root.getChild("u_beak");
        this.uBeak2 = root.getChild("u_beak2");
        this.lBeak = root.getChild("l_beak");
        this.lBeak2 = root.getChild("l_beak2");
        this.neckU = root.getChild("neck_u");
        this.neckD = root.getChild("neck_d");
        this.neckL = root.getChild("neck_l");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail2");
        this.tail3 = root.getChild("tail3");
        this.lLegA = root.getChild("l_leg_a");
        this.lLegB = root.getChild("l_leg_b");
        this.lLegC = root.getChild("l_leg_c");
        this.lFoot = root.getChild("l_foot");
        this.rLegA = root.getChild("r_leg_a");
        this.rLegB = root.getChild("r_leg_b");
        this.rLegC = root.getChild("r_leg_c");
        this.rFoot = root.getChild("r_foot");
        this.lWingB = root.getChild("l_wing_b");
        this.lWingC = root.getChild("l_wing_c");
        this.rWingB = root.getChild("r_wing_b");
        this.rWingC = root.getChild("r_wing_c");
        this.uniHorn = root.getChild("uni_horn");
        this.lWingD = root.getChild("l_wing_d");
        this.lWingE = root.getChild("l_wing_e");
        this.rWingD = root.getChild("r_wing_d");
        this.rWingE = root.getChild("r_wing_e");
        this.neckUFeather = root.getChild("neck_u_feather");
        this.neckLFeather = root.getChild("neck_l_feather");
        this.tailpart1 = root.getChild("tailpart1");
        this.tailpart2 = root.getChild("tailpart2");
        this.tailpart3 = root.getChild("tailpart3");
        this.tailpart4 = root.getChild("tailpart4");
        this.tailpart5 = root.getChild("tailpart5");
        this.saddleA = root.getChild("saddle_a");
        this.saddleB = root.getChild("saddle_b");
        this.saddleC = root.getChild("saddle_c");
        this.saddleL = root.getChild("saddle_l");
        this.saddleR = root.getChild("saddle_r");
        this.saddleL2 = root.getChild("saddle_l2");
        this.saddleR2 = root.getChild("saddle_r2");
        this.neckHarness = root.getChild("neck_harness");
        this.neckHarness2 = root.getChild("neck_harness2");
        this.helmetLeather = root.getChild("helmet_leather");
        this.helmetIron = root.getChild("helmet_iron");
        this.helmetGold = root.getChild("helmet_gold");
        this.helmetDiamond = root.getChild("helmet_diamond");
        this.helmetHide = root.getChild("helmet_hide");
        this.helmetNeckHide = root.getChild("helmet_neck_hide");
        this.helmetHideEar1 = root.getChild("helmet_hide_ear1");
        this.helmetHideEar2 = root.getChild("helmet_hide_ear2");
        this.helmetFur = root.getChild("helmet_fur");
        this.helmetNeckFur = root.getChild("helmet_neck_fur");
        this.helmetFurEar1 = root.getChild("helmet_fur_ear1");
        this.helmetFurEar2 = root.getChild("helmet_fur_ear2");
        this.helmetReptile = root.getChild("helmet_reptile");
        this.helmetReptileEar1 = root.getChild("helmet_reptile_ear1");
        this.helmetReptileEar2 = root.getChild("helmet_reptile_ear2");
        this.helmetGreenChitin = root.getChild("helmet_green_chitin");
        this.helmetYellowChitin = root.getChild("helmet_yellow_chitin");
        this.helmetBlueChitin = root.getChild("helmet_blue_chitin");
        this.helmetBlackChitin = root.getChild("helmet_black_chitin");
        this.helmetRedChitin = root.getChild("helmet_red_chitin");
        this.saddlebag = root.getChild("saddlebag");
        this.flagpole = root.getChild("flagpole");
        this.flagOrange = root.getChild("flag_orange");
        this.flagPurple = root.getChild("flag_purple");
        this.flagLightBlue = root.getChild("flag_light_blue");
        this.flagYellow = root.getChild("flag_yellow");
        this.flagGreen = root.getChild("flag_green");
        this.flagLightRed = root.getChild("flag_light_red");
        this.flagDarkGrey = root.getChild("flag_dark_grey");
        this.flagGrey = root.getChild("flag_grey");
        this.flagCyan = root.getChild("flag_cyan");
        this.flagDarkPurple = root.getChild("flag_dark_purple");
        this.flagDarkBlue = root.getChild("flag_dark_blue");
        this.flagBrown = root.getChild("flag_brown");
        this.flagDarkGreen = root.getChild("flag_dark_green");
        this.flagRed = root.getChild("flag_red");
        this.flagBlack = root.getChild("flag_black");
        this.flagWhite = root.getChild("flag_white");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -16F, -4.5F, 3, 4, 3),
                PartPose.offset(0F, 3F, -6F));

        root.addOrReplaceChild("u_beak",
                CubeListBuilder.create().texOffs(12, 16).addBox(-1.5F, -15F, -5.5F, 3, 1, 1),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("u_beak2",
                CubeListBuilder.create().texOffs(20, 16).addBox(-1F, -15F, -7.5F, 2, 1, 2),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("l_beak",
                CubeListBuilder.create().texOffs(12, 22).addBox(-1.5F, -14F, -5.5F, 3, 1, 1),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("l_beak2",
                CubeListBuilder.create().texOffs(20, 22).addBox(-1F, -14F, -7.5F, 2, 1, 2),
                PartPose.offset(0F, 3F, -6F));

        root.addOrReplaceChild("neck_u",
                CubeListBuilder.create().texOffs(20, 0).addBox(-1F, -12F, -4F, 2, 5, 2),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("neck_d",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -4F, -2F, 3, 8, 3),
                PartPose.offsetAndRotation(0F, 3F, -6F, 0.4363323F, 0F, 0F));
        root.addOrReplaceChild("neck_l",
                CubeListBuilder.create().texOffs(20, 7).addBox(-1F, -8F, -2.5F, 2, 5, 2),
                PartPose.offsetAndRotation(0F, 3F, -6F, 0.2007129F, 0F, 0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 38).addBox(-4F, 1F, 0F, 8, 10, 16),
                PartPose.offset(0F, 0F, -6F));

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(30, 28).addBox(-2.5F, -1F, 0F, 5, 5, 5),
                PartPose.offset(0F, 4F, 10F));
        root.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(44, 18).addBox(-0.5F, -2F, -2F, 1, 4, 6),
                PartPose.offsetAndRotation(0F, 4F, 15F, 0.3490659F, 0F, 0F));
        root.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(58, 18).addBox(-2.6F, -2F, -2F, 1, 4, 6),
                PartPose.offsetAndRotation(0F, 4F, 15F, 0.3490659F, -0.2617994F, 0F));
        root.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(30, 18).addBox(1.6F, -2F, -2F, 1, 4, 6),
                PartPose.offsetAndRotation(0F, 4F, 15F, 0.3490659F, 0.2617994F, 0F));

        root.addOrReplaceChild("l_leg_a",
                CubeListBuilder.create().texOffs(50, 28).addBox(-2F, -1F, -2.5F, 4, 6, 5),
                PartPose.offsetAndRotation(4F, 5F, 4F, 0.1745329F, 0F, 0F));
        root.addOrReplaceChild("l_leg_b",
                CubeListBuilder.create().texOffs(50, 39).addBox(-1.5F, 5F, -1.5F, 3, 4, 3),
                PartPose.offsetAndRotation(4F, 5F, 4F, 0.1745329F, 0F, 0F));
        root.addOrReplaceChild("l_leg_c",
                CubeListBuilder.create().texOffs(8, 38).addBox(-1F, 8F, 2.5F, 2, 10, 2),
                PartPose.offsetAndRotation(4F, 5F, 4F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("l_foot",
                CubeListBuilder.create().texOffs(32, 42).addBox(-1F, 17F, -9F, 2, 1, 5),
                PartPose.offsetAndRotation(4F, 5F, 4F, 0.1745329F, 0F, 0F));

        root.addOrReplaceChild("r_leg_a",
                CubeListBuilder.create().texOffs(0, 27).addBox(-2F, -1F, -2.5F, 4, 6, 5),
                PartPose.offsetAndRotation(-4F, 5F, 4F, 0.1745329F, 0F, 0F));
        root.addOrReplaceChild("r_leg_b",
                CubeListBuilder.create().texOffs(18, 27).addBox(-1.5F, 5F, -1.5F, 3, 4, 3),
                PartPose.offsetAndRotation(-4F, 5F, 4F, 0.1745329F, 0F, 0F));
        root.addOrReplaceChild("r_leg_c",
                CubeListBuilder.create().texOffs(0, 38).addBox(-1F, 8F, 2.5F, 2, 10, 2),
                PartPose.offsetAndRotation(-4F, 5F, 4F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("r_foot",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1F, 17F, -9F, 2, 1, 5),
                PartPose.offsetAndRotation(-4F, 5F, 4F, 0.1745329F, 0F, 0F));

        root.addOrReplaceChild("l_wing_b",
                CubeListBuilder.create().texOffs(68, 46).addBox(-0.5F, -3F, 0F, 1, 4, 14),
                PartPose.offsetAndRotation(4F, 4F, -3F, 0.0872665F, 0.0872665F, 0F));
        root.addOrReplaceChild("l_wing_c",
                CubeListBuilder.create().texOffs(98, 46).addBox(-1F, 0F, 0F, 1, 4, 14),
                PartPose.offsetAndRotation(4F, 4F, -3F, 0F, 0.0872665F, 0F));
        root.addOrReplaceChild("r_wing_b",
                CubeListBuilder.create().texOffs(68, 0).addBox(-0.5F, -3F, 0F, 1, 4, 14),
                PartPose.offsetAndRotation(-4F, 4F, -3F, 0.0872665F, -0.0872665F, 0F));
        root.addOrReplaceChild("r_wing_c",
                CubeListBuilder.create().texOffs(98, 0).addBox(0F, 0F, 0F, 1, 4, 14),
                PartPose.offsetAndRotation(-4F, 4F, -3F, 0F, -0.0872665F, 0F));

        // ---- Unicorn horn (legacy UniHorn; shown only for type 8; gated visible in setupAnim) ----
        root.addOrReplaceChild("uni_horn",
                CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, -21F, 0.5F, 1, 6, 1),
                PartPose.offsetAndRotation(0F, 3F, -6F, 0.3171542F, 0F, 0F));

        // ---- Demon/darkness alternate wings + neck feathers (legacy LWingD/E, RWingD/E, NeckUFeather/NeckLFeather;
        //      shown for type 5 or 6 in place of the normal wing cubes). ----
        root.addOrReplaceChild("l_wing_d",
                CubeListBuilder.create().texOffs(26, 84).addBox(0F, -1F, -1F, 15, 2, 2),
                PartPose.offsetAndRotation(4F, 3F, -3F, 0F, 0F, -0.3490659F));
        root.addOrReplaceChild("l_wing_e",
                CubeListBuilder.create().texOffs(0, 103).addBox(0F, 0F, 1F, 15, 0, 15),
                PartPose.offsetAndRotation(4F, 3F, -3F, 0F, 0F, -0.3490659F));
        root.addOrReplaceChild("r_wing_d",
                CubeListBuilder.create().texOffs(26, 80).addBox(-15F, -1F, -1F, 15, 2, 2),
                PartPose.offsetAndRotation(-4F, 3F, -3F, 0F, 0F, 0.3490659F));
        root.addOrReplaceChild("r_wing_e",
                CubeListBuilder.create().texOffs(0, 88).addBox(-15F, 0F, 1F, 15, 0, 15),
                PartPose.offsetAndRotation(-4F, 3F, -3F, 0F, 0F, 0.3490659F));
        root.addOrReplaceChild("neck_u_feather",
                CubeListBuilder.create().texOffs(0, 73).addBox(0F, -16F, -2F, 0, 9, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("neck_l_feather",
                CubeListBuilder.create().texOffs(8, 73).addBox(0F, -8F, -0.5F, 0, 7, 4),
                PartPose.offsetAndRotation(0F, 3F, -6F, 0.2007129F, 0F, 0F));

        // ---- Five-segment darkness tail (legacy Tailpart1-5; shown for type 6 in place of Tail1/Tail2/Tail3). ----
        root.addOrReplaceChild("tailpart1",
                CubeListBuilder.create().texOffs(30, 28).addBox(-2.5F, -2.2F, 5F, 5, 5, 5),
                PartPose.offsetAndRotation(0F, 4F, 10F, -0.2974289F, 0F, 0F));
        root.addOrReplaceChild("tailpart2",
                CubeListBuilder.create().texOffs(60, 73).addBox(-2.5F, -4.3F, 9F, 5, 5, 8),
                PartPose.offsetAndRotation(0F, 4F, 10F, -0.5205006F, 0F, 0F));
        root.addOrReplaceChild("tailpart3",
                CubeListBuilder.create().texOffs(60, 86).addBox(-2F, 1F, 16F, 4, 4, 7),
                PartPose.offsetAndRotation(0F, 4F, 10F, -0.2230717F, 0F, 0F));
        root.addOrReplaceChild("tailpart4",
                CubeListBuilder.create().texOffs(60, 97).addBox(-1.5F, 8F, 20.6F, 3, 3, 7),
                PartPose.offsetAndRotation(0F, 4F, 10F, 0.0743572F, 0F, 0F));
        root.addOrReplaceChild("tailpart5",
                CubeListBuilder.create().texOffs(60, 107).addBox(-1F, 16.5F, 22.9F, 2, 2, 5),
                PartPose.offsetAndRotation(0F, 4F, 10F, 0.4089647F, 0F, 0F));

        // ---- Saddle + neck harness (ported from legacy MoCModelOstrich; gated visible on state.saddled) ----
        root.addOrReplaceChild("saddle_a",
                CubeListBuilder.create().texOffs(72, 18).addBox(-4F, 0.5F, -3F, 8, 1, 8),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("saddle_b",
                CubeListBuilder.create().texOffs(72, 27).addBox(-1.5F, 0F, -3F, 3, 1, 2),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("saddle_c",
                CubeListBuilder.create().texOffs(84, 27).addBox(-4F, 0F, 3F, 8, 1, 2),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("saddle_l",
                CubeListBuilder.create().texOffs(72, 30).addBox(-0.5F, 0F, -0.5F, 1, 6, 1),
                PartPose.offset(4F, 1F, 0F));
        root.addOrReplaceChild("saddle_r",
                CubeListBuilder.create().texOffs(84, 30).addBox(-0.5F, 0F, -0.5F, 1, 6, 1),
                PartPose.offset(-4F, 1F, 0F));
        root.addOrReplaceChild("saddle_l2",
                CubeListBuilder.create().texOffs(76, 30).addBox(-0.5F, 6F, -1F, 1, 2, 2),
                PartPose.offset(4F, 1F, 0F));
        root.addOrReplaceChild("saddle_r2",
                CubeListBuilder.create().texOffs(88, 30).addBox(-0.5F, 6F, -1F, 1, 2, 2),
                PartPose.offset(-4F, 1F, 0F));
        root.addOrReplaceChild("neck_harness",
                CubeListBuilder.create().texOffs(0, 11).addBox(-2F, -3F, -2.5F, 4, 1, 4),
                PartPose.offsetAndRotation(0F, 3F, -6F, 0.4363323F, 0F, 0F));
        root.addOrReplaceChild("neck_harness2",
                CubeListBuilder.create().texOffs(84, 55).addBox(-3F, -2.5F, -2F, 6, 1, 1),
                PartPose.offset(0F, 3F, -6F));

        // ---- Helmet cubes (ported from legacy; pivot at head offset 0,3,-6; gated on state.ostrichHelmet) ----
        root.addOrReplaceChild("helmet_leather",
                CubeListBuilder.create().texOffs(66, 0).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_iron",
                CubeListBuilder.create().texOffs(84, 46).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_gold",
                CubeListBuilder.create().texOffs(112, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_diamond",
                CubeListBuilder.create().texOffs(96, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_hide",
                CubeListBuilder.create().texOffs(96, 5).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_neck_hide",
                CubeListBuilder.create().texOffs(58, 0).addBox(-1.5F, -12F, -4.5F, 3, 1, 3),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_hide_ear1",
                CubeListBuilder.create().texOffs(84, 9).addBox(-2.5F, -18F, -3F, 2, 2, 1),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_hide_ear2",
                CubeListBuilder.create().texOffs(90, 9).addBox(0.5F, -18F, -3F, 2, 2, 1),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_fur",
                CubeListBuilder.create().texOffs(84, 0).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_neck_fur",
                CubeListBuilder.create().texOffs(96, 0).addBox(-1.5F, -12F, -4.5F, 3, 1, 3),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_fur_ear1",
                CubeListBuilder.create().texOffs(66, 9).addBox(-2.5F, -18F, -3F, 2, 2, 1),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_fur_ear2",
                CubeListBuilder.create().texOffs(76, 9).addBox(0.5F, -18F, -3F, 2, 2, 1),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_reptile",
                CubeListBuilder.create().texOffs(64, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_reptile_ear1",
                CubeListBuilder.create().texOffs(114, 50).addBox(-2.5F, -16.5F, -2F, 0, 5, 5),
                PartPose.offsetAndRotation(0F, 3F, -6F, 0F, -0.6108652F, 0F));
        root.addOrReplaceChild("helmet_reptile_ear2",
                CubeListBuilder.create().texOffs(114, 45).addBox(2.5F, -16.5F, -2F, 0, 5, 5),
                PartPose.offsetAndRotation(0F, 3F, -6F, 0F, 0.6108652F, 0F));
        root.addOrReplaceChild("helmet_green_chitin",
                CubeListBuilder.create().texOffs(80, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_yellow_chitin",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_blue_chitin",
                CubeListBuilder.create().texOffs(16, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_black_chitin",
                CubeListBuilder.create().texOffs(32, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));
        root.addOrReplaceChild("helmet_red_chitin",
                CubeListBuilder.create().texOffs(48, 64).addBox(-2F, -16.5F, -5F, 4, 5, 4),
                PartPose.offset(0F, 3F, -6F));

        // ---- Chest (saddlebag + flagpole) + coloured flag quads (gated on state.ostrichChested) ----
        root.addOrReplaceChild("saddlebag",
                CubeListBuilder.create().texOffs(32, 7).addBox(-4.5F, -3F, 5F, 9, 4, 7),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flagpole",
                CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, -15F, -0.5F, 1, 17, 1),
                PartPose.offsetAndRotation(0F, 0F, 5F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_orange",
                CubeListBuilder.create().texOffs(88, 24).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_purple",
                CubeListBuilder.create().texOffs(88, 32).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_light_blue",
                CubeListBuilder.create().texOffs(68, 32).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_yellow",
                CubeListBuilder.create().texOffs(48, 46).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_green",
                CubeListBuilder.create().texOffs(48, 38).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_light_red",
                CubeListBuilder.create().texOffs(108, 28).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_dark_grey",
                CubeListBuilder.create().texOffs(108, 12).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_grey",
                CubeListBuilder.create().texOffs(108, 16).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_cyan",
                CubeListBuilder.create().texOffs(48, 50).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_dark_purple",
                CubeListBuilder.create().texOffs(88, 28).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_dark_blue",
                CubeListBuilder.create().texOffs(68, 28).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_brown",
                CubeListBuilder.create().texOffs(48, 42).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_dark_green",
                CubeListBuilder.create().texOffs(108, 32).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_red",
                CubeListBuilder.create().texOffs(108, 24).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_black",
                CubeListBuilder.create().texOffs(108, 8).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("flag_white",
                CubeListBuilder.create().texOffs(108, 20).addBox(0F, -2.1F, 0F, 0, 4, 10),
                PartPose.offsetAndRotation(0F, -12F, 8F, -0.2602503F, 0F, 0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;
        float f3 = state.yRot;
        float f4 = state.xRot;

        float lLegXRot = Mth.cos(f * 0.4F) * 1.1F * f1;
        float rLegXRot = Mth.cos((f * 0.4F) + 3.141593F) * 1.1F * f1;

        this.head.y = 3.0F;
        this.head.xRot = (rLegXRot / 20F) + (-f4 * DEG_TO_RAD);
        this.head.yRot = f3 * DEG_TO_RAD;

        if (state.ostrichHiding) {
            // Head-in-the-sand hiding pose (legacy getHiding): the ostrich pitches its head and whole neck
            // sharply down so the beak drives into the ground. Head-tracking is overridden while hiding.
            this.head.xRot = 1.9F;
            this.head.yRot = 0.0F;
        }

        // beak + neck follow the head
        this.uBeak.xRot = this.head.xRot;
        this.uBeak.yRot = this.head.yRot;
        this.uBeak2.xRot = this.head.xRot;
        this.uBeak2.yRot = this.head.yRot;
        this.lBeak.xRot = this.head.xRot;
        this.lBeak.yRot = this.head.yRot;
        this.lBeak2.xRot = this.head.xRot;
        this.lBeak2.yRot = this.head.yRot;

        this.neckU.xRot = this.head.xRot;
        this.neckU.yRot = this.head.yRot;
        this.neckD.xRot = 0.4363323F + this.head.xRot;
        this.neckD.yRot = this.head.yRot;
        this.neckL.xRot = (11.5F / RADIAN_F) + this.head.xRot;
        this.neckL.yRot = this.head.yRot;

        // legs
        this.lLegA.xRot = 0.1745329F + lLegXRot;
        this.lLegB.xRot = this.lLegA.xRot;
        this.lLegC.xRot = -0.2617994F + lLegXRot;
        this.lFoot.xRot = this.lLegA.xRot;
        this.rLegA.xRot = 0.1745329F + rLegXRot;
        this.rLegB.xRot = this.rLegA.xRot;
        this.rLegC.xRot = -0.2617994F + rLegXRot;
        this.rFoot.xRot = this.rLegA.xRot;

        // wings
        float wingF = (10F / RADIAN_F) + Mth.cos(f * 0.6F) * 0.2F * f1;
        this.lWingB.yRot = 0.0872665F + wingF;
        this.lWingC.yRot = 0.0872665F + wingF;
        this.rWingB.yRot = -0.0872665F - wingF;
        this.rWingC.yRot = -0.0872665F - wingF;

        this.lWingB.xRot = 0.0872665F + (rLegXRot / 10F);
        this.lWingC.xRot = (rLegXRot / 10F);
        this.rWingB.xRot = 0.0872665F + (rLegXRot / 10F);
        this.rWingC.xRot = (rLegXRot / 10F);

        // Saddle + neck harness only render once a saddle has been equipped.
        boolean saddled = state.saddled;
        this.saddleA.visible = saddled;
        this.saddleB.visible = saddled;
        this.saddleC.visible = saddled;
        this.saddleL.visible = saddled;
        this.saddleR.visible = saddled;
        this.saddleL2.visible = saddled;
        this.saddleR2.visible = saddled;
        this.neckHarness.visible = saddled;
        this.neckHarness2.visible = saddled;

        // ---- Helmet / flag / chest (ported from legacy MoCModelOstrich) ----
        int helmet = state.ostrichHelmet;
        int flag = state.ostrichFlagColor;
        boolean chested = state.ostrichChested;

        // 1. Hide every new part by default.
        this.helmetLeather.visible = false;
        this.helmetIron.visible = false;
        this.helmetGold.visible = false;
        this.helmetDiamond.visible = false;
        this.helmetHide.visible = false;
        this.helmetNeckHide.visible = false;
        this.helmetHideEar1.visible = false;
        this.helmetHideEar2.visible = false;
        this.helmetFur.visible = false;
        this.helmetNeckFur.visible = false;
        this.helmetFurEar1.visible = false;
        this.helmetFurEar2.visible = false;
        this.helmetReptile.visible = false;
        this.helmetReptileEar1.visible = false;
        this.helmetReptileEar2.visible = false;
        this.helmetGreenChitin.visible = false;
        this.helmetYellowChitin.visible = false;
        this.helmetBlueChitin.visible = false;
        this.helmetBlackChitin.visible = false;
        this.helmetRedChitin.visible = false;
        this.saddlebag.visible = false;
        this.flagpole.visible = false;
        this.flagOrange.visible = false;
        this.flagPurple.visible = false;
        this.flagLightBlue.visible = false;
        this.flagYellow.visible = false;
        this.flagGreen.visible = false;
        this.flagLightRed.visible = false;
        this.flagDarkGrey.visible = false;
        this.flagGrey.visible = false;
        this.flagCyan.visible = false;
        this.flagDarkPurple.visible = false;
        this.flagDarkBlue.visible = false;
        this.flagBrown.visible = false;
        this.flagDarkGreen.visible = false;
        this.flagRed.visible = false;
        this.flagBlack.visible = false;
        this.flagWhite.visible = false;

        // 2. Every helmet part tracks the head each frame.
        this.helmetLeather.y = this.head.y;
        this.helmetLeather.xRot = this.head.xRot;
        this.helmetLeather.yRot = this.head.yRot;
        this.helmetIron.y = this.head.y;
        this.helmetIron.xRot = this.head.xRot;
        this.helmetIron.yRot = this.head.yRot;
        this.helmetGold.y = this.head.y;
        this.helmetGold.xRot = this.head.xRot;
        this.helmetGold.yRot = this.head.yRot;
        this.helmetDiamond.y = this.head.y;
        this.helmetDiamond.xRot = this.head.xRot;
        this.helmetDiamond.yRot = this.head.yRot;
        this.helmetHide.y = this.head.y;
        this.helmetHide.xRot = this.head.xRot;
        this.helmetHide.yRot = this.head.yRot;
        this.helmetNeckHide.y = this.head.y;
        this.helmetNeckHide.xRot = this.head.xRot;
        this.helmetNeckHide.yRot = this.head.yRot;
        this.helmetHideEar1.y = this.head.y;
        this.helmetHideEar1.xRot = this.head.xRot;
        this.helmetHideEar1.yRot = this.head.yRot;
        this.helmetHideEar2.y = this.head.y;
        this.helmetHideEar2.xRot = this.head.xRot;
        this.helmetHideEar2.yRot = this.head.yRot;
        this.helmetFur.y = this.head.y;
        this.helmetFur.xRot = this.head.xRot;
        this.helmetFur.yRot = this.head.yRot;
        this.helmetNeckFur.y = this.head.y;
        this.helmetNeckFur.xRot = this.head.xRot;
        this.helmetNeckFur.yRot = this.head.yRot;
        this.helmetFurEar1.y = this.head.y;
        this.helmetFurEar1.xRot = this.head.xRot;
        this.helmetFurEar1.yRot = this.head.yRot;
        this.helmetFurEar2.y = this.head.y;
        this.helmetFurEar2.xRot = this.head.xRot;
        this.helmetFurEar2.yRot = this.head.yRot;
        this.helmetReptile.y = this.head.y;
        this.helmetReptile.xRot = this.head.xRot;
        this.helmetReptile.yRot = this.head.yRot;
        this.helmetReptileEar1.y = this.head.y;
        this.helmetReptileEar1.xRot = this.head.xRot;
        this.helmetReptileEar1.yRot = (-35F / RADIAN_F) + this.head.yRot;
        this.helmetReptileEar2.y = this.head.y;
        this.helmetReptileEar2.xRot = this.head.xRot;
        this.helmetReptileEar2.yRot = (35F / RADIAN_F) + this.head.yRot;
        this.helmetGreenChitin.y = this.head.y;
        this.helmetGreenChitin.xRot = this.head.xRot;
        this.helmetGreenChitin.yRot = this.head.yRot;
        this.helmetYellowChitin.y = this.head.y;
        this.helmetYellowChitin.xRot = this.head.xRot;
        this.helmetYellowChitin.yRot = this.head.yRot;
        this.helmetBlueChitin.y = this.head.y;
        this.helmetBlueChitin.xRot = this.head.xRot;
        this.helmetBlueChitin.yRot = this.head.yRot;
        this.helmetBlackChitin.y = this.head.y;
        this.helmetBlackChitin.xRot = this.head.xRot;
        this.helmetBlackChitin.yRot = this.head.yRot;
        this.helmetRedChitin.y = this.head.y;
        this.helmetRedChitin.xRot = this.head.xRot;
        this.helmetRedChitin.yRot = this.head.yRot;

        // 3. Reveal the matching helmet cube(s).
        switch (helmet) {
            case 1:
                this.helmetLeather.visible = true;
                break;
            case 2:
                this.helmetIron.visible = true;
                break;
            case 3:
                this.helmetGold.visible = true;
                break;
            case 4:
                this.helmetDiamond.visible = true;
                break;
            case 5:
                this.helmetHide.visible = true;
                this.helmetNeckHide.visible = true;
                this.helmetHideEar1.visible = true;
                this.helmetHideEar2.visible = true;
                break;
            case 6:
                this.helmetFur.visible = true;
                this.helmetNeckFur.visible = true;
                this.helmetFurEar1.visible = true;
                this.helmetFurEar2.visible = true;
                break;
            case 7:
                this.helmetReptile.visible = true;
                this.helmetReptileEar1.visible = true;
                this.helmetReptileEar2.visible = true;
                break;
            case 8:
                this.helmetGreenChitin.visible = true;
                break;
            case 9:
                this.helmetYellowChitin.visible = true;
                break;
            case 10:
                this.helmetBlueChitin.visible = true;
                break;
            case 11:
                this.helmetBlackChitin.visible = true;
                break;
            case 12:
                this.helmetRedChitin.visible = true;
                break;
            default:
                break;
        }

        // 4. Chest (saddlebag + flagpole) and coloured flag.
        this.saddlebag.visible = chested;
        this.flagpole.visible = chested;
        if (chested) {
            float flagF = Mth.cos(state.walkAnimationPos * 0.8F) * 0.1F * state.walkAnimationSpeed;
            switch (flag) {
                case 1:
                    this.flagOrange.visible = true;
                    this.flagOrange.yRot = flagF;
                    break;
                case 2:
                    this.flagPurple.visible = true;
                    this.flagPurple.yRot = flagF;
                    break;
                case 3:
                    this.flagLightBlue.visible = true;
                    this.flagLightBlue.yRot = flagF;
                    break;
                case 4:
                    this.flagYellow.visible = true;
                    this.flagYellow.yRot = flagF;
                    break;
                case 5:
                    this.flagGreen.visible = true;
                    this.flagGreen.yRot = flagF;
                    break;
                case 6:
                    this.flagLightRed.visible = true;
                    this.flagLightRed.yRot = flagF;
                    break;
                case 7:
                    this.flagDarkGrey.visible = true;
                    this.flagDarkGrey.yRot = flagF;
                    break;
                case 8:
                    this.flagGrey.visible = true;
                    this.flagGrey.yRot = flagF;
                    break;
                case 9:
                    this.flagCyan.visible = true;
                    this.flagCyan.yRot = flagF;
                    break;
                case 10:
                    this.flagDarkPurple.visible = true;
                    this.flagDarkPurple.yRot = flagF;
                    break;
                case 11:
                    this.flagDarkBlue.visible = true;
                    this.flagDarkBlue.yRot = flagF;
                    break;
                case 12:
                    this.flagBrown.visible = true;
                    this.flagBrown.yRot = flagF;
                    break;
                case 13:
                    this.flagDarkGreen.visible = true;
                    this.flagDarkGreen.yRot = flagF;
                    break;
                case 14:
                    this.flagRed.visible = true;
                    this.flagRed.yRot = flagF;
                    break;
                case 15:
                    this.flagBlack.visible = true;
                    this.flagBlack.yRot = flagF;
                    break;
                case 16:
                    this.flagWhite.visible = true;
                    this.flagWhite.yRot = flagF;
                    break;
                default:
                    break;
            }
        }

        // ---- Type-specific geometry (ported from legacy MoCModelOstrich). ----
        int type = state.typeMoC;
        boolean demonWings = (type == 5 || type == 6); // demon (fire/nightmare) + darkness ostriches
        boolean darknessTail = (type == 6);

        // Unicorn horn (type 8): tracks the head (legacy added an 18-degree pitch on top of the head pitch).
        this.uniHorn.visible = (type == 8);
        this.uniHorn.y = this.head.y;
        this.uniHorn.xRot = (18F / RADIAN_F) + this.head.xRot;
        this.uniHorn.yRot = this.head.yRot;

        // Normal wings render only for the non-demon types; demon/darkness types swap to the feathered wings.
        this.lWingB.visible = !demonWings;
        this.lWingC.visible = !demonWings;
        this.rWingB.visible = !demonWings;
        this.rWingC.visible = !demonWings;
        this.lWingD.visible = demonWings;
        this.lWingE.visible = demonWings;
        this.rWingD.visible = demonWings;
        this.rWingE.visible = demonWings;
        this.neckUFeather.visible = demonWings;
        this.neckLFeather.visible = demonWings;

        // Neck feathers follow the head (legacy: NeckUFeather tracks head, NeckLFeather adds an 11.5-degree pitch).
        this.neckUFeather.y = this.head.y;
        this.neckUFeather.xRot = this.head.xRot;
        this.neckUFeather.yRot = this.head.yRot;
        this.neckLFeather.y = this.head.y;
        this.neckLFeather.xRot = (11.5F / RADIAN_F) + this.head.xRot;
        this.neckLFeather.yRot = this.head.yRot;

        if (demonWings) {
            // Legacy demon/darkness wing flap (walking case): a folding motion driven by the walk cycle.
            float demonWingF = Mth.cos(f * 0.3F) * f1;
            this.lWingD.zRot = (-20F / RADIAN_F) - demonWingF;
            this.lWingE.zRot = (-20F / RADIAN_F) - demonWingF;
            this.rWingD.zRot = (20F / RADIAN_F) + demonWingF;
            this.rWingE.zRot = (20F / RADIAN_F) + demonWingF;
        }

        // Normal three-quill tail renders for every type except the darkness ostrich (type 6).
        this.tail1.visible = !darknessTail;
        this.tail2.visible = !darknessTail;
        this.tail3.visible = !darknessTail;
        this.tailpart1.visible = darknessTail;
        this.tailpart2.visible = darknessTail;
        this.tailpart3.visible = darknessTail;
        this.tailpart4.visible = darknessTail;
        this.tailpart5.visible = darknessTail;

        if (darknessTail) {
            // Legacy cascading yRot sway: each successive segment gets a slightly larger sway than the last.
            // Faithful to legacy, including the double-assignment of tailpart1 (it lands on the second increment).
            float segStep = 15F;
            float rotF = Mth.cos(f * 0.5F) * 0.3F * f1;
            this.tail.yRot = rotF;
            rotF += (rotF / segStep);
            this.tailpart1.yRot = rotF;
            rotF += (rotF / segStep);
            this.tailpart1.yRot = rotF;
            rotF += (rotF / segStep);
            this.tailpart2.yRot = rotF;
            rotF += (rotF / segStep);
            this.tailpart3.yRot = rotF;
            rotF += (rotF / segStep);
            this.tailpart4.yRot = rotF;
            rotF += (rotF / segStep);
            this.tailpart5.yRot = rotF;
        } else {
            this.tail.yRot = 0.0F;
        }
    }
}
