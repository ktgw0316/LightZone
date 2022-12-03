/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import static com.lightcrafts.ui.metadata2.Locale.LOCALE;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;


class MetadataTitle extends JLabel {
    MetadataTitle() {
        super(LOCALE.get("MetadataTitle"));
        setHorizontalAlignment(LEADING);
        Font font = getFont();
        font = font.deriveFont(20f);
        setFont(font);
    }
}
