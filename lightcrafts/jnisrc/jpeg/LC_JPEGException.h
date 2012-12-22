/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_JPEGException_H
#define LC_JPEGException_H

// standard
#include <exception>

/**
 * An LC_JPEGException is-an exception that's used to break out of processing
 * a JPEG image.
 */
class LC_JPEGException : public std::exception {
public:
    char const* what() const throw() {
        return "LC_JPEGException";
    }
};

#endif  /* LC_JPEGException_H */
/* vim:set et sw=4 ts=4: */
