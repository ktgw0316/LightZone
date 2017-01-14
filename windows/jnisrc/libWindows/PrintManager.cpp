/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <jni.h>

// windows
#include <windows.h>

// local
#include "LC_JNIUtils.h"
#include "LC_WinError.h"
#include "LC_WinUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_windows_WindowsPrintManager.h"
#endif

#include <iostream>

using namespace std;
using namespace LightCrafts;

double const INCH2MM     = 25.4;
int    const INCH2POINTS = 72;
double const MM2INCH     = 1.0 / INCH2MM;
double const POINTS2INCH = 1.0 / INCH2POINTS;

int const PageFormat_LANDSCAPE = 0;
int const PageFormat_PORTRAIT  = 1;

static BOOL             gAbortedPrinting;
static HGLOBAL          gDevMode, gDevNames;
static BITMAPINFO       gBMI;
static RECT             gMargins = { 1000, 1000, 1000, 1000 };
static PRINTDLGEX       gPD;
static PRINTPAGERANGE   gPPR;

////////// Local functions ////////////////////////////////////////////////////

/**
 * Dispose of native resources.
 */
static void dispose() {
    if ( gPD.hDC ) {
        ::DeleteDC( gPD.hDC );
        gPD.hDC = NULL;
    }
    if ( gDevMode ) {
        ::GlobalFree( gDevMode );
        gDevMode = NULL;
    }
    if ( gDevNames ) {
        ::GlobalFree( gDevNames );
        gDevNames = NULL;
    }
}

/**
 * Gets the imageable area of the paper in points (1/72" units).
 */
static bool getPaperImageableArea( jdouble *pX, jdouble *pY, jdouble *pWidth,
                                   jdouble *pHeight ) {
    if ( !gPD.hDC )
        return false;

    //
    // The width/height of the physical page, in device units.  For example, a
    // printer set to print at 600 dpi on 8.5-x11" paper has a physical width
    // value of 5100 device units.
    //
    int const pageWidthDU  = ::GetDeviceCaps( gPD.hDC, PHYSICALWIDTH  );
    int const pageHeightDU = ::GetDeviceCaps( gPD.hDC, PHYSICALHEIGHT );

    //
    // The distance from the left/top edge of the physical page to the left/top
    // edge of the printable area, in device units. For example, a printer set
    // to print at 600 dpi on 8.5-by-11" paper, that cannot print on the
    // leftmost 0.25" of paper, has a horizontal physical offset of 150 device
    // units.
    //
    int const dxDU = ::GetDeviceCaps( gPD.hDC, PHYSICALOFFSETX );
    int const dyDU = ::GetDeviceCaps( gPD.hDC, PHYSICALOFFSETY );

    //
    // Convert device units to logical units (in this case, to .01" units).
    //
    POINT p[] = {
        { dxDU, dyDU },
        { pageWidthDU - dxDU * 2, pageHeightDU - dyDU * 2 }
    };
    int const oldMapMode = ::SetMapMode( gPD.hDC, MM_LOENGLISH );
    ::DPtoLP( gPD.hDC, p, 2 );
    ::SetMapMode( gPD.hDC, oldMapMode );

    //
    // Convert to points (because Java wants it that way).
    //
    *pX      =   p[0].x / 100.0 * INCH2POINTS;
    *pY      = - p[0].y / 100.0 * INCH2POINTS;
    *pWidth  =   p[1].x / 100.0 * INCH2POINTS;
    *pHeight = - p[1].y / 100.0 * INCH2POINTS;

    return true;
}

/**
 * Gets the width & height of the paper in points (1/72" units).
 */
