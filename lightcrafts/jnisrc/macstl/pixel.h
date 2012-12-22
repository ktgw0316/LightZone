/*
 *  pixel.h
 *  macstl
 *
 *  Created by Glen Low on Sep 13 2003.
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

#ifndef MACSTL_PIXEL_H
#define MACSTL_PIXEL_H

namespace macstl
	{
		/// 1/5/5/5 ARGB pixel.
		
		/// Stores a 16-bit ARGB color value, where the alpha occupies 1 bit, and each color channel occupies 5 bits.
		/// The type corresponds to the scalar type of an Altivec __vector pixel.
		///
		/// @header	#include <macstl/pixel.h>
		///
		/// @see	macstl::pixel
		
		struct pixel
			{
				/// Alpha value, 0 or 1.
				unsigned short a: 1;
				
				/// Red value, 0 to 31.
				unsigned short r: 5;
				
				/// Green value, 0 to 31.
				unsigned short g: 5;
				
				/// Blue value, 0 to 31.
				unsigned short b: 5;
				
				/// Constructs a pixel with @a alpha, @a red, @a green and @a blue values.				
				pixel (unsigned short alpha, unsigned short red, unsigned short green, unsigned short blue):
					a (alpha), r (red), g (green), b (blue)
					{
					}
			};
			
		/// @relates pixel
		/// @brief Tests whether @a left is equal to @a right.
		inline bool operator== (pixel left, pixel right)
			{
				return *reinterpret_cast <unsigned short*> (&left) == *reinterpret_cast <unsigned short*> (&right);
			}

		/// @relates pixel
		/// @brief Tests whether @a left is not equal to @a right.
		inline bool operator!= (pixel left, pixel right)
			{
				return *reinterpret_cast <unsigned short*> (&left) != *reinterpret_cast <unsigned short*> (&right);
			}

	}
	
namespace stdext
	{
		namespace impl
			{
				template <> struct has_trivial_copy <macstl::pixel>			{ enum { value = true }; };
				template <> struct has_trivial_assign <macstl::pixel>		{ enum { value = true }; };
				template <> struct has_trivial_destructor <macstl::pixel>	{ enum { value = true }; };
			}
	}

#endif

