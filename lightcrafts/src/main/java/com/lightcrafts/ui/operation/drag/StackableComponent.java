/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

import javax.swing.*;

public interface StackableComponent {

    JComponent getDraggableComponent();

    boolean isSwappable();
}
