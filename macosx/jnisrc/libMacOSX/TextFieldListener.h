/* Copyright (C) 2005-2011 Fabio Riccardi */

#import <Cocoa/Cocoa.h>

/**
 * A TextFieldListener is used to receive notifications that some string has
 * changed.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */
@interface TextFieldListener : NSObject
{
    // nothing
}

/**
 * Tell this listener the current value of the string.
 */
- (void) tell:
    (NSString*)aString;

@end
/* vim:set et sw=4 ts=4: */
