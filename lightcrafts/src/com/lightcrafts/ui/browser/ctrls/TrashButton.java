/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.prefs.ApplicationMode;

import javax.swing.*;

/**
 * A little trash can button that deletes selected images in a browser.
 */
public class TrashButton extends CoolButton {

    private final static String ToolTip = LOCALE.get("TrashToolTip");

    public TrashButton(AbstractImageBrowser browser) {
        ImageBrowserActions actions = browser.getActions();
        Action action = actions.getTrashAction();
        setAction(action);
        setIcon(ButtonFactory.getIconByName("trash"));
        setText(null);
        setToolTipText(ToolTip);

        ApplicationMode.maybeSetToolTip(this);
    }
}