static bool getPaperSize( jdouble *pWidth, jdouble *pHeight ) {
    if ( !gPD.hDC )
        return false;

    auto_GlobalLock<DEVMODE const*> const pDM( gDevMode );
    if ( pDM->dmFields & DM_PAPERLENGTH && pDM->dmFields & DM_PAPERWIDTH ) {
        //
        // Windows paper dimensions are in .1mm units.
        //
        *pWidth  = pDM->dmPaperWidth  * MM2INCH * INCH2POINTS / 10;
        *pHeight = pDM->dmPaperLength * MM2INCH * INCH2POINTS / 10;
        return true;
    }

    //
    // The width/height of the physical page, in device units.  For example, a
    // printer set to print at 600 dpi on 8.5-x11" paper has a physical width
    // value of 5100 device units.
    //
    int const pageWidthDU  = ::GetDeviceCaps( gPD.hDC, PHYSICALWIDTH );
    int const pageHeightDU = ::GetDeviceCaps( gPD.hDC, PHYSICALHEIGHT );

    //
    // Convert device units to logical units (in this case, to 0.01" units).
    //
    POINT p = { pageWidthDU, pageHeightDU };
    int const oldMapMode = ::SetMapMode( gPD.hDC, MM_LOENGLISH );
    ::DPtoLP( gPD.hDC, &p, 1 );
    ::SetMapMode( gPD.hDC, oldMapMode );

    //
    // Convert to points (because Java wants it that way).
    //
    *pWidth  =   p.x / 100.0 * INCH2POINTS;
    *pHeight = - p.y / 100.0 * INCH2POINTS;

    return true;
}

/**
 * Throw a PrinterException back on the Java side.
 */
static void LC_throwPrinterException( JNIEnv *env, char const *msg ) {
    if ( env->ExceptionCheck() )
        return;
    static char const PrinterExceptionClass[] =
        "java/awt/print/PrinterException";
    env->ThrowNew( LC_findClassOrDie( env, PrinterExceptionClass ), msg );
}

/**
 * Throw a PrinterException back on the Java side using the error message for
 * the last Windows error.
 */
inline void LC_throwPrinterException( JNIEnv *env ) {
    LC_throwPrinterException(
        env, LC_formatError( "Printing", ::GetLastError() )
    );
}

/**
 * A standard Windows print-abort procedure.
 */
extern "C" BOOL CALLBACK printAbortProc( HDC hDC, int error ) {
    MSG msg;
    while ( ::PeekMessage( (LPMSG)&msg, NULL, 0, 0, PM_REMOVE ) ) {
        ::TranslateMessage( &msg );
        ::DispatchMessage( &msg );
    }
    return !gAbortedPrinting;
}

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsPrintManager_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_windows_WindowsPrintManager,_,method)

/**
 * Abort printing.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(abortPrinting)
    ( JNIEnv *env, jclass )
{
    gAbortedPrinting = true;
}

/**
 * Begin a page.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(beginPage)
    ( JNIEnv *env, jclass )
{
    if ( ::StartPage( gPD.hDC ) <= 0 )
        LC_throwPrinterException( env );
}

/**
 * Begin printing.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(beginPrinting)
    ( JNIEnv *env, jclass, jstring jDocName, jint jBitsPerPixel )
{
    jstring_to_w const wDocName( env, jDocName );

    gAbortedPrinting = false;
    if ( ::SetAbortProc( gPD.hDC, printAbortProc ) == SP_ERROR )
        goto error;

    DOCINFO di;
    ::memset( &di, 0, sizeof di );
    di.cbSize = sizeof di;
    di.lpszDocName = wDocName;

    if ( ::StartDoc( gPD.hDC, &di ) <= 0 )
        goto error;

    ::memset( &gBMI, 0, sizeof gBMI );
    PBITMAPINFOHEADER pBMIH;
    pBMIH = &gBMI.bmiHeader;
    pBMIH->biSize        = sizeof( BITMAPINFOHEADER );
    pBMIH->biPlanes      = 1;
    pBMIH->biBitCount    = jBitsPerPixel;
    pBMIH->biCompression = BI_RGB;

    return;
error:
    LC_throwPrinterException( env );
}

/**
 * Dispose of native resources.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(dispose)
    ( JNIEnv *env, jclass )
{
    dispose();
}

/**
 * End a page.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(endPage)
    ( JNIEnv *env, jclass )
{
    if ( ::EndPage( gPD.hDC ) <= 0 )
        LC_throwPrinterException( env );
}

/**
 * End printing.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(endPrinting)
    ( JNIEnv *env, jclass )
{
    int const result = gAbortedPrinting ?
        ::AbortDoc( gPD.hDC ) : ::EndDoc( gPD.hDC );
    if ( result <= 0 )
        LC_throwPrinterException( env );
}

/**
 * Set the fields of a Java java.awt.print.PageFormat object.
 */
