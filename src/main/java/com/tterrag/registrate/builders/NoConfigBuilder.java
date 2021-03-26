package com.tterrag.registrate.builders;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonnullType;

public class NoConfigBuilder<R, T extends R, P> extends AbstractBuilder<R, T, P, NoConfigBuilder<R, T, P>> {
    
    private final NonNullSupplier<T> factory;

    public NoConfigBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, Class<? super R> registryType, NonNullSupplier<T> factory) {
        super(owner, parent, name, callback, registryType);
        this.factory = factory;
    }

    @Override
    protected @NonnullType T createEntry() {
        return factory.get();
    }
}
