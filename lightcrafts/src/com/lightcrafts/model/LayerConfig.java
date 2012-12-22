/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/** This class is a simple struct for holding a LayerMode and an opacity
  * number.  Each Operation has accepts arbitrary LayerConfigs, and every
  * Operation has a default LayerConfig.
  * <p>
  * @see com.lightcrafts.model.Operation#setLayerConfig
  * @see com.lightcrafts.model.Operation#getDefaultLayerConfig
  */

public class LayerConfig {

    private LayerMode mode;
    private double opacity;

    /** Make a new LayerConfig with the given LayerMode and opacity number.
      * The opacity is always between zero and one.
      * @param mode A LayerMode from the List returned by
      * <code>Engine.getLayerModes()</code>
      * @param opacity A number from zero to one inclusive.  If this argument
      * is less than zero, it will be treated as zero; and if it is greater
      * than one, it will be treated as one.
      */
    public LayerConfig(LayerMode mode, double opacity) {
        this.mode = mode;
        opacity = Math.max(opacity, 0d);
        opacity = Math.min(opacity, 1d);
        this.opacity = opacity;
    }

    /** Return the LayerMode from the constructor.
      * @return The LayerMode given to the Constructor.
      */
    public LayerMode getMode() {
        return mode;
    }

    /** Return the opacity number from the constructor.
      * @return The opacity given to the constructor.
      */
    public double getOpacity() {
        return opacity;
    }
}
