/*
 *  valarray_indirect.h
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

#ifndef MACSTL_IMPL_VALARRAY_INDIRECT_H
#define MACSTL_IMPL_VALARRAY_INDIRECT_H

namespace stdext
	{
		namespace impl
			{
				template <typename TermIt, typename InTermIt> class indirect_iterator
					{
						public:
							typedef typename std::iterator_traits <InTermIt>::iterator_category iterator_category;
							typedef typename std::iterator_traits <TermIt>::value_type value_type;
							typedef typename std::iterator_traits <InTermIt>::difference_type difference_type;
							typedef typename std::iterator_traits <TermIt>::pointer pointer;
							typedef typename std::iterator_traits <TermIt>::reference reference;
							
							indirect_iterator (const TermIt& subterm_iter, const InTermIt& index_subterm_iter): subterm_iter_ (subterm_iter), index_subterm_iter_ (index_subterm_iter)
								{
								}
								
							reference operator* () const
								{
									TermIt it = subterm_iter_;
									std::advance (it, *index_subterm_iter_);
									return *it;
								}
								
							reference operator[] (difference_type n) const
								{
									TermIt it = subterm_iter_;
									std::advance (it, index_subterm_iter_ [n]);
									return *it;
								}
							
							indirect_iterator& operator++ ()					{ ++index_subterm_iter_; return *this; }
							indirect_iterator operator++ (int)					{ return const_iterator (subterm_iter_, index_subterm_iter_++); }
							indirect_iterator& operator+= (difference_type n)	{ index_subterm_iter_ += n; return *this; }
		
							indirect_iterator& operator-- ()					{ --index_subterm_iter_; return *this; }
							indirect_iterator operator-- (int)					{ return const_iterator (subterm_iter_, index_subterm_iter_--); }
							indirect_iterator& operator-= (difference_type n)	{ index_subterm_iter_ -= n; return *this; }
								
							friend indirect_iterator operator+ (const indirect_iterator& left, difference_type right)
								{
									return indirect_iterator (left.subterm_iter_, left.index_subterm_iter_ + right);
								}
		
							friend indirect_iterator operator+ (difference_type left, const indirect_iterator& right)
								{
									return indirect_iterator (right.subterm_iter_, right.index_subterm_iter_ + left);
								}
		
							friend indirect_iterator operator- (const indirect_iterator& left, difference_type right)
								{
									return indirect_iterator (left.subterm_iter_, left.index_subterm_iter_ - right);
								}
							
							friend difference_type operator- (const indirect_iterator& left, const indirect_iterator& right)
								{
									return left.index_subterm_iter_ - right.index_subterm_iter_;
								}
								
							friend bool operator== (const indirect_iterator& left, const indirect_iterator& right)
								{
									return left.index_subterm_iter_ == right.index_subterm_iter_;
								}
								
							friend bool operator!= (const indirect_iterator& left, const indirect_iterator& right)
								{
									return left.index_subterm_iter_ != right.index_subterm_iter_;
								}
		
							friend bool operator< (const indirect_iterator& left, const indirect_iterator& right)
								{
									return left.index_subterm_iter_ < right.index_subterm_iter_;
								}
								
						private:
							const TermIt subterm_iter_;
							InTermIt index_subterm_iter_;
					};
		
				/// Expression template indirect term.
				
				/// @internal
				/// This branch term indirects the subterm. An indirect expression is not const chunkable.
				///
				/// @param	Term	The subterm type.
				/// @param	InTerm	The indirect subterm type. Element type should be std::size_t.
				/// @param	Enable	If void, enables a particular template specialization.

				template <typename Term, typename InTerm> class indirect_term:
					public term <typename Term::value_type, indirect_term <Term, InTerm> >
					{
						public:
							typedef typename Term::value_type value_type;
							
							/// Iterator to access elements.
							typedef indirect_iterator <typename Term::const_iterator, typename InTerm::const_iterator> const_iterator;
							
							/// Iterator to access or change elements.
							typedef indirect_iterator <typename Term::iterator, typename InTerm::const_iterator> iterator;
							
							typedef typename Term::reference reference;
								
							indirect_term (const Term& subterm, const InTerm& index_subterm): subterm_ (subterm), index_subterm_ (index_subterm) { }
		
							/// Assigns the @a other indirect term.
							indirect_term& operator= (const indirect_term& other)
								{
									if (this != &other)
										copy_array (*this, other);
									return *this;
								}
							
							/// Assigns the @a other term.
							template <typename Expr> indirect_term& operator= (const term <typename Term::value_type, Expr>& other)
								{
									copy_array (*this, other.that ());
									return *this;
								}					

							/// Assigns @a x to each element.
							indirect_term& operator= (const value_type& x)
								{
									fill_array (*this, x);
									return *this;
								}

							/// Gets the number of elements.
							std::size_t size () const					{ return index_subterm_.size (); }

							/// Gets the element at index @a n.
							value_type operator[] (std::size_t n) const	{ return subterm_ [index_subterm_ [n]]; }

							/// Gets the element at index @a n.
							reference operator[] (std::size_t n)		{ return subterm_ [index_subterm_ [n]]; }
		
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin (), index_subterm_.begin ());
								}

							/// Gets an iterator to the first element.
							iterator begin ()
								{
									return iterator (subterm_.begin (), index_subterm_.begin ());
								}
								
							using term <typename Term::value_type, indirect_term <Term, InTerm> >::operator[];
		
						protected:
							Term subterm_;
							InTerm index_subterm_;
					};
			}
			
	}
	
#endif
