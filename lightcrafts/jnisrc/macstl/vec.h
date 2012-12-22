/*
 *  vec.h
 *  macstl
 *
 *  Created by Glen Low on Feb 07 2004.
 *
 *  Copyright (c) 2004-2005 Pixelglow Software, all rights reserved.
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

#ifndef MACSTL_VEC_H
#define MACSTL_VEC_H

#include "impl/config.h"

#include <sstream>

#include "functional.h"
#include "impl/data.h"
#include "impl/meta.h"

#include "boolean.h"
#include "complex.h"
#include "pixel.h"

namespace macstl
	{
		#ifdef DOXYGEN
		
		/// SIMD type.
		
		/// This platform-independent SIMD type offers valarray-like behavior in a package the size of a single CPU register.
		///
		/// @param	T	The element value type.
		/// @param	n	The number of elements.
		///
		/// @note	The primary template is not actually defined, only the explicit specializations corresponding to actual SIMD
		///			types are e.g. in Altivec, vec <short, 8> is defined (corresponding to __vector signed short on that platform) but vec <short, 16> isn't.
		///			This allows you to use template metaprogramming to determine whether an actual SIMD type exists on a platform.
		///
		/// @header	#include <macstl/vec.h>
		///
		/// @see	macstl::altivec, macstl::mmx
		
		template <typename T, std::size_t n> class vec
			{
				public:
					/// Type used for static initialization. Usually the same as the value type, but may vary due to C++ template non-type parameter restrictions.
					typedef value_type init_type;

					/// Type used for packing and unpacking elements.
					union union_type
						{
							/// For unpacking elements. This usually has the same as the value type, but may vary due to C++ union data member restrictions.
							value_type val [n];
							
							/// For packing elements.
							data_type vec;
						};
					
						
					/// Constructs with all elements zero.
					INLINE vec ()
						{
						}
						
					/// Constructs from the raw vector data.
					INLINE vec (data_type data): data_ (data)
						{
						}
					
					/// Sets a vector with n elements, @a v0...
					static vec set (value_type v0...);
										
					/// Fills a vector with the same element @a v.
					static vec fill (value_type v);
											
					/// Sets a vector with n constant elements, @a v0...
					template <init_type v0...> static vec set ();

					/// Fills a vector with the same constant element @a v.
					template <init_type v> static vec fill ();

					/// Loads a vector at @a offset from @a address.
					static vec load (const value_data* address, std::ptrdiff_t offset);
										
					/// Stores this vector to @a offset from @a address.
					vec store (value_data* address, std::ptrdiff_t offset);
			
			}
		
		#else
		// forward declare only, we will explicitly specialize particular vec
		template <typename T, std::size_t n> class vec;
		#endif
	}

namespace stdext
	{
		template <typename F> struct accumulator;
		template <typename T, typename I> struct cshifter;
		template <typename T, typename I> struct shifter;
	}
	
namespace macstl
	{
		namespace impl
			{
				template <typename V> class vec_reference
					{
						public:
							INLINE vec_reference& operator= (typename V::value_type lhs)
								{
									reinterpret_cast <typename V::union_type*> (data_)->val [index_] = data_of (lhs);
									return *this;
								}
								
							INLINE operator typename V::value_type () const
								{
									return reinterpret_cast <typename V::union_type*> (data_)->val [index_];
								}
								
							INLINE vec_reference (typename V::data_type* data, std::size_t index): data_ (data), index_ (index)
								{
								}
								
						private:
							typename V::data_type* const data_;
							const std::size_t index_;
					};

				template <typename T> struct data_vec
					{
						typedef T type;
					};
					
				template <typename T> struct is_vec
					{
						static const int value = 0;
					};

				template <typename T, std::size_t n> struct is_vec <vec <T, n> >
					{
						static const int value = 1;
					};

				template <typename T, typename Enable = void> struct data_type_of
					{
						typedef T type;
					};

				template <typename T> struct data_type_of <T, typename stdext::impl::enable_if <stdext::impl::exists <typename T::data_type>::value>::type>
					{
						typedef typename T::data_type type;
					};

			}
					
		/// @name I/O
		/// @relates vec
		
		//@{
		
		/// Inserts a @a vector into an output stream @a os.
		template <typename CharT, typename Traits, typename T, std::size_t n>
			std::basic_ostream <CharT, Traits>& operator<< (std::basic_ostream <CharT, Traits>& os, const vec <T, n>& vector)
			{
				if (os.good ())
					{
						typename std::basic_ostream <CharT, Traits>::sentry opfx (os);

						if (opfx)
							{
								typedef typename vec <T, n>::value_type value_type;
								typename vec <T, n>::union_type un;
								
								un.vec = vector.data ();
								
								std::basic_ostringstream <CharT, Traits> s;
								s.flags (os.flags ());
								s.imbue (os.getloc ());
								s.precision (os.precision ());
								
								s << value_type (un.val [0]);
								for (std::size_t i = 1; i < n; ++i)
									s << os.fill () << value_type (un.val [i]);

								if (s.fail ())
									os.setstate (std::ios_base::failbit);
								else
									os << s.str();
							}
					}
				return os;
			}
		
		/// Extracts a @a vector from an input stream @a is.
		template <typename CharT, typename Traits, typename T, std::size_t n>
			std::basic_istream <CharT, Traits>& operator>> (std::basic_istream <CharT, Traits>& is, vec <T, n>& vector)
			{
				if (is.good ())
					{
						typename std::basic_istream <CharT, Traits>::sentry ipfx (is);

						if (ipfx)
							{
								typedef typename vec <T, n>::value_type value_type;
								typename vec <T, n>::union_type un;
								
								for (std::size_t i = 0; i < n; ++i)
									{
										T val;
										is >> val;
										un.val [i] = data_of (val);
									}
									
								if (!is.fail ())
									vector = vec <T, n> (un.vec);
							}
					}
				return is;
			}

		/// Inserts a vector element into an output stream @a os, through an element proxy.
		template <typename CharT, typename Traits, typename T, std::size_t n>
			std::basic_ostream <CharT, Traits>& operator<< (std::basic_ostream <CharT, Traits>& os, const typename vec <T, n>::reference& reference)
			{
				return os << static_cast <typename vec <T, n>::value_type> (reference);
			}
			
		//@}
	}


namespace stdext
	{
		namespace impl
			{
				template <typename T, std::size_t n> struct has_trivial_copy <macstl::vec <T, n> >		{ enum { value = has_trivial_copy <T>::value }; };
				template <typename T, std::size_t n> struct has_trivial_assign <macstl::vec <T, n> >		{ enum { value = has_trivial_assign <T>::value }; };
				template <typename T, std::size_t n> struct has_trivial_destructor <macstl::vec <T, n> >	{ enum { value = has_trivial_destructor <T>::value }; };
			}
	}
	
#if defined(__INTEL_COMPILER)

#define DEFINE_VEC_CLASS_GUTS(VEC,VAL,BOO)														\
																								\
public:																							\
	typedef VEC data_type;																		\
	typedef VAL value_type;																		\
	typedef BOO boolean_type;																	\
																								\
	static const size_t length = sizeof (data_type) / sizeof (value_type);						\
																								\
	typedef vec <BOO, length> vec_boolean;														\
	typedef impl::data_type_of <VAL >::type value_data;											\
	typedef impl::vec_reference <vec> reference;												\
																								\
	INLINE vec (data_type data): data_ (data)	{ }												\
	INLINE void operator= (data_type lhs)		{ data_ = lhs; }								\
	INLINE data_type data () const				{ return data_; }								\
	INLINE static std::size_t size ()			{ return length; }								\
																								\
	INLINE value_type operator[] (std::size_t i) const											\
		{																						\
            return *((reinterpret_cast<const value_type *>(&data_)) + i);                       \
		}																						\
																								\
	INLINE value_type& operator[] (std::size_t i)												\
		{																						\
            return *((reinterpret_cast<value_type *>(&data_)) + i);                             \
		}																						\
																								\
	INLINE vec (const vec& other): data_ (other.data_)											\
		{																						\
		}																						\
																								\
private:																						\
	data_type data_;

#else

#if defined(NO_VEC_COPY_CONSTRUCTOR)

#define DEFINE_VEC_CLASS_GUTS(VEC,VAL,BOO)														\
																								\
public:																							\
	typedef VEC data_type;																		\
	typedef VAL value_type;																		\
	typedef BOO boolean_type;																	\
																								\
	static const size_t length = sizeof (data_type) / sizeof (value_type);						\
																								\
	typedef vec <BOO, length> vec_boolean;														\
	typedef impl::data_type_of <VAL >::type value_data;											\
	typedef impl::vec_reference <vec> reference;												\
																								\
	INLINE vec (data_type data): data_ (data)	{ }												\
	INLINE void operator= (data_type lhs)		{ data_ = lhs; }								\
	INLINE data_type data () const				{ return data_; }								\
	INLINE static std::size_t size ()			{ return length; }								\
																								\
	INLINE value_type operator[] (std::size_t i) const											\
		{																						\
			return value_type (reinterpret_cast <const union_type&> (data_).val [i]);			\
		}																						\
																								\
	INLINE reference operator[] (std::size_t i)													\
		{																						\
			return reference (&data_, i);														\
		}																						\
																								\
private:																						\
	data_type data_;

#else

#define DEFINE_VEC_CLASS_GUTS(VEC,VAL,BOO)														\
																								\
public:																							\
	typedef VEC data_type;																		\
	typedef VAL value_type;																		\
	typedef BOO boolean_type;																	\
																								\
	static const size_t length = sizeof (data_type) / sizeof (value_type);						\
																								\
	typedef vec <BOO, length> vec_boolean;														\
	typedef impl::data_type_of <VAL >::type value_data;											\
	typedef impl::vec_reference <vec> reference;												\
																								\
	INLINE vec (data_type data): data_ (data)	{ }												\
	INLINE void operator= (data_type lhs)		{ data_ = lhs; }								\
	INLINE data_type data () const				{ return data_; }								\
	INLINE static std::size_t size ()			{ return length; }								\
																								\
	INLINE value_type operator[] (std::size_t i) const											\
		{																						\
			return value_type (reinterpret_cast <const union_type&> (data_).val [i]);			\
		}																						\
																								\
	INLINE reference operator[] (std::size_t i)													\
		{																						\
			return reference (&data_, i);														\
		}																						\
																								\
	INLINE vec (const vec& other): data_ (other.data_)											\
		{																						\
		}																						\
																								\
private:																						\
	data_type data_;

#endif
#endif

///* Element Access Only, no modifications to elements*/
//const float& operator[](int i) const				
//{
//    return *(((float*)&vec)+i);
//}
//
///* Element Access and Modification*/
//float& operator[](int i)				
//{
//    return *(((float*)&vec)+i);
//}


