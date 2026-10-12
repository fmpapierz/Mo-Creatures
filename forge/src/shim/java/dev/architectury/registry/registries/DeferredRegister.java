package dev.architectury.registry.registries;

import dev.architectury.impl.ForgeShim;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): backed by Forge's DeferredRegister on the mod bus group. */
public final class DeferredRegister<T> implements Iterable<RegistrySupplier<T>> {
    private final net.minecraftforge.registries.DeferredRegister<T> forge;
    private final List<RegistrySupplier<T>> entries = new ArrayList<>();

    private DeferredRegister(net.minecraftforge.registries.DeferredRegister<T> forge) {
        this.forge = forge;
    }

    public static <T> DeferredRegister<T> create(String modId, ResourceKey<Registry<T>> key) {
        return new DeferredRegister<>(net.minecraftforge.registries.DeferredRegister.create(key, modId));
    }

    public <R extends T> RegistrySupplier<R> register(String id, Supplier<? extends R> supplier) {
        RegistrySupplier<R> entry = new RegistrySupplier<>(this.forge.register(id, supplier));
        @SuppressWarnings("unchecked")
        RegistrySupplier<T> asT = (RegistrySupplier<T>) entry;
        this.entries.add(asT);
        return entry;
    }

    @Override
    public Iterator<RegistrySupplier<T>> iterator() {
        return this.entries.iterator();
    }

    public void register() {
        this.forge.register(ForgeShim.modBusGroup());
    }
}
