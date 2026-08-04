package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.projectile.MoCThrownDuckEgg;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The duck egg item — an ordinary egg you can cook/craft with AND throw. Right-click hurls a
 * {@link MoCThrownDuckEgg} (mirroring the vanilla egg): on impact it has a ~1/8 chance to hatch a duckling.
 * It stacks and is accepted by the duck-egg variants of the vanilla egg recipes (cake, pumpkin pie).
 */
public class MoCDuckEggItem extends Item {

    public MoCDuckEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            MoCThrownDuckEgg thrown = new MoCThrownDuckEgg(serverLevel, player);
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            serverLevel.addFreshEntity(thrown);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
