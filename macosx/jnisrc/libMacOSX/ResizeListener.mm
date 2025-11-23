// local
#import "ResizeListener.h"

@implementation ResizeListener

/**
 * Allocate an ResizeListener.
 */
+ allocForField:
    (NSTextField*)field
    aspectRatio:(double)aspectRatio
{
    ResizeListener *const that = [ResizeListener alloc];
    that->m_field = field;
    that->m_aspectRatio = aspectRatio;
    return that;
}

/**
 * Set the integer value of the text field.
 */
- (void) tell:
    (NSString*)textValue
{
    [m_field setIntValue:(int)([textValue intValue] * m_aspectRatio)];
}

@end
/* vim:set et sw=4 ts=4: */
