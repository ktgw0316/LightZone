/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.GenericOperation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.jai.utils.Transform;

import javax.media.jai.PlanarImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A ImageEditor implementation of GenericOperation that does nothing when its
  * settings are updated.
  */

abstract class GenericOperationImpl extends OperationImpl implements GenericOperation {
    OperationType type;

    List<String> sliderKeys;
    List<String> checkboxKeys;
    List<String> choiceKeys;

    Map<String, List<String>> choiceValues;       // Maps choiceKeys Strings to Lists of String values.
    Map<String, SliderConfig> sliderConfigs;      // Maps sliderKeys Strings to SliderConfigs.

    protected GenericOperationImpl(Rendering rendering, OperationType type) {
        super(rendering, type.getName());
        this.type = type;
        sliderKeys = new ArrayList<String>();
        checkboxKeys = new ArrayList<String>();
        choiceKeys = new ArrayList<String>();
        choiceValues = new HashMap<String, List<String>>();
        sliderConfigs = new HashMap<String, SliderConfig>();
    }

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

    void setCheckboxKeys(List<String> keys) {
        checkboxKeys = keys;
    }

    public List<String> getSliderKeys() {
        return new ArrayList<String>(sliderKeys);
    }

    public List<String> getCheckboxKeys() {
        return new ArrayList<String>(checkboxKeys);
    }

    public List<String> getChoiceKeys() {
        return new ArrayList<String>(choiceKeys);
    }

    public List<String> getChoiceValues(String key) {
        return new ArrayList<String>(choiceValues.get(key));
    }

    public double roundValue(String key, double value) {
        SliderConfig sliderConfig = getSliderConfig(key);
        double increment = sliderConfig.getIncrement();
        return Math.round(value / increment) * increment;
    }

    public void setSliderValue(String key, double value) {
        // System.out.println(getName() + " updated: " + key + " = " + value);
        settingsChanged();
    }

    public void setCheckboxValue(String key, boolean value) {
        // System.out.println(getName() + " updated: " + key + " = " + value);
        settingsChanged();
    }

    public void setChoiceValue(String key, String value) {
        // System.out.println(getName() + " updated: " + key + " = " + value);
        settingsChanged();
    }

    void setSliderConfig(String key, SliderConfig config) {
        sliderConfigs.put(key, config);
    }

    public SliderConfig getSliderConfig(String key) {
        return sliderConfigs.get(key);
    }

    abstract protected void updateOp(Transform op);

    abstract protected Transform createOp(PlanarImage source);
}
