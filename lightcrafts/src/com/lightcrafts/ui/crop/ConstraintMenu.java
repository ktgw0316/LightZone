/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import static com.lightcrafts.ui.crop.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This is a JPopupMenu whose items are Actions that update the current
 * AspectConstraint in a ConstraintModel.
 */
class ConstraintMenu extends CoolButton {

    private static Icon Icon = IconFactory.createInvertedIcon(
        ConstraintMenu.class, "menu.png"
    );
    private static final AspectConstraint OneOneConstraint =
        new AspectConstraint(1, 1);
    private static final AspectConstraint ThreeFiveConstraint =
        new AspectConstraint(3, 5);
    private static final AspectConstraint FourSixConstraint =
        new AspectConstraint(4, 6);
    private static final AspectConstraint FiveSevenConstraint =
        new AspectConstraint(5, 7);
    private static final AspectConstraint EightTenConstraint =
        new AspectConstraint(8, 10);
    private static final AspectConstraint ElevenFourteenConstraint =
        new AspectConstraint(11, 14);

    private JPopupMenu menu;

    ConstraintMenu(ConstraintModel constraints) {
        setIcon(Icon);

        setFocusable(false);

        setToolTipText(LOCALE.get("AspectRatioToolTip"));
        
        menu = new JPopupMenu();
        menu.add(constraints.createMenuAction(OneOneConstraint));
        menu.addSeparator();
        menu.add(constraints.createMenuAction(ThreeFiveConstraint));
        menu.add(constraints.createMenuAction(FourSixConstraint));
        menu.add(constraints.createMenuAction(FiveSevenConstraint));
        menu.add(constraints.createMenuAction(EightTenConstraint));
        menu.add(constraints.createMenuAction(ElevenFourteenConstraint));
        menu.addSeparator();
        menu.add(constraints.createMenuAction(ThreeFiveConstraint.getInverse()));
        menu.add(constraints.createMenuAction(FourSixConstraint.getInverse()));
        menu.add(constraints.createMenuAction(FiveSevenConstraint.getInverse()));
        menu.add(constraints.createMenuAction(EightTenConstraint.getInverse()));
        menu.add(constraints.createMenuAction(ElevenFourteenConstraint.getInverse()));

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Rectangle bounds = getBounds();
                    menu.show(ConstraintMenu.this, 0, bounds.height);
                }
            }
        );
    }

    public void setFont(Font font) {
        super.setFont(font);
        // Guard against setFont() calls from base class constructors.
        if (menu != null) {
            menu.setFont(font);
            MenuElement[] items = menu.getSubElements();
            for (MenuElement item : items) {
                item.getComponent().setFont(font);
            }
        }
    }
}
