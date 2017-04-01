/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.generic;

import com.lightcrafts.model.GenericOperation;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.ui.help.HelpConstants;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.*;

public class GenericControl extends OpControl {

    // This resource bundle holds the user presentable forms of the
    // GenericOperation slider and checkbox keys.  (Choice keys are still
    // not localizable.)
    //
    // The format for the properties file is:
    //
    //     operationTypeName-keyString=userPresentableKeyString
    //
    // where "operationTypeName" is the String returned by
    // GenericOperation.getOperationType().getName().replaceAll(" ", "").

    private final static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/ui/operation/generic/GenericControl"
    );

    private GenericOperation op;

    // GenericOperation settings keys mapped to their control Components:

    private Map sliders = new HashMap();        // Strings to GenericSliders
    private Map checkboxes = new HashMap();     // Strings to JCheckBoxes
    private Map choices = new HashMap();        // Strings to JComboBoxes

    public GenericControl(GenericOperation op, OpStack stack) {
        super(op, stack);
        operationChanged(op);
        readyForUndo();
    }

    protected void operationChanged(Operation operation) {
        super.operationChanged(operation);

        this.op = (GenericOperation) operation;

        Box box = Box.createVerticalBox();

        box.add(Box.createVerticalStrut(6));

        // Add all the sliders:

        // A special layout that aligns the GenericSlider pieces in rows
        // and columns:
        GenericSliderContainer sliderContainer = new GenericSliderContainer();

        List sliderKeys = op.getSliderKeys();
        for (Iterator i=sliderKeys.iterator(); i.hasNext(); ) {
            final String key = (String) i.next();
            final String userKey = getUserPresentableKey(key);
            final SliderConfig config = op.getSliderConfig(key);
            final GenericSlider slider = new GenericSlider(userKey, config);
            slider.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent event) {
                        double value = slider.getConfiguredValue();
                        op.setSliderValue(key, value);
                    }
                }
            );
            GenericSlider oldSlider = (GenericSlider) sliders.get(key);
            if (oldSlider != null) {
                slider.setConfiguredValue(oldSlider.getConfiguredValue());
            }
            slider.addSliderMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent event) {
                        op.changeBatchStarted();
                    }
                    public void mouseReleased(MouseEvent event) {
                        op.changeBatchEnded();
                        undoSupport.postEdit(key + " Slider");
                    }
                }
            );
            slider.setBackgroundRecurse(Background);
            slider.setFontRecurse(ControlFont);
            sliderContainer.addGenericSlider(slider);

            sliders.put(key, slider);
        }
        sliderContainer.setBackground(Background);
        box.add(sliderContainer);

        // Add all the checkboxes:

        List checkboxKeys = op.getCheckboxKeys();
        for (Iterator i=checkboxKeys.iterator(); i.hasNext(); ) {
            final String key = (String) i.next();
            final String userKey = getUserPresentableKey(key);
            final JCheckBox checkbox = new JCheckBox(userKey);
            checkbox.addItemListener(
                new ItemListener() {
                    public void itemStateChanged(ItemEvent event) {
                        boolean value = checkbox.isSelected();
                        op.setCheckboxValue(key, value);
                        undoSupport.postEdit(key + " Checkbox");
                    }
                }
            );
            JCheckBox oldCheckbox = (JCheckBox) checkboxes.get(key);
            if (oldCheckbox != null) {
                checkbox.setSelected(oldCheckbox.isSelected());
            }
            checkbox.setBackground(Background);
            checkbox.setFont(ControlFont);
            box.add(checkbox);

            checkboxes.put(key, checkbox);
        }

        // Add all the choices:

        List choiceKeys = op.getChoiceKeys();
        for (Iterator i=choiceKeys.iterator(); i.hasNext(); ) {
            final String key = (String) i.next();
            Vector values = new Vector(op.getChoiceValues(key));
            final JComboBox choice = new JComboBox(values);
            choice.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        String value = (String) choice.getSelectedItem();
                        op.setChoiceValue(key, value);
                        undoSupport.postEdit(key + " Choice");
                    }
                }
            );
            choice.addMouseWheelListener(
                new MouseWheelListener() {
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        JComboBox source = (JComboBox) e.getComponent();
                        if (!source.hasFocus()) {
                            return;
                        }
                        int ni = source.getSelectedIndex() + e.getWheelRotation();
                        if (ni >= 0 && ni < source.getItemCount()) {
                            source.setSelectedIndex(ni);
                        }
                    }
                }
            );
            JComboBox oldChoice = (JComboBox) choices.get(key);
            if (oldChoice != null) {
                choice.setSelectedItem(oldChoice.getSelectedItem());
            }
            choice.setBackground(Background);
            choice.setFont(ControlFont);
            box.add(choice);

            choices.put(key, choice);
        }
        box.add(Box.createVerticalStrut(6));

        setContent(box);

        undoSupport.initialize();
    }

    protected void slewSlider(String key, double value) {
        GenericSlider slider = (GenericSlider) sliders.get(key);
        if (slider != null) {
            slider.setConfiguredValue(value);
        }
    }

    // Find the user presentable version of the given slider or
    // checkbox key in the properties.  If none is configured, just
    // return the given String.
    private String getUserPresentableKey(String key) {
        OperationType type = op.getType();
        String name = type.getName();
        name = name.replaceAll(" ", "").replaceAll("V[0-9]+\\Z", "");
        String propKey = name + "-" + key;
        try {
            return Resources.getString(propKey);
        }
        catch (MissingResourceException e) {
            return key;
        }
    }

    private final static String SliderTag = "Slider";
    private final static String CheckBoxTag = "Checkbox";
    private final static String ChoiceTag = "Choice";

    public void save(XmlNode node) {
        super.save(node);
        Set keys;
        XmlNode sliderNode = node.addChild(SliderTag);
        keys = sliders.keySet();
        for (Iterator i=keys.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            GenericSlider slider = (GenericSlider) sliders.get(key);
            double value = slider.getConfiguredValue();
            sliderNode.setAttribute(key, Double.toString(value));
        }
        XmlNode checkboxNode = node.addChild(CheckBoxTag);
        keys = checkboxes.keySet();
        for (Iterator i=keys.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            JCheckBox checkbox = (JCheckBox) checkboxes.get(key);
            boolean value = checkbox.isSelected();
            checkboxNode.setAttribute(key, value ? "True" : "False");
        }
        XmlNode choiceNode = node.addChild(ChoiceTag);
        keys = choices.keySet();
        for (Iterator i=keys.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            JComboBox choice = (JComboBox) choices.get(key);
            String value = (String) choice.getSelectedItem();
            choiceNode.setAttribute(key, value);
        }
    }

    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        undoSupport.restoreStart();
        op.changeBatchStarted();
        Set keys;
        if (node.hasChild(SliderTag)) {
            XmlNode sliderNode = node.getChild(SliderTag);
            keys = sliders.keySet();
            for (Iterator i=keys.iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                GenericSlider slider = (GenericSlider) sliders.get(key);
                try {
                    int version = sliderNode.getVersion();
                    if ((version >= 3) || (version < 0)) {
                        double value = Double.parseDouble(
                            sliderNode.getAttribute(key)
                        );
                        slider.setConfiguredValue(value);
                    }
                    else {
                        int value = Integer.parseInt(sliderNode.getAttribute(key));
                        slider.setSliderPosition(value);
                    }
                }
                catch (NumberFormatException e) {
                    throw new XMLException(
                        "Value at attribute \"" + key + "\" is not a number", e
                    );
                }
            }
        }
        if (node.hasChild(CheckBoxTag)) {
            XmlNode checkboxNode = node.getChild(CheckBoxTag);
            keys = checkboxes.keySet();
            for (Iterator i=keys.iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                JCheckBox checkbox = (JCheckBox) checkboxes.get(key);
                String value = checkboxNode.getAttribute(key);
                checkbox.setSelected(value.equals("True"));
            }
        }
        if (node.hasChild(ChoiceTag)) {
            XmlNode choiceNode = node.getChild(ChoiceTag);
            keys = choices.keySet();
            for (Iterator i=keys.iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                JComboBox choice = (JComboBox) choices.get(key);
                String value = choiceNode.getAttribute(key);
                choice.setSelectedItem(value);
            }
        }
        op.changeBatchEnded();
        undoSupport.restoreEnd();
    }

    // This is a crude mapping from GenericOperation OperationType names
    // (as found, for instance, in opActions.properties) into help topics
    // (as defined in HelpConstants).
    //
    // This mapping needs maintenance, as tools come and go.
    protected String getHelpTopic() {
        OperationType type = op.getType();
        String name = type.getName();
        if (name.startsWith("ZoneMapper")) {
            return HelpConstants.HELP_TOOL_ZONEMAPPER;
        }
        if (name.startsWith("UnSharp Mask")) {
            return HelpConstants.HELP_TOOL_SHARPEN;
        }
        if (name.startsWith("Gaussian Blur")) {
            return HelpConstants.HELP_TOOL_BLUR;
        }
        if (name.startsWith("Hue/Saturation")) {
            return HelpConstants.HELP_TOOL_HUE_SATURATION;
        }
        if (name.startsWith("Color Balance")) {
            return HelpConstants.HELP_TOOL_COLOR_BALANCE;
        }
        if (name.startsWith("White Point")) {
            return HelpConstants.HELP_TOOL_WHITE_BALANCE;
        }
        if (name.startsWith("Channel Mixer")) {
            return HelpConstants.HELP_TOOL_BLACK_AND_WHITE;
        }
        if (name.startsWith("Advanced Noise Reduction")) {
            return HelpConstants.HELP_TOOL_NOISE_REDUCTION;
        }
        if (name.startsWith("Clone")) {
            return HelpConstants.HELP_TOOL_CLONE;
        }
        if (name.startsWith("Spot")) {
            return HelpConstants.HELP_TOOL_SPOT;
        }
        if (name.startsWith("RAW Adjustments")) {
            return HelpConstants.HELP_TOOL_RAW_ADJUSTMENTS;
        }
        if (name.startsWith("Relight") || name.startsWith("Tone")) {
            return HelpConstants.HELP_TOOL_RELIGHT;
        }
        if (name.startsWith("Red Eyes")) {
            return HelpConstants.HELP_TOOL_RED_EYE;
        }
        // This null leads to the help home page.
        return null;
    }
}
