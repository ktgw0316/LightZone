/*
 *  valarray_shift.h
 *  macstl
 *
 *  Created by Glen Low on Jun 23 2003.
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

#ifndef MACSTL_IMPL_VALARRAY_SHIFT_H
#define MACSTL_IMPL_VALARRAY_SHIFT_H

namespace stdext
	{
		namespace impl
			{
				template <typename TermIt> class shift_iterator
					{
						public:
							typedef typename iterator_ranker <std::bidirectional_iterator_tag,
								typename std::iterator_traits <TermIt>::iterator_category>::type iterator_category;
							
							typedef typename std::iterator_traits <TermIt>::value_type value_type;
							typedef typename std::iterator_traits <TermIt>::difference_type difference_type;
							typedef value_type* pointer;
							typedef value_type& reference;
							
							shift_iterator (TermIt subterm_iter, int limit, int offset):
								subterm_iter_ (subterm_iter), limit_ (limit), offset_ (offset)
								{
									if (offset >= 0 && offset < limit)
										std::advance (subterm_iter_, offset);
								}
								
							value_type operator* () const
								{
									return offset_ >= 0 && offset_ < limit_ ? *subterm_iter_ : value_type ();
								}								
		
							shift_iterator& operator++ ()
								{
									if (offset_ >= 0 && offset_ < limit_)
										++subterm_iter_;
									++offset_;
									return *this;
								}
								
							shift_iterator operator++ (int)
								{
									shift_iterator copy (*this);
									return ++copy;
								}
								
							shift_iterator& operator-- ()
								{
									if (offset_ > 0 && offset_ <= limit_)
										--subterm_iter_;
									--offset_;
									return *this;
								}
								
							shift_iterator operator-- (int)
								{
									shift_iterator copy (*this);
									return --copy;
								}
																	
							friend bool operator== (const shift_iterator& left, const shift_iterator& right)
								{
									return left.offset_ == right.offset_;
								}
								
							friend bool operator!= (const shift_iterator& left, const shift_iterator& right)
								{
									return left.offset_ != right.offset_;
								}
								
						private:
							TermIt subterm_iter_;
							const int limit_;
							int offset_;
					};
					
				/// Expression template shift term.
				
				/// @internal
				/// Shifts the subterm. A shift expression is not const chunkable.
				///
				/// @param	Term	The subterm type.
				/// @param	Enable	If void, enables a particular template specialization.
					
				template <typename Term> class shift_term:
					public term <typename Term::value_type, shift_term <Term> >
					{
						public:
							typedef typename Term::value_type value_type;
							
							typedef shift_iterator <typename Term::const_iterator> const_iterator;
							typedef shift_iterator <typename Term::const_iterator> iterator;
							
							typedef value_type reference;
							
							value_type operator[] (std::size_t n) const
								{
									int index = n + offset_;
									return index >= 0 && index < subterm_.size () ? subterm_ [index] : value_type ();
								}
								
							std::size_t size () const			{ return subterm_.size (); }
							
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin (), subterm_.size (), offset_);
								}

							shift_term (const Term& subterm, int offset): subterm_ (subterm), offset_ (offset) { }
							
							using term <typename Term::value_type, shift_term <Term> >::operator[];
												
						private:
							const Term subterm_;
							const int offset_;
					};

				template <typename TermIt> class cshift_iterator
					{
						public:
							typedef typename iterator_ranker <std::bidirectional_iterator_tag,
								typename std::iterator_traits <TermIt>::iterator_category>::type iterator_category;
							typedef typename std::iterator_traits <TermIt>::value_type value_type;
							typedef typename std::iterator_traits <TermIt>::difference_type difference_type;
							typedef value_type* pointer;
							typedef value_type& reference;
							
							cshift_iterator (TermIt exprit, std::size_t size, difference_type offset):
								firstit_ (exprit), lastit_ (exprit), subterm_iter_ (exprit)
								{
									std::advance (lastit_, size);
									std::advance (subterm_iter_, offset % size);
								}
								
							value_type operator* () const
								{
									return *subterm_iter_;
								}
							
							cshift_iterator& operator++ ()
								{
									if (++subterm_iter_ == lastit_)
										subterm_iter_ = firstit_;
										
									return *this;
								}
								
							cshift_iterator operator++ (int)
								{
									cshift_iterator copy (*this);
									return ++copy;
								}
								
							cshift_iterator& operator-- ()
								{
									if (subterm_iter_ == firstit_)
										subterm_iter_ = lastit_;
										
									--subterm_iter_;
		
									return *this;
								}
		
							cshift_iterator operator-- (int)
								{
									cshift_iterator copy (*this);
									return --copy;
								}
							
							friend bool operator== (const cshift_iterator& left, const cshift_iterator& right)
								{
									return left.subterm_iter_ == right.subterm_iter_;
								}
								
							friend bool operator!= (const cshift_iterator& left, const cshift_iterator& right)
								{
									return left.subterm_iter_ != right.subterm_iter_;
								}
								
						private:
							const TermIt firstit_;
							
							TermIt lastit_;
							TermIt subterm_iter_;
					};
		
				/// Expression template cshift term.
				
				/// @internal
				/// Shifts circularly the subterm. A cshift expression is not const chunkable.
				///
				/// @param	Term	The subterm type.
				/// @param	Enable	If void, enables a particular template specialization.
							
				template <typename Term> class cshift_term:
					public term <typename Term::value_type, cshift_term <Term> >
					{
						public:
							typedef typename Term::value_type value_type;
							
							typedef cshift_iterator <typename Term::const_iterator> const_iterator;
							typedef cshift_iterator <typename Term::const_iterator> iterator;
							
							typedef value_type reference;
							
							value_type operator[] (std::size_t n) const		{ return subterm_ [(n + offset_) % subterm_.size ()]; }
							std::size_t size () const						{ return subterm_.size (); }
							
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin (), subterm_.size (), offset_);
								}

							cshift_term (const Term& subterm, int offset): subterm_ (subterm), offset_ (offset) { }							

							using term <typename Term::value_type, cshift_term <Term> >::operator[];
							
						private:
							const Term subterm_;
							const int offset_;
					};			
			}
			
	}
	
#endif
