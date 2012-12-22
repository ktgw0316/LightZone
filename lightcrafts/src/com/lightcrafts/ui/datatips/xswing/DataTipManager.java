/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * Copyright (c) 2002 - 2005, Stephen Kelvin Friedrich. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list
 *   of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 * - Neither the name of the copyright holder nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lightcrafts.ui.datatips.xswing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.security.AccessControlException;

/**
 * <code>DataTipManager</code> provides data tips for tree, table and list components. Whenever the mouse cursor is
 * over a cell whose content is partially hidden a popup with the complete cell content is shown.
 * The cell content can be hidden because it is clipped at either the parent component bounds (e.g. scrollpane) or at
 * the cell bounds (e.g. table row height is too small).
 */
public class DataTipManager {
    private static DataTipManager   instance;

    private ListDataTipListener     listMouseListener   = new ListDataTipListener();
    private TableDataTipListener    tableMouseListener  = new TableDataTipListener();
    private TreeDataTipListener     treeMouseListener   = new TreeDataTipListener();
    private Component               parentComponent;
    private Window                  tipComponentWindow;
    private MouseEvent              lastMouseEvent;
    private static boolean          allowUntrustedUsage;

    private DataTipManager() {
        try {
            long eventMask = AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK;
            Toolkit.getDefaultToolkit().addAWTEventListener(new MouseEventModifier(), eventMask);
        }
        catch(AccessControlException e) {
            if(!allowUntrustedUsage) {
                throw new RuntimeException("DataTipManager needs to run in a trusted application", e);
            }
        }
    }

    static void enableUntrustedUsage(boolean enable) {
        allowUntrustedUsage = enable;
    }

    /**
     * @return the single, shared instance of the DataTipmanager
     */
    public static synchronized DataTipManager get() {
        if(instance == null) {
            instance = new DataTipManager();
        }
        return instance;
    }

    /**
     * Enable data tips for a list component.
     * @param list the list which should be enhanced with data tips.
     */
    public synchronized void register(JList list) {
        list.addMouseListener(listMouseListener);
        list.addMouseMotionListener(listMouseListener);
        list.addComponentListener(listMouseListener);
    }

    /**
     * Enable data tips for a tree component.
     * @param tree the tree which should be enhanced with data tips.
     */
    public synchronized void register(JTree tree) {
        tree.addMouseListener(treeMouseListener);
        tree.addMouseMotionListener(treeMouseListener);
        tree.addComponentListener(treeMouseListener);
    }

    /**
     * Enable data tips for a list component.
     * @param table the table which should be enhanced with data tips.
     */
    public synchronized void register(JTable table) {
        table.addMouseListener(tableMouseListener);
        table.addMouseMotionListener(tableMouseListener);
        table.addComponentListener(tableMouseListener);
    }

    /**
     * Undo register(JList).
     * @param list A JList that was passed to <code>register</code>
     */
    public synchronized void unregister(JList list) {
        list.removeMouseListener(listMouseListener);
        list.removeMouseMotionListener(listMouseListener);
        list.removeComponentListener(listMouseListener);
    }

    /**
     * Undo register(JTree).
     * @param tree A JTree that was passed to <code>register</code>
     */
    public synchronized void unregister(JTree tree) {
        tree.removeMouseListener(treeMouseListener);
        tree.removeMouseMotionListener(treeMouseListener);
        tree.removeComponentListener(treeMouseListener);
    }

    /**
     * Undo register(JTable).
     * @param table A JTable that was passed to <code>register</code>
     */
    public synchronized void unregister(JTable table) {
        table.removeMouseListener(tableMouseListener);
        table.removeMouseMotionListener(tableMouseListener);
        table.removeComponentListener(tableMouseListener);
    }

    void setTipWindow(Component parentComponent, Window dataTipComponent) {
        this.parentComponent = parentComponent;
        tipComponentWindow = dataTipComponent;
    }

