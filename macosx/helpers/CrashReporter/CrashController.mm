#import "CrashController.h"
#import "LC_ModalAlertSheet.h"

/**
 * The application name we're the crash reporter for (for debugging only).
 */
#define DEBUG_APP_NAME      "LightZone"

#ifndef DEBUG
////////// Production /////////////////////////////////////////////////////////

#define CRASH_REPORT_URL    "http://crash.lightcrafts.com/cgi-bin/crash.cgi"

#ifdef  FORBID_OLD_VERSIONS

#define APP_DOWNLOAD_URL    "http://download.lightcrafts.com/lightzone/"
#define LATEST_APP_VERSION_URL \
    "http://download.lightcrafts.com/lightzone/lc_version.plist"

#endif  /* FORBID_OLD_VERSIONS */

#else   /* DEBUG */
////////// Debug //////////////////////////////////////////////////////////////

#define CRASH_REPORT_URL    "http://localhost/cgi-bin/crash.cgi"

#ifdef  FORBID_OLD_VERSIONS

#define APP_DOWNLOAD_URL    "http://www.lightcrafts.com/"
#define LATEST_APP_VERSION_URL \
    "http://www.pauljlucas.org:81/lightzone/lc_version.plist"
    
#endif  /* FORBID_OLD_VERSIONS */

#endif  /* DEBUG */
///////////////////////////////////////////////////////////////////////////////

int const   AppleCrashReporterKillAttempts  = 200;
float const AppleCrashReporterKillInterval  = 0.1;

int const   CrashLogWaitAttempts            = 100;
float const CrashLogWaitInterval            = 0.2;
int const   SendCrashReportTimeout          = 60;   // seconds

#ifdef  FORBID_OLD_VERSIONS
int const   FetchLatestAppVersionTimeout    = 60;   // seconds
#endif

@implementation CrashController

/**
 * We're done launching: start running.
 */
- (void) applicationDidFinishLaunching:
    (NSNotification*)notification
{
    [statusText setStringValue:@""];    // get rid of placeholder text

    //
    // First, kill off Apple's Crash Reporter.
    //
    [NSTimer
        scheduledTimerWithTimeInterval:AppleCrashReporterKillInterval
        target:self
        selector:@selector(killAppleCrashReporter:)
        userInfo:nil
        repeats:YES
    ];

    // Display the main window.
    [mainWindow makeKeyAndOrderFront:nil];

    //
    // Get the crashed app's and latest app's versions and compare.
    //
    [self readCrashedAppInfo];
#ifdef  FORBID_OLD_VERSIONS
    if ( [self fetchLatestAppVersion] &&
         ![latestAppBuild isEqualToString:crashedAppBuild] )
        [self showOldVersionAlertAndQuit];
#endif

    //
    // Then wait for a crash log to appear.
    //
    [NSTimer
        scheduledTimerWithTimeInterval:CrashLogWaitInterval
        target:self
        selector:@selector(waitForCrashLog:)
        userInfo:nil
        repeats:YES
    ];
}

/**
 * Make sure the application quits when the last window is closed.
 */
- (BOOL) applicationShouldTerminateAfterLastWindowClosed:
    (NSApplication*)app
{
    return YES;
}

/**
 * Initialize a CrashController.
 */
- (id) init
{
    self = [super init];

    crashedAppBuild = nil;
    logContents = nil;
#ifdef  FORBID_OLD_VERSIONS
    latestAppBuild = nil;
#endif

    NSString *const script =
        @"tell application \"UserNotificationCenter\" to quit";
    killAppleCrashReporterScript =
        [[[NSAppleScript alloc] initWithSource:script] retain];

    return self;
}

/**
 * Deallocate a CrashController.
 */
- (void) dealloc
{
    [crashedAppName release];
    [crashedAppBuild release];
    [killAppleCrashReporterScript release];
    [logContents release];
#ifdef  FORBID_OLD_VERSIONS
    [latestAppBuild release];
#endif
    [super dealloc];
}

#ifdef  FORBID_OLD_VERSIONS
/**
 * Fetch the latest application version from our web site.
 */
- (BOOL) fetchLatestAppVersion
{
    [self startTask:NSLocalizedString(@"Getting latest version info",nil)];

    NSURLRequest *const request =
        [NSURLRequest
            requestWithURL:[NSURL URLWithString:@LATEST_APP_VERSION_URL]
            cachePolicy:NSURLRequestReloadIgnoringCacheData
            timeoutInterval:FetchLatestAppVersionTimeout
        ];

    NSURLResponse *response;
    NSError *error;
    NSData *const data =
        [NSURLConnection
            sendSynchronousRequest:request
            returningResponse:&response error:&error
        ];
        
    [self stopTask];
    if ( data && ![error code] ) {
        NSDictionary *const latestAppVersionDict =
            [NSPropertyListSerialization
                propertyListFromData:data
                mutabilityOption:NSPropertyListImmutable
                format:nil
                errorDescription:nil
            ];
        id const build = [latestAppVersionDict objectForKey:@"Build"];
        if ( ![build isKindOfClass:[NSString class]] )
            return NO;
        latestAppBuild = (NSString*)[build retain];
        return YES;
    }
    return NO;
}
#endif  /* FORBID_OLD_VERSIONS */

