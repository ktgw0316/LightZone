/*
 *  mmapping.h
 *  macstl
 *
 *  Created by Glen Low on Dec 04 2004.
 *
 *  Copyright (c) 2004-2005 Pixelglow Software, all rights reserved.
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
 
#ifndef MACSTL_MMAPPING_H
#define MACSTL_MMAPPING_H

#include <cstddef>
#include <new>
#include <stdexcept>

#include "impl/config.h"

#ifdef HAS_MMAP	// will eventually have a Windows implementation

#include "channel.h"
#include <sys/types.h>
#include <sys/mman.h>

namespace macstl
	{
		/// Mapping mode.
		struct mmap_mode
			{
				/// Anonymous enum.
				enum mode
					{
						/// You can only read from the macstl::mmapping.
						rdonly,
						
						/// You can read from and write to the macstl::mmapping, and the changes are saved to the file.
						rdwr,
						
						/// You can read from and write to the macstl::mmapping, but the changes are not saved to the file.
						copy
					};
			};
			
		/// Memory mapping container.
		
		/// Puts the STL random-access container interface on a memory mapped region. This lets you treat a file as if it were a part of memory.
		/// For the right element types, this avoids having to write special I/O or serialization code, improves virtual memory usage
		/// for large files and allows sharing between processes.
		///
		/// @param	T	The element value type. Must be independent of address space.
		///
		/// @header	#include <macstl/mmapping.h>
		
		template <typename T> class mmapping
			{
				public:
					/// The element value type.
					typedef T value_type;
					
					/// Iterator to access the elements.
					typedef const T* const_iterator;
					
					/// Iterator to access or change the elements.
					typedef T* iterator;

					/// An iterator to access the elements in reverse order.
					typedef std::reverse_iterator <const_iterator> const_reverse_iterator;
					
					/// An iterator to access or change the elements in reverse order.
					typedef std::reverse_iterator <iterator> reverse_iterator;
					
					/// Reference to access an element.
					typedef const T& const_reference;
					
					/// Reference to access or change an element.
					typedef T& reference;

					/// Pointer to access an element.
					typedef const T* const_pointer;
					
					/// Pointer to access or change an element.
					typedef T* pointer;

					/// Type for the difference between two iterators.
					typedef std::ptrdiff_t difference_type;
					
					/// Type for the size of the container.
					typedef std::size_t size_type;
					
					/// @name Constructors and Destructors
					
					//@{
					
					/// Constructs a mmapping from a channel @a chan, using @a mode, number of elements @a size and byte offset into the channel of @a pos.
					explicit mmapping (const channel& chan, mmap_mode::mode mode = mmap_mode::rdonly, std::size_t size = 0, std::size_t pos = 0)
						{
							int prot, flags;
							
							switch (mode)
								{
									case mmap_mode::rdonly:
										prot = PROT_READ;
										flags = MAP_SHARED;
										break;
									case mmap_mode::rdwr:
										prot = PROT_READ | PROT_WRITE;
										flags = MAP_SHARED;
										break;
									case mmap_mode::copy:
										prot = PROT_READ | PROT_WRITE;
										flags = MAP_PRIVATE;
										break;
								}
								
							int fd = chan.fd_;
							flags |= fd == -1 ? MAP_ANON : MAP_FILE;
															
							begin_ = (T*) ::mmap (0, size * sizeof (T), prot, flags, fd, pos);
							if (begin_ == (T*) -1)
								throw std::bad_alloc ();
							end_ = begin_ + size;
						}
						
					/// Destructs the region.
					~mmapping ()
						{
							::munmap (begin_, size () * sizeof (T));
						}
					
					//@}
					
					/// @name Iterators
					
					//@{
					
					/// Gets a constant iterator to the first element.
					const_iterator begin () const	{ return begin_; }
					
					/// Gets an iterator to the first element.
					iterator begin ()				{ return begin_; }

					/// Gets a constant iterator to the past-the-last element.
					const_iterator end () const		{ return end_; }
					
					/// Gets an iterator to the past-the-last element.
					iterator end ()					{ return end_; }

					/// Gets a constant reverse iterator to the last element.
					const_reverse_iterator rbegin () const	{ return const_reverse_iterator (end_); }
					
					/// Gets a reverse iterator to the last element.
					reverse_iterator rbegin ()				{ return reverse_iterator (end_); }

					/// Gets a constant reverse iterator to the past-the-first element.
					const_reverse_iterator rend () const		{ return const_reverse_iterator (begin_); }
					
					/// Gets an iterator to the past-the-first element.
					reverse_iterator rend ()					{ return const_reverse_iterator (begin_); }
					
					//@}
					
					/// @name References
					
					//@{
					
					/// Gets the element at index i.
					const_reference operator[] (size_type i) const	{ return begin_ [i]; }
						
					/// Gets the element at index i.
					reference operator[] (size_type i)				{ return begin_ [i]; }

					/// Gets the element at index i, if within bounds.
					const_reference at (size_type i) const			{ range_check (i); return begin_ [i]; }
						
					/// Gets the element at index i, if within bounds.
					reference at (size_type i)						{ range_check (i); return begin_ [i]; }

					/// Gets the first element.
					const_reference front () const	{ return begin_ [0]; }
					
					/// Gets the first element.
					reference front ()				{ return begin_ [0]; }

					/// Gets the last element.
					const_reference back () const	{ return end_ [-1]; }
					
					/// Gets the last element.
					reference back ()				{ return end_ [-1]; }
					
					//@}
					
					/// @name Sizes
					
					//@{
					
					/// Gets the number of elements.
					size_type size () const			{ return end_ - begin_; }
					
					/// Whether there are no elements.
					bool empty () const				{ return begin_ == end_; }
					
					//@}

					/// Gets the maximum number of elements.
					static size_type max_size ()	{ return static_cast <size_type> (-1); }					
					
					/// @name Assignments
					
					//@{
					
					/// Assigns the @a other region.
					mmapping& operator= (const mmapping& other)		{ std::copy (other.begin (), other.end (), begin ()); return *this; }
					
					/// Swaps with the @a other region.
					void swap (mmapping& other)						{ std::swap (begin_, other.begin_); std::swap (end_, other.end_); }					
			
					//@}
					
					/// @name Synchronizers
					
					//@{
					
					/// Synchronizes @a first to @a last with the file system.
					void sync (const_iterator first, const_iterator last)	{ ::msync (first, (last - first) * sizeof (T), MS_ASYNC); }
					
					/// Synchronizes whole region with the file system.
					void sync ()											{ sync (begin_, end_); }
					
					//@}
					
					/// Whether the two mmappings are equal i.e. their elements are equal.
					friend bool operator== (const mmapping& lhs, const mmapping& rhs)
						{
							return lhs.size () == rhs.size () && std::equal (lhs.begin (), lhs.begin (), rhs.begin ());
						}
						
					/// Whether the two mmappings are not equal i.e. their elements are not equal.
					friend bool operator!= (const mmapping& lhs, const mmapping& rhs)
						{
							return lhs.size () != rhs.size () || !std::equal (lhs.begin (), lhs.begin (), rhs.begin ());
						}

					/// Whether @a lhs is less than @a rhs i.e. lexicographically less.
					friend bool operator< (const mmapping& lhs, const mmapping& rhs)
						{
							return std::lexicographical_compare (lhs.begin (), lhs.end (), rhs.begin (), rhs.end ());
						}
					 
				private:
					T* begin_;
					T* end_;
					
					void range_check (size_type i)
						{
							if (i >= size ())
								throw std::out_of_range ("mmapping::at");
						}
			};
	}
	
#endif
#endif
