// local
#import "KeyStrokeNumberFormatter.h"

@implementation KeyStrokeNumberFormatter

/**
 * Initialize a KeyStrokeNumberFormatter.
 */
- (id) init
{
    self = [super init];
    m_listener = nil;
    return self;
}

/**
 * Override this NSNumberFormatter method to trap every keystroke.
 */
- (BOOL) isPartialStringValid:
    (NSString**)partialStringPtr
    proposedSelectedRange:(NSRangePointer)proposedSelRangePtr
    originalString:(NSString*)origString
    originalSelectedRange:(NSRange)origSelRange
    errorDescription:(NSString**)error
{
    //
    // Call super's method to process as normal.
    //
    BOOL const isValid =
        [super
            isPartialStringValid:partialStringPtr
            proposedSelectedRange:proposedSelRangePtr
            originalString:origString
            originalSelectedRange:origSelRange
            errorDescription:error
        ];
    if ( isValid ) {
        static NSCharacterSet const *const digitSet =
            [[NSCharacterSet
                characterSetWithCharactersInString:@"0123456789"] retain];
        //
        // The NSNumberFormatter apparently only prevents the user from leaving
        // an NSTextField if there is a non-digit character in it.  But we want
        // to prevent the user from entering a non-digit character in the first
        // place, so we have to handle this ourselves.
        //
        NSCharacterSet *const partialStringSet =
            [NSCharacterSet
                characterSetWithCharactersInString:*partialStringPtr];

        if ( ![digitSet isSupersetOfSet:partialStringSet] ) {
            NSBeep();
            return NO;
        }

        if ( m_listener ) {
            //
            // Tell the listener about the current value after the keystroke.
            //
            [m_listener tell:*partialStringPtr];
        }
    }
    return isValid;
}

/**
 * Deallocate a KeyStrokeNumberFormatter.
 */
- (void) dealloc
{
    [m_listener release];
    [super dealloc];
}

/**
 * Set the TextFieldListener.
 */
- (void) setListener:
    (TextFieldListener*)listener
{
    [listener retain];
    [m_listener release];
    m_listener = listener;
}

@end
/* vim:set et sw=4 ts=4: */
