/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import javax.swing.*;

import com.lightcrafts.ui.print.PrintLayoutModel.LengthUnit;

import java.awt.*;

class UnitComboBox extends JComboBox {

    UnitComboBox() {
        LengthUnit[] units = LengthUnit.getAll();
        for (LengthUnit unit : units) {
            addItem(unit);
        }
        setFixedSize();
    }

    LengthUnit getSelectedUnit() {
        return (LengthUnit) getSelectedItem();
    }

    private void setFixedSize() {
        Dimension size = getPreferredSize();
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }
}
