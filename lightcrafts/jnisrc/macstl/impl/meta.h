/*
 *  meta.h
 *  macstl
 *
 *  Created by Glen Low on Oct 13 2004.
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

#ifndef MACSTL_IMPL_META_H
#define MACSTL_IMPL_META_H

namespace stdext
	{
		namespace impl
			{
				// enable_if
				
				template <bool If, typename T = void> struct enable_if
					{
					};
					
				template <typename T> struct enable_if <true, T>
					{
						typedef T type;
					};
					
				// is_same
				
				template <typename T, typename U> struct is_same
					{
						static const int value = 0;
					};
					
				template <typename T> struct is_same <T, T>
					{
						static const int value = 1;
					};
					
				// exists -- note: this appears to do nothing useful, but it's really for use within an overload set when T may be a non-existent type
				
				template <typename T> struct exists
					{
						static const bool value = 1;
					};
					
				// is_unary_function, is_binary_function
				
				enum yes_no
					{
						yes = 1,
						no = 2
					};
				
				template <typename T> char (*has_result_type (typename T::result_type*)) [yes];
				template <typename T> char (*has_result_type (...)) [no];
				
				template <typename T> char (*has_argument_type (typename T::argument_type*)) [yes];
				template <typename T> char (*has_argument_type (...)) [no];

				template <typename T> char (*has_first_argument_type (typename T::first_argument_type*)) [yes];
				template <typename T> char (*has_first_argument_type (...)) [no];

				template <typename T> char (*has_second_argument_type (typename T::second_argument_type*)) [yes];
				template <typename T> char (*has_second_argument_type (...)) [no];
				
				template <typename T> char (*will_convert (T)) [yes];
				template <typename T> char (*will_convert (...)) [no];

				template <typename T> struct is_unary_function
					{
						enum { value =
							sizeof (*has_argument_type <T> (NULL)) == yes
							&& sizeof (*has_result_type <T> (NULL)) == yes  };
					};
		
				template <typename T> struct is_binary_function
					{
						enum { value =
							sizeof (*has_first_argument_type <T> (NULL)) == yes
							&& sizeof (*has_second_argument_type <T> (NULL)) == yes
							&& sizeof (*has_result_type <T> (NULL)) == yes };
					};

				template <typename T1, typename T2> struct is_convertible
					{
						enum { value =
							sizeof (*will_convert <T2> (*(T1*) NULL)) == yes  };
					};
					
				// type traits
				
				template <typename T> struct is_pod
					{
						static const int value = 0;
					};
					
				template <> struct is_pod <unsigned char>		{ static const int value = 1; };
				template <> struct is_pod <signed char>			{ static const int value = 1; };
				template <> struct is_pod <char>				{ static const int value = 1; };
				template <> struct is_pod <unsigned short>		{ static const int value = 1; };
				template <> struct is_pod <short>				{ static const int value = 1; };
				template <> struct is_pod <unsigned int>		{ static const int value = 1; };
				template <> struct is_pod <int>					{ static const int value = 1; };
				template <> struct is_pod <unsigned long long>	{ static const int value = 1; };
				template <> struct is_pod <long long>			{ static const int value = 1; };
				template <> struct is_pod <unsigned long>		{ static const int value = 1; };
				template <> struct is_pod <long>				{ static const int value = 1; };
				template <> struct is_pod <float>				{ static const int value = 1; };
				template <> struct is_pod <double>				{ static const int value = 1; };
				
				template <typename T> struct has_trivial_constructor
					{
						enum { value = is_pod <T>::value };
					};

				template <typename T> struct has_trivial_copy
					{
						enum { value = is_pod <T>::value };
					};

				template <typename T> struct has_trivial_assign
					{
						enum { value = is_pod <T>::value };
					};

				template <typename T> struct has_trivial_destructor
					{
						enum { value = is_pod <T>::value };
					};
					
				template <typename T> struct is_integral
					{
						static const int value = 0;
					};
					
				template <> struct is_integral <unsigned char>		{ static const int value = 1; };
				template <> struct is_integral <signed char>		{ static const int value = 1; };
				template <> struct is_integral <char>				{ static const int value = 1; };
				template <> struct is_integral <unsigned short>		{ static const int value = 1; };
				template <> struct is_integral <short>				{ static const int value = 1; };
				template <> struct is_integral <unsigned int>		{ static const int value = 1; };
				template <> struct is_integral <int>				{ static const int value = 1; };
				template <> struct is_integral <unsigned long long>	{ static const int value = 1; };
				template <> struct is_integral <long long>			{ static const int value = 1; };
				template <> struct is_integral <unsigned long>		{ static const int value = 1; };
				template <> struct is_integral <long>				{ static const int value = 1; };
				
				template <typename T> struct is_const				{ static const int value = 0; };
				template <typename T> struct is_const <const T>		{ static const int value = 1; };

				template <typename T> struct is_signed
					{
						static const int value = 0;
					};
					
				template <> struct is_signed <signed char>		{ static const int value = 1; };
				template <> struct is_signed <short>			{ static const int value = 1; };
				template <> struct is_signed <int>				{ static const int value = 1; };
				template <> struct is_signed <long long>		{ static const int value = 1; };
				template <> struct is_signed <long>				{ static const int value = 1; };

				template <typename T> struct is_unsigned
					{
						static const int value = 0;
					};
					
				template <> struct is_unsigned <unsigned char>			{ static const int value = 1; };
				template <> struct is_unsigned <unsigned short>			{ static const int value = 1; };
				template <> struct is_unsigned <unsigned int>			{ static const int value = 1; };
				template <> struct is_unsigned <unsigned long long>		{ static const int value = 1; };
				template <> struct is_unsigned <unsigned long>			{ static const int value = 1; };
				
				// if_c
				
				template <bool c, typename T1, typename T2> struct if_c
					{
						typedef T2 type;
					};
				
				template <typename T1, typename T2> struct if_c <true, T1, T2>
					{
						typedef T1 type;
					};		
			}
	}
	
#endif

