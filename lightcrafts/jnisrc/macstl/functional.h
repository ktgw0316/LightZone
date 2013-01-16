/*
 *  functional.h
 *  macstl
 *
 *  Created by Glen Low on Nov 13 2002.
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
 
#ifndef MACSTL_FUNCTIONAL_H
#define MACSTL_FUNCTIONAL_H

#include <cmath>
#include <functional>
#include <limits.h>

#include "impl/meta.h"

#ifndef HAS_LOG2
// Cygwin has a log2 macro but no log2 function, so we #undef it here
#ifdef log2
#undef log2
#endif
inline float log2 (float lhs)	{ return logf (lhs) / 0.693147180559945309417f; }
inline double log2 (double lhs)	{ return log (lhs) / 0.693147180559945309417; }
#endif

namespace stdext
	{
	
		namespace impl
			{
				template <typename T> struct smaller_than_half_int
					{
						enum { value = sizeof (T) > sizeof (int) / 2 };
					};
			}

		template <typename T> inline typename impl::enable_if <impl::smaller_than_half_int <T>::value, T>::type mulhi (const T& lhs, const T& rhs)
			{
				// if lhs * rhs will fit into an int, just do the multiply and grab the high word
				return (lhs * rhs) >> (sizeof (T) * CHAR_BIT);
			}
			
		template <typename T> inline typename impl::enable_if <impl::smaller_than_half_int <T>::value == 0, T>::type mulhi (const T& lhs, const T& rhs)
			{
				// if lhs * rhs won't fit into an int, split into high and low words and do it the schoolyard way...
				
				static const int half_size = sizeof (T) * CHAR_BIT / 2;
				static const T lo_mask = (1 << half_size) - 1;
				
				T lhs_hi = lhs >> half_size;
				T lhs_lo = lhs & lo_mask;
				T rhs_hi = rhs >> half_size;
				T rhs_lo = rhs & lo_mask;
				
				T mid = ((lhs_lo * rhs_lo) >> half_size) + (lhs_lo * rhs_hi);
				T mid_hi = mid >> half_size;
				T mid_lo = mid & lo_mask;
				
				return lhs_hi * rhs_hi + mid_hi + ((mid_lo + lhs_hi * rhs_lo) >> half_size);
			}
			
		#if defined(__PPC__) || defined(__PPC64__)

		inline int mulhi (int lhs, int rhs)
			{
				int result;
				__asm__ ("mulhw %0, %1, %2" 
					   /* outputs:  */ : "=r" (result) 
					   /* inputs:   */ : "r" (lhs), "r"(rhs));
				return result;
			}

		inline unsigned int mulhi (unsigned int lhs, unsigned int rhs)
			{
				unsigned int result;
				__asm__ ("mulhwu %0, %1, %2" 
					   /* outputs:  */ : "=r" (result) 
					   /* inputs:   */ : "r" (lhs), "r"(rhs));
				return result;
			}

		#endif

		inline float rsqrt (float lhs)
			{
				return (float) (1.0 / std::sqrt ((double) lhs));	// note, we use double precision here so that the double op should be accurate...
			}
			
		// absolute

		template <typename T> struct absolute: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::abs;
						return abs (lhs);
					}
			};

		template <typename T> struct arc_cosine: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::acos;
						return acos (lhs);
					}
			};
			
		// arc_sine

		template <typename T> struct arc_sine: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::asin;
						return asin (lhs);
					}
			};
			
		// arc_tangent

		template <typename T> struct arc_tangent: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::atan;
						return atan (lhs);
					}
			};
			
		// arc_tangent2
			
		template <typename T1, typename T2 = T1> struct arc_tangent2;
		
		template <typename T> struct arc_tangent2 <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						using std::atan2;
						return atan2 (lhs, rhs);
					}
			};
						
		// bitwise_and

		template <typename T1, typename T2 = T1> struct bitwise_and;
		
		template <typename T> struct bitwise_and <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs & rhs;
					}
			};
			
		// bitwise_not
		
		template <typename T> struct bitwise_not: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						return ~lhs;
					}
			};
			
		// bitwise_or
		
		template <typename T1, typename T2 = T1> struct bitwise_or;
		
		template <typename T> struct bitwise_or <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs | rhs;
					}
			};
		
		// bitwise_xor
		
		template <typename T1, typename T2 = T1> struct bitwise_xor;
		
		template <typename T> struct bitwise_xor <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs ^ rhs;
					}
			};
			
		// conjugate
		
		template <typename T> struct conjugate: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						return conj (lhs);
					}
			};
			
		// cosine
			
		template <typename T> struct cosine: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::cos;
						return cos (lhs);
					}
			};
			
		// divides
			
		template <typename T1, typename T2 = T1> struct divides;
		
		template <typename T> struct divides <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs / rhs;
					}
			};
			
		// equal_to

		template <typename T1, typename T2 = T1> struct equal_to;
		
		template <typename T> struct equal_to <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs == rhs;
					}
			};
			
		// exponent

		template <typename T> struct exponent: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::exp;
						return exp (lhs);
					}
			};

		// exponent2
		
		template <typename T> struct exponent2: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						return exp2 (lhs);
					}
			};
						
		// greater
			
		template <typename T1, typename T2 = T1> struct greater;
		
		template <typename T> struct greater <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs > rhs;
					}
			};
			
		// greater_equal

		template <typename T1, typename T2 = T1> struct greater_equal;
		
		template <typename T> struct greater_equal <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs >= rhs;
					}
			};

		// hyperbolic_cosine
			
		template <typename T> struct hyperbolic_cosine: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::cosh;
						return cosh (lhs);
					}
			};

		// hyperbolic_sine
		
		template <typename T> struct hyperbolic_sine: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::sinh;
						return sinh (lhs);
					}
			};

		// hyperbolic_tangent
		
		template <typename T> struct hyperbolic_tangent: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::tanh;
						return tanh (lhs);
					}
			};

		// identity

		template <typename T> struct identity: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						return lhs;
					}
			};
			
		// less

		template <typename T1, typename T2 = T1> struct less;
		
		template <typename T> struct less <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs < rhs;
					}
			};
			
		// less_equal

		template <typename T1, typename T2 = T1> struct less_equal;
		
		template <typename T> struct less_equal <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs <= rhs;
					}
			};
			
		// logarithm
		
		template <typename T> struct logarithm: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::log;
						return log (lhs);
					}
			};

		// logarithm2
		
		template <typename T> struct logarithm2: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						return log2 (lhs);
					}
			};

		// logarithm10
		
		template <typename T> struct logarithm10: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::log10;
						return log10 (lhs);
					}
			};

			
		// logical_and

		template <typename T1, typename T2 = T1> struct logical_and;
		
		template <typename T> struct logical_and <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs && rhs;
					}
			};
			
		// logical_not

		template <typename T> struct logical_not: public std::unary_function <T, bool>
			{
				bool operator() (const T& lhs) const
					{
						return !lhs;
					}
			};

		// logical_or
		
		template <typename T1, typename T2 = T1> struct logical_or;
		
		template <typename T> struct logical_or <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs || rhs;
					}
			};
			
		// maximum

		template <typename T1, typename T2 = T1> struct maximum;
		
		template <typename T> struct maximum <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return rhs < lhs ? lhs : rhs;
					}
			};

		#if !__FINITE_MATH_ONLY__
		
		template <> struct maximum <float, float>: public std::binary_function <float, float, float>
			{
				float operator() (float lhs, float rhs) const
					{
						return rhs < lhs || rhs != rhs ? lhs : rhs;	// in case lhs is NAN
					}
			};

		template <> struct maximum <double, double>: public std::binary_function <double, double, double>
			{
				double operator() (double lhs, double rhs) const
					{
						return rhs < lhs || rhs != rhs ? lhs : rhs;	// in case lhs is NAN
					}
			};
			
		#endif
		
		// minimum
		
		template <typename T1, typename T2 = T1> struct minimum;
		
		template <typename T> struct minimum <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs < rhs ? lhs : rhs;
					}
			};
		
		#if !__FINITE_MATH_ONLY__

		template <> struct minimum <float, float>: public std::binary_function <float, float, float>
			{
				float operator() (float lhs, float rhs) const
					{
						return lhs < rhs || rhs != rhs ? lhs : rhs;	// in case lhs is NAN
					}
			};

		template <> struct minimum <double, double>: public std::binary_function <double, double, double>
			{
				double operator() (double lhs, double rhs) const
					{
						return lhs < rhs || rhs != rhs ? lhs : rhs;	// in case lhs is NAN
					}
			};
			
		#endif
						
		// minus

		template <typename T1, typename T2 = T1> struct minus;
		
		template <typename T> struct minus <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs - rhs;
					}
			};
			
		// modulus

		template <typename T1, typename T2 = T1> struct modulus;
		
		template <typename T> struct modulus <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs % rhs;
					}
			};
			
		// multiplies

		template <typename T1, typename T2 = T1> struct multiplies;
		
		template <typename T> struct multiplies <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs * rhs;
					}
			};
			
		// multiplies_high
					
		template <typename T1, typename T2 = T1> struct multiplies_high;
		
		template <typename T> struct multiplies_high <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return mulhi (lhs, rhs);
					}
			};
			
		// multiplies_plus

		template <typename T1, typename T2 = T1, typename T3 = T2> struct multiplies_plus;

		// negate
		
		template <typename T> struct negate: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						return -lhs;
					}
			};

		// not_equal_to
		
		template <typename T1, typename T2 = T1> struct not_equal_to;
		
		template <typename T> struct not_equal_to <T, T>: public std::binary_function <T, T, bool>
			{
				bool operator() (const T& lhs, const T& rhs) const
					{
						return lhs != rhs;
					}
			};
			
		// power
		
		template <typename T1, typename T2 = T1> struct power;
		
		template <typename T> struct power <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						using std::pow;
						return pow (lhs, rhs);
					}
			};

		// plus
		
		template <typename T1, typename T2 = T1> struct plus;
		
		template <typename T> struct plus <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs + rhs;
					}
			};
			
		// reciprocal_square_root
		
		template <typename T> struct reciprocal_square_root: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						return rsqrt (lhs);
					}
			};

		// selection
		
		template <typename T1, typename T2 = T1, typename T3 = T2> struct selection;
		
		template <typename T> struct selection <bool, T, T>
			{
				typedef bool first_argument_type;
				typedef T second_argument_type;
				typedef T third_argument_type;
				typedef T result_type;
				
				const T operator() (bool lhs, const T& mhs, const T& rhs) const
					{
						return lhs ? mhs : rhs;
					}
			};

		// shift_left
		
		template <typename T1, typename T2 = T1> struct shift_left;
		
		template <typename T> struct shift_left <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs << rhs;
					}
			};
		
		// shift_right
		
		template <typename T1, typename T2 = T1> struct shift_right;
		
		template <typename T> struct shift_right <T, T>: public std::binary_function <T, T, T>
			{
				const T operator() (const T& lhs, const T& rhs) const
					{
						return lhs >> rhs;
					}
			};
			
		// sine
			
		template <typename T> struct sine: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::sin;
						return sin (lhs);
					}
			};
			
		// square_root

		template <typename T> struct square_root: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::sqrt;
						return sqrt (lhs);
					}
			};
			
		// tangent

		template <typename T> struct tangent: public std::unary_function <T, T>
			{
				const T operator() (const T& lhs) const
					{
						using std::tan;
						return tan (lhs);
					}
			};
						
		template <typename Type> struct parameter
			{
				typedef const Type& type;
			};
			
		template <typename Type> struct parameter <Type&>
			{
				typedef Type& type;
			};
				
		template <typename Operation> class binder1st: protected Operation
			{
				public:
					typedef typename Operation::second_argument_type argument_type;
					typedef typename Operation::result_type result_type;
					
					binder1st (const Operation& op,
						typename parameter <typename Operation::first_argument_type>::type bound):
						Operation (op), bound_ (bound)
						{
						}
						
					typename Operation::result_type operator()
						(typename parameter <typename Operation::second_argument_type>::type x) const
						{
							return (*this) (bound_, x); 
						}

					typename Operation::first_argument_type bound () const
						{
							return bound_;
						}

				private:
					const typename Operation::first_argument_type bound_;
					
					using Operation::operator();
			};
		
		template <typename Operation, typename Type> inline binder1st <Operation> 
			bind1st (const Operation& fn, const Type& x) 
			{
				typedef typename parameter <typename Operation::first_argument_type>::type first_argument_type;
				return binder1st <Operation> (fn, first_argument_type (x));
			}
	
		template <typename Operation, typename Type> inline binder1st <Operation> 
			bind1st (const Operation& fn, Type& x) 
			{
				typedef typename parameter <typename Operation::first_argument_type>::type first_argument_type;
				return binder1st<Operation> (fn, first_argument_type (x));
			}
	
		template <typename Operation> class binder2nd: protected Operation
			{
				public:
					typedef typename Operation::first_argument_type argument_type;
					typedef typename Operation::result_type result_type;
					
					binder2nd (const Operation& op,
						typename parameter <typename Operation::second_argument_type>::type bound):
						Operation (op), bound_ (bound)
						{
						}
						
					typename Operation::result_type operator()
						(typename parameter <typename Operation::first_argument_type>::type x) const
						{
							return (*this) (x, bound_); 
						}
						
					typename Operation::second_argument_type bound () const
						{
							return bound_;
						}
						
				private:
					const typename Operation::second_argument_type bound_;
					
					using Operation::operator();
			};
		
		template <typename Operation, typename Type> inline binder2nd <Operation> 
			bind2nd (const Operation& fn, const Type& x) 
			{
				typedef typename parameter <typename Operation::second_argument_type>::type second_argument_type;
				return binder2nd <Operation> (fn, second_argument_type (x));
			}
	
		template <typename Operation, typename Type> inline binder2nd <Operation> 
			bind2nd (const Operation& fn, Type& x) 
			{
				typedef typename parameter <typename Operation::second_argument_type>::type second_argument_type;
				return binder2nd <Operation> (fn, second_argument_type (x));
			}
	}
	
#endif
	


