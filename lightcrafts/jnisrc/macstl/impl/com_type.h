/*
 *  com_type.h
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

#ifndef MACSTL_IMPL_COM_TYPE_H
#define MACSTL_IMPL_COM_TYPE_H

namespace macstl
	{
		template <typename IList> class com_type;

		namespace impl
			{
				struct compare_iid
					{
						bool operator() (const REFIID& left, const REFIID& right) const
							{
								// do a lexicographical compare but backwards, in the hopes that later bytes differ faster...
								const unsigned char* leftptr = reinterpret_cast <const unsigned char*> (&left);
								const unsigned char* rightptr = reinterpret_cast <const unsigned char*> (&right);
								for (std::ptrdiff_t i = 15; i != -1; --i)
									if (leftptr [i] < rightptr [i])
										return true;
									else if (leftptr [i] > rightptr [i])
										return false;
										
								// now if could be sure these were 16-byte aligned we could do a compare using SIMD...
										
								return false;
							}
					};
					
				typedef std::map <REFIID, std::ptrdiff_t, compare_iid> offset_map;
				
				template <typename T1, typename T2> std::ptrdiff_t offsetof_class ()
					{
						// can't set dummy to NULL since static casting NULL doesn't work, but this should be a legally aligned non-NULL address for T1...
						T1* dummy = reinterpret_cast <T1*> (sizeof (T2));
						
						return reinterpret_cast <char*> (static_cast <T2*> (dummy)) - reinterpret_cast <char*> (dummy);
					}
					
				/// COM interface registration.
		
				/// @internal
				/// Registers the COM interfaces listed in the typelist.
				///
				/// @param	IList	A macstl::typelist of COM interfaces to implement.

				template <typename IList> class com_interface:
					public com_interface <typename IList::odd>,
					public com_interface <typename IList::even>				
					{
						private:
							typedef com_interface <IList> us;
							typedef com_interface <typename IList::odd> odd;
							typedef com_interface <typename IList::even> even;
							
							friend class com_type <IList>;
							template <typename IList2> friend class com_interface;
							
							static void register_interfaces (offset_map& offsets, std::ptrdiff_t offset)
								{
									odd::register_interfaces (offsets, offset + offsetof_class <us, odd> ());
									even::register_interfaces (offsets, offset + offsetof_class <us, even> ());
								}
					};
			
				template <typename I> class com_interface <typelist <I> >: public I
					{
						private:
							typedef com_interface <typelist <I> > us;
							
							template <typename IList> friend class com_type;
							template <typename IList> friend class com_interface;
							
							static void register_interfaces (offset_map& offsets, std::ptrdiff_t offset)
								{
									// no repeats in the IList or IUnknown*
									assert (offsets.find (uuid_of <I>::value) == offsets.end ());
									offsets [uuid_of <I>::value] = offset + offsetof_class <us, I> ();
								}
					};
					
				template <> class com_interface <typelist <> >
					{
						private:
							template <typename IList> friend class com_type;
							// template <typename IList2> friend class com_interface;
							
							static void register_interfaces (offset_map&, std::ptrdiff_t)
								{
								}
					};

			}

		/// COM server.
		
		/// Implements all the IUnknown functionality of a COM server: referencing counting and interface vending. This uses a static map
		/// of interface pointer offsets for fast vending and reduced instance space, while preserving thread safety.
		///
		/// To write a COM server vending interfaces I1, I2..., just inherit from com_type <typelist <I1, I2...> > and implement the abstract
		/// members of I1, I2... We do the rest!
		///
		/// @param	IList	A macstl::typelist of COM interfaces to implement.
		///
		/// @note	Do not include IUnknown in the typelist.
		///
		/// @header	#include <macstl/com.h>
			
		template <typename IList> class com_type: public impl::com_interface <typelist <IUnknown> >, public impl::com_interface <IList>
			{
				public:
					virtual ~com_type ()
						{
						}
						
					/// Queries for an interface @a ppv for the identifier @a iid.
					virtual HRESULT STDMETHODCALLTYPE QueryInterface (REFIID iid, LPVOID* ppv)
						{
							impl::offset_map::const_iterator found = iid_offsets_.find (iid);
							if (found == iid_offsets_.end ())
								{
									// no such interface found
									*ppv = NULL;
									return E_NOINTERFACE;
								}
							else
								{
									// interface was found in our interfaces, add ref
									*ppv = reinterpret_cast <char*> (this) + found->second;
									++ref_;
									return S_OK;
								}
						}
					
					/// Adds a reference to the object.
					virtual ULONG STDMETHODCALLTYPE AddRef ()
						{
							return ++ref_;
						}
						
					/// Releases a reference. When there are no more references, frees the object.
					virtual ULONG STDMETHODCALLTYPE Release ()
						{
							if (--ref_)
								return ref_;
							else
								{
									// reference count fell to zero: free object
									delete this;
									return 0;
								}
						}
						
				protected:
					com_type (): ref_ (1)
						{
						}

				private:
					static const impl::offset_map iid_offsets_;
					ULONG ref_;
					
					typedef com_type <IList> us;
					typedef impl::com_interface <typelist <IUnknown> > head;
					typedef impl::com_interface <IList> tail;
					
					static impl::offset_map init_offsets ()
						{
							impl::offset_map offsets;
							head::register_interfaces (offsets, impl::offsetof_class <us, head> ());							
							tail::register_interfaces (offsets, impl::offsetof_class <us, tail> ());							
							return offsets;
						}
			};
			
		template <typename IList> const impl::offset_map com_type <IList>::iid_offsets_ = init_offsets ();
	}

#endif
