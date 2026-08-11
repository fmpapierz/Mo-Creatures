package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelMiniGolem;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.monster.MoCEntityMiniGolem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mini golem renderer. It is the generic {@link MoCMobRenderer} plus one extra layer: the block the golem
 * has ripped out of the ground and is heaving over its head.
 *
 * <p>Legacy drew that block as a separate {@code MoCEntityThrowableRock} entity in "behaviour 1", which the
 * golem teleported to {@code posY + 1.0} every tick ({@code MoCEntityMiniGolem.attackWithTRock}:131-133) and
 * which {@code MoCRenderTRock} then drew as a full 1x1x1 world block. The port carries the block as synched
 * state on the golem itself, so {@link HeldRockLayer} can put it in exactly the same place with no second
 * entity to keep in sync (or to orphan in mid-air when the golem loses its target mid-hold).</p>
 */
public class MoCMiniGolemRenderer extends MoCMobRenderer<MoCEntityMiniGolem> {

    public MoCMiniGolemRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.MINI_GOLEM, MoCModelMiniGolem::new, 0.5F);
        this.addLayer(new HeldRockLayer(this));
    }

    @Override
    public void extractRenderState(MoCEntityMiniGolem golem, MoCEntityRenderState state, float partialTick) {
        super.extractRenderState(golem, state, partialTick);
        state.miniGolemAngry = golem.getIsAngry();
        state.miniGolemHasRock = golem.getHasRock();

        BlockState held = golem.getHeldRock();
        if (!state.miniGolemHasRock || held == null || held.isAir()) {
            state.miniGolemHeldBlock = null;
            return;
        }
        // Same packing the golem's cube layer and the falling-block renderer use: a MovingBlockRenderState
        // carrying the block plus the light/biome context of the golem's own position.
        MovingBlockRenderState moving = new MovingBlockRenderState();
        BlockPos pos = golem.blockPosition();
        moving.blockState = held;
        moving.blockPos = pos;
        moving.randomSeedPos = pos;
        Level level = golem.level();
        if (level instanceof ClientLevel clientLevel) {
            moving.biome = clientLevel.getBiome(pos);
            moving.cardinalLighting = clientLevel.cardinalLighting();
            moving.lightEngine = clientLevel.getLightEngine();
        }
        state.miniGolemHeldBlock = moving;
    }

    // ----------------------------------------------------------------- held rock layer

    /** Draws the hoisted block as a real full-size world block, gripped between the raised arms. */
    private static final class HeldRockLayer
            extends RenderLayer<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> {

        /**
         * Height (in blocks, above the golem's feet) of the CENTRE of the held block. Legacy pinned the
         * rock entity's position — the bottom of its 1x1x1 box — to {@code posY + 1.0}, so the block spans
         * 1.0 to 2.0 above the feet and its centre sits at 1.5. That is right where the arms end up once
         * they swing overhead, so the golem reads as holding the boulder rather than balancing it.
         */
        private static final float HELD_ROCK_CENTRE_Y = 1.5F;

        private HeldRockLayer(RenderLayerParent<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                MoCEntityRenderState state, float yRot, float xRot) {
            MovingBlockRenderState moving = state.miniGolemHeldBlock;
            if (moving == null || moving.blockState == null
                    || moving.blockState.getRenderShape() != RenderShape.MODEL) {
                return;
            }

            poseStack.pushPose();
            // The layer's frame is the model frame: Y points DOWN and the origin sits
            // -MODEL_Y_OFFSET (1.501) blocks above the golem's feet. Convert the wanted world height.
            poseStack.translate(0.0F, -EntityModel.MODEL_Y_OFFSET - HELD_ROCK_CENTRE_Y, 0.0F);
            // Flip back out of the model's inverted axes into upright block-model space, then centre the
            // unit block on the origin (the same two steps the golem's per-block cube layer performs).
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            collector.submitMovingBlock(poseStack, moving, state.outlineColor);
            poseStack.popPose();
        }
    }
}
