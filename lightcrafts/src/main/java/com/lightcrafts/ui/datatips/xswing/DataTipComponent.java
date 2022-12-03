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
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Note: It's important to inherit from JToolTip: Mouse events get dispatched to the parent.<br/>
 * I do not really know why this is so. At least PopupFactory creates a different type of popup
 * if the content is instance of JToolTip.
 */
class DataTipComponent extends JToolTip {
    private DataTipCell         cell;
    private CellRendererPane    rendererPane;
    private Rectangle           withoutBorderRectangle;
    private Color               backgroundColor;
    private boolean             isHeavyWeight;

    DataTipComponent(DataTipCell cell, Rectangle withoutBorderRectangle, Color backgroundColor) {
        this.cell = cell;
        this.withoutBorderRectangle = withoutBorderRectangle;
        this.backgroundColor = backgroundColor;
        rendererPane = new CellRendererPane();
        add(rendererPane);
        setFocusable(false);
        setBorder(null);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }

    /**
     * Overriden, so that Swing does not create a ToolTipUI.
     */
    public void updateUI() {
    }

    /**
     * Overriden to always return false, so that no component ever receives mouse events.
     * Instead a mouse press will be caught on the popup's window ancestor, the popup will be hidden and the mouse press redispatched
     * to the underlying component.
     */
    public boolean contains(int x, int y) {
        return isHeavyWeight;
    }

    protected void processMouseEvent(MouseEvent e) {
        DataTipManager.get().handleEventFromDataTipComponent(e);
    }

    protected void processMouseMotionEvent(MouseEvent e) {
        DataTipManager.get().handleEventFromDataTipComponent(e);
    }

    protected void processMouseWheelEvent(MouseWheelEvent e) {
        DataTipManager.get().handleEventFromDataTipComponent(e);
    }

    public void paintComponent(Graphics g) {
        Component component = cell.getRendererComponent();

        // Leave the component's opacity settings as is, just paint the background myself.
        // This seems to be the only viable solution: DefaultTableCellRenderer overrides isOpaque() and returns
        // true only if renderer color does not equal parent color. The problem is that rendererPane.paintComponent()
        // re-parents the renderer.
        g.setColor(backgroundColor);
        int width = getWidth();
        int height = getHeight();
        g.fillRect(0, 0, width, height);

        g.setColor(Color.black);
        g.drawRect(0, 0, width - 1, height - 1);

        if (withoutBorderRectangle != null) {
            Shape oldClip = g.getClip();
            g.setClip(withoutBorderRectangle);
            g.setColor(backgroundColor);
            g.fillRect(0, 0, width, height);
            g.setClip(oldClip);
        }

        g.setClip(1, 1, width - 2, height - 2);
        rendererPane.paintComponent(g, component, this, 0, 0, width, height);
        g.setClip(withoutBorderRectangle);
        rendererPane.paintComponent(g, component, this, 0, 0, width, height);
    }

    public void setHeavyWeight(boolean isHeavyWeight) {
        this.isHeavyWeight = isHeavyWeight;
    }
}
