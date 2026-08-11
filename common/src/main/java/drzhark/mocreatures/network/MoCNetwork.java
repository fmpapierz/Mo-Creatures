package drzhark.mocreatures.network;

import dev.architectury.networking.NetworkManager;
import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Cross-loader networking setup (called from common init). */
public final class MoCNetwork {

    private MoCNetwork() {
    }

    public static void init() {
        // Client -> server: apply a name typed in the naming screen to a tamed creature the player owns.
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MoCNamePayload.TYPE, MoCNamePayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player == null) {
                        return;
                    }
                    Entity target = player.level().getEntity(payload.entityId());
                    if (target instanceof IMoCEntity moc && moc.getIsTamed()
                            && moc.getOwnerName().equals(player.getName().getString())
                            && target.distanceToSqr(player) < 400.0D) {
                        String name = payload.name().trim();
                        if (!name.isEmpty() && name.length() <= 30) {
                            target.setCustomName(Component.literal(name));
                            target.setCustomNameVisible(true);
                        }
                    }
                }));

        // Server -> client: pop up the naming screen for a creature the player has just tamed.
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MoCOpenNamePayload.TYPE, MoCOpenNamePayload.CODEC,
                (payload, context) -> context.queue(() -> dev.architectury.utils.EnvExecutor.runInEnv(
                        dev.architectury.utils.Env.CLIENT,
                        () -> () -> drzhark.mocreatures.client.MoCClientHelper.openNameScreen(
                                payload.entityId(), payload.currentName()))));

        // Client -> server: jump the Mo'Creatures mount the sender is riding (legacy Jump keybind).
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MoCJumpPayload.TYPE, MoCJumpPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player != null && player.getVehicle() instanceof IMoCEntity moc) {
                        moc.makeEntityJump();
                    }
                }));

        // Client -> server: dismount from the Mo'Creatures mount the sender is riding (legacy Dismount keybind).
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MoCDismountPayload.TYPE, MoCDismountPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player != null && player.getVehicle() instanceof IMoCEntity moc
                            && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        moc.dismountMoCEntity(serverLevel);
                    }
                }));
    }

    /**
     * Legacy {@code MoCTools.tameWithName}: assigns ownership and immediately prompts the taming player to
     * name their new pet. Every taming path in the mod funnels through here, so a newly tamed creature is
     * named the moment it is won over rather than only when a Medallion is right-clicked onto it later.
     *
     * @return {@code false} (and does nothing) when the player is already at their tamed-pet cap.
     */
    public static boolean tameWithName(IMoCEntity moc, Player player) {
        if (!(moc instanceof Entity entity) || entity.level().isClientSide()) {
            return false;
        }
        if (drzhark.mocreatures.entity.MoCAnimal.exceedsTameCap(entity, player)) {
            return false;
        }
        moc.setTamed(true);
        moc.setOwnerName(player.getName().getString());
        promptName(moc, player);
        return true;
    }

    /** Opens the naming screen on {@code player}'s client for a creature they own. Server-side. */
    public static void promptName(IMoCEntity moc, Player player) {
        if (!(moc instanceof Entity entity) || !(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        NetworkManager.sendToPlayer(sp, new MoCOpenNamePayload(entity.getId(),
                entity.hasCustomName() && entity.getCustomName() != null
                        ? entity.getCustomName().getString() : ""));
    }
}
