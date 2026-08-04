package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.passive.MoCEntityKitty;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Wool Ball — a kitty toy. Offering it to a tamed kitty drops a wool ball on the ground that the kitty
 * then chases and bats around (see {@code MoCKittyPlayGoal}).
 */
public class MoCWoolBallItem extends Item {

    public MoCWoolBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof MoCEntityKitty kitty && kitty.getIsTamed()) {
            if (player.level() instanceof ServerLevel level) {
                ItemEntity ball = new ItemEntity(level, kitty.getX(), kitty.getY() + 0.2D, kitty.getZ(),
                        new ItemStack(this, 1));
                ball.setPickUpDelay(60);
                ball.setDeltaMovement((level.getRandom().nextDouble() - 0.5D) * 0.2D, 0.1D,
                        (level.getRandom().nextDouble() - 0.5D) * 0.2D);
                level.addFreshEntity(ball);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        kitty.getX(), kitty.getY() + 0.4D, kitty.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.0D);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
