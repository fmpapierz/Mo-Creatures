package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Creaturepedia — right-click a Mo'Creatures creature to open its full-screen dossier
 * ({@code MoCCreaturePediaScreen}, the 26.2 rewrite of the legacy {@code MoCGUICreaturePedia}).
 *
 * <p>On the client this opens the info GUI, which reads the entity's fields directly (species,
 * tamed/owner, variant, age, health). On the server it prints the same information to chat as a
 * fallback so the interaction is still legible without the GUI.
 */
public class MoCCreaturePediaItem extends Item {

    public MoCCreaturePediaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof IMoCEntity moc) {
            if (player.level().isClientSide()) {
                // Client has the entity — open the full-screen Creaturepedia GUI.
                final int entityId = target.getId();
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT,
                        () -> () -> drzhark.mocreatures.client.MoCClientHelper.openCreaturePedia(entityId));
            } else {
                // Server-side chat fallback.
                String name = target.getType().getDescription().getString();
                String state = moc.getIsTamed()
                        ? "tamed (owner: " + (moc.getOwnerName().isEmpty() ? "unknown" : moc.getOwnerName()) + ")"
                        : "wild";
                player.sendSystemMessage(Component.literal(
                        "§6" + name + "§r — " + state + ", variant " + moc.getTypeMoC()
                        + ", " + (moc.getIsAdult() ? "adult" : "young")));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
