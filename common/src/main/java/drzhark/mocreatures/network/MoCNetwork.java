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
}
