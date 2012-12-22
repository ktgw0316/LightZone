/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.batch;

import static com.lightcrafts.app.batch.Locale.LOCALE;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.help.HelpConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class HelpButton extends CoolButton {

    private static Icon Icon =
        IconFactory.createInvertedIcon(HelpButton.class, "info.png");

    private final static String ToolTip = LOCALE.get("SendHelpButtonToolTip");

    HelpButton() {
        setIcon(Icon);
        setToolTipText(ToolTip);
        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    Platform platform = Platform.getPlatform();
                    platform.showHelpTopic(HelpConstants.HELP_PHOTOS_SENDING);
                }
            }
        );
    }
}
