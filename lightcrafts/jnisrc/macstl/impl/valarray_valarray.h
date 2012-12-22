/*
 *  valarray_valarray.h
 *  macstl
 *
 *  Created by Glen Low on Jun 22 2003.
 *
 *  Copyright (c) 2003-2005 Pixelglow Software, all rights reserved.
 *  http://www.pixelglow.com/macstl/
 *  macstl@pixelglow.com
 *
 *  Unless explicitly acquired and licensed from Licensor under the Pixelglow
 *  Software License ("PSL") Version 2.0 or greater, the contents of this file
 *  are subject to the Reciprocal Public License ("RPL") Version 1.1, or
 *  subsequent versions as allowed by the RPL, and You may not copy or use this
 *  file in either source code or executable form, except in compliance with the
 *  terms and conditions of the RPL.
 *
 *  While it is an open-source license, the RPL prohibits you from keeping your
 *  derivations of this file proprietary even if you only deploy them in-house.
 *  You may obtain a copy of both the PSL and the RPL ("the Licenses") from
 *  Pixelglow Software ("the Licensor") at http://www.pixelglow.com/.
 *
 *  Software distributed under the Licenses is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the Licenses
 *  for the specific language governing rights and limitations under the
 *  Licenses. Notwithstanding anything else in the Licenses, if any clause of
 *  the Licenses which purports to disclaim or limit the Licensor's liability
 *  for breach of any condition or warranty (whether express or implied by law)
 *  would otherwise be void, that clause is deemed to be subject to the
 *  reservation of liability of the Licensor to supply the software again or to
 *  repair the software or to pay the cost of having the software supplied again
 *  or repaired, at the Licensor's option. 
 */

#ifndef MACSTL_IMPL_VALARRAY_VALARRAY_H
#define MACSTL_IMPL_VALARRAY_VALARRAY_H

#if defined(__linux__) || defined(_WIN32) || defined(_WIN64)
#include <malloc.h>		// for Linux, Windows memalign
#endif

