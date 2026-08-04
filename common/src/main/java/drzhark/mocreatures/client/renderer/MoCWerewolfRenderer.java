package drzhark.mocreatures.client.renderer;

import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelWereHuman;
import drzhark.mocreatures.client.model.MoCModelWerewolf;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.monster.MoCEntityWerewolf;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Dedicated werewolf renderer that swaps between two distinct models by form: the big 64x128 beast
 * model ({@link MoCModelWerewolf}) at night, and a 64x32 biped ({@link MoCModelWereHuman}) by day.
 * The matching {@code were*} texture is chosen by the entity's {@code getTexture()}. This mirrors the
 * legacy {@code MoCRenderWerewolf}, which carried both a wolf and a human model.
 */
public class MoCWerewolfRenderer extends MoCMobRenderer<MoCEntityWerewolf> {

    private final EntityModel<MoCEntityRenderState> wolfModel;
    private final EntityModel<MoCEntityRenderState> humanModel;

    public MoCWerewolfRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.WEREWOLF, MoCModelWerewolf::new, 0.45F);
        this.wolfModel = this.model; // captured from the super constructor (the beast model)
        this.humanModel = new MoCModelWereHuman(context.bakeLayer(MoCModelLayers.WEREHUMAN));
    }

    @Override
    public void extractRenderState(MoCEntityWerewolf entity, MoCEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.humanForm = entity.isHumanForm();
    }

    @Override
    public void submit(MoCEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState cameraState) {
        // Pick the model matching the current form before the base renderer animates + submits it.
        this.model = state.humanForm ? this.humanModel : this.wolfModel;
        super.submit(state, poseStack, collector, cameraState);
    }
}
