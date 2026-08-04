package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.entity.projectile.MoCEntityThrowableRock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders a {@link MoCEntityThrowableRock} as the block it is carrying — the vacuumed cube visibly
 * tumbling back toward the golem. This is the {@code submitMovingBlock} route used by the vanilla
 * {@link net.minecraft.client.renderer.entity.FallingBlockRenderer}: the carried {@link BlockState}
 * is packed into a {@link FallingBlockRenderState}'s {@code movingBlockRenderState} at extract time and
 * drawn as a real block model at submit time. Mirrors the legacy {@code MoCRenderTRock}, which spun the
 * block via {@code renderBlockAsItem}.
 */
public class MoCThrowableRockRenderer extends EntityRenderer<MoCEntityThrowableRock, FallingBlockRenderState> {

    public MoCThrowableRockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public FallingBlockRenderState createRenderState() {
        return new FallingBlockRenderState();
    }

    @Override
    public void extractRenderState(MoCEntityThrowableRock entity, FallingBlockRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BlockState carried = entity.getCarried();
        if (carried == null || carried.isAir()) {
            carried = Blocks.COBBLESTONE.defaultBlockState();
        }
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.movingBlockRenderState.randomSeedPos = pos;
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = carried;
        Level level = entity.level();
        if (level instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(FallingBlockRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        BlockState blockState = state.movingBlockRenderState.blockState;
        if (blockState != null && blockState.getRenderShape() == RenderShape.MODEL) {
            poseStack.pushPose();
            // Slowly tumble the block in flight (legacy MoCRenderTRock spun it about the Y axis).
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(state.ageInTicks * 6.0F));
            // Centre the block on the entity origin (same offset the falling-block renderer uses).
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            collector.submitMovingBlock(poseStack, state.movingBlockRenderState, state.outlineColor);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, cameraState);
    }
}