namespace stdext
	{
		namespace impl
			{
				 /// Allocating base for valarray
				 
				 /// @internal
				 /// This class allocates but does not initialize memory so that its subclass valarray will not
				 /// leak memory if an exception is thrown during construction. Partial specializations may
				 /// allocate memory differently e.g. with a different alignment or type, to fulfill chunking
				 /// requirements.
				 ///
				 /// @param	T		The element type.
				 /// @param	Enable	If void, enables a particular specialization
				 
				template <typename T, typename Enable> class valarray_base: public array_term <T>
					{
						protected:
							typedef array_term <T> base;
							
							valarray_base (std::size_t n): base (reinterpret_cast <typename array_term <T>::value_data*> (std::malloc (sizeof (T) * n)), n)
								{
								}

							~valarray_base ()
								{
									std::free (base::values_);
								}
					};

				template <int alignment> void* allocate_aligned (std::size_t size);
				template <int alignment> void deallocate_aligned (void* ptr);
				
			#if defined(__MACH__)
				// on Mach-based systems i.e. Mac OS X, malloc always returns 16-byte aligned memory, but we don't guarantee arbitrary alignment, yet...
				template <> inline void* allocate_aligned <16> (std::size_t size)	{ return std::malloc (size); }
				template <> inline void deallocate_aligned <16> (void* ptr)			{ std::free (ptr); }
			#elif defined(_WIN32) || defined (_WIN64) || defined (__CYGWIN__)
				// on Windows, we can get arbitrary aligned memory...
				template <int alignment> void* allocate_aligned (std::size_t size)	{ return ::_mm_malloc (size, alignment); }
				template <int alignment> void deallocate_aligned (void* ptr)		{ ::_mm_free (ptr); }
			#elif defined(__linux__)
				// on Linux-based systems, we also can get arbitrary aligned memory...
				template <int alignment> void* allocate_aligned (std::size_t size)	{ return ::memalign (alignment, size); }
				template <int alignment> void deallocate_aligned (void* ptr)		{ ::free (ptr); }
			#endif
								
				template <typename T> class valarray_base <T, typename enable_if <exists <typename array_term <T>::chunk_type>::value>::type>:
					public array_term <T>
					{
						public:
							typedef array_term <T> base;
							
							typedef typename array_term <T>::chunk_type chunk_type;
							typedef typename chunk_type::data_type data_type;
														
							/** Constructs with space for @a n elements. */
							valarray_base (std::size_t n): base (
								reinterpret_cast <typename array_term <T>::value_data*> (allocate_aligned <sizeof (chunk_type)> (sizeof (chunk_type) * ((n + chunk_type::length - 1) / chunk_type::length))), n)
								{
								}

							/** Destructs entire array. */
							~valarray_base ()
								{
									deallocate_aligned <sizeof (chunk_type)> (base::values_);
								}
					};				
			}
		 
		/// Numeric (n-at-a-time) array.
		
		/// A valarray is a replacement for a C array that allows n-at-a-time operation with various arithmetic, logical and transcendental functions.
		/// This implementation is largely a superset of std::valarray functionality, and highly optimized to use SIMD opcodes on supporting CPUs.
		///
		/// @param	T	The element value type, which shall satisfy the following requirements:
		///				- not an abstract class, reference type or cv-qualified type
		///				- if a class, must publicly define: default constructor, copy constructor, destructor and assignment operator
		///				- Default construct + assign == copy construct
		///				- Destruct + copy construct == assign
		///				.
		///				For example, built-in arithmetic types like char, short, int, float, double.
		///
		/// @header	#include <macstl/valarray.h>
		///
		/// @see	stdext::statarray, stdext::mmaparray
		
		template <typename T> class valarray: public impl::valarray_base <T>
			{
				public:
					/// @name Constructors and Destructors
					
					//@{
					
					/// Constructs a zero-size array.
					valarray (): impl::valarray_base <T> (0)
						{
						}
						
					/// Constructs an array of n elements, each zero.
					explicit valarray (std::size_t n): impl::valarray_base <T> (n)
						{
							impl::uninitialized_fill_array (*this, T ());
						}
						
					/// Constructs an array of n elements, each a copy of x.
					valarray (const T& x, std::size_t n): impl::valarray_base <T> (n)
						{
							impl::uninitialized_fill_array (*this, x);
						}
		
					/// Constructs an array with copies of the first n elements from x.
					valarray (const T* x, std::size_t n): impl::valarray_base <T> (n)
						{
							impl::uninitialized_copy_array_ptr (*this, x);
						}
		
					/// Constructs a copy of other array.
					valarray (const valarray& other): impl::valarray_base <T> (other.size ())
						{
							impl::uninitialized_copy_array (*this, other);
						}
		
					/// Constructs a copy of other term.
					template <typename T1, typename Term> valarray (const impl::term <T1, Term>& other, typename impl::enable_if <impl::is_convertible <T1, T>::value>::type* = NULL):
						impl::valarray_base <T> (other.that ().size ())
						{
							impl::uninitialized_copy_array (*this, other.that ());
						}
								
					/// Destructs the array.
					~valarray ()
						{
							impl::destroy_array (*this);
						}
					
					//@}
					
					/// @name Assignments
					
					//@{
					
					/// Assigns the other array.
					valarray& operator= (const valarray& other)
						{
							if (this != &other)
								impl::copy_array (*this, other);
							return *this;
						}
					
					/// Assigns the other term.
					template <typename T1, typename Term>
						typename impl::enable_if <impl::is_convertible <T1, T>::value, valarray&>::type operator= (const impl::term <T1, Term>& other)
						{
							impl::copy_array (*this, other.that ());
							return *this;
						}					

					/// Assigns x to each element.
					valarray& operator= (const T& x)
						{
							impl::fill_array (*this, x);
							return *this;
						}
	
					//@}
					
					/// Resizer
					
					//@{
					
					/// Changes the size to n, copying x to each element.
					void resize (std::size_t n, const T& x = T ())
						{
							valarray resized (x, n);
							swap (resized);
						}
						
					//@}
			};
	}
	
#endif
