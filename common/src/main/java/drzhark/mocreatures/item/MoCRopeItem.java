package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Rope — leashes a tamed Mo'Creatures creature to the player so it follows, like a lead (the legacy
 * rope was a custom follow-leash; this uses vanilla leashing as a faithful, robust equivalent).
 */
public class MoCRopeItem extends Item {

    public MoCRopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Mob mob && target instanceof IMoCEntity moc && moc.getIsTamed()) {
            if (!player.level().isClientSide()) {
                // Only attach (and consume a rope) if not already leashed to this player, so a
                // no-op click doesn't waste a rope. Legacy consumed one rope per successful attach.
                if (!(mob.isLeashed() && mob.getLeashHolder() == player)) {
                    mob.setLeashedTo(player, true);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
