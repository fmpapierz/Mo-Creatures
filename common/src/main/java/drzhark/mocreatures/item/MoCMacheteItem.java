package drzhark.mocreatures.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ogre Machete — the fire ogre prince's drop. A diamond-tier sword (damage/speed/durability come from
 * the {@code Item.Properties.sword(ToolMaterial.DIAMOND, ...)} set at registration) that sets struck
 * targets on fire, like a built-in fire aspect.
 *
 * <p>{@link #postHurtEnemy} is the 26.2 on-hit hook: it only runs server-side (both the player and mob
 * attack paths invoke it inside a server-level check) and only after the strike actually landed, which
 * is exactly when the ignite should apply (same pattern as {@link MoCWeaponItem.Effect#FIRE}).
 */
public class MoCMacheteItem extends Item {

    /** How long a struck target burns, in seconds. */
    private static final float IGNITE_SECONDS = 5.0F;

    public MoCMacheteItem(Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        target.igniteForSeconds(IGNITE_SECONDS);
    }
}
