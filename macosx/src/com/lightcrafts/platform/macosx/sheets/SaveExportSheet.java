/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx.sheets;

import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Document;

import com.lightcrafts.image.export.*;
import com.lightcrafts.image.types.*;
import com.lightcrafts.platform.macosx.MacOSXFileUtil;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.ColorProfileInfo;
import com.lightcrafts.utils.xml.XMLUtil;

/**
 * This class is a go-between for the native Cocoa implementation of sheets.
 * It's based on sample code provided by Apple Computer.
 *
 * @see <a href="http://developer.apple.com/samplecode/JSheets/">JSheets
 * sample code</a>.
 */
public final class SaveExportSheet {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the {@link ImageExportOptions} specified by the user.
     *
     * @return Returns said options or <code>null</code> if there were no
     * options specified.
     */
    public ImageExportOptions getExportOptions() {
        return m_exportOptions;
    }

    /**
     * Gets the name of the selected file.
     *
     * @return Returns the name of the file the user selected or
     * <code>null</code> if the user clicked Cancel.
     */
    public String getSelectedFilename() {
        return m_selectedFilename;
    }

    /**
     * Show an export sheet to the user and wait either for a file to be
     * specified or for Cancel to be clicked.
     *
     * @param parentFrame The parent window to attach the sheet to.
     * @param initialFile The file to select initially.  It may be
     * <code>null</code> to indicate no file.
     * @param options The array of {@link ImageExportOptions} to use.
     */
    public void showExportAndWait( Frame parentFrame, File initialFile,
                                   ImageExportOptions[] options ) {
        showAndWait(
            EXPORT_SHEET, parentFrame, initialFile, options, m_exportNIBPath
        );
    }

    /**
     * Show a save sheet to the user and wait either for a file to be specified
     * or for Cancel to be clicked.
     *
     * @param parentFrame The parent window to attach the sheet to.
     * @param initialFile The file to select initially.  It may be
     * <code>null</code> to indicate no file.
     * @param options The array of {@link ImageExportOptions} to use.
     */
    public void showSaveAndWait( Frame parentFrame, File initialFile,
                                 ImageExportOptions[] options ) {
        showAndWait(
            SAVE_SHEET, parentFrame, initialFile, options, m_exportNIBPath
        );
    }

    ////////// private ////////////////////////////////////////////////////////

    private static final int SAVE_SHEET   = 1;
    private static final int EXPORT_SHEET = 2;

