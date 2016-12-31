/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import com.lightcrafts.ui.toolkit.CoolButton;

import javax.swing.*;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;

/**
 * A Box holding two buttons, to flip horizontal and vertical in a browser.
 */
public class FlipButtons extends Box {

    private final static String HorizontalToolTip = "Horizontal"; // TODO: LOCALE.get("FlipHorizontalToolTip");
    private final static String VerticalToolTip = "Vertical"; // TODO: LOCALE.get("FlipVerticalToolTip");

    public FlipButtons(AbstractImageBrowser browser) {
        super(BoxLayout.X_AXIS);

        ImageBrowserActions actions = browser.getActions();

        Action horizontalAction = actions.getHorizontalAction();
        JButton horizontal = new CoolButton(/*CoolButton.ButtonStyle.LEFT*/);
        horizontal.setAction(horizontalAction);
        horizontal.setIcon(ButtonFactory.getIconByName("rotateLeft" /* TODO: "flipHorizontal" */));
        horizontal.setToolTipText(HorizontalToolTip);

        Action verticalAction = actions.getVerticalAction();
        JButton vertical = new CoolButton(/*CoolButton.ButtonStyle.RIGHT*/);
        vertical.setAction(verticalAction);
        vertical.setIcon(ButtonFactory.getIconByName("rotateRight" /* TODO: "flipVertical" */));
        vertical.setToolTipText(VerticalToolTip);

        add(horizontal);
        add(vertical);
    }
}
