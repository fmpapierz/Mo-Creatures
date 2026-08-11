package drzhark.mocreatures.item;

import java.util.List;

import drzhark.mocreatures.entity.passive.MoCEntityFishBowl;
import drzhark.mocreatures.entity.passive.MoCEntityFishy;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Port of the legacy {@code MoCItemFishBowl}. One class serves all twelve fish-bowl item variants; the
 * behaviour is chosen by a {@link Role} (plus a fish type 1-10 for the FISH role):
 *
 * <ul>
 *   <li>{@link Role#EMPTY} — right-click a water source to scoop it up into a water-filled bowl.</li>
 *   <li>{@link Role#WATER} — right-click (or right-click a fishy directly) to capture the nearest
 *       {@link MoCEntityFishy} within ~2.0 blocks into a matching fish bowl.</li>
 *   <li>{@link Role#FISH} — right-click a water source to release the fish back into the world (tamed
 *       to the player).</li>
 * </ul>
 *
 * Additionally, faithful to the legacy solid-block placement branch, right-clicking a solid block with
 * <em>any</em> bowl (empty, fish, or water) places a decorative {@link MoCEntityFishBowl} of the matching
 * type on top — for water bowls this only happens if no nearby fishy was captured first.
 *
 * Faithful to the legacy {@code onItemRightClick}, everything is server-side; item consumption respects
 * creative mode.
 */
public class MoCFishBowlItem extends Item {

    public enum Role { EMPTY, WATER, FISH }

    /**
     * Search radius (blocks) around the player for a fishy to capture with a water bowl. Legacy
     * {@code getClosestFish} is called with {@code d = 2.0} and gates on {@code d2 < d*d}, i.e. a
     * strict 2.0-block sphere.
     */
    private static final double CAPTURE_RANGE = 2.0D;

    private final Role role;
    /** For {@link Role#FISH}, the fishy type held (1-10); unused for EMPTY / WATER. */
    private final int fishType;

    public MoCFishBowlItem(Properties properties, Role role, int fishType) {
        super(properties);
        this.role = role;
        this.fishType = fishType;
    }

    // -------------------------------------------------------------------- block right-click (useOn)

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        net.minecraft.world.level.material.FluidState fluid = level.getFluidState(pos);
        boolean waterSource = fluid.isSource() && fluid.is(net.minecraft.tags.FluidTags.WATER);

        switch (this.role) {
            case EMPTY -> {
                // Scoop a water source into a water-filled bowl.
                if (waterSource) {
                    if (!level.isClientSide()) {
                        consumeAndGive(player, context.getItemInHand(), toItemStack(11));
                    }
                    return InteractionResult.SUCCESS;
                }
                // PLACE an empty bowl (type 0) on top of the clicked (solid) face.
                if (placeBowlOnSolid(context, level, player, pos, 0)) {
                    return InteractionResult.SUCCESS;
                }
            }
            case FISH -> {
                if (waterSource) {
                    // RELEASE: spawn a tamed fishy back into the world, hand back an empty bowl.
                    if (!level.isClientSide()) {
                        releaseFishy(level, player, context.getItemInHand(), pos);
                    }
                    return InteractionResult.SUCCESS;
                }
                // PLACE the fish bowl (type 1-10) on top of the clicked (solid) face.
                if (placeBowlOnSolid(context, level, player, pos, this.fishType)) {
                    return InteractionResult.SUCCESS;
                }
            }
            case WATER -> {
                // A water bowl clicked on a block still tries to capture a nearby fishy first.
                if (tryCapture(level, player, context.getItemInHand())) {
                    return InteractionResult.SUCCESS;
                }
                // No fishy captured: PLACE a water bowl (type 11) on top of the clicked (solid) face.
                if (placeBowlOnSolid(context, level, player, pos, 11)) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Faithful port of the legacy solid-block placement branch: if {@code pos} is a solid block, spawns a
     * {@link MoCEntityFishBowl} of {@code type} on the clicked face (empty = 0, fish = 1-10, water = 11) and
     * consumes one bowl (respecting creative). Returns {@code true} if the block was solid (placement
     * handled, even client-side where it only reports success for the swing animation), {@code false}
     * otherwise so the caller can fall through to {@code PASS}.
     */
    private boolean placeBowlOnSolid(UseOnContext context, Level level, Player player, BlockPos pos, int type) {
        if (!level.getBlockState(pos).isSolidRender()) {
            return false;
        }
        if (!level.isClientSide()) {
            BlockPos placePos = pos.relative(context.getClickedFace());
            MoCEntityFishBowl bowl = MoCEntities.FISH_BOWL.get()
                    .create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (bowl != null) {
                bowl.setType(type);
                float yaw = context.getHorizontalDirection().toYRot();
                bowl.snapTo(placePos.getX() + 0.5D, placePos.getY(), placePos.getZ() + 0.5D, yaw, 0.0F);
                // Lock body/head yaw too, so the renderer doesn't lerp the bowl into place
                // (which shows as a brief 2-3° tilt right after placing).
                bowl.lockRotation(yaw);
                level.addFreshEntity(bowl);
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return true;
    }

    // --------------------------------------------------------------------- air right-click (use)

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        switch (this.role) {
            case WATER -> {
                // Legacy onItemRightClick first requires getMovingObjectPositionFromPlayer(...,true)
                // != null: you must be aiming at a block/liquid within reach for any capture to run.
                // (A solid block within reach is handled by useOn instead; here we only need the
                // aim gate for the air/liquid path so a right-click at the open sky captures nothing.)
                net.minecraft.world.phys.BlockHitResult hit = getPlayerPOVHitResult(level, player,
                        net.minecraft.world.level.ClipContext.Fluid.ANY);
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        && tryCapture(level, player, stack)) {
                    return InteractionResult.SUCCESS;
                }
            }
            case EMPTY -> {
                // Fluids aren't solid blocks, so useOn won't fire on them: ray-trace including fluid
                // sources (legacy getMovingObjectPositionFromPlayer(..., true)) to scoop water.
                BlockPos pos = pickWaterSource(level, player);
                if (pos != null) {
                    if (!level.isClientSide()) {
                        consumeAndGive(player, stack, toItemStack(11));
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            case FISH -> {
                BlockPos pos = pickWaterSource(level, player);
                if (pos != null) {
                    if (!level.isClientSide()) {
                        releaseFishy(level, player, stack, pos);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    /** Ray-traces from the player's eyes, including fluid sources, and returns the hit water-source pos. */
    private static BlockPos pickWaterSource(Level level, Player player) {
        net.minecraft.world.phys.BlockHitResult hit = getPlayerPOVHitResult(level, player,
                net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        net.minecraft.world.level.material.FluidState fluid = level.getFluidState(pos);
        return (fluid.isSource() && fluid.is(net.minecraft.tags.FluidTags.WATER)) ? pos : null;
    }

    /** Spawns a tamed fishy of this bowl's type into the world and hands back an empty bowl. */
    private void releaseFishy(Level level, Player player, ItemStack stack, BlockPos pos) {
        MoCEntityFishy fishy = MoCEntities.FISHY.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (fishy != null) {
            fishy.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            fishy.setTypeMoC(this.fishType);
            fishy.selectType();
            // Legacy tameWithName enforced the per-player pet cap on every taming path, bowl releases included.
            if (!drzhark.mocreatures.entity.MoCAnimal.exceedsTameCap(fishy, player)) {
                fishy.setTamed(true);
                fishy.setOwnerName(player.getName().getString());
                // Legacy tameWithName prompted for a name the instant a creature was tamed.
                drzhark.mocreatures.network.MoCNetwork.promptName(fishy, player);
            }
            level.addFreshEntity(fishy);
            consumeAndGive(player, stack, toItemStack(0));
        }
    }

    // -------------------------------------------------------- right-click a fishy directly (WATER)

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
            net.minecraft.world.entity.LivingEntity target, InteractionHand hand) {
        if (this.role == Role.WATER && target instanceof MoCEntityFishy fishy) {
            if (!player.level().isClientSide()) {
                captureFishy(player, stack, fishy);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------------------- helpers

    /** Finds the nearest {@link MoCEntityFishy} within {@link #CAPTURE_RANGE} and captures it. */
    private boolean tryCapture(Level level, Player player, ItemStack stack) {
        if (level.isClientSide()) {
            // Report success so the swing/hand animation plays; the capture happens server-side.
            return findNearestFishy(level, player) != null;
        }
        MoCEntityFishy fishy = findNearestFishy(level, player);
        if (fishy != null) {
            captureFishy(player, stack, fishy);
            return true;
        }
        return false;
    }

    private static MoCEntityFishy findNearestFishy(Level level, Player player) {
        AABB box = player.getBoundingBox().inflate(CAPTURE_RANGE);
        List<MoCEntityFishy> fish = level.getEntitiesOfClass(MoCEntityFishy.class, box, f -> f.isAlive());
        MoCEntityFishy nearest = null;
        double best = -1.0D;
        for (MoCEntityFishy f : fish) {
            // Legacy getClosestFish gated on canEntityBeSeen: a fishy behind a wall can't be
            // scooped. hasLineOfSight is the 26.2 equivalent (eye-to-fish clip, block-blocked).
            if (!player.hasLineOfSight(f)) {
                continue;
            }
            double d = f.distanceToSqr(player);
            // Legacy getClosestFish gates on d2 < d*d: a fishy outside the 2.0-block sphere
            // (even if inside the expanded AABB corner) is not scoopable.
            if (d >= CAPTURE_RANGE * CAPTURE_RANGE) {
                continue;
            }
            if (best < 0.0D || d < best) {
                best = d;
                nearest = f;
            }
        }
        return nearest;
    }

    /** Server-side: turn the captured fishy into a matching fish bowl in the player's hand/inventory. */
    private void captureFishy(Player player, ItemStack stack, MoCEntityFishy fishy) {
        int type = fishy.getTypeMoC();
        if (type < 1 || type > 10) {
            type = 1;
        }
        fishy.discard();
        consumeAndGive(player, stack, toItemStack(type));
    }

    /**
     * Consumes one from {@code stack} (respecting creative) and gives the player {@code result}; if the
     * consumed bowl was the last one it is replaced in-hand by the result rather than added to a slot.
     */
    private void consumeAndGive(Player player, ItemStack stack, ItemStack result) {
        if (player.getAbilities().instabuild) {
            player.addItem(result);
            return;
        }
        // Resolve which hand actually holds this bowl before shrinking (works for use/useOn/entity).
        InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND
                : player.getOffhandItem() == stack ? InteractionHand.OFF_HAND : null;
        stack.shrink(1);
        if (stack.isEmpty() && hand != null) {
            player.setItemInHand(hand, result);
        } else {
            player.addItem(result);
        }
    }

    /**
     * Maps a fish-bowl type to its item stack. {@code 0} = empty bowl, {@code 1-10} = fish bowls,
     * {@code 11} = water bowl. Used by the water-bowl capture and by the placed entity's pickup.
     */
    public static ItemStack toItemStack(int type) {
        return switch (type) {
            case 1 -> new ItemStack(MoCItems.FISHBOWL_1.get());
            case 2 -> new ItemStack(MoCItems.FISHBOWL_2.get());
            case 3 -> new ItemStack(MoCItems.FISHBOWL_3.get());
            case 4 -> new ItemStack(MoCItems.FISHBOWL_4.get());
            case 5 -> new ItemStack(MoCItems.FISHBOWL_5.get());
            case 6 -> new ItemStack(MoCItems.FISHBOWL_6.get());
            case 7 -> new ItemStack(MoCItems.FISHBOWL_7.get());
            case 8 -> new ItemStack(MoCItems.FISHBOWL_8.get());
            case 9 -> new ItemStack(MoCItems.FISHBOWL_9.get());
            case 10 -> new ItemStack(MoCItems.FISHBOWL_10.get());
            case 11 -> new ItemStack(MoCItems.FISHBOWL_WATER.get());
            default -> new ItemStack(MoCItems.FISHBOWL_EMPTY.get());
        };
    }
}