JNIEXPORT jobject JNICALL WindowsPrintManager_METHOD(getPageFormat)
    ( JNIEnv *env, jclass )
{
    if ( !gPD.hDC )
        return NULL;

    ////////// Construct a PageFormat object //////////////////////////////////

    char const PageFormatClass[] = "java/awt/print/PageFormat";
    jclass const jPageFormatClass = LC_findClassOrDie( env, PageFormatClass );
    jmethodID const jPageFormatCtorMethodID =
        env->GetMethodID( jPageFormatClass, "<init>", "()V" );
    if ( !jPageFormatCtorMethodID )
        return NULL;
    jobject jPageFormat =
        env->NewObject( jPageFormatClass, jPageFormatCtorMethodID );
    if ( !jPageFormat )
        return NULL;

    ////////// Construct a Paper object ///////////////////////////////////////

    char const PaperClass[] = "java/awt/print/Paper";
    jclass const jPaperClass = LC_findClassOrDie( env, PaperClass );
    jmethodID const jPaperCtorMethodID =
        env->GetMethodID( jPaperClass, "<init>", "()V" );
    if ( !jPaperCtorMethodID )
        return NULL;
    jobject jPaper = env->NewObject( jPaperClass, jPaperCtorMethodID );
    if ( !jPaper )
        return NULL;

    jmethodID const setSizeMethodID =
        env->GetMethodID( jPaperClass, "setSize", "(DD)V" );
    if ( !setSizeMethodID )
        return NULL;

    jdouble jPaperWidth, jPaperHeight;
    getPaperSize( &jPaperWidth, &jPaperHeight );
    env->CallVoidMethod( jPaper, setSizeMethodID, jPaperWidth, jPaperHeight );

    jmethodID const setImageableAreaMethodID =
        env->GetMethodID( jPaperClass, "setImageableArea", "(DDDD)V" );
    if ( !setImageableAreaMethodID )
        return NULL;

    env->CallVoidMethod(
        jPaper, setImageableAreaMethodID,
        (jdouble)gMargins.left / 1000 * INCH2POINTS,
        (jdouble)gMargins.top  / 1000 * INCH2POINTS,
        jPaperWidth  - ((jdouble)(gMargins.left + gMargins.right)
                                    / 1000 * INCH2POINTS),
        jPaperHeight - ((jdouble)(gMargins.top + gMargins.bottom)
                                    / 1000 * INCH2POINTS)
    );

    ////////// Set the Paper object in the PageFormat object //////////////////

    jmethodID const setPaperMethodID = env->GetMethodID(
        jPageFormatClass, "setPaper", "(Ljava/awt/print/Paper;)V"
    );
    if ( !setPaperMethodID )
        return NULL;
    env->CallVoidMethod( jPageFormat, setPaperMethodID, jPaper );

    ////////// Set the orientation of the PageFormat //////////////////////////

    auto_GlobalLock<DEVMODE const*> const pDM( gDevMode );

    if ( pDM->dmFields & DM_ORIENTATION ) {
        jint jOrientation;
        switch ( pDM->dmOrientation ) {
            case DMORIENT_LANDSCAPE:
                jOrientation = PageFormat_LANDSCAPE;
                break;
            case DMORIENT_PORTRAIT:
                jOrientation = PageFormat_PORTRAIT;
                break;
        }
        jmethodID const setOrientationMethodID = env->GetMethodID(
            jPageFormatClass, "setOrientation", "(I)V"
        );
        if ( !setOrientationMethodID )
            return NULL;
        env->CallVoidMethod(
            jPageFormat, setOrientationMethodID, jOrientation
        );
    }

    return jPageFormat;
}

