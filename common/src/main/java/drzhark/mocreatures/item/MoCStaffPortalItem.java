package drzhark.mocreatures.item;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Staff of Portal — right-click to travel between the Overworld and the Wyvern Lair dimension.
 * Since the Wyvern Lair is floating-island terrain, arrivals search for a solid island surface near
 * the destination column (and drop a small platform as a last resort) so the player never lands in
 * the void below the islands.
 */
public class MoCStaffPortalItem extends Item {

    public static final ResourceKey<Level> WYVERN_LAIR = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "wyvern_lair"));

    public MoCStaffPortalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Never change dimensions while mounted or being ridden — that would strand the mount/rider
        // in the departure dimension (faithful to the legacy ItemStaffPortal guard).
        if (player.isPassenger() || player.isVehicle()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                ItemStack staff = player.getItemInHand(hand);
                ResourceKey<Level> currentKey = serverPlayer.level().dimension();
                int cx = Mth.floor(serverPlayer.getX());
                int cz = Mth.floor(serverPlayer.getZ());
                if (!currentKey.equals(WYVERN_LAIR)) {
                    // --- Travel TO the Lair ---
                    ServerLevel target = server.getLevel(WYVERN_LAIR);
                    if (target != null) {
                        // Persist the exact origin (dimension + position) ON THE STAFF, so the return is
                        // exact, survives a server restart, and lands back in the ORIGINAL dimension (not
                        // always the Overworld). Stored in the item's CUSTOM_DATA component.
                        storeReturn(staff, currentKey, serverPlayer.position());
                        // Force the destination chunk to fully generate FIRST, so the surface scan sees the
                        // real island terrain instead of ungenerated air.
                        target.getChunk(cx >> 4, cz >> 4);
                        BlockPos landing = findIslandSurface(target, cx, cz);
                        if (landing == null) {
                            landing = new BlockPos(cx, 96, cz);
                            buildPlatform(target, landing.below());
                        }
                        target.setBlock(landing, Blocks.AIR.defaultBlockState(), 3);
                        target.setBlock(landing.above(), Blocks.AIR.defaultBlockState(), 3);
                        Vec3 pos = new Vec3(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D);
                        serverPlayer.teleport(new TeleportTransition(target, pos, Vec3.ZERO,
                                serverPlayer.getYRot(), serverPlayer.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
                        // Legacy ItemStaffPortal: each portal jump wears the staff by 1 durability.
                        staff.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
                    }
                } else {
                    // --- Return to the stored origin dimension + position ---
                    ReturnPoint back = readReturn(staff, server);
                    ServerLevel target = back != null ? back.level() : server.getLevel(Level.OVERWORLD);
                    if (target != null) {
                        Vec3 pos;
                        if (back != null) {
                            pos = back.pos();
                        } else {
                            BlockPos surface = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    new BlockPos(cx, 0, cz));
                            pos = new Vec3(surface.getX() + 0.5D, surface.getY() + 1.0D, surface.getZ() + 0.5D);
                        }
                        clearReturn(staff);
                        serverPlayer.teleport(new TeleportTransition(target, pos, Vec3.ZERO,
                                serverPlayer.getYRot(), serverPlayer.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
                        // Legacy ItemStaffPortal: each portal jump wears the staff by 1 durability.
                        staff.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The persisted return destination (dimension + exact position) read back from the staff. */
    private record ReturnPoint(ServerLevel level, Vec3 pos) {}

    private static void storeReturn(ItemStack staff, ResourceKey<Level> dim, Vec3 pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("mocReturnDim", dim.identifier().toString());
        tag.putDouble("mocReturnX", pos.x);
        tag.putDouble("mocReturnY", pos.y);
        tag.putDouble("mocReturnZ", pos.z);
        staff.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static @Nullable ReturnPoint readReturn(ItemStack staff, MinecraftServer server) {
        CustomData cd = staff.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return null;
        }
        CompoundTag tag = cd.copyTag();
        String dimStr = tag.getStringOr("mocReturnDim", "");
        if (dimStr.isEmpty()) {
            return null;
        }
        ServerLevel lvl = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr)));
        if (lvl == null) {
            return null;
        }
        Vec3 pos = new Vec3(tag.getDoubleOr("mocReturnX", 0.0D),
                tag.getDoubleOr("mocReturnY", 64.0D), tag.getDoubleOr("mocReturnZ", 0.0D));
        return new ReturnPoint(lvl, pos);
    }

    private static void clearReturn(ItemStack staff) {
        staff.remove(DataComponents.CUSTOM_DATA);
    }

    /** Spiral-search nearby columns for a solid block with two air blocks of headroom above it. */
    private static @Nullable BlockPos findIslandSurface(ServerLevel target, int cx, int cz) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= 24; r += 2) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = cx + dx;
                    int z = cz + dz;
                    for (int y = 160; y >= -32; y--) {
                        p.set(x, y, z);
                        if (!target.getBlockState(p).isAir()) {
                            if (target.getBlockState(p.above()).isAir()
                                    && target.getBlockState(new BlockPos(x, y + 2, z)).isAir()) {
                                return new BlockPos(x, y + 1, z);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void buildPlatform(ServerLevel target, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                target.setBlock(center.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }
}
