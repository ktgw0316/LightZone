/*
 *  refptr.h
 *  macstl
 *
 *  Created by Glen Low on Jan 24 2005.
 *
 *  Copyright (c) 2005 Pixelglow Software, all rights reserved.
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

#ifndef MACSTL_REFPTR_H
#define MACSTL_REFPTR_H

namespace macstl
	{
		namespace impl
			{
				struct retain_tag;
			}
			
		/// Reference semantics for value types.
		
		/// Many modern C APIs have reference-counted classes, which also have a copy function. In order to get a more natural C++
		/// idiom, we wrap these reference-counted classes into value types, and let refptr represent the reference type interface.
		/// Thus, copying the value type does a real copy, whereas copying the refptr only increments the refcount. The refptr then calls
		/// into private constructors which are all tagged with impl::retain_tag.
		///
		/// The comparison operators simply reuse the equivalents on the value type.
		///
		/// The following types use refptr: macstl::core_array.
		///
		/// @param	T	The value type, which should have the following constructors:
		///				- T (typename T::data_type data, impl::retain_tag*) retains a reference to data.
		///				- T (const T& other, impl::retain_tag*) retains a reference to other.
		///				- T (const U& other, impl::retain_tag*) retains a reference to other, if a U can be used as T.
		///				.
		/// @header	#include <macstl/refptr.h>
			
		template <typename T> class refptr
			{
				public:
					/// The value type.
					typedef T element_type;
					
					/// The raw data type.
					typedef typename T::data_type data_type;
					
					/// Constructs from a raw data @a ptr.
					explicit refptr (data_type ptr = NULL): ref_ (ptr, (impl::retain_tag*) NULL)
						{
						}

					/// Constructs from an element @a ptr.
					explicit refptr (element_type* ptr): ref_ (*ptr, (impl::retain_tag*) NULL)
						{
						}
						
					/// Copies the @a other.
					refptr (refptr& other): ref_ (other.ref_, (impl::retain_tag*) NULL)
						{
						}
						
					/// Copies the different type @a other.
					template <typename T2> refptr (const refptr <T2>& other): ref_ (static_cast <const T&> (other.ref_), (impl::retain_tag*) NULL)
						{
						}
						
					/// Assigns the @a other.
					refptr& operator= (const refptr& other)
						{
							if (&other != this)
								{
									refptr temp (other);
									swap (other);
								}
							return *this;
						}

					/// Assigns the different type @a other.
					template <typename T2> refptr& operator= (const refptr <T2>& other)
						{
							refptr temp (other);
							swap (other);
							
							return *this;
						}
						
					/// Swaps the pointer with @a other.
					void swap (refptr& other)
						{
							std::swap (ref_, other.ref_);
						}
						
					/// Gets a reference to the element type.
					element_type& operator* () const
						{
							return ref_;
						}
						
					/// Gets a pointer to the element type.
					element_type* operator-> () const
						{
							return &ref_;
						}
						
					/// Gets a pointer to the element type.
					element_type* get () const
						{
							return &ref_;
						}
						
					/// Resets the pointer to point to @a ptr.
					template <typename T2> void reset (T2* ptr)
						{
							refptr temp (ptr);
							swap (temp);
						}
			
				private:
					mutable T ref_;
			};

		 /// @relates refptr
		 /// @brief Tests whether the elements of @a lhs and @a rhs are equal.
		template <typename T1, typename T2> inline bool operator== (const refptr <T1> &lhs, const refptr <T2> &rhs)
			{
				return *lhs == *rhs;
			}

		 /// @relates refptr
		 /// @brief Tests whether the elements of @a lhs and @a rhs are not equal.
		template <typename T1, typename T2> inline bool operator!= (const refptr <T1> &lhs, const refptr <T2> &rhs)
			{
				return *lhs != *rhs;
			}

		 /// @relates refptr
		 /// @brief Tests whether the elements of @a lhs and @a rhs are less than.
		template <typename T1, typename T2> inline bool operator< (const refptr <T1> &lhs, const refptr <T2> &rhs)
			{
				return *lhs < *rhs;
			}

		 /// @relates refptr
		 /// @brief Swaps the pointers of @a lhs and @a rhs.			
		template <typename T> inline void swap (refptr <T> &lhs, refptr <T> &rhs)
			{
				lhs.swap (rhs);
			}
			
	}

#endif