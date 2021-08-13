package com.lightcrafts.ui.metadata2;

import lombok.val;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class UrgencyTableCellRenderer extends MetadataTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column > 0) {
            val i = ((UrgencyMetadataEntry.UrgencyObject)value).getValue();
            val color = valueToColorMap.get(i);
            label.setBackground(color);
        }
        return label;
    }

    private final Map<Integer, Color> valueToColorMap = new HashMap<>() {{
        // cf. https://jfly.uni-koeln.de/colorset/CUD_color_set_GuideBook_2018_for_print_cs4.pdf
        put(0, new Color(0, 0, 0, 0)); // transparent
        put(1, new Color(255, 75, 0)); // red
        put(2, new Color(246, 170, 0)); // orange
        put(3, new Color(255, 241, 0)); // yellow
        put(4, new Color(3, 175, 122)); // green
        put(5, new Color(77, 196, 255)); // blue
        put(6, new Color(153, 0, 153)); // purple
        put(7, new Color(132, 145, 158)); // gray
        put(8, Color.BLACK);
    }};
}
