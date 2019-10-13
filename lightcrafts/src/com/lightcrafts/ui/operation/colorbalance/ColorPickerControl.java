/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.colorbalance;

import com.lightcrafts.model.ColorPickerOperation;
import com.lightcrafts.model.Operation;
import com.lightcrafts.ui.help.HelpConstants;
import com.lightcrafts.ui.operation.OpStack;
import static com.lightcrafts.ui.operation.colorbalance.Locale.LOCALE;
import com.lightcrafts.ui.operation.generic.GenericControl;
import com.lightcrafts.ui.swing.ColorSwatch;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * An OpControl to hold a ColorPickerOperation in an OpStack.  Provides
 * a color wheel to pick a color directly, and updates sliders accordingly.
 */
public class ColorPickerControl extends GenericControl {

    private ColorSwatch swatch;
    private ColorText text;
    private ColorWheel wheel;
    protected Box colorContent;

    // Track batch changes
    private boolean wasChanging;

    // Flag color wheel picks that just synchronize the color wheel with
    // a change commanded througha slider or the dropper:
    private boolean isColorWheelUpdating;

    public ColorPickerControl(ColorPickerOperation op, OpStack stack) {
        super(op, stack);
    }

    protected void operationChanged(Operation operation) {
        super.operationChanged(operation);

        Color color = ((ColorPickerOperation) operation).getColor();
        swatch = new ColorSwatch(color);
        text = new ColorText(color);

        wheel = new ColorWheel();
        ColorWheelMouseListener listener = new ColorWheelMouseListener(wheel) {
            void colorPicked(Color color, boolean isChanging) {
                // Colors from the ColorWheel are always fully bright.
                // This merges wheel color H and S with operation color B:
                color = getAdjustedWheelColor();
                setColor(color, isChanging);
            }
        };
        wheel.addMouseListener(listener);
        wheel.addMouseMotionListener(listener);

        colorContent = Box.createHorizontalBox();
        colorContent.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        colorContent.add(wheel);
        colorContent.add(Box.createHorizontalStrut(8));
        colorContent.add(swatch);
        colorContent.add(Box.createHorizontalStrut(8));
        colorContent.add(text);
        colorContent.add(Box.createHorizontalStrut(8));

        JComponent content = getContent();
        content.add(colorContent, 0);
        content.add(Box.createVerticalStrut(8), 0);
    }

    private void setColor(Color color, boolean isChanging) {
        if (isChanging && ! wasChanging) {
            Operation op = getOperation();
            op.changeBatchStarted();
        }
        else if (wasChanging && ! isChanging) {
            Operation op = getOperation();
            op.changeBatchEnded();
        }
        wasChanging = isChanging;

        ColorPickerOperation op = (ColorPickerOperation) getOperation();
        Map<String, Double> map = op.setColor(color);
        updateColor(map);
        if (! undoSupport.isRestoring() && ! isChanging) {
            undoSupport.postEdit(LOCALE.get("SetColorEditName"));
        }
    }

    private Color getColor() {
        return ((ColorPickerOperation) getOperation()).getColor();
    }

    protected void updateColor(Map<String, Double> sliders) {
        for (String key : sliders.keySet()) {
            double value = sliders.get(key);
            slewSlider(key, value);
        }
        Color color = getColor();
        swatch.setColor(color);
        text.setColor(color);
        if (! isColorWheelUpdating) {
            isColorWheelUpdating = true;
            wheel.pickColor(color);
            isColorWheelUpdating = false;
        }
    }

    // Colors from the Color wheel are always fully bright.  This method merges
    // the hue and saturation from the ColorWheel and the brightness from the
    // ColorPickerOperation into a third Color.
    private Color getAdjustedWheelColor() {
        Color wheelColor = wheel.getPickedColor();
        Color opColor = ((ColorPickerOperation) getOperation()).getColor();
        float[] wheelHsb = Color.RGBtoHSB(
            wheelColor.getRed(),
            wheelColor.getGreen(),
            wheelColor.getBlue(),
            null
        );
        float[] opHsb = Color.RGBtoHSB(
            opColor.getRed(),
            opColor.getGreen(),
            opColor.getBlue(),
            null
        );
        int rgb = Color.HSBtoRGB(wheelHsb[0], wheelHsb[1], opHsb[2]);
        return new Color(rgb);
    }

    private final static String ColorTag = "Color";

    public void save(XmlNode node) {
        super.save(node);
        Color color = getColor();
        XmlNode colorNode = node.addChild(ColorTag);
        colorNode.setAttribute("r", Integer.toString(color.getRed()));
        colorNode.setAttribute("g", Integer.toString(color.getGreen()));
        colorNode.setAttribute("b", Integer.toString(color.getBlue()));
    }

    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        XmlNode whiteNode = node.getChild(ColorTag);
        int r = Integer.parseInt(whiteNode.getAttribute("r"));
        int g = Integer.parseInt(whiteNode.getAttribute("g"));
        int b = Integer.parseInt(whiteNode.getAttribute("b"));
        Color color = new Color(r, g, b);
        undoSupport.restoreStart();
        setColor(color, false);
        undoSupport.restoreEnd();
    }

    protected String getHelpTopic() {
        return HelpConstants.HELP_TOOL_BLACK_AND_WHITE;
    }
}
