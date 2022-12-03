/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.MultipageTIFFImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import java.io.*;
import java.util.prefs.Preferences;
import java.awt.*;

/**
 * A SaveOptions object is a complete saved file specification as entered by
 * a user interacting with a SaveDialog.  Its structure depends on which file
 * format was selected (LZN, Sidecar TIFF, Sidecar JPEG, or Multilayer TIFF).
 */
public class SaveOptions {

    // A private, abstract base class to simplify access to all the
    // format special cases.
    static abstract class Options {
        abstract File getFile();
        abstract void setFile(File file);
        abstract String getName();
        abstract String getSuffix();
        void saveMarker(XmlNode node) {
            node.setAttribute("type", getName());
        }
        void verifyMarker(XmlNode node) throws XMLException {
            if (! node.getAttribute("type").equals(getName())) {
                throw new XMLException("Unexpected SaveOptions Options class");
            }
        }
        protected boolean m_shouldSaveDirectly;
    }

    /**
     * If the save format is LZN, then the auxilliary options are just the File
     * to use.
     */
    public static class Lzn extends Options {

        private File file;

        Lzn(File file) {
            this.file = file;
        }

        File getFile() {
            return file;
        }

        void setFile(File file) {
            this.file = file;
        }

        String getName() {
            return "LZN";
        }

        String getSuffix() {
            return "lzn";
        }

        void save(XmlNode node) {
            saveMarker(node);
            String path = file.getAbsolutePath();
            node.setAttribute("path", path);
        }

        static Lzn restore(XmlNode node) throws XMLException {
            String path = node.getAttribute("path");
            File file = new File(path);
            Lzn lzn = new Lzn(file);
            lzn.verifyMarker(node);
            return lzn;
        }
    }

    /**
     * If the save format is Sidecar TIFF, then the auxilliary options are a
     * TIFFImageType.ExportOptions.
     */
    public static class SidecarTiff extends Options {

        private ImageExportOptions export;

        SidecarTiff(ImageExportOptions export) {
            this.export = export;
        }

        public ImageExportOptions getExportOptions() {
            return export;
        }

        File getFile() {
            return export.getExportFile();
        }

        void setFile(File file) {
            export.setExportFile(file);
        }

        String getName() {
            return "TIFF (sidecar)";
        }

        String getSuffix() {
            return "tif";
        }

        void save(XmlNode node) {
            saveMarker(node);
            export.write(node);
        }

        static SidecarTiff restore(XmlNode node) throws XMLException {
            ImageExportOptions export = ImageExportOptions.read(node);
            SidecarTiff tiff = new SidecarTiff(export);
            tiff.verifyMarker(node);
            return tiff;
        }
    }

    /**
     * If the save format is Sidecar JPEG, then the auxilliary options are a
     * JPEGImageType.ExportOptions (guaranteed to have its multilayer flag
     * unset).
     */
    public static class SidecarJpeg extends Options {

        private ImageExportOptions export;

        SidecarJpeg(ImageExportOptions export) {
            this.export = export;
        }

        public ImageExportOptions getExportOptions() {
            return export;
        }

        File getFile() {
            return export.getExportFile();
        }

        void setFile(File file) {
            export.setExportFile(file);
        }

        String getName() {
            return "JPEG (sidecar)";
        }

        String getSuffix() {
            return "jpg";
        }

        void save(XmlNode node) {
            saveMarker(node);
            export.write(node);
        }

        static SidecarJpeg restore(XmlNode node) throws XMLException {
            ImageExportOptions export = ImageExportOptions.read(node);
            SidecarJpeg jpeg = new SidecarJpeg(export);
            jpeg.verifyMarker(node);
            return jpeg;
        }
    }

    /**
     * If the save format is Multilayer TIFF, then the auxilliary option is
     * a TIFFImageType.ExportOptions that is guaranteed to have its
     * multilayer flag set.
     */
    public static class MultilayerTiff extends Options {

