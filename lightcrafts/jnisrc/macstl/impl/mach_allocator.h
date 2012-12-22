/*
 *  mach_allocator.h
 *  macstl
 *
 *  Created by Glen Low on Dec 27 2002.
 *
 *  Copyright (c) 2002-2005 Pixelglow Software, all rights reserved.
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

#ifndef MACSTL_IMPL_MACH_ALLOCATOR_H
#define MACSTL_IMPL_MACH_ALLOCATOR_H

namespace macstl
	{
		template <typename T> class mach_allocator;
		
		template <> class mach_allocator <void>: public impl::allocator_base <void>
			{
				public:
					template <class T2> struct rebind
						{
							typedef mach_allocator <T2> other;
						};
			};

		/// Mach allocator.
		
		/// Puts the Standard C++ Allocator interface on the Mach VM allocator. This is the fundamental allocator
		/// on the OS X system, whose main characteristic is that it allocates pages "lazily" i.e. memory
		/// is not actually allocated until it is accessed. Also allocated memory is always
		/// page-aligned and zero-filled.
		///
		/// This lets you use Mach allocators to allocate memory for STL containers and C++ objects. All allocator
		/// instances are not distinct and compare equal.
		///
		/// @param	T	The allocated type.
		///
		/// @header	#include <macstl/mach.h>
		
		template <typename T> class mach_allocator: public impl::allocator_base <T>
			{
				public:
					/// The base type.
					typedef impl::allocator_base <T> base;
					
					/// Rebinder for allocator.
					template <class T2> struct rebind
						{
							/// Template typedef for rebound allocator.
							typedef mach_allocator <T2> other;
						};
		
					/// Constructs an allocator.
					mach_allocator () throw ()
						{
						}
						
					/// Copies the allocator.
					template <class T2> mach_allocator (const mach_allocator <T2>&) throw ()
						{
						}
						
					/// Allocates @a n objects.
					static typename base::pointer allocate (typename base::size_type n, mach_allocator <void>::const_pointer = 0)
						{
							if (n)
								{
									vm_address_t addr;
									if (vm_allocate (mach_task_self (),
										&addr, n * sizeof (T), TRUE) == KERN_SUCCESS)
										return reinterpret_cast <typename base::pointer> (addr);
									else
										throw std::bad_alloc ();
								}
							else
								return NULL;
						}
					
					/// Deallocates @a n objects at address @a ptr.
					static void deallocate (typename base::pointer ptr, typename base::size_type n)
						{
							if (ptr)
								vm_deallocate (mach_task_self (),
									reinterpret_cast <vm_address_t> (ptr), n * sizeof (T));
						}

					/// Tests whether @a left and @a right are the same allocator. Always true.
					friend bool operator== (const mach_allocator&, const mach_allocator&)
						{
							return true;
						}

					/// Tests whether @a left and @a right are different allocators. Always false.
					friend bool operator!= (const mach_allocator&, const mach_allocator&)
						{
							return false;
						}

			};
			
	}

#endif