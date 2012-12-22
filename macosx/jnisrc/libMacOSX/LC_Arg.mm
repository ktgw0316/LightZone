// local
#import "LC_Arg.h"

@implementation LC_Arg

/**
 * Allocate an LC_Arg with a bool value.
 */
+ (LC_Arg*) allocBool:
    (BOOL)arg
{
    LC_Arg *const that = [LC_Arg alloc];
    that->b = arg;
    return that;
}

/**
 * Allocate an LC_Arg with an integer value.
 */
+ (LC_Arg*) allocInt:
    (int)arg
{
    LC_Arg *const that = [LC_Arg alloc];
    that->i = arg;
    return that;
}

@end
/* vim:set et sw=4 ts=4: */
