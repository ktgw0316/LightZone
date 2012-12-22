/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import static com.lightcrafts.model.Locale.LOCALE;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Jul 30, 2007
 * Time: 2:23:16 PM
 */
public enum RGBColorSelectionPreset {
    AllColors,
    SampledColors,
    Reds,
    Yellows,
    Greens,
    Cyans,
    Blues,
    Magentas;

    public String toString() {
        return LOCALE.get( super.toString() );
    }
}
