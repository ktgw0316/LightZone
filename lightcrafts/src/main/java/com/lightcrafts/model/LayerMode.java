/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/** A marker interface for specifying a blending mode of an Operation.  Legal
  * implementations of LayerMode come from an Engine.
  * <p>
  * @see com.lightcrafts.model.LayerConfig
  * @see com.lightcrafts.model.Operation#setLayerConfig(LayerConfig)
  * @see com.lightcrafts.model.Engine#getLayerModes
  */
public interface LayerMode {

    /** Get a user-presentable name for this LayerMode.
      * @return A user-presentable String that distinguishes this LayerMode
      * from all others.
      */
    String getName();
}
