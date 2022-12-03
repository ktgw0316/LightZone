/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.util.Map;

/**
 * The only special property of the RawAdjustmentsOperation is that it
 * provides two sets of preset values for its sliders: "Auto" and "As Shot".
 */
public interface RawAdjustmentOperation extends ColorDropperOperation {

    /**
     * Get the Map of slider keys and values corresponding to the "Auto" raw
     * adjustment preset.
     */
    Map<String, Double> getAuto();

    /**
     * Get the Map of slider keys and values corresponding to the "As Shot"
     * raw adjustment preset.
     */
    Map<String, Double> getAsShot();

    @Override
    default void accept(GenericOperationVisitor visitor) {
        visitor.visitRawAdjustmentOperation(this);
    }
}
