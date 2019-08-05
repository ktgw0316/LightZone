/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.image.color.ColorProfileInfo;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import org.jvnet.substance.SubstanceLookAndFeel;

/**
 * There is a problem with the popup component in JComboBox components under
 * the windows and linux PLAFs: the popup width is constrained by the combo
 * box width, so the full menu items in combo popups are commonly unreadable.
 * <p>
 * This happens for us when we are letting the user select a color profile.
 * <p>
 * This JComboBox overrides the JComboBox component UI for the popup, just
 * enough to make the popup have the right width, on windows and linux only.
 * <p>
 * It also overrides setSelectedItem() to interpret null items as separators,
 * and to guard against the separator items being selected.
 */
public class WidePopupComboBox extends JComboBox {

    public WidePopupComboBox() {
        super(
            new DefaultComboBoxModel() {
                /**
                 * Don't let null items in the list become selected.
                 */
                public void setSelectedItem(Object o) {
                    if (o != null) {
                        super.setSelectedItem(o);
                    }
                }
            }
        );
//        if (Platform.getType() == Platform.Linux) {
//            setUI(
//                new MetalComboBoxUI() {
//                    protected ComboPopup createPopup() {
//                        return new BasicComboPopup(WidePopupComboBox.this) {
//                            protected Rectangle computePopupBounds(
//                                int px, int py, int pw, int ph
//                                ) {
//                                Rectangle bounds = super.computePopupBounds(
//                                    px, py, pw, ph
//                                );
//                                bounds.width = getPreferredSize().width;
//                                return bounds;
//                            }
//                        };
//                    }
//                }
//            );
//        }
//        if (Platform.getType() == Platform.Windows) {
//            setUI(
//                new WindowsComboBoxUI() {
//                    protected ComboPopup createPopup() {
//                        return new BasicComboPopup(WidePopupComboBox.this) {
//                            protected Rectangle computePopupBounds(
//                                int px, int py, int pw, int ph
//                                ) {
//                                Rectangle bounds = super.computePopupBounds(
//                                    px, py, pw, ph
//                                );
//                                bounds.width = getPreferredSize().width;
//                                return bounds;
//                            }
//                        };
//                    }
//                }
//            );
//        }
        setRenderer(new ComboBoxRenderer());
    }

    private int maxItemLenght = 0;
    public void addItem(Object anObject) {
        super.addItem(anObject);

        if (anObject != null) {
            int lenght = anObject.toString().length();
            if (lenght > maxItemLenght) {
                maxItemLenght = lenght;
                putClientProperty(SubstanceLookAndFeel.COMBO_POPUP_PROTOTYPE, anObject);
            }
        }
    }

    /**
     * A ListCellRenderer that shows null items as separators.
     */
    static class ComboBoxRenderer extends DefaultListCellRenderer {

        final static Component Separator = new JSeparator();

        static {
            Separator.setSize(Separator.getPreferredSize());
        }

        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            if (value == null) {
                return Separator;
            }
            else {
                return super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
                );
            }
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        WidePopupComboBox combo = new WidePopupComboBox();
        Collection PrinterProfiles =
            Platform.getPlatform().getPrinterProfiles();
        List<ColorProfileInfo> profiles =
            ColorProfileInfo.arrangeForMenu(PrinterProfiles);
        for (ColorProfileInfo profile : profiles) {
            combo.addItem(profile);
        }
        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(combo);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