#define DEFINE_VEC_COMMON_UNARY_FUNCTION(FN,FTR)															\
																											\
namespace stdext																							\
	{																										\
		template <typename T, std::size_t n> struct FTR <macstl::vec <T, n> >;								\
	}																										\
																											\
namespace macstl																							\
	{																										\
		template <typename T> INLINE const typename stdext::impl::enable_if <								\
			impl::is_vec <typename impl::data_vec <T>::type>::value,										\
			typename stdext::FTR <typename impl::data_vec <T>::type>::result_type>::type FN					\
			(const T& lhs)																					\
			{																								\
				return stdext::FTR <typename impl::data_vec <T>::type> () (lhs);							\
			}																								\
	}

#define DEFINE_VEC_COMMON_BINARY_FUNCTION(FN,FTR)															\
																											\
namespace stdext																							\
	{																										\
		template <typename T1, std::size_t n1, typename T2, std::size_t n2>									\
			struct FTR <macstl::vec <T1, n1>, macstl::vec <T2, n2> >;										\
		template <typename T, std::size_t n>																\
			struct FTR <macstl::vec <T, n>, macstl::vec <T, n> >;											\
	}																										\
																											\
namespace macstl																							\
	{																										\
		template <typename T1, typename T2> INLINE const typename stdext::impl::enable_if <					\
			impl::is_vec <typename impl::data_vec <T1>::type>::value != 0									\
			&& impl::is_vec <typename impl::data_vec <T2>::type>::value != 0,								\
			typename stdext::FTR <																			\
				typename impl::data_vec <T1>::type,															\
				typename impl::data_vec <T2>::type>::result_type>::type FN									\
			(const T1& lhs, const T2& rhs)																	\
			{																								\
				return stdext::FTR <																		\
					typename impl::data_vec <T1>::type,														\
					typename impl::data_vec <T2>::type> () (lhs, rhs);										\
			}																								\
	}

