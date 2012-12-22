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
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract class DataTipListener extends MouseInputAdapter implements ComponentListener {
    private DataTipPopup dataTipPopup;

    // When running under JDK 1.5 (or higher) DataTipListener will check if the tip must be updated when the parent
    // component changes size or moves.
    // With JDK 1.4 or earlier this does not work, because no mouse event gets posted (even though the relative mouse
    // position changes) - yet to be able to compile with 1.4 the necessary JDK 1.5 methods are called by reflection.
    // On JDK 1.4 the popup is just always hidden qwhen the component changes (and reshown when the user first moves
    // the mouse again).
    private static final Class[]    NO_PARAMETERS           = new Class[0];
    private static final Object[]   NO_ARGUMENTS            = new Object[0];
    private static Class            mouseInfoClass;
    private static Method           getPointerInfoMethod;
    private static Class            pointerInfoClass;
    private static Method           getLocationMethod;

    static {
        try {
            mouseInfoClass          = Class.forName("java.awt.MouseInfo");
            getPointerInfoMethod    = mouseInfoClass.getMethod("getPointerInfo", NO_PARAMETERS);
            pointerInfoClass        = Class.forName("java.awt.PointerInfo");
            getLocationMethod       = pointerInfoClass.getMethod("getLocation", NO_PARAMETERS);
        }
        catch(NoSuchMethodException e) {
            // fine probably running on pre-1.5-JDK
        }
        catch(ClassNotFoundException e) {
            // fine probably running on pre-1.5-JDK
        }
    }

    DataTipListener() {
    }

    /**
     * @return the cell that is at position 'point' in the component or DataTipCell.NONE if there isn't a cell at that
     * point.
     */
    abstract DataTipCell getCell(JComponent component, Point point);

    /**
     * If the user presses a mouse button on a popup, Swing's behaviour depends on the popup type:
     * - lightweight popup:
     *   This case is handled here. Because the TipComponent.contains(int x, int y) is overriden to always return false
     *   Swing will dispath the event directly to the parent component.
     * - heavyweight popup:
     *   Swing will dispatch the event to the popup's window, which is handled in DataTipPopup.
     */
    public void mousePressed(MouseEvent e) {
        //hideTip(); Can't: Double click would not work. Click count of the second click would be '1', because it would
        // go to a different window (for heavyweight datatips).
    }

    public void mouseEntered(MouseEvent event) {
        checkShowOrHide(event);
    }

    public void mouseExited(MouseEvent event) {
        checkShowOrHide(event);
    }

    public void mouseDragged(MouseEvent event) {
        checkShowOrHide(event);
    }

    public void mouseMoved(MouseEvent event) {
        checkShowOrHide(event);
    }

    private void checkShowOrHide(MouseEvent event) {
        JComponent      component     = (JComponent) event.getSource();
        Point           mousePosition = event.getPoint();
        checkShowOrHide(component, mousePosition);
    }

    private void checkShowOrHide(JComponent component, Point mousePosition) {
        Window windowAncestor = SwingUtilities.getWindowAncestor(component);
        if (windowAncestor == null || !windowAncestor.isActive()) {
            hideTip();
            return;
        }

        DataTipCell    dataTipCell    = getCell(component, mousePosition);
        Rectangle      visRect        = component.getVisibleRect();

        if(!visRect.contains(mousePosition)) {
            dataTipCell = DataTipCell.NONE;
        }

        DataTipCell currentPopupCell = getCurrentPopupCell();
        if(dataTipCell.equals(currentPopupCell)) {
            return;
        }

        hideTip();
        if(!dataTipCell.isSet()) {
            return;
        }

        dataTipPopup = createPopup(component, mousePosition, dataTipCell);
    }

    private DataTipCell getCurrentPopupCell() {
        if(!isTipShown()) {
            return DataTipCell.NONE;
        }
        return dataTipPopup.getCell();
    }

    private DataTipPopup createPopup(JComponent component, Point mousePosition, DataTipCell dataTipCell) {
        Rectangle       cellBounds              = dataTipCell.getCellBounds();

        Rectangle       visRect                 = component.getVisibleRect();
        Rectangle       visibleCellRectangle    = cellBounds.intersection(visRect);
        if (!visibleCellRectangle.contains(mousePosition)) {
            return null;
        }

        Component       rendererComponent       = dataTipCell.getRendererComponent();
        Dimension       rendCompDim             = rendererComponent.getMinimumSize();
        Rectangle       rendCompBounds          = new Rectangle(cellBounds.getLocation(), rendCompDim);
        if(cellBounds.contains(rendCompBounds) && visRect.contains(rendCompBounds)) {
            return null;
        }

        Dimension       preferredSize           = rendererComponent.getPreferredSize();
        Point           tipPosition             = cellBounds.getLocation();
        int             width                   = Math.max(cellBounds.width, preferredSize.width);
        int             height                  = Math.max(cellBounds.height, preferredSize.height);
        Dimension       tipDimension            = new Dimension(width, height);
        DataTipPopup    dataTipPopup            = new DataTipPopup(component, dataTipCell, tipPosition, tipDimension);

        return dataTipPopup;
    }

    private boolean isTipShown() {
        return dataTipPopup != null && dataTipPopup.isTipShown();
    }

    private void hideTip() {
        if (dataTipPopup != null) {
            dataTipPopup.hideTip();
            dataTipPopup = null;
        }
    }

    public void componentResized(ComponentEvent e) {
        checkShowOrHide(e);
    }

    public void componentMoved(ComponentEvent e) {
        checkShowOrHide(e);
    }

    public void componentShown(ComponentEvent e) {
        checkShowOrHide(e);
    }

    public void componentHidden(ComponentEvent e) {
        hideTip();
    }

    private void checkShowOrHide(ComponentEvent e) {
        JComponent component = (JComponent) e.getSource();
        Point mousePosition = getCurrentMousePosition();
        if(mousePosition == null) {
            hideTip();
        }
        else {
            SwingUtilities.convertPointFromScreen(mousePosition, component);
            checkShowOrHide(component, mousePosition);
        }
    }

    private static Point getCurrentMousePosition() {
        if(mouseInfoClass == null) {
            return null;
        }
        try {
            Object pointerInfo = getPointerInfoMethod.invoke(null, NO_ARGUMENTS);
            Point mousePosition = (Point) getLocationMethod.invoke(pointerInfo, NO_ARGUMENTS);
            return mousePosition;
        }
        catch(IllegalAccessException e) {
            // strange, but nothing I can do here
        }
        catch(InvocationTargetException e) {
            // strange, but nothing I can do here
        }

        return null;
    }
}
