/*
 * $RCSfile: OpImageTester.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/02/24 02:07:43 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.test;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.lang.reflect.Method;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.opimage.PatternOpImage;

public class OpImageTester {

    long start=0, end=0;
    int width, height, bands;
    RenderedImage source;
    private static final int allDataTypes[] = {0,1,2,3,4,5};
    private static final String dataTypeNames[] = {"BYTE  ","USHORT","SHORT ",
                                                   "INT   ","FLOAT ","DOUBLE"};
    
    public OpImageTester (int width, int height, int bands, int dataType){
        this.width = width;
        this.height = height;
        this.bands = bands;

        int tileWidth = width, tileHeight = height;
        Raster srcRaster =
           RasterFactory.createInterleavedRaster(dataType,
                                          tileWidth,tileHeight,bands,null);
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int bits[] = {8,8,8};
        ComponentColorModel ccm = 
           new ComponentColorModel(cs,bits,false,false,
                                   Transparency.OPAQUE,DataBuffer.TYPE_BYTE);

        source = new PatternOpImage(srcRaster, ccm, 0, 0, width, height);
    }

    public OpImageTester (int width, int height, int bands) {
       this(width,height,bands,DataBuffer.TYPE_BYTE);
    }

    public RenderedImage getSource() {
        return source;
    }

    public Raster[] getSrcRasters() {
        Raster[] r = {source.getTile(0,0)};
        return r;
    }

    public WritableRaster getDstRaster() {
        return RasterFactory.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                              width,height,bands,null);
    }

    public Rectangle getDstRect() {
        return new Rectangle(0,0,width,height);
    }

    public static OpImage createRandomOpImage(ImageLayout layout) {
        return new RandomOpImage(0, 0, 800, 800,
                                RasterFactory.createPixelInterleavedSampleModel(
                                             DataBuffer.TYPE_BYTE, 200, 200, 3),
                                 null,
                                 layout);
    }

    public static OpImage createRandomOpImage(Map configuration, 
					      ImageLayout layout) {
        return new RandomOpImage(0, 0, 800, 800,
                                RasterFactory.createPixelInterleavedSampleModel(
                                             DataBuffer.TYPE_BYTE, 200, 200, 3),
                                 configuration,
                                 layout);
    }

    public static ImageLayout createImageLayout(int minX, int minY,
                                                int width, int height,
                                                int tileX, int tileY,
                                                int tileWidth, int tileHeight,
                                                int dataType, int numBands,
                                                boolean isBanded) {
        SampleModel sampleModel;
        if (isBanded) {
            sampleModel = RasterFactory.createBandedSampleModel(
                          dataType, tileWidth, tileHeight, numBands);
        } else {
            sampleModel = RasterFactory.createPixelInterleavedSampleModel(
                                  dataType, tileWidth, tileHeight, numBands);
        }
        return new ImageLayout(minX, minY, width, height,
                               tileX, tileY, tileWidth, tileHeight,
                               sampleModel, createComponentColorModel());
    }

    public static ComponentColorModel createComponentColorModel() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int bits[] = {8, 8, 8};
        return new ComponentColorModel(cs, bits, false, false,
                                       Transparency.OPAQUE,
                                       DataBuffer.TYPE_BYTE);
    }

    public static void printPixels(String message, PlanarImage image,
                                   Rectangle rect) {
        int[] pixels = image.getData(rect).getPixels(
                       rect.x, rect.y, rect.width, rect.height, (int[])null);

        System.out.println(message +
                           " x=" + rect.x +
                           " y=" + rect.y +
                           " width=" + rect.width +
                           " height=" + rect.height);
        int j = 0;
        for (int h = 0; h < rect.height; h++) {
            System.out.print("    ");
            for (int w = 0; w < rect.width; w++) {
                System.out.print("( ");
                for (int b = 0; b < image.getSampleModel().getNumBands(); b++) {
                    System.out.print(pixels[j] + " ");
                    j++;
                }
                System.out.print(")");
            }
            System.out.println();
        }
    }

    /**
     * Print out the pixel values of the destination image within a
     * specific rectangle, and all of its source image(s) pixel values
     * within the cooresponding rectangle obtained by calling
     * <code>OpImage.mapDestRect()</code>.
     *
     * @param dst      The destination image to be tested.
     * @param dstRect  The rectangle of interest within dst.
     */
    public static void testOpImage(OpImage dst, Rectangle dstRect) {
        for (int i = 0; i < dst.getNumSources(); i++) {
            PlanarImage src = dst.getSourceImage(i);
            Rectangle srcRect = dst.mapDestRect(dstRect, i);
            String message = "Source " + (i+1) + ":";
            printPixels(message, src, srcRect);
        }
        printPixels("Dest:", dst, dstRect);
    }

    private static long benchmarkOpImage(OpImage img, int loop) {
        img.setTileCache(null);
        int minX = img.getMinTileX();
        int maxX = img.getMaxTileX();
        int minY = img.getMinTileY();
        int maxY = img.getMaxTileY();

        long total = 0;
        for (int i = 0; i < loop; i++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                //    System.gc();
                    long start = System.currentTimeMillis();
                    img.getTile(x, y);
                    long end = System.currentTimeMillis();
                //    System.gc();
                    int diff = (int)(end-start);
                    total += diff;
                }
            }
        }
        return total;
    }

    public static long timeOpImage(OpImage img, int loops) {

        long total = benchmarkOpImage(img,loops);

        int w = img.getWidth();
        int h = img.getHeight();
        SampleModel sm = img.getSampleModel();

        double time = (total)/1000.0;
        int width = img.getWidth();
        int height = img.getHeight();
        System.out.print("\tLoops : " + loops);
        System.out.print("\tTime : " + time);
        System.out.println("\tMpixels/sec : "
               +((double)loops/1000000.0)*((double)width*(double)height/time));

        return total;
    }

    public static void performDiagnostics(String classname, String args[]) {
        int dataTypes[] = allDataTypes;
        boolean verbose = false;
        int width = 512;
        int height = 512;
        int bands = 1;
        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-fast")) {
               int dt[] = {0};
               dataTypes = dt;
           } else if (args[i].equals("-verbose")) {
               verbose = true;
           } 
        }
        runDiagnostics(classname,dataTypes,width,height,bands,verbose);
    }

    public static void runDiagnostics(String classname, 
                                      int dataTypes[],
                                      int width,
                                      int height,
                                      int bands,
                                      boolean verbose) {
        System.out.println("Performing Diagnostics for " + classname);

        for (int i = 0; i < dataTypes.length; i++) {
            OpImageTester oit = new OpImageTester(512,512,1,dataTypes[i]);
            Class clazz = null;
            Method createTestMethod = null;
            System.out.print(" Testing DataBuffer.TYPE_" + 
                dataTypeNames[dataTypes[i]] + " ");
            System.out.println("  Size : " + width + "x" + height +
                " by " + bands + " bands");
            try {
                clazz = Class.forName(classname);
                Class params[] = {
                    Class.forName("com.lightcrafts.media.jai.opimage.OpImageTester")};
                createTestMethod = clazz.getMethod("createTestImage",params);
                Object methodArgs[] = {oit};
                OpImage o = (OpImage)createTestMethod.invoke(null,methodArgs);
     
                long total = benchmarkOpImage(o,10);
                total = benchmarkOpImage(o,10);
    
                int loops = (int)(15000/((double)total/10.0));
    
                timeOpImage(o,loops);
    
            } catch (Exception e) {
                if (verbose) {
                    e.printStackTrace();
                }
                System.err.println("\tException thrown");
            }
        }
        System.out.println("Finished Diagnostics\n");
    }
}