#define DEFINE_VEC_COMMON_TERNARY_FUNCTION(FN,FTR)															\
																											\
namespace stdext																							\
	{																										\
		template <typename T1, std::size_t n1, typename T2, std::size_t n2, typename T3, std::size_t n3>	\
			struct FTR <macstl::vec <T1, n1>, macstl::vec <T2, n2>, macstl::vec <T3, n3> >;					\
		template <typename T, std::size_t n>																\
			struct FTR <macstl::vec <T, n>, macstl::vec <T, n>, macstl::vec <T, n> >;						\
	}																										\
																											\
namespace macstl																							\
	{																										\
		template <typename T1, typename T2, typename T3> INLINE const typename stdext::impl::enable_if <	\
			impl::is_vec <typename impl::data_vec <T1>::type>::value != 0									\
			&& impl::is_vec <typename impl::data_vec <T2>::type>::value != 0								\
			&& impl::is_vec <typename impl::data_vec <T3>::type>::value != 0,								\
			typename stdext::FTR <																			\
				typename impl::data_vec <T1>::type,															\
				typename impl::data_vec <T2>::type,															\
				typename impl::data_vec <T3>::type>::result_type>::type FN									\
			(const T1& lhs, const T2& mhs, const T3& rhs)													\
			{																								\
				return stdext::FTR <																		\
					typename impl::data_vec <T1>::type,														\
					typename impl::data_vec <T2>::type,														\
					typename impl::data_vec <T3>::type> () (lhs, mhs, rhs);								\
			}																								\
	}
	
