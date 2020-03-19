/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

interface ZoneModelListener {

    void zoneModelBatchStart(ZoneModelEvent event);

    void zoneModelChanged(ZoneModelEvent event);

    void zoneModelBatchEnd(ZoneModelEvent event);
}
