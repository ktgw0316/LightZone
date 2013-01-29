/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.ImageMetadata;
import static com.lightcrafts.ui.metadata2.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * A tall component that goes alongside a MetadataTable and holds the button
 * that lets a user specify that the IPTC copyright field should be assigned
 * from its preferences default.
 */
class DefaultButtons extends JPanel {

    private final static Icon icon = IconFactory.createInvertedIcon(
        DefaultButtons.class, "left_arrow.png", 12
    );

    // We need to know the size of the buttons to calculate our preferred size.
    private final static Dimension ButtonSize;
    static {
        CoolButton button = new CoolButton();
        button.setIcon(icon);
        ButtonSize = button.getPreferredSize();
    }
    private MetadataTable table;

    private ImageMetadata meta;

    // Buttons to set the default value on every entry that has a default
    private Map<MetadataEntry, JButton> buttons;

    DefaultButtons(MetadataTable table, ImageMetadata meta) {
        this.table = table;
        this.meta = meta;

        setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        setOpaque(true);

        setLayout(null);

        initButtons();
        updateButtons();

        // The button location must track the table layout.
        table.addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    updateButtons();
                    repaint();
                }
            }
        );
    }

    public Dimension getPreferredSize() {
        return new Dimension(
            ButtonSize.width, table.getPreferredSize().height
        );
    }

    private void initButtons() {
        buttons = new HashMap<MetadataEntry, JButton>();
        final MetadataTableModel model = (MetadataTableModel) table.getModel();
        for (int row=0; row<model.getRowCount(); row++) {
            final MetadataEntry entry = model.getEntryAt(row);
            if (entry.hasDefaultValue()) {
                JButton button = new CoolButton();
                button.setIcon(icon);
                button.setToolTipText(LOCALE.get("DefaultButtonToolTip"));
                buttons.put(entry, button);
                button.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            entry.setDefaultValue(meta);
                            model.commit();
                            table.repaint();
                        }
                    }
                );
                add(button);
                button.setEnabled(true);
            }
        }
    }

    private void updateButtons() {
        MetadataTableModel model = (MetadataTableModel) table.getModel();
        int rowHeight = table.getRowHeight();
        for (int row=0; row<model.getRowCount(); row++) {
            final MetadataEntry entry = model.getEntryAt(row);
            if (entry.hasDefaultValue()) {
                JButton button = buttons.get(entry);
                Dimension buttonSize = button.getPreferredSize();
                button.setSize(buttonSize);
                button.setEnabled(
                    entry.hasDefaultValue()
                );
                int y = row * rowHeight + rowHeight / 2 - buttonSize.height / 2;
                button.setLocation(0, y);
            }
        }
    }
}
