/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;

/**
 * This button is really a JLabel with a MouseListener that listens for mouse
 * pressed and then changes the label's icon and simultaneously shows a popup
 * menu.  The icon switches back when the popup closes.
 */

public class MenuButton extends CoolButton implements MouseListener, PopupMenuListener
{
    private JPopupMenu menu;

    public MenuButton() {
        addMouseListener(this);
    }

    public MenuButton(Icon icon) {
        setIcon(icon);
        addMouseListener(this);
        menu = new JPopupMenu();
        menu.addPopupMenuListener(this);
    }

    public void add(JMenuItem item) {
        if (menu == null) {
            menu = new JPopupMenu();
            menu.addPopupMenuListener(this);
            setEnabled(true);
        }
        menu.add(item);
    }

    public void add(Action action) {
        JMenuItem item = new JMenuItem(action);
        add(item);
    }

    public void clear() {
        if (menu != null) {
            menu.removeAll();
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent event) {
        if (isEnabled() && (menu != null) && (! menu.isVisible())) {
            setSelected(true);
            Dimension size = getSize();
            menu.show(this, 0, size.height);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
        setSelected(false);
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    public static void main(String[] args) {
        Icon normal = new ImageIcon(args[0]);

        final MenuButton button = new MenuButton();
        button.setIcon(normal);

        JMenuItem item1 = new JMenuItem("Item 1");
        item1.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    System.out.println("item 1");
                }
            }
        );
        button.add(item1);

        JMenuItem item2 = new JMenuItem("Item 2");
        item2.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    System.out.println("item 2");
                }
            }
        );
        button.add(item2);

        JButton enable = new JButton("Enable");
        enable.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    button.setEnabled(true);
                }
            }
        );
        JButton disable = new JButton("Disable");
        disable.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    button.setEnabled(false);
                }
            }
        );

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(button);
        panel.add(enable);
        panel.add(disable);

        JFrame frame = new JFrame("MenuButton Test");
        frame.setContentPane(panel);
        frame.setBounds(100, 100, 200, 200);
        frame.setVisible(true);
    }
}
