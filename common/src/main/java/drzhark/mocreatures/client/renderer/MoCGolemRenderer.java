package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelGolem;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.monster.MoCEntityGolem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Dedicated golem renderer (Phase 3 visuals). It keeps the generic {@link MoCMobRenderer} behaviour
 * (the golemt.png head/chest + animated cube poses) and adds two layers:
 *
 * <ul>
 *   <li>{@link GolemBlockCubeLayer} — draws each present anatomical cube as its <em>real absorbed
 *       block</em> (dirt looks like dirt, gold like gold), positioned at the cube's own animated
 *       {@link ModelPart} transform. The golemt.png cube geometry is hidden by the model so only the
 *       real blocks show.</li>
 *   <li>{@link GolemAuraLayer} — a full-bright glowing shell (golemeffect1-4.png) over the golem's
 *       core once it activates, brightening as it grows more powerful (legacy {@code getEffectTexture}).</li>
 * </ul>
 */
public class MoCGolemRenderer extends MoCMobRenderer<MoCEntityGolem> {

    public MoCGolemRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.GOLEM, MoCModelGolem::new, 0.75F);
        this.addLayer(new GolemBlockCubeLayer(this));
        this.addLayer(new GolemAuraLayer(this));
    }

    @Override
    protected void setupRotations(MoCEntityRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        // Asymmetric lean (legacy adjustTilt / tiltOffset): a golem missing leg cubes on one side staggers
        // toward its weaker side. tiltOffset = (missing left-leg cubes) - (missing right-leg cubes); the
        // whole golem rotates about Z by tiltOffset*10 degrees. Cube slots 15-17 = left leg, 18-20 = right.
        int mask = state.golemCubeMask;
        int missingLeft = 0;
        int missingRight = 0;
        for (int i = 15; i <= 17; i++) {
            if ((mask & (1 << i)) == 0) {
                missingLeft++;
            }
        }
        for (int i = 18; i <= 20; i++) {
            if ((mask & (1 << i)) == 0) {
                missingRight++;
            }
        }
        int tilt = missingLeft - missingRight;
        if (tilt != 0) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tilt * 10.0F));
        }
    }

    @Override
    public void extractRenderState(MoCEntityGolem golem, MoCEntityRenderState state, float partialTick) {
        super.extractRenderState(golem, state, partialTick);
        // Decode the synched per-cube block ids into moving-block render states for the cube layer.
        MovingBlockRenderState[] blocks = state.golemCubeBlocks;
        if (blocks == null || blocks.length != MoCEntityGolem.CUBE_COUNT) {
            blocks = new MovingBlockRenderState[MoCEntityGolem.CUBE_COUNT];
            state.golemCubeBlocks = blocks;
        }
        String sync = golem.getCubeBlocksSync();
        String[] ids = sync.isEmpty() ? new String[0] : sync.split(",");
        Level level = golem.level();
        BlockPos pos = golem.blockPosition();
        for (int i = 0; i < blocks.length; i++) {
            int id = 0;
            if (i < ids.length) {
                try {
                    id = Integer.parseInt(ids[i]);
                } catch (NumberFormatException ignored) {
                    id = 0;
                }
            }
            if (id == 0) {
                blocks[i] = null;
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.byId(id);
            MovingBlockRenderState mb = new MovingBlockRenderState();
            mb.blockState = block.defaultBlockState();
            mb.blockPos = pos;
            mb.randomSeedPos = pos;
            if (level instanceof ClientLevel clientLevel) {
                mb.biome = clientLevel.getBiome(pos);
                mb.cardinalLighting = clientLevel.cardinalLighting();
                mb.lightEngine = clientLevel.getLightEngine();
            }
            blocks[i] = mb;
        }
    }

    // ----------------------------------------------------------------- per-block cube layer

    /** Draws each present cube as its real absorbed block at that cube's animated {@link ModelPart} pose. */
    private static final class GolemBlockCubeLayer
            extends RenderLayer<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> {

        private GolemBlockCubeLayer(RenderLayerParent<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                MoCEntityRenderState state, float yRot, float xRot) {
            MovingBlockRenderState[] blocks = state.golemCubeBlocks;
            if (blocks == null || !(getParentModel() instanceof MoCModelGolem golemModel)) {
                return;
            }
            ModelPart[] cubes = golemModel.cubeParts();
            float[][] centers = MoCModelGolem.CUBE_BOX_CENTERS;
            for (int i = 0; i < cubes.length && i < blocks.length; i++) {
                MovingBlockRenderState mb = blocks[i];
                if (mb == null || mb.blockState == null) {
                    continue;
                }
                poseStack.pushPose();
                // Move into the cube's own animated frame (pivot + rotation); translateAndRotate divides
                // the pixel offsets by 16, so we are now in block space.
                cubes[i].translateAndRotate(poseStack);
                float[] c = centers[i];
                poseStack.translate(c[0] / 16.0F, c[1] / 16.0F, c[2] / 16.0F); // to the 8px box centre
                // An 8px golem cube is half a block; flip X/Y into block-model space (the entity model
                // space is Y-down) then centre the unit block on the origin.
                poseStack.scale(-0.5F, -0.5F, 0.5F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                collector.submitMovingBlock(poseStack, mb, state.outlineColor);
                poseStack.popPose();
            }
        }
    }

    // ----------------------------------------------------------------- glowing aura layer

    /** A full-bright glowing shell (golemeffect1-4) over the golem's core, keyed to its power state. */
    private static final class GolemAuraLayer
            extends RenderLayer<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> {

        private GolemAuraLayer(RenderLayerParent<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                MoCEntityRenderState state, float yRot, float xRot) {
            int s = state.golemState;
            if (s < 1) {
                return; // dormant / just-spawned golems have no aura
            }
            // Shimmer through golemeffect1..4 over time so the aura animates (legacy getEffectTexture cycled
            // the frames) rather than sitting on a single static frame.
            int idx = 1 + (((int) (state.ageInTicks / 3.0F)) % 4);
            Identifier tex = Identifier.fromNamespaceAndPath("mocreatures", "textures/models/golemeffect" + idx + ".png");
            // Re-render the model with the effect skin at full brightness (0xF000F0) so it reads as a glow.
            renderColoredCutoutModel(getParentModel(), tex, poseStack, collector,
                    0x00F000F0, state, 0xFFFFFFFF, 0);
        }
    }
}
