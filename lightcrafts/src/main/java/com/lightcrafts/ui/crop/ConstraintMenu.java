/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.crop;

import static com.lightcrafts.ui.crop.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

/**
 * This is a JPopupMenu whose items are Actions that update the current
 * AspectConstraint in a ConstraintModel.
 */
class ConstraintMenu extends CoolButton {

    private static Icon Icon = IconFactory.createInvertedIcon(
        ConstraintMenu.class, "menu.png"
    );
    private static final List<AspectConstraint> aspects = Arrays.asList(
            new AspectConstraint(1, 1),               // 1
            new AspectConstraint(5, 6),               // 1.2
            new AspectConstraint(4, 5),               // 1.25
            new AspectConstraint(11, 14),             // 1.27
            new AspectConstraint(17, 22, "Letter"),   // 1.29
            new AspectConstraint(3, 4, "VGA"),        // 1.33
            new AspectConstraint(5, 7),               // 1.4
            new AspectConstraint(105, 148, "ISO A"),  // 1.414
            new AspectConstraint(2, 3, "35mm"),       // 1.5
            new AspectConstraint(5, 8),               // 1.6
            new AspectConstraint(3, 5),               // 1.666
            new AspectConstraint(9, 16, "HDTV"),      // 1.78
            new AspectConstraint(1, 2),               // 2
            new AspectConstraint(18, 39, "iPhone X")  // 2.166
    );

    private JPopupMenu menu;

    ConstraintMenu(ConstraintModel constraints) {
        setIcon(Icon);

        setFocusable(false);

        setToolTipText(LOCALE.get("AspectRatioToolTip"));
        
        menu = new JPopupMenu();
        for (final AspectConstraint aspect : aspects) {
            menu.add(constraints.createMenuAction(aspect));
        }

        addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Rectangle bounds = getBounds();
                    menu.show(ConstraintMenu.this, 0, bounds.height);
                }
            }
        );
    }

    @Override
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
