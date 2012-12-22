/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

import javax.swing.*;
import java.awt.*;

class StackLayer extends JPanel {

    private DraggableStack draggableStack;

    private JComponent dragComp;
    private JComponent placeholderComp;
    private boolean isDragSwappable;

    // Take a DraggableStack so we can notify swapOccurred() on it:

    StackLayer(DraggableStack draggableStack) {
        this.draggableStack = draggableStack;
        setLayout(null);
        setOpaque(false);
    }

    void setDragComponent(JComponent comp, boolean isSwappable) {
        if (dragComp == comp) {
            return;
        }
        if (comp != null) {
            placeholderComp = new JPanel();
            placeholderComp.setOpaque(false);
            Dimension size = comp.getPreferredSize();
            placeholderComp.setPreferredSize(size);
            int index = getIndexOf(comp);
            remove(comp);
            add(placeholderComp, index);
        }
        else {
            int index = getIndexOf(placeholderComp);
            remove(placeholderComp);
            add(dragComp, index);
            placeholderComp = null;
        }
        dragComp = comp;
        isDragSwappable = isSwappable;
        validate();
        repaint();
    }

    public void doLayout() {
        Dimension size = getSize();

        Component[] comps = getComponents();
        if (comps.length == 0) {
            return;
        }
        int y = 0;
        int midX = size.width / 2;

        for (int n=comps.length-1; n>=0; n--) {
            Component comp = comps[n];
            Dimension pref = comp.getPreferredSize();
            int width = pref.width;
            int height = pref.height;
            int x = midX - width / 2;
            if (x < 0) {
                x = 0;
                width = size.width;
            }
            comp.setLocation(x, y);
            comp.setSize(width, height);
            y += height;
        }
    }

    public Dimension getPreferredSize() {
        Component[] comps = getComponents();
        int width = 0;
        int height = 0;
        for (int n=0; n<comps.length; n++) {
            Component comp = comps[n];
            Dimension pref = comp.getPreferredSize();
            width = Math.max(width, pref.width);
            height += pref.height;
        }
        return new Dimension(width, height);
    }

    // See whether the current drag height crosses the threshold to swap
    // Component layout:

    void dragTo(int dragTop) {
        if (! isDragSwappable) {
            return;
        }
        int index = getIndexOf(placeholderComp);
        Rectangle phBounds = placeholderComp.getBounds();
        int phTop = phBounds.y;
        int dragBottom = dragTop + phBounds.height;
        if (dragTop <= phTop) {
            if (index < getComponentCount() - 1) {
                Component upper = getComponent(index + 1);
                if (! ((StackableComponent) upper).isSwappable()) {
                    return;
                }
                int upperMidY = upper.getY() + upper.getHeight() / 2;
                if (dragTop < upperMidY) {
                    remove(placeholderComp);
                    remove(upper);
                    add(upper, index);
                    add(placeholderComp, index + 1);
                    notifySwap(index);
                }
            }
        }
        else {
            if (index > 0) {
                Component lower = getComponent(index - 1);
                if (! ((StackableComponent) lower).isSwappable()) {
                    return;
                }
                int lowerMidY = lower.getY() + lower.getHeight() / 2;
                if (dragBottom > lowerMidY) {
                    remove(lower);
                    remove(placeholderComp);
                    add(placeholderComp, index - 1);
                    add(lower, index);
                    notifySwap(index - 1);
                }
            }
        }
    }

    private void notifySwap(int index) {
        draggableStack.swapOccurred(index);
        validate();
        repaint();
    }

    private int getIndexOf(Component comp) {
        Component[] comps = getComponents();
        for (int n=0; n<comps.length; n++) {
            if (comp == comps[n]) {
                return n;
            }
        }
        return -1;
    }
}