/**
 * Initialize the printer to the default printer.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(initDefaultPrinter)
    ( JNIEnv *env, jclass )
{
    dispose();
    ::memset( &gPD, 0, sizeof gPD );
    gPD.lStructSize  = sizeof gPD;
    gPD.hwndOwner    = ::GetForegroundWindow();
    gPD.Flags        = PD_ALLPAGES                   |
                       PD_NOCURRENTPAGE              |
                       PD_NOPAGENUMS                 |
                       PD_NOSELECTION                |
                       PD_RETURNDC                   |
                       PD_RETURNDEFAULT              |
                       PD_USEDEVMODECOPIESANDCOLLATE ;
    gPD.nStartPage   = START_PAGE_GENERAL;

    if ( ::PrintDlgEx( &gPD ) == S_OK ) {
        gDevMode  = gPD.hDevMode;
        gDevNames = gPD.hDevNames;
    }
}

/**
 * Check whether the selected printer is capable of printing using our
 * implementation method.
 */
JNIEXPORT jboolean JNICALL WindowsPrintManager_METHOD(isPrinterCapable)
    ( JNIEnv *env, jclass )
{
    if ( !( gPD.hDC && ::GetDeviceCaps( gPD.hDC, RASTERCAPS ) & RC_DIBTODEV ) )
        return JNI_FALSE;
    return JNI_TRUE;
}

/**
 * Print a tile.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(printTile)
    ( JNIEnv *env, jclass, jint jX, jint jY, jint jWidth, jint jHeight,
      jbyteArray jData )
{
    PBITMAPINFOHEADER pBMIH  = &gBMI.bmiHeader;
    pBMIH->biWidth           = jWidth;
    pBMIH->biHeight          = - jHeight;

    jarray_to_c<jbyte> const cData( env, jData );
    int const nScanLines = ::SetDIBitsToDevice(
        gPD.hDC,
        jX, jY, jWidth, jHeight,
        0, 0, 0, jHeight,
        cData, &gBMI, DIB_RGB_COLORS
    );
    if ( !nScanLines )
        LC_throwPrinterException( env );
}

/**
 * Sets the PageFormat.
 */
JNIEXPORT void JNICALL WindowsPrintManager_METHOD(setPageFormat)
    ( JNIEnv *env, jclass, jdouble jPaperW, jdouble jPaperH,
      jdouble jImageableX, jdouble jImageableY, jdouble jImageableW,
      jdouble jImageableH, jint jOrientation )
{
    auto_GlobalLock<DEVMODE*> const pDM( gDevMode );

    //
    // Windows paper dimensions are in .1mm units.
    //
    pDM->dmPaperWidth  = (short)(jPaperW  * POINTS2INCH * INCH2MM * 10);
    pDM->dmPaperLength = (short)(jPaperH * POINTS2INCH * INCH2MM * 10);

    switch ( jOrientation ) {
        case PageFormat_LANDSCAPE:
            pDM->dmOrientation = DMORIENT_LANDSCAPE;
            break;
        case PageFormat_PORTRAIT:
            pDM->dmOrientation = DMORIENT_PORTRAIT;
            break;
    }

    pDM->dmFields |= DM_PAPERWIDTH | DM_PAPERLENGTH | DM_ORIENTATION;

    //
    // Margins are in .001" units.
    //
    gMargins.left   = (LONG)(jImageableX * POINTS2INCH * 1000);
    gMargins.top    = (LONG)(jImageableY * POINTS2INCH * 1000);
    gMargins.right  = (LONG)( (jPaperW - jImageableW) * POINTS2INCH * 1000)
                    - gMargins.left;
    gMargins.bottom = (LONG)( (jPaperH - jImageableH) * POINTS2INCH * 1000)
                    - gMargins.top;

    jdouble jPaperImageableX, jPaperImageableY,
            jPaperImageableW, jPaperImageableH;
    if ( getPaperImageableArea( &jPaperImageableX, &jPaperImageableY,
                                &jPaperImageableW, &jPaperImageableH ) ) {
        LONG const minL = (LONG)(jPaperImageableX * POINTS2INCH * 1000);
        LONG const minT = (LONG)(jPaperImageableY * POINTS2INCH * 1000);
        LONG const minR = (LONG)( (jPaperW  - jPaperImageableW)
                                        * POINTS2INCH * 1000 ) - minL;
        LONG const minB = (LONG)( (jPaperH - jPaperImageableH)
                                        * POINTS2INCH * 1000 ) - minT;
        //
        // Ensure the margins are within the imageable area.
        //
        if ( gMargins.left   < minL ) gMargins.left   = minL;
        if ( gMargins.top    < minT ) gMargins.top    = minT;
        if ( gMargins.right  < minR ) gMargins.right  = minR;
        if ( gMargins.bottom < minB ) gMargins.bottom = minB;
    }
}

