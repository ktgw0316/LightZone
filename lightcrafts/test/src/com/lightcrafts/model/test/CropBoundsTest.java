/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.test;

import javax.swing.*;
import java.awt.*;

public class CropBoundsTest {

    public static void main(String[] args) throws Exception {

        // Initialize the crop controls, crop renderer, and image renderer:
        CropBoundsControls controls = new CropBoundsControls();
        CropBoundsRenderer cropRenderer = new CropBoundsRenderer(args[0]);

        // The test image renderer (always works):
        CroppedImageRenderer testRenderer =
            new TestCroppedImageRenderer(args[0]);

        // The Engine image renderer (needs fixing):
        CroppedImageRenderer engineRenderer =
            new EngineCroppedImageRenderer(args[0]);

        // Hook up the renderers to the controls:
        controls.addListener(cropRenderer);
        controls.addListener(testRenderer);
        controls.addListener(engineRenderer);

        // Make the input frame container:
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(cropRenderer);
        inputPanel.add(controls, BorderLayout.SOUTH);

        // Make the Engine output frame container:
        JPanel enginePanel = new JPanel(new BorderLayout());
        enginePanel.add(engineRenderer.getComponent());

        // Make the test output frame container:
        JPanel testPanel = new JPanel(new BorderLayout());
        testPanel.add(testRenderer.getComponent());

        // Show the input frame:
        JFrame inputFrame = new JFrame("Controls");
        inputFrame.setContentPane(inputPanel);
        inputFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        inputFrame.setBounds(100, 100, 400, 400);
        inputFrame.setVisible(true);

        // Show the Engine output frame:
        JFrame engineFrame = new JFrame("EngineImageRenderer");
        engineFrame.setContentPane(enginePanel);
        engineFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        engineFrame.setBounds(500, 100, 400, 400);
        engineFrame.setVisible(true);

        // Show the test output frame:
        JFrame testFrame = new JFrame("TestImageRenderer");
        testFrame.setContentPane(testPanel);
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setBounds(900, 100, 400, 400);
        testFrame.setVisible(true);
    }
}
