/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.advice;

import com.lightcrafts.ui.toolkit.ImageOnlyButton;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

import org.jvnet.substance.SubstanceLookAndFeel;

class CloseButton extends ImageOnlyButton {

    private final static Icon NormalIcon;
    private final static Icon PressedIcon;
    private final static Icon HighlightIcon;

    static {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        String path;
        URL url;
        Image image;
        path = "resources/circleX.png";
        url = CloseButton.class.getResource(path);
        image = toolkit.createImage(url);
        NormalIcon = new ImageIcon(image);
        path = "resources/circleXpressed.png";
        url = CloseButton.class.getResource(path);
        image = toolkit.createImage(url);
        PressedIcon = new ImageIcon(image);
        path = "resources/circleXhighlight.png";
        url = CloseButton.class.getResource(path);
        image = toolkit.createImage(url);
        HighlightIcon = new ImageIcon(image);
    }

    CloseButton() {
        super(NormalIcon, PressedIcon);
        setRolloverIcon(HighlightIcon);
        setRolloverEnabled(true);
        putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, Boolean.TRUE);
    }
}