        private ImageExportOptions export;

        MultilayerTiff(ImageExportOptions export) {
            this.export = export;
        }

        public ImageExportOptions getExportOptions() {
            return export;
        }

        File getFile() {
            return export.getExportFile();
        }

        void setFile(File file) {
            export.setExportFile(file);
        }

        String getName() {
            return "TIFF (multilayer)";
        }

        String getSuffix() {
            return "tif";
        }

        void save(XmlNode node) {
            saveMarker(node);
            export.write(node);
        }

        static MultilayerTiff restore(XmlNode node) throws XMLException {
            ImageExportOptions export = ImageExportOptions.read(node);
            MultilayerTiff tiff = new MultilayerTiff(export);
            tiff.verifyMarker(node);
            return tiff;
        }
    }

    // Exactly one of these is non-null:
    private Lzn lzn;
    private SidecarTiff sideTiff;
    private SidecarJpeg sideJpeg;
    private MultilayerTiff multiTiff;

    // And whichever one is non-null is equal to this:
    private Options options;

    public static SaveOptions createLzn() {
        return createLzn(null);
    }

    public static SaveOptions createSidecarTiff() {
        return createSidecarTiff(TIFFImageType.INSTANCE.newExportOptions());
    }

    public static SaveOptions createSidecarJpeg() {
        return createSidecarJpeg(JPEGImageType.INSTANCE.newExportOptions());
    }

    public static SaveOptions createMultilayerTiff() {
        return createMultilayerTiff(
            MultipageTIFFImageType.INSTANCE.newExportOptions()
        );
    }

    public static SaveOptions createLzn(File file) {
        SaveOptions settings = new SaveOptions();
        settings.lzn = new Lzn(file);
        settings.options = settings.lzn;
        return settings;
    }

    public static SaveOptions createSidecarTiff(ImageExportOptions options) {
        ((TIFFImageType.ExportOptions) options).multilayer.setValue(false);
        SaveOptions settings = new SaveOptions();
        settings.sideTiff = new SidecarTiff(options);
        settings.options = settings.sideTiff;
        return settings;
    }

    public static SaveOptions createSidecarJpeg(ImageExportOptions options) {
        SaveOptions settings = new SaveOptions();
        settings.sideJpeg = new SidecarJpeg(options);
        settings.options = settings.sideJpeg;
        return settings;
    }

    public static SaveOptions createMultilayerTiff(ImageExportOptions options) {
        ((TIFFImageType.ExportOptions) options).multilayer.setValue(true);
        SaveOptions settings = new SaveOptions();
        settings.multiTiff = new MultilayerTiff(options);
        settings.options = settings.multiTiff;
        return settings;
    }

    private final static Preferences Prefs =
        Preferences.userNodeForPackage(SaveOptions.class);
    private final static String SaveOptionsKey = "SaveOptions";

    public static SaveOptions getDefaultSaveOptions() {
        String text = Prefs.get(SaveOptionsKey, null);
        if (text != null) {
            if (text != null) {
                try {
                    InputStream in = new ByteArrayInputStream(
                        text.getBytes("UTF-8")
                    );
                    XmlDocument doc = new XmlDocument(in);
                    XmlNode root = doc.getRoot();
                    SaveOptions options = SaveOptions.restore(root);
                    return options;
                }
                catch (Exception e) {   // IOException, XMLException
                    e.printStackTrace();
                    Prefs.remove(SaveOptionsKey);
                    // Fall back to the default default options.
                }
            }
        }
        // The default default:
        SaveOptions options = createSidecarJpeg();
        ImageFileExportOptions export =
            (ImageFileExportOptions) getExportOptions(options);
        // These default default sizes must match up with one of the multiple
        // choice options in ExportControls.
        export.resizeWidth.setValue(ExportControls.defaultSaveSize);
        export.resizeHeight.setValue(ExportControls.defaultSaveSize);
        return options;
    }

