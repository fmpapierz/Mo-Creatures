package dev.architectury.event;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): an event the shared code can listen to. */
@FunctionalInterface
public interface Event<T> {
    void register(T listener);
}
