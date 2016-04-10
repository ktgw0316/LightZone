/* Copyright (C) 2005-2011 Fabio Riccardi */

#import <Cocoa/Cocoa.h>

/**
 * Define if we're to check a web site to obtain the latest version of the
 * application and, if the version that crashed is older, forbid submitting
 * crash reports.
 */
/* #define FORBID_OLD_VERSIONS */

/**
 * A CrashController is the main class for this application.
 */
@interface CrashController : NSObject
{
    IBOutlet NSTextView*            descriptionText;
    IBOutlet NSButton*              detailsOKButton;
    IBOutlet NSPanel*               detailsPanel;
    IBOutlet NSTextView*            detailsText;
    IBOutlet NSWindow*              mainWindow;
    IBOutlet NSProgressIndicator*   progressIndicator;
    IBOutlet NSButton*              sendButton;
    IBOutlet NSButton*              showDetailsButton;
    IBOutlet NSTextField*           statusText;

    NSString*                       crashedAppName;
    NSString*                       crashedAppBuild;
    NSAppleScript*                  killAppleCrashReporterScript;
    NSString*                       logContents;
#ifdef  FORBID_OLD_VERSIONS
    NSString*                       latestAppBuild;
#endif
}

- (IBAction) hideDetails:
    (id)sender;

- (void) interactWithUser;

- (void) quit;

- (BOOL) readCrashedAppInfo;

- (BOOL) readCrashLog:
    (NSString*)logPath;

- (IBAction) sendReport:
    (id)sender;

- (void) setStatusMessage:
    (NSString*)message;

- (int) showAlert:
    (NSString*)msg
    info:(NSString*)info
    localizeInfo:(BOOL)localizeInfo
    button1:(NSString*)button1
    button2:(NSString*)button2;

- (IBAction) showDetails:
    (id)sender;

- (void) startTask:
    (NSString*)message;
    
- (void) stopTask;

- (void) waitForCrashLog:
    (NSTimer*)timer;

#ifdef  FORBID_OLD_VERSIONS

- (BOOL) fetchLatestAppVersion;

- (void) showOldVersionAlertAndQuit;

#endif  /* FORBID_OLD_VERSIONS */

@end
/* vim:set et sw=4 ts=4: */
