package drzhark.mocreatures.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -&gt; server packet: make the mount the player is riding jump (legacy Jump keybind /
 * {@code makeEntityJump}). Carries no meaningful data (a single unused int keeps the composite-codec
 * pattern identical to {@code MoCNamePayload}); the server jumps whatever the sender is riding.
 */
public record MoCJumpPayload(int unused) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MoCJumpPayload> TYPE = new CustomPacketPayload.Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("mocreatures", "mount_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoCJumpPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoCJumpPayload::unused,
            MoCJumpPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