/**
 * Show the native Windows Page Setup dialog.
 */
JNIEXPORT jboolean JNICALL WindowsPrintManager_METHOD(showPageSetupDialog)
    ( JNIEnv *env, jclass, jobject jParentWindow )
{
    PAGESETUPDLG psd;
    ::memset( &psd, 0, sizeof psd );
    psd.lStructSize = sizeof psd;
    psd.hwndOwner   = LC_getHWNDFromAWTComponent( env, jParentWindow );
    psd.Flags       = PSD_INTHOUSANDTHSOFINCHES | PSD_MARGINS;
    psd.hDevMode    = gDevMode;
    psd.hDevNames   = gDevNames;
    psd.rtMargin    = gMargins;

    if ( ::PageSetupDlg( &psd ) ) {
        gDevMode  = psd.hDevMode;
        gDevNames = psd.hDevNames;
        gMargins  = psd.rtMargin;
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

/**
 * Show the native Windows Print dialog.
 */
JNIEXPORT jboolean JNICALL WindowsPrintManager_METHOD(showPrintDialog)
    ( JNIEnv *env, jclass, jobject jParentWindow )
{
    ::memset( &gPPR, 0, sizeof gPPR );
    gPPR.nFromPage = gPPR.nToPage = 1;

    ::memset( &gPD, 0, sizeof gPD );
    gPD.lStructSize  = sizeof gPD;
    gPD.hwndOwner    = LC_getHWNDFromAWTComponent( env, jParentWindow );
    gPD.hDevMode     = gDevMode;
    gPD.hDevNames    = gDevNames;
    gPD.Flags        = PD_ALLPAGES                   |
                       PD_NOCURRENTPAGE              |
                       PD_NOPAGENUMS                 |
                       PD_NOSELECTION                |
                       PD_RETURNDC                   |
                       PD_USEDEVMODECOPIESANDCOLLATE ;
    gPD.lpPageRanges = &gPPR;
    gPD.nMinPage     = gPD.nMaxPage = 1;
    gPD.nCopies      = 1;
    gPD.nStartPage   = START_PAGE_GENERAL;

    bool result = false;
    if ( ::PrintDlgEx( &gPD ) == S_OK ) {
        switch ( gPD.dwResultAction ) {
            case PD_RESULT_PRINT:
                result = true;
                // no break;
            case PD_RESULT_APPLY:
                gDevMode  = gPD.hDevMode;
                gDevNames = gPD.hDevNames;
                break;
        }
    }
    return result;
}

/**
 * Get the printer's resolution in pixels per inch.
 */
JNIEXPORT jobject JNICALL WindowsPrintManager_METHOD(getPrinterResolution)
    ( JNIEnv *env, jclass )
{
    if ( !gPD.hDC )
        return NULL;

    int const ppiX = ::GetDeviceCaps( gPD.hDC, LOGPIXELSX );
    int const ppiY = ::GetDeviceCaps( gPD.hDC, LOGPIXELSY );

    ////////// Construct a Dimension object ///////////////////////////////////

    char const DimensionClass[] = "java/awt/Dimension";
    jclass const jDimensionClass = LC_findClassOrDie( env, DimensionClass );
    jmethodID const jDimensionCtorMethodID =
        env->GetMethodID( jDimensionClass, "<init>", "(II)V" );
    if ( !jDimensionCtorMethodID )
        return NULL;
    return env->NewObject(
        jDimensionClass, jDimensionCtorMethodID, ppiX, ppiY
    );
}

/* vim:set et sw=4 ts=4: */
