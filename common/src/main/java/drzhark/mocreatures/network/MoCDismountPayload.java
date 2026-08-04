package drzhark.mocreatures.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -&gt; server packet: dismount from the Mo'Creatures mount the player is riding (legacy Dismount
 * keybind / {@code dismountEntity}). The server ejects the rider from whatever it is riding.
 */
public record MoCDismountPayload(int unused) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MoCDismountPayload> TYPE = new CustomPacketPayload.Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("mocreatures", "mount_dismount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoCDismountPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoCDismountPayload::unused,
            MoCDismountPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
