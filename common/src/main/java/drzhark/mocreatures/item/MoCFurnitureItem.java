package drzhark.mocreatures.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Item that places a static Mo'Creatures furniture entity (kitty bed / litter box) on top of the
 * clicked block, mirroring the legacy {@code MoCItemKittyBed} / {@code MoCItemLitterBox} behaviour.
 * The concrete entity type is supplied at construction, so one class serves every furniture item.
 */
public class MoCFurnitureItem extends Item {

    private final Supplier<? extends EntityType<?>> entityType;
    /** Kitty-bed dye colour (0 = plain, 1..16 = a dye colour, DyeColor id + 1); ignored by the litter box. */
    private final int kittyBedColour;

    public MoCFurnitureItem(Properties properties, Supplier<? extends EntityType<?>> entityType) {
        this(properties, entityType, 0);
    }

    public MoCFurnitureItem(Properties properties, Supplier<? extends EntityType<?>> entityType, int kittyBedColour) {
        super(properties);
        this.entityType = entityType;
        this.kittyBedColour = kittyBedColour;
    }

    /** Stamp this furniture item's dye colour onto the freshly placed bed (legacy round-trips the sheet colour). */
    private void applyColour(Entity entity) {
        if (this.kittyBedColour > 0
                && entity instanceof drzhark.mocreatures.entity.passive.MoCEntityKittyBed bed) {
            bed.setSheetColour(this.kittyBedColour);
        }
    }

    /**
     * Plain right-click (including at air) drops the furniture entity at the player's own feet with a
     * small random toss, mirroring legacy {@code MoCItemKittyBed}/{@code MoCItemLitterBox#onItemRightClick}
     * (decrement stack, spawn at posX/posY/posZ, motionY += rand*0.05F, motionX/Z += (rand-rand)*0.3F).
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = this.entityType.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity != null) {
                entity.snapTo(player.getX(), player.getY(), player.getZ(), entity.getYRot(), 0.0F);
                RandomSource random = serverLevel.getRandom();
                double motionX = (random.nextFloat() - random.nextFloat()) * 0.3F;
                double motionY = random.nextFloat() * 0.05F;
                double motionZ = (random.nextFloat() - random.nextFloat()) * 0.3F;
                entity.setDeltaMovement(entity.getDeltaMovement().add(motionX, motionY, motionZ));
                applyColour(entity);
                serverLevel.addFreshEntity(entity);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
            Entity entity = this.entityType.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity != null) {
                entity.snapTo(placePos.getX() + 0.5D, placePos.getY(), placePos.getZ() + 0.5D,
                        placeRotation(context), 0.0F);
                applyColour(entity);
                level.addFreshEntity(entity);
                Player player = context.getPlayer();
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Face the furniture roughly toward the player who placed it (snapped to 90-degree steps). */
    private static float placeRotation(UseOnContext context) {
        Direction facing = context.getHorizontalDirection();
        return facing.toYRot();
    }
}
