/*
 *  valarray_mmaparray.h
 *  macstl
 *
 *  Created by Glen Low on Jan 8 2005.
 *
 *  Copyright (c) 2005 Pixelglow Software, all rights reserved.
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

#ifndef MACSTL_IMPL_VALARRAY_MMAPARRAY_H
#define MACSTL_IMPL_VALARRAY_MMAPARRAY_H

#ifdef HAVE_MMAP	// to include Windows implementation...

namespace stdext
	{
		namespace impl
			{
				/// Allocating base for mmaparray.
				 
				/// @internal
				/// This class allocates but does not initialize memory so that its subclass mmaparray will not
				/// leak memory if an exception is thrown during construction. Partial specializations may
				/// allocate memory differently e.g. with a different alignment or type, to fulfill chunking
				/// requirements.
				///
				/// @param	T		The element type.
				
				template <typename T> class mmaparray_base
					{
						protected:
							mmaparray_base (const macstl::channel& chan, macstl::mmap_mode::mode mode, std::size_t n, std::size_t pos):
								mmap_ (chan, mode, n, pos)
								{
								}

						private:
							macstl::mmapping <typename array_term <T>::value_data> mmap_;
					};
				
			}
			
		/// Memory-mapped numeric (n-at-a-time) array.
		
		/// This class is similar to stdext::valarray but is based on memory-mapped files. This makes the class useful
		/// to avoid special coding for I/O and for sharing between processes.
		///
		/// @param	T	The value type, which shall satisfy the following requirements:
		///				- not an abstract class, reference type or cv-qualified type
		///				- if a class, must publicly define: default constructor, copy constructor, destructor and assignment operator
		///				- Default construct + assign == copy construct
		///				- Destruct + copy construct == assign
		///				- Independent of the address space of the object.
		///				.
		///				For example, built-in arithmetic types like char, short, int, float, double.
		///
		/// @note	Since the values are simply mapped in from the file, neither value constructor nor value destructor are called. Also you cannot
		///			copy this array, since it represents an external resource.
		///
		/// @header	#include <macstl/valarray.h>
		///
		/// @see	macstl::mmapping

		template <typename T> class mmaparray: private impl::mmaparray_base <T>, public impl::array_term <T>
			{
				public:
					typedef typename impl::array_term <T>::value_type value_type;
					
					/// Constructs an array, memory-mapped from channel chan.
					mmaparray (const macstl::channel& chan, macstl::mmap_mode::mode mode = macstl::mmap_mode::rdonly, std::size_t n = 0, std::size_t pos = 0):
						impl::mmaparray_base <T> (chan, mode, n, pos), impl::array_term <T> (mmap_.begin (), n)
						{
						}

					/// Assigns the other array.
					mmaparray& operator= (const mmaparray& other)
						{
							if (this != &other)
								impl::copy_array (*this, other);
							return *this;
						}
					
					/// Assigns the other term.
					template <typename Expr>
						typename impl::enable_if <impl::is_convertible <T1, T>::value, mmaparray&>::type operator= (const impl::term <T1, Expr>& other)
						{
							impl::copy_array (*this, other.that ());
							return *this;
						}					

					/// Assigns x to each element.
					mmaparray& operator= (const T& x)
						{
							impl::fill_array (*this, x);
							return *this;
						}
	
				private:
					mmaparray (const mmaparray& other);	// disallow copying
			};
	}
	
#endif
#endif
