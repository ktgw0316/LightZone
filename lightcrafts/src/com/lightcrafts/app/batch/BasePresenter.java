package com.lightcrafts.app.batch;

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
}
