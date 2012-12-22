/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import com.lightcrafts.ui.print.PrintLayoutModel.LengthUnit;

import static com.lightcrafts.ui.print.Locale.LOCALE;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

class SizePanel extends JPanel implements PrintLayoutModelListener {

    private PrintLayoutModel model;

    private PpiComboBox scaleCombo;
    private ScaleTextField scaleText;
    private JButton scaleButton;

    private DimensionTextField heightText;
    private UnitComboBox heightUnit;
    private DimensionTextField widthText;
    private UnitComboBox widthUnit;

    private boolean readingFromModel;   // prevent update loops
    private JComponent writingToModel;  // whichever pushed a change

    private JPanel titlePanel;  // intermediary container allows title borders

    SizePanel(PrintLayoutModel model) {
        this.model = model;

        titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        Border border = BorderFactory.createTitledBorder(
            LOCALE.get("SizeTitle")
        );
        titlePanel.setBorder(border);

        addPpi();
        add(Box.createVerticalStrut(3));
        addScale();
        titlePanel.add(Box.createVerticalStrut(3));
        addWidth();
        titlePanel.add(Box.createVerticalStrut(3));
        addHeight();

        model.addListener(this);

        setLayout(new BorderLayout());
        add(titlePanel);
    }

    private void addPpi() {
        scaleCombo = new PpiComboBox();
        updatePpi();

        scaleCombo.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        int ppi = scaleCombo.getPpi();
                        model.setPpi(ppi);
                        updateScale();
                    }
                }
            }
        );
        Box box = createLabelledText(LOCALE.get("PpiLabel"), scaleCombo);
        titlePanel.add(box);
    }

    private void addScale() {
        scaleText = new ScaleTextField();
        updateScale();

        scaleText.setListener(
            new ScaleTextField.Listener() {
                public void scaleChanged(double scale) {
                    if (! readingFromModel) {
                        writingToModel = scaleText;
                        model.setScale(scale);
                        writingToModel = null;
                    }
                }
            }
        );

        scaleButton = new JButton(LOCALE.get("ScaleButton"));
        scaleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    model.scaleToFit();
                }
            }
        );
        Box box = createLabelledText(
            LOCALE.get("ScaleLabel"), scaleText, scaleButton
        );
        titlePanel.add(box);
    }

    private void addHeight() {
        heightText = new DimensionTextField();
        heightUnit = new UnitComboBox();
        updateHeight();

        heightText.setListener(
            new DimensionTextField.Listener() {
                public void dimensionChanged(double height) {
                    if (! readingFromModel) {
                        writingToModel = heightText;
                        LengthUnit unit = heightUnit.getSelectedUnit();
                        model.setImageHeight(height, unit);
                        writingToModel = null;
                    }
                }
            }
        );
        syncTextWithUnit(heightText, heightUnit);

        Box box = createLabelledText(
            LOCALE.get("HeightLabel"), heightText, heightUnit
        );
        titlePanel.add(box);
    }

    private void addWidth() {
        widthText = new DimensionTextField();
        widthUnit = new UnitComboBox();
        updateWidth();

        widthText.setListener(
            new DimensionTextField.Listener() {
                public void dimensionChanged(double width) {
                    if (! readingFromModel) {
                        writingToModel = widthText;
                        LengthUnit unit = widthUnit.getSelectedUnit();
                        model.setImageWidth(width, unit);
                        writingToModel = null;
                    }
                }
            }
        );
        syncTextWithUnit(widthText, widthUnit);

        Box box = createLabelledText(
            LOCALE.get("WidthLabel"), widthText, widthUnit
        );
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
        String name, JComponent text, JComponent units
    ) {
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        box.add(new JLabel(name));
        box.add(Box.createHorizontalStrut(3));
        box.add(text);
        box.add(Box.createHorizontalStrut(3));
        box.add(units);
        return box;
    }

    private static Box createLabelledText(
        String name, JComponent text
    ) {
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        box.add(new JLabel(name + ':'));
        box.add(Box.createHorizontalStrut(3));
        box.add(text);
        return box;
    }

    private void updatePpi() {
        int ppi = model.getPpi();
        scaleCombo.setPpi(ppi);
    }

    private void updateScale() {
        double scale = model.getScale();
        scaleText.setScale(scale);
    }

    private void updateHeight() {
        double h = model.getImageRect().getHeight();
        LengthUnit unit = (LengthUnit) heightUnit.getSelectedItem();
        h = unit.fromPoints(h);
        heightText.setUnit(unit);
        heightText.setDimension(h);
    }

    private void updateWidth() {
        double w = model.getImageRect().getWidth();
        LengthUnit unit = (LengthUnit) widthUnit.getSelectedItem();
        w = unit.fromPoints(w);
        widthText.setUnit(unit);
        widthText.setDimension(w);
    }

    public void layoutChanged(PrintLayoutModel source) {
        readingFromModel = true;
        if (writingToModel != widthText) {
            updateWidth();
        }
        if (writingToModel != heightText) {
            updateHeight();
        }
        if (writingToModel != scaleText) {
            updateScale();
        }
        readingFromModel = false;
    }

    public static void main(String[] args) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new SizePanel(new PrintLayoutModel(100, 100)));
        JFrame frame = new JFrame("SizePanel Test");
        frame.setContentPane(panel);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);
    }
}
