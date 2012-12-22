/*
 *  valarray_function.h
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
 *  While it is an open-source license, the RPL you from keeping your
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

#ifndef MACSTL_IMPL_VALARRAY_FUNCTION_H
#define MACSTL_IMPL_VALARRAY_FUNCTION_H

namespace stdext
	{
		namespace impl
			{
			
				template <typename T> class literal_iterator
					{
						public:
							typedef std::random_access_iterator_tag iterator_category;
							typedef T value_type; 
							typedef std::ptrdiff_t difference_type;
							typedef const value_type* pointer;
							typedef value_type reference;
							
							INLINE literal_iterator (const value_type& literal, std::ptrdiff_t index): literal_ (literal), index_ (index)
								{
								}
								
							// this looks and smells like a trivial copy constructor, but it actually inhibits the "vectorized memcpy disabled due to use of -faltivec without -maltivec"
							// pessimization, which otherwise would have created a non-inlineable memcpy for the trivial copy constructor...
							INLINE literal_iterator (const literal_iterator& other): literal_ (other.literal_)
								{
								}
							
							INLINE const value_type& operator* () const					{ return literal_; }
							INLINE const value_type& operator[] (std::ptrdiff_t) const	{ return literal_; }

							INLINE literal_iterator& operator++ ()						{ ++index_; return *this; }
							INLINE literal_iterator operator++ (int)					{ return literal_iterator (literal_, index_++); }
							INLINE literal_iterator& operator+= (difference_type n)		{ index_ += n; return *this; }
		
							INLINE literal_iterator& operator-- ()						{ --index_; return *this; }
							INLINE literal_iterator operator-- (int)					{ return literal_iterator (literal_, index_--); }
							INLINE literal_iterator& operator-= (difference_type n)		{ index_ -= n; return *this; }
								
							friend INLINE literal_iterator operator+ (const literal_iterator& left, difference_type right)
								{
									return literal_iterator (left) += right;
								}
		
							friend INLINE literal_iterator operator+ (difference_type left, const literal_iterator& right)
								{
									return literal_iterator (right) += left;
								}
		
							friend INLINE literal_iterator operator- (const literal_iterator& left, difference_type right)
								{
									return literal_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const literal_iterator& left, const literal_iterator& right)
								{
									return left.index_ - right.index_;
								}
								
							friend INLINE bool operator== (const literal_iterator& left, const literal_iterator& right)
								{
									return left.index_ == right.index_;
								}
								
							friend INLINE bool operator!= (const literal_iterator& left, const literal_iterator& right)
								{
									return left.index_ != right.index_;
								}
		
							friend INLINE bool operator< (const literal_iterator& left, const literal_iterator& right)
								{
									return left.index_ < right.index_;
								}
								
						protected:
							const value_type literal_;
							std::ptrdiff_t index_;
					};
					
				template <typename Term, typename Enable2, typename Enable3, typename Enable4> class chunker <literal_term <Term>,
					typename enable_if <exists <typename Term::const_chunk_iterator>::value>::type,
					Enable2,
					Enable3,
					Enable4>
					{
						public:
							typedef typename std::iterator_traits <typename Term::const_chunk_iterator>::value_type chunk_type;
							
							typedef literal_iterator <chunk_type> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (chunk_type::fill (that ().literal_), 0);
								}
																
						private:
							const literal_term <Term>& that () const
								{
									return static_cast <const literal_term <Term>&> (*this);
								}
					};
				
				/// Expression template literal term.
				
				/// @internal
				/// This leaf term stores a single value. A literal term is const chunkable if the subterm is const chunkable. Such terms are declared as
				/// partial specializations with additional members.
				///
				/// @param	Term	The subterm type.
				/// @param	Enable	If void, enables a particular template specialization.
				///
				/// @note	This term does not actually store its subterm, but uses the subterm type to figure out how to chunk the single value.

				template <typename Term> class literal_term:
					public term <typename Term::value_type, literal_term <Term> >,
					public chunker <literal_term <Term> >
					{
						public:
							/// Element value type.
							typedef typename Term::value_type value_type;
							
							/// Iterator to access elements.
							typedef literal_iterator <value_type> const_iterator;
							typedef literal_iterator <value_type> iterator;
							
							typedef value_type reference;

							/// Constructs from a single value and a subterm.
							literal_term (const value_type& literal, const Term& term):
								literal_ (literal), size_ (term.size ())
								{
								}

							/// Gets the element at the index. Since this is a literal, all elements are the same.
							value_type operator[] (std::size_t) const	{ return literal_; }
							
							/// Gets the number of elements. Same as the subterm size.
							std::size_t size () const					{ return size_; }
								
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (literal_, 0);
								}

						private:
							const value_type literal_;
							const std::size_t size_;
							
							template <typename Term2, typename Enable1, typename Enable2, typename Enable3, typename Enable4> friend class chunker;
					};

				template <typename TermIt, template <typename> class UOp> class unary_iterator
					{
						public:
							typedef UOp <typename std::iterator_traits <TermIt>::value_type> operation;
							
							typedef typename operation::result_type value_type; 
							typedef typename std::iterator_traits <TermIt>::iterator_category iterator_category;
							typedef typename std::iterator_traits <TermIt>::difference_type difference_type;
							typedef const value_type* pointer;
							typedef value_type reference;
							
							INLINE unary_iterator (const TermIt& subterm_iter): subterm_iter_ (subterm_iter)
								{
								}
								
							INLINE const value_type operator* () const
								{
									return operation () (*subterm_iter_);
								}
								
							INLINE const value_type operator[] (typename std::iterator_traits <TermIt>::difference_type n) const
								{
									return operation () (subterm_iter_ [n]);
								}
								
							INLINE unary_iterator& operator++ ()					{ ++subterm_iter_; return *this; }
							INLINE unary_iterator operator++ (int)					{ return unary_iterator (subterm_iter_++); }
							INLINE unary_iterator& operator+= (difference_type n)	{ subterm_iter_ += n; return *this; }
		
							INLINE unary_iterator& operator-- ()					{ --subterm_iter_; return *this; }
							INLINE unary_iterator operator-- (int)					{ return unary_iterator (subterm_iter_--); }
							INLINE unary_iterator& operator-= (difference_type n)	{ subterm_iter_ -= n; return *this; }
								
							friend INLINE unary_iterator operator+ (const unary_iterator& left, difference_type right)
								{
									return unary_iterator (left) += right;
								}
		
							friend INLINE unary_iterator operator+ (difference_type left, const unary_iterator& right)
								{
									return unary_iterator (right) += left;
								}
		
							friend INLINE unary_iterator operator- (const unary_iterator& left, difference_type right)
								{
									return unary_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const unary_iterator& left, const unary_iterator& right)
								{
									return left.subterm_iter_ - right.subterm_iter_;
								}
								
							friend INLINE bool operator== (const unary_iterator& left, const unary_iterator& right)
								{
									return left.subterm_iter_ == right.subterm_iter_;
								}
								
							friend INLINE bool operator!= (const unary_iterator& left, const unary_iterator& right)
								{
									return left.subterm_iter_ != right.subterm_iter_;
								}
		
							friend INLINE bool operator< (const unary_iterator& left, const unary_iterator& right)
								{
									return left.subterm_iter_ < right.subterm_iter_;
								}
								
						private:
							TermIt subterm_iter_;
					};
				
				template <typename Term, template <typename> class UOp, typename Enable3, typename Enable4>
					class chunker <unary_term <Term, UOp>,
						typename enable_if <exists <typename Term::const_chunk_iterator>::value>::type,
						typename enable_if <exists <typename UOp <typename std::iterator_traits <typename Term::const_chunk_iterator>::value_type>::result_type>::value>::type,
						Enable3,
						Enable4>
					{
						public:
							typedef unary_iterator <typename Term::const_chunk_iterator, UOp> const_chunk_iterator;
								
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (that ().subterm_.chunk_begin ());
								}
								
						private:
							const unary_term <Term, UOp>& that () const
								{
									return static_cast <const unary_term <Term, UOp>&> (*this);
								}
					};

				/// Expression template unary term.
				
				/// @internal
				/// This branch term applies a unary functor to its subterm. A unary term is const chunkable if the subterm is const chunkable and there is
				/// an appropriate unary functor on the chunk type. Such terms are declared as partial specializations with additional members.
				///
				/// @param	Term	The subterm type.
				/// @param	UOp		The unary functor.

				template <typename Term, template <typename> class UOp> class unary_term:
					public term <typename UOp <typename Term::value_type>::result_type, unary_term <Term, UOp> >,
					public chunker <unary_term <Term, UOp> >					
					{
						public:
							typedef UOp <typename Term::value_type> operation;
							typedef typename operation::result_type value_type;
							
							/// Iterator to access elements.
							typedef unary_iterator <typename Term::const_iterator, UOp> const_iterator;
							typedef unary_iterator <typename Term::const_iterator, UOp> iterator;
							
							typedef value_type reference;
							
							unary_term (const Term& subterm): subterm_ (subterm)
								{
								}
								
							/// Gets the element at index n.
							const value_type operator[] (std::size_t n) const	{ return operation () (subterm_ [n]); }
							
							/// Gets the number of elements.
							std::size_t size () const					{ return subterm_.size (); }
							
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin ());
								}
								
						private:
							const Term subterm_;

							template <typename Term2, typename Enable1, typename Enable2, typename Enable3, typename Enable4> friend class chunker;
					};

				template <typename TermIt, typename Fn>
					class apply_iterator
					{
						public:
							typedef typename std::iterator_traits <TermIt>::iterator_category iterator_category;
							typedef typename std::iterator_traits <TermIt>::value_type value_type; 
							typedef typename std::iterator_traits <TermIt>::difference_type difference_type;
							typedef const value_type* pointer;
							typedef value_type reference;
							
							apply_iterator (const TermIt& subterm_iter, Fn func): subterm_iter_ (subterm_iter), func_ (func)
								{
								}
								
							const value_type operator* () const
								{
									return func_ (*subterm_iter_);
								}
								
							const value_type operator[] (difference_type n) const
								{
									return func_ (subterm_iter_ [n]);
								}

							apply_iterator& operator++ ()					{ ++subterm_iter_; return *this; }
							apply_iterator operator++ (int)					{ return apply_iterator (subterm_iter_++, func_); }
							apply_iterator& operator+= (difference_type n)	{ subterm_iter_ += n; return *this; }
		
							apply_iterator& operator-- ()					{ --subterm_iter_; return *this; }
							apply_iterator operator-- (int)					{ return apply_iterator (subterm_iter_--, func_); }
							apply_iterator& operator-= (difference_type n)	{ subterm_iter_ -= n; return *this; }
								
							friend apply_iterator operator+ (const apply_iterator& left, difference_type right)
								{
									return apply_iterator (left) += right;
								}
		
							friend apply_iterator operator+ (difference_type left, const apply_iterator& right)
								{
									return apply_iterator (right) += left;
								}
		
							friend apply_iterator operator- (const apply_iterator& left, difference_type right)
								{
									return apply_iterator (left) -= right;
								}
							
							friend difference_type operator- (const apply_iterator& left, const apply_iterator& right)
								{
									return left.subterm_iter_ - right.subterm_iter_;
								}
								
							friend bool operator== (const apply_iterator& left, const apply_iterator& right)
								{
									return left.subterm_iter_ == right.subterm_iter_ && left.func_ == right.func_;
								}
								
							friend bool operator!= (const apply_iterator& left, const apply_iterator& right)
								{
									return left.subterm_iter_ != right.subterm_iter_ || left.func_ != right.func_;
								}
		
							friend bool operator< (const apply_iterator& left, const apply_iterator& right)
								{
									return left.subterm_iter_ < right.subterm_iter_;
								}

						protected:
							TermIt subterm_iter_;
							Fn const func_;
					};

				/// Expression template apply term.
				
				/// @internal
				/// This branch term applies an arbitrary unary function to its subterm. A apply term is not const chunkable.
				///
				/// @param	Term	The subterm type.
				/// @param	Fn		The unary function.

				template <typename Term, typename Fn> class apply_term:
					public term <typename Term::value_type, apply_term <Term, Fn> >
					{
						public:
							typedef typename Term::value_type value_type;
							
							/// Iterator to access elements.
							typedef apply_iterator <typename Term::const_iterator, Fn> const_iterator;
							typedef apply_iterator <typename Term::iterator, Fn> iterator;
							
							typedef value_type reference;
							
							/// Gets the element at index n.
							const value_type operator[] (std::size_t n) const	{ return func_ (subterm_ [n]); }
							
							/// Gets the number of elements.
							std::size_t size () const					{ return subterm_.size (); }
							
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (subterm_.begin (), func_);
								}
								
							apply_term (const Term& subterm, Fn func): subterm_ (subterm), func_ (func)
								{
								}
								
							using term <typename Term::value_type, apply_term <Term, Fn> >::operator[];
								
						protected:
							const Term subterm_;
							Fn const func_;
					};

				// ranking iterator categories: given 2 iterator categories we want to know which is least refined
				
				template <typename Cat> struct iterator_category_rank;
				
				template <> struct iterator_category_rank <std::input_iterator_tag>			{ static const int value = 1; };
				template <> struct iterator_category_rank <std::forward_iterator_tag>		{ static const int value = 2; };
				template <> struct iterator_category_rank <std::bidirectional_iterator_tag>	{ static const int value = 3; };
				template <> struct iterator_category_rank <std::random_access_iterator_tag> { static const int value = 4; };
					
				template <int Rank> struct iterator_rank_category;
				
				template <> struct iterator_rank_category <1>	{ typedef std::input_iterator_tag type; };
				template <> struct iterator_rank_category <2>	{ typedef std::forward_iterator_tag type; };
				template <> struct iterator_rank_category <3>	{ typedef std::bidirectional_iterator_tag type; };
				template <> struct iterator_rank_category <4>	{ typedef std::random_access_iterator_tag type; };
		
				template <typename Cat1, typename Cat2> struct iterator_ranker
					{
						enum
							{
								first_category_rank = iterator_category_rank <Cat1>::value,
								second_category_rank = iterator_category_rank <Cat2>::value
							};
						
						typedef typename iterator_rank_category
							<first_category_rank < second_category_rank ? first_category_rank : second_category_rank>::type	type;
					};
					
				template <typename LTermIt, typename RTermIt, template <typename, typename> class BOp> class binary_iterator
					{
						public:
							typedef BOp <
								typename std::iterator_traits <LTermIt>::value_type,
								typename std::iterator_traits <RTermIt>::value_type> operation;
							
							// iterator category is the category with the least rank of the two expression's categories
							typedef typename iterator_ranker
								<typename std::iterator_traits <LTermIt>::iterator_category,
								typename std::iterator_traits <RTermIt>::iterator_category>::type iterator_category;
							
							typedef typename std::iterator_traits <LTermIt>::difference_type difference_type;
							typedef typename operation::result_type value_type;
							typedef const value_type* pointer;
							typedef value_type reference;
							
							INLINE binary_iterator (const LTermIt& left_subterm_iter, const RTermIt& right_subterm_iter):
								left_subterm_iter_ (left_subterm_iter), right_subterm_iter_ (right_subterm_iter)
								{
								}
								
							INLINE const value_type operator* () const
								{
									return operation () (*left_subterm_iter_, *right_subterm_iter_);
								}
								
							INLINE const value_type operator[] (typename std::iterator_traits <LTermIt>::difference_type n) const
								{
									return operation () (left_subterm_iter_ [n], right_subterm_iter_ [n]);
								}
								
							INLINE binary_iterator& operator++ ()					{ ++left_subterm_iter_; ++right_subterm_iter_; return *this; }
							INLINE binary_iterator operator++ (int)					{ return binary_iterator (left_subterm_iter_++, right_subterm_iter_++); }
							INLINE binary_iterator& operator+= (difference_type n)	{ left_subterm_iter_ += n; right_subterm_iter_ += n; return *this; }
		
							INLINE binary_iterator& operator-- ()					{ --left_subterm_iter_; --right_subterm_iter_; return *this; }
							INLINE binary_iterator operator-- (int)					{ return binary_iterator (left_subterm_iter_--, right_subterm_iter_--); }
							INLINE binary_iterator& operator-= (difference_type n)	{ left_subterm_iter_ -= n; right_subterm_iter_ -= n; return *this; }
								
							friend INLINE binary_iterator operator+ (const binary_iterator& left, difference_type right)
								{
									return binary_iterator (left) += right;
								}

							friend INLINE binary_iterator operator+ (difference_type left, const binary_iterator& right)
								{
									return binary_iterator (right) += left;
								}

							friend INLINE binary_iterator operator- (const binary_iterator& left, difference_type right)
								{
									return binary_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const binary_iterator& left, const binary_iterator& right)
								{
									return left.left_subterm_iter_ - right.left_subterm_iter_;
								}
								
							friend INLINE bool operator== (const binary_iterator& left, const binary_iterator& right)
								{
									return left.left_subterm_iter_ == right.left_subterm_iter_;
								}
								
							friend INLINE bool operator!= (const binary_iterator& left, const binary_iterator& right)
								{
									return left.left_subterm_iter_ != right.left_subterm_iter_;
								}

							friend INLINE bool operator< (const binary_iterator& left, const binary_iterator& right)
								{
									return left.left_subterm_iter_ < right.left_subterm_iter_;
								}
															
						// private:
						public:
							LTermIt left_subterm_iter_;
							RTermIt right_subterm_iter_;
							
						//	template <template <typename> class Func, typename Expr, typename Enable1, typename Enable2> friend struct accumulate_array_dispatch;
							template <typename InIter, typename Size, typename T, typename BOp2, typename Enable1, typename Enable2> friend struct accumulate_n_dispatch;
					};
					
				template <typename LTerm, typename RTerm, template <typename, typename> class BOp, typename Enable2, typename Enable3, typename Enable4>
					class chunker <binary_term <LTerm, RTerm, BOp>,
						typename enable_if <exists <typename BOp <
							typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename RTerm::const_chunk_iterator>::value_type>::result_type>::value>::type,
						Enable2,
						Enable3,
						Enable4>
					{
						public:
							typedef binary_iterator <typename LTerm::const_chunk_iterator, typename RTerm::const_chunk_iterator,
								BOp> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().left_subterm_.chunk_begin (),
										that ().right_subterm_.chunk_begin ());
								}
								
						private:
							const binary_term <LTerm, RTerm, BOp>& that () const
								{
									return static_cast <const binary_term <LTerm, RTerm, BOp>&> (*this);
								}
					};
		
				/// Expression template binary term.
				
				/// @internal
				/// This branch term applies a binary functor to its subterms. A binary term is const chunkable if each subterm's chunked types
				/// are the same and there is an appropriate binary functor on the chunked type. Such terms are declared as partial specializations
				/// with additional members.
				///
				/// @param	LTerm	The left subterm type.
				/// @param	RTerm	The right subterm type.
				/// @param	BOp		The binary functor.
				
				template <typename LTerm, typename RTerm, template <typename, typename> class BOp> class binary_term:
					public term <typename BOp <typename LTerm::value_type, typename RTerm::value_type>::result_type, binary_term <LTerm, RTerm, BOp> >,
					public chunker <binary_term <LTerm, RTerm, BOp> >					
					{
						public:
							typedef BOp <typename LTerm::value_type, typename RTerm::value_type> operation;
							
							/// The element value type.
							typedef typename operation::result_type value_type;
							
							typedef binary_iterator <typename LTerm::const_iterator, typename RTerm::const_iterator, BOp> const_iterator;
							typedef binary_iterator <typename LTerm::const_iterator, typename RTerm::const_iterator, BOp> iterator;
							
							typedef value_type reference;
							
							binary_term (const LTerm& left_subterm, const RTerm& right_subterm):
								left_subterm_ (left_subterm), right_subterm_ (right_subterm)
								{
								}
								
							// Gets the element at index @a n.
							const value_type operator[] (std::size_t index) const	{ return operation () (left_subterm_ [index], right_subterm_ [index]); }
							
							/// Gets the number of elements.
							std::size_t size () const						{ return left_subterm_.size (); }
							
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (left_subterm_.begin (), right_subterm_.begin ());
								}

						private:
							const LTerm left_subterm_;
							const RTerm right_subterm_;
							
							template <typename Term, typename Enable1, typename Enable2, typename Enable3, typename Enable4> friend class chunker;
					};

				template <typename LTermIt, typename MTermIt, typename RTermIt, template <typename, typename, typename> class TOp> class ternary_iterator
					{
						public:
							typedef TOp <
								typename std::iterator_traits <LTermIt>::value_type,
								typename std::iterator_traits <MTermIt>::value_type,
								typename std::iterator_traits <RTermIt>::value_type> operation;
							
							// iterator category is the category with the least rank of the two expression's categories
							typedef typename iterator_ranker
								<typename std::iterator_traits <LTermIt>::iterator_category,
								typename std::iterator_traits <RTermIt>::iterator_category>::type iterator_category;
							
							typedef typename std::iterator_traits <LTermIt>::difference_type difference_type;
							typedef typename operation::result_type value_type;
							typedef const value_type* pointer;
							typedef value_type reference;
							
							INLINE ternary_iterator (const LTermIt& left_subterm_iter, const MTermIt& mid_subterm_iter, const RTermIt& right_subterm_iter):
								left_subterm_iter_ (left_subterm_iter), mid_subterm_iter_ (mid_subterm_iter), right_subterm_iter_ (right_subterm_iter)
								{
								}
								
							INLINE const value_type operator* () const
								{
									return operation () (*left_subterm_iter_, *mid_subterm_iter_, *right_subterm_iter_);
								}
								
							INLINE const value_type operator[] (typename std::iterator_traits <LTermIt>::difference_type n) const
								{
									return operation () (left_subterm_iter_ [n], mid_subterm_iter_ [n], right_subterm_iter_ [n]);
								}
								
							INLINE ternary_iterator& operator++ ()					{ ++left_subterm_iter_; ++mid_subterm_iter_; ++right_subterm_iter_; return *this; }
							INLINE ternary_iterator operator++ (int)				{ return ternary_iterator (left_subterm_iter_++, mid_subterm_iter_++, right_subterm_iter_++); }
							INLINE ternary_iterator& operator+= (difference_type n)	{ left_subterm_iter_ += n; mid_subterm_iter_ += n; right_subterm_iter_ += n; return *this; }
		
							INLINE ternary_iterator& operator-- ()					{ --left_subterm_iter_; --mid_subterm_iter_; --right_subterm_iter_; return *this; }
							INLINE ternary_iterator operator-- (int)				{ return ternary_iterator (left_subterm_iter_--, mid_subterm_iter_--, right_subterm_iter_--); }
							INLINE ternary_iterator& operator-= (difference_type n)	{ left_subterm_iter_ -= n; mid_subterm_iter_ -= n; right_subterm_iter_ -= n; return *this; }
								
							friend INLINE ternary_iterator operator+ (const ternary_iterator& left, difference_type right)
								{
									return ternary_iterator (left) += right;
								}

							friend INLINE ternary_iterator operator+ (difference_type left, const ternary_iterator& right)
								{
									return ternary_iterator (right) += left;
								}

							friend INLINE ternary_iterator operator- (const ternary_iterator& left, difference_type right)
								{
									return ternary_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const ternary_iterator& left, const ternary_iterator& right)
								{
									return left.left_subterm_iter_ - right.left_subterm_iter_;
								}
								
							friend INLINE bool operator== (const ternary_iterator& left, const ternary_iterator& right)
								{
									return left.left_subterm_iter_ == right.left_subterm_iter_;
								}
								
							friend INLINE bool operator!= (const ternary_iterator& left, const ternary_iterator& right)
								{
									return left.left_subterm_iter_ != right.left_subterm_iter_;
								}

							friend INLINE bool operator< (const ternary_iterator& left, const ternary_iterator& right)
								{
									return left.left_subterm_iter_ < right.left_subterm_iter_;
								}
														
						private:
							LTermIt left_subterm_iter_;
							MTermIt mid_subterm_iter_;
							RTermIt right_subterm_iter_;
					};

				template <typename LTerm, typename MTerm, typename RTerm, template <typename, typename, typename> class TOp, typename Enable2, typename Enable3, typename Enable4>
					class chunker <ternary_term <LTerm, MTerm, RTerm, TOp>,
						typename enable_if <exists <typename TOp <
							typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename MTerm::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename RTerm::const_chunk_iterator>::value_type>::result_type>::value>::type,
						Enable2,
						Enable3,
						Enable4>
					{
						public:
							typedef ternary_iterator <typename LTerm::const_chunk_iterator, typename MTerm::const_chunk_iterator, typename RTerm::const_chunk_iterator,
								TOp> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().left_subterm_.chunk_begin (),
										that ().mid_subterm_.chunk_begin (),
										that ().right_subterm_.chunk_begin ());
								}
								
						private:
							const ternary_term <LTerm, MTerm, RTerm, TOp>& that () const
								{
									return static_cast <const ternary_term <LTerm, MTerm, RTerm, TOp>&> (*this);
								}
					};

				/// Expression template ternary term.
				
				/// @internal
				/// This branch term applies a ternary functor to its subterms. A ternary term is const chunkable if each subterm's chunked types
				/// are the same and there is an appropriate ternary functor on the chunked type. Such terms are declared as partial specializations
				/// with additional members.
				///
				/// @param	LTerm	The left subterm type.
				/// @param	MTerm	The middle subterm type.
				/// @param	RTerm	The right subterm type.
				/// @param	TOp		The ternary functor.
				
				template <typename LTerm, typename MTerm, typename RTerm, template <typename, typename, typename> class TOp> class ternary_term:
					public term <typename TOp <typename LTerm::value_type, typename MTerm::value_type, typename RTerm::value_type>::result_type, ternary_term <LTerm, MTerm, RTerm, TOp> >,
					public chunker <ternary_term <LTerm, MTerm, RTerm, TOp> >					
					{
						public:
							typedef TOp <typename LTerm::value_type, typename MTerm::value_type, typename RTerm::value_type> operation;
							
							/// The element value type.
							typedef typename operation::result_type value_type;
							
							typedef ternary_iterator <typename LTerm::const_iterator, typename MTerm::const_iterator, typename RTerm::const_iterator, TOp> const_iterator;
							typedef ternary_iterator <typename LTerm::const_iterator, typename MTerm::const_iterator, typename RTerm::const_iterator, TOp> iterator;
							
							typedef value_type reference;
							
							ternary_term (const LTerm& left_subterm, const MTerm& mid_subterm, const RTerm& right_subterm):
								left_subterm_ (left_subterm), mid_subterm_ (mid_subterm), right_subterm_ (right_subterm)
								{
								}
								
							// Gets the element at index @a n.
							value_type operator[] (std::size_t index) const	{ return operation () (left_subterm_ [index], mid_subterm_ [index], right_subterm_ [index]); }
							
							/// Gets the number of elements.
							std::size_t size () const						{ return right_subterm_.size (); }
							
							/// Gets an iterator to the first element.
							const_iterator begin () const
								{
									return const_iterator (left_subterm_.begin (), mid_subterm_.begin (), right_subterm_.begin ());
								}

						private:
							const LTerm left_subterm_;
							const MTerm mid_subterm_;
							const RTerm right_subterm_;
							
							template <typename Term, typename Enable1, typename Enable2, typename Enable3, typename Enable4> friend class chunker;
					};
				
			}
	}

#endif
