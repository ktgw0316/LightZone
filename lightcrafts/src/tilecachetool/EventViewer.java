/* @(#)EventViewer.java	1.2 02/10/24 21:03:22 */
package tilecachetool;

import java.util.Vector;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * <p>Title: Tile Cache Monitoring Tool</p>
 * <p>Description: Monitors and displays JAI Tile Cache activity.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>   All Rights Reserved</p>
 * <p>Company: Virtual Visions Software, Inc.</p>
 *
 * @author Dennis Sigel
 * @version 1.01
 *
 * Purpose:  Display various JAI events in a table.
 */

public final class EventViewer extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JScrollPane scrollpane;

    private Object[][] initial_data = { {"-", "-", "-", "-"} };

    private static final Color LIGHT_BLUE = new Color(240, 240, 255);
    private static final Font TABLE_FONT  = new Font("monospaced", Font.BOLD, 12);
    private static final String[] COLUMN_LABELS = {
        "JAI Operator",
        "Event",
        "Tile Size",
        "Timestamp"
    };


    /**
     * Default Constructor
     */
    public EventViewer() {
        setLayout( new FlowLayout(FlowLayout.LEFT, 1, 1) );

        model = new DefaultTableModel(initial_data, COLUMN_LABELS);
        table = new JTable(model);

        table.setFont(TABLE_FONT);
        table.setBackground(LIGHT_BLUE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoscrolls(true);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);

        scrollpane = new JScrollPane(table);
        scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        EmptyBorder empty_border = new EmptyBorder(5, 5, 5, 5);
        BevelBorder bevel_border = new BevelBorder(BevelBorder.LOWERED);

        scrollpane.setBorder( new CompoundBorder(empty_border, bevel_border) );

        // add some initial room for width (arbitrary)
        Dimension dim = new Dimension(scrollpane.getPreferredSize().width + 100,
                                      140);

        scrollpane.setPreferredSize(dim);
        add(scrollpane);
    }

    /**
     * Sets the number of displayed rows in the event viewer.
     *
     * @param rows Positive number of displayed event rows
     * @throws IllegalArgumentException if rows is .le. 0
     * @since TCT 1.0
     */
    public void setRows(int rows) {
        if ( rows <= 0 ) {
            throw new IllegalArgumentException("rows must be greater than 0.");
        }

        int hdr_height = (int)table.getTableHeader().getHeaderRect(0).getHeight();
        int height     = table.getRowHeight() * (rows + 1) + hdr_height;
        Dimension dim  = new Dimension(scrollpane.getPreferredSize().width,
                                       height);

        scrollpane.setPreferredSize(dim);
        scrollpane.revalidate();
    }

    /**
     * Removes all of the rows of events in the event viewer.
     * @since TCT 1.0
     */
    public void clear() {
        int count = model.getRowCount() - 1;
        for ( int i = count; i >= 0; i-- ) {
            model.removeRow(i);
        }

        model = new DefaultTableModel(initial_data, COLUMN_LABELS);

        table.setModel(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
    }

    /**
     * Insert a new row of event information.
     * @param row insert data before this <code>row</code>
     * @param data set of events
     * @throws IllegalArgumentException if <code>rows</code> is .lt. 0
     * @throws IllegalArgumentException if <code>data</code> is <code>null</code>
     */
    public synchronized void insertRow(int row, Vector data) {
        if ( row < 0 ) {
            throw new IllegalArgumentException("Rows must be greater or equal to 0.");
        }

        if ( data == null ) {
            throw new IllegalArgumentException("Data cannot be null.");
        }

        model.insertRow(row, data);
    }
}