    /**
     * An <code>SaveExportShower</code> implements is-a {@link SheetShower}
     * that implements {@link SaveExportListener}.  The reason for having
     * this class as a nested, private class rather than having
     * {@link SaveExportSheet} implement {@link SaveExportListener}
     * directly is not to expose the {@link SaveExportListener} API to the
     * user of {@link SaveExportSheet}.
     */
    private final class SaveExportShower extends SheetShower
        implements SaveExportListener {

        /**
         * Construct an <code>SaveExportShower</code>.
         *
         * @param sheetType The type of sheet to show.
         * @param parentFrame The parent window to attach the sheet to.
         * @param windowTitle The title of the window.
         * @param saveButtonLabel The label for the Save button.
         * @param initialFile The file to select initially.  It may be
         * <code>null</code> to indicate no file.
         * @param defaultExtension The filename extension to set by default.
         * It may be <code>null</code> to indicate no default.
         * @param colorProfileNames The names of the color profiles as they
         * should be displayed in the pop-up menu.  A <code>null</code> array
         * element means that a menu seperator should be put there.
         * @param defaultColorProfileName The name of the color profile to
         * select by default.  It may be <code>null</code> to indicate no
         * default.
         * @param bitsPerChannel The bits per channel.
         * @param blackPointCompensation Whether black-point compensation
         * should be performed.
         * @param jpegQuality The JPEG quality: 0-100.
         * @param lzwCompression Whether LZW compression should be used.
         * @param multilayer Whether multilayer TIFF images are to be produced.
         * @param originalWidth The original width of the image.
         * @param originalHeight The original height of the image.
         * @param renderingIntent The rendering intent.
         * @param resizeWidth The resize width of the image.
         * @param resizeHeight The resize height of the image.
         * @param resolution The resolution (in pixels per unit).
         * @param resolutionUnit The resolution unit; must be either
         * {@link TIFFConstants#TIFF_RESOLUTION_UNIT_CM} or
         * {@link TIFFConstants#TIFF_RESOLUTION_UNIT_INCH}.
         * @param nibPath The path to the Mac OS X NIB file.  It may be
         */
        SaveExportShower( int sheetType, Component parentFrame,
                          String windowTitle, String saveButtonLabel,
                          File initialFile, String defaultExtension,
                          String[] colorProfileNames,
                          String defaultColorProfileName,
                          int bitsPerChannel, boolean blackPointCompensation,
                          int jpegQuality, boolean lzwCompression,
                          boolean multilayer,
                          int originalWidth, int originalHeight,
                          int renderingIntent,
                          int resizeWidth, int resizeHeight,
                          int resolution, int resolutionUnit,
                          String nibPath ) {
            m_bitsPerChannel = bitsPerChannel;
            m_blackPointCompensation = blackPointCompensation;
            m_colorProfileNames = colorProfileNames;
            m_defaultColorProfileName = defaultColorProfileName;
            m_defaultExtension = defaultExtension;
            m_initialFile = initialFile;
            m_jpegQuality = jpegQuality;
            m_lzwCompression = lzwCompression;
            m_multilayer = multilayer;
            m_nibPath = nibPath;
            m_originalWidth = originalWidth;
            m_originalHeight = originalHeight;
            m_parentFrame = parentFrame;
            m_renderingIntent = renderingIntent;
            m_resizeHeight = resizeHeight;
            m_resizeWidth = resizeWidth;
            m_resolution = resolution;
            m_resolutionUnit = resolutionUnit;
            m_saveButtonLabel = saveButtonLabel;
            m_sheetType = sheetType;
            m_windowTitle = windowTitle;
        }

        /**
         * {@inheritDoc}
         */
        public void sheetOK( String filename,
                             ImageExportOptions exportOptions ) {
            //
            // Copy the information and wake up the thread that called
            // showAndWait() allowing it to continue.
            //
            m_selectedFilename = filename;
            m_exportOptions = exportOptions;
            done();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void sheetCancelled() {
            //
            // There is nothing to do except wake up the thread that called
            // showAndWait() allowing it to continue.
            //
            done();
        }

        ////////// protected //////////////////////////////////////////////////

        /**
         * {@inheritDoc}.
         */
        protected void showSheet() {
            final String initialDirectory, initialFilename;
            if ( m_initialFile != null ) {
                initialDirectory = m_initialFile.getParent();
                initialFilename = m_initialFile.getName();
            } else {
                initialDirectory = initialFilename = null;
            }
            showNativeSheet(
                m_sheetType, m_parentFrame, m_windowTitle, m_saveButtonLabel,
                initialDirectory, initialFilename, m_defaultExtension,
                m_bitsPerChannel, m_blackPointCompensation, m_colorProfileNames,
                m_defaultColorProfileName, m_jpegQuality, m_lzwCompression,
                m_multilayer, m_originalWidth, m_originalHeight,
                m_renderingIntent, m_resizeWidth, m_resizeHeight,
                m_resolution, m_resolutionUnit, m_nibPath,
                this
            );
        }

        ////////// private ////////////////////////////////////////////////////

        private final int m_bitsPerChannel;
        private final boolean m_blackPointCompensation;
        private final String[] m_colorProfileNames;
        private final String m_defaultColorProfileName;
        private final String m_defaultExtension;
        private final File m_initialFile;
        private final int m_jpegQuality;
        private final boolean m_lzwCompression;
        private final boolean m_multilayer;
        private final String m_nibPath;
        private final int m_originalHeight;
        private final int m_originalWidth;
        private final Component m_parentFrame;
        private final int m_renderingIntent;
        private final int m_resizeHeight;
        private final int m_resizeWidth;
        private final int m_resolution;
        private final int m_resolutionUnit;
        private final String m_saveButtonLabel;
        private final int m_sheetType;
        private final String m_windowTitle;
    }

    /**
     * Initialize the native code.
     */
    private static native void init();

    /**
     * This is called from native code to indicate that the user clicked
     * Cancel.
     *
     * @param sl The {@link SaveExportListener} to notify.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static void sheetCanceledCallback( SaveExportListener sl ) {
        sl.sheetCancelled();
    }

    /**
     * This is called from native code to indicate that the user clicked OK and
     * specified a file.
     * <p>
     * To simplify the native code, it provides the options as an array of
     * strings where each string is of the form
     * <i>key</i><code>:</code><i>value</i>.
     * The 0th string is guaranteed to have a <i>key</i> of <code>Type</code>
     * and a value that is an integer that is the ASCII code of the first
     * letter of the export image file format, e.g., "74" for JPEG.
     * The remaining strings are then options for the given image type.
     * <p>
     * These options are then converted to an instance of
     * {@link ImageExportOptions}.
     *
     * @param sel The {@link SaveExportListener} to notify.
     * @param filename The name of the file the user specified.
     * @param options The export options specified by the user or
     * <code>null</code> if there are no options.  Note that some of the array
     * values may be <code>null</code>; these should simply be skipped.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static void sheetOKCallback( SaveExportListener sel,
                                         String filename, String[] options ) {
        ImageExportOptions exportOptions = null;
        if ( options != null ) {
            for ( int i = 0; i < options.length; ++i ) {
                if ( options[i] == null )
                    continue;
                final int seperator = options[i].indexOf( ':' );
                final String key = options[i].substring( 0, seperator );
                final String value = options[i].substring( seperator + 1 );
                if ( key.equals( "Type" ) ) {
                    switch ( Integer.parseInt( value ) ) {
                        case 'J':   // JPEG
                            exportOptions = new JPEGImageType.ExportOptions();
                            break;
                        case 'L':   // LZN
                            exportOptions = new LZNImageType.ExportOptions();
                            break;
                        case 'T':   // TIFF
                            exportOptions = new TIFFImageType.ExportOptions();
                            break;
                        default:
                            throw new IllegalStateException(
                                "bad type:" + value
                            );
                    }
                    continue;
                }
                assert exportOptions != null;
                exportOptions.setValueOf( key, value );
            }
            assert exportOptions != null;
            exportOptions.setExportFile( new File( filename ) );
        }
        sel.sheetOK( filename, exportOptions );
    }

    /**
     * Show either an export or save dialog to the user and wait either for a
     * file to be specified or for Cancel to be clicked.
     *
     * @param sheetType Tyhe type of sheet to show.
     * @param parentFrame The parent window to attach the sheet to.
     * @param initialFile The file to select initially.  It may be
     * <code>null</code> to indicate no file.
     * @param options The array of {@link ImageExportOptions} to use.
     * @param nibPath The full path to the NIB file to use.
     */
    private void showAndWait( int sheetType, Frame parentFrame,
                              File initialFile, ImageExportOptions[] options,
                              String nibPath ) {
        final String label;
        switch ( sheetType ) {
            case EXPORT_SHEET:
                label = "Convert";
                break;
            case SAVE_SHEET:
                label = "Save";
                break;
            default:
                throw new IllegalArgumentException(
                    "Unknown sheet type: " + sheetType
                );
        }

        JPEGImageType.ExportOptions jpegExportOptions = null;
        TIFFImageType.ExportOptions tiffExportOptions = null;
        ImageFileExportOptions fileExportOptions = null;

        for ( ImageExportOptions opt : options ) {
            if ( opt instanceof JPEGImageType.ExportOptions )
                jpegExportOptions = (JPEGImageType.ExportOptions)opt;
            else if ( opt instanceof TIFFImageType.ExportOptions )
                tiffExportOptions = (TIFFImageType.ExportOptions)opt;
            if ( opt instanceof ImageFileExportOptions &&
                 fileExportOptions == null )
                fileExportOptions = (ImageFileExportOptions)opt;
        }
        if ( jpegExportOptions == null )
            jpegExportOptions = JPEGImageType.INSTANCE.newExportOptions();
        if ( tiffExportOptions == null )
            tiffExportOptions = TIFFImageType.INSTANCE.newExportOptions();

        //
        // Build the list of color profile names.
        //
        final Collection<ColorProfileInfo> colorProfiles =
            Platform.getPlatform().getExportProfiles();
        final List<ColorProfileInfo> menuProfiles =
            ColorProfileInfo.arrangeForMenu( colorProfiles );

        //
        // If there's no default, use sRGB.
        //
        final String defaultProfileName = fileExportOptions != null ?
            fileExportOptions.colorProfile.getValue() :
            ColorProfileOption.DEFAULT_PROFILE_NAME;

        //
        // We need to search the list of color profile names for the name of
        // the default profile to see if it's there.  If not, we need to insert
        // it first.
        //
        boolean foundDefaultProfile = false;
        for ( ColorProfileInfo cpi : menuProfiles )
            if ( cpi != null &&
                 defaultProfileName.equals( cpi.getName() ) ) {
                foundDefaultProfile = true;
                break;
            }

        //
        // The color profile names need to be converted to a String[].  But
        // first, insert the name of the default color profile if we didn't
        // find it.
        //
        int i = !foundDefaultProfile && defaultProfileName != null ? 2 : 0;
        final String[] colorProfileNames =
            new String[ menuProfiles.size() + i ];
        if ( i == 2 )
            colorProfileNames[0] = defaultProfileName;

        //
        // Now (finally) convert the color profile names to a String[].
        //
        for ( ColorProfileInfo cpi : menuProfiles )
            colorProfileNames[ i++ ] = cpi != null ? cpi.getName() : null;

        final boolean blackPointCompensation;
        final int renderingIntent;
        int resizeWidth = 0, resizeHeight = 0;
        final int resolution, resolutionUnit;
        if ( fileExportOptions != null ) {
            blackPointCompensation =
                fileExportOptions.blackPointCompensation.getValue();
            renderingIntent = fileExportOptions.renderingIntent.getValue();
            resizeWidth = fileExportOptions.resizeWidth.getValue();
            resizeHeight = fileExportOptions.resizeHeight.getValue();
            resolution = fileExportOptions.resolution.getValue();
            resolutionUnit = fileExportOptions.resolutionUnit.getValue();
        } else {
            blackPointCompensation = BlackPointCompensationOption.DEFAULT_VALUE;
            renderingIntent = RenderingIntentOption.DEFAULT_VALUE;
            resolution = ResolutionOption.DEFAULT_VALUE;
            resolutionUnit = ResolutionUnitOption.DEFAULT_VALUE;
        }
        if ( resizeWidth == 0 )
            resizeWidth = options[0].originalWidth.getValue();
        if ( resizeHeight == 0 )
            resizeHeight = options[0].originalHeight.getValue();

        final SaveExportShower ose = new SaveExportShower(
            sheetType, parentFrame, label, label, initialFile,
            options[0].getImageType().getExtensions()[0],
            colorProfileNames, defaultProfileName,
            tiffExportOptions.bitsPerChannel.getValue(),
            blackPointCompensation, jpegExportOptions.quality.getValue(),
            tiffExportOptions.lzwCompression.getValue(),
            tiffExportOptions.multilayer.getValue(),
            options[0].originalWidth.getValue(),
            options[0].originalHeight.getValue(),
            renderingIntent, resizeWidth, resizeHeight,
            resolution, resolutionUnit, nibPath
        );
        SheetDelegate.showAndWait( ose );
    }

    /**
     * Call native code to show a sheet.
     *
     * @param sheetType The type of sheet to show.
     * @param parent The parent window to attach the sheet to.
     * @param windowTitle The title of the window.
     * @param saveButtonLabel The label for the Save button.
     * @param initialDirectory The name of the directory to open initially.  It
     * may be <code>null</code> to indicate no directory.
     * @param initialFilename The name of the file to select initially.  It may
     * be <code>null</code> to indicate no file.
     * @param defaultExtension The filename extension to set by default.  It
     * maye be <code>null</code> to indicate no default.
     * @param bitsPerChannel The bits per channel.
     * @param blackPointCompensation Whether black-point compensation should be
     * performed.
     * @param colorProfileNames The names of the color profiles as they should
     * be displayed in the pop-up menu.  A <code>null</code> array element
     * means that a menu seperator should be put there.
     * @param defaultColorProfileName The name of the color profile to select
     * by default.  It may be <code>null</code> to indicate no default.
     * @param jpegQuality The JPEG quality: 0-100.
     * @param lzwCompression Whether LZW compression should be used.
     * @param multilayer Whether multilayer TIFF files are to be produced.
     * @param originalWidth The original width of the image.
     * @param originalHeight The original height of the image.
     * @param renderingIntent The rendering intent.
     * @param resizeWidth The resize width of the image.
     * @param resizeHeight The resize height of the image.
     * @param resolution The resolution (in pixels per unit).
     * @param resolutionUnit The resolution unit; must be either
     * {@link TIFFConstants#TIFF_RESOLUTION_UNIT_CM} or
     * {@link TIFFConstants#TIFF_RESOLUTION_UNIT_INCH}.
     * @param nibPath The path to the Mac OS X NIB file.  It may be
     * <code>null</code> to indicate no NIB file.
     * @param sl The {@link SaveExportListener} to use.
     */
    private static native void showNativeSheet( int sheetType,
                                                Component parent,
                                                String windowTitle,
                                                String saveButtonLabel,
                                                String initialDirectory,
                                                String initialFilename,
                                                String defaultExtension,
                                                int bitsPerChannel,
                                                boolean blackPointCompensation,
                                                String[] colorProfileNames,
                                                String defaultColorProfileName,
                                                int jpegQuality,
                                                boolean lzwCompression,
                                                boolean multilayer,
                                                int originalWidth,
                                                int originalHeight,
                                                int renderingIntent,
                                                int resizeWidth,
                                                int resizeHeight,
                                                int resolution,
                                                int resolutionUnit,
                                                String nibPath,
                                                SaveExportListener sl );

    /**
     * The path to the NIB file used for the custom export dialog.
     */
    private static final String m_exportNIBPath;

    /**
     * The {@link ImageExportOptions} specified by the user.
     */
    private ImageExportOptions m_exportOptions;

    /**
     * The name of the file the user selected to open.
     */
    private String m_selectedFilename;

    static {
        m_exportNIBPath = MacOSXFileUtil.getNIBPathOf( "Export.nib" );
        System.loadLibrary( "MacOSX" );
        init();
    }

    ////////// main() for testing /////////////////////////////////////////////

    public static void main( String[] args ) throws Exception {
        final File dir = new File( System.getProperty( "user.home" ) );
        final File file = new File( dir, "test.tif" );

        final ImageExportOptions lznOptions =
            LZNImageType.INSTANCE.newExportOptions();
        lznOptions.originalWidth.setValue( 400 );
        lznOptions.originalHeight.setValue( 300 );

        final ImageFileExportOptions tiffOptions =
            TIFFImageType.INSTANCE.newExportOptions();
        tiffOptions.originalWidth.setValue( 400 );
        tiffOptions.originalHeight.setValue( 300 );
        tiffOptions.setExportFile( file );

        final ImageFileExportOptions jpegOptions =
            JPEGImageType.INSTANCE.newExportOptions();
        jpegOptions.originalWidth.setValue( 400 );
        jpegOptions.originalHeight.setValue( 300 );
        jpegOptions.setExportFile( file );

        final SaveExportSheet sheet = new SaveExportSheet();
        sheet.showSaveAndWait(
            null, file,
            new ImageExportOptions[]{ lznOptions, tiffOptions, jpegOptions }
        );

        final ImageExportOptions options = sheet.getExportOptions();
        final Document doc = XMLUtil.createDocument( "Test" );
        final ImageExportOptionXMLWriter w =
            new ImageExportOptionXMLWriter( options, doc.getDocumentElement() );
        options.writeTo( w );
        XMLUtil.writeDocumentTo( doc, System.out );
    }
}
/* vim:set et sw=4 ts=4: */