#define DEFINE_VEC_COMMON_CASSIGN_FUNCTION(FN,FTR)															\
																											\
namespace macstl																							\
	{																										\
		template <typename T1, typename T2, std::size_t n> INLINE const typename stdext::impl::enable_if <	\
			impl::is_vec <typename impl::data_vec <T2>::type>::value != 0,									\
			vec <T1, n>&>::type FN (vec <T1, n>& lhs, const T2& rhs)										\
			{																								\
				return lhs = stdext::FTR <vec <T1, n>, typename impl::data_vec <T2> > () (lhs, rhs);		\
			}																								\
	}

/// @name Unary Arithmetic
/// @relates macstl::vec
/// Each returns a new vector with elements of type T.

//@{
DEFINE_VEC_COMMON_UNARY_FUNCTION(operator+,identity)
DEFINE_VEC_COMMON_UNARY_FUNCTION(operator+,negate)
DEFINE_VEC_COMMON_UNARY_FUNCTION(operator~,bitwise_not)
//@}

/// @name Unary Logic
/// @relates macstl::vec
/// Each returns a new vector with boolean elements.

//@{
DEFINE_VEC_COMMON_UNARY_FUNCTION(operator!,logical_not)
//@}

/// @name Binary Arithmetic
/// @relates vec
/// Each returns a new vector with elements of type T. You can also use a raw vector for either lhs or rhs.

//@{
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator*,multiplies)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator/,divides)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator%,modulus)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator+,plus)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator-,minus)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator^,bitwise_xor)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator-,bitwise_and)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator-,bitwise_or)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator<<,shift_left)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator>>,shift_right)
//@}

/// @name Binary Logic
/// @relates macstl::vec
/// Each returns a new vector with boolean elements. You can also use a raw vector for either lhs or rhs.
//@{
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator==,equal_to)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator!=,not_equal_to)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator<,less)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator>,greater)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator<=,less_equal)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator>=,greater_equal)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator&&,logical_and)
DEFINE_VEC_COMMON_BINARY_FUNCTION(operator||,logical_or)
//@}

/// @name Transcendentals
/// @relates macstl::vec
/// Each returns a new vector with elements of type T. You can also use a raw vector for either lhs or rhs.

