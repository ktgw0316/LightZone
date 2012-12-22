/*
 *  core_allocator.h
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

#ifndef MACSTL_IMPL_CORE_ALLOCATOR_H
#define MACSTL_IMPL_CORE_ALLOCATOR_H

namespace macstl
	{
		template <class T> class core_allocator;
				
		template <> class core_allocator <void>: public impl::allocator_base <void>
			{
				public:
					template <class T2> struct rebind
						{
							typedef core_allocator <T2> other;
						};
			};
			
		/// Core Foundation allocator.
		
		/// Puts the Standard C++ Allocator interface on Core Foundation allocators.
		///
		/// This lets you use CF allocators to allocate memory for STL containers and C++ objects.
		/// Wrappers of distinct CF allocators are distinct instances themselves.
		///
		/// @param	T	The allocated type.
		///
		/// @header	#include <macstl/core.h>

		template <typename T> class core_allocator: public impl::allocator_base <T>
			{
				public:
					/// The base type.
					typedef impl::allocator_base <T> base;
					
					/// Rebinder for core allocator.
					template <class T2> struct rebind
						{
							/// Template typedef for rebound allocator.
							typedef core_allocator <T2> other;
						};
		
					/// Constructs with allocator @a alloc.
					core_allocator (CFAllocatorRef alloc = kCFAllocatorDefault): alloc_ (alloc)
						{
						}
						
					/// Copies the allocator @a other.
					template <class T2> core_allocator (const core_allocator <T2>& other):
						alloc_ (other.alloc_)
						{
						}
						
					/// Gets the allocator.
					CFAllocatorRef data () const
						{
							return alloc_;
						}
						
					/// Allocates @a n objects.
					typename base::pointer allocate (typename base::size_type n, typename core_allocator <void>::const_pointer = 0)
						{
							if (n)
								{
									void* addr = CFAllocatorAllocate (alloc_, n * sizeof (T), 0);
									if (addr)
										return reinterpret_cast <typename base::pointer> (addr);
									else
										throw std::bad_alloc ();
								}
							else
								return NULL;
						}
					
					/// Deallocates objects at address @a ptr.
					void deallocate (typename base::pointer ptr, typename base::size_type)
						{
							CFAllocatorDeallocate (alloc_, ptr);
						}
						
					/// Tests whether @a left and @a right are the same allocator.
					friend bool operator== (const core_allocator& left, const core_allocator& right)
						{
							return left.alloc_ == right.alloc_;
						}

					/// Tests whether @a left and @a right are different allocators.
					friend bool operator!= (const core_allocator& left, const core_allocator& right)
						{
							return left.alloc_ != right.alloc_;
						}
														
				private:
					const CFAllocatorRef alloc_;
			};
	}
	
#endif
