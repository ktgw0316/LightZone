/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.*;
import java.awt.*;

class CoolButtonCenterBackground extends CoolButtonBackground {

    CoolButtonCenterBackground(AbstractButton button) {
        super(button);
    }

    Insets getInsets() {
        return new Insets(4, 4, 4, 4);
    }
}
