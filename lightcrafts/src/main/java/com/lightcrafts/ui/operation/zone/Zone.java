/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;

import java.awt.*;
import java.util.ResourceBundle;

/** An enum class to represent the zones of intensity, including an
  * interpretation as a Color.
  */

class Zone {

    private final static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/ui/operation/zone/resources/Zone"
    );

    final static int MinimumZone =
        Integer.parseInt(Resources.getString("minimumZone"));

    final static int MaximumZone =
        Integer.parseInt(Resources.getString("maximumZone"));

    final static int ZoneCount =
        Integer.parseInt(Resources.getString("zoneCount"));

    final static Zone[] zones = new Zone[MaximumZone - MinimumZone + 1];

    static {
        assert (ZoneCount >= 2);
        assert (0 <= MinimumZone);
        assert (MinimumZone < MaximumZone);
        assert (MaximumZone < ZoneCount);

        for (int n=MinimumZone; n<=MaximumZone; n++) {
            zones[n - MinimumZone] = new Zone(n);
        }
    }

    private Color color;

    private Zone(int level) {

        // Each Zone has a Color, which is a shade of gray.  The gray levels
        // are calculated to approximate an exponential progression, while
        // still including the maximum and minimum values in the sRGB color
        // space.

        float intensity = (float) (
            Math.pow(
                2, 8. * (level - MinimumZone) / (MaximumZone - MinimumZone)
            ) - 1
        ) / 255f;

        float[] srgbColor = Functions.fromLinearToCS(
            JAIContext.systemColorSpace,
            new float[] {intensity, intensity, intensity}
        );
        color = new Color(srgbColor[0], srgbColor[1], srgbColor[2]);
    }

    Color getColor() {
        return color;
    }

    static Zone[] getZones() {
        return zones;
    }
}
