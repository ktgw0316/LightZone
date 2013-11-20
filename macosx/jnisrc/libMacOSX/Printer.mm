// standard
#import <appkit/AppKit.h>
#import <foundation/Foundation.h>
#import <Cocoa/Cocoa.h>
#import <jni.h>

// local
#include "LC_CocoaUtils.h"
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_MacOSXPrinter.h"
#endif

#include <iostream>

using namespace std;
using namespace LightCrafts;

static NSPrintInfo *g_info = [[NSPrintInfo sharedPrintInfo] retain];

////////// Objective C interface //////////////////////////////////////////////

@interface LCImageView : NSImageView {
}

- (NSPoint) locationOfPrintRect:
    (NSRect)r;

@end

////////// Objective C implementation /////////////////////////////////////////

@implementation LCImageView

- (NSPoint) locationOfPrintRect:
    (NSRect)r
{
    NSRect bounds = [self bounds];
    return NSMakePoint( -bounds.origin.x, -bounds.origin.y );
}

@end

////////// local //////////////////////////////////////////////////////////////

//
// Orientation  Cocoa  Java
// -----------  -----  ----
// Portrait     0      1
// Landscape    1      0
//
inline int orientationCocoaToJava( NSPrintingOrientation nsOrientation ) {
    return !nsOrientation;
}

inline NSPrintingOrientation orientationJavaToCocoa( int jOrientation ) {
    return static_cast<NSPrintingOrientation>( !jOrientation );
}

static NSPrintInfo *getSharedPrintInfo() {
    [g_info setHorizontalPagination:NSClipPagination];
    [g_info setVerticalPagination:NSClipPagination];
    [g_info setHorizontallyCentered:NO];
    [g_info setVerticallyCentered:NO];
    return g_info;
}

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXPrinter_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXPrinter,_,method)

/**
 * TODO
 */
JNIEXPORT void JNICALL MacOSXPrinter_METHOD(nativePageLayout)
    ( JNIEnv *env, jclass, jobject jPageFormatInfo,
      jboolean showPageLayoutDialog )
{
    auto_obj<NSAutoreleasePool> pool;
    NSPrintInfo *const info = getSharedPrintInfo();

    if ( showPageLayoutDialog )
        [[NSPageLayout pageLayout] runModalWithPrintInfo:info];

    NSSize size = [info paperSize];
    LC_setFloatField( env, jPageFormatInfo, "m_paperWidth", size.width );
    LC_setFloatField( env, jPageFormatInfo, "m_paperHeight", size.height );

    NSRect bounds = [info imageablePageBounds];
    LC_setFloatField( env, jPageFormatInfo, "m_pageBoundsX", bounds.origin.x );
    LC_setFloatField( env, jPageFormatInfo, "m_pageBoundsY", bounds.origin.y );
    LC_setFloatField( env, jPageFormatInfo, "m_pageBoundsWidth",
                      bounds.size.width );
    LC_setFloatField( env, jPageFormatInfo, "m_pageBoundsHeight",
                      bounds.size.height );

    int jOrientation = orientationCocoaToJava( [info orientation] );
    LC_setIntField( env, jPageFormatInfo, "m_orientation", jOrientation );

#ifdef DEBUG
    cout << "\nnativePageLayout()" << endl;
    cout << "------------------" << endl;
    cout << "paperWidth=" << size.width << endl;
    cout << "paperHeight=" << size.height << endl;
    cout << "pageBoundsX=" << bounds.origin.x << endl;
    cout << "pageBoundsY=" << bounds.origin.y << endl;
    cout << "pageBoundsWidth=" << bounds.size.width << endl;
    cout << "pageBoundsHeight=" << bounds.size.height << endl;
    cout << "orientation=" << jOrientation << endl;
    cout << "jobDisposition=" << [[info jobDisposition] UTF8String] << endl;
#endif
}

/**
 * TODO
 */
