#import "LC_ModalAlertSheet.h"

@implementation LC_ModalAlertSheet

/**
 * This is the delegate that gets called by Cocoa when the user clicks a button
 * in the sheet.
 */
- (void) alertEnded:
    (NSAlert*)alert
    returnCode:(int)returnCode
    contextInfo:(void*)contextInfo
{
    [[alert window] orderOut:self];     // roll up the sheet
    [NSApp stopModalWithCode:returnCode];
}

/**
 * Show an alert attached to the given window and block until the user clicks
 * a button.
 * Returns the button clicked.
 */
- (int) showAttachedToWindow:
    (NSWindow*)window
{
    [self
        beginSheetModalForWindow:window
        modalDelegate:self
        didEndSelector:@selector(alertEnded:returnCode:contextInfo:)
        contextInfo:nil
    ];
    return [NSApp runModalForWindow:[self window]];
}

@end
/* vim:set et sw=4 ts=4: */