    public boolean handleEventFromParentComponent(MouseEvent mouseEvent) {
        // filter out events that come from client explicitly calling this method, but we have already handled in awt event listener
        if(mouseEvent == lastMouseEvent) {
            return false;
        }
        Object      source = mouseEvent.getSource();
        if(source != parentComponent) {
            return false;
        }
        int         id = mouseEvent.getID();
        int         x = mouseEvent.getX();
        int         y = mouseEvent.getY();
        long        when = mouseEvent.getWhen();
        int         modifiers = mouseEvent.getModifiers();
        int         clickCount = mouseEvent.getClickCount();
        boolean     isPopupTrigger = mouseEvent.isPopupTrigger();
        if(id == MouseEvent.MOUSE_EXITED) {
            Point point = SwingUtilities.convertPoint(parentComponent, x, y, tipComponentWindow);
            if(tipComponentWindow.contains(point)) {
                MouseEvent newEvent = new MouseEvent(parentComponent, MouseEvent.MOUSE_MOVED, when, modifiers,
                        x, y, clickCount, isPopupTrigger);
                parentComponent.dispatchEvent(newEvent);
                // If the datatip has been hidden as a result, then process the exit event, too, so that
                // e.g. tooltips will hide.
                boolean stillVisible = parentComponent != null;
                return stillVisible;
            }
        }
        return false;
    }

    public void handleEventFromDataTipComponent(MouseEvent mouseEvent) {
        mouseEvent.consume();
        int         id = mouseEvent.getID();
        if(id != MouseEvent.MOUSE_ENTERED) {
            int     x       = mouseEvent.getX();
            int     y       = mouseEvent.getY();
            Point   point   = SwingUtilities.convertPoint(mouseEvent.getComponent(), x, y, parentComponent);

            if(id == MouseEvent.MOUSE_EXITED && parentComponent.contains(point)) {
                return;
            }
            long        when            = mouseEvent.getWhen();
            int         modifiers       = mouseEvent.getModifiers();
            int         clickCount      = mouseEvent.getClickCount();
            boolean     isPopupTrigger  = mouseEvent.isPopupTrigger();
            MouseEvent newEvent;
            if(id == MouseEvent.MOUSE_WHEEL) {
                MouseWheelEvent mouseWheelEvent = (MouseWheelEvent) mouseEvent;
                int             scrollType = mouseWheelEvent.getScrollType();
                int             scrollAmount = mouseWheelEvent.getScrollAmount();
                int             wheelRotation = mouseWheelEvent.getWheelRotation();
                newEvent = new MouseWheelEvent(parentComponent, id, when, modifiers, point.x, point.y,
                        clickCount, isPopupTrigger, scrollType, scrollAmount,
                        wheelRotation);
            }
            else {
                newEvent = new MouseEvent(parentComponent, id, when, modifiers, point.x, point.y,
                        clickCount, isPopupTrigger);
            }
            Component parentComponentBackup = parentComponent;
            parentComponent.dispatchEvent(newEvent);
            if(parentComponent == null && id != MouseEvent.MOUSE_EXITED) {
                MouseEvent exitEvent = new MouseEvent(parentComponentBackup, MouseEvent.MOUSE_EXITED, when,
                        modifiers, point.x, point.y, clickCount, isPopupTrigger);
                parentComponentBackup.dispatchEvent(exitEvent);
            }
            if(tipComponentWindow != null && id != MouseEvent.MOUSE_MOVED) {
                // mouse click might have changed appearance (e.g. selection color)
                tipComponentWindow.repaint();
            }
        }
    }

    private class MouseEventModifier implements AWTEventListener {
        private MouseEventModifier() {
        }

        public void eventDispatched(AWTEvent event) {
            if(tipComponentWindow == null) {
                return;
            }
            Object      source      = event.getSource();

            if(source == parentComponent) {
                MouseEvent  mouseEvent = (MouseEvent) event;
                boolean filter = handleEventFromParentComponent(mouseEvent);
                if(filter) {
                    mouseEvent.consume();
                }
                else {
                    lastMouseEvent = mouseEvent;
                }
            }
        }
    }
}

