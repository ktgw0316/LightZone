/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import static com.lightcrafts.ui.crop.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

class CropPopupMenu extends JPopupMenu implements MouseListener {
    
    CropPopupMenu(final CropMode mode, final ResetAction reset) {
        JMenuItem commitItem = new JMenuItem(LOCALE.get("CommitMenuItem"));
        commitItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mode.doCrop();
                }
            }
        );
        add(commitItem);

        JMenuItem resetItem = new JMenuItem(LOCALE.get("ResetMenuItem"));
        resetItem.addActionListener(reset);
        add(resetItem);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger()) {
            handlePopup(event);
        }
    }

    public void mouseReleased(MouseEvent event) {
        if (event.isPopupTrigger()) {
            handlePopup(event);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    private void handlePopup(MouseEvent event) {
        Component comp = event.getComponent();
        Point p = event.getPoint();
        show(comp, p.x, p.y);
    }
}
