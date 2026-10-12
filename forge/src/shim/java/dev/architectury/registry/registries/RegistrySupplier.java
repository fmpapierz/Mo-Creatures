package dev.architectury.registry.registries;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.RegistryObject;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): a registered object, backed by Forge's RegistryObject. */
public final class RegistrySupplier<T> implements Supplier<T> {
    private final RegistryObject<T> object;

    RegistrySupplier(RegistryObject<T> object) {
        this.object = object;
    }

    @Override
    public T get() {
        return this.object.get();
    }

    public Identifier getId() {
        return this.object.getId();
    }

    public ResourceKey<T> getKey() {
        return this.object.getKey();
    }

    public boolean isPresent() {
        return this.object.isPresent();
    }
}