//@{
DEFINE_VEC_COMMON_UNARY_FUNCTION(abs,absolute)
DEFINE_VEC_COMMON_UNARY_FUNCTION(acos,arc_cosine)
DEFINE_VEC_COMMON_UNARY_FUNCTION(asin,arc_sine)
DEFINE_VEC_COMMON_UNARY_FUNCTION(atan,arc_tangent)
DEFINE_VEC_COMMON_BINARY_FUNCTION(atan2,arc_tangent2)
DEFINE_VEC_COMMON_UNARY_FUNCTION(conj,conjugate)
DEFINE_VEC_COMMON_UNARY_FUNCTION(cos,cosine)
DEFINE_VEC_COMMON_UNARY_FUNCTION(cosh,hyperbolic_cosine)
DEFINE_VEC_COMMON_UNARY_FUNCTION(exp,exponent)
DEFINE_VEC_COMMON_UNARY_FUNCTION(exp2,exponent2)
DEFINE_VEC_COMMON_TERNARY_FUNCTION(fma,multiplies_plus)
DEFINE_VEC_COMMON_UNARY_FUNCTION(log,logarithm)
DEFINE_VEC_COMMON_UNARY_FUNCTION(log2,logarithm2)
DEFINE_VEC_COMMON_UNARY_FUNCTION(log10,logarithm10)
DEFINE_VEC_COMMON_BINARY_FUNCTION(max,maximum)
DEFINE_VEC_COMMON_BINARY_FUNCTION(min,minimum)
DEFINE_VEC_COMMON_BINARY_FUNCTION(mulhi,multiplies_high)
DEFINE_VEC_COMMON_BINARY_FUNCTION(pow,power)
DEFINE_VEC_COMMON_UNARY_FUNCTION(rsqrt,reciprocal_square_root)
DEFINE_VEC_COMMON_TERNARY_FUNCTION(select,selection)
DEFINE_VEC_COMMON_UNARY_FUNCTION(sin,sine)
DEFINE_VEC_COMMON_UNARY_FUNCTION(sinh,hyperbolic_sine)
DEFINE_VEC_COMMON_UNARY_FUNCTION(sqrt,square_root)
DEFINE_VEC_COMMON_UNARY_FUNCTION(tan,tangent)
DEFINE_VEC_COMMON_UNARY_FUNCTION(tanh,hyperbolic_tangent)
//@}

/// @name Computed Assignments
/// @relates macstl::vec

/// @{
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator*=,multiplies)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator/=,divides)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator%=,modulus)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator+=,plus)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator-=,minus)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator^=,bitwise_xor)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator&=,bitwise_and)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator|=,bitwise_or)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator<<=,shift_left)
DEFINE_VEC_COMMON_CASSIGN_FUNCTION(operator>>=,shift_right)
/// @}


#define DEFINE_VEC_PLATFORM_UNARY_FUNCTION(FN,DESC)														\
																									\
template <typename T> struct FN##_function;																	\
																									\
template <typename T>																				\
	INLINE const typename FN##_function <typename macstl::impl::data_vec <T>::type>::result_type FN (const T& lhs)				\
	{																								\
		return FN##_function <typename macstl::impl::data_vec <T>::type> () (lhs);																	\
	}			

#define DEFINE_VEC_PLATFORM_UNARY_FUNCTION_WITH_LITERAL(FN,DESC)											\
																									\
template <unsigned int i, typename T> struct FN##_function;													\
																									\
template <unsigned int i, typename T>																\
	INLINE const typename FN##_function <i, typename macstl::impl::data_vec <T>::type>::result_type FN (const T& lhs)				\
	{																								\
		return FN##_function <i, typename macstl::impl::data_vec <T>::type> () (lhs);																	\
	}			

#define DEFINE_VEC_PLATFORM_UNARY_FUNCTION_WITH_LITERAL4(FN,DESC)											\
																									\
template <unsigned int i, unsigned int j, unsigned int k, unsigned int l, typename T> struct FN##_function;													\
																									\
