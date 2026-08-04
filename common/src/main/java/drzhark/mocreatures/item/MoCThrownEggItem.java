package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.projectile.MoCThrownEgg;
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
 * The {@code mocegg} item — throwable again, exactly like the legacy {@code MoCItemEgg}: right-click hurls a
 * {@link MoCThrownEgg} projectile (mirroring a vanilla snowball) which, on impact, spawns a laid egg that
 * hatches into a random species' baby.
 */
public class MoCThrownEggItem extends Item {

    public MoCThrownEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            // Throw a copy of the actual stack so its EggType subtype (if any) rides along and hatches
            // deterministically.
            MoCThrownEgg thrownEgg = new MoCThrownEgg(serverLevel, player, stack.copyWithCount(1));
            thrownEgg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            serverLevel.addFreshEntity(thrownEgg);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
