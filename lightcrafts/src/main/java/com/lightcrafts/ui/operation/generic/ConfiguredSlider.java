/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.generic;

import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.ui.toolkit.LCSliderUI;

import javax.swing.*;

/**
 * A JSlider modelled by a ConfiguredBoundedRangeModel, a package-private
 * class implementing BoundedRangeModel.
 */

class ConfiguredSlider extends JSlider {

    private ConfiguredBoundedRangeModel model;

    ConfiguredSlider(SliderConfig config) {
        this.setUI(new LCSliderUI(this));
        model = new ConfiguredBoundedRangeModel(config);
        setModel(model);
    }

    ConfiguredBoundedRangeModel getConfiguredModel() {
        return (ConfiguredBoundedRangeModel) getModel();
    }

    /** ConfiguredSlider uses a ConfiguredBoundedRangeModel, a
     * package-protected class.  This method will throw a ClassCastException
     * if it gets an incompatible BoundedRangeModel.
     * @param model An instance of ConfiguredBoundedRangeModel
     * @throws ClassCastException If the model argument is of the wrong class
     */
    public void setModel(BoundedRangeModel model) throws ClassCastException {
        super.setModel(model);
    }
}
