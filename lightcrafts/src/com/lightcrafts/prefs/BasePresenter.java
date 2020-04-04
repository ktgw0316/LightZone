/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.prefs;

public abstract class BasePresenter<V> {

    V mView;

    boolean isViewAttached() {
        return mView != null;
    }

    void attachView(V view) {
        mView = view;
    }

    void detachView() {
        mView = null;
    }

    /**
     * Take the current values from the view and push them to preferences.
     */
    abstract void commit();

    /**
     * Read the current preference values and use them to initialize the view.
     */
    abstract void restore();
}
