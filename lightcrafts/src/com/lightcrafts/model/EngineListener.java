/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/** A listener to learn about how busy an Engine is.  Updates are
  * asynchronous and may arrive on any thread.  The
  * <code>engineActive()</code>method passes an integer activity level
  * that may be from zero to Integer.MAX_VALUE.
  * <p>
  * @see com.lightcrafts.model.Engine
  */
public interface EngineListener {

    /** The Engine's activity level has changed.
      * @param level A number from zero to Integer.MAX_VALUE, where a bigger
      * number means more active.
      */
    void engineActive(int level);
}
