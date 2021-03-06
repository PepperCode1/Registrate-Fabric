package com.tterrag.registrate.util;

import com.tterrag.registrate.fabric.Lazy;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonnullType;

public class NonNullLazyValue<T> extends Lazy<T> implements NonNullSupplier<T> {

    public NonNullLazyValue(NonNullSupplier<T> supplier) {
        super(supplier);
    }

    @Override
    public @NonnullType T get() {
        return super.get();
    }
}
