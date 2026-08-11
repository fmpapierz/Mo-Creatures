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
        // Flyers flap/tuck while airborne instead of the walk gait. Wyvern uses a stable synched flag;
        // a winged horse flies via normal physics so !onGround() is stable enough (it gallops on the ground).
        // Insects, crickets and ducks flap their wings and switch to the in-flight leg pose whenever airborne
        // (legacy isOnAir()/getIsFlying()).
        state.flying = (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityWyvern wyvern && wyvern.isWyvernFlying())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse horse && horse.isFlyer() && !entity.onGround())
                || (entity instanceof drzhark.mocreatures.entity.MoCFlyingInsect && !entity.onGround())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityCricket && !entity.onGround())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityDuck && !entity.onGround())
                || (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityRoach && !entity.onGround());
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
        state.snakeHoodFlared = entity instanceof drzhark.mocreatures.entity.passive.MoCEntitySnake snake && snake.isHoodFlared();
        state.litterBoxUsed = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityLitterBox lb && lb.getUsed();
        state.kittyBedColour = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityKittyBed kb ? kb.getSheetColour() : 0;
        state.jellyfishGlowing = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityJellyFish jf && jf.isGlowingNow();
        state.jellyfishBeached = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityJellyFish jfb
                && !jfb.isInWater() && jfb.onGround();
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityWyvern wyv) {
            state.wyvernArmor = wyv.getArmorType();
            state.wyvernChested = wyv.getIsChested();
            state.wyvernSitting = wyv.isSitting();
        } else {
            state.wyvernArmor = 0;
            state.wyvernChested = false;
            state.wyvernSitting = false;
        }
        state.scorpionHasBabies = entity instanceof drzhark.mocreatures.entity.monster.MoCEntityScorpion scb && scb.getHasBabies();
        state.crocRolling = entity instanceof drzhark.mocreatures.entity.passive.MoCEntityCrocodile cr
                && cr.getHasCaughtPrey() && cr.isInWater();
        if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityKitty kitty) {
            state.kittySitting = kitty.isSitting();
            state.kittyState = kitty.getKittyState();
            state.kittyTamed = kitty.getIsTamed();
        } else {
            state.kittySitting = false;
            state.kittyState = 0;
            state.kittyTamed = false;
        }
        state.attackSwing = entity.getAttackAnim(partialTick); // 0..1 melee swing progress for attack-lunge poses
        // Manticore, both forms (wild monster + tameable pet, via the shared IMoCManticore contract): wing
        // beat, airborne wing/leg pose, the scorpion sting strike, and the big cat jaw drop it reuses.
        if (entity instanceof drzhark.mocreatures.entity.IMoCManticore manticore) {
            state.manticoreFlapping = manticore.isWingFlapping();
            state.manticoreAirborne = !entity.onGround();
            state.manticoreStinging = manticore.isStingStriking();
            state.bigcatJawOpen = manticore.getJawOpen();
        } else {
            state.manticoreFlapping = false;
            state.manticoreAirborne = false;
            state.manticoreStinging = false;
        }
    }

    @Override
    protected void setupRotations(MoCEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack,
            float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        // A rearing horse pitches its whole body back onto its hind legs (the front-leg lift + head-up pose
        // is applied in the model; this is the global body tilt).
        if (state.horseRearing) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30.0F));
        }
        // Crocodile death-roll: the croc barrel-rolls about its long (forward) axis while drowning its prey
        // underwater (legacy spinInt spin). Continuous roll driven by the animation timer.
        if (state.crocRolling) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((state.ageInTicks * 24.0F) % 360.0F));
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
        if (entity.getVehicle() != null && entity.getVehicle() == minecraft.getCameraEntity()
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
