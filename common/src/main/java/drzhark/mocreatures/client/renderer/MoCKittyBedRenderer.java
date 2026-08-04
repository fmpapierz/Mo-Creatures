package drzhark.mocreatures.client.renderer;

import java.util.function.Function;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityKittyBed;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.DyeColor;

/**
 * Renderer for the kitty bed. Behaves exactly like {@link MoCMobRenderer} but tints the single
 * {@code fullkittybed.png} texture by the bed's dye colour (legacy {@code MoCRenderKittyBed} applied the
 * sheep {@code fleeceColorTable} colour). Colour 0 is the plain, untinted bed; 1..16 map to a
 * {@link DyeColor} (index = DyeColor id + 1).
 */
public class MoCKittyBedRenderer extends MoCMobRenderer<MoCEntityKittyBed> {

    public MoCKittyBedRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
            Function<ModelPart, ? extends EntityModel<MoCEntityRenderState>> modelFactory, float shadowRadius) {
        super(context, layer, modelFactory, shadowRadius);
    }

    @Override
    protected int getModelTint(MoCEntityRenderState state) {
        int colour = state.kittyBedColour;
        if (colour >= 1 && colour <= 16) {
            return 0xFF000000 | (DyeColor.byId(colour - 1).getTextureDiffuseColor() & 0xFFFFFF);
        }
        return super.getModelTint(state);
    }
}
