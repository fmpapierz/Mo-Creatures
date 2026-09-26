package drzhark.mocreatures.client.renderer;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.model.MoCModelKittyBed;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityKittyBed;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Renderer for the kitty bed, a port of the legacy {@code MoCRenderKittyBed}. The wooden frame renders
 * untinted through the generic {@link MoCMobRenderer} path; the wool sheet is a second model drawn by
 * {@link SheetLayer} in the bed's wool colour, exactly as legacy's {@code LayerMoCKittyBed} drew
 * {@code MoCModelKittyBed2} (12.0.5 {@code MoCRenderKittyBed.java}:44-66).
 *
 * <p>The first port tinted the WHOLE model instead ({@code getModelTint}) and had no sheet at all, so a
 * placed bed showed a colour-washed wooden frame with no wool — and no floor, since the texture leaves the
 * floor plank's top face empty for the sheet to cover. Colour 0 (the plain {@code kittybed} item) and 1..16
 * ({@code DyeColor} id + 1, the coloured items) are stamped by {@code MoCFurnitureItem}, synched by
 * {@code MoCEntityKittyBed.SHEET_COLOUR} and read here from {@code state.kittyBedColour}.</p>
 */
public class MoCKittyBedRenderer extends MoCMobRenderer<MoCEntityKittyBed> {

    /**
     * Legacy {@code MoCRenderKittyBed.fleeceColorTable} (12.0.5 {@code MoCRenderKittyBed.java}:20-22), in wool
     * metadata order — which is {@code DyeColor} id order, so row {@code colour - 1}. These are Mo'Creatures'
     * own sheet colours, not 26.2's {@code DyeColor.getTextureDiffuseColor()} (which the first port used).
     */
    private static final float[][] FLEECE_COLOR_TABLE = {
            {1.0F, 1.0F, 1.0F}, {0.95F, 0.7F, 0.2F}, {0.9F, 0.5F, 0.85F}, {0.6F, 0.7F, 0.95F},
            {0.9F, 0.9F, 0.2F}, {0.5F, 0.8F, 0.1F}, {0.95F, 0.7F, 0.8F}, {0.3F, 0.3F, 0.3F},
            {0.6F, 0.6F, 0.6F}, {0.3F, 0.6F, 0.7F}, {0.7F, 0.4F, 0.9F}, {0.2F, 0.4F, 0.8F},
            {0.5F, 0.4F, 0.3F}, {0.4F, 0.5F, 0.2F}, {0.8F, 0.3F, 0.3F}, {0.1F, 0.1F, 0.1F}};

    /** Legacy {@code f8 = 0.35F}: the sheet was drawn with {@code glColor3f(0.35 * fleece)} (12.0.5 :54-56). */
    private static final float LEGACY_SHEET_SHADE = 0.35F;

    /**
     * The light an up-facing face received under 1.12's fixed-function entity lighting:
     * {@code RenderHelper.enableStandardItemLighting} set a 0.4 ambient plus two 0.6 diffuse lights along
     * (0.2, 1, -0.7) and (-0.2, 1, 0.7), so a top face got {@code 0.4 + 0.6 * 2 / sqrt(1.53) = 1.370}. GL
     * multiplied the glColor by that sum and only then clamped, so legacy's sheet top showed at
     * {@code 0.35 * 1.370 = 0.48} of the wool colour. 26.2's entity shader uses the same lights and
     * weights but clamps the light sum to 1.0 BEFORE the tint multiply (mc262-ref
     * {@code shaders/include/light.glsl}:17), so passing the bare 0.35 would draw every sheet ~27% darker
     * than legacy did. Folding the sum into the tint reproduces legacy's on-screen top face exactly (the
     * narrow side strip ends up a little brighter than legacy's, which a single tint cannot avoid).
     */
    private static final float LEGACY_TOP_FACE_LIGHT =
            0.4F + 0.6F * 2.0F / (float) Math.sqrt(0.2D * 0.2D + 1.0D + 0.7D * 0.7D);

