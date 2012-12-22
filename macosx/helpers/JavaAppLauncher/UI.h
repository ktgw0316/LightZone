/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * UI.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef UI_H
#define UI_H

// standard
#import <Cocoa/Cocoa.h>

/**
 * Show an alert dialog box with the given message in it and quit.  Only the
 * msg string is localized first.
 */
void LC_dieWithoutLocalizedInfo( NSString *msg, NSString *info = 0 );

/**
 * Show an alert dialog box with the given message in it and quit.  Both the
 * msg and info strings are localized first.
 */
inline void LC_die( NSString *msg, NSString *info = 0 ) {
    LC_dieWithoutLocalizedInfo( msg, info ? NSLocalizedString(info,nil) : nil );
}

#endif  /* UI_H */
/* vim:set et sw=4 ts=4: */
