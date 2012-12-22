/*
 *  complex.h
 *  macstl
 *
 *  Created by Glen Low on Aug 2 2004.
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

#ifndef MACSTL_COMPLEX_H
#define MACSTL_COMPLEX_H

#include <cmath>
#include <sstream>

#include "impl/config.h"
#include "impl/meta.h"

namespace stdext
	{
		template <typename T> class complex;
		
		namespace impl
			{
				/// Base for complex number.
				
				/// @internal
				/// This base defines all the basic operations of complex numbers. The primary template uses an array to store the real and imaginary parts,
				/// while a partial specialization uses C99 complex numbers (if available) for the same.
				///
				/// @param	T		The element value type.
				/// @param	Enable	If void, enables a particular specialization
				///
				/// @note	Uses the Barton-Nackman trick so that the friend functions can use either stdext::complex or the underlying data type
				
				template <typename T, typename Enable = void> class complex_base
					{
						public:
							/// The data type, usually an array. In the partial specialization, it is the C99 complex number.
							typedef T data_type [2];

							/// Gets the real part.
							T real () const		{ return val_ [0]; }
							
							/// Gets the complex part.
							T imag () const		{ return val_ [1]; }
							
							const data_type& data () const
								{
									return val_;
								}

							/// @name Unary Arithmetic
							
							//@{
							
							/// Copies @a lhs.
							friend const complex <T> operator+ (const complex <T>& lhs)
								{
									return lhs;
								}
								 
							/// Negates @a lhs.
							friend const complex <T> operator- (const complex <T>& lhs)
								{
									return complex <T> () - lhs;
								}

							/// @name Unary Logic
							
							//@{
							
							/// Tests whether @a lhs is zero.
							friend bool operator! (const complex <T>& z)
								{
									return z == complex <T> ();
								}

							//@}
								
							//@}
							
							/// @name Binary Arithmetic
							/// You can also use a value of type T for either @a lhs or @a rhs.

							//@{
							
							/// Adds @a lhs to @a rhs.
							friend const complex <T> operator+ (const complex <T>& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs.real () + rhs.real (), lhs.imag () + rhs.imag ());
								}

							#ifndef DOXYGEN
							
							friend const complex <T> operator+ (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.real () + rhs, lhs.imag ());
								}

							friend const complex <T> operator+ (const T& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs + rhs.real (), rhs.imag ());
								}
							
							#endif

							/// Subtracts @a lhs to @a rhs.
							friend const complex <T> operator- (const complex <T>& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs.real () - rhs.real (), lhs.imag () - rhs.imag ());
								}

							#ifndef DOXYGEN
							
							friend const complex <T> operator- (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.real () - rhs, lhs.imag ());
								}

							friend const complex <T> operator- (const T& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs - rhs.real (), rhs.imag ());
								}
							
							#endif
							
							/// Multiplies @a lhs by @a rhs.
							friend const complex <T> operator* (const complex <T>& lhs, const complex <T>& rhs)
								{
									T lhs_re = lhs.real ();
									T lhs_im = lhs.imag ();
									T rhs_re = rhs.real ();
									T rhs_im = rhs.imag ();
									T lhs_re_rhs_re = lhs_re * rhs_re;
									T lhs_im_rhs_im = lhs_im * rhs_im;
									return complex <T> (lhs_re_rhs_re - lhs_im_rhs_im,
										lhs_re_rhs_re - (lhs_re - lhs_im) * (rhs_re - rhs_im) + lhs_im_rhs_im);
								}

							#ifndef DOXYGEN
							
							friend const complex <T> operator* (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.real () * rhs, lhs.imag () * rhs);
								}

							friend const complex <T> operator* (const T& lhs, const complex <T>& rhs)	
								{
									return complex <T> (lhs * rhs.real (), lhs * rhs.imag ());
								}

							#endif
							
							/// Divides @a lhs by @a rhs.
							friend const complex <T> operator/ (const complex <T>& lhs, const complex <T>& rhs)
								{
									T lhs_re = lhs.real ();
									T lhs_im = lhs.imag ();
									T rhs_re = rhs.real ();
									T rhs_im = rhs.imag ();
									T lhs_re_rhs_re = lhs_re * rhs_re;
									T lhs_im_rhs_im = lhs_im * rhs_im;
									T denom = rhs_re * rhs_re + rhs_im * rhs_im;
									return complex <T> ((lhs_re_rhs_re + lhs_im_rhs_im) / denom,
										(lhs_im_rhs_im - (lhs_re - lhs_im) * (rhs_re + rhs_im) - lhs_re_rhs_re) / denom);
								}

							#ifndef DOXYGEN
							
							friend const complex <T> operator/ (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.real () / rhs, lhs.imag () / rhs);
								}

							friend const complex <T> operator/ (const T& lhs, const complex <T>& rhs)	
								{
									return complex <T> (lhs / rhs.real (), lhs / rhs.imag ());
								}
							
							#endif
							
							//@}
							
							/// @name Binary Logic
							/// You can also use a value of type T for either @a lhs or @a rhs.
							
							//@{
							
							/// Tests whether @a lhs is equal to @a rhs.
							friend bool operator== (const complex <T>& lhs, const complex <T>& rhs)	
								{
									return lhs.real () == rhs.real () && lhs.imag () == rhs.real ();
								} 

							#ifndef DOXYGEN
							
							friend bool operator== (const complex <T>& lhs, const T& rhs)
								{
									return lhs.real () == rhs && lhs.imag () == T ();
								}

							friend bool operator== (const T& lhs, const complex <T>& rhs)
								{
									return lhs == rhs.real () && T () == rhs.imag ();
								}

							#endif
							
							/// Tests whether @a lhs is not equal to @a rhs.
							friend bool operator!= (const complex <T>& lhs, const complex <T>& rhs)
								{
									return lhs.real () != rhs.real () || lhs.imag () != rhs.real ();
								} 

							#ifndef DOXYGEN
							
							friend bool operator!= (const complex <T>& lhs, const T& rhs)
								{
									return lhs.real () != rhs || lhs.imag () != T ();
								}

							friend bool operator!= (const T& lhs, const complex <T>& rhs)
								{
									return lhs != rhs.real () || T () != rhs.imag ();
								}

							#endif
							
							/// Tests whether both @a lhs and @a rhs are non-zero.
							friend bool operator&& (const complex <T>& lhs, const complex <T>& rhs)
								{
									return lhs != complex <T> () && rhs != complex <T> ();
								}

							#ifndef DOXYGEN
							
							friend bool operator&& (const complex <T>& lhs, const T& rhs)
								{
									return lhs != complex <T> () && rhs != T ();
								}

							friend bool operator&& (const T& lhs, const complex <T>& rhs)
								{
									return lhs != T () && rhs != complex <T> ();;
								}

							#endif
							
							/// Tests whether either @a lhs or @a rhs are non-zero.
							friend bool operator|| (const complex <T>& lhs, const complex <T>& rhs)
								{
									return lhs != complex <T> () || rhs != complex <T> ();
								}

							#ifndef DOXYGEN
							
							friend bool operator|| (const complex <T>& lhs, const T& rhs)
								{
									return lhs != complex <T> () || rhs != T ();
								}

							friend bool operator|| (const T& lhs, const complex <T>& rhs)
								{
									return lhs != T () || rhs != complex <T> ();
								}
							
							#endif
							
							//@}
							
						protected:
							T val_ [2];
							
							complex_base (const T& re, const T& im)
								{
									val_ [0] = re;
									val_ [1] = im;
								}

							complex_base (const data_type& val)
								{
									val_ [0] = val [0];
									val_ [1] = val [1];
								}
								

					};
					
				template <typename T> struct c99_complex;

				#if 0 // def HAS_C99_COMPLEX
				
				template <> struct c99_complex <float>
					{
						typedef float _Complex type;
						enum { value = true };
					};

				template <> struct c99_complex <double>
					{
						typedef double _Complex type;
						enum { value = true };
					};
					
				template <typename T> class complex_base <T, typename enable_if <c99_complex <T>::value>::type>
					{
						public:
							typedef typename c99_complex <T>::type data_type;

							T real () const				{ return __real__ val_; }
							T imag () const				{ return __imag__ val_; }
							data_type data () const		{ return val_; }

							friend const complex <T> operator+ (const complex <T>& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs.data () + rhs.data ());
								}

							friend const complex <T> operator+ (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.data () + rhs);
								}

							friend const complex <T> operator+ (const T& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs + rhs.data ());
								}

							friend const complex <T> operator- (const complex <T>& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs.data () - rhs.data ());
								}

							friend const complex <T> operator- (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.data () - rhs);
								}

							friend const complex <T> operator- (const T& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs - rhs.data ());
								}

							friend const complex <T> operator* (const complex <T>& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs.data () * rhs.data ());
								}

							friend const complex <T> operator* (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.data () * rhs);
								}

							friend const complex <T> operator* (const T& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs * rhs.data ());
								}

							friend const complex <T> operator/ (const complex <T>& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs.data () / rhs.data ());
								}

							friend const complex <T> operator/ (const complex <T>& lhs, const T& rhs)
								{
									return complex <T> (lhs.data () / rhs);
								}

							friend const complex <T> operator/ (const T& lhs, const complex <T>& rhs)
								{
									return complex <T> (lhs / rhs.data ());
								}

							friend const complex <T> operator+ (const complex <T>& z)
								{
									return z;
								}
								 
							friend const complex <T> operator- (const complex <T>& z)
								{
									return T () - z.data ();
								}

							friend bool operator! (const complex <T>& z)
								{
									return z.data () == complex <T> ();
								}

							friend bool operator== (const complex <T>& lhs, const complex <T>& rhs)	
								{
									return lhs.data () == rhs.data ();
								} 

							friend bool operator== (const complex <T>& lhs, const T& rhs)
								{
									return lhs.data () == rhs;
								}

							friend bool operator== (const T& lhs, const complex <T>& rhs)
								{
									return lhs == rhs.data ();
								}

							friend bool operator!= (const complex <T>& lhs, const complex <T>& rhs)	
								{
									return lhs.data () != rhs.data ();
								} 

							friend bool operator!= (const complex <T>& lhs, const T& rhs)
								{
									return lhs.data () != rhs;
								}

							friend bool operator!= (const T& lhs, const complex <T>& rhs)
								{
									return lhs != rhs.data ();
								}
								
							friend bool operator&& (const complex <T>& lhs, const complex <T>& rhs)
								{
									return lhs.data () && rhs.data ();
								}

							friend bool operator&& (const complex <T>& lhs, const T& rhs)
								{
									return lhs.data () && rhs;
								}

							friend bool operator&& (const T& lhs, const complex <T>& rhs)
								{
									return lhs && rhs.data ();
								}

							friend bool operator|| (const complex <T>& lhs, const complex <T>& rhs)
								{
									return lhs.data () || rhs.data ();
								}

							friend bool operator|| (const complex <T>& lhs, const T& rhs)
								{
									return lhs.data () || rhs;
								}

							friend bool operator|| (const T& lhs, const complex <T>& rhs)
								{
									return lhs || rhs.data ();
								}
								
						protected:
							data_type val_;

							complex_base (const T& re, const T& im)
								{
									__real__ val_ = re;
									__imag__ val_ = im;
								}
								
							complex_base (const data_type& val): val_ (val)
								{
								}
					};

				#endif
					
				template <typename T> struct has_trivial_copy <complex <T> >		{ enum { value = has_trivial_copy <T>::value }; };
				template <typename T> struct has_trivial_assign <complex <T> >		{ enum { value = has_trivial_assign <T>::value }; };
				template <typename T> struct has_trivial_destructor <complex <T> >	{ enum { value = has_trivial_destructor <T>::value }; };
			}

		/// Complex number.
		
		/// Representation of the mathematical concept of complex numbers. This implementation is largely a superset of std::complex functionality,
		/// and wraps the C99 complex data types if present.
		///
		/// @param	T	The element value type, which shall satisfy the following requirements:
		///				- not an abstract class, reference type or cv-qualified type
		///				- if a class, must publicly define: default constructor, copy constructor, destructor and assignment operator
		///				- Default construct + assign == copy construct
		///				- Destruct + copy construct == assign
		///				.
		///				For example, built-in arithmetic types like char, short, int, float, double.
		///
		/// @note	We do not call through to the equivalent C99 transcendental functions, but rather use our own faster inline versions.
		///
		/// @header	#include <macstl/complex.h>
			
		template <typename T> class complex: public impl::complex_base <T>
			{
				public:
					/// The element value type.
					typedef T value_type;
					
					/// @name Constructors
					
					//@{
					
					/// Constructs a complex from a real @a re and imaginary @a im.
					complex (const T& re = T (), const T& im = T ()):
						impl::complex_base <T> (re, im)
						{
						}
						
					/// Constructs a complex from raw @a data.
					complex (const typename impl::complex_base <T>::data_type& data):
						impl::complex_base <T> (data)
						{
						}
						
					/// Constructs a complex number from the @a other complex.
					template <typename U> explicit complex (const complex <U>& other):
						impl::complex_base <T> (other.real (), other.imag ())
						{
						}

					//@}
					
					/// @name Assignments
					/// You can also use a value of type T for rhs.
					
					//@{

					#ifdef DOXYGEN
					
					/// Assigns @a rhs.
					complex& operator= (const complex& rhs);
					
					#else
			
					complex& operator= (const T& rhs)			{ return *this = complex (rhs, T ()); }
			
					#endif
					
					//@}
					
					/// @name Computed Assignments
					/// You can also use a value of type T for rhs.
					
					//@{
					
					#ifndef DOXYGEN
					complex& operator+= (const T& rhs)			{ return *this = *this + rhs; }
					complex& operator-= (const T& rhs)			{ return *this = *this - rhs; }
					complex& operator*= (const T& rhs)			{ return *this = *this * rhs; }
					complex& operator/= (const T& rhs)			{ return *this = *this / rhs; }
					#endif
					
					/// Adds the complex to @a rhs, then assigns it.
					complex& operator+= (const complex& rhs)	{ return *this = *this + rhs; }

					/// Subtracts the complex to @a rhs, then assigns it.
					complex& operator-= (const complex& rhs)	{ return *this = *this - rhs; }

					/// Multiplies the complex by @a rhs, then assigns it.
					complex& operator*= (const complex& rhs)	{ return *this = *this * rhs; }

					/// Divides the complex by @a rhs, then assigns it.
					complex& operator/= (const complex& rhs)	{ return *this = *this / rhs; }
					
					//@}

					/// @name Values
					
					//@{
					
					/// Gets the real part of @a lhs
					friend T real (const complex& lhs)  { return lhs.real (); }
					
					/// Gets the imaginary part of @a lhs.
					friend T imag (const complex& lhs)  { return lhs.imag (); }

					// complex transcendentals --
					// we resist the temptation to call the equivalent C99 functions in complex.h because:
					// 1. complex.h is not available on 10.2 and requires a separate link to libmx
					// 2. inline functions are faster than library functions -- we only call the standard math functions as few times as possible
					// 3. this makes them generic for all T, not just floats, doubles and long doubles
					
					/// Gets the magnitude of @a lhs.
					friend T abs (const complex& lhs)
						{
							using namespace std;
							
							return hypot (lhs.real (), lhs.imag ());
						}
					
					/// Gets the phase angle of @a lhs.
					friend T arg (const complex& lhs)
						{
							using std::atan2;
							
							return atan2 (lhs.imag (), lhs.real ());
						}
						
					/// Gets the square magniture of @a lhs.
					friend T norm (const complex& lhs)
						{
							T lhs_re = lhs.real ();
							T lhs_im = lhs.imag ();
							return lhs_re * lhs_re + lhs_im * lhs_im;
						}
						
					/// Gets the complex conjugate of @a lhs.
					friend const complex conj (const complex& lhs)
						{
							return complex (lhs.real (), -lhs.imag ());
						}
						
					//@}
					
					/// @name Transcendentals
					
					//@{
						
					/// Gets the complex natural exponential of @a lhs.
					friend const complex exp (const complex& lhs)
						{
							using std::exp;	// in case T is builtin, need to resolve exp call to it
							
							return polar (static_cast <T> (exp (lhs.real ())), lhs.imag ());
						}

					/// Gets the complex natural logarithm of @a lhs.
					friend const complex log (const complex& lhs)
						{
							using std::log;	// in case T is builtin, need to resolve log call to it
							
							return complex (log (abs (lhs)), arg (lhs));
						}

					/// Gets the complex common logarithm of @a lhs.
					friend const complex log10 (const complex& lhs)
						{
							using std::log;	// in case T is builtin, need to resolve log call to it
							
							return complex (log (abs (lhs)), arg (lhs));
						}

					/// Raises @a lhs to the complex power @a rhs.
					friend const complex pow (const complex& lhs, int rhs)
						{
							using std::pow;	// in case T is builtin, need to resolve pow call to it
							
							return polar (pow (abs (lhs), rhs), arg (lhs) * rhs);
						}
						
					/// Raises @a lhs to the complex power @a rhs.
					friend const complex pow (const complex& lhs, const complex& rhs)
						{
							return exp (rhs * log (lhs));
						}
						
					/// Raises @a lhs to the complex power @a rhs.
					friend const complex pow (const complex& lhs, const T& rhs)
						{
							using std::pow;	// in case T is builtin, need to resolve pow call to it
							
							return polar (pow (abs (lhs), rhs), arg (lhs) * rhs);
						}
						
					/// Raises @a lhs to the complex power @a rhs.
					friend const complex pow (const T& lhs, const complex& rhs)
						{
							using std::log;
							
							return exp (rhs * log (lhs));
						}

					/// Gets the complex sine of @a lhs.
					friend const complex sin (const complex& lhs)
						{
							// sin (lhs) = (exp (zi) - 1 / exp (zi)) / 2i
						
							complex exp_zi = exp (complex (-lhs.imag (), lhs.real ()));
							complex result = (exp_zi - 1 / exp_zi) / 2;
							return complex (result.imag (), -result.real ());				
						}

					/// Gets the complex cosine of @a lhs.
					friend const complex cos (const complex& lhs)
						{
							// cos (lhs) = (exp (zi) + 1 / exp (zi)) / 2
							complex exp_zi = exp (complex (-lhs.imag (), lhs.real ()));
							return (exp_zi + 1 / exp_zi) / 2;				
						}

					/// Gets the complex tangent of @a lhs.
					friend const complex tan (const complex& lhs)
						{
							// tan (z) = (exp (2zi) - 1) / i (exp (2zi) + 1)
							complex zi = complex (-lhs.imag (), lhs.real ());
							complex exp_2zi = exp (zi + zi);
							complex exp_2zi_1 = exp_2zi + 1;
							return (exp_2zi - 1) / complex (-exp_2zi_1.imag (), exp_2zi_1.real ());
						}

					/// Gets the complex hyperbolic sine of @a lhs.
					friend const complex sinh (const complex& lhs)
						{
							// sinh (z) = (exp (z) - 1 / exp (z)) / 2
							complex exp_z = exp (lhs);
							return (exp_z - 1 / exp_z) / 2;				
						}

					/// Gets the complex hyperbolic cosine of @a lhs.
					friend const complex cosh (const complex& lhs)
						{
							// cosh (z) = (exp (z) + 1 / exp (z)) / 2
							complex exp_z = exp (lhs);
							return (exp_z + 1 / exp_z) / 2;				
						}

					/// Gets the complex hyperbolic tangent of @a lhs.
					friend const complex tanh (const complex& lhs)
						{
							// tanh (z) = (exp (2z) - 1) / (exp (2z) + 1)

							complex exp_2z = exp (lhs + lhs);
							return (exp_2z - 1) / (exp_2z + 1);			
						}

					/// Gets the complex square root of @a lhs.
					friend const complex sqrt (const complex& lhs)
						{
							using std::sqrt;	// in case T is builtin, need to resolve sqrt call to it
							
							return polar (sqrt (abs (lhs)), arg (lhs) / 2);
						}
						
					//@}
 			};
			
		/// @relates stdext::complex
		/// @brief Constructs a complex with magnitude @a r and phase angle @a ang.
		template <typename T> inline const complex <T> polar (const T& r, const T& ang)
			{
				using std::cos;
				using std::sin;
				
				return complex <T> (r * cos (ang), r * sin (ang));
			}

		/// @relates stdext::complex
		/// @brief Extracts @a rhs from the input stream @a is.
		template <typename T, typename CharT, typename Traits>
			inline std::basic_istream <CharT, Traits>& operator>> (std::basic_istream <CharT, Traits>& is, complex <T>& rhs)
			{
				if (is.good ())
					{
						typename std::basic_istream <CharT, Traits>::sentry ipfx (is);
						if (ipfx)
							{
								T re = T ();
								T im = T ();
								
								CharT ch;
								is >> ch;
								if (ch == '(')
									{
										// (re,im) or (re)
										is >> re >> ch;
										if (ch == ',')
											is >> im >> ch;
										if (ch != ')')
											is.setstate (std::ios_base::failbit);
									}
								else
									{
										// re
										is.putback (ch);
										is >> re;
									}
									
								if (!is.fail ())
									rhs = complex <T> (re, im);
							}
					}
				return is;
			}
			
		/// @relates stdext::complex
		/// @brief Inserts @a rhs into the output stream @a os.
		template <typename T, typename CharT, typename Traits>
			inline std::basic_ostream <CharT, Traits>& operator<< (std::basic_ostream <CharT, Traits>& os, const complex <T>& rhs)
			{
				if (os.good ())
					{
						typename std::basic_ostream <CharT, Traits>::sentry opfx (os);
						if (opfx)
							{
								std::basic_ostringstream <CharT, Traits> s;
								s.flags (os.flags ());
								s.imbue (os.getloc ());
								s.precision (os.precision ());
								s << '(' << rhs.real () << ',' << rhs.imag () << ')';
								
								if (s.fail ())
									os.setstate (std::ios_base::failbit);
								else
									os << s.str ();
							}
					}
				return os;
			}
	}
	
	
#endif
