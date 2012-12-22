/*
 *  config.h
 *  macstl
 *
 *  Created by Glen Low on Sep 22 2004.
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

#ifndef MACSTL_IMPL_CONFIG_H
#define MACSTL_IMPL_CONFIG_H

#ifdef __INTEL_COMPILER
#define INLINE __forceinline
#endif

#if defined(__GNUC__) && !defined(__INTEL_COMPILER)
	#if __GNUC__ < 4
		#define NO_CHUNKING_ITERATOR		// use pointer to may_alias type instead
		#define NO_VEC_COPY_CONSTRUCTOR		// use the builtin/trivial copy constructor
											// NOTE: on gcc 4.0+ a non-trivial vec copy doesn't pessimize code on -faltivec + -maltivec
											// but definitely pessimizes code on -faltivec - -maltivec
	#endif
	#define HAS_C99_COMPLEX
	#define INLINE __attribute__((always_inline)) inline
	#if __APPLE_CC__ <= 1
		#define USE_ALTIVEC_H		// use the altivec.h header to define intrinsics
	#endif
	#define USE_C99_VEC_INIT_IN_TEMPL
#endif

#ifdef _MSC_VER
	// maximize inlining
	#pragma inline_depth(255)
	
	#pragma warning(disable:4675)	// don't warn resolved overload through ADL
	#pragma warning(disable:4800)	// don't warn bool conversions
	#pragma warning(disable:4804)	// don't warn unsafe use of bool
	
	#define INLINE __forceinline
#endif

#ifdef __MWERKS__
	// maximize inlining
	#pragma inline_depth(512)
	#pragma inline_max_size(16384)
	#pragma inline_max_total_size(16384)
	#define NO_VEC_COPY_CONSTRUCTOR
	#define USE_CONTEXTUAL_BOOL
#endif

#if defined(__APPLE__) || defined(__linux__)
	#define HAS_LOG2
#endif

#if defined(__APPLE__) || defined(__CYGWIN__) || defined(__linux__)
	#define HAS_MMAP
#endif

#ifndef INLINE
#define INLINE inline
#endif

#endif
