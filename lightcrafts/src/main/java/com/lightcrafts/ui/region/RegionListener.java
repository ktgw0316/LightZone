/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

public interface RegionListener {

    void regionBatchStart(Object cookie);

    void regionChanged(Object cookie, SharedShape shape);

    void regionBatchEnd(Object cookie);
}
