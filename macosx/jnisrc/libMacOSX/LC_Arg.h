/* Copyright (C) 2005-2011 Fabio Riccardi */

#import <Cocoa/Cocoa.h>

/**
 * An LC_Arg is used to pass a simple data type via a selector.
 */
@interface LC_Arg : NSObject {
@public
    bool    b;
    int     i;
}

/**
 * Allocate an LC_Arg with a bool value.
 */
+ (LC_Arg*) allocBool:
    (BOOL)arg;

/**
 * Allocate an LC_Arg with an integer value.
 */
+ (LC_Arg*) allocInt:
    (int)arg;

@end
/* vim:set et sw=4 ts=4: */
