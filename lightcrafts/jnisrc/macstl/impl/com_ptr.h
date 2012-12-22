/*
 *  com_client.h
 *  macstl
 *
 *  Created by Glen Low on Jul 9 2002.
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

#ifndef MACSTL_IMPL_COM_CLIENT_H
#define MACSTL_IMPL_COM_CLIENT_H

namespace macstl
	{
		/// COM Error.
		
		/// Encapsulates a COM HRESULT error.
		///
		/// @header	#include <macstl/com.h>
		
		class com_error: public std::runtime_error
			{
				public:
					/** Constructs from a COM HRESULT @a hr. */
					com_error (HRESULT hr): std::runtime_error ("COM"), hr_ (hr)
						{
						}
					
					/** Returns the raw HRESULT. */
					HRESULT result () const
						{
							return hr_;
						}

					static void throw_if (HRESULT hr)
						{
							if (FAILED (hr))
								throw com_error (hr);
						}
						
				private:
					HRESULT hr_;
			};
			
			
		/// COM interface smart pointer.
		
		/// The com_ptr encapsulates the COM interface pointer as a "smart pointer". It is conceptually similar to
		/// boost::counted_ptr, but based on COM reference counting. While it looks like Microsoft ATL's _com_ptr_t,
		/// it is far simpler with fewer loopholes and gotchas.
		///
		/// AddRef is always only done at construction and Release at destruction, thus typical C++ usage takes care
		/// of reference-counting issues. Swaps, resets and assignment operators all create internal temporaries to
		/// preserve these invariants, ensuring the object does not leak references.
		///
		/// Comparisons use COM equality rules: the interface pointers are converted to IUnknown pointers for
		/// comparison.
		///
		/// On Mac OS X, you can use this to access a CFPlugIn.
		///
		/// @param	T	The COM interface type.
		///
		/// @header	#include <macstl/com.h>
		
		template <typename T> class com_ptr
			{
				public:
					/// The COM interface type.
					typedef T element_type;
					
					/// Constructs from a raw COM interface @a ptr.
					explicit com_ptr (element_type* ptr = NULL): ptr_ (ptr)
						{
							if (ptr_) ptr_->AddRef ();
						}
			
					/// Constructs a copy of @a other.
					com_ptr (const com_ptr& other): ptr_ (other.ptr_)
						{
							if (ptr_) ptr_->AddRef ();
						}
	
					/// Constructs from a different interface pointer @a other.
					template <typename T2> com_ptr (const com_ptr <T2>& other): ptr_ (NULL)
						{
							T2* ptr = other.get ();
							if (ptr)
								com_error::throw_if (ptr->QueryInterface (uuid_of <T>::value, (void **) &ptr_));
						}
						
					/// Destructs the pointer.
					~com_ptr ()
						{
							if (ptr_) ptr_->Release ();
						}
								
					/// Assigns from @a other.
					com_ptr& operator= (const com_ptr& other)
						{
							if (&other != this)
								{
									com_ptr temp (other);
									swap (temp);
								}
							return *this;
						}
		
					/// Assigns from a different interface pointer @a other.
					template <typename T2> com_ptr& operator= (const com_ptr <T2>& other)
						{
							if (other.get () != get ())
								{
									com_ptr temp (other);
									swap (other);
								}
							return *this;
						}
						
					/// Swaps interface pointer with @a other.
					void swap (com_ptr& other)
						{
							std::swap (ptr_, other.ptr_);
						}
												
					/// Returns a reference to the COM type.
					element_type& operator* () const
						{
							return *ptr_;
						}
						
					/// Returns a pointer to the COM type.
					element_type* operator-> () const
						{
							return ptr_;
						}
						
					/// Returns the raw interface pointer.
					element_type* get () const
						{
							return ptr_;
						}
						
					/// Resets the pointer to point to @a ptr.
					void reset (element_type* ptr = NULL) const
						{
							if (ptr != ptr_)
								{
									com_ptr temp (ptr);
									swap (temp);
								}
						}
		
				private:
					T* ptr_;
			};
			
		/// @relates com_ptr
		/// @brief Checks whether the IUnknown pointer of @a left is equal to the IUnknown pointer of @a right.
		template <typename T1, typename T2> inline bool operator== (const com_ptr <T1> &left, const com_ptr <T2> &right)
			{
				T1* leftptr = left.get ();
				T2* rightptr = right.get ();
				if (leftptr == rightptr)
					// if they have the same underlying COM object, or both are null, they are equal
					return true;
				else if (leftptr && rightptr)
					// if they are non-null and have the same underlying IUnknown interface, they are equal
					return (com_ptr <IUnknown> (left)).get () == (com_ptr <IUnknown> (right)).get ();
				else
					// either one is null and other is non-null, so they are not equal
					return false;
			}

		/// @relates com_ptr
		/// @brief Checks whether the IUnknown pointer of @a left is not equal to the IUnknown pointer of @a right.
		template <typename T1, typename T2> inline bool operator!= (const com_ptr <T1> &left, const com_ptr <T2> &right)
			{
				T1* leftptr = left.get ();
				T2* rightptr = right.get ();
				if (!leftptr && !rightptr)
					// both null, definitely unequal
					return false;
				else if (leftptr && rightptr)
					// if they are non-null and have the different underlying IUnknown interface, they are unequal
					return (com_ptr <IUnknown> (left)).get () != (com_ptr <IUnknown> (right)).get ();
				else
					// either one is null and other is non-null, so they are not equal
					return true;
			}
		
		/// @relates com_ptr
		/// @brief Checks whether the IUnknown pointer of @a left is less than the IUnknown pointer of @a right.
		template <class T1, class T2> inline bool operator< (const com_ptr <T1> &left, const com_ptr <T2> &right)
			{
				T1* leftptr = left.get ();
				T2* rightptr = right.get ();
				if (leftptr && rightptr)
					// if they are non-null, compare underlying IUnknown interface
					return (com_ptr <IUnknown> (left)).get () < (com_ptr <IUnknown> (right)).get ();
				else
					return !leftptr && rightptr;
			}
		
		/// @relates com_ptr
		/// @brief Swaps the interface pointers of @a left and @a right.
		template <typename T> inline void swap (com_ptr <T> &left, com_ptr <T> &right)
			{
				left.swap (right);
			}
	}
	
#endif
