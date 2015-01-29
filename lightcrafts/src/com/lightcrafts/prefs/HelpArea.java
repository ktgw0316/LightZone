/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.platform.Platform;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

class HelpArea extends JTextArea{

    HelpArea() {
        setColumns(40);
        setRows(4);
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);

        // In Windows L&F, the default TextArea font is monospaced and
        // difficult to read:
        Font font;
        if (Platform.getType() == Platform.MacOSX) {
            font = getFont();
            font = font.deriveFont(11f);
        }
        else {
            JButton button = new JButton();
            font = button.getFont();
        }
        setFont(font);

        Border empty = BorderFactory.createEmptyBorder(2, 4, 2, 4);
        Border line = BorderFactory.createLineBorder(Color.gray);
        Border compound = BorderFactory.createCompoundBorder(line, empty);
        setBorder(compound);
    }
}
