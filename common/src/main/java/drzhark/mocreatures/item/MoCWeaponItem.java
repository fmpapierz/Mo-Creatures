package drzhark.mocreatures.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the legacy {@code MoCItemWeapon}: a melee weapon that applies a mapped on-hit effect, restoring
 * the elemental identity of the eight scorpion swords/stings. Faithful to the legacy {@code hitEntity}
 * switch (damageType: 1 = poison, 2 = frost slowdown, 3 = fire, 4 = confusion, 5 = blindness) with the same
 * 100-tick effect duration. The base sword attributes (damage/speed, set on the {@link Item.Properties})
 * are unchanged.
 *
 * <p>Like the legacy stings, the weapon also behaves like a sword against cobwebs: it mines
 * {@link Blocks#COBWEB} quickly ({@link #getDestroySpeed}), can actually harvest it
 * ({@link #isCorrectToolForDrops}, legacy {@code canHarvestBlock}), and takes durability wear when it
 * breaks a block ({@link #mineBlock}, legacy {@code onBlockDestroyed}).
 *
 * <p>Legacy {@code MoCItemWeapon} also carried a {@code fragile} flag: fragile weapons (the four
 * scorpion stings, registered as {@code EnumToolMaterial.GOLD} with {@code fragile=true}) took
 * {@code i=10} durability wear per mob hit in {@code hitEntity} instead of the normal {@code i=1},
 * so a sting shatters after ~3-4 hits. Non-fragile weapons (the IRON scorpion swords, the tusks)
 * keep the default 1-per-attack wear from the vanilla weapon component and are unaffected.
 */
public class MoCWeaponItem extends Item {

    /** Legacy damageType → on-hit effect. */
    public enum Effect { NONE, POISON, SLOWNESS, FIRE, CONFUSION, BLINDNESS }

    private static final int EFFECT_TICKS = 100;

    /** Legacy {@code hitEntity}: fragile weapons (the stings) take this much durability wear per mob hit. */
    private static final int FRAGILE_HIT_WEAR = 10;

    private final Effect effect;
    private final boolean fragile;

    public MoCWeaponItem(Properties properties, Effect effect) {
        this(properties, effect, false);
    }

    public MoCWeaponItem(Properties properties, Effect effect, boolean fragile) {
        super(properties);
        this.effect = effect;
        this.fragile = fragile;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (this.fragile) {
            // Legacy hitEntity damaged fragile stings by 10 per hit (on top of the component's default 1).
            stack.hurtAndBreak(FRAGILE_HIT_WEAR, attacker, EquipmentSlot.MAINHAND);
        }
        switch (this.effect) {
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_TICKS, 0), attacker);
            case SLOWNESS -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EFFECT_TICKS, 0), attacker);
            case FIRE -> target.igniteForSeconds(10.0F);
            case CONFUSION -> target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, EFFECT_TICKS, 0), attacker);
            case BLINDNESS -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0), attacker);
            default -> {
            }
        }
    }

    /**
     * Legacy {@code getStrVsBlock}: the stings cut cobwebs like a sword, mining {@link Blocks#COBWEB}
     * quickly instead of at the default hand speed.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(Blocks.COBWEB)) {
            return 15.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    /**
     * Legacy {@code canHarvestBlock}: the stings can actually harvest cobwebs (drop the web/string).
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (state.is(Blocks.COBWEB)) {
            return true;
        }
        return super.isCorrectToolForDrops(stack, state);
    }

    /**
     * Legacy {@code onBlockDestroyed}: breaking a block wears the sting by 2 durability. Applied directly
     * (rather than relying on the {@code Tool} data component) so the wear matches the legacy behaviour.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(2, owner, EquipmentSlot.MAINHAND);
        }
        return true;
    }
}
