/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import com.lightcrafts.ui.print.PrintLayoutModel.LengthUnit;

import static com.lightcrafts.ui.print.Locale.LOCALE;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

class PositionPanel extends JPanel implements PrintLayoutModelListener {

    private PrintLayoutModel model;

    private DimensionTextField leftText;
    private UnitComboBox leftUnit;
    private DimensionTextField topText;
    private UnitComboBox topUnit;

    private boolean readingFromModel;   // prevent update loops
    private boolean writingToModel;

    private JPanel titlePanel;  // intermediary container allows title borders

    PositionPanel(PrintLayoutModel model) {
        this.model = model;
        model.addListener(this);

        titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        Border border = BorderFactory.createTitledBorder(
            LOCALE.get("PositionTitle")
        );
        titlePanel.setBorder(border);

        addLeft();
        titlePanel.add(Box.createVerticalStrut(3));
        addTop();

        setLayout(new BorderLayout());
        add(titlePanel);
    }

    private void addLeft() {
        leftText = new DimensionTextField();
        leftUnit = new UnitComboBox();
        updateLeft();

        leftText.setListener(
            new DimensionTextField.Listener() {
                public void dimensionChanged(double left) {
                    if (! readingFromModel) {
                        writingToModel = true;
                        LengthUnit unit = leftUnit.getSelectedUnit();
                        model.setImageX(left, unit);
                        writingToModel = false;
                    }
                }
            }
        );
        syncTextWithUnit(leftText, leftUnit);

        Box box = createLabelledText(
            LOCALE.get("LeftLabel"), leftText, leftUnit
        );
        titlePanel.add(box);
    }

    private void addTop() {
        topText = new DimensionTextField();
        topUnit = new UnitComboBox();
        updateTop();

        topText.setListener(
            new DimensionTextField.Listener() {
                public void dimensionChanged(double top) {
                    if (! readingFromModel) {
                        writingToModel = true;
                        LengthUnit unit = topUnit.getSelectedUnit();
                        model.setImageY(top, unit);
                        writingToModel = false;
                    }
                }
            }
        );
        syncTextWithUnit(topText, topUnit);

        Box box = createLabelledText(LOCALE.get("TopLabel"), topText, topUnit);
        titlePanel.add(box);
    }

    // Update a dimension text field when its corresonding units change:

    private static void syncTextWithUnit(
        final DimensionTextField text, final UnitComboBox unit
    ) {
        unit.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        double oldDim = text.getDimension();
                        LengthUnit oldUnit = text.getUnit();
                        LengthUnit newUnit = unit.getSelectedUnit();
                        double newDim =
                            newUnit.fromPoints(oldUnit.toPoints(oldDim));
                        text.setUnit(newUnit);
                        text.setDimension(newDim);
                    }
                }
            }
        );
    }

    private static Box createLabelledText(
        String name, DimensionTextField text, UnitComboBox units
    ) {
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        box.add(new JLabel(name + ':'));
        box.add(Box.createHorizontalStrut(3));
        box.add(text);
        box.add(Box.createHorizontalStrut(3));
        box.add(units);
        return box;
    }

    private void updateTop() {
        double y = model.getImageRect().getY();
        LengthUnit unit = topUnit.getSelectedUnit();
        y = unit.fromPoints(y);
        topText.setUnit(unit);
        topText.setDimension(y);
    }

    private void updateLeft() {
        double x = model.getImageRect().getX();
        LengthUnit unit = leftUnit.getSelectedUnit();
        x = unit.fromPoints(x);
        leftText.setUnit(unit);
        leftText.setDimension(x);
    }

    public void layoutChanged(PrintLayoutModel source) {
        if (! writingToModel) {
            readingFromModel = true;
            updateLeft();
            updateTop();
            readingFromModel = false;
        }
    }

    public static void main(String[] args) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new PositionPanel(new PrintLayoutModel(100, 100)));
        JFrame frame = new JFrame("PositionPanel Test");
        frame.setContentPane(panel);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);
    }
}
