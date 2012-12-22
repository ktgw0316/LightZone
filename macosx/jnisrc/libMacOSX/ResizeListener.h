/* Copyright (C) 2005-2011 Fabio Riccardi */

#import <Cocoa/Cocoa.h>
#import "TextFieldListener.h"

/**
 * An ResizeListener is-a TextFieldListener that updates a text field
 * when told about a new string value.
 *
 * For the Save and Export dialogs, there are two "Resize" text fields: one for
 * width (or X) and another for height (or Y).  When the user changes the value
 * in one, the value of the other needs to be recomputed and set so that the
 * image proportions remain constant.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */
@interface ResizeListener : TextFieldListener
{
    /**
     * The NSTextField that is to be updated when this listener is told about a
     * new string value.
     */
    NSTextField *m_field;

    /**
     * The integer value of the new string value is multiplied by this number
     * to compute the new value for the text field.
     */
    double m_aspectRatio;
}

/**
 * Allocate an ResizeListener.
 */
+ allocForField:
    (NSTextField*)field
    aspectRatio:(double)aspectRatio;

/**
 * Set the integer value of the text field.
 */
- (void) tell:
    (NSString*)textValue;

@end
/* vim:set et sw=4 ts=4: */
