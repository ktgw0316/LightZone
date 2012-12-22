/* Copyright (C) 2005-2011 Fabio Riccardi */

/* ProgressController */

#import <Cocoa/Cocoa.h>

/**
 * A ProgressController is the "controller" for the entire progress panel
 * dialog.
 */
@interface ProgressController : NSObject
{
@public
    IBOutlet NSButton *cancelButton;
    IBOutlet NSTextField *message;
    IBOutlet NSPanel *panel;
    IBOutlet NSProgressIndicator *progressIndicator;
}

- (IBAction) cancel:
    (id)sender;

@end
/* vim:set et sw=4 ts=4: */
