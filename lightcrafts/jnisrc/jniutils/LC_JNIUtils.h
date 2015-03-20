/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * Light Crafts' JNI Utilities.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef LC_JNIUtils_H
#define LC_JNIUtils_H

// standard
#include <jni.h>
#include <stdio.h>

#ifdef  name2
#undef  name2
#endif
#define name2(a,b) a##b

#ifdef  name3
#undef  name3
#endif
#define name3(a,b,c) a##b##c

#ifdef  name4
#undef  name4
#endif
#define name4(a,b,c,d) a##b##c##d

#if defined( __CYGWIN__ ) || defined( __MINGW32__ ) || \
    defined( WIN32 ) || defined( _WIN32 ) || defined( __WIN32 )
    //
    // Define our own shorthand to know when we're compiling for Windows.
    //
#   define __WINDOWS__
#endif

#ifdef  __cplusplus
extern "C" {
#endif

/**
 * The running JVM.
 */
#ifndef __WINDOWS__
    extern JavaVM *g_jvm;
#else
#   ifndef LC_JNIUtils_IMPLEMENTATION
        JNIIMPORT JavaVM *g_jvm;
#   endif
#endif

/**
 * Attaches to the current JVM thread.
 */
JNIEXPORT JNIEnv* JNICALL LC_attachCurrentThread();

/** 
 * Checks to see if Java threw an exception: if so, report it, then clear it.
 */     
JNIEXPORT int JNICALL LC_exceptionOccurred( JNIEnv* );

/**
 * Given a class name, finds the actual Java class for it.
 * The className must use '/' for package separators, not '.'.
 */
JNIEXPORT jclass JNICALL LC_findClassOrDie( JNIEnv*, char const *className,
                                            int newGlobalRef
#ifdef __cplusplus
                                                             = 0
#endif
);

/**
 * Opens a file.  This function correctly handles path names containing Unicode
 * (UTF-8) on all platforms.
 */
JNIEXPORT FILE* JNICALL LC_fopen( char const *path, char const *mode );

/**
 * Gets the JNI env for the current thread.
 */
JNIEXPORT JNIEnv* JNICALL LC_getJNIEnv( int *mustDetach );

/**
 * Gets the pointer value previously stored inside the Java object.
 */
JNIEXPORT void* JNICALL LC_getNativePtr( JNIEnv*, jobject );

/**
 * Given a class, method name, and method signature, gets the jmethodID.
 */
JNIEXPORT jmethodID JNICALL LC_getStaticMethodID( JNIEnv*, jclass,
                                                  char const *methodName,
                                                  char const *methodSignature );

/**
 * Returns non-zero only if the CPU has SSE2 support.
 */
#ifdef __APPLE__
#ifndef __cplusplus
static // inline functions are not implicitly static in C
#endif
       inline int LC_hasSSE2() { return 1; }
#else
JNIEXPORT int JNICALL LC_hasSSE2();
#endif

/**
 * Sets a float field of a Java class to a given value.
 */
JNIEXPORT void JNICALL LC_setFloatField( JNIEnv*, jobject,
                                         char const *fieldName, float value );

/**
 * Sets an int field of a Java class to a given value.
 */
JNIEXPORT void JNICALL LC_setIntField( JNIEnv*, jobject, char const *fieldName,
                                       int value );

/**
 * Sets the pointer inside the Java object.
 */
JNIEXPORT void JNICALL LC_setNativePtr( JNIEnv*, jobject, void* );

/**
 * Sets a static int field of a Java class to a given value.
 */
JNIEXPORT void JNICALL LC_setStaticIntField( JNIEnv*, jclass,
                                             char const *fieldName, int value );

/**
 * Throws a FileNotFoundException back on the Java side.
 */
JNIEXPORT void JNICALL LC_throwFileNotFoundException( JNIEnv*,
                                                      char const *msg );

/**
 * Throws an IllegalArgumentException back on the Java side.
 */
JNIEXPORT void JNICALL LC_throwIllegalArgumentException( JNIEnv*,
                                                         char const *msg );

/**
 * Throws an IllegalStateException back on the Java side.
 */
JNIEXPORT void JNICALL LC_throwIllegalStateException( JNIEnv*,
                                                      char const *msg );

/**
 * Throws an IOException back on the Java side with the given message.
 */
JNIEXPORT void JNICALL LC_throwIOException( JNIEnv*, char const *msg );

/**
 * Throws an IOException back on the Java side based on errno.
 */
JNIEXPORT void JNICALL LC_throwIOExceptionForErrno( JNIEnv* );

/**
 * Throws an IOException back on the Java side for the given error code.
 */
JNIEXPORT void JNICALL LC_throwIOExceptionForErrorCode( JNIEnv*,
                                                        int errorCode );

/**
 * Throws an OutOfMemoryError back on the Java side.
 */
JNIEXPORT void JNICALL LC_throwOutOfMemoryError( JNIEnv*, char const *msg );

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
        ~auto_JNIEnv();
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
     * Gets an jobject's jclass and ensure the local reference thereto is
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

    /**
     * Convert a jarray to a C array.
     */
    template<typename T> class jarray_to_c {
    public:
        typedef T value_type;
        typedef T* pointer;

        jarray_to_c( JNIEnv *env, jarray jArray ) :
            m_env( env ), m_jArray( jArray ),
            m_cArray( env->GetPrimitiveArrayCritical( jArray, 0 ) )
        {
        }

        void release() {
            if ( m_cArray ) {
                m_env->ReleasePrimitiveArrayCritical( m_jArray, m_cArray, 0 );
                m_cArray = 0;
            }
        }

        ~jarray_to_c() {
            release();
        }

        jsize length() const {
            return m_env->GetArrayLength( m_jArray );
        }

        operator pointer() const {
            return static_cast<pointer>( m_cArray );
        }

        value_type& operator[]( int i ) {
            return (static_cast<pointer>( m_cArray ))[i];
        }

        value_type operator[]( int i ) const {
            return (static_cast<pointer>( m_cArray ))[i];
        }

        value_type operator*() const {
            return operator[]( 0 );
        }

        friend pointer operator+( jarray_to_c const &a, int i ) {
            return static_cast<pointer>( a ) + i;
        }
        friend pointer operator+( int i, jarray_to_c const &a ) {
            return a + i;
        }
    private:
        JNIEnv *const m_env;
        jarray m_jArray;
        void *m_cArray;
    };

    /**
     * Convert a jobjectArray to a C array.
     */
    template<typename T> class jobjectArray_to_c {
    public:
        typedef T value_type;

        class assigner {
        public:
            assigner& operator=( value_type value ) {
                m_ref.m_env->SetObjectArrayElement(
                    m_ref.m_jArray, m_index, value
                );
                return *this;
            }
            operator value_type() const {
                return static_cast<value_type>(
                    m_ref.m_env->GetObjectArrayElement(
                        m_ref.m_jArray, m_index
                    )
                );
            }
        private:
            assigner( jobjectArray_to_c<value_type> &ref, int index ) :
                m_ref( ref ), m_index( index )
            {
            }
            assigner( assigner const& );    // forbid

            jobjectArray_to_c<value_type> &m_ref;
            int const m_index;

            friend class jobjectArray_to_c;
        };
        friend class assigner;

        jobjectArray_to_c( JNIEnv *env, jobjectArray jArray ) :
            m_env( env ), m_jArray( jArray )
        {
        }

        void release() {
            // do nothing
        }

        jsize length() const {
            return m_env->GetArrayLength( m_jArray );
        }

        assigner operator[]( int i ) {
            return assigner( *this, i );
        }

        value_type operator[]( int i ) const {
            return static_cast<value_type>(
                m_env->GetObjectArrayElement( m_jArray, i )
            );
        }

        value_type operator*() const {
            return operator[]( 0 );
        }

    private:
        JNIEnv *const m_env;
        jobjectArray m_jArray;
    };

    /**
     * Convert a jstring to a C string.
     */
    class jstring_to_c {
    public:
        jstring_to_c( JNIEnv *env , jstring js ) :
            m_env( env ), m_jString( js ),
            m_cString( env->GetStringUTFChars( js, NULL ) )
        {
        }

        void release() {
            if ( m_cString ) {
                m_env->ReleaseStringUTFChars( m_jString, m_cString );
                m_cString = 0;
            }
        }

        ~jstring_to_c() {
            release();
        }

        operator char const*() const {
            return m_cString;
        }

        char operator[]( int i ) const {
            return m_cString[i];
        }

        char operator*() const {
            return operator[]( 0 );
        }

        friend char const* operator+( jstring_to_c const &s, int i ) {
            return s.m_cString + i;
        }
        friend char const* operator+( int i, jstring_to_c const &s ) {
            return s + i;
        }
    private:
        JNIEnv *const m_env;
        jstring const m_jString;
        char const *m_cString;
    };

    /**
     * Convert a jbyteArray to a C string.
     */
    class jbyteArray_to_c {
    public:
        jbyteArray_to_c( JNIEnv *env , jbyteArray jba ) :
            m_env( env ), m_jbyteArray( jba ),
            m_cString( (char*)env->GetByteArrayElements( jba, NULL ) )
        {
        }

        void release() {
            if ( m_cString ) {
                m_env->ReleaseByteArrayElements( m_jbyteArray, (jbyte *) m_cString, 0 );
                m_cString = 0;
            }
        }

        ~jbyteArray_to_c() {
            release();
        }

        operator char const*() const {
            return m_cString;
        }

        char operator[]( int i ) const {
            return m_cString[i];
        }

        char operator*() const {
            return operator[]( 0 );
        }

        friend char const* operator+( jbyteArray_to_c const &s, int i ) {
            return s.m_cString + i;
        }
        friend char const* operator+( int i, jbyteArray_to_c const &s ) {
            return s + i;
        }
    private:
        JNIEnv *const m_env;
        jbyteArray const m_jbyteArray;
        char const *m_cString;
    };

} // namespace

#endif  /* __cplusplus */

#endif  /* LC_JNIUtils_H */
/* vim:set et sw=4 ts=4: */
