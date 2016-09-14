/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.LayerMode;

/** A String-based implementation of LayerMode, just enough to
  * provide a user-presentable String and distinguish identity.
  */

class LayerModeImpl implements LayerMode {

    private String name;

    LayerModeImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof LayerMode) && name.equals(((LayerMode) o).getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
