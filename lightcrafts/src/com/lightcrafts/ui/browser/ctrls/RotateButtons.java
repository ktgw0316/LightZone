/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;

import javax.swing.*;

/**
 * A Box holding two buttons, to rotate left and right in a browser.
 */
public class RotateButtons extends Box {

    private final static String LeftToolTip = LOCALE.get("RotateLeftToolTip");
    private final static String RightToolTip = LOCALE.get("RotateRightToolTip");

    public RotateButtons(AbstractImageBrowser browser) {
        super(BoxLayout.X_AXIS);

        ImageBrowserActions actions = browser.getActions();

        Action leftAction = actions.getLeftAction();
        JButton left = new CoolButton(/*CoolButton.ButtonStyle.LEFT*/);
        left.setAction(leftAction);
        left.setIcon(ButtonFactory.getIconByName("rotateLeft"));
        left.setToolTipText(LeftToolTip);

        Action rightAction = actions.getRightAction();
        JButton right = new CoolButton(/*CoolButton.ButtonStyle.RIGHT*/);
        right.setAction(rightAction);
        right.setIcon(ButtonFactory.getIconByName("rotateRight"));
        right.setToolTipText(RightToolTip);

        add(left);
        add(right);
    }
}
