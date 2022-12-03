/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

public class ZoneModelEvent {

    private ZoneModel source;

    ZoneModelEvent(ZoneModel source) {
        this.source = source;
    }

    public ZoneModel getSource() {
        return source;
    }
}
