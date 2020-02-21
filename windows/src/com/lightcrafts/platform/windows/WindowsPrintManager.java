/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import java.awt.*;
import java.awt.image.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;

import javax.media.jai.PlanarImage;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.thread.ProgressThread;

/**
 * Do native printing on Windows.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsPrintManager {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Disposes of native resources.  Calling this multiple times in a row is
     * guaranteed to be harmless.
     *
     * @see #initDefaultPrinter()
     */
    public static native void dispose();

    /**
     * Gets the {@link PageFormat} for the printer.
     *
     * @return Returns said {@link PageFormat}.
     * @see #setPageFormat(PageFormat)
     */
    public static native PageFormat getPageFormat();

    /**
     * Gets the printer's resolution in pixels per inch.
     *
     * @return Returns said resolution.
     */
    public static native Dimension getPrinterResolution();

    /**
     * Initializes the default printer information.
     *
     * @see #dispose()
     */
    public static native void initDefaultPrinter();

    /**
     * Prints an image.
     *
     * @param image The image to print.
     * @param offset The offset in pixels from the upper-left corner of the
     * paper where to start printing the image.
     * @param documentName The name of the image.
     * @param thread The {@link ProgressThread} to use.
     */
    public static synchronized void print( PlanarImage image,
                                           Point offset,
                                           String documentName,
                                           ProgressThread thread )
        throws PrinterException
    {
        if ( !isPrinterCapable() ) {
            Platform.getPlatform().getAlertDialog().showAlert(
                null, "Incapable Printer",
                "The selected printer can not be used with LightZone.",
                AlertDialog.ERROR_ALERT, new String[]{ "OK" }
            );
            return;
        }

        thread.getProgressIndicator().setMaximum(image.getMaxTileX() * image.getMaxTileY());

        beginPrinting( documentName, 24 );
        beginPage();
        for ( int tileX = 0; tileX <= image.getMaxTileX(); ++tileX )
            for ( int tileY = 0; tileY <= image.getMaxTileY(); ++tileY ) {
                if ( thread.isCanceled() ) {
                    abortPrinting();
                    return;
                }
                Raster tile = image.getTile( tileX, tileY );
                final Rectangle tileBounds = tile.getBounds();
                final Rectangle clippedTileBounds = tile.getBounds().intersection(image.getBounds());

                final BufferedImage tileImage = new BufferedImage(
                    image.getColorModel(),
                    (WritableRaster)tile.createTranslatedChild( 0, 0 ),
                    false, null
                );
                final BufferedImage winImage = new BufferedImage(
                    clippedTileBounds.width, clippedTileBounds.height,
                    BufferedImage.TYPE_3BYTE_BGR
                );

                final Graphics2D g2d = winImage.createGraphics();
                g2d.drawImage( tileImage, null, 0, 0 );
                g2d.dispose();

                tile = winImage.getRaster().createTranslatedChild(
                    (int)tileBounds.getMinX(),
                    (int)tileBounds.getMinY()
                );

                final DataBufferByte dbb =
                    (DataBufferByte)tile.getDataBuffer();
                printTile(
                    (int)tileBounds.getMinX() + offset.x,
                    (int)tileBounds.getMinY() + offset.y,
                    winImage.getWidth(),
                    winImage.getHeight(),
                    dbb.getData()
                );
                thread.getProgressIndicator().incrementBy( 1 );
            }
        endPage();
        endPrinting();
    }

    /**
     * Sets the {@link PageFormat}.
     *
     * @param format The {@link PageFormat} to use.
     * @see #getPageFormat()
     */
    public static void setPageFormat( PageFormat format ) {
        final Paper paper = format.getPaper();
        setPageFormat(
            paper.getWidth(), paper.getHeight(),
            paper.getImageableX(), paper.getImageableY(),
            paper.getImageableWidth(), paper.getImageableHeight(),
            format.getOrientation()
        );
    }

    /**
     * Shows the native Windows Page Setup dialog.
     *
     * @param parent The parent window; may be <code>null</code>.
     * @return Returns <code>true</code> only if the user clicked OK.
     */
    public static native boolean showPageSetupDialog( Frame parent );

    /**
     * Shows the native Windows Print dialog.
     *
     * @param parent The parent window; may be <code>null</code>.
     * @return Returns <code>true</code> only if the user clicked Print.
     */
    public static native boolean showPrintDialog( Frame parent );

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Signals the native code to abort printing.
     */
    private static native void abortPrinting();

    /**
     * Begins a page.
     *
     * @see #endPage()
     */
    private static native void beginPage() throws PrinterException;

    /**
     * Begins printing.
     *
     * @param documentName The name of the image document.
     * @param bitsPerPixel The number of bits per pixel of the image.
     * @see #endPrinting()
     */
    private static native void beginPrinting( String documentName,
                                              int bitsPerPixel )
        throws PrinterException;

    /**
     * Ends a page.
     *
     * @see #beginPage()
     */
    private static native void endPage() throws PrinterException;

    /**
     * End printing.
     *
     * @see #beginPrinting(String,int)
     */
    private static native void endPrinting() throws PrinterException;

    /**
     * Checks whether the selected printer is capable of printing using our
     * implementation method.
     *
     * @return Returns <code>true</code> only if the printer is capable.
     */
    private static native boolean isPrinterCapable();

    /**
     * Print a tile.
     *
     * @param x The X coordinate in pixels of where the tile should be placed
     * on the paper.
     * @param y The Y coordinate in pixels of where the file should be placed
     * on the paper.
     * @param width The width in pixels of the tile.
     * @param height The height in pixels of the tile.
     * @param data The RGB image data of the tile.
     */
    private static native void printTile( int x, int y, int width, int height,
                                          byte[] data )
        throws PrinterException;

    /**
     * Sets the page format.
     *
     * @param paperWidth The paper width in 1/72" units.
     * @param paperHeight The paper height in 1/72" units.
     * @param imageableX The imagable X coordinate in 1/72" units.
     * @param imageableY The imagable Y coordinate in 1/72" units.
     * @param imageableWidth The imagable width in 1/72" units.
     * @param imageableHeight The imagable height in 1/72" units.
     * @param orientation The orientation, either {@link PageFormat#LANDSCAPE}
     * or {@link PageFormat#PORTRAIT}
     */
    private static native void setPageFormat( double paperWidth,
                                              double paperHeight,
                                              double imageableX,
                                              double imageableY,
                                              double imageableWidth,
                                              double imageableHeight,
                                              int orientation );

    static {
        System.loadLibrary( "Windows" );
    }

    ////////// main() for testing /////////////////////////////////////////////

    public static void main( String[] args ) throws Exception {
/*
        final ImageInfo info = ImageInfo.getInstanceFor( new File( args[0] ) );
        final PlanarImage image = info.getImage( null );

        final JFrame frame = new JFrame( "TIFF Image" );
        final JPanel imagePanel = new JPanel() {
            public void paintComponent( Graphics g ) {
                ((Graphics2D)g).drawRenderedImage( image, new AffineTransform() );
            }
        };
        imagePanel.setPreferredSize(
            new Dimension( image.getWidth(), image.getHeight() )
        );
        final JScrollPane scrollPane = new JScrollPane( imagePanel );
        frame.setContentPane( scrollPane );
        frame.pack();
        frame.setSize( 1000, 700 );
        frame.show();
*/

        showPageSetupDialog( null );
        final PageFormat pf = getPageFormat();
        dumpPage( pf );

        //print( image, args[0], frame );
        System.exit( 0 );
    }

    public static void dumpPage(PageFormat pageFormat) {
        System.out.println(
            "page area: w=" + pageFormat.getWidth()
            + ", h=" + pageFormat.getHeight()
            + ", orientation="
            + (pageFormat.getOrientation() == PageFormat.LANDSCAPE ? "landscape" : "portrait")
        );

        System.out.println(
            "imageable area: x=" + pageFormat.getImageableX()
            + ", y=" + pageFormat.getImageableY()
            + ", w=" + pageFormat.getImageableWidth()
            + ", h=" + pageFormat.getImageableHeight()
        );
    }

}
/* vim:set et sw=4 ts=4: */
