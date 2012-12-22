/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;

public class MacOSXPrinter {

    public static synchronized PageFormat getPageFormat( boolean pageDialog ) {
        PageFormatRunner pageFormatRunner = new PageFormatRunner( pageDialog );
        CocoaMainThreadRunner.invokeAndWait( pageFormatRunner );
        final PageFormatInfo info = pageFormatRunner.m_info;

        final Paper paper = new Paper();
        switch ( info.m_orientation ) {

            case PageFormat.PORTRAIT:
                paper.setSize( info.m_paperWidth, info.m_paperHeight );
                paper.setImageableArea(
                    info.m_pageBoundsX, info.m_pageBoundsY,
                    info.m_pageBoundsWidth, info.m_pageBoundsHeight
                );
                break;

            case PageFormat.LANDSCAPE:
                paper.setSize( info.m_paperHeight, info.m_paperWidth );
                paper.setImageableArea(
                    info.m_pageBoundsY, info.m_pageBoundsX,
                    info.m_pageBoundsHeight, info.m_pageBoundsWidth
                );
                break;
        }

        final PageFormat format = new PageFormat();
        format.setOrientation( info.m_orientation );
        format.setPaper( paper );
        return format;
    }

    public static synchronized void print( final String jobTitle,
                                           final File spoolFile,
                                           final Rectangle2D bounds ) {
        CocoaMainThreadRunner.invokeAndWait(
            new Runnable() {
                public void run() {
                    nativePrint(
                        jobTitle, spoolFile.getAbsolutePath(),
                        bounds.getX(), bounds.getY(),
                        bounds.getWidth(), bounds.getHeight()
                    );
                }
            }
        );
    }

    public static synchronized boolean printDialog() {
        final MutableBoolean result = new MutableBoolean();
        CocoaMainThreadRunner.invokeAndWait(
            new Runnable() {
                public void run() {
                    result.b = nativePrintDialog();
                }
            }
        );
        return result.b;
    }

    public static synchronized void setPageFormat( final PageFormat format ) {
        CocoaMainThreadRunner.invokeAndWait(
            new Runnable() {
                public void run() {
                    nativeSetPageFormat(
                        format.getOrientation(),
                        format.getWidth(), format.getHeight(),
                        format.getImageableX(), format.getImageableY(),
                        format.getImageableWidth(), format.getImageableHeight()
                    );
                }
            }
        );
    }

    ////////// private ////////////////////////////////////////////////////////

    private static final class MutableBoolean {
        boolean b;
    }

    private static final class PageFormatInfo {
        int m_orientation;

        float m_paperWidth;
        float m_paperHeight;

        float m_pageBoundsX;
        float m_pageBoundsY;
        float m_pageBoundsWidth;
        float m_pageBoundsHeight;
    }

    private static final class PageFormatRunner implements Runnable {

        PageFormatRunner( boolean pageDialog ) {
            m_pageDialog = pageDialog;
        }

        public void run() {
            nativePageLayout( m_info, m_pageDialog );
        }

        final PageFormatInfo m_info = new PageFormatInfo();
        final boolean m_pageDialog;
    }

    @SuppressWarnings( { "MethodOnlyUsedFromInnerClass" } )
    private static native void nativePageLayout( PageFormatInfo info,
                                                 boolean showPageLayoutDialog );

    private static native void nativePrint( String jobTitle, String spoolFile,
                                            double boundsX, double boundsY,
                                            double boundsWidth,
                                            double boundsHeight );

    private static native boolean nativePrintDialog();

    private static native void nativeSetPageFormat( int orientation,
                                                    double width,
                                                    double height,
                                                    double imageableX,
                                                    double imageableY,
                                                    double imageableWidth,
                                                    double imageableHeight );
}
/* vim:set et sw=4 ts=4: */
