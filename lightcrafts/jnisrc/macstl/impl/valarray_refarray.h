/*
 *  valarray_refarray.h
 *  macstl
 *
 *  Created by Glen Low on Aug 12 2005.
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

#ifndef MACSTL_IMPL_VALARRAY_REFARRAY_H
#define MACSTL_IMPL_VALARRAY_REFARRAY_H

namespace stdext
	{
		/// Referenced numeric (n-at-a-time) array.
		
		/// This class is similar to stdext::valarray but does not copy or keep its own storage. This makes the class useful
		/// for memory already allocated by a different method. You are responsible for correct alignment and disposal of the memory.
		///
		/// @param	T	The value type, which shall satisfy the following requirements:
		///				- not an abstract class, reference type or cv-qualified type
		///				- if a class, must publicly define: default constructor, copy constructor, destructor and assignment operator
		///				- Default construct + assign == copy construct
		///				- Destruct + copy construct == assign
		///				.
		///				For example, built-in arithmetic types like char, short, int, float, double.
		///
		/// @header	#include <macstl/valarray.h>
		
		template <typename T> class refarray: public impl::array_term <T>
			{
				public:
					/// @name Constructors and Destructors
					
					//@{
					
					/// Constructs an array with no reference.
					refarray (): impl::array_term <T> (NULL, 0)
						{
						}
						
					/// Constructs an array referencing @a n elements at @a data.
					refarray (typename impl::array_term <T>::value_data* data, std::size_t n): impl::array_term <T> (data, n)
						{
						}
		
					//@}
					
					/// @name Assignments
					
					//@{
					
					/// Assigns the other array.
					refarray& operator= (const refarray& other)
						{
							if (this != &other)
								impl::copy_array (*this, other);
							return *this;
						}
					
					/// Assigns the other term.
					template <typename T1, typename Term>
						typename impl::enable_if <impl::is_convertible <T1, T>::value, refarray&>::type operator= (const impl::term <T1, Term>& other)
						{
							impl::copy_array (*this, other.that ());
							return *this;
						}					

					/// Assigns x to each element.
					refarray& operator= (const T& x)
						{
							impl::fill_array (*this, x);
							return *this;
						}
						
					//@}
					
					/// Reset
					
					//@{
					
					/// Changes the data and size.
					void reset (typename impl::array_term <T>::value_data* data, std::size_t n)
						{
							refarray resetted (data, n);
							swap (resetted);
						}
						
					//@}
			};
	}

#endif
