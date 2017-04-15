/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.whitebalance;

import com.lightcrafts.model.Operation;
import com.lightcrafts.model.RawAdjustmentOperation;
import com.lightcrafts.ui.operation.OpStack;
import static com.lightcrafts.ui.operation.whitebalance.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class RawAdjustmentControl extends ColorDropperControl {

    public RawAdjustmentControl(RawAdjustmentOperation op, OpStack stack) {
        super(op, stack);
    }

    protected void operationChanged(Operation operation) {
        super.operationChanged(operation);

        final RawAdjustmentOperation raw = (RawAdjustmentOperation) operation;

//        JButton auto =
//           new CoolButton();
//        auto.setText(LOCALE.get("AutoButtonText"));
//        auto.addActionListener(
//        auto.addActionListener(
//            new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    Map<String, Double> sliders = raw.getAuto();
//                    for (String key : sliders.keySet()) {
//                        double value = sliders.get(key);
//                        slewSlider(key, value);
//                    }
//                    if (! undoSupport.isRestoring()) {
//                        undoSupport.postEdit(
//                            LOCALE.get("RawAutoEditName")
//                        );
//                    }
//                }
//            }
//        );

        JButton asShot = new CoolButton();
        asShot.setText(LOCALE.get("AsShotButtonText"));
        asShot.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Map<String, Double> sliders = raw.getAsShot();
                    for (String key : sliders.keySet()) {
                        double value = sliders.get(key);
                        slewSlider(key, value);
                    }
                    if (! undoSupport.isRestoring()) {
                        undoSupport.postEdit(
                            LOCALE.get("RawAsShotEditName")
                        );
                    }
                }
            }
        );
        Box buttons = Box.createHorizontalBox();
//        buttons.add(auto);
//        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(asShot);

        Box colorContent = getColorContent();
        colorContent.add(Box.createHorizontalStrut(8), 0);
//        colorContent.add(auto, 1);
//        colorContent.add(Box.createHorizontalStrut(8), 2);
        colorContent.add(asShot, 1);
        colorContent.add(Box.createHorizontalStrut(8), 2);
    }
}
