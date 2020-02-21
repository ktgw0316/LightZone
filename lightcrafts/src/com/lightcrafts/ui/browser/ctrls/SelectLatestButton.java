/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.ctrls;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFontFactory;

import javax.swing.*;

/**
 * A button that calls AbstractImageBrowser.selectLatest(), which sets the
 * browser selection state to include the most recently modified image in
 * each image group.
 */
public class SelectLatestButton extends CoolButton {

    private final static String ToolTip = LOCALE.get("SelectLatestToolTip");

    private final static Icon icon = IconFontFactory.buildIcon("recent");

    public SelectLatestButton(AbstractImageBrowser browser) {
        ImageBrowserActions actions = browser.getActions();
        Action action = actions.getSelectLatestAction();
        setAction(action);
        // String name = (String) action.getValue(Action.NAME);
        // setText(name);
        setToolTipText(ToolTip);
        setIcon(icon);
    }
}
