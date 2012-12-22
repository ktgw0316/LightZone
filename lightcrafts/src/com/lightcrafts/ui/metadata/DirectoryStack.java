/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.makernotes.MakerNotesDirectory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class DirectoryStack extends JPanel implements Scrollable {

    private static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/ui/metadata/resources/Directories"
    );

    private Map<ImageMetadataDirectory, MetadataTable> tables;

    private boolean showIDs;    // Show metadata key ID numbers (for developers)

    // A flag telling if we're showing one of our error messages.
    // (Needed for the Scrollable implementation.)
    private boolean error;

    private Set<Class<? extends ImageMetadataDirectory>> expandedDirectories =
        new HashSet<Class<? extends ImageMetadataDirectory>>();

    private boolean filter;
    private boolean sort;

    public DirectoryStack(ImageMetadata metadata) {
        this(metadata, true, true, false);
    }

    public DirectoryStack(
        ImageMetadata metadata, boolean filter, boolean sort, boolean showIDs
    ) {
        this.showIDs = showIDs;
        this.filter = filter;
        this.sort = sort;

        expandedDirectories.add(CoreDirectory.class);
        setMetadata(metadata);
    }

    public void setMetadata(ImageMetadata metadata) {
        removeAll();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (metadata == null) {
            String no = Resources.getString("NoLabel");
            JLabel label = new JLabel(no);
            label.setAlignmentX(.5f);
            add(Box.createVerticalGlue());
            add(label);
            add(Box.createVerticalGlue());
            error = true;
            return;
        }
        Collection<ImageMetadataDirectory> directories =
            metadata.getDirectories();
        if (directories.isEmpty()) {
            String empty = Resources.getString("EmptyLabel");
            JLabel label = new JLabel(empty);
            label.setAlignmentX(.5f);
            add(Box.createVerticalGlue());
            add(label);
            add(Box.createVerticalGlue());
            error = true;
            return;
        }

        ImageMetadataDirectory dir;

        if (tables != null) {
            disposeTables();
        }
        // Keep track of displayed directories
        tables = new HashMap<ImageMetadataDirectory, MetadataTable>();

        // Special-case logic for presenting metadata directories:

        // First, add the "core" directory:
        dir = metadata.getDirectoryFor(CoreDirectory.class);
        if (dir != null) {
            // Never filter "core", always init it expanded:
            addTableExpanded(dir, false, sort);
        }

        // Second, handle any TIFF, EXIF, CIFF or IPTC:
        Class[] middles = new Class[] {
            TIFFDirectory.class,
            EXIFDirectory.class,
            CIFFDirectory.class,
            IPTCDirectory.class,
        };
        for (Class clazz : middles) {
            dir = metadata.getDirectoryFor(clazz);
            if (dir != null) {
                addTable(dir, filter, sort);
            }
        }

        // Third, add anything derived from MakerNotesDirectory:
        for (ImageMetadataDirectory maker : directories) {
            if (maker instanceof MakerNotesDirectory) {
                addTable(maker, filter, sort);
            }
        }

        // Fourth, add anything that has not been added already:
        for (ImageMetadataDirectory imd : directories) {
            if (! tables.containsKey(imd)) {
                addTable(imd, filter, sort);
            }
        }
    }

    boolean isDirectoryExpanded(ImageMetadataDirectory dir) {
        return expandedDirectories.contains(dir.getClass());
    }

    void expandDirectory(ImageMetadataDirectory dir) {
        expandedDirectories.add(dir.getClass());
    }

    void collapseDirectory(ImageMetadataDirectory dir) {
        expandedDirectories.remove(dir.getClass());
    }

    void showDirectory(ImageMetadataDirectory dir) {
        MetadataTable table = tables.get(dir);
        table.setPreferredSize(null);
        revalidate();
   }

    void hideDirectory(ImageMetadataDirectory dir) {
        MetadataTable table = tables.get(dir);
        table.setPreferredSize(new Dimension(0, 0));
        revalidate();
    }

    private void addTable(
        ImageMetadataDirectory dir, boolean filter, boolean sort
    ) {
        if (! tables.isEmpty()) {
            add(new JSeparator());
        }
        String name = getDirName(dir);
        MetadataTableModel model =
            new MetadataTableModel(dir, filter, sort, showIDs);
        MetadataTable table = new MetadataTable(model);
        tables.put(dir, table);
        DirectoryLabel label = new DirectoryLabel(dir, name, this);
        add(label);
        add(table);
    }

    private void addTableExpanded(
        ImageMetadataDirectory dir, boolean filter, boolean sort
    ) {
        if (! tables.isEmpty()) {
            add(new JSeparator());
        }
        String name = getDirName(dir);
        MetadataTableModel model =
            new MetadataTableModel(dir, filter, sort, showIDs);
        MetadataTable table = new MetadataTable(model);
        tables.put(dir, table);
        DirectoryLabel label = new DirectoryLabel(dir, name, this);
        add(label);
        add(table);
    }

    private String getDirName(ImageMetadataDirectory dir) {
        try {
            return Resources.getString(dir.getName() + "Name");
        }
        catch (MissingResourceException e) {
            return dir.getName();
        }
    }

    public void disposeTables() {
        Collection<MetadataTable> values = tables.values();
        for (MetadataTable table : values) {
            table.dispose();
        }
    }

    public boolean getScrollableTracksViewportHeight() {
        return error;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        // If we have any JTables, then defer to one of them:
        Component[] comps = getComponents();
        for (Component comp : comps) {
            if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                return table.getScrollableBlockIncrement(
                    visibleRect, orientation, direction
                );
            }
        }
        return 1;
    }

    public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        // If we have any JTables, then defer to one of them:
        Component[] comps = getComponents();
        for (Component comp : comps) {
            if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                return table.getScrollableUnitIncrement(
                    visibleRect, orientation, direction
                );
            }
        }
        return 1;
    }

    public static void main(String[] args)
        throws IOException, BadImageFileException, UnknownImageTypeException
    {
        if (args.length != 1) {
            System.err.println("usage: DirectoryStack (file)");
            System.exit(1);
        }
        File file = new File(args[0]);
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata metadata = info.getMetadata();
        DirectoryStack dirs = new DirectoryStack(metadata, true, true, true);

        JScrollPane panel = new JScrollPane(dirs);

        JFrame frame = new JFrame("Test");
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);
    }
}
