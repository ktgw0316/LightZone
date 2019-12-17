/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFontFactory;
import com.lightcrafts.ui.help.HelpConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class HelpButton extends CoolButton {

    enum Mode { Browse, Edit, Combo, Basic }

    private final static Icon Icon = IconFontFactory.buildIcon("info");

    private final static String ToolTip = LOCALE.get("HelpButtonToolTip");

    private String topic;

    HelpButton() {
        setIcon(Icon);
        setToolTipText(ToolTip);
        setMode(Mode.Combo);
        addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Platform platform = Platform.getPlatform();
                    platform.showHelpTopic(topic);
                }
            }
        );
    }

    void setMode(Mode mode) {
        topic = null;
        switch (mode) {
            case Browse:
                topic = HelpConstants.HELP_ANATOMY_BROWSER;
                break;
            case Edit:
                topic = HelpConstants.HELP_ANATOMY_EDITOR;
                break;
        }
    }
}
