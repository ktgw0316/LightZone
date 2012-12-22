/*
 *  boolean.h
 *  macstl
 *
 *  Created by Glen Low on Dec 27 2002.
 *
 *  Copyright (c) 2002-2005 Pixelglow Software, all rights reserved.
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

#ifndef MACSTL_BOOLEAN_H
#define MACSTL_BOOLEAN_H

#include "impl/config.h"
#include "impl/meta.h"

namespace macstl
	{
		namespace impl
			{
				template <typename T> inline T boolean_of (bool b)
					{
						return -static_cast <T> (b);
					}
					
				template <> inline float boolean_of <float> (bool b)
					{
						union
							{
								int i;
								float f;
							} iun = {-static_cast <int> (b)};	// assume sizeof (int) == sizeof (float)
						return iun.f;
					}

				template <> inline double boolean_of <double> (bool b)
					{
						union
							{
								long long i;
								double d;
							} iun = {-static_cast <long long> (b)};	// assume sizeof (long long) == sizeof (double)
						return iun.d;
					}
			}
			
		/// Sized boolean.
		
		/// A bool-valued object with a definite size, where sizeof (boolean \<T\>) == sizeof (T).
		/// It either has all bits zero, representing false, or all bits one, representing true -- the class defends
		/// this invariant. The type corresponds to the scalar type of an SIMD boolean type e.g. in Altivec, macstl::boolean \<char\>
		/// corresponds to the scalar type of Altivec __vector bool char.
		///
		/// @param	T	The type with same size. Usually an integral type.
		///
		/// @header	#include <macstl/boolean.h>
		///
		/// @see	macstl::vec
		
		template <typename T> class boolean
			{
				public:
					/// The underlying data type.
					typedef T data_type;
					
					/// Constructs with given @a value.
					boolean (bool value = false): value_ (impl::boolean_of <T> (value))
						{
						}
					
					/// Constructs from @a other boolean.
					template <typename T2> explicit boolean (const boolean <T2>& value): value_ (value.value_)
						{
						}
					
					/// Assigns the @a other value.
					boolean& operator= (bool other)
						{
							value_ = impl::boolean_of <T> (other);
							return *this;
						}

					/// Assigns the @a other boolean.
					template <typename T2> boolean& operator= (boolean <T2> value)
						{
							value_ = value.value_;
							return *this;
						}
					
					/// Gets the data.
					T data () const
						{
							return value_;
						}

					const boolean operator+ () const		{ return +static_cast <bool> (value_); }
					const boolean operator- () const		{ return -static_cast <bool> (value_); }
					const boolean operator~ () const		{ return ~static_cast <bool> (value_); }
					
					bool operator! () const					{ return !static_cast <bool> (value_); }

					friend const boolean operator* (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) * static_cast <bool> (rhs.value_); }
					friend const boolean operator/ (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) / static_cast <bool> (rhs.value_); }
					friend const boolean operator% (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) % static_cast <bool> (rhs.value_); }
					friend const boolean operator+ (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) + static_cast <bool> (rhs.value_); }
					friend const boolean operator- (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) - static_cast <bool> (rhs.value_); }
					friend const boolean operator^ (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) ^ static_cast <bool> (rhs.value_); }
					friend const boolean operator& (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) & static_cast <bool> (rhs.value_); }
					friend const boolean operator| (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) | static_cast <bool> (rhs.value_); }
					friend const boolean operator<< (const boolean& lhs, const boolean& rhs)	{ return static_cast <bool> (lhs.value_) << static_cast <bool> (rhs.value_); }
					friend const boolean operator>> (const boolean& lhs, const boolean& rhs)	{ return static_cast <bool> (lhs.value_) >> static_cast <bool> (rhs.value_); }

					friend bool operator== (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) == static_cast <bool> (rhs.value_); }
					friend bool operator!= (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) != static_cast <bool> (rhs.value_); }
					friend bool operator< (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) < static_cast <bool> (rhs.value_); }
					friend bool operator> (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) > static_cast <bool> (rhs.value_); }
					friend bool operator<= (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) <= static_cast <bool> (rhs.value_); }
					friend bool operator>= (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) >= static_cast <bool> (rhs.value_); }
					friend bool operator&& (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) && static_cast <bool> (rhs.value_); }
					friend bool operator|| (const boolean& lhs, const boolean& rhs)		{ return static_cast <bool> (lhs.value_) || static_cast <bool> (rhs.value_); }
					
					friend const boolean abs (const boolean& lhs)						{ return lhs; }
					friend const boolean mulhi (const boolean&, const boolean&)			{ return boolean (false); }

				private:
					T value_;
			};

		template <typename T, typename CharT, typename Traits>
			inline std::basic_istream <CharT, Traits>& operator>> (std::basic_istream <CharT, Traits>& is, boolean <T>& x)
			{
				bool b;
				is >> b;
				x = b;
				return is;
			}
			
		template <typename T, typename CharT, typename Traits>
			inline std::basic_ostream <CharT, Traits>& operator<< (std::basic_ostream <CharT, Traits>& os, const boolean <T>& x)
			{
				return os << static_cast <bool> (x.data ());			
			}
			
		namespace impl
			{
				template <std::size_t size> struct boolean_sized;
				
				template <> struct boolean_sized <1>	{ typedef boolean <char> type; };
				template <> struct boolean_sized <2>	{ typedef boolean <short> type; };
				template <> struct boolean_sized <4>	{ typedef boolean <int> type; };
			}
	}
	
namespace stdext
	{
		// forward declare of selection template from functional.h, then specialized for use with sized booleans
		
		template <typename T1, typename T2, typename T3> struct selection;
		
		template <typename T1, typename T2> struct selection <macstl::boolean <T1>, T2, T2>
			{
				typedef macstl::boolean <T1> first_argument_type;
				typedef T2 second_argument_type;
				typedef T2 third_argument_type;
				typedef T2 result_type;
				
				const T2 operator() (const macstl::boolean <T1>& lhs, const T2& mhs, const T2& rhs) const
					{
						return lhs.data () ? mhs : rhs;
					}
			};
	}
#endif
