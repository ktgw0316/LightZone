/* Copyright (C) 2005-2011 Fabio Riccardi */

#import <Cocoa/Cocoa.h>
#import "TextFieldListener.h"

/**
 * A KeyStrokeNumberFormatter is-a NSNumberFormatter that traps every keystroke
 * and tells a TextFieldListener about the current value of the NSTextField
 * this KeyStrokeNumberFormatter is a NSNumberFormatter for.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */
@interface KeyStrokeNumberFormatter : NSNumberFormatter
{
    TextFieldListener *m_listener;
}

/**
 * Initialize a KeyStrokeNumberFormatter.
 */
- (id) init;

/**
 * Override this NSNumberFormatter method to trap every keystroke.
 */
- (BOOL) isPartialStringValid:
    (NSString**)partialStringPtr
    proposedSelectedRange:(NSRangePointer)proposedSelRangePtr
    originalString:(NSString*)origString
    originalSelectedRange:(NSRange)origSelRange
    errorDescription:(NSString**)error;

/**
 * Deallocate a KeyStrokeNumberFormatter.
 */
- (void) dealloc;

/**
 * Set the TextFieldListener.
 */
- (void) setListener:
    (TextFieldListener*)listener;

@end
/* vim:set et sw=4 ts=4: */
