/* Copyright (C) 2005-2011 Fabio Riccardi */

package painting;

//import ch.randelshofer.quaqua.QuaquaManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.event.WindowAdapter;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Dec 4, 2005
 * Time: 11:25:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class TableTest extends WindowAdapter {
    static String[][] data = {
        {"a", "b", "c", "d"},
        {"e", "f", "g", "h"},
        {"i", "l", "m", "n"},
        {"o", "p", "q", "r"},
        {"a", "b", "c", "d"},
        {"e", "f", "g", "h"},
        {"i", "l", "m", "n"},
        {"o", "p", "q", "r"},
        {"a", "b", "c", "d"},
        {"e", "f", "g", "h"},
        {"i", "l", "m", "n"},
        {"o", "p", "q", "r"},
        {"a", "b", "c", "d"},
        {"e", "f", "g", "h"},
        {"i", "l", "m", "n"},
        {"o", "p", "q", "r"}
    };

    static class TablePane extends JTable {
        TablePane() {
            super(data, new String[]{"1", "2", "3", "4"});
            setCellSelectionEnabled(true);
            setDefaultRenderer(Object.class, new ImageCellRenderer());
        }

        static int epoch = 0;

        static class ImageCellRenderer extends JTextPane implements TableCellRenderer {
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                if (row == 0 && column == 0)
                    epoch++;
                System.out.println("updating (" + epoch + ") " + row + ":" + column);
                if (isSelected)
                    this.setBackground(Color.gray);
                else
                    setBackground(Color.white);
                setText((String) value);
                return this;
            }
        }
    }

    TableTest() {
//        try {
//            UIManager.setLookAndFeel(QuaquaManager.getLookAndFeelClassName());
//        } catch (Exception e) {
//        }
        JFrame frame = new JFrame();
        frame.addWindowListener(this);

        JScrollPane scrollPane = new JScrollPane(new TablePane(),
                                                 JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        frame.getContentPane().add(scrollPane);

        frame.pack();
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new TableTest();
    }
}