/**
 * Hide the crash report details sheet.
 */
- (void) hideDetails:
    (id)sender
{
    [detailsPanel orderOut:self];       // roll up the sheet
    [NSApp endSheet:detailsPanel returnCode:NSOKButton];
}

/**
 * Interact with the user and wait for the user to click the Send Report
 * button.
 */
- (void) interactWithUser
{
    //
    // Pre-select the the default details text (the instructions to the user)
    // so that when the user starts to type, it will completely replace the
    // default text.
    //
    int const defaultDetailsTextLength = [[detailsText textStorage] length];
    [detailsText setSelectedRange:NSMakeRange( 0, defaultDetailsTextLength )];

    [sendButton setEnabled:YES];
    [showDetailsButton setEnabled:YES];
}

/**
 * This gets called repeatedly to try to kill Apple's default CrashReporter.
 */
- (void) killAppleCrashReporter:
    (NSTimer*)timer
{
    static int killAttempts = AppleCrashReporterKillAttempts;
    if ( !killAttempts-- ||
         ![[killAppleCrashReporterScript executeAndReturnError:nil]
            booleanValue] )
        [timer invalidate];
}

/**
 * Quit.
 */
- (void) quit
{
    [mainWindow performClose:nil];
    [NSApp terminate:nil];
}

/**
 * Read the crashed app's version information.
 */
- (BOOL) readCrashedAppInfo
{
    NSString *const bundlePath = [[NSBundle mainBundle] bundlePath];
    //
    // We assume our bundle path is like:
    //
    //      CrashedApp.app/Contents/Resources/CrashReporter.app
    //
    // i.e., we're inside the Resources directory of our "parent" application.
    // Hence, its version information is one level up.
    //
    NSDictionary const *const crashedAppInfoDict =
        [NSDictionary dictionaryWithContentsOfFile:
            [bundlePath stringByAppendingPathComponent:@"/"
#ifdef  DEBUG
                "../../../../../release/" DEBUG_APP_NAME ".app/Contents"
#else
                "../.."
#endif
                "/Info.plist"
            ]
        ];
    if ( !crashedAppInfoDict ) {
        [self
            showAlert:@"Unexpected"
                info:@"Can't get crashed app's Info.plist" localizeInfo:YES
                button1:@"Quit" button2:nil];
        [self quit];
    }
    crashedAppName = [crashedAppInfoDict objectForKey:@"CFBundleName"];
    if ( !crashedAppName ) {
        [self
            showAlert:@"Unexpected"
                info:@"Can't get crashed app's name" localizeInfo:YES
                button1:@"Quit" button2:nil];
        [self quit];
    }
    [crashedAppName retain];

    NSDictionary const *const crashedAppVersionDict =
        [NSDictionary dictionaryWithContentsOfFile:
            [bundlePath stringByAppendingPathComponent:@"/"
#ifdef  DEBUG
                "../../../../../release/" DEBUG_APP_NAME ".app/Contents"
#else
                ".."
#endif
                "/lc_version.plist"
            ]
        ];
    if ( !crashedAppVersionDict )
        return NO;
    id const build = [crashedAppVersionDict objectForKey:@"Build"];
    if ( ![build isKindOfClass:[NSString class]] )
        return NO;
    crashedAppBuild = (NSString*)[build retain];
    return YES;
}

/**
 * Checks whether the given crash log exists and isn't empty.  If so, reads it
 * all in.
 */
- (BOOL) readCrashLog:
    (NSString*)logPath
{
    if ( ![[NSFileManager defaultManager] fileExistsAtPath:logPath] )
        return NO;
    logContents = [NSString stringWithContentsOfFile:logPath];
    if ( !(logContents && [logContents length]) )
        return NO;
#ifndef DEBUG
    //
    // Trash the log file so this crash is never reported again.
    //
    [[NSFileManager defaultManager] removeFileAtPath:logPath handler:nil];
#endif
    //
    // Look for and, if found, strip off the "Thread State" and "Binary Images
    // Description" -- we don't need all that.
    //
    NSRange range = [logContents rangeOfString:@"Thread State"];
    if ( range.location != NSNotFound )
        logContents = [logContents substringToIndex:range.location];

    [logContents retain];
    return YES;
}

/**
 * Send the crash report.
 */
