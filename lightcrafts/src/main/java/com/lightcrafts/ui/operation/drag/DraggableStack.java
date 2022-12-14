/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;

/** A lightweight Container that places its children in a fixed-width
  * vertical stack, with mouse listeners attached to allow drag-and-drop style
  * shuffling of the layout.
  */

public class DraggableStack extends JLayeredPane {

    private enum Layer { Stack, Drag }

    private final StackLayer stack;
    private final DragLayer drag;
    private final StackDragListener dragListener;

    private final LinkedList<DraggableStackListener> listeners;       // DraggableStackListeners

    public DraggableStack() {
        setLayout(null);
        stack = new StackLayer(this);
        drag = new DragLayer();
        add(stack, Layer.Stack);
        dragListener = new StackDragListener(this);
        listeners = new LinkedList<>();
    }

    /** Components must be added and removed via push() and pop().  This method
      * throws an AssertionError.
      */
    protected void addImpl(Component comp, Object constraints, int index) {
        assert ((comp == stack) || (comp == drag));
        super.addImpl(comp, constraints, index);
    }

    public void push(JComponent comp) {
        int count = stack.getComponentCount();
        push(comp, count);
    }

    public void push(JComponent comp, int index) {
        if (comp instanceof StackableComponent) {
            addListeners((StackableComponent) comp);
        }
//        stack.add(new BirthContainer(comp), index);
        stack.add(comp, index);
    }

    public JComponent pop() {
        int count = stack.getComponentCount();
        return pop(count - 1);
    }

    public JComponent pop(JComponent comp) {
        int count = stack.getComponentCount();
        for (int i=0; i<count; i++) {
            Component c = stack.getComponent(i);
            if (comp == c) {
                return pop(i);
            }
        }
        return null;
    }
    
    public JComponent pop(int index) {
        JComponent comp = (JComponent) stack.getComponent(index);
        if (comp instanceof StackableComponent) {
            removeListeners((StackableComponent) comp);
        }
        stack.remove(index);
//        stack.add(new DeathContainer(comp), index);
        return comp;
    }

    public void addDraggableStackListener(DraggableStackListener listener) {
        listeners.add(listener);
    }

    public void removeDraggableStackListener(DraggableStackListener listener) {
        listeners.remove(listener);
    }

    public void doLayout() {
        Dimension size = getSize();
        drag.setLocation(0, 0);
        drag.setSize(size);
        stack.setLocation(0, 0);
        stack.setSize(size);
    }

    public Dimension getPreferredSize() {
        Dimension stackSize = stack.getPreferredSize();
        Dimension dragSize = drag.getPreferredSize();
        int width = Math.max(stackSize.width, dragSize.width);
        int height = Math.max(stackSize.height, dragSize.height);
        return new Dimension(width, height);
    }

    void dragStart(JComponent comp, boolean isSwappable) {
        stack.setDragComponent(comp, isSwappable);
        add(drag, Layer.Drag);
        drag.setDragComponent(comp);
        notifyStart();
    }

    void dragTo(Component comp, int y) {
        if (y >= 0) {
            drag.dragTo(y);
            stack.dragTo(y);
            revalidate();
        }
    }

    void swapOccurred(int index) {
        notifySwap(index);
    }

    void dragEnd(JComponent comp) {
        stack.setDragComponent(null, false);
        drag.setDragComponent(null);
        remove(drag);

        notifyEnd();
        revalidate();
    }

    private void addListeners(StackableComponent stackable) {
        Component comp = stackable.getDraggableComponent();
        addListenerRecurse(comp);
    }

    private void removeListeners(StackableComponent stackable) {
        Component comp = stackable.getDraggableComponent();
        removeListenerRecurse(comp);
    }

    private void addListenerRecurse(Component comp) {
        comp.addMouseListener(dragListener);
        comp.addMouseMotionListener(dragListener);
        if (comp instanceof Container) {
            Component[] children = ((Container) comp).getComponents();
            for (int n=0; n<children.length; n++) {
                if (! children[n].isEnabled()) {
                    addListenerRecurse(children[n]);
                }
            }
        }
    }

    private void removeListenerRecurse(Component comp) {
        comp.removeMouseListener(dragListener);
        comp.removeMouseMotionListener(dragListener);
        if (comp instanceof Container) {
            Component[] children = ((Container) comp).getComponents();
            for (int n=0; n<children.length; n++) {
                removeListenerRecurse(children[n]);
            }
        }
    }

    private void notifyStart() {
        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            DraggableStackListener listener = (DraggableStackListener) i.next();
            listener.dragStarted();
        }
    }

    private void notifySwap(int index) {
        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            DraggableStackListener listener = (DraggableStackListener) i.next();
            listener.swapped(index);
        }
    }

    private void notifyEnd() {
        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            DraggableStackListener listener = (DraggableStackListener) i.next();
            listener.dragStopped();
        }
    }
}
