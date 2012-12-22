/*
 *  data.h
 *  macstl
 *
 *  Created by Glen Low on Sep 30 2004.
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

#ifndef MACSTL_IMPL_DATA_H
#define MACSTL_IMPL_DATA_H

namespace macstl
	{
		/// Casts from @a lhs to type @a T1, if T1 is compatible with lhs.data ().
		template <typename T1, typename T2>
			INLINE const T1 data_cast (const T2& lhs)
			{
				return T1 ((typename T1::data_type) lhs.data ());
			}
			
		INLINE char data_of (char lhs)								{ return lhs; }
		INLINE signed char data_of (signed char lhs)				{ return lhs; }
		INLINE unsigned char data_of (unsigned char lhs)			{ return lhs; }
		INLINE short data_of (short lhs)							{ return lhs; }
		INLINE unsigned short data_of (unsigned short lhs)			{ return lhs; }
		INLINE int data_of (int lhs)								{ return lhs; }
		INLINE unsigned int data_of (unsigned int lhs)				{ return lhs; }
		INLINE long long data_of (long long lhs)					{ return lhs; }
		INLINE unsigned long long data_of (unsigned long long lhs)	{ return lhs; }
		INLINE float data_of (float lhs)							{ return lhs; }
		INLINE double data_of (double lhs)							{ return lhs; }
		
		template <typename T> INLINE T* data_of (T* lhs)							{ return lhs; }
		template <typename T> INLINE typename T::data_type data_of (const T& lhs)	{ return lhs.data (); }
	}

#endif
