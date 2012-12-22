/*
 *  valarray_slice.h
 *  macstl
 *
 *  Created by Glen Low on Jun 09 2003.
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
 
#ifndef MACSTL_IMPL_VALARRAY_SLICE_H
#define MACSTL_IMPL_VALARRAY_SLICE_H

namespace stdext
	{
		/// BLAS-like slice.
		
		/// Represents a BLAS-like slice out of an array. It specifies a starting index, a length and a stride.
		/// Used by stdext::term to construct a stdext::impl::slice_term.
		
		class slice
			{
				public:
					/// Constructs a slice that selects nothing.
					slice (): start_ (0), size_ (0), stride_ (0) { }
					
					/// Constructs a slice that selects @a start, @a start + @a stride ... [@a size times].
					slice (std::size_t start, std::size_t size, std::size_t stride): start_ (start), size_ (size), stride_ (stride) { }
					
					/// Returns the start index of the slice.
					std::size_t start () const	{ return start_; }
					
					/// Returns the length of the slice.
					std::size_t size () const	{ return size_; }
					
					/// Returns the stride of the slice.
					std::size_t stride () const	{ return stride_; }

				private:
					const std::size_t start_;
					const std::size_t size_;
					const std::size_t stride_;
			};

		namespace impl
			{
				template <typename TermIt> class slice_iterator
					{
						public:
							typedef typename std::iterator_traits <TermIt>::iterator_category iterator_category;
							typedef typename std::iterator_traits <TermIt>::value_type value_type;
							typedef typename std::iterator_traits <TermIt>::difference_type difference_type;
							typedef typename std::iterator_traits <TermIt>::pointer pointer;
							typedef typename std::iterator_traits <TermIt>::reference reference;
							
							slice_iterator (TermIt exprit, std::size_t start, std::size_t stride): termit_ (exprit), stride_ (stride)
								{
									std::advance (termit_, start);
								}
								
							reference operator* () const						{ return *termit_; }
							reference operator[] (difference_type n) const		{ return termit_ [n * stride_]; }
							
							slice_iterator& operator++ ()						{ std::advance (termit_, stride_); return *this; }
							slice_iterator operator++ (int)						{ slice_iterator copy (*this); return ++copy; }
							slice_iterator& operator+= (difference_type n)		{ termit_ += n * stride_; return *this; }
			
							slice_iterator& operator-- ()						{ std::advance (termit_, -stride_); return *this; }
							slice_iterator operator-- (int)						{ slice_iterator copy (*this); return --copy; }
							slice_iterator& operator-= (difference_type n)		{ termit_ -= n * stride_; return *this; }
								
							friend slice_iterator operator+ (const slice_iterator& left, difference_type right)
								{
									return slice_iterator (left.termit_, right * left.stride_, left.stride_);
								}
			
							friend slice_iterator operator+ (difference_type left, const slice_iterator& right)
								{
									return slice_iterator (right.termit_, left * right.stride_, right.stride_);
								}
			
							friend slice_iterator operator- (const slice_iterator& left, difference_type right)
								{
									return slice_iterator (left.termit_, -right * left.stride_, left.stride_);
								}
							
							friend difference_type operator- (const slice_iterator& left, const slice_iterator& right)
								{
									return (left.termit_ - right.termit_) / left.stride_;
								}
								
							friend bool operator== (const slice_iterator& left, const slice_iterator& right)
								{
									return left.termit_ == right.termit_;
								}
								
							friend bool operator!= (const slice_iterator& left, const slice_iterator& right)
								{
									return left.termit_ != right.termit_;
								}
		
							friend bool operator< (const slice_iterator& left, const slice_iterator& right)
								{
									return left.termit_ < right.termit_;
								}
								
						private:
							TermIt termit_;
							const std::size_t stride_;
					};
					
				/// Expression template slice term.
				
				/// @internal
				/// This branch term selects a slice out of the subterm. A slice expression is not const chunkable, except for
				/// Altivec int, unsigned int and float.
				///
				/// @param	Term	The subterm type.
															
				template <typename Term> class slice_term:
					public term <typename Term::value_type, slice_term <Term> >,
					public chunker <slice_term <Term> >
					{
						public:
							typedef typename Term::value_type value_type;
							
							/// Iterator to access elements.
							typedef slice_iterator <typename Term::const_iterator> const_iterator;
							
							/// Iterator to access or change elements.
							typedef slice_iterator <typename Term::iterator> iterator;
							typedef typename Term::reference reference;
							
							slice_term (const Term& subterm, const stdext::slice& sliced):
								subterm_ (subterm), slice_ (sliced)
								{
								}
							
							/// Gets the element at index @a n.
							value_type operator[] (std::size_t index) const	{ return subterm_ [index * slice_.stride () + slice_.start ()]; }
							
							/// Gets the element at index @a n
							reference operator[] (std::size_t index)		{ return subterm_ [index * slice_.stride () + slice_.start ()]; }
							
							/// Gets the number of elements.
							std::size_t size () const						{ return slice_.size (); }
							
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin (), slice_.start (), slice_.stride ());
								}

							/// Gets an iterator to the first element.
							iterator begin ()
								{
									return iterator (subterm_.begin (), slice_.start (), slice_.stride ());
								}

							/// Assigns the @a other slice term.
							slice_term& operator= (const slice_term& other)
								{
									if (this != &other)
										copy_array (*this, other);
									return *this;
								}

							/// Assigns the @a other term.
							template <typename Expr> slice_term& operator= (const term <typename Term::value_type, Expr>& other)
								{
									copy_array (*this, other.that ());
									return *this;
								}					

							/// Assigns @a x to each element.
							slice_term& operator= (const typename Term::value_type& x)
								{
									fill_array (*this, x);
									return *this;
								}

						private:
							Term subterm_;
							const stdext::slice slice_;
							
							template <typename Term2, typename Enable1, typename Enable2, typename Enable3, typename Enable4> friend class chunker;
					};

			}
	}
	
#endif

