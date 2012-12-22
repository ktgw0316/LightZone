/*
 *  mach_vector.h
 *  macstl
 *
 *  Created by Glen Low on Dec 26 2002.
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

#ifndef MACSTL_MACH_VECTOR_H
#define MACSTL_MACH_VECTOR_H

namespace stdext
	{
		namespace impl
			{
				/// Vector allocation base.
				
				/// @internal
				/// Associates an allocator and memory allocation with the vector. If the vector constructor
				/// throws, then this class does the right cleanup.
				///
				/// @param	Alloc	The allocator type.
				///
				/// @note	If the allocator has zero size, the empty base class optimization also causes this class
				///			to be of size zero.
				
				template <typename Alloc> class vector_base: private Alloc
					{
						public:
							/// Gets the allocator.
							Alloc get_allocator () const
								{
									return *this;
								}

							/// Gets an iterator to the first element.
							typename Alloc::pointer 		begin ()			{ return start_; }

							/// Gets an iterator to the first element.
							typename Alloc::const_pointer	begin () const		{ return start_; }

							/// Gets the maximum number of elements before a reallocation.
							std::size_t capacity () const		{ return end_of_storage_ - start_; }
							
						protected:
							vector_base (std::size_t size, const Alloc& alloc):
								Alloc (alloc), start_ (NULL), end_of_storage_ (NULL)
								{
									start_ = Alloc::allocate (size);
									end_of_storage_ = start_ + size;
								}
							
							~vector_base ()
								{
									Alloc::deallocate (start_, end_of_storage_ - start_);
								}
								
							void swap (vector_base& other)
								{
									std::swap (start_, other.start_);
									std::swap (end_of_storage_, other.end_of_storage_);
								}
								
						private:
							typename Alloc::pointer start_;								
							typename Alloc::pointer end_of_storage_;			
					};
					
			
				template <typename T, typename Iter> typename stdext::impl::enable_if <stdext::impl::is_integral <Iter>::value == 0>::type
					inline vector_insert_dispatch (std::vector <T, macstl::mach_allocator <T> >& vec, T* pos, Iter first, Iter last)
					{
						return vec.insert_range (pos, first, last);
					}

				template <typename T, typename Iter> typename stdext::impl::enable_if <stdext::impl::is_integral <Iter>::value != 0>::type
					inline vector_insert_dispatch (std::vector <T, macstl::mach_allocator <T> >& vec, T* pos, Iter n, Iter val)
					{
						return vec.insert_fill (pos, n, val);
					}

			}
	}
	
namespace std
	{
		
		/// Mach vector.
		
		/// Specialization of vector tuned for the Mach allocator:
		/// - If the type has a trivial default constructor, the constructor vector (count) doesn't need to initialize
		///   elements since the Mach allocator allocates zero-filled memory.
		/// - If the type has a trivial copy constructor, the vector's copy constructor, assignment operator and any
		///   capacity-increasing member functions (e.g. reserve) use the Mach copy-on-write when reallocating. The initial
		///   pages are simply remapped using virtual memory instead of copied.
		/// .
		/// Overall, this will increase the speed of copying vectors (e.g. returning vectors from a function) and reduce
		/// memory impact (i.e. doesn't require twice vector's memory for copy).
		///
		/// @param	T	The element value type.										
		///
		/// @header	#include <macstl/mach.h>
			
		template <typename T> class vector <T, macstl::mach_allocator <T> >: public stdext::impl::vector_base <macstl::mach_allocator <T> >
			{
				public:					
					/// The base type.
					typedef stdext::impl::vector_base <macstl::mach_allocator <T> > base;
					
					/// The allocator type.
					typedef macstl::mach_allocator <T>								allocator_type;

					/// The element value type.
					typedef typename macstl::mach_allocator <T>::value_type			value_type;
					
					/// Pointer to access or change elements.
					typedef typename macstl::mach_allocator <T>::pointer			pointer;
					
					/// Pointer to access elements.
					typedef typename macstl::mach_allocator <T>::const_pointer		const_pointer;
					
					/// Reference to access or change elements.
					typedef typename macstl::mach_allocator <T>::reference			reference;
					
					/// Reference to access elements.
					typedef typename macstl::mach_allocator <T>::const_reference	const_reference;
					
					/// Represents the size of a vector.
					typedef typename macstl::mach_allocator <T>::size_type			size_type;
					
					/// Represents the difference between two iterators.
					typedef typename macstl::mach_allocator <T>::difference_type	difference_type;
					
					
					/// Iterator to access or change elements.
					typedef typename macstl::mach_allocator <T>::pointer			iterator;
					
					/// Iterator to access elements.
					typedef typename macstl::mach_allocator <T>::const_pointer		const_iterator;
					
					/// Iterator to access or change elements, in reverse order.
					typedef std::reverse_iterator <iterator>						reverse_iterator;
					
					/// Iterator to access elements, in reverse order.
					typedef std::reverse_iterator <const_iterator>					const_reverse_iterator;

					/// @name Constructors and Destructors
					
					//@{
					
					/// Constructs an empty vector, using allocator @a alloc.
					explicit vector (const allocator_type& alloc = allocator_type ()):
						base (0, alloc), finish_ (0)
						{
						}
						
					/// Constructs a vector of @a n copies of @a val, using allocator @a alloc.
					vector (size_type n, const value_type& val, const allocator_type& alloc = allocator_type ()):
						base (upsize (count), alloc), finish_ (0)
						{
							finish_ = std::uninitialized_fill_n (base::begin (), n, val);
						}
			
					/// Constructs a vector of @a n default-constructed elements, using allocator @a alloc.
					explicit vector (size_type n):
						base (upsize (n), allocator_type ()), finish_ (0)
						{
							finish_ = stdext::impl::has_trivial_constructor <T>::value ?
								base::begin () + n : std::uninitialized_fill_n (base::start, n, T ());
						}
						
					/// Copies the vector.
					vector (const vector& other):
						base (upsize (other.size ()), other.get_allocator ()), finish_ (0)
						{
							copy_init (other, other.size ());
						}
						
					/// Constructs a vector from the range @a first to @a last, using the allocator @a alloc.
					template <typename Iter> vector (Iter first, Iter last, const allocator_type& alloc = allocator_type ()):
						base (0, alloc), finish_ (base::begin ())
						{
							insert (base::begin (), first, last);
						}
			
					/// Destructs the vector.
					~vector ()
						{
							if (!stdext::impl::has_trivial_destructor <T>::value)
								{
									size_type n = size ();
									// we of course assume that destructors NEVER throw...
									for (std::size_t i = 0; i != n; ++i)
										base::begin () [i].~T ();
								}
						}
					
					//@}
					
					/// @name Assignments
					
					//@{
					
					/// Assigns the @a other vector.
					vector& operator= (const vector& other)
						{
							if (this != &other)
								{
									size_type other_sz = other.size ();
									if (other_sz <= base::capacity ())
										{
											size_type sz = size ();
											if (other_sz <= sz)
												{
													copy (other, other_sz);
													erase (base::begin () + other_sz, finish_);
												}
											else
												{
													copy (other, sz);
													insert (finish_, other.begin () + sz, other.end ());
												}
										}
									else
										{
											vector temp (other, other_sz, other_sz);
											swap (temp);										
										}
								}
							return *this;
						}

					/// Swaps the @a other vector.
					void swap (vector& other)
						{
							base::swap (other);
							std::swap (finish_, other.finish_);
						}
												
					//@}
						
					/// @name Iterators
					
					//@{
					
					/// Gets an iterator to the past-the-last element.
					iterator end ()							{ return finish_; }

					/// Gets an iterator to the past-the-last element.
					const_iterator end () const				{ return finish_; }
					
					/// Gets a reverse iterator to the last element.
					reverse_iterator rbegin ()				{ return reverse_iterator (finish_); }

					/// Gets a reverse iterator to the last element.
					const_reverse_iterator rbegin () const	{ return const_reverse_iterator (finish_); }

					/// Gets a reverse iterator to the past-the-first element.
					reverse_iterator rend ()				{ return reverse_iterator (base::begin ()); }

					/// Gets a reverse iterator to the past-the-first element.
					const_reverse_iterator rend () const 	{ return const_reverse_iterator (base::begin ()); }
					
					//@}
					
					/// @name Sizes
					
					//@{
					
					/// Gets the number of elements.
					size_type size () const			{ return finish_ - base::begin (); }
					
					/// Tests whether the vector is empty.
					bool empty () const				{ return base::begin () == finish_; }
					
					//@}

					/// Gets the maximum possible size.
					static size_type max_size ()	{ return size_type (-1) / sizeof (value_type); }

					/// @name References
					
					/// Gets the element at index @a i.
					reference operator[] (size_type i)				{ return base::begin () [i]; }
					
					/// Gets the element at index @a i.
					const_reference operator[] (size_type i) const	{ return base::begin () [i]; }
										
					/// Gets the element at index @a i, if within bounds.
					reference at (size_type i)				{ range_check (i); return base::begin () [i]; }
					
					/// Gets the element at index @a i, if within bounds.
					const_reference at (size_type i) const	{ range_check (i); return base::begin () [i]; }					
						
					/// Gets the first element.
					reference front ()				{ return base::begin () [0]; }
						
					/// Gets the first element.
					const_reference front () const	{ return base::begin () [0]; }
						
					/// Gets the last element.
					reference back ()				{ return finish_ [-1]; }
						
					/// Gets the last element.
					const_reference back () const	{ return finish_ [-1]; }
					
					//@}
					
					/// @name Inserters
					
					//@{
					
					/// Inserts at @a pos the range from @a first to @a last.
					template <typename Iter> void insert (iterator pos, Iter first, Iter last)
						{
							stdext::impl::vector_insert_dispatch (*this, pos, first, last);
						}

					/// Inserts at @a pos @a n copies of @a val.
					void insert (iterator pos, size_type n, const value_type& val)
						{
							insert_fill (pos, n, val);
						}

					/// Inserts at @a pos a copy of @a val.
					iterator insert (iterator pos, const value_type& val)
						{
							size_t sz = size ();
							if (sz + 1 <= base::capacity ())
								{
									if (pos == finish_)
										{
											new (finish_) value_type (val);
											++finish_;
										}
									else
										{
											new (finish_) value_type (back ());
											++finish_;
											std::copy_backward (pos, finish_ - 2, finish_ - 1);
											*pos = val;
										}
									return pos;
								}
							else
								{
									size_type place = pos - base::begin ();
									vector temp (*this, place, sz + 1);
									new (temp.finish_) value_type (val);
									++temp.finish_;
									temp.finish_ = std::uninitialized_copy (pos, finish_, temp.finish_);
									swap (temp);
									return base::begin () + place;							
								}
						}
						
					/// Inserts the value @a val at the end.
					void push_back (const value_type& val)
						{
							size_t sz = size ();
							if (sz + 1 <= base::capacity ())
								{
									new (finish_) value_type (val);
									++finish_;
								}
							else
								{
									vector temp (*this, sz, sz + 1);
									new (temp.finish_) value_type (val);
									++temp.finish_;
									swap (temp);
								}						
						}

					//@}
					
					/// @name Erasers
					
					//@{
					
					/// Erase the range @a first to @a last.
					iterator erase (iterator first, iterator last)
						{
							destroy (std::copy (last, finish_, first), finish_);
							finish_ -= (last - first);
							return first;
						}
						
					/// Erases the element at @a pos.
					iterator erase (iterator pos)
						{
							std::copy (pos + 1, finish_, pos)->~T ();
							--finish_;
							return pos;
						}
						
					/// Erases the entire vector.
					void clear ()
						{
							destroy (base::begin (), finish_);
							finish_ = base::begin ();
						}
						
					/// Erases the last element.
					void pop_back ()
						{
							back ().~T ();
							--finish_;
						}

					//@}
					
					/// @name Resizers
					
					//@{
					
					/// Changes the size of the array to @a sz, filling with @a val elements if necessary.
					void resize (size_type sz, const T& val = T ())
						{
							if (sz < size ())
								erase (base::begin () + sz, end ());
							else
								insert (end (), sz - size (), val);
						}
					
					/// Changes the capacity of the array to @a cap.
					void reserve (size_type cap)
						{
							if (base::capacity () < cap)
								{
									vector temp (*this, size (), cap);
									swap (temp);
								}
						}

					//@}
					
					// should not be public
					template <typename Iter> void insert_range (iterator pos, Iter first, Iter last)
						{
							if (stdext::impl::is_same <typename std::iterator_traits <Iter>::iterator_category, std::random_access_iterator_tag>::value)
								{
									size_type n = std::distance (first, last);
									size_type sz = size ();
									
									if (sz + n <= base::capacity ())
										{
											size_type after = finish_ - pos;
											if (after > n)
												{
													std::uninitialized_copy (finish_ - n, finish_, finish_);
													finish_ += n;
													std::copy_backward (pos, pos + after - n, pos + after);
													std::copy (first, last, pos);										
												}
											else
												{
													Iter mark = first;
													std::advance (mark, after);
													
													std::uninitialized_copy (pos, finish_, pos + n);
													std::uninitialized_copy (mark, last, finish_);
													finish_ += n;
													std::copy (first, mark, pos);
												}
										}
									else
										{
											size_type place = pos - base::begin ();
											vector temp (*this, place, sz + n);
											temp.finish_ = std::uninitialized_copy (first, last, temp.finish_);
											temp.finish_ = std::uninitialized_copy (pos, finish_, temp.finish_);
											swap (temp);								
										}
								
								}
							else
								// not random access, have to do this slowly...
								for (; first != last; ++first, ++pos)
									pos = insert (pos, *first);
						}

					// should not be public
					void insert_fill (iterator pos, size_type n, const value_type& val)
						{
							size_t sz = size ();
							if (sz + n <= base::capacity ())
								{
									size_type after = finish_ - pos;
									if (after > n)
										{
											std::uninitialized_copy (finish_ - n, finish_, finish_);
											finish_ += n;
											std::copy_backward (pos, pos + after - n, pos + after);
											std::fill_n (pos, n, val);										
										}
									else
										{
											std::uninitialized_copy (pos, finish_, pos + n);
											std::uninitialized_fill (finish_, pos + n, val);
											finish_ += n;
											std::fill_n (pos, after, val);
										}
								}
							else
								{
									size_type place = pos - base::begin ();
									vector temp (*this, place, sz + n);
									std::uninitialized_fill_n (temp.finish_, n, val);
									temp.finish_ += n;
									temp.finish_ = std::uninitialized_copy (pos, finish_, temp.finish_);
									swap (temp);								
								}
						}
						
				private:
					pointer finish_;

					vector (const vector& other, size_type n, size_type cap):
						base (upsize (cap), other.get_allocator ()), finish_ (0)
						{
							copy_init (other, n);
						}
						
					void copy_init (const vector& other, size_t n)
						{
							if (stdext::impl::has_trivial_copy <T>::value)
								{
									// trivial copy constructor: copy is simply a vm_copy of leading values, memcpy for the rest
									std::size_t bytes = n * sizeof (value_type);
									std::size_t trailing = bytes % vm_page_size;
									std::size_t leading = bytes - trailing;
									if (leading)
										vm_copy (mach_task_self (),
											reinterpret_cast <vm_address_t> (other.begin ()),
											leading,
											reinterpret_cast <vm_address_t> (base::begin ()));
										// hmm... do we need to handle vm_copy errors??
									if (trailing)
										memcpy (reinterpret_cast <char*> (base::begin ()) + leading,
											reinterpret_cast <const char*> (other.begin ()) + leading,
											trailing);
									finish_ = base::begin () + n;
								}
							else
								finish_ = std::uninitialized_copy (other.begin (), other.begin () + n, base::begin ());							
						}

					void copy (const vector& other, size_t n)
						{
							if (stdext::impl::has_trivial_assign <T>::value)
								{
									// trivial assign: assign is simply a vm_copy of leading values, memcpy for the rest
									std::size_t bytes = n * sizeof (value_type);
									std::size_t trailing = bytes % vm_page_size;
									std::size_t leading = bytes - trailing;
									if (leading)
										vm_copy (mach_task_self (),
											reinterpret_cast <vm_address_t> (other.begin ()),
											leading,
											reinterpret_cast <vm_address_t> (base::begin ()));
										// hmm... do we need to handle vm_copy errors??
									if (trailing)
										memcpy (reinterpret_cast <char*> (base::begin ()) + leading,
											reinterpret_cast <char*> (other.begin ()) + leading,
											trailing);
									finish_ = base::begin () + n;
								}
							else
								finish_ = std::copy (other.begin (), other.begin () + n, base::begin ());							
						}
						
					static size_type upsize (size_type n)
						{
							// ask for enough space to fit at least n objects and at most up to 1.5 x the page boundary
							size_type pages = (n * sizeof (T) + vm_page_size - 1) / vm_page_size;
							return (pages + (pages / 2)) * vm_page_size / sizeof (T);
						}

						
					static void destroy (pointer first, pointer last)
						{
							if (!stdext::impl::has_trivial_destructor <T>::value)
								for (; first != last; ++first)
									first->~T ();
						}

					void range_check (std::size_t i) const
						{
							if (i >= size ())
								throw std::out_of_range ("vector::at");
						}
			
			};

			
	
	
	
	}
	
#endif

