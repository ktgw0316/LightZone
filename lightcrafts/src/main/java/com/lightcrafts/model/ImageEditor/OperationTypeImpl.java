/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.OperationType;

class OperationTypeImpl implements OperationType {

    private String name;

    OperationTypeImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean hasPreview() {
        return false;
    }

    public boolean isPreviewOnly() {
        return false;
    }
}
