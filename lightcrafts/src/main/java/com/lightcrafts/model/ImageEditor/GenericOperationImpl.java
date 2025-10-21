/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.GenericOperation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.jai.utils.Transform;

import org.eclipse.imagen.PlanarImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** A ImageEditor implementation of GenericOperation that does nothing when its
  * settings are updated.
  */

abstract class GenericOperationImpl extends OperationImpl implements GenericOperation {
    OperationType type;

    private List<String> sliderKeys;
    private List<String> checkboxKeys;
    private List<String> choiceKeys;

    private Map<String, List<String>> choiceValues;
    private Map<String, SliderConfig> sliderConfigs;

    @Getter @Setter (AccessLevel.PROTECTED)
    private String helpTopic = null; // null leads to the help home page.

    GenericOperationImpl(Rendering rendering, OperationType type) {
        super(rendering, type.getName());
        this.type = type;
        sliderKeys = new ArrayList<String>();
        checkboxKeys = new ArrayList<String>();
        choiceKeys = new ArrayList<String>();
        choiceValues = new HashMap<String, List<String>>();
        sliderConfigs = new HashMap<String, SliderConfig>();
    }

    @Override
    public OperationType getType() {
        return type;
    }

    void addSliderKey(String key) {
        sliderKeys.add(key);
        sliderConfigs.put(key, new SliderConfig());
    }

    void addCheckboxKey(String key) {
        checkboxKeys.add(key);
    }

    void addChoiceKey(String key) {
        choiceKeys.add(key);
        choiceValues.put(key, new ArrayList<String>());
    }

    void addChoiceValue(String key, String value) {
        choiceValues.get(key).add(value);
    }

    void addChoiceValues(String key, List<String> values) {
        choiceValues.get(key).addAll(values);
    }

    void clearChoiceValues(String key) {
        choiceValues.get(key).clear();
    }

    void setCheckboxKeys(List<String> keys) {
        checkboxKeys = keys;
    }

    @Override
    public List<String> getSliderKeys() {
        return new ArrayList<String>(sliderKeys);
    }

    @Override
    public List<String> getCheckboxKeys() {
        return new ArrayList<String>(checkboxKeys);
    }

    @Override
    public List<String> getChoiceKeys() {
        return new ArrayList<String>(choiceKeys);
    }

    @Override
    public List<String> getChoiceValues(String key) {
        return new ArrayList<String>(choiceValues.get(key));
    }

    public double roundValue(String key, double value) {
        SliderConfig sliderConfig = getSliderConfig(key);
        double increment = sliderConfig.getIncrement();
        return Math.round(value / increment) * increment;
    }

    @Override
    public void setSliderValue(String key, double value) {
        // System.out.println(getName() + " updated: " + key + " = " + value);
        settingsChanged();
    }

    @Override
    public void setCheckboxValue(String key, boolean value) {
        // System.out.println(getName() + " updated: " + key + " = " + value);
        settingsChanged();
    }

    @Override
    public void setChoiceValue(String key, String value) {
        // System.out.println(getName() + " updated: " + key + " = " + value);
        settingsChanged();
    }

    void setSliderConfig(String key, SliderConfig config) {
        sliderConfigs.put(key, config);
    }

    @Override
    public SliderConfig getSliderConfig(String key) {
        return sliderConfigs.get(key);
    }

    @Override
    abstract protected void updateOp(Transform op);

    @Override
    abstract protected Transform createOp(PlanarImage source);
}
