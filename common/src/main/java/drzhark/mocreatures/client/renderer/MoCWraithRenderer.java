package drzhark.mocreatures.client.renderer;

import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelWraith;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.monster.MoCEntityWraith;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Wraith renderer: a ghostly, semi-transparent grey apparition (legacy {@code glColor4f(0.8,0.8,0.8,0.6)}).
 * The model uses a translucent render type so the multiplied alpha shows through.
 */
public class MoCWraithRenderer extends MoCMobRenderer<MoCEntityWraith> {

    public MoCWraithRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.WRAITH, MoCModelWraith::new, 0.75F);
    }

    @Override
    protected int getModelTint(MoCEntityRenderState state) {
        return 0x99CCCCCC; // A=0.60, RGB=0.8 grey
    }
}
