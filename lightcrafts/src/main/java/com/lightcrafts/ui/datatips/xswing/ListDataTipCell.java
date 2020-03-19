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

class ListDataTipCell implements DataTipCell {
    private final JList list;
    private final int rowIndex;

    ListDataTipCell(JList list, int rowIndex) {
        this.list = list;
        this.rowIndex = rowIndex;
    }

    public boolean isSet() {
        return rowIndex >= 0;
    }

    public Rectangle getCellBounds() {
        Rectangle cellRect = list.getCellBounds(rowIndex, rowIndex);
        return cellRect;
    }

    public Component getRendererComponent() {
        Object item = list.getModel().getElementAt(rowIndex);
        boolean isSelected = list.isSelectedIndex(rowIndex);
        boolean isFocussed = list.hasFocus() && rowIndex == list.getLeadSelectionIndex();
        ListCellRenderer renderer = list.getCellRenderer();
        Component component = renderer.getListCellRendererComponent(list, item, rowIndex, isSelected, isFocussed);
        return component;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }

        ListDataTipCell listDataTipCell = (ListDataTipCell) o;

        if(rowIndex != listDataTipCell.rowIndex) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return rowIndex;
    }
}
