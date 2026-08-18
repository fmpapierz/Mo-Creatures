package drzhark.mocreatures.fabric.mixin;

import drzhark.mocreatures.client.MoCLeadCarriers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stills a player's MAIN-HAND arm while they lead-carry a kitty ({@code MoCEntityKitty} state 14): the
 * arm hangs straight down, eased slightly outward, as if holding the lead at their side — instead of
 * doing the walk swing (and idle bob) straight through the cat dangling at that hip. Whether a given
 * rendered player is carrying comes from {@link MoCLeadCarriers}, the client-side registry the carried
 * kitty's own client tick refreshes; {@code AvatarRenderState.id} carries the rendered player's entity
 * id (mc262-ref AvatarRenderer.java:188) to look them up, and {@code mainArm} picks the arm.
 *
 * <p>Injected at TAIL of {@code HumanoidModel.setupAnim(HumanoidRenderState)} rather than of
 * {@code PlayerModel.setupAnim}, and gated on the state being an {@code AvatarRenderState} (only players
 * use one). PlayerModel calls {@code super.setupAnim} LAST (mc262-ref PlayerModel.java:110-124), so this
 * TAIL still runs after every arm animation — walk swing, item pose, attack swing, idle bob
 * (HumanoidModel.java:216-341) — and, unlike a PlayerModel injection, it also catches the ARMOR models:
 * the armor layer poses its own HumanoidModel with the same AvatarRenderState
 * (HumanoidArmorLayer.renderArmorPiece -> EquipmentLayerRenderer.renderLayers -> submitModel(model, state)),
 * so a chestplate sleeve freezes with the arm instead of swinging through it. First-person hands are
 * untouched: AvatarRenderer.renderHand (:247-258) resets the arm pose directly and never calls
 * setupAnim. The NeoForge counterpart is {@code drzhark.mocreatures.neoforge.mixin.HumanoidModelMixin}.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL"))
    private void mocreatures$stillLeadCarryArm(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatar)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !MoCLeadCarriers.isLeadCarrying(avatar.id, minecraft.level.getGameTime())) {
            return;
        }
        boolean rightHanded = avatar.mainArm == HumanoidArm.RIGHT;
        ModelPart arm = rightHanded ? this.rightArm : this.leftArm;
        arm.xRot = 0.0F; // hanging straight down: no walk swing, no bob
        arm.yRot = 0.0F;
        // Positive zRot tips the RIGHT arm away from the body (renderHand's natural splay is +0.1);
        // mirror for a left-handed player. 0.25 rad holds the hand just over the cat at the hip.
        arm.zRot = rightHanded ? 0.25F : -0.25F;
    }
}
