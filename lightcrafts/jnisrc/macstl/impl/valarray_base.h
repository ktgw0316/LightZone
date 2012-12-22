/*
 *  valarray_base.h
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

#ifndef MACSTL_IMPL_VALARRAY_BASE_H
#define MACSTL_IMPL_VALARRAY_BASE_H

namespace stdext
	{
		namespace impl
			{
				#define DEFINE_VALARRAY_CASSIGN_FUNCTION(FUNC,OPER)												\
				template <typename Term2> Term& FUNC (const term <T, Term2>& rhs)								\
					{																							\
						impl::copy_array (that (), binary_term <Term, Term2, OPER> (that (), rhs.that ()));		\
						return that ();																			\
					}																							\
																												\
				Term& FUNC (const T& rhs)																		\
					{																							\
						impl::copy_array (that (), binary_term <Term, literal_term <Term>, OPER> (that (),		\
							literal_term <Term> (rhs, that ())));												\
						return that ();																			\
					}
					
				/// Expression template term.
				
				/// @internal
				/// An expression template represents a valarray expression parsed at compile time; each term
				/// is then a node of the parse tree. This class defines all the member and free functions of valarray, except for index and size,
				/// and limits templated operator overloads to its subclasses.
				///
				/// @param T The element type, see stdext::valarray.
				/// @param Term The actual term, which should be the subclass.
				///
				/// @note	A slightly different formulation from the classic technique. The term refers to its wrapped expression using CRTP (curiously
				///			recursive template pattern): this saves on space and minimizes template nesting.
				
				template <typename T, typename Term> class term
					{
						public:							
							/// @name Summarizers
							
							//@{
							
							/// Sums all the elements.
							T sum () const	{ return accumulate_array <stdext::plus> (that ()); }
							
							/// Gets the minimum element.
							T min () const	{ return accumulate_array <stdext::minimum> (that ()); }
							
							/// Gets the maximum element.
							T max () const	{ return accumulate_array <stdext::maximum> (that ()); }
							
							//@}

							/// @name Subsetters
							/// Each returns a new term with elements of type T.
							
							//@{
							
							/// Selects a BLAS-like slice @a sub out of the term.
							const slice_term <Term> operator[] (const slice& sub) const
								{
									return slice_term <Term> (that (), sub);
								}
								
							/// Selects a generalized slice @a sub out of the term.
							const gslice_term <Term> operator[] (const gslice& sub) const
								{
									return gslice_term <Term> (that (), sub);
								}
							
							/// Selects the elements masked by @a sub out of the term.
							template <typename BTerm> const mask_term <Term, BTerm>
								operator[] (const term <bool, BTerm>& sub) const
								{
									return mask_term <Term, BTerm> (that (), sub.that ());
								}
		
							/// Selects the elements indirected by @a sub out of the term.
							template <typename InTerm> const indirect_term <Term, InTerm>
								operator[] (const term <std::size_t, InTerm>& sub) const
								{
									return indirect_term <Term, InTerm> (that (), sub.that ());
								}

							/// Selects a BLAS-like slice @a sub out of the term.
							slice_term <Term> operator[] (const slice& sub)
								{
									return slice_term <Term> (that (), sub);
								}
								
							/// Selects a generalized slice @a sub out of the term.
							gslice_term <Term> operator[] (const gslice& sub)
								{
									return gslice_term <Term> (that (), sub);
								}
							
							/// Selects the elements masked by @a sub out of the term.
							template <typename BTerm> mask_term <Term, BTerm>
								operator[] (const term <bool, BTerm>& sub)
								{
									return mask_term <Term, BTerm> (that (), sub.that ());
								}
		
							/// Selects the elements indirected by @a sub out of the term.
							template <typename InTerm> indirect_term <Term, InTerm>
								operator[] (const term <std::size_t, InTerm>& sub)
								{
									return indirect_term <Term, InTerm> (that (), sub.that ());
								}
							
							//@}
							
							/// @name Shifters
							/// Each returns a new term with elements of type T.
							
							//@{
							
							/// Shifts each element left n places, then fills shifted places with zero.
							shift_term <Term> shift (int n) const		{ return shift_term <Term> (that (), n); }
							
							/// Shifts circularly each element left n places.
							cshift_term <Term> cshift (int n) const		{ return cshift_term <Term> (that (), n); }
							
							//@}
							
							/// @name Appliers
							/// Each returns a new term with elements of type T.
							
							//@{

							/// Applies the function func to each element.
							template <typename Fn> apply_term <Term, Fn> apply (Fn func) const
								{
									return apply_term <Term, Fn> (that (), func);
								}
							
							//@}
							
							/// @name Computed Assignments
							/// You can also use a value of type T for rhs.
							
							//@{
																				
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator*=, multiplies)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator/=, divides)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator%=, modulus)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator+=, plus)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator-=, minus)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator^=, bitwise_xor)			
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator&=, bitwise_and)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator|=, bitwise_or)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator<<=, shift_left)
							DEFINE_VALARRAY_CASSIGN_FUNCTION (operator>>=, shift_right)
							
							//@}
														
							/// @name Reference
							
							//@{
							
							/// Gets the wrapped term.
							const Term& that () const
								{
									return static_cast <const Term&> (*this);
								}
							
							/// Gets the wrapped term
							Term& that ()
								{
									return static_cast <Term&> (*this);
								}
								
							//@}
					
					};

				#define DEFINE_VALARRAY_UNARY_FUNCTION(FUNC,OPER)												\
				template <typename T, typename Term>															\
					inline const typename impl::enable_if <														\
					exists <typename OPER <T>::result_type>::value, unary_term <Term, OPER> >::type FUNC		\
					(const term <T, Term>& lhs)																	\
					{																							\
						return unary_term <Term, OPER> (lhs.that ());											\
					}

				#define DEFINE_VALARRAY_BINARY_FUNCTION(FUNC, OPER)												\
				template <typename T1, typename T2, typename LTerm, typename RTerm>								\
					inline const typename impl::enable_if <exists <typename OPER <T1, T2>::result_type>::value,	\
					binary_term <LTerm, RTerm, OPER> >::type FUNC												\
					(const term <T1, LTerm>& lhs, const term <T2, RTerm>& rhs)									\
					{																							\
						return binary_term <LTerm, RTerm, OPER> (lhs.that (), rhs.that ());						\
					}																							\
																												\
				template <typename T1, typename T2, typename RTerm>												\
					inline const typename impl::enable_if <exists <typename OPER <T1, T2>::result_type>::value,	\
					binary_term <literal_term <RTerm>, RTerm, OPER> >::type FUNC								\
					(const T1& lhs, const term <T2, RTerm>& rhs)												\
					{																							\
						return binary_term <literal_term <RTerm>, RTerm, OPER>									\
							(literal_term <RTerm> (lhs, rhs.that ()), rhs.that ());								\
					}																							\
																												\
				template <typename T1, typename T2, typename LTerm>												\
					inline const typename impl::enable_if <exists <typename OPER <T1, T2>::result_type>::value,	\
					binary_term <LTerm, literal_term <LTerm>, OPER> >::type FUNC								\
					(const term <T1, LTerm>& lhs, const T2& rhs)												\
					{																							\
						return binary_term <LTerm, literal_term <LTerm>, OPER>									\
							(lhs.that (), literal_term <LTerm> (rhs, lhs.that ()));								\
					}														
					
				#define DEFINE_VALARRAY_TERNARY_FUNCTION(FUNC, OPER)														\
				template <typename T1, typename T2, typename T3, typename LTerm, typename MTerm, typename RTerm>			\
					inline const ternary_term <LTerm, MTerm, RTerm, OPER> FUNC												\
					(const term <T1, LTerm>& lhs, const term <T2, MTerm>& mhs, const term <T3, RTerm>& rhs)					\
					{																										\
						return ternary_term <LTerm, MTerm, RTerm, OPER> (lhs.that (), mhs.that (), rhs.that ());			\
					}
					
				/// @name Unary Arithmetic
				/// @relates term
				/// Each returns a new term with elements of type T.
				
				//@{
				
				DEFINE_VALARRAY_UNARY_FUNCTION (operator+, identity)
				DEFINE_VALARRAY_UNARY_FUNCTION (operator-, negate)
				DEFINE_VALARRAY_UNARY_FUNCTION (operator~, bitwise_not)
				
				//@}
				
				/// @name Unary Logic
				/// @relates term
				/// Each returns a new term with bool elements.
				
				//@{
				
				DEFINE_VALARRAY_UNARY_FUNCTION (operator!, logical_not)
				
				//@}

				/// @name Binary Arithmetic
				/// @relates term
				/// Each returns a new term with elements of type T. You can also use a value of type T for either lhs or rhs.

				//@{
				
				DEFINE_VALARRAY_BINARY_FUNCTION (operator*, multiplies)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator/, divides)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator%, modulus)            
				DEFINE_VALARRAY_BINARY_FUNCTION (operator+, plus)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator-, minus)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator^, bitwise_xor)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator&, bitwise_and)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator|, bitwise_or)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator<<, shift_left)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator>>, shift_right)
				
				//@}
				
				/// @name Binary Logic
				///	@relates term
				/// Each returns a new term with bool elements. You can also use a value of type T for either lhs or rhs.
				
				//@{
				
				DEFINE_VALARRAY_BINARY_FUNCTION (operator==, equal_to)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator!=, not_equal_to)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator<, less)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator>, greater)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator<=, less_equal)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator>=, greater_equal)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator&&, logical_and)
				DEFINE_VALARRAY_BINARY_FUNCTION (operator||, logical_or)
				
				//@}
				
				/// @name Transcendentals
				///	@relates term
				/// Each returns a new term with elements of type T. You can also use a value of type T for either lhs or rhs.
				
				//@{
				
				DEFINE_VALARRAY_UNARY_FUNCTION (abs, absolute)
				DEFINE_VALARRAY_UNARY_FUNCTION (acos, arc_cosine)
				DEFINE_VALARRAY_UNARY_FUNCTION (asin, arc_sine)
				DEFINE_VALARRAY_UNARY_FUNCTION (atan, arc_tangent)
				DEFINE_VALARRAY_BINARY_FUNCTION (atan2, arc_tangent2)
				DEFINE_VALARRAY_UNARY_FUNCTION (conj, conjugate)
				DEFINE_VALARRAY_UNARY_FUNCTION (cos, cosine)
				DEFINE_VALARRAY_UNARY_FUNCTION (cosh, hyperbolic_cosine)
				DEFINE_VALARRAY_UNARY_FUNCTION (exp, exponent)
				DEFINE_VALARRAY_UNARY_FUNCTION (log, logarithm)
				DEFINE_VALARRAY_UNARY_FUNCTION (log10, logarithm10)
				DEFINE_VALARRAY_BINARY_FUNCTION (max, maximum)
				DEFINE_VALARRAY_BINARY_FUNCTION (min, minimum)
				DEFINE_VALARRAY_BINARY_FUNCTION (mulhi, multiplies_high)
				DEFINE_VALARRAY_BINARY_FUNCTION (pow, power)
				DEFINE_VALARRAY_UNARY_FUNCTION (rsqrt, reciprocal_square_root)
				DEFINE_VALARRAY_TERNARY_FUNCTION (select, selection)
				DEFINE_VALARRAY_UNARY_FUNCTION (sin, sine)
				DEFINE_VALARRAY_UNARY_FUNCTION (sinh, hyperbolic_sine)
				DEFINE_VALARRAY_UNARY_FUNCTION (sqrt, square_root)
				DEFINE_VALARRAY_UNARY_FUNCTION (tan, tangent)
				DEFINE_VALARRAY_UNARY_FUNCTION (tanh, hyperbolic_tangent)

				//@}
					
				template <typename Term,
					typename Enable1 = void, typename Enable2 = void, typename Enable3 = void, typename Enable4 = void> class chunker
					{
					};
					
				/// Expression template array term.
				
				/// @internal
				/// This leaf term stores elements as a sequence.
				///
				/// @param	T		The element type, see stdext::valarray.
				/// @param	Enable	If void, enables a particular template specialization.
				///
				/// @note	This object has an implicit copy constructor and intentionally allows slicing:
				///			subclasses use this term as a reference to their data.
				
				template <typename T, typename Enable = void> class array_term:
					public term <T, array_term <T> >, public chunker <array_term <T> >
					{
						public:
							/// The element type, see stdext::valarray. */
							typedef T value_type;
							typedef T value_data;
							
							/// The iterator that allows element access.
							typedef const T* const_iterator;
							
							/// The iterator that allows element access and change.
							typedef T* iterator;
							
							/// The element reference.
							typedef T& reference;
							
							/// Gets the number of elements.
							std::size_t size () const					{ return size_; }
							
							using term <T, array_term <T> >::operator[];

							/// Gets the element at index n.
							const value_type& operator[] (std::size_t n) const	{ return values_ [n]; }
							
							/// Gets the element at index n
							value_type& operator[] (std::size_t n)		{ return values_ [n]; }

							/// Gets an iterator to the first element.
							const_iterator begin () const			{ return values_; }
							
							/// Gets an iterator to the first element.
							iterator begin ()						{ return values_; }

							/// Gets an iterator past the last element.
							const_iterator end () const				{ return values_ + size_; }
							
							/// Gets an iterator past the last element
							iterator end ()							{ return values_ + size_; }

						protected:
							value_data* values_;
							std::size_t size_;
							
							array_term (value_data* values, std::size_t size): values_ (values), size_ (size)
								{
								}
		
							void swap (array_term& other)
								{
									std::swap (values_, other.values_);
									std::swap (size_, other.size_);
								}
								
							template <typename Term2, typename Enable1, typename Enable2, typename Enable3, typename Enable4> friend class chunker;
					};

				template <typename T> class array_iterator
					{
						public:
							typedef typename std::iterator_traits <T*>::value_type value_type;
							typedef typename if_c <is_const <T>::value, const typename T::data_type, typename T::data_type>::type value_data;
							
							typedef std::random_access_iterator_tag iterator_category;
							typedef std::ptrdiff_t difference_type;
							typedef typename std::iterator_traits <T*>::pointer pointer;
							typedef typename std::iterator_traits <T*>::reference reference;
														
							explicit INLINE array_iterator (value_data* ptr): ptr_ (ptr)
								{
								}
								
							INLINE reference operator* () const
								{
									// reinterpret_cast is kosher by aliasing rules, since value_type is expected to be a struct containing value_data_type e.g. boolean <T>
									return reinterpret_cast <reference> (*ptr_);
								}
								
							INLINE reference operator[] (difference_type n) const
								{
									// reinterpret_cast is kosher by aliasing rules, since value_type is expected to be a struct containing value_data_type e.g. boolean <T>
									return reinterpret_cast <reference> (ptr_ [n]);
								}
								
							INLINE array_iterator& operator++ ()					{ ++ptr_; return *this; }
							INLINE array_iterator operator++ (int)					{ return ++array_iterator (*this); }
							INLINE array_iterator& operator+= (difference_type n)	{ ++ptr_ += n; return *this; }
		
							INLINE array_iterator& operator-- ()					{ --ptr_; return *this; }
							INLINE array_iterator operator-- (int)					{ return --array_iterator (*this); }
							INLINE array_iterator& operator-= (difference_type n)	{ ptr_ -= n; return *this; }
								
							friend INLINE array_iterator operator+ (const array_iterator& left, difference_type right)
								{
									return array_iterator (left) += right;
								}
		
							friend INLINE array_iterator operator+ (difference_type left, const array_iterator& right)
								{
									return array_iterator (right) += left;
								}
		
							friend INLINE array_iterator operator- (const array_iterator& left, difference_type right)
								{
									return array_iterator (left) -= right;
								}
							
							friend INLINE difference_type operator- (const array_iterator& left, const array_iterator& right)
								{
									return left.ptr_ - right.ptr_;
								}
								
							friend INLINE bool operator== (const array_iterator& left, const array_iterator& right)
								{
									return left.ptr_ == right.ptr_;
								}
								
							friend INLINE bool operator!= (const array_iterator& left, const array_iterator& right)
								{
									return left.ptr_ != right.ptr_;
								}
		
							friend INLINE bool operator< (const array_iterator& left, const array_iterator& right)
								{
									return left.ptr_ < right.ptr_;
								}
								
						private:
							value_data* ptr_;
					};

				template <typename T> class array_term <T, typename enable_if <exists <typename T::data_type>::value>::type>:
					public term <T, array_term <T> >, public chunker <array_term <T> >
					{
						public:
							/// The element type, see stdext::valarray. */
							typedef T value_type;
							typedef typename T::data_type value_data;
							
							/// The iterator that allows element access.
							typedef array_iterator <const T> const_iterator;
							
							/// The iterator that allows element access and change.
							typedef array_iterator <T> iterator;
							
							/// The element reference.
							typedef T& reference;
							
							/// Gets the number of elements.
							std::size_t size () const					{ return size_; }
							
							using term <T, array_term <T> >::operator[];

							/// Gets the element at index n.
							const value_type& operator[] (std::size_t n) const
								{
									// reinterpret_cast is kosher by aliasing rules, since value_type is expected to be a struct containing value_data_type e.g. boolean <T>
									return reinterpret_cast <const value_type&> (values_ [n]);
								}
							
							/// Gets the element at index n
							value_type& operator[] (std::size_t n)
								{
									// reinterpret_cast is kosher by aliasing rules, since value_type is expected to be a struct containing value_data_type e.g. boolean <T>
									return reinterpret_cast <value_type&> (values_ [n]);
								}

							/// Gets an iterator to the first element.
							const_iterator begin () const			{ return const_iterator (values_); }
							
							/// Gets an iterator to the first element.
							iterator begin ()						{ return iterator (values_); }

							/// Gets an iterator past the last element.
							const_iterator end () const				{ return const_iterator (values_ + size_); }
							
							/// Gets an iterator past the last element
							iterator end ()							{ return iterator (values_ + size_); }

						protected:
							value_data* values_;
							std::size_t size_;
							
							array_term (value_data* values, std::size_t size): values_ (values), size_ (size)
								{
								}
		
							void swap (array_term& other)
								{
									std::swap (values_, other.values_);
									std::swap (size_, other.size_);
								}
								
							template <typename Term2, typename Enable1, typename Enable2, typename Enable3, typename Enable4> friend class chunker;
					};
					
			}
	}
	
#endif

