/*
 *  valarray_vec.h
 *  macstl
 *
 *  Created by Glen Low on Jan 05 2005.
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

#ifndef MACSTL_IMPL_VALARRAY_VEC_H
#define MACSTL_IMPL_VALARRAY_VEC_H

namespace stdext
	{
		namespace impl
			{
			
			#ifndef NO_CHUNKING_ITERATOR
			
				template <typename V> class chunking_iterator
					{
						public:
							typedef V value_type;
							typedef typename V::value_data element_type;
							
							typedef std::random_access_iterator_tag iterator_category;
							typedef std::ptrdiff_t difference_type;
							typedef value_type* pointer;
							
							class reference
								{
									public:
										INLINE reference (element_type* ptr, difference_type n): ptr_ (ptr), n_ (n)
											{
											}
											
										INLINE reference& operator= (const value_type& lhs)
											{
												lhs.store (ptr_, n_);
												return *this;
											}
											
										INLINE operator const value_type () const
											{
												return value_type::load (ptr_, n_);
											}
											
									private:
										element_type* const ptr_;
										const difference_type n_;
								};
							
							explicit INLINE chunking_iterator (element_type* ptr): ptr_ (ptr)
								{
								}
								
							INLINE reference operator* () const
								{
									return reference (ptr_, 0);
								}
								
							INLINE reference operator[] (difference_type n) const
								{
									return reference (ptr_, n);
								}
								
							INLINE chunking_iterator& operator++ ()						{ ptr_ += value_type::length; return *this; }
							INLINE chunking_iterator operator++ (int)					{ return ++chunking_iterator (*this); }
							INLINE chunking_iterator& operator+= (difference_type n)	{ ptr_ += value_type::length * n; return *this; }
		
							INLINE chunking_iterator& operator-- ()						{ ptr_ -= value_type::length; return *this; }
							INLINE chunking_iterator operator-- (int)					{ return --chunking_iterator (*this); }
							INLINE chunking_iterator& operator-= (difference_type n)	{ ptr_ -= value_type::length * n; return *this; }
								
							friend INLINE chunking_iterator operator+ (const chunking_iterator& left, difference_type right)
								{
									return chunking_iterator (left) += right;
								}
		
							friend INLINE chunking_iterator operator+ (difference_type left, const chunking_iterator& right)
								{
									return chunking_iterator (right) += left;
								}
		
							friend INLINE chunking_iterator operator- (const chunking_iterator& left, difference_type right)
								{
									return chunking_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const chunking_iterator& left, const chunking_iterator& right)
								{
									return (left.ptr_ - right.ptr_) / value_type::length;
								}
								
							friend INLINE bool operator== (const chunking_iterator& left, const chunking_iterator& right)
								{
									return left.ptr_ == right.ptr_;
								}
								
							friend INLINE bool operator!= (const chunking_iterator& left, const chunking_iterator& right)
								{
									return left.ptr_ != right.ptr_;
								}
		
							friend INLINE bool operator< (const chunking_iterator& left, const chunking_iterator& right)
								{
									return left.ptr_ < right.ptr_;
								}
								
						private:
							element_type* ptr_;
					};

				template <typename V> class chunking_iterator <const V>
					{
						public:
							typedef V value_type;
							typedef typename V::value_data element_type;
							
							typedef std::random_access_iterator_tag iterator_category;
							typedef std::ptrdiff_t difference_type;
							typedef const value_type* pointer;
							typedef value_type reference;
							
							explicit INLINE chunking_iterator (const element_type* ptr): ptr_ (ptr)
								{
								}
								
							INLINE const value_type operator* () const
								{
									return value_type::load (ptr_, 0);
								}
								
							INLINE const value_type operator[] (difference_type n) const
								{
									return value_type::load (ptr_, n);
								}
								
							INLINE chunking_iterator& operator++ ()						{ ptr_ += value_type::length; return *this; }
							INLINE chunking_iterator operator++ (int)					{ return ++chunking_iterator (*this); }
							INLINE chunking_iterator& operator+= (difference_type n)	{ ptr_ += value_type::length * n; return *this; }
		
							INLINE chunking_iterator& operator-- ()						{ ptr_ -= value_type::length; return *this; }
							INLINE chunking_iterator operator-- (int)					{ return --chunking_iterator (*this); }
							INLINE chunking_iterator& operator-= (difference_type n)	{ ptr_ -= value_type::length * n; return *this; }
								
							friend INLINE chunking_iterator operator+ (const chunking_iterator& left, difference_type right)
								{
									return chunking_iterator (left) += right;
								}
		
							friend INLINE chunking_iterator operator+ (difference_type left, const chunking_iterator& right)
								{
									return chunking_iterator (right) += left;
								}
		
							friend INLINE chunking_iterator operator- (const chunking_iterator& left, difference_type right)
								{
									return chunking_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const chunking_iterator& left, const chunking_iterator& right)
								{
									return (left.ptr_ - right.ptr_) / value_type::length;
								}
								
							friend INLINE bool operator== (const chunking_iterator& left, const chunking_iterator& right)
								{
									return left.ptr_ == right.ptr_;
								}
								
							friend INLINE bool operator!= (const chunking_iterator& left, const chunking_iterator& right)
								{
									return left.ptr_ != right.ptr_;
								}
		
							friend INLINE bool operator< (const chunking_iterator& left, const chunking_iterator& right)
								{
									return left.ptr_ < right.ptr_;
								}
								
						private:
							const element_type* ptr_;
					};
			
			#endif
					
				/// Chunking type.
				
				/// @internal
				/// Template typedef to declare the chunking type corresponding to a scalar type.
				///
				/// @param	T	The scalar type.
				
				template <typename T> struct chunk;
					
				template <typename T, typename Enable2, typename Enable3, typename Enable4> class chunker <array_term <T>,
					typename enable_if <exists <typename chunk <T>::type>::value>::type,
					Enable2,
					Enable3,
					Enable4>
					{
						public:
							typedef typename chunk <T>::type chunk_type;
							
						#ifdef NO_CHUNKING_ITERATOR
						
							// in gcc 3.x, the chunking iterator's reference proxy slows down code, so we use a type not subject to type-based alias analysis instead
						
							typedef chunk_type __attribute__ ((may_alias))* chunk_iterator;
							typedef const chunk_type __attribute__ ((may_alias))* const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return reinterpret_cast <const_chunk_iterator> (that ().values_);
								}

							chunk_iterator chunk_begin ()
								{
									return reinterpret_cast <chunk_iterator> (that ().values_);
								}
								
						#else
							
							typedef chunking_iterator <chunk_type> chunk_iterator;
							typedef chunking_iterator <const chunk_type> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (that ().values_);
								}

							chunk_iterator chunk_begin ()
								{
									return chunk_iterator (that ().values_);
								}
						
						#endif							
																									
						private:
							const array_term <T>& that () const
								{
									return static_cast <const array_term <T>&> (*this);
								}
								
							array_term <T>& that ()
								{
									return static_cast <array_term <T>&> (*this);
								}
					};

				#if 0

				template <std::size_t n> struct boolean_of;

				template <> struct boolean_of <1> { typedef macstl::vec <macstl::boolean <char> > type; };
				template <> struct boolean_of <2> { typedef macstl::vec <macstl::boolean <short> > type; };
				template <> struct boolean_of <4> { typedef macstl::vec <macstl::boolean <int> > type; };
				template <> struct boolean_of <8> { typedef macstl::vec <macstl::boolean <long long> > type; };
				
				// boolean specializations: (1) for C++ bool, it is chunkable but not const-chunkable
				template <> class array_term <bool, void>: public term <bool, array_term <bool> >
					{
						public:
							typedef bool value_type;					/**< The element type, see std::valarray. */
							typedef boolean_of <sizeof (bool)>::type chunk_type;
							
							using term <bool, array_term <bool> >::operator[];

							typedef dechunk_iterator <const chunk_type> const_iterator;		/**< Constant iterator into the array. */
							typedef dechunk_iterator <chunk_type> iterator;					/**< Mutable iterator into the array. */
							typedef chunk_type::reference reference;

							/** Returns the element at index @a n. */
							value_type operator[] (std::size_t n) const
								{
									return data_of (static_cast <const chunk_type*> (values_) [n / chunk_type::length] [n % chunk_type::length]);
								}
							
							/** Returns a reference to the element at index @a n. */
							chunk_type::reference operator[] (std::size_t n)
								{ 
									return values_ [n / chunk_type::length] [n % chunk_type::length];
								}
								
							const_iterator begin () const	{ return const_iterator (values_, 0); }
							iterator begin ()				{ return iterator (values_, 0); }
													
							typedef chunk_type* chunk_iterator;
							
							/** Returns the number of elements. */
							std::size_t size () const			{ return size_; }

							chunk_iterator chunk_begin ()				{ return values_; }
							
						protected:
							chunk_type* values_;
							std::size_t size_;
							
							void init (chunk_type* data, std::size_t size)
								{
									values_ = data;
									size_ = size;
								}
		
							void swap (array_term& other)
								{
									std::swap (values_, other.values_);
									std::swap (size_, other.size_);
								}
					};																															
				#endif



			}
	}
	
#if defined(__VEC__)
#include "valarray_altivec.h"
#else
#if defined(__MMX__) || defined(__SSE__) || defined(__SSE2__)
#include "valarray_mmx.h"
#endif
#endif

#endif

