package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelHorse;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityHorse;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * Horse renderer. Horse armour is a full-texture swap, handled by {@link MoCEntityHorse#getTexture()}
 * (which appends the tier suffix — metal / gold / diamond / crystaline — to the base coat name). No
 * colour tint is applied here; the armoured coat renders at its true colour via the swapped texture.
 *
 * <p>Ghost horses (coat types 21/22) hover and bob gently, restoring the legacy {@code adjustHeight}
 * ghostly float.
 */
public class MoCHorseRenderer extends MoCMobRenderer<MoCEntityHorse> {

    public MoCHorseRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.HORSE, MoCModelHorse::new, 0.7F);
    }

    @Override
    protected void scale(MoCEntityRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.typeMoC == 21 || state.typeMoC == 22) {
            float bob = Mth.sin(state.ageInTicks * 0.1F) * 0.08F;
            poseStack.translate(0.0F, -0.15F + bob, 0.0F); // hover + gentle vertical bob
        }
    }
}
