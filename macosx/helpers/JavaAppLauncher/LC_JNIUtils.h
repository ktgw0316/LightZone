/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * LC_JNIUtils.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef LC_JNIUtils_H
#define LC_JNIUtils_H

// standard
#include <jni.h>                        /* for JNIEnv */

#ifdef  __cplusplus
extern "C" {
#endif

/**
 * Attach to the current JVM thread.
 */
JNIEnv* LC_attachCurrentThread();

/**
 * Check to see if Java threw an exception: if so, report it, then clear it.
 */
bool LC_exceptionOccurred( JNIEnv* );


/**
 * Gets the JNI env for the current thread.
 */
JNIEnv* LC_getJNIEnv( int *mustDetach );

extern JavaVM *g_jvm;

#ifdef  __cplusplus
}

// local
#include <LC_CPPUtils.h>

namespace LightCrafts {

    /**
     * Ensure we detach from the current JVM thread.
     */
    class auto_JNIEnv {
    public:
        auto_JNIEnv() : m_env( LC_getJNIEnv( &m_mustDetach ) ) { }
        ~auto_JNIEnv() {
            if ( m_mustDetach )
                g_jvm->DetachCurrentThread();
        }
        operator JNIEnv*() const {
            return m_env;
        }
        JNIEnv* operator->() const {
            return m_env;
        }
    private:
        JNIEnv *const m_env;
        int m_mustDetach;
    };

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
     * Get an jobject's jclass and ensure the local reference thereto is
     * destroyed.
     */
    class auto_jclass : public auto_local_ref<jclass> {
    public:
        typedef auto_local_ref<jclass> base_type;

        auto_jclass( JNIEnv *env, jobject jObject ) :
            base_type( env, env->GetObjectClass( jObject ) )
        {
        }
    };

    /**
     * Ensure a jstring is destroyed.
     */
    typedef auto_local_ref<jstring> auto_jstring;

} // namespace

#endif  /* __cplusplus */

#endif  /* LC_JNIUtils_H */
/* vim:set et sw=4 ts=4: */
