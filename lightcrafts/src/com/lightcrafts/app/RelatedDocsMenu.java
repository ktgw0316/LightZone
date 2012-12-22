/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.image.metadata.CoreDirectory;
import static com.lightcrafts.image.metadata.CoreTags.CORE_DIR_NAME;
import static com.lightcrafts.image.metadata.CoreTags.CORE_FILE_NAME;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.ui.editor.assoc.DocumentDatabase;
import com.lightcrafts.ui.editor.assoc.DocumentDatabaseListener;
import com.lightcrafts.ui.toolkit.ButtonIcon;
import com.lightcrafts.ui.toolkit.MenuButton;

import static com.lightcrafts.app.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A button that shows a menu of Documents.  It takes an ImageMetadata, looks
 * up all related Document files in the DocumentDatabase, provides a menu
 * item for each, and associates each item with the action of opening the
 * corresponding Document.
 */
class RelatedDocsMenu extends MenuButton implements DocumentDatabaseListener {

    private static Icon Icon;

    static {
        final String versions = LOCALE.get("VersionsLabel");

        Icon = new ButtonIcon(
            versions, ButtonIcon.UNSELECTED, ButtonIcon.PLAIN_BUTTON
        );
    }

    private ComboFrame frame;
    private File imageFile;

    RelatedDocsMenu(ComboFrame frame, ImageMetadata meta) {
        this.frame = frame;

        setIcon(Icon);

        ImageMetadataDirectory coreDir =
            meta.getDirectoryFor(CoreDirectory.class);

        ImageMetaValue fileValue =
            coreDir.getValue(CORE_FILE_NAME);
        ImageMetaValue dirValue =
            coreDir.getValue(CORE_DIR_NAME);

        imageFile = new File(dirValue.toString(), fileValue.toString());

        updateFromDatabase();

        DocumentDatabase.addListener(this);
    }

    // Create a disabled button, for the no-Document display mode:
    RelatedDocsMenu() {
        setEnabled(false);
    }

    public void docFilesChanged(File imageFile) {
        updateFromDatabase();
    }

    private void updateFromDatabase() {
        clear();

        // A special menu item, to access the original image:
        JMenuItem origItem = new JMenuItem(
            "Original (" + imageFile.getName() + ")"
        );
        origItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    Application.open(frame, imageFile);
                }
            }
        );
        add(origItem);

        // The list of related Document files:
        List<File> docs = DocumentDatabase.getDocumentsForImage(imageFile);

        // Sort the Document files for presentation:
        Comparator<File> alphaFileComparator = new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        };
        Collections.sort(docs, alphaFileComparator);

        // Items for all the related Documents:
        for (final File file : docs) {
            JMenuItem item = new JMenuItem(file.getName());
            item.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        Application.open(frame, file);
                    }
                }
            );
            add(item);
        }
        boolean empty = docs.isEmpty();
        setEnabled(! empty);
    }
}
