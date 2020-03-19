/*
 * Copyright (C) 2020.     Masahiro Kitagawa
 */

package com.lightcrafts.ui.base;

public abstract class BasePresenter<V> {
    protected V mView;

    protected final boolean isViewAttached() {
        return mView != null;
    }

    public final void attachView(V view) {
        mView = view;
    }

    public final void detachView() {
        mView = null;
    }
}
