/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * LC_JNIUtils.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef LC_JNIUtils_H
#define LC_JNIUtils_H

#include <jni.h>                        /* for JNIEnv */

/**
 * Attach to the current JVM thread.
 */
JNIEnv* LC_attachCurrentThread();

/**
 * Check to see if Java threw an exception: if so, report it, then clear it.
 */
bool LC_exceptionOccurred( JNIEnv* );

/**
 * Given a class name, find the actual Java class for it.
 */
jclass LC_findClassOrDie( JNIEnv*, char const *className );

namespace LightCrafts {

    /**
     * Ensure a Java local reference is destroyed.
     */
    template<typename T> class auto_local_ref {
    public:
        typedef T value_type;

        auto_local_ref( JNIEnv *env, value_type ref ) :
            m_env( env ), m_ref( ref )
        {
        }

        auto_local_ref& operator=( value_type ref ) {
            if ( ref != m_ref ) {
                m_env->DeleteLocalRef( m_ref );
                m_ref = ref;
            }
            return *this;
        }

        void release() {
            m_env->DeleteLocalRef( m_ref );
            m_ref = 0;
        }

        ~auto_local_ref() {
            release();
        }

        operator value_type() const {
            return m_ref;
        }

        bool operator!() const {
            return !m_ref;
        }
    private:
        JNIEnv *const m_env;
        value_type m_ref;
    };

    /**
     * Ensure a jstring is destroyed.
     */
    typedef auto_local_ref<jstring> auto_jstring;

}

#endif  /* LC_JNIUtils_H */
/* vim:set et sw=4 ts=4: */
