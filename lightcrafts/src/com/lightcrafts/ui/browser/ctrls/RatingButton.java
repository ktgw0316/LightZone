/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.platform.Platform;
import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import com.lightcrafts.ui.toolkit.CoolButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * A button that triggers a popup menu of rating actions for a browser.
 */
public class RatingButton extends CoolButton {

    private final static String ToolTip = LOCALE.get("RateButtonToolTip");
    // private final static String Star = "\u2605";

    private final static Icon starIcon =
        ButtonFactory.getIconByName("star");

    private JPopupMenu popup;

    public RatingButton(AbstractImageBrowser browser) {
        // setText(Star);
        setIcon(starIcon);
        setToolTipText(ToolTip);

        popup = new JPopupMenu();
        
        ImageBrowserActions actions = browser.getActions();
        List<Action> rateActions = actions.getRatingActions();
        for (Action rateAction : rateActions) {
            JMenuItem item = new JMenuItem(rateAction);
            // On Windogs only the core fonts seem to see stars
            if (Platform.isWindows())
                item.setFont(new Font("Serif", Font.PLAIN, 14));
            item.setAccelerator(null);
            popup.add(item);
        }
        popup.addSeparator();
        
        Action clearAction = actions.getClearRatingAction();
        JMenuItem item = new JMenuItem(clearAction);
        item.setAccelerator(null);
        popup.add(item);
        
        // Show the popup on mouse-pressed, not on action-performed.
        addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    Dimension size = RatingButton.this.getSize();
                    popup.show(RatingButton.this, 0, size.height);
                }
            }
        );
        // Enable and disable this button with one of the actions, which
        // follow the browser selection.
        clearAction.addPropertyChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    String propName = event.getPropertyName();
                    if (propName.equals("enabled")) {
                        boolean enabled = (Boolean) event.getNewValue();
                        setEnabled(enabled);
                        repaint();
                    }
                }
            }
        );
        setEnabled(clearAction.isEnabled());
    }
}
