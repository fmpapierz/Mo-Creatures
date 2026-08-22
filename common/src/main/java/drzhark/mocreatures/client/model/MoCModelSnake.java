package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.Set;

/**
 * Snake model, converted faithfully from the legacy {@code MoCModelSnake} ({@code ModelBase}).
 *
 * <p>The legacy model built a {@code bodySnake[40]} array of segments procedurally and animated the whole
 * body by pushing a GL matrix per segment in {@code render()} — a travelling lateral wave plus a few stacked
 * pose translations (legacy {@code MoCModelSnake}:173-333). That maps one-for-one onto the modern pipeline:
 * every snake part here is a direct child of the root, never a link in a parent chain, so {@link ModelPart#x}
 * / {@code y} / {@code z} — which {@code translateAndRotate} applies before the part's own rotation, after
 * dividing by 16 (mc262-ref {@code ModelPart}:166-175) — reproduce the legacy per-segment {@code glTranslatef}
 * exactly. Legacy translated in block units, outside {@code ModelRenderer.render(0.0625F)}, hence the 16x
 * conversion in {@link #offset}.</p>
 *
 * <p>Per segment the pose is the sum of: the travelling wave
 * {@code 0.5*sin(1.5t - 0.3i) - (movInt/20)*sin(0.8t - 0.2i)} with {@code t = walkAnimationPos/2}
 * (legacy :264-268) — a function of the walk position alone, never of the walk speed, so it freezes at its
 * last phase rather than straightening out when the snake stops; the climbing arc; the near-player head rear
 * with the {@code sideperf} ramp that damps the wave dead over the front sixth; the rattlesnake tail raise;
 * and the picked-up rear-half droop (legacy :193-233). The attached parts — head/nose/teeth/tongues, the five
 * cobra hood pairs, the rattle — inherit the offsets of the segment inside whose iteration legacy drew them
 * (:281-330), which is also where their {@code visible} gating comes from: exactly one of the three tongue
 * planes at a time, the hood only for a type-6 cobra rearing at a player, the rattle only for type 7.</p>
 *
 * <p>Legacy's {@code isResting} branch (:179-190) is deliberately not ported. Its body is entirely commented
 * out, so the branch never posed anything itself; its only effect was heading the else-if chain and thereby
 * SUPPRESSING the two branches below it. It cannot collide with the climbing branch, which needs upward
 * motion while {@code isResting} needs a still snake. It genuinely CAN collide with the third branch,
 * though: that branch fires on {@code nearPlayer || picked}, and {@code isResting} only excludes the
 * {@code nearPlayer} half — being carried does not imply a player is near, so a carried, motionless,
 * grounded snake satisfies both. (An earlier revision of this javadoc claimed otherwise; that proof was
 * wrong.) What kept legacy out of the overlap was {@code onGround}: a legacy carried snake was a real
 * passenger ({@code startRiding(player)}, legacy {@code MoCEntitySnake}:176-179), and legacy's own
 * {@code onUpdate}:353 treats {@code !onGround && getRidingEntity() != null} as the ordinary carried case.</p>
 *
 * <p>PORT-SPECIFIC HAZARD. This port carries a pet with {@code MoCAnimal.tickCarried}, which pins it at the
 * carrier's head with {@code setPos()} and never calls {@code move()} itself. {@code onGround()} is
 * nonetheless still false while carried — but only incidentally, because {@code tickCarried} runs after
 * {@code super.tick()}, so the server's {@code LivingEntity.travel} -> {@code Entity.move} still executes
 * each tick and recomputes {@code onGround} from {@code verticalCollisionBelow} (mc262-ref
 * {@code Entity}:764-767), which is false for a pet hanging in mid-air; the client then mirrors that flag
 * (mc262-ref {@code ServerEntity}:155-170). Nothing in the carry code guarantees it. If {@code tickCarried}
 * ever short-circuits the AI/travel step, {@code onGround()} would freeze at its stale pre-pickup
 * {@code true}, {@code MoCEntitySnake.isResting()} would start returning true for a carried snake, and this
 * omitted branch would begin to matter — legacy would then freeze the carried droop off. Harmless today
 * because {@link #setupAnim} never reads {@code isResting()} at all.</p>
 */
