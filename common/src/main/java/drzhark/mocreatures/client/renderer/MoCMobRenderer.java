package drzhark.mocreatures.client.renderer;

import java.util.function.Function;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Generic renderer for Mo'Creatures entities. Because every creature's texture is selected by the
 * entity itself (via {@link IMoCEntity#getTexture()}) and exposed through {@link MoCEntityRenderState},
 * a single parameterized renderer can serve every creature — animals, mobs and aquatics alike —
 * eliminating ~40 hand-written renderer classes. Creatures that need bespoke render behaviour can
 * subclass this.
 *
 * @param <T> the Mo'Creatures entity type (any {@link Mob} implementing {@link IMoCEntity})
 */
public class MoCMobRenderer<T extends Mob & IMoCEntity> extends MobRenderer<T, MoCEntityRenderState, EntityModel<MoCEntityRenderState>> {

    /** How far ahead of the torso's center the lead-dangled kitty hangs, in blocks (user-tuned). */
    private static final float KITTY_DANGLE_FORWARD = 0.18F;

    public MoCMobRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
            Function<ModelPart, ? extends EntityModel<MoCEntityRenderState>> modelFactory, float shadowRadius) {
        super(context, modelFactory.apply(context.bakeLayer(layer)), shadowRadius);
    }

    @Override
    public MoCEntityRenderState createRenderState() {
        return new MoCEntityRenderState();
    }

    @Override
    public void extractRenderState(T entity, MoCEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // A carried pet is pinned to its carrier's head once per TICK (MoCAnimal.tickCarried), but the
        // carrier is drawn at a per-FRAME interpolated position, so the pet trailed its carrier by up to a
        // tick while moving. Vanilla has a mechanism for exactly this — EntityRenderState.passengerOffset,
        // used for new-physics minecart passengers (mc262-ref EntityRenderer.java:174-184; returned by
        // getRenderOffset:101-103 and added to the render translation in EntityRenderDispatcher.submit:153-158).
        // The super call above just nulled it, so reuse it: recompute the carrier's interpolated head
        // position this frame and offset the pet from its own interpolated position to it. The tick-side
        // pin still owns the pet's LOGICAL position (collision, save, AI); this only moves where it is drawn.
        if (entity instanceof drzhark.mocreatures.entity.MoCAnimal carried && carried.isBeingCarried()
                && carried.getCarrier() instanceof net.minecraft.world.entity.player.Player carrier) {
            double px = net.minecraft.util.Mth.lerp(partialTick, entity.xOld, entity.getX());
            double py = net.minecraft.util.Mth.lerp(partialTick, entity.yOld, entity.getY());
            double pz = net.minecraft.util.Mth.lerp(partialTick, entity.zOld, entity.getZ());
            double cx = net.minecraft.util.Mth.lerp(partialTick, carrier.xOld, carrier.getX());
            double cy = net.minecraft.util.Mth.lerp(partialTick, carrier.yOld, carrier.getY())
                    + carrier.getBbHeight() - carried.carriedHeadSink();
            double cz = net.minecraft.util.Mth.lerp(partialTick, carrier.zOld, carrier.getZ());
            double ox = cx - px;
            double oy = cy - py;
            double oz = cz - pz;
            // The kitty's side/shoulder carries (states 14/15) are locked to the carrier's BODY, not the
            // look: super.extractRenderState just set state.bodyRot from the PET's own tick-sampled yaw
            // (solveBodyRot, mc262-ref LivingEntityRenderer.java:311-323), which MoCAnimal.tickCarried
            // copies from the carrier only once per tick. Overwrite it with the carrier's per-frame
            // interpolated body yaw — the EXACT expression solveBodyRot uses to orient the carrier's own
            // model (rotLerp of yBodyRotO/yBodyRot, :323) — so the cat turns frame-locked with the torso
            // and ignores camera panning entirely. state.yRot is the head-minus-body delta (:249); zero it
            // so the carried pose can't cock the head against the new frame.
            if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityKitty kitty
                    && (kitty.getKittyState() == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_HELD_UPSIDE_DOWN
                            || kitty.getKittyState() == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_ON_SHOULDER)) {
                float carrierBodyRot = net.minecraft.util.Mth.rotLerp(partialTick, carrier.yBodyRotO, carrier.yBodyRot);
                state.bodyRot = carrierBodyRot;
                state.yRot = 0.0F;
                // The dangling cat hangs at the MAIN-HAND side — the same arm the HumanoidModelMixins
                // freeze into the lead-holding pose — centered on the body's front-back axis (no forward
                // component; the old pose rode 0.55 in front of the player). Applied here in world space,
                // from the same interpolated body yaw as the rotation above, because setupRotations has no
                // access to the carrier's main hand: right of the body heading is (-cos yaw, 0, -sin yaw).
                if (kitty.getKittyState() == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_HELD_UPSIDE_DOWN) {
                    float yawRad = carrierBodyRot * ((float) Math.PI / 180.0F);
                    float towardMainHand =
                            carrier.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
                    ox -= net.minecraft.util.Mth.cos(yawRad) * 0.35F * towardMainHand;
                    oz -= net.minecraft.util.Mth.sin(yawRad) * 0.35F * towardMainHand;
                    // The kitty model's origin sits behind its body midpoint, so a purely sideways offset
                    // reads as the cat hanging aft of the torso. Nudge it forward along the body heading
                    // (forward = (-sin yaw, 0, cos yaw)) until the legs straddle the torso's front-back
                    // center — user-tuned.
                    ox += -net.minecraft.util.Mth.sin(yawRad) * KITTY_DANGLE_FORWARD;
                    oz += net.minecraft.util.Mth.cos(yawRad) * KITTY_DANGLE_FORWARD;
                }
            }
            state.passengerOffset = new net.minecraft.world.phys.Vec3(ox, oy, oz);
        }
        state.texture = entity.getTexture();
        state.typeMoC = entity.getTypeMoC();
        state.baby = entity.isBaby();
        state.saddled = entity instanceof drzhark.mocreatures.entity.MoCAnimal animal && animal.isSaddled();
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityElephant elephant) {
            state.armorStage = elephant.getArmorStage();
            state.hasChest = elephant.hasChest();
            state.tusks = elephant.getTusks();
        } else {
            state.armorStage = 0;
            state.hasChest = false;
            state.tusks = 0;
        }
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse horse) {
            state.horseArmor = horse.getArmor();
            state.hasChest = horse.hasChest(); // saddlebags (bag1/bag2) render on a chested bagger horse
            state.horseEating = horse.getEating();
            state.horseRearing = horse.getRearing();
        } else {
            state.horseArmor = 0;
            state.horseEating = false;
            state.horseRearing = false;
        }
        // Flyers flap/tuck while airborne instead of the walk gait. Wyvern: legacy onAir (its synched cruise flag
        // or no ground under it, see MoCEntityWyvern.isWyvernFlying);
        // a winged horse uses the same ground probe (MoCAnimal.isOffGround), since a rider-flown horse does not report
        // onGround reliably while hovering.
        // Insects, crickets and ducks flap their wings and switch to the in-flight leg pose whenever airborne
        // (legacy isOnAir()/getIsFlying()).
        state.flying = (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityWyvern wyvern && wyvern.isWyvernFlying())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse horse && horse.isFlyer() && horse.isOffGround())
                || (entity instanceof drzhark.mocreatures.entity.MoCFlyingInsect && !entity.onGround())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityCricket && !entity.onGround())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityDuck && !entity.onGround())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityRoach && !entity.onGround())
                // Bird: legacy MoCModelBird's isOnAir (air under it, and not perched on anyone) tucks its legs.
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityBird bird && !bird.hasGroundBelow(1.2D)
                        && !bird.isPassenger() && !bird.isBeingCarried());
        if (entity instanceof drzhark.mocreatures.entity.monster.MoCEntityGolem golem) {
            state.golemCubeMask = golem.getCubeMask();
            state.golemState = golem.getGolemState();
            state.golemOpenChest = golem.openChest();
            state.golemThrowing = golem.isThrowing();
        } else {
            state.golemCubeMask = 0;
            state.golemState = 0;
            state.golemOpenChest = false;
            state.golemThrowing = false;
        }
        state.fishBowlType = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityFishBowl fb ? fb.getBowlType() : 0;
        state.fishBowlRotation = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityFishBowl fbr ? fbr.getSwimRotation() : 0;
        state.moCAge = entity.getMoCAge();
        state.adult = entity.getIsAdult();
        state.sizeFactor = entity.getSizeFactor();
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityOstrich ostrich) {
            state.ostrichHelmet = ostrich.getHelmet();
            state.ostrichFlagColor = ostrich.getFlagColor();
            state.ostrichChested = ostrich.getIsChested();
        } else {
            state.ostrichHelmet = 0;
            state.ostrichFlagColor = 0;
            state.ostrichChested = false;
        }
        state.bearState = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityBear bear ? bear.getBearState() : 0;
        state.moleState = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityMole mole ? mole.getState() : 0;
        state.riding = entity.isPassenger()
                || (entity instanceof drzhark.mocreatures.entity.MoCAnimal carried && carried.isBeingCarried());
        state.sprinting = entity.isSprinting();
        // Bunny bound progress (0..1, 0 = not hopping) for MoCModelBunny's leap pose: the vanilla
        // RabbitRenderer.extractRenderState jumpCompletion pattern (see MoCEntityBunny.getHopCompletion).
        state.bunnyHop = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityBunny bunny
                ? bunny.getHopCompletion(partialTick) : 0.0F;
        if (entity instanceof drzhark.mocreatures.entity.monster.MoCEntitySilverSkeleton skeleton) {
            state.silverSkeletonLeftSwing = skeleton.getLeftSwingTick();
            state.silverSkeletonRightSwing = skeleton.getRightSwingTick();
        } else {
            state.silverSkeletonLeftSwing = 0;
            state.silverSkeletonRightSwing = 0;
        }
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityTurtle turtle) {
            state.turtleUpsideDown = turtle.getIsUpsideDown();
            state.turtleHiding = turtle.getIsHiding();
            state.turtleFlipProgress = turtle.getFlipProgress();
        } else {
            state.turtleUpsideDown = false;
            state.turtleHiding = false;
            state.turtleFlipProgress = 0.0F;
        }
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityCrocodile croc) {
            state.crocInWater = croc.isInWater();
            state.crocBiting = croc.getHasCaughtPrey();
        } else {
            state.crocInWater = false;
            state.crocBiting = false;
        }
        state.ostrichHiding = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityOstrich ost && ost.getHiding();
        state.werewolfHunched = entity instanceof drzhark.mocreatures.entity.monster.MoCEntityWerewolf ww && ww.isHunched();
        state.scorpionAttacking = entity instanceof drzhark.mocreatures.entity.monster.MoCEntityScorpion scp && scp.isStinging();
        state.crabClawsUp = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityCrab crab && crab.getClawsUp();
        state.bigcatJawOpen = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat bc && bc.getJawOpen();
        // Snake: the whole body pose is driven from client-side cosmetic timers the legacy model read straight
        // off the entity (legacy MoCModelSnake.render:146-156). snakeHoodFlared is derived here rather than
        // carried on the entity because legacy derived it client-side too, opening the hood on exactly the
        // test that rears the neck — typeI == 6 && nearplayer (legacy MoCModelSnake.render:304-326).
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntitySnake snake) {
            state.snakeMovInt = snake.getMovInt();
            state.snakeNearPlayer = snake.getNearPlayer();
            state.snakeClimbing = snake.isClimbing();
            state.snakeTongue = snake.getFTongue();
            state.snakeMouth = snake.getFMouth();
            state.snakeRattle = snake.getFRattle();
            state.snakeBodySwing = snake.getBodySwing();
            state.snakeHoodFlared = snake.getTypeMoC() == 6 && state.snakeNearPlayer;
        } else {
            state.snakeMovInt = 0;
            state.snakeNearPlayer = false;
            state.snakeClimbing = false;
            state.snakeTongue = 0.0F;
            state.snakeMouth = 0.0F;
            state.snakeRattle = 0.0F;
            state.snakeBodySwing = 2.0F;
            state.snakeHoodFlared = false;
        }
        state.litterBoxUsed = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityLitterBox lb && lb.getUsed();
        state.kittyBedColour = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityKittyBed kb ? kb.getSheetColour() : 0;
        state.jellyfishGlowing = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityJellyFish jf && jf.isGlowingNow();
        state.jellyfishBeached = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityJellyFish jfb
                && !jfb.isInWater() && jfb.onGround();
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityWyvern wyv) {
            state.wyvernArmor = wyv.getArmorType();
            state.wyvernChested = wyv.getIsChested();
            state.wyvernSitting = wyv.isSitting();
            state.wyvernFlapping = wyv.isWingFlapping();
            state.wyvernRidden = wyv.isVehicle();
        } else {
            state.wyvernArmor = 0;
            state.wyvernChested = false;
            state.wyvernSitting = false;
            state.wyvernFlapping = false;
            state.wyvernRidden = false;
        }
        state.scorpionHasBabies = entity instanceof drzhark.mocreatures.entity.monster.MoCEntityScorpion scb && scb.getHasBabies();
        state.crocRolling = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityCrocodile cr
                && cr.getHasCaughtPrey() && cr.isInWater();
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityKitty kitty) {
            state.kittySitting = kitty.isSitting();
            state.kittyState = kitty.getKittyState();
            state.kittyTamed = kitty.getIsTamed();
            if (kitty.isRotationLocked()) {
                // Lying in its bed or asleep: drawn exactly at its seat, head straight and still (the server
                // pins the same rotations; drawing them here keeps interpolation and look jitter out of it).
                state.bodyRot = kitty.getSeatYaw();
                state.yRot = 0.0F;
                state.xRot = 0.0F;
            }
            state.kittyEmoticon = kittyEmoticon(kitty, state);
            state.kittySleepZ = kitty.getKittyState() == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_SLEEPING
                    && !kitty.isBeingCarried();
            if (state.kittySleepZ) {
                // the head: a little ahead of the feet, the way the sleeper faces, and up at curled-up head height
                float yaw = kitty.getSeatYaw() * net.minecraft.util.Mth.DEG_TO_RAD;
                float size = kitty.getIsAdult() ? 1.0F : 0.6F;
                state.kittyHeadX = -net.minecraft.util.Mth.sin(yaw) * 0.3F * size;
                state.kittyHeadZ = net.minecraft.util.Mth.cos(yaw) * 0.3F * size;
                state.kittyHeadY = 0.5F * size;
            }
        } else {
            state.kittySitting = false;
            state.kittyState = 0;
            state.kittyTamed = false;
            state.kittyEmoticon = null;
            state.kittySleepZ = false;
        }
        state.attackSwing = entity.getAttackAnim(partialTick); // 0..1 melee swing progress for attack-lunge poses
        // Manticore, both forms (wild monster + tameable pet, via the shared IMoCManticore contract): wing
        // beat, airborne wing/leg pose, the scorpion sting strike, and the big cat jaw drop it reuses.
        if (entity instanceof drzhark.mocreatures.entity.IMoCManticore manticore) {
            state.manticoreFlapping = manticore.isWingFlapping();
            state.manticoreAirborne = manticore.isAirborne();
            state.manticoreStinging = manticore.isStingStriking();
            state.bigcatJawOpen = manticore.getJawOpen();
        } else {
            state.manticoreFlapping = false;
            state.manticoreAirborne = false;
            state.manticoreStinging = false;
        }
        state.wingOpen = easeWingOpen(entity, state.flying || state.manticoreAirborne, partialTick);
        state.birdWingFlap = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityBird flapper
                ? flapper.getWingFlap(partialTick) : 0.0F;
    }

    /** Legacy MoCRenderKitty drew the mood face only within 12 blocks. */
    private static final double EMOTICON_RANGE_SQ = 12.0D * 12.0D;
    /** The floating mood face: half a block across, its centre this far above the top of the kitty. */
    private static final float EMOTICON_HALF = 0.25F;
    private static final float EMOTICON_LIFT = 0.45F;

    /**
     * The mood face a tamed kitty shows above its head (legacy MoCRenderKitty, {@code displayPetIcons}): hungry,
     * playing or eating. Asleep it shows rising "z"s instead (MoCEntityKitty.tick), and a carried kitty none.
     * Legacy also required a pet name and drew nothing on a kitty in its bed; the port shows it for every tamed
     * kitty, in a bed too.
     */
    private static Identifier kittyEmoticon(drzhark.mocreatures.entity.passive.MoCEntityKitty kitty,
            MoCEntityRenderState state) {
        if (!drzhark.mocreatures.config.MoCConfig.get().displayPetIcons || !kitty.getIsTamed()
                || kitty.isBeingCarried() || state.distanceToCameraSq > EMOTICON_RANGE_SQ) {
            return null;
        }
        int mood = kitty.getKittyState();
        return mood == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_HUNGRY
                || mood == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_PLAYING
                || mood == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_EATING
                ? kitty.getEmoticonTexture() : null;
    }

    @Override
    public void submit(MoCEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.kittyEmoticon != null) {
            // A camera-facing quad above the head, full-bright so it reads at night (DragonFireballRenderer's quad)
            poseStack.pushPose();
            poseStack.translate(0.0F, state.boundingBoxHeight + EMOTICON_LIFT, 0.0F);
            poseStack.mulPose(camera.orientation);
            collector.submitCustomGeometry(poseStack,
                    net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(state.kittyEmoticon),
                    (pose, buffer) -> {
                        emoticonVertex(buffer, pose, -EMOTICON_HALF, -EMOTICON_HALF, 0.0F, 1.0F);
                        emoticonVertex(buffer, pose, EMOTICON_HALF, -EMOTICON_HALF, 1.0F, 1.0F);
                        emoticonVertex(buffer, pose, EMOTICON_HALF, EMOTICON_HALF, 1.0F, 0.0F);
                        emoticonVertex(buffer, pose, -EMOTICON_HALF, EMOTICON_HALF, 0.0F, 0.0F);
                    });
            poseStack.popPose();
        }
        if (state.kittySleepZ) {
            submitSleepZ(state, poseStack, collector, camera);
        }
    }

    private static final Identifier SLEEP_Z =
            Identifier.fromNamespaceAndPath(drzhark.mocreatures.MoCreatures.MOD_ID, "textures/misc/sleep_z.png");
    /** One "z" rises for this many ticks; a second one follows half a cycle behind. */
    private static final float SLEEP_Z_CYCLE = 60.0F;

    /**
     * A sleeping kitty's "z z": two small pale-blue letters taking turns to float up from its head, each rising
     * about two-thirds of a block over 3 s, swaying a little, growing as it rises and fading out over its second
     * half. Timed by the kitty's own age, so every sleeper breathes at its own pace and the letters stop while
     * the game is paused.
     */
    private static void submitSleepZ(MoCEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        for (int i = 0; i < 2; i++) {
            float t = ((state.ageInTicks + i * SLEEP_Z_CYCLE / 2.0F) % SLEEP_Z_CYCLE) / SLEEP_Z_CYCLE;
            float alpha = t < 0.1F ? t / 0.1F : t < 0.5F ? 1.0F : 1.0F - (t - 0.5F) / 0.5F;
            float half = 0.05F * (1.0F + 0.7F * t);
            float sway = net.minecraft.util.Mth.sin(t * (float) (Math.PI * 2.0D)) * 0.06F;
            int color = net.minecraft.util.ARGB.colorFromFloat(alpha, 0.86F, 0.92F, 1.0F);
            poseStack.pushPose();
            poseStack.translate(state.kittyHeadX, state.kittyHeadY + 0.15F + t * 0.65F, state.kittyHeadZ);
            poseStack.mulPose(camera.orientation);
            poseStack.translate(sway + 0.08F * i, 0.0F, 0.0F);
            collector.submitCustomGeometry(poseStack,
                    net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SLEEP_Z),
                    (pose, buffer) -> {
                        sleepVertex(buffer, pose, -half, -half, 0.0F, 1.0F, color);
                        sleepVertex(buffer, pose, half, -half, 1.0F, 1.0F, color);
                        sleepVertex(buffer, pose, half, half, 1.0F, 0.0F, color);
                        sleepVertex(buffer, pose, -half, half, 0.0F, 0.0F, color);
                    });
            poseStack.popPose();
        }
    }

    private static void sleepVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer,
            com.mojang.blaze3d.vertex.PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void emoticonVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer,
            com.mojang.blaze3d.vertex.PoseStack.Pose pose, float x, float y, float u, float v) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** Ticks for spread wings to fold down once a flyer lands (0.6 s), and to spread again on take-off. */
    private static final float WING_FOLD_TICKS = 12.0F;
    private static final float WING_OPEN_TICKS = 4.0F;
    /** Each creature's wing spread and the entity-clock time it was last eased to, per renderer (weak keys). */
    private final java.util.Map<net.minecraft.world.entity.Entity, float[]> wingSpread = new java.util.WeakHashMap<>();

    /**
     * Eases a winged creature's wings toward spread (airborne) or folded (grounded) at a fixed rate, on the entity's
     * own clock ({@code tickCount + partialTick}) so the speed is the same at any frame rate and the wings freeze while
     * the game is paused. The model blends its grounded and flight wing poses by the smoothstepped result, so a landing
     * flyer folds its wings over {@link #WING_FOLD_TICKS} instead of snapping them shut in one frame.
     */
    private float easeWingOpen(T entity, boolean airborne, float partialTick) {
        float now = entity.tickCount + partialTick;
        float target = airborne ? 1.0F : 0.0F;
        float[] spread = this.wingSpread.computeIfAbsent(entity, e -> new float[] {target, now});
        float elapsed = net.minecraft.util.Mth.clamp(now - spread[1], 0.0F, 5.0F);
        spread[1] = now;
        if (spread[0] < target) {
            spread[0] = Math.min(target, spread[0] + elapsed / WING_OPEN_TICKS);
        } else if (spread[0] > target) {
            spread[0] = Math.max(target, spread[0] - elapsed / WING_FOLD_TICKS);
        }
        float p = spread[0];
        return p * p * (3.0F - 2.0F * p);
    }

    @Override
    protected void setupRotations(MoCEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack,
            float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        // A rearing horse pitches its whole body back onto its hind legs (the front-leg lift + head-up pose
        // is applied in the model; this is the global body tilt). The tilt hinges at the hind hooves (model
        // z = +11 px, the rear legs' pivot line) rather than at the feet's centre, which sank the hind hooves
        // almost half a block into the ground as the front came up. MoCModelHorse.REAR_PITCH must stay 30.
        if (state.horseRearing) {
            poseStack.translate(0.0F, 0.0F, 0.6875F);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30.0F));
            poseStack.translate(0.0F, 0.0F, -0.6875F);
        }
        // Crocodile death-roll: the croc barrel-rolls about its long (forward) axis while drowning its prey
        // underwater (legacy spinInt spin). Continuous roll driven by the animation timer.
        if (state.crocRolling) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((state.ageInTicks * 24.0F) % 360.0F));
        }
        // Kitty carry poses: picked up by a lead the cat dangles upside down (legacy
        // MoCRenderKitty.upsideDown:131-134, state 14); picked up empty-handed it lies sideways across
        // the carrier's shoulder (legacy MoCRenderKitty.onMaBack:85-93, state 15). Guarded on riding —
        // which covers isBeingCarried (see extractRenderState) — so a stale saved state can never flip
        // a cat standing on the ground. Both poses run in the BODY frame: extractRenderState overrides
        // state.bodyRot (the rotation super just applied) with the carrier's interpolated body yaw.
        //
        // FRAME CONJUGATION: legacy applied these from preRenderCallback, which 1.12.2's
        // RenderLivingBase.prepareScale calls AFTER GlStateManager.scale(-1,-1,1); this hook runs BEFORE
        // the equivalent poseStack.scale(-1,-1,1) (mc262-ref LivingEntityRenderer.java:87-90). Moving a
        // transform across S = diag(-1,-1,1) conjugates it: rotations about Z are unchanged (flipping X
        // and Y IS a 180-degree Z rotation, which commutes with any Z rotation), but a translation
        // (x, y, z) becomes (-x, -y, z). So each legacy translate below keeps its Z and negates X and Y.
        if (state.riding
                && state.kittyState == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_HELD_UPSIDE_DOWN) {
            // Legacy upsideDown: rotate 180 about -Z (== +Z), then translate(-0.35, 0, -0.55) post-flip —
            // 0.35 to the side and (conjugated: z keeps its sign, so -0.55 local = +0.55 through the
            // Y-rotation) 0.55 in FRONT of the player, which is what read as "not centered with my body".
            // Both components are gone from here: the forward push is simply dropped (the cat hangs
            // centered on the body's front-back axis), and the side displacement moved to
            // extractRenderState's passengerOffset, where the carrier's main hand is known. Only the
            // upside-down flip remains, spinning the cat in place about its own hang point.
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        }
        if (state.riding
                && state.kittyState == drzhark.mocreatures.entity.passive.MoCEntityKitty.STATE_ON_SHOULDER) {
            // Legacy onMaBack: rotate 90 about -Z, then translate(0.1, 0.2, -0.2) post-flip — conjugated
            // to (-0.1, -0.2, z). Legacy's -0.2 Z pushed the shoulder cat 0.2 in front of the body, for
            // the same reason as state 14 above; with the pose now locked to the body frame the shoulder
            // lies ON the body's front-back axis, so the forward component is zeroed and only the
            // sideways/height nudge (0.2 onto the off-shoulder, 0.1 up) survives.
            poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(90.0F));
            poseStack.translate(-0.1F, -0.2F, 0.0F);
        }
    }

    @Override
    protected void scale(MoCEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        super.scale(state, poseStack);
        // Continuous age-based growth (legacy getSizeFactor): a hatchling starts at ~0.5x and grows to full
        // size at age 100; adults render at full scale regardless.
        float f = (state.adult ? 1.0F : 0.5F + 0.5F * Math.min(state.moCAge, 100) / 100.0F) * state.sizeFactor;
        if (f != 1.0F) {
            poseStack.scale(f, f, f);
        }
    }

    /**
     * A creature you are carrying sits on your own head, which fills the screen the moment you look up.
     * Legacy sidestepped this by dropping the carried pet's render offset to the carrier's feet while the
     * carrier was the local player ({@code MoCEntityBunny.getYOffset}); hiding it outright is the cleaner
     * equivalent. Other players still see the pet on your head, and third-person still shows it.
     */
    @Override
    public boolean shouldRender(T entity, net.minecraft.client.renderer.culling.Frustum frustum,
            double camX, double camY, double camZ) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        // The carry system never makes the pet a passenger (a player cannot be a vehicle on a 26.2
        // server — see MoCAnimal.CARRIER), so this must ask the carry state, not getVehicle().
        if (entity instanceof drzhark.mocreatures.entity.MoCAnimal carried && carried.isBeingCarried()
                && carried.getCarrier() == minecraft.getCameraEntity()
                && minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }
        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public Identifier getTextureLocation(MoCEntityRenderState state) {
        return state.texture;
    }
}