template <unsigned int i, unsigned int j, unsigned int k, unsigned int l, typename T>																\
	INLINE const typename FN##_function <i, j, k, l, typename macstl::impl::data_vec <T>::type>::result_type FN (const T& lhs)				\
	{																								\
		return FN##_function <i, j, k, l, typename macstl::impl::data_vec <T>::type> () (lhs);																	\
	}			

#define DEFINE_VEC_PLATFORM_UNARY_FUNCTION_WITH_RETURN_TYPE(FN,DESC)										\
																									\
template <typename R, typename T> struct FN##_function;												\
																									\
template <typename R, typename T> INLINE const R FN (const T& lhs)									\
	{																								\
		return FN##_function <R, typename macstl::impl::data_vec <T>::type> () (lhs);				\
	}			
	
#define DEFINE_VEC_PLATFORM_BINARY_FUNCTION(FN,DESC)														\
																									\
template <typename T1, typename T2 = T1> struct FN##_function;												\
																									\
template <typename T1, typename T2>																	\
	INLINE const typename FN##_function <typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type>::result_type FN (const T1& lhs, const T2& rhs)						\
	{																								\
		return FN##_function <typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type> () (lhs, rhs);															\
	}

#define DEFINE_VEC_PLATFORM_BINARY_FUNCTION_WITH_LITERAL(FN,DESC)										\
																									\
template <unsigned int i, typename T1, typename T2 = T1> struct FN##_function;								\
																									\
template <unsigned int i, typename T1, typename T2>													\
	INLINE const typename FN##_function <i, typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type>::result_type FN (const T1& lhs, const T2& rhs)					\
	{																								\
		return FN##_function <i, typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type> () (lhs, rhs);														\
	}

#define DEFINE_VEC_PLATFORM_BINARY_FUNCTION_WITH_LITERAL2(FN,DESC)										\
																									\
template <unsigned int i, unsigned int j, typename T1, typename T2 = T1> struct FN##_function;								\
																									\
template <unsigned int i, unsigned int j, typename T1, typename T2>													\
	INLINE const typename FN##_function <i, j, typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type>::result_type FN (const T1& lhs, const T2& rhs)					\
	{																								\
		return FN##_function <i, j, typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type> () (lhs, rhs);														\
	}

#define DEFINE_VEC_PLATFORM_BINARY_FUNCTION_WITH_LITERAL4(FN,DESC)										\
																									\
template <unsigned int i, unsigned int j, unsigned int k, unsigned int l, typename T1, typename T2 = T1> struct FN##_function;								\
																									\
template <unsigned int i, unsigned int j, unsigned int k, unsigned int l, typename T1, typename T2>													\
	INLINE const typename FN##_function <i, j, k, l, typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type>::result_type FN (const T1& lhs, const T2& rhs)					\
	{																								\
		return FN##_function <i, j, k, l, typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type> () (lhs, rhs);														\
	}

#define DEFINE_VEC_PLATFORM_BINARY_FUNCTION_WITH_RETURN_TYPE(FN,DESC)									\
																									\
template <typename R, typename T1, typename T2 = T1> struct FN##_function;							\
																									\
template <typename R, typename T1, typename T2> INLINE const R FN (const T1& lhs, const T2& rhs)					\
	{																								\
		return FN##_function <R, typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type> () (lhs, rhs);			\
	}			

#define DEFINE_VEC_PLATFORM_TERNARY_FUNCTION(FN,DESC)													\
																									\
template <typename T1, typename T2 = T1, typename T3 = T1> struct FN##_function;								\
																									\
template <typename T1, typename T2, typename T3>													\
	INLINE const typename FN##_function <typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type, typename macstl::impl::data_vec <T3>::type>::result_type FN (const T1& lhs, const T2& mhs, const T3& rhs)	\
	{																								\
		return FN##_function <typename macstl::impl::data_vec <T1>::type, typename macstl::impl::data_vec <T2>::type, typename macstl::impl::data_vec <T3>::type> () (lhs, mhs, rhs);													\
	}



#ifdef __VEC__
#include "impl/vec_altivec.h"
#else
#if defined(__MMX__) || defined(__SSE__) || defined(__SSE2__)
#include "impl/vec_mmx.h"
#endif
#endif

#endif