    /** Brightness applied to the fleece colour (about 0.48); raise toward 1.0 for vanilla-bright wool. */
    private static final float SHEET_SHADE = LEGACY_SHEET_SHADE * LEGACY_TOP_FACE_LIGHT;

    /**
     * The kitty bed's render state: the shared {@link MoCEntityRenderState} plus the bed's synched milk / pet
     * food flags, which the frame model needs to show or hide the fill (legacy {@code preRenderCallback} copied
     * {@code getHasMilk()}/{@code getHasFood()} onto the model, 12.0.5 :31-37). Kept local to this renderer so
     * the shared state class does not grow two more bed-only fields.
     */
    public static class KittyBedRenderState extends MoCEntityRenderState {
        public boolean hasMilk;
        public boolean hasFood;
    }

    public MoCKittyBedRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
            Function<ModelPart, ? extends EntityModel<MoCEntityRenderState>> modelFactory, float shadowRadius) {
        super(context, layer, modelFactory, shadowRadius);
        // The sheet is baked straight from its layer definition: EntityModelSet.bakeLayer is nothing more than
        // roots.get(id).bakeRoot() (mc262-ref EntityModelSet.java:18-25), so this needs no extra registered layer.
        this.addLayer(new SheetLayer(this,
                new MoCModelKittyBed.Sheet(MoCModelKittyBed.Sheet.createSheetLayer().bakeRoot())));
    }

    @Override
    public MoCEntityRenderState createRenderState() {
        return new KittyBedRenderState();
    }

    @Override
    public void extractRenderState(MoCEntityKittyBed bed, MoCEntityRenderState state, float partialTick) {
        super.extractRenderState(bed, state, partialTick);
        if (state instanceof KittyBedRenderState kittyBed) {
            kittyBed.hasMilk = bed.getHasMilk();
            kittyBed.hasFood = bed.getHasFood();
        }
    }

    /**
     * The sheet's ARGB tint for a bed colour. 1..16 select legacy fleece rows 0..15; the plain bed (0) — which
     * legacy never had, its colour 0 being white — renders as white, the colour it would have been in legacy.
     */
    static int sheetTint(int colour) {
        float[] rgb = FLEECE_COLOR_TABLE[colour >= 1 && colour <= 16 ? colour - 1 : 0];
        int r = Math.round(Math.min(1.0F, rgb[0] * SHEET_SHADE) * 255.0F);
        int g = Math.round(Math.min(1.0F, rgb[1] * SHEET_SHADE) * 255.0F);
        int b = Math.round(Math.min(1.0F, rgb[2] * SHEET_SHADE) * 255.0F);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * Draws {@link MoCModelKittyBed.Sheet} over the bed's own texture, tinted with the sheet colour — legacy
     * {@code LayerMoCKittyBed.doRenderLayer}, which set the colour and rendered {@code MoCModelKittyBed2} without
     * rebinding, i.e. on {@code fullkittybed.png}. Deliberately NOT {@code renderColoredCutoutModel}: that
     * helper hard-codes {@code RenderTypes.entityCutout}, which in 26.2 is the UNCULLED type, whereas legacy
     * culled back faces; the sheet model's own {@code entityCutoutCull} is used instead. The red hurt overlay
     * is passed through like legacy's {@code shouldCombineTextures() == true}.
     */
    private static final class SheetLayer
            extends RenderLayer<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> {

        private final MoCModelKittyBed.Sheet sheet;

        private SheetLayer(RenderLayerParent<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> parent,
                MoCModelKittyBed.Sheet sheet) {
            super(parent);
            this.sheet = sheet;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                MoCEntityRenderState state, float yRot, float xRot) {
            if (state.isInvisible) {
                return; // as vanilla's coloured layers do (RenderLayer.coloredCutoutModelCopyLayerRender)
            }
            collector.submitModel(this.sheet, state, poseStack, this.sheet.renderType(state.texture), lightCoords,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), sheetTint(state.kittyBedColour), null,
                    state.outlineColor, null);
        }
    }
}
