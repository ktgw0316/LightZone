/* Copyright (C) 2005-2011 Fabio Riccardi */

#import <Cocoa/Cocoa.h>

/**
 * An LC_ModalAlertSheet is-an NSAlert that displays an alert as a sheet, but
 * acts like a modal dialog, i.e., a call to showAttachedToWindow() blocks
 * until the user clicks a button.
 */
@interface LC_ModalAlertSheet : NSAlert {
}

/**
 * Show an alert attached to the given window and block until the user clicks
 * a button.
 * Returns the button clicked.
 */
- (int) showAttachedToWindow:
    (NSWindow*)window;

@end
/* vim:set et sw=4 ts=4: */
