package dev.architectury.networking;

import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadFlow;

/**
 * Forge stand-in (see dev.architectury.impl.ForgeShim): Mo'Creatures' payloads ride one Forge payload channel,
 * {@code mocreatures:main}, in the play phase. Forge builds a channel once, with every payload it carries, so the
 * receivers are collected first and the Forge entrypoint calls {@link #buildChannel()} after the shared init has
 * registered them all.
 */
public final class NetworkManager {
    private static final List<Entry<?>> ENTRIES = new ArrayList<>();
    private static Channel<CustomPacketPayload> channel;

    private NetworkManager() {}

    public enum Side {
        S2C,
        C2S
    }

    @FunctionalInterface
    public interface NetworkReceiver<T> {
        void receive(T value, PacketContext context);
    }

    public interface PacketContext {
        /** The sender on the server; the local player on the client. */
        Player getPlayer();

        void queue(Runnable runnable);

        Env getEnvironment();
    }

    private record Entry<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec, NetworkReceiver<T> receiver) {
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> addTo(
                PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow) {
            return flow.add(this.type, this.codec, (payload, context) -> {
                this.receiver.receive(payload, new Context(context));
                context.setPacketHandled(true);
            });
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerReceiver(Side side, CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkReceiver<T> receiver) {
        if (channel != null) {
            throw new IllegalStateException("Mo'Creatures: payload " + type.id() + " registered after the channel was built");
        }
        ENTRIES.add(new Entry<>(type, (StreamCodec<RegistryFriendlyByteBuf, T>) codec, receiver));
    }

    /** Builds the channel with every payload registered so far (both directions; each side only receives its own). */
    public static void buildChannel() {
        var flow = ChannelBuilder.named(Identifier.fromNamespaceAndPath("mocreatures", "main"))
                .networkProtocolVersion(1)
                .payloadChannel()
                .play()
                .bidirectional();
        for (Entry<?> entry : ENTRIES) {
            flow = entry.addTo(flow);
        }
        channel = flow.build();
    }

    public static void sendToServer(CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.PLAYER.with(player));
    }

    private record Context(CustomPayloadEvent.Context forge) implements PacketContext {
        @Override
        public Player getPlayer() {
            return this.forge.isServerSide() ? this.forge.getSender() : ClientPlayer.get();
        }

        @Override
        public void queue(Runnable runnable) {
            this.forge.enqueueWork(runnable);
        }

        @Override
        public Env getEnvironment() {
            return this.forge.isServerSide() ? Env.SERVER : Env.CLIENT;
        }
    }

    /** Kept apart so a dedicated server never loads the client's Minecraft class. */
    private static final class ClientPlayer {
        static Player get() {
            return net.minecraft.client.Minecraft.getInstance().player;
        }
    }
}
