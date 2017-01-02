/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

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
    private final static String HorizontalToolTip = LOCALE.get("FlipHorizontalToolTip");
    private final static String VerticalToolTip = LOCALE.get("FlipVerticalToolTip");

    public RotateButtons(AbstractImageBrowser browser) {
        super(BoxLayout.X_AXIS);

        ImageBrowserActions actions = browser.getActions();

        Action leftAction = actions.getLeftAction();
        JButton left = new CoolButton(/*CoolButton.ButtonStyle.LEFT*/);
        left.setAction(leftAction);
        left.setIcon(ButtonFactory.getIconByName("rotateLeft"));
        left.setToolTipText(LeftToolTip);

        Action rightAction = actions.getRightAction();
        JButton right = new CoolButton(/*CoolButton.ButtonStyle.CENTER*/);
        right.setAction(rightAction);
        right.setIcon(ButtonFactory.getIconByName("rotateRight"));
        right.setToolTipText(RightToolTip);

        Action horizontalAction = actions.getHorizontalAction();
        JButton horizontal = new CoolButton(/*CoolButton.ButtonStyle.CENTER*/);
        horizontal.setAction(horizontalAction);
        horizontal.setIcon(ButtonFactory.getIconByName("rotateLeft" /* TODO: "flipHorizontal" */));
        horizontal.setToolTipText(HorizontalToolTip);

        Action verticalAction = actions.getVerticalAction();
        JButton vertical = new CoolButton(/*CoolButton.ButtonStyle.RIGHT*/);
        vertical.setAction(verticalAction);
        vertical.setIcon(ButtonFactory.getIconByName("rotateRight" /* TODO: "flipVertical" */));
        vertical.setToolTipText(VerticalToolTip);

        add(left);
        add(right);
        add(horizontal);
        add(vertical);
    }
}
