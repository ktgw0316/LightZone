// local
#import "ProgressController.h"

@implementation ProgressController

/**
 * This is called by the Cocoa framework when the user clicks the Cancel
 * button.
 */
- (IBAction) cancel:
    (id)sender
{
    [message setStringValue:@"Canceling..."];
    [message displayIfNeeded];          // force redraw now
    [NSApp endSheet:panel returnCode:NSModalResponseCancel];
}

@end
/* vim:set et sw=4 ts=4: */
