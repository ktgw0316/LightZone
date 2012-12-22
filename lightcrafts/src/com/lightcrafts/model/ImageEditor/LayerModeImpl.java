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

    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }

    public boolean equals(Object o) {
        if (! (o instanceof LayerMode)) {
            return false;
        }
        return name.equals(((LayerMode) o).getName());
    }

    public int hashCode() {
        return name.hashCode();
    }
}
