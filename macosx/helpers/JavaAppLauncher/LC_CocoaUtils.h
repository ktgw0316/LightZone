/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_CocoaUtils_H
#define LC_CocoaUtils_H

// standard
#include <Cocoa/Cocoa.h>
#include <jni.h>

// local
#include "LC_CPPUtils.h"                /* for auto_obj */

namespace LightCrafts {

    /**
     * An auto_obj<NSAutoreleasePool> is a specialization of auto_obj<T> for
     * NSAutoreleasePool that guarantees that an instance will be released
     * when it goes out of scope.
     */
    template<> class auto_obj<NSAutoreleasePool> {
    public:
        auto_obj() : m_pool( [[NSAutoreleasePool alloc] init] ) {
        }

        void release() {
            [m_pool release];
            m_pool = nil;
        }

        ~auto_obj() {
            release();
        }
    private:
        NSAutoreleasePool *m_pool;
    };

    /**
     * An auto_CFRef<T> guarantees that a Core Foundation Reference will be
     * released when it goes out of scope.
     */
    template<typename T> class auto_CFRef {
    public:
        typedef T value_type;

        auto_CFRef( void *p ) : m_ref( (T)p ) { }

        auto_CFRef( void const *p ) : m_ref( (T)p ) { }

        void release() {
            if ( m_ref ) {
                ::CFRelease( m_ref );
                m_ref = 0;
            }
        }

        ~auto_CFRef() {
            release();
        }

        operator value_type() const {
            return m_ref;
        }

        bool operator!() const {
            return !m_ref;
        }
    private:
        T m_ref;
    };

}

/**
 * Given a Java AWT component, return its NSWindow*.
 */
NSWindow* LC_getNSWindowFromAWTComponent( JNIEnv*, jobject awtComponent );

/**
 * Convert an NSString* to a jstring.
 */
inline jstring LC_NSStringTojstring( JNIEnv *env, NSString const *s ) {
    return s ? env->NewStringUTF( [s UTF8String] ) : NULL;
}

#endif  /* LC_CocoaUtils_H */
/* vim:set et sw=4 ts=4: */
