package drzhark.mocreatures.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
 * Item that places a static Mo'Creatures furniture entity (kitty bed / litter box), mirroring the legacy
 * {@code MoCItemKittyBed} / {@code MoCItemLitterBox} behaviour and adding a click-on-a-block placement.
 * The concrete entity type is supplied at construction, so one class serves every furniture item. The
 * entities fall under gravity (see {@code MoCEntityKittyBed}'s constructor), so wherever one is put it
 * comes to rest on the ground.
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
     * Like legacy, the toss and the fall to the ground are real motion now that the entities have physics.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = this.entityType.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity != null) {
                entity.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
                faceTowardPlacer(entity, player.getDirection());
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

    /**
     * Places the furniture on the clicked block the way a vanilla spawn egg places a mob (mc262-ref
     * {@code SpawnEggItem.useOn}:69-77): a click on something without collision — a grass tuft, a flower, a
     * snow layer — puts it IN that block, anything else puts it against the clicked face, and
     * {@code EntityType.create(..., tryMoveDown = true, movedUp)} then lowers it onto the collision surface it
     * lands on (EntityType.java:177-220), so it sits flush on slabs, carpets, farmland and paths. The old code
     * always took {@code clickedPos.relative(face)} and kept that block's bottom, so a click on a grass tuft
     * left the bed a full block up, and a click on a wall or a ceiling left it wherever the face pointed; with
     * the entity also frozen by NoAI, it stayed in the air. Those side and underside clicks still start
     * airborne, but the bed now falls onto whatever is below.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            BlockPos clicked = context.getClickedPos();
            Direction face = context.getClickedFace();
            BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                    ? clicked
                    : clicked.relative(face);
            boolean movedUp = !clicked.equals(spawnPos) && face == Direction.UP;
            Entity entity = this.entityType.get().create(serverLevel, null, spawnPos,
                    EntitySpawnReason.SPAWN_ITEM_USE, true, movedUp);
            if (entity != null) {
                faceTowardPlacer(entity, context.getHorizontalDirection());
                applyColour(entity);
                serverLevel.addFreshEntity(entity);
                Player player = context.getPlayer();
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Turns freshly placed furniture to face the player who placed it, snapped to 90 degrees. The kitty bed's
     * open side, with its food bowl, is the model's -X side, which faces world (-cos yaw, -sin yaw) once
     * {@code LivingEntityRenderer} has applied its 180 - bodyRot turn; a yaw of the placer's facing + 90
     * therefore points it back at the placer. (The litter box is closed on all four sides, so it only cares
     * that the turn is square.)
     *
     * <p>Setting {@code yRot} alone — all the old {@code snapTo} did — never showed: a living entity is drawn at
     * its BODY yaw, and a client builds that from the spawn packet's HEAD yaw (mc262-ref
     * {@code LivingEntity.recreateFromPacket}:3937-3940), which stayed 0. So head and body are set as well.</p>
     */
    private static void faceTowardPlacer(Entity entity, Direction placerFacing) {
        float yaw = Mth.wrapDegrees(placerFacing.toYRot() + 90.0F);
        entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), yaw, 0.0F);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }
}
