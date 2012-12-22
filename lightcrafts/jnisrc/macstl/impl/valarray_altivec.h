/*
 *  valarray_altivec.h
 *  macstl
 *
 *  Created by Glen Low on May 14 2003.
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

#ifndef MACSTL_IMPL_VALARRAY_ALTIVEC_H
#define MACSTL_IMPL_VALARRAY_ALTIVEC_H

namespace stdext
	{
		namespace impl
			{
				template <> struct chunk <unsigned char>					{ typedef macstl::vec <unsigned char, 16> type; };
				template <> struct chunk <signed char>						{ typedef macstl::vec <signed char, 16> type; };
				template <> struct chunk <unsigned short>					{ typedef macstl::vec <unsigned short, 8> type; };
				template <> struct chunk <short>							{ typedef macstl::vec <short, 8> type; };
				template <> struct chunk <unsigned int>						{ typedef macstl::vec <unsigned int, 4> type; };
				template <> struct chunk <int>								{ typedef macstl::vec <int, 4> type; };
				template <> struct chunk <float>							{ typedef macstl::vec <float, 4> type; };
				template <> struct chunk <stdext::complex <float> >			{ typedef macstl::vec <stdext::complex <float>, 2> type; };
				template <typename T> struct chunk <macstl::boolean <T> >	{ typedef macstl::vec <macstl::boolean <T>, 16 / sizeof (T)> type; };
																																																			
				// optimize (v1 * v2) + v3 and v1 + (v2 * v3) for vec <short/float/complex> to use fused multiply add

				template <typename T> struct has_fma									{ enum { value = false }; };
				template <> struct has_fma <macstl::vec <float, 4> >					{ enum { value = true }; };
				template <> struct has_fma <macstl::vec <unsigned short, 8> >			{ enum { value = true }; };
				template <> struct has_fma <macstl::vec <short, 8> >					{ enum { value = true }; };
				template <> struct has_fma <macstl::vec <stdext::complex <float>, 2> >	{ enum { value = true }; };
				
				template <typename T> struct no_multiplies																			{ enum { value = true }; };
				template <typename LTermIt, typename RTermIt> struct no_multiplies <binary_term <LTermIt, RTermIt, multiplies> >	{ enum { value = false }; };

				template <typename LLTerm, typename LRTerm, typename RTerm, typename Enable3, typename Enable4>
					class chunker <binary_term <binary_term <LLTerm, LRTerm, multiplies>, RTerm, plus>,
						typename enable_if <exists <typename plus <
							typename std::iterator_traits <typename binary_term <LLTerm, LRTerm, multiplies>::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename RTerm::const_chunk_iterator>::value_type>::result_type>::value>::type,
						typename enable_if <has_fma <typename std::iterator_traits <typename RTerm::const_chunk_iterator>::value_type>::value>::type,
						Enable3,
						Enable4>
					{
						public:
							typedef ternary_iterator <typename LLTerm::const_chunk_iterator, typename LRTerm::const_chunk_iterator, typename RTerm::const_chunk_iterator,
								multiplies_plus> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().left_subterm_.left_subterm_.chunk_begin (),
										that ().left_subterm_.right_subterm_.chunk_begin (),
										that ().right_subterm_.chunk_begin ());
								}
								
						private:
							const binary_term <binary_term <LLTerm, LRTerm, multiplies>, RTerm, plus>& that () const
								{
									return static_cast <const binary_term <binary_term <LLTerm, LRTerm, multiplies>, RTerm, plus>&> (*this);
								}
					};

				template <typename LTerm, typename RLTerm, typename RRTerm, typename Enable4>
					class chunker <binary_term <LTerm, binary_term <RLTerm, RRTerm, multiplies>, plus>,
						typename enable_if <exists <typename plus <
							typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename binary_term <RLTerm, RRTerm, multiplies>::const_chunk_iterator>::value_type>::result_type>::value>::type,
						typename enable_if <has_fma <typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type>::value>::type,
						typename enable_if <no_multiplies <LTerm>::value>::type,
						Enable4>
					{
						public:
							typedef ternary_iterator <typename RLTerm::const_chunk_iterator, typename RRTerm::const_chunk_iterator, typename LTerm::const_chunk_iterator,
								multiplies_plus> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().right_subterm_.left_subterm_.chunk_begin (),
										that ().right_subterm_.right_subterm_.chunk_begin (),
										that ().left_subterm_.chunk_begin ());
								}
								
						private:
							const binary_term <LTerm, binary_term <RLTerm, RRTerm, multiplies>, plus>& that () const
								{
									return static_cast <const binary_term <LTerm, binary_term <RLTerm, RRTerm, multiplies>, plus>&> (*this);
								}
					};
				
				template <typename LTermIt, typename RTermIt, typename Size, typename T, typename U>
					struct accumulate_n_dispatch <binary_iterator <LTermIt, RTermIt, multiplies>, Size, T, plus <U>,
					typename enable_if <is_same <typename std::iterator_traits <binary_iterator <LTermIt, RTermIt, multiplies> >::iterator_category, std::random_access_iterator_tag>::value>::type,
					typename enable_if <has_fma <T>::value>::type>
					{
						static T call (binary_iterator <LTermIt, RTermIt, multiplies> first, Size n, T init, plus <U>)
							{
								for (std::size_t index = 0; index != n; ++index)
									init = fma (first.left_subterm_iter_ [index], first.right_subterm_iter_ [index], init);
								return init;
							
							}
					};
					
				// optimize v1 - (v2 * v3) to use altivec negative multiply subtract

				template <typename T> struct has_nmsub									{ enum { value = false }; };
				template <> struct has_nmsub <macstl::vec <float, 4> >					{ enum { value = true }; };

				template <typename LTerm, typename RLTerm, typename RRTerm, typename Enable3, typename Enable4>
					class chunker <binary_term <LTerm, binary_term <RLTerm, RRTerm, multiplies>, minus>,
						typename enable_if <exists <
							typename minus <typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename binary_term <RLTerm, RRTerm, multiplies>::const_chunk_iterator>::value_type>::result_type>::value>::type,
						typename enable_if <has_nmsub <typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type>::value>::type,
						Enable3,
						Enable4>
					{
						public:
							typedef ternary_iterator <typename RLTerm::const_chunk_iterator, typename RRTerm::const_chunk_iterator, typename LTerm::const_chunk_iterator,
								macstl::altivec::nmsub_function> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().right_subterm_.left_subterm_.chunk_begin (),
										that ().right_subterm_.right_subterm_.chunk_begin (),
										that ().left_subterm_.chunk_begin ());
								}
								
						private:
							const binary_term <LTerm, binary_term <RLTerm, RRTerm, multiplies>, minus>& that () const
								{
									return static_cast <const binary_term <LTerm, binary_term <RLTerm, RRTerm, multiplies>, minus>&> (*this);
								}
					};
					
				// optimize v1 / sqrt (v2) to use v1 * rsqrt (v2)
				
				template <typename T> struct has_rsqrt									{ enum { value = false }; };
				template <> struct has_rsqrt <macstl::vec <float, 4> >					{ enum { value = true }; };

				template <typename LTerm, typename RTerm, typename Enable3, typename Enable4>
					class chunker <binary_term <LTerm, unary_term <RTerm, square_root>, divides>,
						typename enable_if <exists <
							typename divides <typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename unary_term <RTerm, square_root>::const_chunk_iterator>::value_type>::result_type>::value>::type,
						typename enable_if <has_rsqrt <typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type>::value>::type,
						Enable3,
						Enable4>
					{
						public:
							typedef binary_iterator <typename LTerm::const_chunk_iterator, unary_iterator <typename RTerm::const_chunk_iterator, reciprocal_square_root>, multiplies>
								const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().left_subterm_.chunk_begin (),
										unary_iterator <typename RTerm::const_chunk_iterator, reciprocal_square_root> (that ().right_subterm_.subterm_.chunk_begin ()));
								}
								
						private:
							const binary_term <LTerm, unary_term <RTerm, square_root>, divides>& that () const
								{
									return static_cast <const binary_term <LTerm, unary_term <RTerm, square_root>, divides>&> (*this);
								}
					};

				// optimize (v1 / sqrt (v2)) + v3 to use fma (v1, rsqrt (v2), v3)
				
				template <typename LLTerm, typename LRTerm, typename RTerm, typename Enable3, typename Enable4>
					class chunker <binary_term <binary_term <LLTerm, unary_term <LRTerm, square_root>, divides>, RTerm, plus>,
						typename enable_if <exists <typename plus <
							typename std::iterator_traits <typename binary_term <LLTerm, unary_term <LRTerm, square_root>, divides>::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename RTerm::const_chunk_iterator>::value_type>::result_type>::value>::type,
						typename enable_if <has_rsqrt <typename std::iterator_traits <typename LLTerm::const_chunk_iterator>::value_type>::value>::type,
						Enable3,
						Enable4>
					{
						public:
							typedef ternary_iterator <
								typename LLTerm::const_chunk_iterator, unary_iterator <typename LRTerm::const_chunk_iterator, reciprocal_square_root>, typename RTerm::const_chunk_iterator,
								multiplies_plus> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().left_subterm_.left_subterm_.chunk_begin (),
										unary_iterator <typename RTerm::const_chunk_iterator, reciprocal_square_root> (that ().left_subterm_.right_subterm_.subterm_.chunk_begin ()),
										that ().right_subterm_.chunk_begin ());
								}
								
						private:
							const binary_term <binary_term <LLTerm, unary_term <LRTerm, square_root>, divides>, RTerm, plus>& that () const
								{
									return static_cast <const binary_term <binary_term <LLTerm, unary_term <LRTerm, square_root>, divides>, RTerm, plus>&> (*this);
								}
					};

					
				// optimize v1 & ~v2 to use altivec and complement

				template <typename LTerm, typename RTerm, typename Enable2, typename Enable3, typename Enable4>
					class chunker <binary_term <LTerm, unary_term <RTerm, bitwise_not>, bitwise_and>,
						typename enable_if <exists <typename bitwise_and <
							typename std::iterator_traits <typename LTerm::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename unary_term <RTerm, bitwise_not>::const_chunk_iterator>::value_type>::result_type>::value>::type,
						Enable2,
						Enable3,
						Enable4>
					{
						public:
							typedef binary_iterator <typename LTerm::const_chunk_iterator, typename RTerm::const_chunk_iterator, macstl::altivec::andc_function> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().left_subterm_.chunk_begin (),
										that ().right_subterm_.subterm_.chunk_begin ());
								}
								
						private:
							const binary_term <LTerm, unary_term <RTerm, bitwise_not>, bitwise_and>& that () const
								{
									return static_cast <const binary_term <LTerm, unary_term <RTerm, bitwise_not>, bitwise_and>&> (*this);
								}
					};

				// optimize ~(v1 | v2) to use altivec complement or

				template <typename LTerm, typename RTerm, typename Enable3, typename Enable4>
					class chunker <unary_term <binary_term <LTerm, RTerm, bitwise_or>, bitwise_not>,
						typename enable_if <exists <typename binary_term <LTerm, RTerm, bitwise_or>::const_chunk_iterator>::value>::type,
						typename enable_if <exists <typename bitwise_not <
							typename std::iterator_traits <typename binary_term <LTerm, RTerm, bitwise_or>::const_chunk_iterator>::value_type>::result_type>::value>::type,
						Enable3,
						Enable4>
					{
						public:
							typedef binary_iterator <typename LTerm::const_chunk_iterator, typename RTerm::const_chunk_iterator, macstl::altivec::nor_function> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const
								{
									return const_chunk_iterator (
										that ().left_subterm_.chunk_begin (),
										that ().right_subterm_.chunk_begin ());
								}
								
						private:
							const unary_term <binary_term <LTerm, RTerm, bitwise_or>, bitwise_not>& that () const
								{
									return static_cast <const unary_term <binary_term <LTerm, RTerm, bitwise_or>, bitwise_not>&> (*this);
								}
					};
	
				// optimizations for expressions of the form (v1 cmp v2).min () to use altivec predicates
				
				template <template <typename, typename> class Func, template <typename, typename> class BOp, typename LTermIt, typename RTermIt> struct predicator;
				
				template <typename LTermIt, typename RTermIt> struct predicator <minimum, equal_to, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::all_eq_function> type;
					};

				template <typename LTermIt, typename RTermIt> struct predicator <minimum, not_equal_to, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::all_ne_function> type;
					};

				template <typename LTermIt, typename RTermIt> struct predicator <minimum, less, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::all_lt_function> type;
					};
					
				template <typename LTermIt, typename RTermIt> struct predicator <minimum, greater, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::all_gt_function> type;
					};
					
				template <typename LTermIt, typename RTermIt> struct predicator <minimum, less_equal, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::all_le_function> type;
					};
					
				template <typename LTermIt, typename RTermIt> struct predicator <minimum, greater_equal, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::all_ge_function> type;
					};

				template <typename LTermIt, typename RTermIt> struct predicator <maximum, equal_to, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::any_eq_function> type;
					};

				template <typename LTermIt, typename RTermIt> struct predicator <maximum, not_equal_to, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::any_ne_function> type;
					};

				template <typename LTermIt, typename RTermIt> struct predicator <maximum, less, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::any_lt_function> type;
					};
					
				template <typename LTermIt, typename RTermIt> struct predicator <maximum, greater, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::any_gt_function> type;
					};
					
				template <typename LTermIt, typename RTermIt> struct predicator <maximum, less_equal, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::any_le_function> type;
					};
					
				template <typename LTermIt, typename RTermIt> struct predicator <maximum, greater_equal, LTermIt, RTermIt>
					{
						typedef binary_iterator <LTermIt, RTermIt, macstl::altivec::any_ge_function> type;
					};

				template <template <typename, typename> class Func, template <typename, typename> class BOp, typename LTerm, typename RTerm>
					struct accumulate_array_dispatch <Func, binary_term <LTerm, RTerm, BOp>,
					typename enable_if <
						exists <typename stdext::accumulator <Func <
							typename std::iterator_traits <typename binary_term <LTerm, RTerm, BOp>::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename binary_term <LTerm, RTerm, BOp>::const_chunk_iterator>::value_type> >::result_type>::value>::type,
					typename enable_if <exists <typename predicator <Func, BOp, typename LTerm::const_chunk_iterator, typename RTerm::const_chunk_iterator>::type>::value>::type>
					{
						static bool tail (const binary_term <LTerm, RTerm, BOp>& expr, bool partial)
							{
								std::size_t size = expr.size ();
								std::size_t tailed = size % std::iterator_traits <typename binary_term <LTerm, RTerm, BOp>::const_chunk_iterator>::value_type::length;
				
								// skip over all the chunked parts
								typename binary_term <LTerm, RTerm, BOp>::const_iterator iter = expr.begin ();
								std::advance (iter, size - tailed);
								
								typedef Func <bool, bool> function;
								return stdext::accumulate_n (iter, tailed, partial, function ());
							}
							
						static bool call (const binary_term <LTerm, RTerm, BOp>& expr)
							{
								typedef typename predicator <Func, BOp, typename LTerm::const_chunk_iterator, typename RTerm::const_chunk_iterator>::type predicator;
								typedef typename std::iterator_traits <typename binary_term <LTerm, RTerm, BOp>::const_chunk_iterator>::value_type chunk_type;
							
								typename binary_term <LTerm, RTerm, BOp>::const_chunk_iterator iter = expr.chunk_begin ();
								predicator pred (iter.left_subterm_iter_, iter.right_subterm_iter_);
								
								bool init = *pred;
								++pred;
								
								typedef Func <bool, bool> function;
								return tail (expr, stdext::accumulate_n (pred, expr.size () / chunk_type::length - 1, init, function ()));
							}

					};

				// optimizations for expressions of the form v1 [slice]
		
				template <typename TermIt> class slice4_iterator
					{
						public:
							typedef typename std::iterator_traits <TermIt>::iterator_category iterator_category;
							typedef typename std::iterator_traits <TermIt>::value_type value_type;
							typedef typename std::iterator_traits <TermIt>::difference_type difference_type;
							typedef typename std::iterator_traits <TermIt>::pointer pointer;
							typedef typename std::iterator_traits <TermIt>::reference reference;
							
							INLINE slice4_iterator (const TermIt& exprit, std::size_t start, std::size_t stride): termit_ (exprit), stride_ (stride),
								stride0_ (start / 4),
								stride1_ ((start + 1 * stride) / 4),
								stride2_ ((start + 2 * stride) / 4),
								stride3_ ((start + 3 * stride) / 4),
								splice_v3_v1_ (macstl::altivec::sld <12> (macstl::altivec::lvsr (12 - (start % 4 * 4), (int*) NULL), macstl::altivec::lvsl ((start + 2 * stride) % 4 * 4, (int*) NULL))),
								splice_v4_v2_ (macstl::altivec::sld <12> (macstl::altivec::lvsr (12 - ((start + stride) % 4 * 4), (int*) NULL), macstl::altivec::lvsl ((start + 3 * stride) % 4 * 4, (int*) NULL)))
								{
								}
									
							INLINE const value_type operator* () const
								{
									// the neat thing about this is the compiler generally hoists all the invariants
									// out of any loop, so all the start_ + n * stride_ stuff gets evaluated outside the loop!!
									TermIt iter1 = termit_;
									TermIt iter2 = termit_;
									TermIt iter3 = termit_;
									TermIt iter4 = termit_;
									std::advance (iter1, stride0_);
									std::advance (iter2, stride1_);
									std::advance (iter3, stride2_);
									std::advance (iter4, stride3_);
									return stuff4 (
										*iter1,
										*iter2,
										*iter3,
										*iter4);
								}
								
							INLINE const value_type operator[] (difference_type n) const
								{
									const TermIt iter = termit_ + n * stride_;
									return stuff4 (
										iter [stride0_],
										iter [stride1_],
										iter [stride2_],
										iter [stride3_]);
								}
							
							INLINE slice4_iterator& operator++ ()					{ std::advance (termit_, stride_); return *this; }
							INLINE slice4_iterator operator++ (int)					{ slice4_iterator copy (*this); return ++copy; }
							INLINE slice4_iterator& operator+= (difference_type n)	{ termit_ += n * stride_; return *this; }
			
							INLINE slice4_iterator& operator-- ()					{ std::advance (termit_, -stride_); return *this; }
							INLINE slice4_iterator operator-- (int)					{ slice4_iterator copy (*this); return --copy; }
							INLINE slice4_iterator& operator-= (difference_type n)	{ termit_ -= n * stride_; return *this; }
								
							friend INLINE slice4_iterator operator+ (const slice4_iterator& left, difference_type right)
								{
									return slice4_iterator (left) += right;
								}
			
							friend INLINE slice4_iterator operator+ (difference_type left, const slice4_iterator& right)
								{
									return slice4_iterator (right) += left;
								}
			
							friend INLINE slice4_iterator operator- (const slice4_iterator& left, difference_type right)
								{
									return slice4_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const slice4_iterator& left, const slice4_iterator& right)
								{
									return (left.termit_ - right.termit_) / left.stride_;
								}
								
							friend INLINE bool operator== (const slice4_iterator& left, const slice4_iterator& right)
								{
									return left.termit_ == right.termit_;
								}
								
							friend INLINE bool operator!= (const slice4_iterator& left, const slice4_iterator& right)
								{
									return left.termit_ != right.termit_;
								}
		
							friend INLINE bool operator< (const slice4_iterator& left, const slice4_iterator& right)
								{
									return left.termit_ < right.termit_;
								}
								
						private:
							TermIt termit_;
							
							const std::size_t stride_;
							
							const std::size_t stride0_;
							const std::size_t stride1_;
							const std::size_t stride2_;
							const std::size_t stride3_;
							
							const macstl::vec <unsigned char, 16> splice_v3_v1_;
							const macstl::vec <unsigned char, 16> splice_v4_v2_;
								
							INLINE const value_type stuff4 (const value_type& v1, const value_type& v2, const value_type& v3, const value_type& v4) const
								{
							 		using namespace macstl;
									
									return
										altivec::mergeh (
											altivec::perm (v3, v1, splice_v3_v1_),
											altivec::perm (v4, v2, splice_v4_v2_));
								}
					};
					
				template <typename Term> class slice_term_base;
					
				template <typename T> struct is_vec_4							{ enum { value = false }; };
				template <typename T> struct is_vec_4 <macstl::vec <T, 4> >		{ enum { value = true }; };

				template <typename Term, typename Enable2, typename Enable3, typename Enable4> class chunker <slice_term <Term>,
					typename enable_if <is_vec_4 <typename std::iterator_traits <typename Term::const_chunk_iterator>::value_type>::value>::type,
					Enable2,
					Enable3,
					Enable4>
					{														
						public:
							typedef slice4_iterator <typename Term::const_chunk_iterator> const_chunk_iterator;
							
							const_chunk_iterator chunk_begin () const		
								{														
									return const_chunk_iterator (that ().subterm_.chunk_begin (), that ().slice_.start (), that ().slice_.stride ());
								}
								
						private:
							const slice_term <Term>& that () const
								{
									return static_cast <const slice_term <Term>&> (*this);
								}
					};
			}
	}
	

#endif