JNIEXPORT void JNICALL MacOSXPrinter_METHOD(nativePrint)
    ( JNIEnv *env, jclass, jstring jJobTitle, jstring jSpoolFile,
      jdouble jBoundsX, jdouble jBoundsY, jdouble jBoundsWidth,
      jdouble jBoundsHeight )
{
    auto_obj<NSAutoreleasePool> pool;
    NSString *const nsSpoolFile = LC_jstringToNSString( env, jSpoolFile );
    NSImage *const image = [[NSImage alloc] initWithContentsOfFile:nsSpoolFile];
    if ( [image isValid] ) {
        NSRect frame =
            NSMakeRect( jBoundsX, jBoundsY , jBoundsWidth, jBoundsHeight );
        NSImageView *const view = [[LCImageView alloc] initWithFrame:frame];
        [view setImage:image];
        [view setImageScaling:NSScaleProportionally];

        NSPrintInfo *const info = getSharedPrintInfo();

        NSPoint p = NSMakePoint(
            jBoundsX, [info paperSize].height - jBoundsHeight - jBoundsY
        );
        [view translateOriginToPoint:p];

#ifdef DEBUG
        cout << "\nnativePrint()" << endl;
        cout << "-------------" << endl;
        cout << "boundsX=" << jBoundsX << endl;
        cout << "boundsY=" << jBoundsY << endl;
        cout << "boundsWidth=" << jBoundsWidth << endl;
        cout << "boundsHeight=" << jBoundsHeight << endl;
        cout << "origin=(" << p.x << ',' << p.y << ')' << endl;
        cout << "jobDisposition=" << [[info jobDisposition] UTF8String] << endl;
#endif

        NSPrintOperation *const printOp =
            [NSPrintOperation printOperationWithView:view printInfo:info];
        //[printOp setJobTitle:LC_jstringToNSString( env, jJobTitle )];
        [printOp setShowsPrintPanel:NO];
        [printOp runOperation];
        [NSPrintOperation setCurrentOperation:nil];
    }
}

/**
 * Displays the native print dialog.
 */
JNIEXPORT jboolean JNICALL MacOSXPrinter_METHOD(nativePrintDialog)
    ( JNIEnv *env, jclass )
{
    auto_obj<NSAutoreleasePool> pool;
    NSPrintInfo *const info = getSharedPrintInfo();

    NSPrintPanel *const panel = [NSPrintPanel printPanel];
    [panel setJobStyleHint:NSPrintPhotoJobStyleHint];

    NSView *const view = [[NSView alloc] init];
    NSPrintOperation *const printOp =
        [NSPrintOperation printOperationWithView:view printInfo:info];
    [printOp setPrintPanel:panel];
    [NSPrintOperation setCurrentOperation:printOp];

    jboolean result = [panel runModal] == NSOKButton ? JNI_TRUE : JNI_FALSE;
    [g_info release];
    g_info = [[printOp printInfo] retain];
    [NSPrintOperation setCurrentOperation:nil];
    return result;
}

/**
 * Sets the native page format.
 */
JNIEXPORT void JNICALL MacOSXPrinter_METHOD(nativeSetPageFormat)
    ( JNIEnv *env, jclass, jint jOrientation, jdouble jWidth, jdouble jHeight,
      jdouble jImageableX, jdouble jImageableY, jdouble jImageableWidth,
      jdouble jImageableHeight )
{
    auto_obj<NSAutoreleasePool> pool;
    NSPrintInfo *const info = getSharedPrintInfo();

    [info setOrientation:orientationJavaToCocoa( jOrientation )];
    [info setPaperSize:NSMakeSize( jWidth, jHeight )];

    [info setTopMargin:static_cast<float>( jImageableY )];
    [info setLeftMargin:static_cast<float>( jImageableX )];

    [info setBottomMargin:
        static_cast<float>( jHeight - jImageableHeight - jImageableY )];
    [info setRightMargin:
        static_cast<float>( jWidth - jImageableWidth - jImageableX )];

#ifdef DEBUG
    cout << "\nnativeSetPageFormat()" << endl;
    cout << "---------------------" << endl;
    cout << "topMargin=" << jImageableY << endl;
    cout << "leftMargin=" << jImageableX << endl;
    cout << "bottomMargin=" << jHeight - jImageableHeight - jImageableY << endl;
    cout << "rightMargin=" << jWidth - jImageableWidth - jImageableX << endl;
    cout << "jobDisposition=" << [[info jobDisposition] UTF8String] << endl;
#endif
}

/* vim:set et sw=4 ts=4: */