    public static void setDefaultSaveOptions(SaveOptions options) {
        XmlDocument doc = new XmlDocument("SaveOptions");
        XmlNode root = doc.getRoot();
        options.save(root);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            String text = out.toString("UTF-8");
            Prefs.put(SaveOptionsKey, text);
        }
        catch (IOException e) {
            e.printStackTrace();
            Prefs.remove(SaveOptionsKey);
        }
    }

    private SaveOptions() {
        // Only factory construction.
    }

    /**
     * Get an ImageExportOptions object associated with the given SaveOptions,
     * or null if this SaveOptions does not define any.  (Like Lzn.)
     */
    public static ImageExportOptions getExportOptions(SaveOptions options) {
        if (options.isSidecarTiff()) {
            SaveOptions.SidecarTiff export = options.getSidecarTiff();
            return export.getExportOptions();
        }
        if (options.isSidecarJpeg()) {
            SaveOptions.SidecarJpeg export = options.getSidecarJpeg();
            return export.getExportOptions();
        }
        if (options.isMultilayerTiff()) {
            SaveOptions.MultilayerTiff export = options.getMultilayerTiff();
            return export.getExportOptions();
        }
        return null;
    }

    public void setShouldSaveDirectly( boolean b ) {
        options.m_shouldSaveDirectly = b;
    }

    public boolean shouldSaveDirectly() {
        return options.m_shouldSaveDirectly;
    }

    public void updateSize(Dimension size) {
        ImageExportOptions export = getExportOptions(this);
        if (export != null) {
            ExportLogic.maybeUpdateSize(export, size);
        }
    }

    public void save(XmlNode node) {
        if (lzn != null) {
            lzn.save(node);
        }
        if (sideTiff != null) {
            sideTiff.save(node);
        }
        if (sideJpeg != null) {
            sideJpeg.save(node);
        }
        if (multiTiff != null) {
            multiTiff.save(node);
        }
    }

    public static SaveOptions restore(XmlNode node) throws XMLException {
        SaveOptions settings = new SaveOptions();
        try {
            settings.lzn = Lzn.restore(node);
            settings.options = settings.lzn;
            return settings;
        }
        catch (XMLException e) {
            // Try the next format.
        }
        try {
            settings.sideTiff = SidecarTiff.restore(node);
            settings.options = settings.sideTiff;
            return settings;
        }
        catch (XMLException e) {
            // Try the next format.
        }
        try {
            settings.sideJpeg = SidecarJpeg.restore(node);
            settings.options = settings.sideJpeg;
            return settings;
        }
        catch (XMLException e) {
            // Try the next format.
        }
        try {
            settings.multiTiff = MultilayerTiff.restore(node);
            settings.options = settings.multiTiff;
            return settings;
        }
        catch (XMLException e) {
            // Give up.
        }
        // Nothing worked out:
        throw new XMLException("No valid SaveOptions data");
    }

    public boolean isLzn() {
        return lzn != null;
    }

    public boolean isSidecarTiff() {
        return sideTiff != null;
    }

    public boolean isSidecarJpeg() {
        return sideJpeg != null;
    }

    public boolean isMultilayerTiff() {
        return multiTiff != null;
    }

    public Lzn getLzn() {
        return lzn;
    }

    public SidecarTiff getSidecarTiff() {
        return sideTiff;
    }

    public SidecarJpeg getSidecarJpeg() {
        return sideJpeg;
    }

    public MultilayerTiff getMultilayerTiff() {
        return multiTiff;
    }

    public File getFile() {
        return options.getFile();
    }

    public void setFile(File file) {
        options.setFile(file);
    }

    // For presentation in SaveDialog.
    String getName() {
        return options.getName();
    }

    // For presentation in SaveDialog and generating file names.
    String getSuffix() {
        return options.getSuffix();
    }

    // For picking an initial file filter in SaveDialog.
    boolean matchesType(SaveOptions other) {
        return options.getClass().equals(other.options.getClass());
    }
}
