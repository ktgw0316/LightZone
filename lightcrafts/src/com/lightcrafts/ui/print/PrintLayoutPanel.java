/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

class PrintLayoutPanel extends JPanel {

    private PreviewPanel preview;
    private ColorManagementPanel color;
    private PositionPanel position;
    private SizePanel size;
    private JCheckBox centered;
    private ButtonPanel buttons;

    PrintLayoutPanel(PrintLayoutModel model, BufferedImage image) {
        preview = new PreviewPanel(image, model);
        color = new ColorManagementPanel(model);
        position = new PositionPanel(model);
        size = new SizePanel(model);
        centered = new CenteredCheckBox(model);
        buttons = new ButtonPanel();

        Box numbersBox = Box.createVerticalBox();
        numbersBox.add(position);
        numbersBox.add(size);

        addEmptyBorder(this);
        addEmptyBorder(preview);
        addEmptyBorder(color);
        addEmptyBorder(position);
        addEmptyBorder(size);
        addEmptyBorder(centered);
        addEmptyBorder(buttons);

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 2;
        add(preview, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridheight = 1;
        c.gridwidth = 2;
        add(color, c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 2;
        add(numbersBox, c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        box.add(centered);
        box.add(Box.createHorizontalGlue());
        add(box, c);

        c.gridx = 2;
        c.gridy = 2;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.EAST;
        add(buttons, c);

        registerKeyboardActions();
    }

    void addPrintAction(ActionListener action) {
        buttons.addPrintAction(action);
    }

    void addCancelAction(ActionListener action) {
        buttons.addCancelAction(action);
    }

    void addDoneAction(ActionListener action) {
        buttons.addDoneAction(action);
    }

    void addPageSetupAction(ActionListener action) {
        buttons.addPrintSetupAction(action);
    }

    private static void addEmptyBorder(JComponent comp) {
        Border oldBorder = comp.getBorder();
        Border empty = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        Border newBorder;
        if (oldBorder != null) {
            newBorder = BorderFactory.createCompoundBorder(empty, oldBorder);
        }
        else {
            newBorder = empty;
        }
        comp.setBorder(newBorder);
    }

    private void registerKeyboardActions() {
        ActionListener commitAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                buttons.doDoneActions();
            }
        };
        ActionListener cancelAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                buttons.doCancelActions();
            }
        };
        registerKeyboardAction(
            commitAction,
            "Commit",
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
        registerKeyboardAction(
            cancelAction,
            "Cancel",
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(new File(args[0]));
        JPanel panel = new JPanel(new BorderLayout());
        int width = image.getWidth();
        int height = image.getHeight();
        PrintLayoutModel model = new PrintLayoutModel(width, height);
        PrintLayoutPanel test = new PrintLayoutPanel(model, image);
        panel.add(test);
        JFrame frame = new JFrame("PositionPanel Test");
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
