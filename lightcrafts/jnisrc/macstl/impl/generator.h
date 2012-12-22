/*
 *  generator.h
 *  macstl
 *
 *  Created by Glen Low on Nov 23 2004.
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

#ifndef MACSTL_IMPL_GENERATOR_H
#define MACSTL_IMPL_GENERATOR_H

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (0); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000F8U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-8); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000F9U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-7); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000FAU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-6); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000FBU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-5); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000FCU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-4); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000FDU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-3); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000FEU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-2); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x000000FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-1); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F0F0U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-16); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F1F1U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-15); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F2F2U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-14); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F3F3U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-13); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F4F4U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-12); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F5F5U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-11); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F6F6U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-10); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000F7F7U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-9); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF0U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-16); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF1U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-15); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF2U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-14); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF3U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-13); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF4U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-12); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF5U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-11); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF6U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-10); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFF7U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-9); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000000U, 0x00000000U, 0x0000FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-16); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x00000001U, 0x00000000U, 0x00000001U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (1); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000002U, 0x00000000U, 0x00000002U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (2); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000003U, 0x00000000U, 0x00000003U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (3); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000004U, 0x00000000U, 0x00000004U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (4); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000005U, 0x00000000U, 0x00000005U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (5); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000006U, 0x00000000U, 0x00000006U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (6); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000007U, 0x00000000U, 0x00000007U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (7); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000008U, 0x00000000U, 0x00000008U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (8); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000009U, 0x00000000U, 0x00000009U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (9); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x0000000AU, 0x00000000U, 0x0000000AU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (10); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x0000000BU, 0x00000000U, 0x0000000BU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (11); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x0000000CU, 0x00000000U, 0x0000000CU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (12); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x0000000DU, 0x00000000U, 0x0000000DU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (13); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x0000000EU, 0x00000000U, 0x0000000EU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (14); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x0000000FU, 0x00000000U, 0x0000000FU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (15); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0x00000000U, 0x00000101U, 0x00000000U, 0x00000101U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (1); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000202U, 0x00000000U, 0x00000202U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (2); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000303U, 0x00000000U, 0x00000303U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (3); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000404U, 0x00000000U, 0x00000404U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (4); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000505U, 0x00000000U, 0x00000505U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (5); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000606U, 0x00000000U, 0x00000606U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (6); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000707U, 0x00000000U, 0x00000707U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (7); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000808U, 0x00000000U, 0x00000808U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (8); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000909U, 0x00000000U, 0x00000909U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (9); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000A0AU, 0x00000000U, 0x00000A0AU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (10); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000B0BU, 0x00000000U, 0x00000B0BU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (11); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000C0CU, 0x00000000U, 0x00000C0CU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (12); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000D0DU, 0x00000000U, 0x00000D0DU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (13); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000E0EU, 0x00000000U, 0x00000E0EU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (14); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00000F0FU, 0x00000000U, 0x00000F0FU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (15); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0x00000000U, 0x00010001U, 0x00000000U, 0x00010001U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (1); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00020002U, 0x00000000U, 0x00020002U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (2); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00030003U, 0x00000000U, 0x00030003U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (3); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00040004U, 0x00000000U, 0x00040004U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (4); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00050005U, 0x00000000U, 0x00050005U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (5); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00060006U, 0x00000000U, 0x00060006U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (6); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00070007U, 0x00000000U, 0x00070007U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (7); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00080008U, 0x00000000U, 0x00080008U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (8); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x00090009U, 0x00000000U, 0x00090009U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (9); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x000A000AU, 0x00000000U, 0x000A000AU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (10); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x000B000BU, 0x00000000U, 0x000B000BU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (11); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x000C000CU, 0x00000000U, 0x000C000CU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (12); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x000D000DU, 0x00000000U, 0x000D000DU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (13); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x000E000EU, 0x00000000U, 0x000E000EU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (14); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x000F000FU, 0x00000000U, 0x000F000FU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (15); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0x00000000U, 0x08000000U, 0x08000000U, 0x08000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (8); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x09000000U, 0x09000000U, 0x09000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (9); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x0A000000U, 0x0A000000U, 0x0A000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (10); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x0B000000U, 0x0B000000U, 0x0B000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (11); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x0C000000U, 0x0C000000U, 0x0C000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (12); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x0D000000U, 0x0D000000U, 0x0D000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (13); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x0E000000U, 0x0E000000U, 0x0E000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (14); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000000U, 0x0F000000U, 0x0F000000U, 0x0F000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (15); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000001U, 0x00000001U, 0x00000001U, 0x00000001U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (1); }
	};

template <> struct generator <0x00000002U, 0x00000002U, 0x00000002U, 0x00000002U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (2); }
	};

template <> struct generator <0x00000003U, 0x00000003U, 0x00000003U, 0x00000003U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (3); }
	};

template <> struct generator <0x00000004U, 0x00000004U, 0x00000004U, 0x00000004U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (4); }
	};

template <> struct generator <0x00000005U, 0x00000005U, 0x00000005U, 0x00000005U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (5); }
	};

template <> struct generator <0x00000006U, 0x00000006U, 0x00000006U, 0x00000006U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (6); }
	};

template <> struct generator <0x00000007U, 0x00000007U, 0x00000007U, 0x00000007U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (7); }
	};

template <> struct generator <0x00000008U, 0x00000008U, 0x00000008U, 0x00000008U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (8); }
	};

template <> struct generator <0x00000009U, 0x00000009U, 0x00000009U, 0x00000009U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (9); }
	};

template <> struct generator <0x0000000AU, 0x0000000AU, 0x0000000AU, 0x0000000AU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (10); }
	};

template <> struct generator <0x0000000BU, 0x0000000BU, 0x0000000BU, 0x0000000BU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (11); }
	};

template <> struct generator <0x0000000CU, 0x0000000CU, 0x0000000CU, 0x0000000CU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (12); }
	};

template <> struct generator <0x0000000DU, 0x0000000DU, 0x0000000DU, 0x0000000DU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (13); }
	};

template <> struct generator <0x0000000EU, 0x0000000EU, 0x0000000EU, 0x0000000EU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (14); }
	};

template <> struct generator <0x0000000FU, 0x0000000FU, 0x0000000FU, 0x0000000FU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (15); }
	};

template <> struct generator <0x00000100U, 0x00000100U, 0x00000100U, 0x00000100U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (1); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000101U, 0x00000101U, 0x00000101U, 0x00000101U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (1); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000200U, 0x00000200U, 0x00000200U, 0x00000200U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (2); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000202U, 0x00000202U, 0x00000202U, 0x00000202U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (2); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000300U, 0x00000300U, 0x00000300U, 0x00000300U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (3); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000303U, 0x00000303U, 0x00000303U, 0x00000303U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (3); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000400U, 0x00000400U, 0x00000400U, 0x00000400U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (4); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000404U, 0x00000404U, 0x00000404U, 0x00000404U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (4); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000500U, 0x00000500U, 0x00000500U, 0x00000500U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (5); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000505U, 0x00000505U, 0x00000505U, 0x00000505U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (5); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000600U, 0x00000600U, 0x00000600U, 0x00000600U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (6); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000606U, 0x00000606U, 0x00000606U, 0x00000606U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (6); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000700U, 0x00000700U, 0x00000700U, 0x00000700U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (7); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000707U, 0x00000707U, 0x00000707U, 0x00000707U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (7); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000800U, 0x00000800U, 0x00000800U, 0x00000800U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (8); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000800U, 0x08000800U, 0x08000800U, 0x08000800U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (8); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000801U, 0x00000801U, 0x00000801U, 0x00000801U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (1); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00000808U, 0x00000808U, 0x00000808U, 0x00000808U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (8); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000900U, 0x00000900U, 0x00000900U, 0x00000900U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (9); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000900U, 0x09000900U, 0x09000900U, 0x09000900U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (9); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000909U, 0x00000909U, 0x00000909U, 0x00000909U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (9); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000A00U, 0x00000A00U, 0x00000A00U, 0x00000A00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (10); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000A00U, 0x0A000A00U, 0x0A000A00U, 0x0A000A00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (10); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000A0AU, 0x00000A0AU, 0x00000A0AU, 0x00000A0AU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (10); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000B00U, 0x00000B00U, 0x00000B00U, 0x00000B00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (11); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000B00U, 0x0B000B00U, 0x0B000B00U, 0x0B000B00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (11); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000B0BU, 0x00000B0BU, 0x00000B0BU, 0x00000B0BU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (11); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000C00U, 0x00000C00U, 0x00000C00U, 0x00000C00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (12); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000C00U, 0x0C000C00U, 0x0C000C00U, 0x0C000C00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (12); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000C0CU, 0x00000C0CU, 0x00000C0CU, 0x00000C0CU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (12); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000D00U, 0x00000D00U, 0x00000D00U, 0x00000D00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (13); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000D00U, 0x0D000D00U, 0x0D000D00U, 0x0D000D00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (13); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000D0DU, 0x00000D0DU, 0x00000D0DU, 0x00000D0DU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (13); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000E00U, 0x00000E00U, 0x00000E00U, 0x00000E00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (14); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000E00U, 0x0E000E00U, 0x0E000E00U, 0x0E000E00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (14); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000E0EU, 0x00000E0EU, 0x00000E0EU, 0x00000E0EU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (14); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00000F00U, 0x00000F00U, 0x00000F00U, 0x00000F00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (15); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x00000F00U, 0x0F000F00U, 0x0F000F00U, 0x0F000F00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (15); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00000F0FU, 0x00000F0FU, 0x00000F0FU, 0x00000F0FU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (15); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0x00001002U, 0x00001002U, 0x00001002U, 0x00001002U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (2); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00001803U, 0x00001803U, 0x00001803U, 0x00001803U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (3); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00010000U, 0x00010000U, 0x00010000U, 0x00010000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (1); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00010001U, 0x00010001U, 0x00010001U, 0x00010001U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (1); }
	};

template <> struct generator <0x00010004U, 0x00010004U, 0x00010004U, 0x00010004U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (4); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00010805U, 0x00010805U, 0x00010805U, 0x00010805U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (5); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00011006U, 0x00011006U, 0x00011006U, 0x00011006U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (6); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00011807U, 0x00011807U, 0x00011807U, 0x00011807U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (7); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00020000U, 0x00020000U, 0x00020000U, 0x00020000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (2); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00020002U, 0x00020002U, 0x00020002U, 0x00020002U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (2); }
	};

template <> struct generator <0x00020008U, 0x00020008U, 0x00020008U, 0x00020008U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (8); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00020809U, 0x00020809U, 0x00020809U, 0x00020809U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (9); return vec_vupkhpx (v); }
	};

template <> struct generator <0x0002100AU, 0x0002100AU, 0x0002100AU, 0x0002100AU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (10); return vec_vupkhpx (v); }
	};

template <> struct generator <0x0002180BU, 0x0002180BU, 0x0002180BU, 0x0002180BU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (11); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00030000U, 0x00030000U, 0x00030000U, 0x00030000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (3); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00030003U, 0x00030003U, 0x00030003U, 0x00030003U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (3); }
	};

template <> struct generator <0x0003000CU, 0x0003000CU, 0x0003000CU, 0x0003000CU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (12); return vec_vupkhpx (v); }
	};

template <> struct generator <0x0003080DU, 0x0003080DU, 0x0003080DU, 0x0003080DU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (13); return vec_vupkhpx (v); }
	};

template <> struct generator <0x0003100EU, 0x0003100EU, 0x0003100EU, 0x0003100EU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (14); return vec_vupkhpx (v); }
	};

template <> struct generator <0x0003180FU, 0x0003180FU, 0x0003180FU, 0x0003180FU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (15); return vec_vupkhpx (v); }
	};

template <> struct generator <0x00040000U, 0x00040000U, 0x00040000U, 0x00040000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (4); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00040004U, 0x00040004U, 0x00040004U, 0x00040004U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (4); }
	};

template <> struct generator <0x00050000U, 0x00050000U, 0x00050000U, 0x00050000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (5); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00050005U, 0x00050005U, 0x00050005U, 0x00050005U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (5); }
	};

template <> struct generator <0x00060000U, 0x00060000U, 0x00060000U, 0x00060000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (6); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00060006U, 0x00060006U, 0x00060006U, 0x00060006U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (6); }
	};

template <> struct generator <0x00070000U, 0x00070000U, 0x00070000U, 0x00070000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (7); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00070007U, 0x00070007U, 0x00070007U, 0x00070007U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (7); }
	};

template <> struct generator <0x00080000U, 0x00080000U, 0x00080000U, 0x00080000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (8); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00080008U, 0x00080008U, 0x00080008U, 0x00080008U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (8); }
	};

template <> struct generator <0x00080808U, 0x08080808U, 0x08080808U, 0x08080808U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (8); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x00090000U, 0x00090000U, 0x00090000U, 0x00090000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (9); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x00090009U, 0x00090009U, 0x00090009U, 0x00090009U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (9); }
	};

template <> struct generator <0x00090909U, 0x09090909U, 0x09090909U, 0x09090909U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (9); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x000A0000U, 0x000A0000U, 0x000A0000U, 0x000A0000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (10); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x000A000AU, 0x000A000AU, 0x000A000AU, 0x000A000AU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (10); }
	};

template <> struct generator <0x000A0A0AU, 0x0A0A0A0AU, 0x0A0A0A0AU, 0x0A0A0A0AU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (10); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x000B0000U, 0x000B0000U, 0x000B0000U, 0x000B0000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (11); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x000B000BU, 0x000B000BU, 0x000B000BU, 0x000B000BU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (11); }
	};

template <> struct generator <0x000B0B0BU, 0x0B0B0B0BU, 0x0B0B0B0BU, 0x0B0B0B0BU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (11); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x000C0000U, 0x000C0000U, 0x000C0000U, 0x000C0000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (12); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x000C000CU, 0x000C000CU, 0x000C000CU, 0x000C000CU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (12); }
	};

template <> struct generator <0x000C0C0CU, 0x0C0C0C0CU, 0x0C0C0C0CU, 0x0C0C0C0CU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (12); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x000D0000U, 0x000D0000U, 0x000D0000U, 0x000D0000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (13); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x000D000DU, 0x000D000DU, 0x000D000DU, 0x000D000DU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (13); }
	};

template <> struct generator <0x000D0D0DU, 0x0D0D0D0DU, 0x0D0D0D0DU, 0x0D0D0D0DU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (13); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x000E0000U, 0x000E0000U, 0x000E0000U, 0x000E0000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (14); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x000E000EU, 0x000E000EU, 0x000E000EU, 0x000E000EU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (14); }
	};

template <> struct generator <0x000E0E0EU, 0x0E0E0E0EU, 0x0E0E0E0EU, 0x0E0E0E0EU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (14); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x000F0000U, 0x000F0000U, 0x000F0000U, 0x000F0000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (15); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0x000F000FU, 0x000F000FU, 0x000F000FU, 0x000F000FU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (15); }
	};

template <> struct generator <0x000F0F0FU, 0x0F0F0F0FU, 0x0F0F0F0FU, 0x0F0F0F0FU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (15); return vec_vsro (v, (vuc) v); }
	};

template <> struct generator <0x01000000U, 0x01000000U, 0x01000000U, 0x01000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (1); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x01000100U, 0x01000100U, 0x01000100U, 0x01000100U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (1); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x01010101U, 0x01010101U, 0x01010101U, 0x01010101U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (1); }
	};

template <> struct generator <0x02000000U, 0x02000000U, 0x02000000U, 0x02000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (2); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x02000200U, 0x02000200U, 0x02000200U, 0x02000200U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (2); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x02020202U, 0x02020202U, 0x02020202U, 0x02020202U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (2); }
	};

template <> struct generator <0x03000000U, 0x03000000U, 0x03000000U, 0x03000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (3); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x03000300U, 0x03000300U, 0x03000300U, 0x03000300U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (3); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x03030303U, 0x03030303U, 0x03030303U, 0x03030303U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (3); }
	};

template <> struct generator <0x04000000U, 0x04000000U, 0x04000000U, 0x04000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (4); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x04000400U, 0x04000400U, 0x04000400U, 0x04000400U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (4); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x04010401U, 0x04010401U, 0x04010401U, 0x04010401U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (8); return (vui) vec_vpkpx (v, v); }
	};

template <> struct generator <0x04040404U, 0x04040404U, 0x04040404U, 0x04040404U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (4); }
	};

template <> struct generator <0x04210421U, 0x04210421U, 0x04210421U, 0x04210421U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (8); return (vui) vec_vpkpx (v, v); }
	};

template <> struct generator <0x05000000U, 0x05000000U, 0x05000000U, 0x05000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (5); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x05000500U, 0x05000500U, 0x05000500U, 0x05000500U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (5); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x05050505U, 0x05050505U, 0x05050505U, 0x05050505U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (5); }
	};

template <> struct generator <0x06000000U, 0x06000000U, 0x06000000U, 0x06000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (6); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x06000600U, 0x06000600U, 0x06000600U, 0x06000600U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (6); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x06060606U, 0x06060606U, 0x06060606U, 0x06060606U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (6); }
	};

template <> struct generator <0x07000000U, 0x07000000U, 0x07000000U, 0x07000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (7); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x07000700U, 0x07000700U, 0x07000700U, 0x07000700U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (7); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x07070707U, 0x07070707U, 0x07070707U, 0x07070707U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (7); }
	};

template <> struct generator <0x08000000U, 0x08000000U, 0x08000000U, 0x08000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (8); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x08000800U, 0x08000800U, 0x08000800U, 0x08000800U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (8); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x08080808U, 0x08080808U, 0x08080808U, 0x08080800U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (8); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x08080808U, 0x08080808U, 0x08080808U, 0x08080808U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (8); }
	};

template <> struct generator <0x09000000U, 0x09000000U, 0x09000000U, 0x09000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (9); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x09000900U, 0x09000900U, 0x09000900U, 0x09000900U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (9); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x09090909U, 0x09090909U, 0x09090909U, 0x09090900U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (9); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x09090909U, 0x09090909U, 0x09090909U, 0x09090909U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (9); }
	};

template <> struct generator <0x0A000000U, 0x0A000000U, 0x0A000000U, 0x0A000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (10); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x0A000A00U, 0x0A000A00U, 0x0A000A00U, 0x0A000A00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (10); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x0A0A0A0AU, 0x0A0A0A0AU, 0x0A0A0A0AU, 0x0A0A0A00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (10); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x0A0A0A0AU, 0x0A0A0A0AU, 0x0A0A0A0AU, 0x0A0A0A0AU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (10); }
	};

template <> struct generator <0x0B000000U, 0x0B000000U, 0x0B000000U, 0x0B000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (11); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x0B000B00U, 0x0B000B00U, 0x0B000B00U, 0x0B000B00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (11); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x0B0B0B0BU, 0x0B0B0B0BU, 0x0B0B0B0BU, 0x0B0B0B00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (11); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x0B0B0B0BU, 0x0B0B0B0BU, 0x0B0B0B0BU, 0x0B0B0B0BU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (11); }
	};

template <> struct generator <0x0C000000U, 0x0C000000U, 0x0C000000U, 0x0C000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (12); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x0C000C00U, 0x0C000C00U, 0x0C000C00U, 0x0C000C00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (12); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x0C0C0C0CU, 0x0C0C0C0CU, 0x0C0C0C0CU, 0x0C0C0C00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (12); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x0C0C0C0CU, 0x0C0C0C0CU, 0x0C0C0C0CU, 0x0C0C0C0CU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (12); }
	};

template <> struct generator <0x0D000000U, 0x0D000000U, 0x0D000000U, 0x0D000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (13); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x0D000D00U, 0x0D000D00U, 0x0D000D00U, 0x0D000D00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (13); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x0D0D0D0DU, 0x0D0D0D0DU, 0x0D0D0D0DU, 0x0D0D0D00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (13); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x0D0D0D0DU, 0x0D0D0D0DU, 0x0D0D0D0DU, 0x0D0D0D0DU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (13); }
	};

template <> struct generator <0x0E000000U, 0x0E000000U, 0x0E000000U, 0x0E000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (14); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x0E000E00U, 0x0E000E00U, 0x0E000E00U, 0x0E000E00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (14); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x0E0E0E0EU, 0x0E0E0E0EU, 0x0E0E0E0EU, 0x0E0E0E00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (14); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x0E0E0E0EU, 0x0E0E0E0EU, 0x0E0E0E0EU, 0x0E0E0E0EU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (14); }
	};

template <> struct generator <0x0F000000U, 0x0F000000U, 0x0F000000U, 0x0F000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (15); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0x0F000F00U, 0x0F000F00U, 0x0F000F00U, 0x0F000F00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (15); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0x0F0F0F0FU, 0x0F0F0F0FU, 0x0F0F0F0FU, 0x0F0F0F00U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (15); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0x0F0F0F0FU, 0x0F0F0F0FU, 0x0F0F0F0FU, 0x0F0F0F0FU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (15); }
	};

template <> struct generator <0x7BDE7BDEU, 0x7BDE7BDEU, 0x7BDE7BDEU, 0x7BDE7BDEU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-16); return (vui) vec_vpkpx (v, v); }
	};

template <> struct generator <0x7F7F7F7FU, 0x7F7F7F7FU, 0x7F7F7F7FU, 0x7F7F7F7FU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (1); return (vui) vec_vpkshss (v, v); }
	};

template <> struct generator <0x7FFF7FFFU, 0x7FFF7FFFU, 0x7FFF7FFFU, 0x7FFF7FFFU>
	{
		static INLINE vui call () { vsi v = (vsi) vec_vspltish (1); return (vui) vec_vpkswss (v, v); }
	};

template <> struct generator <0x80008000U, 0x80008000U, 0x80008000U, 0x80008000U>
	{
		static INLINE vui call () { vsi v = (vsi) vec_vspltish (-16); return (vui) vec_vpkswss (v, v); }
	};

template <> struct generator <0x80808080U, 0x80808080U, 0x80808080U, 0x80808080U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-16); return (vui) vec_vpkshss (v, v); }
	};

template <> struct generator <0x84218421U, 0x84218421U, 0x84218421U, 0x84218421U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (9); return (vui) vec_vpkpx (v, v); }
	};

template <> struct generator <0xF0F00000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-16); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF0F0F0F0U, 0xF0F0F0F0U, 0xF0F0F0F0U, 0xF0F0F0F0U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-16); }
	};

template <> struct generator <0xF0F0F0FFU, 0xF0F0F0FFU, 0xF0F0F0FFU, 0xF0F0F0FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-16); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF0FFF0FFU, 0xF0FFF0FFU, 0xF0FFF0FFU, 0xF0FFF0FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-16); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF0FFFFFFU, 0xF0FFFFFFU, 0xF0FFFFFFU, 0xF0FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-16); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF1F10000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-15); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF1F1F1F1U, 0xF1F1F1F1U, 0xF1F1F1F1U, 0xF1F1F1F1U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-15); }
	};

template <> struct generator <0xF1F1F1FFU, 0xF1F1F1FFU, 0xF1F1F1FFU, 0xF1F1F1FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-15); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF1FFF1FFU, 0xF1FFF1FFU, 0xF1FFF1FFU, 0xF1FFF1FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-15); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xF1FFFFFFU, 0xF1FFFFFFU, 0xF1FFFFFFU, 0xF1FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-15); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF2F20000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-14); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF2F2F2F2U, 0xF2F2F2F2U, 0xF2F2F2F2U, 0xF2F2F2F2U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-14); }
	};

template <> struct generator <0xF2F2F2FFU, 0xF2F2F2FFU, 0xF2F2F2FFU, 0xF2F2F2FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-14); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF2FFF2FFU, 0xF2FFF2FFU, 0xF2FFF2FFU, 0xF2FFF2FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-14); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF2FFFFFFU, 0xF2FFFFFFU, 0xF2FFFFFFU, 0xF2FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-14); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF3F30000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-13); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF3F3F3F3U, 0xF3F3F3F3U, 0xF3F3F3F3U, 0xF3F3F3F3U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-13); }
	};

template <> struct generator <0xF3FFF3FFU, 0xF3FFF3FFU, 0xF3FFF3FFU, 0xF3FFF3FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-13); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xF3FFFFFFU, 0xF3FFFFFFU, 0xF3FFFFFFU, 0xF3FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-13); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF4F40000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-12); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF4F4F4F4U, 0xF4F4F4F4U, 0xF4F4F4F4U, 0xF4F4F4F4U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-12); }
	};

template <> struct generator <0xF4F4F4FFU, 0xF4F4F4FFU, 0xF4F4F4FFU, 0xF4F4F4FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-12); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF4FFF4FFU, 0xF4FFF4FFU, 0xF4FFF4FFU, 0xF4FFF4FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-12); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF4FFFFFFU, 0xF4FFFFFFU, 0xF4FFFFFFU, 0xF4FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-12); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF5F50000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-11); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF5F5F5F5U, 0xF5F5F5F5U, 0xF5F5F5F5U, 0xF5F5F5F5U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-11); }
	};

template <> struct generator <0xF5F5F5FFU, 0xF5F5F5FFU, 0xF5F5F5FFU, 0xF5F5F5FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-11); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF5FFF5FFU, 0xF5FFF5FFU, 0xF5FFF5FFU, 0xF5FFF5FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-11); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xF5FFFFFFU, 0xF5FFFFFFU, 0xF5FFFFFFU, 0xF5FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-11); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF6F60000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-10); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF6F6F6F6U, 0xF6F6F6F6U, 0xF6F6F6F6U, 0xF6F6F6F6U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-10); }
	};

template <> struct generator <0xF6F6F6FFU, 0xF6F6F6FFU, 0xF6F6F6FFU, 0xF6F6F6FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-10); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF6FFF6FFU, 0xF6FFF6FFU, 0xF6FFF6FFU, 0xF6FFF6FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-10); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF6FFFFFFU, 0xF6FFFFFFU, 0xF6FFFFFFU, 0xF6FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-10); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF7F70000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-9); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF7F7F7F7U, 0xF7F7F7F7U, 0xF7F7F7F7U, 0xF7F7F7F7U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-9); }
	};

template <> struct generator <0xF7FFF7FFU, 0xF7FFF7FFU, 0xF7FFF7FFU, 0xF7FFF7FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-9); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xF7FFFFFFU, 0xF7FFFFFFU, 0xF7FFFFFFU, 0xF7FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-9); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF8000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-8); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF8F8F8F8U, 0xF8F8F8F8U, 0xF8F8F8F8U, 0xF8F8F8F8U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-8); }
	};

template <> struct generator <0xF8F8F8FFU, 0xF8F8F8FFU, 0xF8F8F8FFU, 0xF8F8F8FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-8); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF8FFF8FFU, 0xF8FFF8FFU, 0xF8FFF8FFU, 0xF8FFF8FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-8); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF8FFFFFFU, 0xF8FFFFFFU, 0xF8FFFFFFU, 0xF8FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-8); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xF9000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-7); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xF9F9F9F9U, 0xF9F9F9F9U, 0xF9F9F9F9U, 0xF9F9F9F9U>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-7); }
	};

template <> struct generator <0xF9F9F9FFU, 0xF9F9F9FFU, 0xF9F9F9FFU, 0xF9F9F9FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-7); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xF9FFF9FFU, 0xF9FFF9FFU, 0xF9FFF9FFU, 0xF9FFF9FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-7); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xF9FFFFFFU, 0xF9FFFFFFU, 0xF9FFFFFFU, 0xF9FFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-7); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xFA000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-6); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFAFAFAFAU, 0xFAFAFAFAU, 0xFAFAFAFAU, 0xFAFAFAFAU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-6); }
	};

template <> struct generator <0xFAFAFAFFU, 0xFAFAFAFFU, 0xFAFAFAFFU, 0xFAFAFAFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-6); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xFAFFFAFFU, 0xFAFFFAFFU, 0xFAFFFAFFU, 0xFAFFFAFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-6); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xFAFFFFFFU, 0xFAFFFFFFU, 0xFAFFFFFFU, 0xFAFFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-6); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xFB000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-5); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFBDEFBDEU, 0xFBDEFBDEU, 0xFBDEFBDEU, 0xFBDEFBDEU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisb (-15); return (vui) vec_vpkpx (v, v); }
	};

template <> struct generator <0xFBFBFBFBU, 0xFBFBFBFBU, 0xFBFBFBFBU, 0xFBFBFBFBU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-5); }
	};

template <> struct generator <0xFBFEFBFEU, 0xFBFEFBFEU, 0xFBFEFBFEU, 0xFBFEFBFEU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-16); return (vui) vec_vpkpx (v, v); }
	};

template <> struct generator <0xFBFFFBFFU, 0xFBFFFBFFU, 0xFBFFFBFFU, 0xFBFFFBFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-5); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFBFFFFFFU, 0xFBFFFFFFU, 0xFBFFFFFFU, 0xFBFFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-5); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xFC000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-4); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFCFCFCFCU, 0xFCFCFCFCU, 0xFCFCFCFCU, 0xFCFCFCFCU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-4); }
	};

template <> struct generator <0xFCFCFCFFU, 0xFCFCFCFFU, 0xFCFCFCFFU, 0xFCFCFCFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-4); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xFCFFFCFFU, 0xFCFFFCFFU, 0xFCFFFCFFU, 0xFCFFFCFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-4); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xFCFFFFFFU, 0xFCFFFFFFU, 0xFCFFFFFFU, 0xFCFFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-4); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xFD000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-3); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFDFDFDFDU, 0xFDFDFDFDU, 0xFDFDFDFDU, 0xFDFDFDFDU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-3); }
	};

template <> struct generator <0xFDFDFDFFU, 0xFDFDFDFFU, 0xFDFDFDFFU, 0xFDFDFDFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-3); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xFDFFFDFFU, 0xFDFFFDFFU, 0xFDFFFDFFU, 0xFDFFFDFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-3); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFDFFFFFFU, 0xFDFFFFFFU, 0xFDFFFFFFU, 0xFDFFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-3); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xFE000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-2); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFEFEFEFEU, 0xFEFEFEFEU, 0xFEFEFEFEU, 0xFEFEFEFEU>
	{
		static INLINE vui call () { return (vui) vec_vspltisb (-2); }
	};

template <> struct generator <0xFEFEFEFFU, 0xFEFEFEFFU, 0xFEFEFEFFU, 0xFEFEFEFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-2); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xFEFFFEFFU, 0xFEFFFEFFU, 0xFEFFFEFFU, 0xFEFFFEFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltish (-2); return vec_vperm (v, v, (vuc) v); }
	};

template <> struct generator <0xFEFFFFFFU, 0xFEFFFFFFU, 0xFEFFFFFFU, 0xFEFFFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-2); return vec_vsldoi (v, v, 3); }
	};

template <> struct generator <0xFF000000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-1); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFF1C0710U, 0xFF1C0710U, 0xFF1C0710U, 0xFF1C0710U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-16); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1C0F11U, 0xFF1C0F11U, 0xFF1C0F11U, 0xFF1C0F11U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-15); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1C1712U, 0xFF1C1712U, 0xFF1C1712U, 0xFF1C1712U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-14); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1C1F13U, 0xFF1C1F13U, 0xFF1C1F13U, 0xFF1C1F13U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-13); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1D0714U, 0xFF1D0714U, 0xFF1D0714U, 0xFF1D0714U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-12); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1D0F15U, 0xFF1D0F15U, 0xFF1D0F15U, 0xFF1D0F15U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-11); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1D1716U, 0xFF1D1716U, 0xFF1D1716U, 0xFF1D1716U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-10); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1D1F17U, 0xFF1D1F17U, 0xFF1D1F17U, 0xFF1D1F17U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-9); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1E0718U, 0xFF1E0718U, 0xFF1E0718U, 0xFF1E0718U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-8); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1E0F19U, 0xFF1E0F19U, 0xFF1E0F19U, 0xFF1E0F19U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-7); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1E171AU, 0xFF1E171AU, 0xFF1E171AU, 0xFF1E171AU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-6); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1E1F1BU, 0xFF1E1F1BU, 0xFF1E1F1BU, 0xFF1E1F1BU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-5); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F071CU, 0xFF1F071CU, 0xFF1F071CU, 0xFF1F071CU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-4); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F0F1DU, 0xFF1F0F1DU, 0xFF1F0F1DU, 0xFF1F0F1DU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-3); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F171EU, 0xFF1F171EU, 0xFF1F171EU, 0xFF1F171EU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisb (-2); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F10U, 0xFF1F1F10U, 0xFF1F1F10U, 0xFF1F1F10U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-16); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F11U, 0xFF1F1F11U, 0xFF1F1F11U, 0xFF1F1F11U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-15); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F12U, 0xFF1F1F12U, 0xFF1F1F12U, 0xFF1F1F12U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-14); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F13U, 0xFF1F1F13U, 0xFF1F1F13U, 0xFF1F1F13U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-13); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F14U, 0xFF1F1F14U, 0xFF1F1F14U, 0xFF1F1F14U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-12); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F15U, 0xFF1F1F15U, 0xFF1F1F15U, 0xFF1F1F15U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-11); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F16U, 0xFF1F1F16U, 0xFF1F1F16U, 0xFF1F1F16U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-10); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F17U, 0xFF1F1F17U, 0xFF1F1F17U, 0xFF1F1F17U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-9); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F18U, 0xFF1F1F18U, 0xFF1F1F18U, 0xFF1F1F18U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-8); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F19U, 0xFF1F1F19U, 0xFF1F1F19U, 0xFF1F1F19U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-7); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1AU, 0xFF1F1F1AU, 0xFF1F1F1AU, 0xFF1F1F1AU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-6); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1BU, 0xFF1F1F1BU, 0xFF1F1F1BU, 0xFF1F1F1BU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-5); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1CU, 0xFF1F1F1CU, 0xFF1F1F1CU, 0xFF1F1F1CU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-4); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1DU, 0xFF1F1F1DU, 0xFF1F1F1DU, 0xFF1F1F1DU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-3); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1EU, 0xFF1F1F1EU, 0xFF1F1F1EU, 0xFF1F1F1EU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltish (-2); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F10U, 0xFF1F1F1FU, 0xFF1F1F10U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-16); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F11U, 0xFF1F1F1FU, 0xFF1F1F11U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-15); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F12U, 0xFF1F1F1FU, 0xFF1F1F12U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-14); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F13U, 0xFF1F1F1FU, 0xFF1F1F13U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-13); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F14U, 0xFF1F1F1FU, 0xFF1F1F14U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-12); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F15U, 0xFF1F1F1FU, 0xFF1F1F15U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-11); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F16U, 0xFF1F1F1FU, 0xFF1F1F16U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-10); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F17U, 0xFF1F1F1FU, 0xFF1F1F17U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-9); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F18U, 0xFF1F1F1FU, 0xFF1F1F18U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-8); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F19U, 0xFF1F1F1FU, 0xFF1F1F19U>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-7); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F1AU, 0xFF1F1F1FU, 0xFF1F1F1AU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-6); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F1BU, 0xFF1F1F1FU, 0xFF1F1F1BU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-5); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F1CU, 0xFF1F1F1FU, 0xFF1F1F1CU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-4); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F1DU, 0xFF1F1F1FU, 0xFF1F1F1DU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-3); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F1EU, 0xFF1F1F1FU, 0xFF1F1F1EU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-2); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFF1F1F1FU, 0xFF1F1F1FU, 0xFF1F1F1FU, 0xFF1F1F1FU>
	{
		static INLINE vui call () { vp v = (vp) vec_vspltisw (-1); return vec_vupkhpx (v); }
	};

template <> struct generator <0xFFF00000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-16); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF0FFF0U, 0xFFF0FFF0U, 0xFFF0FFF0U, 0xFFF0FFF0U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-16); }
	};

template <> struct generator <0xFFF0FFFFU, 0xFFF0FFFFU, 0xFFF0FFFFU, 0xFFF0FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-16); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF10000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-15); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF1FFF1U, 0xFFF1FFF1U, 0xFFF1FFF1U, 0xFFF1FFF1U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-15); }
	};

template <> struct generator <0xFFF1FFFFU, 0xFFF1FFFFU, 0xFFF1FFFFU, 0xFFF1FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-15); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF20000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-14); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF2FFF2U, 0xFFF2FFF2U, 0xFFF2FFF2U, 0xFFF2FFF2U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-14); }
	};

template <> struct generator <0xFFF2FFFFU, 0xFFF2FFFFU, 0xFFF2FFFFU, 0xFFF2FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-14); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF30000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-13); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF3FFF3U, 0xFFF3FFF3U, 0xFFF3FFF3U, 0xFFF3FFF3U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-13); }
	};

template <> struct generator <0xFFF3FFFFU, 0xFFF3FFFFU, 0xFFF3FFFFU, 0xFFF3FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-13); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF40000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-12); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF4FFF4U, 0xFFF4FFF4U, 0xFFF4FFF4U, 0xFFF4FFF4U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-12); }
	};

template <> struct generator <0xFFF4FFFFU, 0xFFF4FFFFU, 0xFFF4FFFFU, 0xFFF4FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-12); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF50000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-11); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF5FFF5U, 0xFFF5FFF5U, 0xFFF5FFF5U, 0xFFF5FFF5U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-11); }
	};

template <> struct generator <0xFFF5FFFFU, 0xFFF5FFFFU, 0xFFF5FFFFU, 0xFFF5FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-11); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF60000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-10); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF6FFF6U, 0xFFF6FFF6U, 0xFFF6FFF6U, 0xFFF6FFF6U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-10); }
	};

template <> struct generator <0xFFF6FFFFU, 0xFFF6FFFFU, 0xFFF6FFFFU, 0xFFF6FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-10); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF70000U, 0x00000000U, 0x00000000U, 0x00000000U>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-9); return vec_vslo (v, (vuc) v); }
	};

template <> struct generator <0xFFF7FFF7U, 0xFFF7FFF7U, 0xFFF7FFF7U, 0xFFF7FFF7U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-9); }
	};

template <> struct generator <0xFFF7FFFFU, 0xFFF7FFFFU, 0xFFF7FFFFU, 0xFFF7FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-9); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF8FFF8U, 0xFFF8FFF8U, 0xFFF8FFF8U, 0xFFF8FFF8U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-8); }
	};

template <> struct generator <0xFFF8FFFFU, 0xFFF8FFFFU, 0xFFF8FFFFU, 0xFFF8FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-8); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFF9FFF9U, 0xFFF9FFF9U, 0xFFF9FFF9U, 0xFFF9FFF9U>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-7); }
	};

template <> struct generator <0xFFF9FFFFU, 0xFFF9FFFFU, 0xFFF9FFFFU, 0xFFF9FFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-7); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFFAFFFAU, 0xFFFAFFFAU, 0xFFFAFFFAU, 0xFFFAFFFAU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-6); }
	};

template <> struct generator <0xFFFAFFFFU, 0xFFFAFFFFU, 0xFFFAFFFFU, 0xFFFAFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-6); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFFBFFFBU, 0xFFFBFFFBU, 0xFFFBFFFBU, 0xFFFBFFFBU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-5); }
	};

template <> struct generator <0xFFFBFFFFU, 0xFFFBFFFFU, 0xFFFBFFFFU, 0xFFFBFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-5); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFFCFFFCU, 0xFFFCFFFCU, 0xFFFCFFFCU, 0xFFFCFFFCU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-4); }
	};

template <> struct generator <0xFFFCFFFFU, 0xFFFCFFFFU, 0xFFFCFFFFU, 0xFFFCFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-4); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFFDFFFDU, 0xFFFDFFFDU, 0xFFFDFFFDU, 0xFFFDFFFDU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-3); }
	};

template <> struct generator <0xFFFDFFFFU, 0xFFFDFFFFU, 0xFFFDFFFFU, 0xFFFDFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-3); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFFEFFFEU, 0xFFFEFFFEU, 0xFFFEFFFEU, 0xFFFEFFFEU>
	{
		static INLINE vui call () { return (vui) vec_vspltish (-2); }
	};

template <> struct generator <0xFFFEFFFFU, 0xFFFEFFFFU, 0xFFFEFFFFU, 0xFFFEFFFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-2); return vec_vsldoi (v, v, 2); }
	};

template <> struct generator <0xFFFFF0F0U, 0xFFFFF0F0U, 0xFFFFF0F0U, 0xFFFFF0F0U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-16); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF0FFU, 0xFFFFF0FFU, 0xFFFFF0FFU, 0xFFFFF0FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-16); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF1F1U, 0xFFFFF1F1U, 0xFFFFF1F1U, 0xFFFFF1F1U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-15); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF1FFU, 0xFFFFF1FFU, 0xFFFFF1FFU, 0xFFFFF1FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-15); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF2F2U, 0xFFFFF2F2U, 0xFFFFF2F2U, 0xFFFFF2F2U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-14); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF2FFU, 0xFFFFF2FFU, 0xFFFFF2FFU, 0xFFFFF2FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-14); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF3F3U, 0xFFFFF3F3U, 0xFFFFF3F3U, 0xFFFFF3F3U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-13); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF3FFU, 0xFFFFF3FFU, 0xFFFFF3FFU, 0xFFFFF3FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-13); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF4F4U, 0xFFFFF4F4U, 0xFFFFF4F4U, 0xFFFFF4F4U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-12); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF4FFU, 0xFFFFF4FFU, 0xFFFFF4FFU, 0xFFFFF4FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-12); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF5F5U, 0xFFFFF5F5U, 0xFFFFF5F5U, 0xFFFFF5F5U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-11); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF5FFU, 0xFFFFF5FFU, 0xFFFFF5FFU, 0xFFFFF5FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-11); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF6F6U, 0xFFFFF6F6U, 0xFFFFF6F6U, 0xFFFFF6F6U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-10); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF6FFU, 0xFFFFF6FFU, 0xFFFFF6FFU, 0xFFFFF6FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-10); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF7F7U, 0xFFFFF7F7U, 0xFFFFF7F7U, 0xFFFFF7F7U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-9); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF7FFU, 0xFFFFF7FFU, 0xFFFFF7FFU, 0xFFFFF7FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-9); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF8F8U, 0xFFFFF8F8U, 0xFFFFF8F8U, 0xFFFFF8F8U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-8); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF8FFU, 0xFFFFF8FFU, 0xFFFFF8FFU, 0xFFFFF8FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-8); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFF9F9U, 0xFFFFF9F9U, 0xFFFFF9F9U, 0xFFFFF9F9U>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-7); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFF9FFU, 0xFFFFF9FFU, 0xFFFFF9FFU, 0xFFFFF9FFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-7); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFFAFAU, 0xFFFFFAFAU, 0xFFFFFAFAU, 0xFFFFFAFAU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-6); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFFAFFU, 0xFFFFFAFFU, 0xFFFFFAFFU, 0xFFFFFAFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-6); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFFBFBU, 0xFFFFFBFBU, 0xFFFFFBFBU, 0xFFFFFBFBU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-5); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFFBFFU, 0xFFFFFBFFU, 0xFFFFFBFFU, 0xFFFFFBFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-5); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFFCFCU, 0xFFFFFCFCU, 0xFFFFFCFCU, 0xFFFFFCFCU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-4); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFFCFFU, 0xFFFFFCFFU, 0xFFFFFCFFU, 0xFFFFFCFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-4); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFFDFDU, 0xFFFFFDFDU, 0xFFFFFDFDU, 0xFFFFFDFDU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-3); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFFDFFU, 0xFFFFFDFFU, 0xFFFFFDFFU, 0xFFFFFDFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-3); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFFEFEU, 0xFFFFFEFEU, 0xFFFFFEFEU, 0xFFFFFEFEU>
	{
		static INLINE vui call () { vss v = (vss) vec_vspltisb (-2); return (vui) vec_vupkhsh (v); }
	};

template <> struct generator <0xFFFFFEFFU, 0xFFFFFEFFU, 0xFFFFFEFFU, 0xFFFFFEFFU>
	{
		static INLINE vui call () { vui v = (vui) vec_vspltisw (-2); return vec_vsldoi (v, v, 1); }
	};

template <> struct generator <0xFFFFFFF0U, 0xFFFFFFF0U, 0xFFFFFFF0U, 0xFFFFFFF0U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-16); }
	};

template <> struct generator <0xFFFFFFF1U, 0xFFFFFFF1U, 0xFFFFFFF1U, 0xFFFFFFF1U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-15); }
	};

template <> struct generator <0xFFFFFFF2U, 0xFFFFFFF2U, 0xFFFFFFF2U, 0xFFFFFFF2U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-14); }
	};

template <> struct generator <0xFFFFFFF3U, 0xFFFFFFF3U, 0xFFFFFFF3U, 0xFFFFFFF3U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-13); }
	};

template <> struct generator <0xFFFFFFF4U, 0xFFFFFFF4U, 0xFFFFFFF4U, 0xFFFFFFF4U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-12); }
	};

template <> struct generator <0xFFFFFFF5U, 0xFFFFFFF5U, 0xFFFFFFF5U, 0xFFFFFFF5U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-11); }
	};

template <> struct generator <0xFFFFFFF6U, 0xFFFFFFF6U, 0xFFFFFFF6U, 0xFFFFFFF6U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-10); }
	};

template <> struct generator <0xFFFFFFF7U, 0xFFFFFFF7U, 0xFFFFFFF7U, 0xFFFFFFF7U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-9); }
	};

template <> struct generator <0xFFFFFFF8U, 0xFFFFFFF8U, 0xFFFFFFF8U, 0xFFFFFFF8U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-8); }
	};

template <> struct generator <0xFFFFFFF9U, 0xFFFFFFF9U, 0xFFFFFFF9U, 0xFFFFFFF9U>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-7); }
	};

template <> struct generator <0xFFFFFFFAU, 0xFFFFFFFAU, 0xFFFFFFFAU, 0xFFFFFFFAU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-6); }
	};

template <> struct generator <0xFFFFFFFBU, 0xFFFFFFFBU, 0xFFFFFFFBU, 0xFFFFFFFBU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-5); }
	};

template <> struct generator <0xFFFFFFFCU, 0xFFFFFFFCU, 0xFFFFFFFCU, 0xFFFFFFFCU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-4); }
	};

template <> struct generator <0xFFFFFFFDU, 0xFFFFFFFDU, 0xFFFFFFFDU, 0xFFFFFFFDU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-3); }
	};

template <> struct generator <0xFFFFFFFEU, 0xFFFFFFFEU, 0xFFFFFFFEU, 0xFFFFFFFEU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-2); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF0FFF0U, 0xFFFFFFFFU, 0xFFF0FFF0U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-16); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF1FFF1U, 0xFFFFFFFFU, 0xFFF1FFF1U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-15); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF2FFF2U, 0xFFFFFFFFU, 0xFFF2FFF2U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-14); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF3FFF3U, 0xFFFFFFFFU, 0xFFF3FFF3U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-13); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF4FFF4U, 0xFFFFFFFFU, 0xFFF4FFF4U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-12); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF5FFF5U, 0xFFFFFFFFU, 0xFFF5FFF5U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-11); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF6FFF6U, 0xFFFFFFFFU, 0xFFF6FFF6U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-10); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF7FFF7U, 0xFFFFFFFFU, 0xFFF7FFF7U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-9); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF8FFF8U, 0xFFFFFFFFU, 0xFFF8FFF8U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-8); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFF9FFF9U, 0xFFFFFFFFU, 0xFFF9FFF9U>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-7); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFAFFFAU, 0xFFFFFFFFU, 0xFFFAFFFAU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-6); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFBFFFBU, 0xFFFFFFFFU, 0xFFFBFFFBU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-5); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFCFFFCU, 0xFFFFFFFFU, 0xFFFCFFFCU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-4); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFDFFFDU, 0xFFFFFFFFU, 0xFFFDFFFDU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-3); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFEFFFEU, 0xFFFFFFFFU, 0xFFFEFFFEU>
	{
		static INLINE vui call () { vus v = (vus) vec_vspltisw (-2); return (vui) vec_vmrghh (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF0F0U, 0xFFFFFFFFU, 0xFFFFF0F0U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-16); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF1F1U, 0xFFFFFFFFU, 0xFFFFF1F1U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-15); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF2F2U, 0xFFFFFFFFU, 0xFFFFF2F2U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-14); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF3F3U, 0xFFFFFFFFU, 0xFFFFF3F3U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-13); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF4F4U, 0xFFFFFFFFU, 0xFFFFF4F4U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-12); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF5F5U, 0xFFFFFFFFU, 0xFFFFF5F5U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-11); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF6F6U, 0xFFFFFFFFU, 0xFFFFF6F6U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-10); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF7F7U, 0xFFFFFFFFU, 0xFFFFF7F7U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-9); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF8F8U, 0xFFFFFFFFU, 0xFFFFF8F8U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-8); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFF9F9U, 0xFFFFFFFFU, 0xFFFFF9F9U>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-7); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFAFAU, 0xFFFFFFFFU, 0xFFFFFAFAU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-6); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFBFBU, 0xFFFFFFFFU, 0xFFFFFBFBU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-5); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFCFCU, 0xFFFFFFFFU, 0xFFFFFCFCU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-4); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFDFDU, 0xFFFFFFFFU, 0xFFFFFDFDU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-3); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFEFEU, 0xFFFFFFFFU, 0xFFFFFEFEU>
	{
		static INLINE vui call () { vuc v = (vuc) vec_vspltisw (-2); return (vui) vec_vmrghb (v, v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF0U, 0xFFFFFFFFU, 0xFFFFFFF0U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-16); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF1U, 0xFFFFFFFFU, 0xFFFFFFF1U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-15); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF2U, 0xFFFFFFFFU, 0xFFFFFFF2U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-14); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF3U, 0xFFFFFFFFU, 0xFFFFFFF3U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-13); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF4U, 0xFFFFFFFFU, 0xFFFFFFF4U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-12); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF5U, 0xFFFFFFFFU, 0xFFFFFFF5U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-11); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF6U, 0xFFFFFFFFU, 0xFFFFFFF6U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-10); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF7U, 0xFFFFFFFFU, 0xFFFFFFF7U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-9); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF8U, 0xFFFFFFFFU, 0xFFFFFFF8U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-8); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFF9U, 0xFFFFFFFFU, 0xFFFFFFF9U>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-7); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFFAU, 0xFFFFFFFFU, 0xFFFFFFFAU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-6); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFFBU, 0xFFFFFFFFU, 0xFFFFFFFBU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-5); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFFCU, 0xFFFFFFFFU, 0xFFFFFFFCU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-4); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFFDU, 0xFFFFFFFFU, 0xFFFFFFFDU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-3); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFFEU, 0xFFFFFFFFU, 0xFFFFFFFEU>
	{
		static INLINE vui call () { vsc v = (vsc) vec_vspltisw (-2); return (vui) vec_vupkhsb (v); }
	};

template <> struct generator <0xFFFFFFFFU, 0xFFFFFFFFU, 0xFFFFFFFFU, 0xFFFFFFFFU>
	{
		static INLINE vui call () { return (vui) vec_vspltisw (-1); }
	};
	
#endif

