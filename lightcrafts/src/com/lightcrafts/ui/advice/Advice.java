/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.advice;

import javax.swing.*;
import java.awt.*;

public interface Advice {

    /**
     * The advice message to show to users.
     */
    String getMessage();

    /**
     * The frame that will be owner of the advice dialogs.
     */
    JFrame getOwner();

    /**
     * Relative to the parent frame.  Null means centered.
     */
    Point getLocation();

    /**
     * How many times the user should be bothered with this particular piece
     * of advice.  A negative number means nag forever.
     */
    int getMaxCount();
}
