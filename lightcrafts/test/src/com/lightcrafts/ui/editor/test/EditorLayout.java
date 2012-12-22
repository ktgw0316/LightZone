/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor.test;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.UnsupportedColorProfileException;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.DisabledEditor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This JPanel holds the top-level layout for the editable view of a single
 * Document.  The controls are initialized in an Editor and displayed here.
 */

public class EditorLayout extends JPanel {

    // These are the Component children of this Container:
    private JComponent tools;
    private JComponent toolbar;
    private JComponent image;

    public EditorLayout(Editor editor) {
        tools = editor.getToolStack();
        toolbar = editor.getToolBar();
        image = editor.getImage();
        setLayout(null);
        add(tools);
        add(toolbar);
        add(image);
    }

    public Dimension getPreferredSize() {
        Dimension imageSize = image.getPreferredSize();
        Dimension ctrlSize = tools.getPreferredSize();
        Dimension footerSize = toolbar.getPreferredSize();

        int height =
            Math.max(imageSize.height + footerSize.height, ctrlSize.height);
        int width =
            Math.max(imageSize.width, footerSize.width) + ctrlSize.width;

        return new Dimension(width, height);
    }

    public void doLayout() {
        Dimension size = getSize();

        // Give the editor controls their preferred width:

        Dimension ctrlSize = tools.getPreferredSize();
        int ctrlWidth = ctrlSize.width;

        // Give the header its preferred height:

        Dimension headerSize = toolbar.getPreferredSize();
        int headerHeight = headerSize.height;

        // The operation controls and metadata go on the left:

        tools.setLocation(0, 0);
        tools.setSize(ctrlWidth, size.height);

        // The header controls go on top of the image:

        toolbar.setLocation(ctrlWidth, 0);
        toolbar.setSize(size.width - ctrlWidth, headerHeight);

        // The image goes on the right beneath the header controls:

        image.setLocation(ctrlWidth, headerHeight);
        image.setSize(
            size.width - ctrlWidth, size.height - headerHeight
        );
    }

    // Used only in main().
    private static void fail(String message, Throwable t) {
        System.err.println(message + ": " + t.getMessage());
        System.exit(-1);
    }

    public static void main(String[] args) {
        Editor editor;
        if (args.length == 0) {
            editor = Document.createDisabledEditor(
                new DisabledEditor.Listener() {
                    public void imageClicked(Object key) {
                        System.out.println("image clicked");
                    }
                }
            );
        }
        else {
            File file = new File(args[0]);
            if (! file.isFile()) {
                System.err.println("not a file: " + args[0]);
                System.exit(0);
            }
            System.loadLibrary("DCRaw");
            System.loadLibrary("Segment");
            // Other JNI libraries seem to get pulled in automatically.

            Document doc = null;
            try {
                // First try as a saved-document:
                InputStream in = new FileInputStream(file);
                XmlDocument xmlDoc = null;
                try {
                    xmlDoc = new XmlDocument(in);
                }
                catch (IOException e) {
                    // If xmlDoc is null, we fall back to image-import.
                }
                if (xmlDoc != null) {
                    doc = new Document(xmlDoc);
                }
                else {
                    // Maybe it's an image:
                    ImageInfo info = ImageInfo.getInstanceFor(file);
                    ImageMetadata meta = info.getMetadata();
                    doc = new Document(meta);
                }
            }
            catch (BadImageFileException e) {
                fail("Invalid image file", e);
            }
            catch (Document.MissingImageFileException e) {
                fail("Couldn't find the original image", e);
            }
            catch (XMLException e) {
                fail("Malformed LightZone file", e);
            }
            catch (IOException e) {
                fail("Error reading the file", e);
            }
            catch (OutOfMemoryError e) {
                fail("Insufficient memory to open this file", e);
            }
            catch (UnknownImageTypeException e) {
                fail("Unrecognized image format", e);
            }
            catch (UnsupportedColorProfileException e) {
                fail("Unsupported camera", e);
            }
            catch (Throwable e) {
                System.err.println("Unknown error opening this file:");
                e.printStackTrace();
                return;
            }
            editor = doc.getEditor();
        }
        EditorLayout layout = new EditorLayout(editor);

        JFrame frame = new JFrame("EditorLayout");
        frame.setContentPane(layout);
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
