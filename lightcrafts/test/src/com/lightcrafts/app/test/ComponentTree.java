/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.test;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ComponentTree extends JTree implements TreeSelectionListener {

    private Component comp;

    private Component recentHighlight;

    ComponentTree(Component comp) {
        super(new ComponentTreeNode(comp, null));
        this.comp = comp;
        getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION
        );
        addTreeSelectionListener(this);
        setExpandsSelectedPaths(true);

        expandAll();
    }

    void expandAll() {
        int count = getRowCount();
        for (int row=0; row<count; row++) {
            expandRow(row);
        }
        if (getRowCount() > count) {
            expandAll();
        }
    }

    Component getComponent() {
        return comp;
    }

    public void valueChanged(TreeSelectionEvent e) {
        if (recentHighlight != null) {
            toggleHighlight(recentHighlight);
        }
        Component nextHighlight = null;
        TreePath path = getSelectionPath();
        if (path != null) {
            ComponentTreeNode node =
                (ComponentTreeNode) path.getLastPathComponent();
            nextHighlight = node.getComponent();
            toggleHighlight(nextHighlight);
        }
        recentHighlight = nextHighlight;
    }

    private void toggleHighlight(Component c) {
        Graphics g;
        Rectangle b;
        if (c != null) {
            b = c.getBounds();
            Container parent = c.getParent();
            if (parent != null) {
                g = parent.getGraphics();
                if (g != null) {
                    g.setXORMode(Color.pink);
                    g.setColor(Color.black);
                    g.fillRect(b.x, b.y, b.width, b.height);
                    g.setPaintMode();
                }
            } else {
                g = c.getGraphics();
                if (g != null) {
                    g.setXORMode(Color.red);
                    g.setColor(Color.black);
                    g.fillRect(0, 0, b.width, b.height);
                    g.setPaintMode();
                }
            }
        }
    }

    public static void show(final Component comp) {
        ComponentTree tree = new ComponentTree(comp);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(tree));

        final JButton button = new JButton("Reset");
        button.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    panel.removeAll();
                    ComponentTree tree = new ComponentTree(comp);
                    panel.add(tree);
                    panel.add(button, BorderLayout.SOUTH);
                    panel.validate();
                    panel.repaint();
                }
            }
        );
        panel.add(button, BorderLayout.SOUTH);

        JFrame frame = new JFrame("ComponentTree");
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocation(0, 0);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        JPanel a = new JPanel();
        JPanel b = new JPanel();
        JPanel c = new JPanel();
        a.add(new JLabel("A"));
        b.add(new JLabel("B"));
        c.add(new JLabel("C"));
        a.add(b);
        b.add(c);
        JFrame frame = new JFrame("Test");
        frame.setContentPane(a);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        show(frame);
    }
}
