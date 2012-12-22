/*
 *  typelist.h
 *  macstl
 *
 *  Created by Glen Low on Dec 15 2004.
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

#ifndef MACSTL_TYPELIST_H
#define MACSTL_TYPELIST_H

namespace macstl
	{
		namespace impl
			{
				struct empty;
			}
			
		/// Compile-time list of types.
		
		/// Like Modern C++ Design's typelist, but doesn't use nested templates so the client use is a little easier on the eye.
		/// The main innovation is that recursion happens through the even and odd members -- this allows a faster and deeper compile-time binary splitting
		/// instead of a head/tail idiom, you only need to stop the recursion with an specialization on a single element or empty typelist.
		///
		/// @param	T1...	A macstl::typelist of COM interfaces to implement.
		///
		/// @header	#include <macstl/typelist.h>

		template <
			typename T1 = impl::empty,
			typename T2 = impl::empty,
			typename T3 = impl::empty,
			typename T4 = impl::empty,
			typename T5 = impl::empty,
			typename T6 = impl::empty,
			typename T7 = impl::empty,
			typename T8 = impl::empty,
			typename T9 = impl::empty,
			typename T10 = impl::empty,
			typename T11 = impl::empty,
			typename T12 = impl::empty,
			typename T13 = impl::empty,
			typename T14 = impl::empty,
			typename T15 = impl::empty,
			typename T16 = impl::empty,
			typename T17 = impl::empty,
			typename T18 = impl::empty,
			typename T19 = impl::empty,
			typename T20 = impl::empty,
			typename T21 = impl::empty,
			typename T22 = impl::empty,
			typename T23 = impl::empty,
			typename T24 = impl::empty,
			typename T25 = impl::empty,
			typename T26 = impl::empty,
			typename T27 = impl::empty,
			typename T28 = impl::empty,
			typename T29 = impl::empty,
			typename T30 = impl::empty,
			typename T31 = impl::empty,
			typename T32 = impl::empty>
			struct typelist
			{
				/// The first type.
				typedef T1 head;
				
				/// A typelist with all the types except the first.
				typedef typelist <T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24, T25, T27, T28, T29, T30, T31, T32> tail;
				
				/// A typelist with the odd types.
				typedef typelist <T1, T3, T5, T7, T9, T11, T13, T15, T17, T19, T21, T23, T25, T27, T29, T31> odd;
				
				/// A typelist with the even types.
				typedef typelist <T2, T4, T6, T8, T10, T12, T14, T16, T18, T20, T22, T24, T26, T28, T30, T32> even;
			};
	}
	
#endif

