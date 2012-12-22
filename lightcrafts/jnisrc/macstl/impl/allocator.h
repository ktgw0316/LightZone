/*
 *  allocator.h
 *  macstl
 *
 *  Created by Glen Low on Dec 27 2002.
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

#ifndef MACSTL_IMPL_ALLOCATOR_H
#define MACSTL_IMPL_ALLOCATOR_H

#include <memory>

namespace macstl
	{
		namespace impl
			{
				/// Allocator base.
				
				/// @internal
				/// Defines the common members of memory-based allocators. This eases the definition of allocator classes.
				///
				/// @param	T	The allocated type.
				
				template <typename T> class allocator_base
					{
						public:
							/// Represents size of allocation.
							typedef std::size_t		size_type;
							
							/// Represents difference between pointers.
							typedef std::ptrdiff_t	difference_type;

							/// Pointer to access value.
							typedef const T*		const_pointer;
							
							/// Pointer to access and change value.
							typedef T*				pointer;
							
							/// Reference to access value.
							typedef const T&		const_reference;
							
							/// Reference to access and change value.
							typedef T&				reference;
							
							/// The allocated type.
							typedef T				value_type;
					
							/// Gets the memory address for @a x.
							static pointer address (reference x)				{ return &x; }

							/// Gets the memory address for @a x.
							static const_pointer address (const_reference x)	{ return &x; }
					
							/// Gets the largest value that can be allocated.
							static size_type max_size () throw()
								{
									return size_type (-1) / sizeof (value_type);
								}
					
							/// Constructs a copy of @a val at address @a ptr.
							static void construct (pointer ptr, const T& val)	{ new (ptr) T (val); }

							/// Destructs the value at address @a ptr.
							static void destroy (pointer ptr)					{ ptr->~T(); }
					};
					
				template <> class allocator_base <void>
					{
						public:
							typedef void		value_type;
							typedef void*		pointer;
							typedef const void*	const_pointer;
					};
			}
	}

#endif
