/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.toolkit.ImageOnlyButton;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.lightcrafts.ui.operation.Locale.LOCALE;

class HelpButton extends ImageOnlyButton {

    private static final Icon InfoNormalIcon;
    private static final Icon InfoPressedIcon;

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
        setToolTipText(ToolTip);
        addActionListener(event -> {
            String topic = control.getHelpTopic();
            Platform platform = Platform.getPlatform();
            platform.showHelpTopic(topic);
        });
    }
}
