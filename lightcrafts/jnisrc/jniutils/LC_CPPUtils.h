/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * Light Crafts' C++ Utilities.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef LC_CPPUtils_H
#define LC_CPPUtils_H

// standard
#include <cstring>
#ifdef DEBUG
#include <iostream>
#endif

namespace LightCrafts {

    /**
     * Template declaration allowing specialization of auto_obj<T>.
     */
    template<typename T> class auto_obj;

    /**
     * An auto_vec<T> is like the standard auto_ptr<T> but works where T is a
     * pointer to a T[] and so delete[] must be used.
     */
    template<typename T> class auto_vec {
    public:
        explicit auto_vec( T *p = 0 ) : m_p( p ) { }
        ~auto_vec()                             { delete[] m_p; }

        auto_vec<T>& operator=( T *p ) {
            if ( p != m_p ) {
                delete[] m_p;
                m_p = p;
            }
            return *this;
        }

        T&          operator*()                 { return *m_p; }
        T const&    operator*() const           { return *m_p; }

        T&          operator[]( int i )         { return m_p[i]; }
        T const&    operator[]( int i ) const   { return m_p[i]; }

                    operator T*()               { return m_p; }
                    operator T const*() const   { return m_p; }
    private:
        T *m_p;

        auto_vec( auto_vec<T> const& );         // forbid copy
        auto_vec& operator=( auto_vec< T >& );  // forbid assignment
    };

    /**
     * Does the same thing as strdup(3), but use C++'s "new" operator to
     * allocate storage.
     */
    inline char* new_strdup( char const *s ) {
        return std::strcpy( new char[ std::strlen( s ) + 1 ], s );
    }

    /**
     * A tracer is a utility class to trace program execution for debugging
     * purposes.
     */
    class tracer {
    public:
        tracer( char const *s )
#ifdef DEBUG
            : m_s( s )
#endif
        {
#ifdef DEBUG
            std::cerr << "[tracer] enter " << s << std::endl;
#endif
        }
#ifdef DEBUG
        ~tracer() {
            std::cerr << "[tracer] exit " << m_s << std::endl;
        }
    private:
        char const *const m_s;
#endif
    };

#ifdef DEBUG
#  define LC_TRACER(s) const LightCrafts::tracer t##__LINE__(s)
#else
#  define LC_TRACER(s) /* nothing */
#endif
} // namespace

#endif  /* LC_CPPUtils_H */
/* vim:set et sw=4 ts=4: */
