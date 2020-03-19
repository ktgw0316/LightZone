/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import com.lightcrafts.model.CropBounds;

public interface CropListener {

    void cropCommitted(CropBounds bounds);

    void unCrop();
}
