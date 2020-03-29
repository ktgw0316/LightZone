/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.BrowserFactory;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class CollapseButton extends CoolToggleButton {

    private final static String CollapseToolTip =
        LOCALE.get("CollapseButtonCollapseToolTip");
    private final static String ExpandToolTip =
        LOCALE.get("CollapseButtonExpandToolTip");

    private final static Icon CollapseIcon =
        IconFactory.createInvertedIcon(UndoButton.class, "unstacked.png");
    private final static Icon ExpandIcon =
        IconFactory.createInvertedIcon(UndoButton.class, "stacked.png");

    public CollapseButton(final ComboFrame frame) {
        // Initialize collapsed
        setIcon(CollapseIcon);
        setToolTipText(CollapseToolTip);
        // setSelectedIcon(ExpandIcon);
        // setRolloverIcon(null);

        if (BrowserFactory.isDefaultCollapsed()) {
            setSelected(true);
            setToolTipText(ExpandToolTip);
        }
        addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        frame.setBrowserCollapsed();
                        CollapseButton.this.setIcon(ExpandIcon);
                        // setRolloverIcon(ExpandIcon);
                        setToolTipText(ExpandToolTip);
                    }
                    else {
                        frame.setBrowserExpanded();
                        CollapseButton.this.setIcon(ExpandIcon);
                        // setRolloverIcon(CollapseIcon);
                        setToolTipText(CollapseToolTip);
                    }
                }
            }
        );
        setEnabled(true);
    }
}
