/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.utils.tuple;

import lombok.Data;

import java.util.Map;

@Data(staticConstructor = "of")
public final class Pair<L, R> implements Map.Entry<L, R> {
    public final L left;
    public final R right;

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R r) {
        throw new UnsupportedOperationException();
    }
}
