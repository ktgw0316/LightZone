/*
 *  valarray.h
 *  macstl
 *
 *  Created by Glen Low on Mar 14 2003.
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
 
#ifndef MACSTL_VALARRAY_H
#define MACSTL_VALARRAY_H

#include <cstdlib>
#include <cassert>

#include "impl/config.h"
#include "impl/data.h"
#include "impl/meta.h"

#include "algorithm.h"
#include "functional.h"
#include "mmapping.h"
#include "vec.h"

namespace stdext
	{
		class slice;
		class gslice;
		
		template <typename T> class valarray;

		namespace impl
			{
				// forward declares
				
				template <typename Term> class literal_term;
				template <typename Term, template <typename> class UOp> class unary_term;					
				template <typename Term, typename Fn> class apply_term;					
				template <typename LTerm, typename RTerm, template <typename, typename> class BOp> class binary_term;
				template <typename LTerm, typename MTerm, typename RTerm, template <typename, typename, typename> class TOp> class ternary_term;
		
				template <typename Term> class shift_term;
				template <typename Term> class cshift_term;
				
				template <typename Term> class slice_term;
				template <typename Term> class gslice_term;
				template <typename Term, typename BTerm> class mask_term;
				template <typename Term, typename InTerm> class indirect_term;
				
				template <typename T, typename Enable = void> class valarray_base;

			}
	}
	
#include "impl/valarray_algorithm.h"
#include "impl/valarray_base.h"
#include "impl/valarray_function.h"
#include "impl/valarray_vec.h"
#include "impl/valarray_shift.h"
#include "impl/valarray_valarray.h"
#include "impl/valarray_slice.h"
#include "impl/valarray_gslice.h"
#include "impl/valarray_mask.h"
#include "impl/valarray_indirect.h"
#include "impl/valarray_refarray.h"
#include "impl/valarray_statarray.h"
#include "impl/valarray_mmaparray.h"

#endif
