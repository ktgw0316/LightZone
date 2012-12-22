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

class DataTipPopup {
    private Popup popup;
    private DataTipCell cell;

    DataTipPopup(JComponent parent, DataTipCell cell, Point tipPosition, Dimension tipDimension) {
        this.cell = cell;
        Rectangle parentVisibleRect = parent.getVisibleRect();
        Rectangle withoutBorderRectangle = parentVisibleRect.intersection(new Rectangle(tipPosition, tipDimension));
        withoutBorderRectangle.translate(-tipPosition.x, -tipPosition.y);

        DataTipComponent dataTipComponent = new DataTipComponent(cell, withoutBorderRectangle, parent.getBackground());

        Dimension tipDimensionClipped = new Dimension(tipDimension.width, tipDimension.height);
        Window windowAncestor = SwingUtilities.getWindowAncestor(parent);
        GraphicsConfiguration gc = windowAncestor.getGraphicsConfiguration();
        Rectangle screenBounds = gc.getBounds();
        Point tipScreenPosition = new Point(tipPosition.x, tipPosition.y);
        SwingUtilities.convertPointToScreen(tipScreenPosition, parent);
        Point tipPositionClipped = new Point();
        tipPositionClipped.x = Math.max(tipScreenPosition.x, screenBounds.x);
        tipPositionClipped.y = Math.max(tipScreenPosition.y, screenBounds.y);
        tipDimensionClipped.width = Math.min(screenBounds.x + screenBounds.width - tipPositionClipped.x, tipDimensionClipped.width);
        tipDimensionClipped.height = Math.min(screenBounds.y + screenBounds.height - tipPositionClipped.y, tipDimensionClipped.height);
        SwingUtilities.convertPointFromScreen(tipPositionClipped, parent);
        dataTipComponent.setPreferredSize(tipDimensionClipped);
        SwingUtilities.convertPointToScreen(tipPosition, parent);

        PopupFactory popupFactory = PopupFactory.getSharedInstance();
        popup = popupFactory.getPopup(parent, dataTipComponent, tipPosition.x, tipPosition.y);
        popup.show();
        Window componentWindow = SwingUtilities.windowForComponent(parent);
        Window tipWindow = SwingUtilities.windowForComponent(dataTipComponent);
        //noinspection ObjectEquality
        boolean isHeavyWeight = tipWindow != null && tipWindow != componentWindow;
        dataTipComponent.setHeavyWeight(isHeavyWeight);
        if (isHeavyWeight) {
//            ToolTipManager.sharedInstance().registerComponent(dataTipComponent);
            DataTipManager.get().setTipWindow(parent, tipWindow);
        }
    }

    DataTipCell getCell() {
        return cell;
    }

    void hideTip() {
        if (popup != null) {
            popup.hide();
            popup = null;

            DataTipManager.get().setTipWindow(null, null);
        }
    }

    public boolean isTipShown() {
        return popup != null;
    }
}
