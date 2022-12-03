/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFontFactory;

import javax.swing.*;

/**
 * A Box holding copy and paste buttons for a browser.
 */
public class CopyPasteButtons extends Box {

    private final static String CopyToolTip = LOCALE.get("CopyToolTip");
    private final static String PasteToolTip = LOCALE.get("PasteToolTip");

    public CopyPasteButtons(AbstractImageBrowser browser) {
        super(BoxLayout.X_AXIS);

        ImageBrowserActions actions = browser.getActions();

        Action copyAction = actions.getCopyAction();
        JButton copy = new CoolButton(/*CoolButton.ButtonStyle.LEFT*/);
        copy.setAction(copyAction);
        copy.setIcon(IconFontFactory.buildIcon("copy"));
        copy.setToolTipText(CopyToolTip);

        Action pasteAction = actions.getPasteAction();
        JButton paste = new CoolButton(/*CoolButton.ButtonStyle.RIGHT*/);
        paste.setAction(pasteAction);
        paste.setIcon(IconFontFactory.buildIcon("paste"));
        paste.setToolTipText(PasteToolTip);

        add(copy);
        add(paste);
    }
}
