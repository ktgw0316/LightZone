/*
 *  valarray_mask.h
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
 
#ifndef MACSTL_IMPL_VALARRAY_MASK_H
#define MACSTL_IMPL_VALARRAY_MASK_H

namespace stdext
	{
		namespace impl
			{
				/**
				 * @internal
				 * @ingroup valarray_internal
				 * @brief Mask expression iterator.
				 * @param TermIt The subsetted expression iterator.
				 * @param BTermIt The mask expression iterator. The element type should be <code>bool</code>.
				 * @par Models:
				 * Least refined iterator category as Bidirectional Iterator and @a TermIt.
				 *
				 * Serves as an iterator for mask terms and subsets.
				 */
				template <typename TermIt, typename BTermIt> class mask_iterator
					{
						public:
							typedef typename iterator_ranker <
								std::bidirectional_iterator_tag,
								typename std::iterator_traits <BTermIt>::iterator_category>::type iterator_category;
							typedef typename std::iterator_traits <TermIt>::value_type value_type;
							typedef typename std::iterator_traits <BTermIt>::difference_type difference_type;
							typedef typename std::iterator_traits <TermIt>::pointer pointer;
							typedef typename std::iterator_traits <TermIt>::reference reference;
							
							mask_iterator (TermIt subterm_iter, BTermIt bool_subterm_iter, std::size_t size):
								subterm_iter_ (subterm_iter), bool_subterm_iter_ (bool_subterm_iter), last_bool_subterm_iter_ (bool_subterm_iter)
								{
									std::advance (last_bool_subterm_iter_, size);
									while (bool_subterm_iter_ != last_bool_subterm_iter_ && !*bool_subterm_iter_)
										{
											++subterm_iter_;
											++bool_subterm_iter_;
										}
								}
								
							reference operator* () const
								{
									return *subterm_iter_;
								}
								
							mask_iterator& operator++ ()
								{
									do
										{
											++subterm_iter_;
											++bool_subterm_iter_;
										}
									while (bool_subterm_iter_ != last_bool_subterm_iter_ && !*bool_subterm_iter_);
									return *this;
								}
								
							mask_iterator operator++ (int)
								{
									mask_iterator copy (*this);
									return ++copy;
								}
		
							mask_iterator& operator-- ()
								{
									do
										{
											--subterm_iter_;
											--bool_subterm_iter_;
										}
									while (!*bool_subterm_iter_);
									return *this;
								}
							
							mask_iterator operator-- (int)
								{
									mask_iterator copy (*this);
									return --copy;
								}
		
							friend bool operator== (const mask_iterator& left, const mask_iterator& right)
								{
									return left.subterm_iter_ == right.subterm_iter_;
								}
								
							friend bool operator!= (const mask_iterator& left, const mask_iterator& right)
								{
									return left.subterm_iter_ != right.subterm_iter_;
								}
								
						private:
							TermIt subterm_iter_;
							BTermIt bool_subterm_iter_;
							BTermIt last_bool_subterm_iter_;
					};

				/// Expression template mask term.
				
				/// @internal
				/// This branch terms masks the subterm. An indirect expression is not const chunkable.
				///
				/// @param	Term	The subterm type.
				/// @param	BTerm	The mask subterm type. Element type should be bool.
				/// @param	Enable	If void, enables a particular template specialization.

				template <typename Term, typename BTerm> class mask_term:
					public term <typename Term::value_type, mask_term <Term, BTerm> >
					{
						public:
							typedef typename Term::value_type value_type;					
							
							/// Iterator to access elements.
							typedef mask_iterator <typename Term::const_iterator, typename BTerm::const_iterator> const_iterator;
							
							/// Iterator to access or change elements.
							typedef mask_iterator <typename Term::iterator, typename BTerm::const_iterator> iterator;

							typedef typename Term::reference reference;
														
							mask_term (const Term& subterm, const BTerm& bool_subterm):
								subterm_ (subterm), bool_subterm_ (bool_subterm), size_ (stdext::count_n (bool_subterm.begin (), bool_subterm.size (), true))
								{
								}
								
							/// Assigns the @a other mask term.
							mask_term& operator= (const mask_term& other)
								{
									if (this != &other)
										copy_array (*this, other);
									return *this;
								}
							
							/// Assigns the @a other term.
							template <typename Expr> mask_term& operator= (const term <typename Term::value_type, Expr>& other)
								{
									copy_array (*this, other.that ());
									return *this;
								}					

							/// Assigns @a x to each element.
							mask_term& operator= (const typename Term::value_type& x)
								{
									fill_array (*this, x);
									return *this;
								}
		
							/// Gets the number of elements.
							std::size_t size () const
								{
									return size_;
								}
		
							/// Gets the element at index @a n.
							value_type operator[] (std::size_t index) const
								{
									std::size_t indirect;
									for (indirect = 0; index; ++indirect)
										if (bool_subterm_ [indirect])
											--index;
											
									return subterm_ [indirect];							
								}

							/// Gets the element at index @a n.
							reference operator[] (std::size_t index)
								{
									std::size_t indirect;
									for (indirect = 0; index; ++indirect)
										if (bool_subterm_ [indirect])
											--index;
											
									return subterm_ [indirect];							
								}
							
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin (), bool_subterm_.begin (), bool_subterm_.size ());
								}
							
							/// Gets an iterator to the first element.
							iterator begin ()
								{
									return iterator (subterm_.begin (), bool_subterm_.begin (), bool_subterm_.size ());
								}
								
							using term <typename Term::value_type, mask_term <Term, BTerm> >::operator[];
													
						private:
							Term subterm_;
							const BTerm bool_subterm_;
							const std::size_t size_;
					};
			}
	}

#endif