public class MoCModelSnake extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = 1F / 57.29578F;
    private static final int BODYPARTS = 40;

    // Zero-thickness planes (teeth, tongue segments) are split into two single-face boxes (painted face +
    // a re-aimed opposite face sampling the same painted tile) so the culled render type keeps them
    // visible from both sides.
    private static final Set<Direction> DOWN_ONLY = Set.of(Direction.DOWN);
    private static final Set<Direction> UP_ONLY = Set.of(Direction.UP);
    private static final Set<Direction> WEST_ONLY = Set.of(Direction.WEST);
    private static final Set<Direction> EAST_ONLY = Set.of(Direction.EAST);

    private final ModelPart[] bodySnake = new ModelPart[BODYPARTS];
    /** Cobra-hood cubes flanking the neck (legacy Wing1L..5L / Wing1R..5R); shown flared for a type-6 cobra. */
    private final ModelPart[] hoodL = new ModelPart[5];
    private final ModelPart[] hoodR = new ModelPart[5];
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart lNose;
    private final ModelPart teethUR;
    private final ModelPart teethUL;
    private final ModelPart tongue;
    private final ModelPart tongue1;
    private final ModelPart tongue0;
    private final ModelPart tail;

    public MoCModelSnake(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        for (int i = 0; i < BODYPARTS; i++) {
            this.bodySnake[i] = root.getChild("body" + i);
        }
        for (int i = 0; i < 5; i++) {
            this.hoodL[i] = root.getChild("hood_l" + (i + 1));
            this.hoodR[i] = root.getChild("hood_r" + (i + 1));
        }
        this.head = root.getChild("head");
        this.nose = root.getChild("nose");
        this.lNose = root.getChild("l_nose");
        this.teethUR = root.getChild("teeth_ur");
        this.teethUL = root.getChild("teeth_ul");
        this.tongue = root.getChild("tongue");
        this.tongue1 = root.getChild("tongue1");
        this.tongue0 = root.getChild("tongue0");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        final float fsegm = 1F / 8F;
        final float fsep = -1.6F;
        float flength = 0.0F;

        for (int i = 0; i < BODYPARTS; i++) {
            flength = ((BODYPARTS / 2) - i) * fsep;

            float factor;
            float fport = ((i + 1F) / BODYPARTS);
            if (fport < fsegm) {
                factor = -0.20F;
            } else if (fport < (fsegm * 2F)) {
                factor = -0.15F;
            } else if (fport < (fsegm * 4F)) {
                factor = 0.0F;
            } else if (fport < (fsegm * 6F)) {
                factor = 0.0F;
            } else if (fport < (fsegm * 7F)) {
                factor = -0.15F;
            } else {
                factor = -0.2F;
            }

            int j = (i % 2 == 0) ? 0 : 4;
            root.addOrReplaceChild("body" + i,
                    CubeListBuilder.create().texOffs(8, j)
                            .addBox(-1F, -0.5F, 0F, 2, 2, 2, new CubeDeformation(factor)),
                    PartPose.offset(0F, 23F, flength));
        }

        // Cobra hood cubes (legacy Wing1..5 L/R): 2x2x2 cubes flanking the neck at the body1..5 positions,
        // their baked X offsets forming the flared hood shape. Hidden unless a type-6 cobra rears at a player.
        float[] hoodZ = new float[5];
        for (int n = 1; n <= 5; n++) {
            hoodZ[n - 1] = ((BODYPARTS / 2) - n) * fsep;
        }
        // Left/right baked X offsets per pair (from the legacy addBox origins).
        float[] lx = {0.0F, 0.5F, 1.0F, 0.5F, 0.0F};
        float[] rx = {-2.0F, -2.5F, -3.0F, -2.5F, -2.0F};
        int[][] uv = {{8, 4}, {8, 4}, {16, 4}, {16, 8}, {16, 8}};
        for (int n = 0; n < 5; n++) {
            root.addOrReplaceChild("hood_l" + (n + 1),
                    CubeListBuilder.create().texOffs(uv[n][0], uv[n][1]).addBox(lx[n], -0.5F, 0F, 2, 2, 2),
                    PartPose.offset(0F, 23F, hoodZ[n]));
            root.addOrReplaceChild("hood_r" + (n + 1),
                    CubeListBuilder.create().texOffs(uv[n][0], uv[n][1]).addBox(rx[n], -0.5F, 0F, 2, 2, 2),
                    PartPose.offset(0F, 23F, hoodZ[n]));
        }

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 0.5F, -1.0F, 1, 1, 5),
                PartPose.offset(0F, 23F, flength));

        flength = (BODYPARTS / 2) * fsep;

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -0.5F, -2F, 2, 2, 2),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -0.3F, -4F, 1, 1, 2),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("l_nose",
                CubeListBuilder.create().texOffs(22, 0).addBox(-0.5F, 0.3F, -4F, 1, 1, 2),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("teeth_ur",
                CubeListBuilder.create().texOffs(46, 0).addBox(-0.4F, 0.3F, -3.8F, 0.0F, 1.0F, 1.0F, EAST_ONLY)
                        .texOffs(47, 0).addBox(-0.4F, 0.3F, -3.8F, 0.0F, 1.0F, 1.0F, WEST_ONLY),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("teeth_ul",
                CubeListBuilder.create().texOffs(44, 0).addBox(0.4F, 0.3F, -3.8F, 0.0F, 1.0F, 1.0F, WEST_ONLY)
                        .texOffs(43, 0).addBox(0.4F, 0.3F, -3.8F, 0.0F, 1.0F, 1.0F, EAST_ONLY),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("tongue",
                CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0.5F, -6F, 1.0F, 0.0F, 3.0F, DOWN_ONLY)
                        .texOffs(27, 0).addBox(-0.5F, 0.5F, -6F, 1.0F, 0.0F, 3.0F, UP_ONLY),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("tongue1",
                CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0.5F, -5F, 1.0F, 0.0F, 3.0F, DOWN_ONLY)
                        .texOffs(27, 0).addBox(-0.5F, 0.5F, -5F, 1.0F, 0.0F, 3.0F, UP_ONLY),
                PartPose.offset(0F, 23F, flength));
        root.addOrReplaceChild("tongue0",
                CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0.25F, -4F, 1.0F, 0.0F, 3.0F, DOWN_ONLY)
                        .texOffs(27, 0).addBox(-0.5F, 0.25F, -4F, 1.0F, 0.0F, 3.0F, UP_ONLY),
                PartPose.offset(0F, 23F, flength));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float rAX = state.xRot * DEG_TO_RAD;
        float rAY = state.yRot * DEG_TO_RAD;

        this.head.xRot = rAX;
        this.head.yRot = rAY;
        this.bodySnake[0].xRot = rAX * 0.95F;
        this.bodySnake[1].xRot = rAX * 0.90F;
        this.bodySnake[2].xRot = rAX * 0.85F;
        this.bodySnake[3].xRot = rAX * 0.80F;
        this.bodySnake[4].xRot = rAX * 0.75F;

        // Jaw split + tongue flick, straight off the snake's client-side timers (legacy
        // setRotationAngles:354-362). fMouth is 0 with the mouth shut and 0.1-0.5 through a gape; the tongue
        // tip waves at cos(fTongue * 10) / 40.
        float fMouth = state.snakeMouth;
        float f8 = Mth.cos(state.snakeTongue * 10F) / 40F;

        this.nose.xRot = this.head.xRot - fMouth;
        this.lNose.xRot = this.head.xRot + fMouth;
        this.tongue1.xRot = this.head.xRot + f8;
        this.tongue.xRot = this.head.xRot + f8;
        // legacy :360 — the retracted tongue rides the lower jaw flat, with no flick of its own
        this.tongue0.xRot = this.lNose.xRot;
        this.teethUR.xRot = this.head.xRot - fMouth;
        this.teethUL.xRot = this.head.xRot - fMouth;

        this.bodySnake[0].yRot = rAY * 0.85F;
        this.bodySnake[1].yRot = rAY * 0.65F;
        this.bodySnake[2].yRot = rAY * 0.45F;
        this.bodySnake[3].yRot = rAY * 0.25F;
        this.bodySnake[4].yRot = rAY * 0.10F;

        this.nose.yRot = this.head.yRot;
        this.tongue.yRot = this.head.yRot;
        this.tongue0.yRot = this.head.yRot;
        this.tongue1.yRot = this.head.yRot;
        this.lNose.yRot = this.head.yRot;
        this.teethUR.yRot = this.head.yRot;
        this.teethUL.yRot = this.head.yRot;

        // Exactly one of the three tongue planes is drawn (legacy render:289-298): the retracted tongue0 while
        // the flick timer is idle, the short tongue1 at the start and end of a flick (or whenever the jaw is
        // open) and the long tongue in between. They overlap in z, so drawing more than one at a time reads as
        // a single over-long stacked tongue.
        boolean tongueOut = state.snakeTongue != 0.0F;
        boolean shortTongue = tongueOut
                && (state.snakeMouth != 0.0F || state.snakeTongue < 2.0F || state.snakeTongue > 7.0F);
        this.tongue0.visible = !tongueOut;
        this.tongue1.visible = shortTongue;
        this.tongue.visible = tongueOut && !shortTongue;

        // Cobra hood: shown only when a type-6 cobra rears at a nearby player (legacy render:304-326,
        // typeI == 6 && nearplayer). Each hood cube tracks its neck segment so the flare turns with the head.
        boolean hood = state.snakeHoodFlared;
        for (int n = 0; n < 5; n++) {
            this.hoodL[n].visible = hood;
            this.hoodR[n].visible = hood;
            if (hood) {
                // legacy setRotationAngles:378-401 — Wing1..4 copy bodySnake[1..4], and Wing5 copies
                // bodySnake[4] as well; bodySnake[5] never carries a rotation, so copying it would leave the
                // last hood pair behind when the neck turns.
                int src = Math.min(n + 1, 4);
                float yr = this.bodySnake[src].yRot;
                float xr = this.bodySnake[src].xRot;
                this.hoodL[n].yRot = yr;
                this.hoodR[n].yRot = yr;
                this.hoodL[n].xRot = xr;
                this.hoodR[n].xRot = xr;
            }
        }

        // Rattle: only a rattlesnake has one (legacy render:328-330), and it lies flat unless the snake is
        // near a player or mid-rattle, when it stands up ~110 degrees and jitters ±20 with the head yaw
        // (legacy setRotationAngles:404-412 — the shake argument is f3, the head yaw, not a clock).
        this.tail.visible = state.typeMoC == 7;
        if (this.tail.visible) {
            this.tail.xRot = (state.snakeNearPlayer || state.snakeRattle != 0.0F)
                    ? ((Mth.cos(state.yRot * 10F) * 20F) + 90F) * DEG_TO_RAD
                    : 0.0F;
        }

        // ---------------------------------------------------------------- per-segment body pose
        // Legacy render():173-333 pushed a matrix per segment and accumulated every translate below onto it
        // before drawing that segment; here they are summed and written once into the part's own offsets.
        final float w = 1.5F;
        final float t = state.walkAnimationPos / 2F; // legacy 'f' == limbSwing; NOT scaled by the walk speed
        final float movInt = state.snakeMovInt;
        // Legacy 'f6' == entitysnake.bodyswing, read straight off the entity (legacy render:154). It is a
        // genuine client-side animation ramp, NOT a constant: legacy drove it from the isBiting flag inside
        // onUpdate's if (world.isRemote) block (legacy :327-340), and the client learned about a bite from
        // the MoCMessageAnimation packet the server broadcast. So 2.0 at rest, dropping 0.5 a tick through a
        // bite and resetting to 2.5. It is deliberately NOT state.attackSwing: getAttackAnim is only ever
        // written by updateSwingTime, which in 26.2 never runs for an Animal (mc262-ref Monster:43,
        // Player:454, RemotePlayer:67, Mannequin:179 are its only callers), so attackSwing is pinned at 0
        // for every Mo'Creatures animal and the old 2.0F * (1.0F - attackSwing) was a dead input.
        final float bodySwing = state.snakeBodySwing;
        final boolean climbing = state.snakeClimbing;
        final boolean near = state.snakeNearPlayer;
        final boolean picked = state.riding;

        for (int i = 0; i < BODYPARTS; i++) {
            float sideperf = 1.0F;
            float dx = 0.0F;
            float dy = 0.0F;
            float dz = 0.0F;

            if (climbing && i < BODYPARTS / 2) {
                // legacy :193-196 — the front half rises up the wall (-Y is up in the model frame) and
                // compresses back toward the rest of the body.
                float yOff = (i - (BODYPARTS / 2)) * 0.08F;
                dy += yOff / 3.0F;
                dz += -yOff * 1.2F;
            } else if (near || picked) {
                // legacy :199-217 — the front third rears into a near-vertical column; bodySwing snaps it
                // forward again through a bite.
                if (i < BODYPARTS / 3) {
                    float yOff = (i - (BODYPARTS / 3)) * 0.09F;
                    float zOff = (i - (BODYPARTS / 3)) * 0.065F;
                    dy += yOff / 1.5F;
                    dz += -zOff * bodySwing;
                }
                // legacy :208-215 — the front sixth is dead straight, then the ramp is evaluated from the
                // literal 'i - 7', which is deliberately a hair negative at i == 6 (the wave briefly inverts
                // on that one segment) and clamps to 1 from i == 21 on.
                sideperf = (i < BODYPARTS / 6) ? 0.0F : Math.min((i - 7) / (BODYPARTS / 3F), 1.0F);
            }
            if (state.typeMoC == 7 && near && i > (5 * BODYPARTS / 6) && !picked) {
                // legacy :220-224 — a rattlesnake lifts its last six segments so the rattle shows.
                float yOff = 0.55F + ((i - BODYPARTS) * 0.08F);
                dy += -yOff / 1.5F;
            }
            if (picked && i > BODYPARTS / 2) {
                // legacy :228-233 — carried, the back half hangs down and folds forward under the hand.
                float yOff = (i - (BODYPARTS / 2)) * 0.08F;
                dy += yOff / 1.5F;
                dz += -yOff;
            }

            // legacy :264-268 — the travelling wave itself, damped by sideperf.
            dx += sideperf * (0.5F * Mth.sin(w * t - 0.3F * i)
                    - (movInt / 20F) * Mth.sin(0.8F * t - 0.2F * i));

            offset(this.bodySnake[i], dx, dy, dz);
            if (i == 0) { // legacy :281-300 — the head group rides segment 0
                offset(this.head, dx, dy, dz);
                offset(this.nose, dx, dy, dz);
                offset(this.lNose, dx, dy, dz);
                offset(this.teethUR, dx, dy, dz);
                offset(this.teethUL, dx, dy, dz);
                offset(this.tongue, dx, dy, dz);
                offset(this.tongue1, dx, dy, dz);
                offset(this.tongue0, dx, dy, dz);
            }
            if (i >= 1 && i <= 5) { // legacy :304-326 — hood pair n rides segment n
                offset(this.hoodL[i - 1], dx, dy, dz);
                offset(this.hoodR[i - 1], dx, dy, dz);
            }
            if (i == BODYPARTS - 1) { // legacy :328-330 — the rattle rides the last segment
                offset(this.tail, dx, dy, dz);
            }
        }
    }

    /**
     * Adds a legacy {@code glTranslatef} — given in BLOCKS, as legacy issued it outside
     * {@code ModelRenderer.render(0.0625F)} — to a part's own offset, which {@code translateAndRotate} divides
     * by 16 and applies before the part's rotation (mc262-ref {@code ModelPart}:167). Both are plain
     * pre-rotation translations, so adding 16x the block offset onto the baked pose is exact.
     *
     * <p>{@code +=} rather than {@code =}: {@code super.setupAnim} has already called {@code resetPose()}
     * (mc262-ref {@code Model}:46-54), restoring each part's baked {@code PartPose} for this frame.</p>
     */
    private static void offset(ModelPart part, float dx, float dy, float dz) {
        part.x += dx * 16.0F;
        part.y += dy * 16.0F;
        part.z += dz * 16.0F;
    }
}
