/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.platform.Platform;
import static com.lightcrafts.ui.operation.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.ImageOnlyButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.jvnet.substance.SubstanceLookAndFeel;

class HelpButton extends ImageOnlyButton {

    private static Icon InfoNormalIcon;
    private static Icon InfoPressedIcon;

    static {
        try {
            BufferedImage image = ImageIO.read(
                HelpButton.class.getResource("resources/info.png")
            );
            InfoNormalIcon = IconFactory.invertIcon(new ImageIcon(image));
            image = ImageIO.read(
                HelpButton.class.getResource("resources/infoPressed.png")
            );
            InfoPressedIcon = IconFactory.invertIcon(new ImageIcon(image));
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't initialize HelpButton", e);
        }
    }

    private final static String ToolTip = LOCALE.get("HelpButtonToolTip");

    HelpButton(final SelectableControl control) {
        super(InfoNormalIcon, InfoPressedIcon);
        putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, Boolean.TRUE);
        setToolTipText(ToolTip);
        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String topic = control.getHelpTopic();
                    Platform platform = Platform.getPlatform();
                    platform.showHelpTopic(topic);
                }
            }
        );
    }
}
