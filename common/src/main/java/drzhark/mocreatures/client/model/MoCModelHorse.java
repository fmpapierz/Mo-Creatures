package drzhark.mocreatures.client.model;

import java.util.Set;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Horse model, converted faithfully from the legacy {@code MoCModelNewHorse} ({@code ModelBase}).
 * Every cube, texture offset and rotation point is preserved. The legacy model carried a great deal
 * of state-driven animation (flying, standing, eating, saddles, butterfly/pegasus wings); the
 * modern render state only exposes basic walk and look data, so {@link #setupAnim} reproduces the
 * leg gait and head tracking and leaves the special-case poses at their static rest values.
 *
 * <p><b>Equipment.</b> In MoCreatures the horse's protective armour is a full-texture swap (the
 * armoured look lives entirely in the {@code horsearmor*} texture variants), not extra geometry &mdash;
 * the legacy model contains <em>no</em> armour cubes, so there is nothing here to gate on
 * {@code state.horseArmor}; the texture swap handles that on its own. The only model-level equipment
 * is the saddle group (saddle pad, girth straps, stirrups, bit and head-stall), which the legacy
 * {@code render()} drew only when the horse was saddled. {@link #setupAnim} reproduces that by
 * toggling the saddle parts' {@code visible} from {@code state.saddled}.
 *
 * <p><b>Wings.</b> The mesh holds two mutually-exclusive wing sets that share texture space — the
 * feathered pegasus wing (inner/mid/outer per side) and the flat fairy membranes (butterfly_l/_r).
 * Legacy never drew both on one horse, and neither does {@link #setupAnim}; see the wing block there
 * and {@link #animatePegasusWings} / {@link #animateButterflyWings} for the gating and the beat.
 * The fairy membranes are the one place where the port's geometry departs from legacy's cube list:
 * legacy's single zero-height box relied on 1.12's back-face culling to keep its two coincident faces
 * apart, and 26.2's default entity pipeline has culling switched off, so each membrane is built as two
 * single-face boxes a quarter-unit apart instead — see {@link #MEMBRANE_UPPER_FACE} and
 * {@link #BUTTERFLY_PIVOT_Y}.
 */
public class MoCModelHorse extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /**
     * Face selectors for the fairy membranes. A fairy wing is a zero-height box, and
     * {@link ModelPart.Cube} builds <em>all six</em> faces of every box (ModelPart.java:299-323): with
     * {@code height == 0} the DOWN quad (built at {@code minY}) and the UP quad (built at {@code maxY})
     * land on the same plane, and the four side quads collapse to zero area. Legacy got away with that
     * because 1.12 rendered entities with GL back-face culling on, so exactly one of the twins survived
     * per viewing side. 26.2 does not: {@code EntityModel(ModelPart)} defaults to
     * {@code RenderTypes::entityCutout}, whose pipeline is declared {@code .withCull(false)}
     * (RenderPipelines.java:248-256). Both twins therefore rasterise, at bit-identical depth — the
     * reported strobe. Splitting the membrane into two single-face boxes lets each painted face be
     * placed on its own plane; see {@link #createBodyLayer}.
     */
    // Model space is Y-down (the renderer flips it), so the box's DOWN quad is the surface you see from
    // above the horse and its UP quad is the underside. The names below are in *world* terms.
    private static final Set<Direction> MEMBRANE_UPPER_FACE = Set.of(Direction.DOWN);
    private static final Set<Direction> MEMBRANE_LOWER_FACE = Set.of(Direction.UP);

    /**
     * Half the gap between a fairy membrane's two painted faces, in model units (0.25 total, i.e. a
     * quarter of one texture pixel &mdash; 1/64 of a block). Large enough that the depth buffer separates
     * them at any sane view distance, far too small to read as thickness on a 26x30 wing.
     */
    private static final float MEMBRANE_HALF_GAP = 0.125F;

    /**
     * Y of the fairy wing pivot. Legacy used {@code 3F} (legacy MoCModelNewHorse.java:344/349, restated
     * every frame at :850-855) &mdash; which is <em>exactly</em> the body's top face: the body box is
     * {@code addBox(-5,-8,-19, 10,10,24)} at {@code offset(0,11,9)}, so its top plane is
     * {@code 11 - 8 = 3}, and the saddle pad's underside is that same y=3. Both butterfly boxes start one
     * unit <em>inboard</em> of their own pivot ({@code minX = -1} / {@code maxX = +1}), so that overhang
     * sits inside the body's x-footprint; whenever the flap angle carried the membrane back through
     * horizontal it became exactly coplanar with the horse's back over a 1.5 x 24 strip. Lifting the
     * pivot half a unit clear (smaller y is higher: the renderer flips the model) means a horizontal
     * membrane rests at y=2.5 and can never be coplanar with either y=3 face; at every other angle it
     * crosses the back transversally, which is a clean intersection rather than a fight.
     */
    private static final float BUTTERFLY_PIVOT_Y = 2.5F;

    /**
     * Ceiling on how far a fairy membrane may fold, in radians (85&deg;). See
     * {@link #animateButterflyWings} for why legacy's raw range let the two wings meet at the mid-line.
     */
    private static final float BUTTERFLY_MAX_FOLD = 85.0F * DEG_TO_RAD;

    private final ModelPart head;
    private final ModelPart uMouth;
    private final ModelPart lMouth;
    private final ModelPart uMouth2;
    private final ModelPart lMouth2;
    private final ModelPart unicorn;
    private final ModelPart ear1;
    private final ModelPart ear2;
    private final ModelPart muleEarL;
    private final ModelPart muleEarR;
    private final ModelPart neck;
    private final ModelPart headSaddle;
    private final ModelPart mane;

    private final ModelPart body;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart tailC;

    private final ModelPart leg1A;
    private final ModelPart leg1B;
    private final ModelPart leg1C;
    private final ModelPart leg2A;
    private final ModelPart leg2B;
    private final ModelPart leg2C;
    private final ModelPart leg3A;
    private final ModelPart leg3B;
    private final ModelPart leg3C;
    private final ModelPart leg4A;
    private final ModelPart leg4B;
    private final ModelPart leg4C;

    private final ModelPart bag1;
    private final ModelPart bag2;

    private final ModelPart saddle;
    private final ModelPart saddleB;
    private final ModelPart saddleC;
    private final ModelPart saddleL;
    private final ModelPart saddleL2;
    private final ModelPart saddleR;
    private final ModelPart saddleR2;
    private final ModelPart saddleMouthL;
    private final ModelPart saddleMouthR;
    private final ModelPart saddleMouthLine;
    private final ModelPart saddleMouthLineR;

    private final ModelPart midWing;
    private final ModelPart innerWing;
    private final ModelPart outerWing;
    private final ModelPart innerWingR;
    private final ModelPart midWingR;
    private final ModelPart outerWingR;

    private final ModelPart butterflyL;
    private final ModelPart butterflyR;

    public MoCModelHorse(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.uMouth = root.getChild("u_mouth");
        this.lMouth = root.getChild("l_mouth");
        this.uMouth2 = root.getChild("u_mouth2");
        this.lMouth2 = root.getChild("l_mouth2");
        this.unicorn = root.getChild("unicorn");
        this.ear1 = root.getChild("ear1");
        this.ear2 = root.getChild("ear2");
        this.muleEarL = root.getChild("mule_ear_l");
        this.muleEarR = root.getChild("mule_ear_r");
        this.neck = root.getChild("neck");
        this.headSaddle = root.getChild("head_saddle");
        this.mane = root.getChild("mane");

        this.body = root.getChild("body");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_b");
        this.tailC = root.getChild("tail_c");

        this.leg1A = root.getChild("leg1a");
        this.leg1B = root.getChild("leg1b");
        this.leg1C = root.getChild("leg1c");
        this.leg2A = root.getChild("leg2a");
        this.leg2B = root.getChild("leg2b");
        this.leg2C = root.getChild("leg2c");
        this.leg3A = root.getChild("leg3a");
        this.leg3B = root.getChild("leg3b");
        this.leg3C = root.getChild("leg3c");
        this.leg4A = root.getChild("leg4a");
        this.leg4B = root.getChild("leg4b");
        this.leg4C = root.getChild("leg4c");

        this.bag1 = root.getChild("bag1");
        this.bag2 = root.getChild("bag2");

        this.saddle = root.getChild("saddle");
        this.saddleB = root.getChild("saddle_b");
        this.saddleC = root.getChild("saddle_c");
        this.saddleL = root.getChild("saddle_l");
        this.saddleL2 = root.getChild("saddle_l2");
        this.saddleR = root.getChild("saddle_r");
        this.saddleR2 = root.getChild("saddle_r2");
        this.saddleMouthL = root.getChild("saddle_mouth_l");
        this.saddleMouthR = root.getChild("saddle_mouth_r");
        this.saddleMouthLine = root.getChild("saddle_mouth_line");
        this.saddleMouthLineR = root.getChild("saddle_mouth_line_r");

        this.midWing = root.getChild("mid_wing");
        this.innerWing = root.getChild("inner_wing");
        this.outerWing = root.getChild("outer_wing");
        this.innerWingR = root.getChild("inner_wing_r");
        this.midWingR = root.getChild("mid_wing_r");
        this.outerWingR = root.getChild("outer_wing_r");

        this.butterflyL = root.getChild("butterfly_l");
        this.butterflyR = root.getChild("butterfly_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 34).addBox(-5.0F, -8.0F, -19.0F, 10.0F, 10.0F, 24.0F),
                PartPose.offset(0.0F, 11.0F, 9.0F));

        root.addOrReplaceChild("tail_a",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 14.0F, -1.134464F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_b",
                CubeListBuilder.create().texOffs(38, 7).addBox(-1.5F, -2.0F, 3.0F, 3.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 14.0F, -1.134464F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_c",
                CubeListBuilder.create().texOffs(24, 3).addBox(-1.5F, -4.5F, 9.0F, 3.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 14.0F, -1.40215F, 0.0F, 0.0F));

        root.addOrReplaceChild("leg1a",
                CubeListBuilder.create().texOffs(78, 29).addBox(-2.5F, -2.0F, -2.5F, 4.0F, 9.0F, 5.0F),
                PartPose.offset(4.0F, 9.0F, 11.0F));
        root.addOrReplaceChild("leg1b",
                CubeListBuilder.create().texOffs(78, 43).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(4.0F, 16.0F, 11.0F));
        root.addOrReplaceChild("leg1c",
                CubeListBuilder.create().texOffs(78, 51).addBox(-2.5F, 5.1F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(4.0F, 16.0F, 11.0F));

        root.addOrReplaceChild("leg2a",
                CubeListBuilder.create().texOffs(96, 29).addBox(-1.5F, -2.0F, -2.5F, 4.0F, 9.0F, 5.0F),
                PartPose.offset(-4.0F, 9.0F, 11.0F));
        root.addOrReplaceChild("leg2b",
                CubeListBuilder.create().texOffs(96, 43).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(-4.0F, 16.0F, 11.0F));
        root.addOrReplaceChild("leg2c",
                CubeListBuilder.create().texOffs(96, 51).addBox(-1.5F, 5.1F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(-4.0F, 16.0F, 11.0F));

        root.addOrReplaceChild("leg3a",
                CubeListBuilder.create().texOffs(44, 29).addBox(-1.9F, -1.0F, -2.1F, 3.0F, 8.0F, 4.0F),
                PartPose.offset(4.0F, 9.0F, -8.0F));
        root.addOrReplaceChild("leg3b",
                CubeListBuilder.create().texOffs(44, 41).addBox(-1.9F, 0.0F, -1.6F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(4.0F, 16.0F, -8.0F));
        root.addOrReplaceChild("leg3c",
                CubeListBuilder.create().texOffs(44, 51).addBox(-2.4F, 5.1F, -2.1F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(4.0F, 16.0F, -8.0F));

        root.addOrReplaceChild("leg4a",
                CubeListBuilder.create().texOffs(60, 29).addBox(-1.1F, -1.0F, -2.1F, 3.0F, 8.0F, 4.0F),
                PartPose.offset(-4.0F, 9.0F, -8.0F));
        root.addOrReplaceChild("leg4b",
                CubeListBuilder.create().texOffs(60, 41).addBox(-1.1F, 0.0F, -1.6F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(-4.0F, 16.0F, -8.0F));
        root.addOrReplaceChild("leg4c",
                CubeListBuilder.create().texOffs(60, 51).addBox(-1.6F, 5.1F, -2.1F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(-4.0F, 16.0F, -8.0F));

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -10.0F, -1.5F, 5.0F, 5.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("u_mouth",
                CubeListBuilder.create().texOffs(24, 18).addBox(-2.0F, -10.0F, -7.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("l_mouth",
                CubeListBuilder.create().texOffs(24, 27).addBox(-2.0F, -7.0F, -6.5F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("u_mouth2",
                CubeListBuilder.create().texOffs(24, 18).addBox(-2.0F, -10.0F, -8.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("l_mouth2",
                CubeListBuilder.create().texOffs(24, 27).addBox(-2.0F, -7.0F, -5.5F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.7853982F, 0.0F, 0.0F));

        root.addOrReplaceChild("unicorn",
                CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -18.0F, 2.0F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("ear1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.45F, -12.0F, 4.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("ear2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.45F, -12.0F, 4.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("mule_ear_l",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -16.0F, 4.0F, 2.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.2617994F));
        root.addOrReplaceChild("mule_ear_r",
                CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -16.0F, 4.0F, 2.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, -0.2617994F));

        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.05F, -9.8F, -2.0F, 4.0F, 14.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("bag1",
                CubeListBuilder.create().texOffs(0, 34).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(-7.5F, 3.0F, 10.0F, 0.0F, 1.570796F, 0.0F));
        root.addOrReplaceChild("bag2",
                CubeListBuilder.create().texOffs(0, 47).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(4.5F, 3.0F, 10.0F, 0.0F, 1.570796F, 0.0F));

        root.addOrReplaceChild("saddle",
                CubeListBuilder.create().texOffs(80, 0).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 2.0F, 2.0F));
        root.addOrReplaceChild("saddle_b",
                CubeListBuilder.create().texOffs(106, 9).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 2.0F, 2.0F));
        root.addOrReplaceChild("saddle_c",
                CubeListBuilder.create().texOffs(80, 9).addBox(-4.0F, -1.0F, 3.0F, 8.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 2.0F, 2.0F));

        root.addOrReplaceChild("saddle_l2",
                CubeListBuilder.create().texOffs(74, 0).addBox(-0.5F, 6.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(5.0F, 3.0F, 2.0F));
        root.addOrReplaceChild("saddle_l",
                CubeListBuilder.create().texOffs(70, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(5.0F, 3.0F, 2.0F));
        root.addOrReplaceChild("saddle_r2",
                CubeListBuilder.create().texOffs(74, 4).addBox(-0.5F, 6.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(-5.0F, 3.0F, 2.0F));
        root.addOrReplaceChild("saddle_r",
                CubeListBuilder.create().texOffs(80, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(-5.0F, 3.0F, 2.0F));

        root.addOrReplaceChild("saddle_mouth_l",
                CubeListBuilder.create().texOffs(74, 13).addBox(1.5F, -8.0F, -4.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("saddle_mouth_r",
                CubeListBuilder.create().texOffs(74, 13).addBox(-2.5F, -8.0F, -4.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("saddle_mouth_line",
                CubeListBuilder.create().texOffs(44, 10).addBox(2.6F, -6.0F, -6.0F, 0.0F, 3.0F, 16.0F),
                PartPose.offset(0.0F, 4.0F, -10.0F));
        root.addOrReplaceChild("saddle_mouth_line_r",
                CubeListBuilder.create().texOffs(44, 5).addBox(-2.6F, -6.0F, -6.0F, 0.0F, 3.0F, 16.0F),
                PartPose.offset(0.0F, 4.0F, -10.0F));

        root.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(58, 0).addBox(-1.0F, -11.5F, 5.0F, 2.0F, 16.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("head_saddle",
                CubeListBuilder.create().texOffs(80, 12).addBox(-2.5F, -10.1F, -7.0F, 5.0F, 5.0F, 12.0F, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("mid_wing",
                CubeListBuilder.create().texOffs(82, 68).addBox(1.0F, 0.1F, 1.0F, 12.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(5.0F, 3.0F, -6.0F, 0.0F, 0.0872665F, 0.0F));
        root.addOrReplaceChild("inner_wing",
                CubeListBuilder.create().texOffs(0, 96).addBox(0.0F, 0.0F, 0.0F, 7.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(5.0F, 3.0F, -6.0F, 0.0F, -0.3490659F, 0.0F));
        root.addOrReplaceChild("outer_wing",
                CubeListBuilder.create().texOffs(0, 68).addBox(0.0F, 0.0F, 0.0F, 22.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(17.0F, 3.0F, -6.0F, 0.0F, -0.3228859F, 0.0F));
        root.addOrReplaceChild("inner_wing_r",
                CubeListBuilder.create().texOffs(0, 110).addBox(-7.0F, 0.0F, 0.0F, 7.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(-5.0F, 3.0F, -6.0F, 0.0F, 0.3490659F, 0.0F));
        root.addOrReplaceChild("mid_wing_r",
                CubeListBuilder.create().texOffs(82, 82).addBox(-13.0F, 0.1F, 1.0F, 12.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(-5.0F, 3.0F, -6.0F, 0.0F, -0.0872665F, 0.0F));
        root.addOrReplaceChild("outer_wing_r",
                CubeListBuilder.create().texOffs(0, 82).addBox(-22.0F, 0.0F, 0.0F, 22.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(-17.0F, 3.0F, -6.0F, 0.0F, 0.3228859F, 0.0F));

        // Fairy membranes. Legacy declared each wing as ONE zero-height box —
        //   ButterflyL: new ModelRenderer(this, 0, 98); addBox(-1F, 0F, -14F, 26, 0, 30);   (legacy
        //               MoCModelNewHorse.java:342-345)
        //   ButterflyR: new ModelRenderer(this, 0, 68); addBox(-25F, 0F, -14F, 26, 0, 30);  (:347-350)
        // — and every cube, texture offset and UV tile below is byte-for-byte that box. What changed is
        // that the single box is now expressed as two SINGLE-FACE boxes straddling the legacy plane by
        // +/-MEMBRANE_HALF_GAP, because 26.2 renders entities un-culled and would otherwise draw the two
        // coincident faces on top of each other (see MEMBRANE_UPPER_FACE).
        //
        // The UV tiles are unchanged by the split: ModelPart.Cube derives them from texOffs + w/h/d only
        // (ModelPart.java:289-304), never from the box origin, so the DOWN quad still samples
        // u [texU+30, texU+56] and the UP quad u [texU+56, texU+82] exactly as before. Those two tiles do
        // NOT hold the same picture — on every horsefairy*.png they hold two different butterfly wings
        // (the DOWN tile's wing is painted into the front half of the 30-deep plane, the UP tile's into
        // the rear half), which is why the fight read as "wings clipping through themselves": two unrelated
        // wing silhouettes were tearing through one another in the same pixels. Split apart they simply
        // render as the top and underside of the membrane, which is what the sheet was drawn for.
        //
        // Dropping the four side faces is free: with height 0 they are zero-area quads that rasterise
        // nothing, and their UV strips ran off the 30-deep tile along v=128, the last row of the sheet.
        root.addOrReplaceChild("butterfly_l",
                CubeListBuilder.create().texOffs(0, 98)
                        .addBox(-1.0F, -MEMBRANE_HALF_GAP, -14.0F, 26.0F, 0.0F, 30.0F, MEMBRANE_UPPER_FACE)
                        .addBox(-1.0F, MEMBRANE_HALF_GAP, -14.0F, 26.0F, 0.0F, 30.0F, MEMBRANE_LOWER_FACE),
                PartPose.offsetAndRotation(4.5F, BUTTERFLY_PIVOT_Y, -2.0F, 0.0F, 0.0F, -0.78539F));
        root.addOrReplaceChild("butterfly_r",
                CubeListBuilder.create().texOffs(0, 68)
                        .addBox(-25.0F, -MEMBRANE_HALF_GAP, -14.0F, 26.0F, 0.0F, 30.0F, MEMBRANE_UPPER_FACE)
                        .addBox(-25.0F, MEMBRANE_HALF_GAP, -14.0F, 26.0F, 0.0F, 30.0F, MEMBRANE_LOWER_FACE),
                PartPose.offsetAndRotation(-4.5F, BUTTERFLY_PIVOT_Y, -2.0F, 0.0F, 0.0F, 0.78539F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float headYaw = state.yRot;
        float headPitch = state.xRot;

        if (headYaw > 20.0F) {
            headYaw = 20.0F;
        }
        if (headYaw < -20.0F) {
            headYaw = -20.0F;
        }

        // legs: alternating cosine gait (faithful to legacy RLegXRot / LLegXRot)
        float rLegXRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;

        this.leg1A.xRot = lLegXRot;
        this.leg1B.xRot = lLegXRot;
        this.leg1C.xRot = lLegXRot;
        this.leg2A.xRot = rLegXRot;
        this.leg2B.xRot = rLegXRot;
        this.leg2C.xRot = rLegXRot;
        this.leg3A.xRot = rLegXRot;
        this.leg3B.xRot = rLegXRot;
        this.leg3C.xRot = rLegXRot;
        this.leg4A.xRot = lLegXRot;
        this.leg4B.xRot = lLegXRot;
        this.leg4C.xRot = lLegXRot;

        // Rearing: the front legs (leg3/leg4, at the front of the body) tuck up and forward as the horse
        // stands on its hind legs; the whole body is pitched back by the renderer (see MoCMobRenderer).
        if (state.horseRearing) {
            this.leg3A.xRot = -1.2F;
            this.leg3B.xRot = -1.4F;
            this.leg3C.xRot = -1.4F;
            this.leg4A.xRot = -1.2F;
            this.leg4B.xRot = -1.4F;
            this.leg4C.xRot = -1.4F;
        }

        // head + attached parts track look direction (legacy base rotation = 30 degrees pitch); a grazing
        // horse drives its nose to the ground, a rearing horse throws its head up (legacy eating/standing).
        float headXRot;
        float headYRot = headYaw * DEG_TO_RAD;
        if (state.horseEating) {
            headXRot = 2.0F;   // nose lowered to graze
            headYRot = 0.0F;
        } else if (state.horseRearing) {
            headXRot = -0.35F; // head flung up as it rears
        } else {
            headXRot = 0.5235988F + (headPitch * DEG_TO_RAD);
            // Idle shuffle: a standing horse gently bobs its head (legacy shuffling head-bob).
            if (limbAmount < 0.05F) {
                headXRot += Mth.cos(state.ageInTicks * 0.09F) * 0.05F;
            }
        }

        this.head.xRot = headXRot;
        this.head.yRot = headYRot;
        this.ear1.xRot = headXRot;
        this.ear1.yRot = headYRot;
        this.ear2.xRot = headXRot;
        this.ear2.yRot = headYRot;
        this.muleEarL.xRot = headXRot;
        this.muleEarL.yRot = headYRot;
        this.muleEarR.xRot = headXRot;
        this.muleEarR.yRot = headYRot;
        this.neck.xRot = headXRot;
        this.neck.yRot = headYRot;
        this.mane.xRot = headXRot;
        this.mane.yRot = headYRot;
        this.unicorn.xRot = headXRot;
        this.unicorn.yRot = headYRot;
        this.uMouth.xRot = headXRot;
        this.uMouth.yRot = headYRot;
        this.lMouth.xRot = headXRot;
        this.lMouth.yRot = headYRot;
        this.uMouth2.xRot = headXRot - 0.0872664F;
        this.uMouth2.yRot = headYRot;
        this.lMouth2.xRot = headXRot + 0.261799F;
        this.lMouth2.yRot = headYRot;

        // The saddle group (pad, girth straps, stirrups, bit, head-stall and reins) only renders when
        // the horse is wearing a saddle. Armour, by contrast, is a full-texture swap with no geometry,
        // so there is nothing to toggle for state.horseArmor here.
        // The saddlebags (bag1 / bag2) show once the horse is fitted with a chest (bagger-horse storage,
        // legacy getIsChested) — MoCEntityHorse.hasChest() drives state.hasChest.
        this.bag1.visible = state.hasChest;
        this.bag2.visible = state.hasChest;

        boolean saddled = state.saddled;
        this.saddle.visible = saddled;
        this.saddleB.visible = saddled;
        this.saddleC.visible = saddled;
        this.saddleL.visible = saddled;
        this.saddleL2.visible = saddled;
        this.saddleR.visible = saddled;
        this.saddleR2.visible = saddled;
        this.saddleMouthL.visible = saddled;
        this.saddleMouthR.visible = saddled;
        this.saddleMouthLine.visible = saddled;
        this.saddleMouthLineR.visible = saddled;
        this.headSaddle.visible = saddled;

        // The unicorn horn shows only on the horned coats (unicorn / fairy / undead-unicorn /
        // unicorn-skeleton), computed from the synched type — legacy {@code isUnicorned()}.
        int type = state.typeMoC;
        boolean unicorned = type == 36 || (type >= 45 && type < 60) || type == 27 || type == 24;
        this.unicorn.visible = unicorned;

        // ---------------------------------------------------------------- wings
        // The horse sheet carries TWO mutually-exclusive wing sets that deliberately SHARE the same
        // corner of the 128x128 layout, because no coat ever wears both:
        //
        //   * the feathered pegasus wing — inner_wing (0,96), mid_wing (82,68), outer_wing (0,68) and
        //     their mirrors inner_wing_r (0,110), mid_wing_r (82,82), outer_wing_r (0,82);
        //   * the flat fairy/butterfly membranes — butterfly_l (0,98) and butterfly_r (0,68), each a
        //     26 x 0 x 30 quad whose up/down faces alone span u 30..82.
        //
        // butterfly_r's quad therefore samples exactly the same texels as outer_wing, and butterfly_l's
        // overlaps inner_wing / inner_wing_r. Legacy kept them apart purely by never drawing both:
        // {@code MoCModelNewHorse.render()} (legacy MoCModelNewHorse.java:462-486) draws the feathered
        // set for {@code isFlyer() && !isGhost() && type < 45} (white/dark pegasus 39/40, bat horse 32,
        // undead/skeleton pegasus 25/28) and the butterfly quads for the fairy coats (45-59) and the
        // ghost coats — never both on one horse.
        //
        // {@link ModelPart#visible} defaults to {@code true} and {@code resetPose()} does not clear it,
        // so a part that is never gated draws on EVERY horse. That is what produced the reported
        // "2D wing image floating offset from the actual wings": butterfly_l/butterfly_r were never
        // gated at all, so a pegasus rendered two big flat 26x30 planes cocked at +/-45 degrees that
        // sampled the feathered wing's own texels — a ghost copy of the wing art hanging off the flanks.
        // A fairy horse got the converse: the feathered cubes drawn on top of its butterfly membranes,
        // sampling the fairy sheet's butterfly art.
        boolean ghost = type == 21 || type == 22;
        boolean featheredWings = type == 39 || type == 40 || type == 32 || type == 25 || type == 28;
        boolean butterflyWings = (type >= 45 && type < 60) || ghost;
        this.midWing.visible = featheredWings;
        this.innerWing.visible = featheredWings;
        this.outerWing.visible = featheredWings;
        this.innerWingR.visible = featheredWings;
        this.midWingR.visible = featheredWings;
        this.outerWingR.visible = featheredWings;
        this.butterflyL.visible = butterflyWings;
        this.butterflyR.visible = butterflyWings;

        if (featheredWings) {
            animatePegasusWings(state);
        } else if (butterflyWings) {
            animateButterflyWings(state, ghost);
        }
    }

    /**
     * Feathered pegasus wing beat, converted from legacy {@code setRotationAngles} (legacy
     * MoCModelNewHorse.java:737-824).
     *
     * <p>The three segments of each wing are <em>siblings</em>, not a parent/child chain: inner and mid
     * pivot at the shoulder (x = &plusmn;5) while outer pivots 12 units further out (x = &plusmn;17).
     * Spinning all three about their own baked pivots — which is what the first port pass did — tears the
     * wing into three disconnected slabs the moment the flap angle leaves zero, because the outer segment
     * orbits a point that never moves. Legacy solved it by walking the outer pivot around the arc of the
     * inner one every frame:
     * <pre>
     *   OuterWing.rotationPointX = InnerWing.rotationPointX + cos(WingRot) * 12F;
     *   OuterWing.rotationPointY = InnerWing.rotationPointY + sin(WingRot) * 12F;
     * </pre>
     * so the elbow stays welded to the shoulder at every angle. That translation is reproduced below and
     * is the fix for the "wings render wrong while flapping" half of the report.
     *
     * <p>Sign convention: model space is Y-down (the renderer flips it), so a <em>positive</em> zRot
     * raises the left wing (which extends toward +X) and the mirrored <em>negative</em> zRot raises the
     * right — legacy drives {@code MidWing/InnerWing/OuterWing.rotateAngleZ = WingRot} and the R parts
     * with {@code -WingRot}. The first port pass had this inverted, so the wings beat downward through
     * the horse's ribs.
     *
     * <p>Grounded, legacy does not merely zero the flap: it <em>folds</em> the wing (legacy
     * MoCModelNewHorse.java:786-791) by forcing {@code WingRot} to 60&deg; — which drops the elbow
     * {@code sin(60) * 12 = 10.4} units down the flank — and swinging the outer segment 90&deg; back
     * along the body. Only while airborne does it fan out and beat.
     *
     * <p><b>Deviations from legacy, and why.</b> Legacy split the airborne case on
     * {@code wingFlapCounter != 0} (a deliberate beat, triggered by the rider pressing jump) versus
     * cruising (a 0.1 rad tremor). The port's render state carries no such counter for the horse, so an
     * airborne winged horse always beats at the legacy flap cadence
     * {@code cos(ageInTicks * 0.3 + PI) * 1.2}. Legacy also copied {@code Body.rotateAngleX} onto every
     * wing segment and shifted the shoulder to (y -5, z 4) while rearing; both existed to compensate for
     * a body-only pitch, and the port pitches the whole horse at the renderer instead
     * ({@code MoCMobRenderer.setupRotations}), so the wings already inherit the rear and need neither.
     */
    private void animatePegasusWings(MoCEntityRenderState state) {
        float wingRot;
        if (state.flying) {
            // Legacy: WingRot = cos((f2 * 0.3F) + PI) * 1.2F, with the outer segment's fan angle
            // opening and closing at half the beat amplitude around its baked -0.3228859 sweep.
            wingRot = Mth.cos(state.ageInTicks * 0.3F + (float) Math.PI) * 1.2F;
            this.outerWing.yRot = -0.3228859F + (wingRot / 2.0F);
            this.outerWingR.yRot = 0.3228859F - (wingRot / 2.0F);
        } else {
            // Legacy folded pose: 60 degrees of droop, outer primaries swept 90 degrees back.
            wingRot = 60.0F * DEG_TO_RAD;
            this.outerWing.yRot = -90.0F * DEG_TO_RAD;
            this.outerWingR.yRot = 90.0F * DEG_TO_RAD;
        }

        // Keep the elbow on the shoulder's arc (the 12-unit inner->outer pivot separation).
        float armX = Mth.cos(wingRot) * 12.0F;
        float armY = Mth.sin(wingRot) * 12.0F;
        this.outerWing.x = this.innerWing.x + armX;
        this.outerWingR.x = this.innerWingR.x - armX;
        this.outerWing.y = this.innerWing.y + armY;
        this.outerWingR.y = this.innerWingR.y + armY;
        this.outerWing.z = this.innerWing.z;
        this.outerWingR.z = this.innerWing.z;
        this.midWing.y = this.innerWing.y;
        this.midWingR.y = this.innerWing.y;
        this.midWing.z = this.innerWing.z;
        this.midWingR.z = this.innerWing.z;

        this.innerWing.zRot = wingRot;
        this.midWing.zRot = wingRot;
        this.outerWing.zRot = wingRot;
        this.innerWingR.zRot = -wingRot;
        this.midWingR.zRot = -wingRot;
        this.outerWingR.zRot = -wingRot;
    }

    /**
     * Fairy-horse (and ghost-horse) membrane flutter, converted from legacy {@code setRotationAngles}
     * (legacy MoCModelNewHorse.java:846-935).
     *
     * <p>The two quads pivot at the withers and open/close about Z from a 30&deg; base spread
     * ({@code 0.52359}); legacy overwrites the layer's baked &plusmn;45&deg; rest angle outright, which
     * is why the resting spread here is 30&deg; and not the 0.78539 baked into {@link #createBodyLayer}.
     * The cadence has three legacy branches: a fast cruise flutter while airborne
     * ({@code cos(f2 * 0.6662F) * 0.5F}), and on the ground a slow idle stretch that only runs during
     * ticks 40-60 of every 100 ({@code cos(f2 * 0.15F) * 1.20F}) and otherwise holds the wings still.
     * The ghost coats use a lazier {@code cos(f2 * 0.1F)} about a zero base, so their wings sweep all the
     * way from folded to flat.
     *
     * <p><b>Deviation 1 — fold ceiling.</b> The membranes are mirror images about x=0: the left one spans
     * local x -1..25 off a pivot at x=+4.5, the right one -25..1 off x=-4.5, and legacy rotates them by
     * equal and opposite Z angles. Solving the two swept lines against each other, they cross when
     * {@code cos(zRot) <= -0.18}. Legacy's grounded idle branch is
     * {@code -0.52359 + cos(f2 * 0.15F) * 1.20F}, i.e. down to <b>-98.7&deg;</b> where
     * {@code cos = -0.153}: the wing tips swing over the mid-line to x = &plusmn;0.68 and pass within
     * <em>1.4 model units</em> (0.09 blocks) of each other, a hair short of a real intersection but
     * indistinguishable from one on screen — the reported "wings clip through each other". (Legacy's own
     * to-do at MoCModelNewHorse.java:796-799 wants the idle motion to run "from closing up to
     * horizontal", so folding upright is intended; meeting at the mid-line is not.) Clamping the fold to
     * {@link #BUTTERFLY_MAX_FOLD} keeps {@code cos(zRot)} positive, which makes the two planes provably
     * divergent at every angle: at the new extreme the tips stand 13.4 units apart and the wing still
     * reads as a closed butterfly.
     *
     * <p><b>Deviation 2 — the {@code wingFlapCounter} beat.</b> Legacy's fourth branch
     * ({@code cos(f2 * 0.9F) * 0.9F}) has no counterpart in the port's render state and is folded into
     * the cruise branch. Legacy also drew the fairy membranes blended at 1.3x scale (legacy
     * MoCModelNewHorse.java:456-464); that is renderer-level and not reproduced here.
     *
     * <p>Not reproduced, deliberately: legacy shoved the pivot to (y -2.5, z 6.5) while rearing (legacy
     * MoCModelNewHorse.java:849-855) to compensate for a body-only pitch. The port pitches the whole
     * horse at the renderer ({@code MoCMobRenderer.setupRotations}), so the wings already inherit it —
     * the same reasoning as {@link #animatePegasusWings}.
     */
    private void animateButterflyWings(MoCEntityRenderState state, boolean ghost) {
        float wingRot;
        float baseAngle;
        if (ghost) {
            wingRot = Mth.cos(state.ageInTicks * 0.1F);
            baseAngle = 0.0F;
        } else {
            baseAngle = 0.52359F;
            if (state.flying) {
                wingRot = Mth.cos(state.ageInTicks * 0.6662F) * 0.5F;
            } else {
                float phase = state.ageInTicks % 100.0F;
                wingRot = (phase > 40.0F && phase < 60.0F) ? Mth.cos(state.ageInTicks * 0.15F) * 1.20F : 0.0F;
            }
        }
        // Legacy drives ButterflyL by -baseAngle + WingRot and ButterflyR by +baseAngle - WingRot
        // (MoCModelNewHorse.java:870-871), i.e. one angle mirrored; clamp that single angle so neither
        // wing can ever sweep to the mid-line.
        float fold = Mth.clamp(baseAngle - wingRot, -BUTTERFLY_MAX_FOLD, BUTTERFLY_MAX_FOLD);
        this.butterflyL.zRot = -fold;
        this.butterflyR.zRot = fold;
    }
}
