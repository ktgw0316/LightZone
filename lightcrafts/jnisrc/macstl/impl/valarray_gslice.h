/*
 *  valarray_gslice.h
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
 
#ifndef MACSTL_IMPL_VALARRAY_GSLICE_H
#define MACSTL_IMPL_VALARRAY_GSLICE_H

namespace stdext
	{
		/// Generalized slice.
		
		/// Represents a generalized slice out of an array. It specifies a starting index, a set of lengths and a set of strides;
		/// the number of lengths shall equal the number of strides. The gslice is useful for building multidimensional arrays out
		/// of the one-dimensional stdext::valarray. Used by stdext::term to construct a stdext::impl::gslice_term.
		///
		/// In order to step through each element selected by a gslice:
		/// - Suppose size and stride have n elements.
		/// - Make index with n elements, all set to 0.
		/// - Increment the last index. Each index [i] may range from 0 to size [i] - 1, and if incrementing
		///   index [i] would exceed that range, set to 0 and increment index [i - 1] instead.
		/// - The actual address is start + sum of index [i] * stride [i] for all i.
		/// .
		/// An example that appears in the Standard:
		/// - Suppose start = 3, length = {2, 4, 3}, stride = {19, 4, 1}.
		/// - Make index with 3 elements.
		/// - Increment the last index. This yields {0, 0, 0}, {0, 0, 1}, {0, 0, 2}, {0, 1, 0}, {0, 1, 1}, {0, 1, 2}, {0, 2, 0},
		///   {0, 2, 1}, {0, 2, 2}, {0, 3, 0}, {0, 3, 1}, {0, 3, 2}, {1, 0, 0}, {1, 0, 1} ...
		/// The actual addresses are then: 3, 4, 5, 7, 8, 9, 11, 12, 13, 15, 16, 17, 22, 23 ...
		/// .
		/// @header	#include <macstl/valarray.h>
		
		class gslice
			{
				public:
					/// Constructs a degenerate slice.
					gslice (): start_ (0), length_ (), stride_ ()
						{
						}
						
					/// Constructs a gslice with @a start, a set of @a length and a set of @a stride.
					gslice (std::size_t start, const valarray <std::size_t>& length, const valarray <std::size_t>& stride):
						start_ (start), length_ (length), stride_ (stride)
						{
							assert (length.size () == stride.size ());
						}
						
					/// Gets the start index of the gslice.
					std::size_t start () const					{ return start_; }
					
					/// Gets the lengths of the gslice.
					impl::array_term <std::size_t> size () const		{ return length_; }
					
					/// Gets the strides of the gslice.
					impl::array_term <std::size_t> stride () const	{ return stride_; }
					
				private:
					const std::size_t start_;
					const valarray <std::size_t> length_;
					const valarray <std::size_t> stride_;
			};

		namespace impl
			{
				template <typename TermIt> class gslice_iterator
					{
						public:
							typedef typename iterator_ranker <
								std::bidirectional_iterator_tag,
								typename std::iterator_traits <TermIt>::iterator_category>::type iterator_category;
								
							typedef typename std::iterator_traits <TermIt>::value_type value_type;
							typedef typename std::iterator_traits <TermIt>::difference_type difference_type;
							typedef typename std::iterator_traits <TermIt>::pointer pointer;
							typedef typename std::iterator_traits <TermIt>::reference reference;
							
							gslice_iterator (const TermIt& subterm_iter, const stdext::gslice& slice):
								lengther_ (slice.size ().begin ()),
								strider_ (slice.stride ().begin ()),
								subterm_iter_ (subterm_iter),
								indexer_ (slice.size ().size ())
								{
									std::advance (subterm_iter_, slice.start ());
								}
								
							reference operator* () const
								{
									return *subterm_iter_;
								}
								
							gslice_iterator& operator++ ()
								{
									for (std::size_t dim = indexer_.size (); dim; --dim)
										{
											indexer_ [dim - 1] = indexer_ [dim - 1] + 1;
											if (indexer_ [dim - 1] != lengther_ [dim - 1])
												{
													std::advance (subterm_iter_, strider_ [dim - 1]);
													break;
												}
											else
												{
													indexer_ [dim - 1] = 0;
													std::advance (subterm_iter_, (1 - lengther_ [dim - 1]) * strider_ [dim - 1]);
												}
										}
									return *this;
								}
								
							gslice_iterator operator++ (int)
								{
									gslice_iterator copy (*this);
									return ++copy;
								}
								
							gslice_iterator& operator-- ()
								{
									for (std::size_t dim = indexer_.size (); dim; --dim)
										{
											if (indexer_ [dim - 1])
												{
													indexer_ [dim - 1] = indexer_ [dim - 1] - 1;
													std::advance (subterm_iter_, -strider_ [dim - 1]);
													break;
												}
											else
												{
													indexer_ [dim - 1] = lengther_ [dim - 1] - 1;
													std::advance (subterm_iter_, (lengther_ [dim - 1] - 1) * strider_ [dim - 1]);
												}
										}
									return *this;						
								}
							
							friend bool operator== (const gslice_iterator& left, const gslice_iterator& right)
								{
									return left.subterm_iter_ == right.subterm_iter_;
								}
								
							friend bool operator!= (const gslice_iterator& left, const gslice_iterator& right)
								{
									return left.subterm_iter_ != right.subterm_iter_;
								}
								
						private:
							const stdext::valarray <std::size_t>::const_iterator lengther_;
							const stdext::valarray <std::size_t>::const_iterator strider_;
							
							TermIt subterm_iter_;
							stdext::valarray <std::size_t> indexer_;
					};
					
				/// Expression template gslice term.
				
				/// @internal
				/// This branch term selects a gslice out of the subterm. A gslice expression is not const chunkable.
				///
				/// @param	Term	The subterm type.
				/// @param	Enable	If void, enables a particular template specialization.
				
				template <typename Term> class gslice_term:
					public term <typename Term::value_type, gslice_term <Term> >
					{
						public:
							typedef typename Term::value_type value_type;
							
							/// Iterator to access elements.
							typedef gslice_iterator <typename Term::const_iterator> const_iterator;

							/// Iterator to access or change elements.
							typedef gslice_iterator <typename Term::iterator> iterator;

							typedef typename Term::reference reference;
								
							gslice_term (const Term& subterm, const stdext::gslice& slice): subterm_ (subterm), slice_ (slice),
								size_ (stdext::accumulate_n (slice.size ().begin (), slice.size ().size (), 1, std::multiplies <std::size_t> ()))
								{
								}

							/// Assigns the @a other gslice term.
							gslice_term& operator= (const gslice_term& other)
								{
									if (this != &other)
										copy_array (*this, other);
									return *this;
								}

							/// Assigns the @a other term.
							template <typename Expr> gslice_term& operator= (const term <value_type, Expr>& other)
								{
									copy_array (*this, other.that ());
									return *this;
								}					

							/// Assigns @a x to each element.
							gslice_term& operator= (const value_type& x)
								{
									fill_array (*this, x);
									return *this;
								}
		
							/// Gets the number of elements
							std::size_t size () const
								{
									return size_;
								}
		
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin (), slice_);
								}
								
							/// Gets an iterator to the first element.
							iterator begin ()
								{
									return iterator (subterm_.begin (), slice_);
								}
								
							/// Gets the element at index @a n.
							value_type operator[] (std::size_t n) const
								{
									std::size_t index = slice_.start ();
									
									stdext::valarray <std::size_t>::const_iterator lengther = slice_.size ().begin ();
									stdext::valarray <std::size_t>::const_iterator strider = slice_.stride ().begin ();
									
									for (std::size_t dim = slice_.size ().size (); dim; --dim)
										{
											std::size_t len = lengther [dim - 1];
											index += (n % len) * strider [dim - 1];
										
											n /= len;
										}
									
									return subterm_ [index];							
								}
							
							/// Gets the element at index @a n.
							reference operator[] (std::size_t n)
								{
									std::size_t index = slice_.start ();
									
									stdext::valarray <std::size_t>::const_iterator lengther = slice_.size ().begin ();
									stdext::valarray <std::size_t>::const_iterator strider = slice_.stride ().begin ();
									
									for (std::size_t dim = slice_.size ().size (); dim; --dim)
										{
											std::size_t len = lengther [dim - 1];
											index += (n % len) * strider [dim - 1];
										
											n /= len;
										}
									
									return subterm_ [index];							
								}
							
							using term <typename Term::value_type, gslice_term <Term> >::operator[];
							
						private:
							Term subterm_;
							const stdext::gslice& slice_;
							const std::size_t size_;
					};
		
			}
						
	}
	
#endif
