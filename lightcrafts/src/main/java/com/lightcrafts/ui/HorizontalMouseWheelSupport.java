/*
 * Copyright (C) 2020.     Masahiro Kitagawa
 */

package com.lightcrafts.ui;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;

public interface HorizontalMouseWheelSupport {

    JComponent getHorizontalMouseWheelSupportComponent();

    void horizontalMouseWheelMoved(MouseWheelEvent e);
}
