/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * UI.mm
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#import <cstdlib>                       /* for exit(3) */

// local
#include "LC_CocoaUtils.h"
#include "UI.h"

using namespace std;
using namespace LightCrafts;

/**
 * Show an alert dialog box with the given message in it and quit.  Only the
 * msg string is localized first.
 */
void LC_dieWithoutLocalizedInfo( NSString *msg, NSString *info ) {
    [NSApplication sharedApplication];
    auto_obj<NSAutoreleasePool> pool;
    //
    // We don't use NSAlert because it's not in Mac OS X until 10.3.
    //
    NSRunCriticalAlertPanel(
        NSLocalizedString(msg,nil), info ? info : nil,
        NSLocalizedString(@"Quit",nil), nil, nil, nil
    );
    ::exit( 1 );
}

/* vim:set et sw=4 ts=4: */