- (IBAction) sendReport:
    (id)sender
{
    [showDetailsButton setEnabled:NO];
    [sendButton setEnabled:NO];

    //
    // Construct the HTTP request.
    //
    NSMutableURLRequest *const request =
        [NSMutableURLRequest
            requestWithURL:[NSURL URLWithString:@CRASH_REPORT_URL]
            cachePolicy:NSURLRequestReloadIgnoringCacheData
            timeoutInterval:SendCrashReportTimeout
        ];

    [request setHTTPMethod:@"POST"];
    [request
        setValue:@"application/x-www-form-urlencoded"
        forHTTPHeaderField:@"Content-type"
    ];

    //
    // Add a custom HTTP header of the form:
    //
    //      X-LightCrafts-CrashReport: LightZone (build)
    //
    NSMutableString *const crashedAppHeaderValue =
        [NSMutableString stringWithString:crashedAppName];
    [crashedAppHeaderValue appendString:@" ("];
    [crashedAppHeaderValue
        appendString:crashedAppBuild ? crashedAppBuild : @"unknown"];
    [crashedAppHeaderValue appendString:@")"];
    [request
        addValue:crashedAppHeaderValue
        forHTTPHeaderField:@"X-LightCrafts-CrashReport"
    ];

    //
    // Construct the report.
    //
    NSMutableString *const report =
        [NSMutableString stringWithString:[descriptionText string]];
    [report appendString:@"\n"];
    if ( logContents )
        [report appendString:logContents];

    [request setHTTPBody:[report dataUsingEncoding:NSUTF8StringEncoding]];

    //
    // Try to send the report.
    //
    while ( true ) {
        [self startTask:NSLocalizedString(@"Sending report",nil)];

        NSURLResponse *response;
        NSError *error;
        [NSURLConnection
            sendSynchronousRequest:request
            returningResponse:&response error:&error
        ];

        [self stopTask];
        if ( [error code] ) {
            int const button =
                [self showAlert:@"Unable to send"
                    info:[error localizedDescription] localizeInfo:YES
                    button1:@"Retry" button2:@"Quit"
                ];
            if ( button == NSAlertFirstButtonReturn )
                continue;
        }
        [self quit];
    }
}

/**
 * Set the status message.
 */
- (void) setStatusMessage:
    (NSString*)message
{
    [statusText setStringValue:message];
    [statusText displayIfNeeded];       // force redraw now
}

/**
 * Show an alert.
 */
- (int) showAlert:
    (NSString*)msg
    info:(NSString*)info
    localizeInfo:(BOOL)localizeInfo
    button1:(NSString*)button1
    button2:(NSString*)button2
{
    LC_ModalAlertSheet *const alert =
        [[[LC_ModalAlertSheet alloc] init] autorelease];
    [alert setAlertStyle:NSCriticalAlertStyle];
    [alert setMessageText:NSLocalizedString(msg,nil)];
    if ( info ) {
        if ( localizeInfo )
            info = NSLocalizedString(info,nil);
        [alert setInformativeText:info];
    }
    [alert addButtonWithTitle:NSLocalizedString(button1,nil)];
    if ( button2 )
        [alert addButtonWithTitle:NSLocalizedString(button2,nil)];
    return [alert showAttachedToWindow:mainWindow];
}

/**
 * Show the crash report details sheet.
 */
- (IBAction) showDetails:
    (id)sender
{
    //
    // Stuff the crash log into the details text box.
    //
    NSAttributedString *const attCrashLogString =
        [[[NSAttributedString alloc] initWithString:logContents] autorelease];
    [[detailsText textStorage] setAttributedString:attCrashLogString];

    [NSApp
        beginSheet:detailsPanel
        modalForWindow:mainWindow
        modalDelegate:nil
        didEndSelector:nil
        contextInfo:nil
    ];
}

#ifdef  FORBID_OLD_VERSIONS
/**
 * Tell the user that s/he is running an older version of the crashed
 * application and therefore crash reporting has been disabled and s/he should
 * upgrade.
 */
- (void) showOldVersionAlertAndQuit
{
    int const button =
        [self showAlert:@"Crash reporting disabled"
            info:
                [NSString
                    stringWithFormat:NSLocalizedString(@"You were running",nil),
                    crashedAppName]
            localizeInfo:NO
            button1:@"Upgrade" button2:@"Quit"
        ];
    if ( [alert showAttachedToWindow:mainWindow] == NSAlertFirstButtonReturn )
        [[NSWorkspace sharedWorkspace]
            openURL:[NSURL URLWithString:@APP_DOWNLOAD_URL]];
    [self quit];
}
#endif  /* FORBID_OLD_VERSIONS */

/**
 * Set the status message and start the progress indicator.
 */    
- (void) startTask:
    (NSString*)message;
{
    [progressIndicator startAnimation:nil];
    [self setStatusMessage:message];
}

/**
 * Clear the status message and stop the progress indicator.
 */    
- (void) stopTask
{
    [self setStatusMessage:@""];
    [progressIndicator stopAnimation:nil];
}

/**
 * This gets called repeatedly until a crash log shows up.
 */
- (void) waitForCrashLog:
    (NSTimer*)timer
{
    static int logAttempts = CrashLogWaitAttempts;
    static NSString *crashLog;
    if ( !crashLog ) {
        crashLog =
            [[[[@"~/Library/Logs/CrashReporter"
                stringByAppendingPathComponent:crashedAppName]
                stringByAppendingString:@".crash.log"]
                stringByExpandingTildeInPath] retain];
    }
    if ( !logAttempts-- || [self readCrashLog:crashLog] ) {
        [crashLog release];
        [timer invalidate];
        [self interactWithUser];
    }
}

@end
/* vim:set et sw=4 ts=4: */
