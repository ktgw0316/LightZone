/*
 *  algorithm.h
 *  macstl
 *
 *  Created by Glen Low on Apr 19 2003.
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

#ifndef MACSTL_ALGORITHM_H
#define MACSTL_ALGORITHM_H

#include <algorithm>
#include <functional>

#include "impl/meta.h"
#include "functional.h"

namespace stdext
	{
		namespace impl
			{
				template <typename InIter, typename Size, typename Enable = void>
					struct destroy_n_dispatch
					{
						static void call (InIter first, Size n)
							{
								typedef typename std::iterator_traits <InIter>::value_type value_type;
								for (; n > 0; --n)
									{
										(&*first)->~value_type ();
										++first;
									}
							}
					};

				template <typename InIter, typename Size>
					struct destroy_n_dispatch <InIter, Size, typename enable_if <
						is_same <typename std::iterator_traits <InIter>::iterator_category, std::random_access_iterator_tag>::value>::type>
					{
						static void call (InIter first, Size n)
							{
								typedef typename std::iterator_traits <InIter>::value_type value_type;
								for (Size count = 0; count != n; ++count)
									(&first [count])->~value_type ();
							}
					};
					
			}
			
		template <typename InIter, typename Size> inline void destroy_n (InIter first, Size n)
			{
				impl::destroy_n_dispatch <InIter, Size>::call (first, n);
			}
			
		namespace impl
			{

				// default: use standard uninitialized copy
				template <typename InIter, typename Size, typename OutIter, typename Enable = void>
					struct uninitialized_copy_n_dispatch
					{
						static void call (const InIter& first, Size n, const OutIter& result)
							{
								typedef typename std::iterator_traits <OutIter>::value_type value_type;
								InIter first_copy = first;
								OutIter result_copy = result;
								Size index = 0;
								try
									{
										for (; n > 0; --n)
											{
												new (&*result_copy) value_type (*first);
												++first_copy;
												++result_copy;
											}
									}
								catch (...)
									{
										for (Size unwind = 0; unwind != index; ++unwind)
											result [unwind].~value_type ();
										throw;
									}
							}
					};

				template <typename InIter, typename Size, typename OutIter>
					struct uninitialized_copy_n_dispatch <InIter, Size, OutIter, typename enable_if <
						is_same <typename std::iterator_traits <InIter>::iterator_category, std::random_access_iterator_tag>::value != 0
						&& is_same <typename std::iterator_traits <OutIter>::iterator_category, std::random_access_iterator_tag>::value != 0>::type>
					{
						static void call (const InIter& first, Size n, const OutIter& result)
							{
								typedef typename std::iterator_traits <OutIter>::value_type value_type;
								Size index = 0;
								try
									{
										for (; index != n; ++index)
											new (&result [index]) value_type (first [index]);
									}
								catch (...)
									{
										for (Size unwind = 0; unwind != index; ++unwind)
											result [unwind].~value_type ();
										throw;
									}
							}
					};

			}
		
		template <typename InIter, typename Size, typename OutIter>
			inline void uninitialized_copy_n (const InIter& first, Size n, const OutIter& result)
			{
				impl::uninitialized_copy_n_dispatch <InIter, Size, OutIter>::call (first, n, result);
			}

		// default case
		namespace impl
			{
				template <typename OutIter, typename Size, typename T, typename Enable1 = void, typename Enable2 = void>
					struct uninitialized_fill_n_dispatch
					{
						static void call (const OutIter& first, Size n, const T& val)
							{
								std::uninitialized_fill_n (first, n, val);
							}
					};

				template <typename OutIter, typename Size, typename T, typename Enable2>
					struct uninitialized_fill_n_dispatch <OutIter, Size, T,
					typename enable_if <is_same <typename std::iterator_traits <OutIter>::iterator_category, std::random_access_iterator_tag>::value>::type,
					Enable2>
					{
						static void call (const OutIter& first, Size n, const T& val)
							{
								Size index = 0;
								
								try
									{
										for (; index != n; ++index)
											new (&first [index]) T (val);
									}
								catch (...)
									{
										for (Size unwind = 0; unwind != index; ++unwind)
											(&first [unwind])->~T ();
										throw;
									}
							}
					};
			}

		template <typename OutIter, typename Size, typename T>
			inline void uninitialized_fill_n (const OutIter& first, Size n, const T& val)
			{
				impl::uninitialized_fill_n_dispatch <OutIter, Size, T>::call (first, n, val);
			}

		namespace impl
			{
				template <typename InIter, typename Size, typename T, typename Enable1 = void, typename Enable2 = void>
					struct count_n_dispatch
					{
						static Size call (const InIter& first, Size n, const T& value)
							{
								InIter first_copy = first;
								Size counter = 0;
								for (; n > 0; --n)
									{
										if (*first_copy == value)
											++counter;
										++first_copy;
									}
								return counter;
							}
					};

				template <typename InIter, typename Size, typename T, typename Enable2>
					struct count_n_dispatch <InIter, Size, T, typename enable_if <
					is_same <typename std::iterator_traits <InIter>::iterator_category, std::random_access_iterator_tag>::value>::type,
					Enable2>
					{
						static Size call (const InIter& first, Size n, const T& value)
							{
								InIter first_copy = first;
								Size counter = 0;
								for (Size index = 0; index != n; ++index)
									if (first_copy [index] == value)
										++counter;
								return counter;
							}
					};
			}
			
		template <typename InIter, typename Size, typename T>
			inline Size count_n (const InIter& first, Size n, const T& value)
			{
				return impl::count_n_dispatch <InIter, Size, T>::call (first, n, value);
			}

		namespace impl
			{
				template <typename InIter, typename Size, typename OutIter, typename Enable1 = void, typename Enable2 = void>
					struct copy_n_dispatch
					{
						static void call (const InIter& first, Size n, const OutIter& result)
							{
								InIter first_copy = first;
								OutIter result_copy = result;
								for (; n > 0; --n)
									{
										*result_copy = *first_copy;
										++first_copy;
										++result_copy;
									}
							}
					};

				// both in & out iterators are random access
				// optimization: use explicit loop count, index into instead of incrementing iterators,
				template <typename InIter, typename Size, typename OutIter, typename Enable2>
					struct copy_n_dispatch <InIter, Size, OutIter, typename enable_if <
						is_same <typename std::iterator_traits <InIter>::iterator_category, std::random_access_iterator_tag>::value != 0
						&& is_same <typename std::iterator_traits <OutIter>::iterator_category, std::random_access_iterator_tag>::value != 0>::type,
						Enable2>
					{
						static void call (const InIter& first, Size n, const OutIter& result)
							{
								for (Size index = 0; index != n; ++index)
									result [index] = first [index];
							}
					};
			}

		template <typename InIter, typename Size, typename OutIter>
			inline void copy_n (const InIter& first, Size n, const OutIter& result)
			{
				impl::copy_n_dispatch <InIter, Size, OutIter>::call (first, n, result);
			}
		
		namespace impl
			{
				// default case
				template <typename OutIter, typename Size, typename T, typename Enable1 = void, typename Enable2 = void>
					struct fill_n_dispatch
					{
						static OutIter call (const OutIter& first, Size n, const T& val)
							{
								return std::fill_n (first, n, val);
							}
					};

				// out iterators is random access
				// optimization: use explicit loop count, index into instead of incrementing iterators
				template <typename OutIter, typename Size, typename T, typename Enable2>
					struct fill_n_dispatch <OutIter, Size, T,
					typename enable_if <is_same <typename std::iterator_traits <OutIter>::iterator_category, std::random_access_iterator_tag>::value>::type,
					Enable2>
					{
						static void call (const OutIter& first, Size n, const T& val)
							{
								for (Size index = 0; index != n; ++index)
									first [index] = val;
							}
					};
			}

		template <typename OutIter, typename Size, typename T>
			inline void fill_n (const OutIter& first, Size n, const T& val)
			{
				impl::fill_n_dispatch <OutIter, Size, T>::call (first, n, val);		
			}
			
		namespace impl
			{
				template <typename InIter, typename Size, typename T, typename BOp, typename Enable1 = void, typename Enable2 = void>
					struct accumulate_n_dispatch
					{
						static T call (const InIter& first, Size n, const T& init, BOp op)
							{
								InIter first_copy = first;
								T init_copy = init;		// param init may be an aligned object, which causes MSC to choke and die...
								for (; n > 0; --n)
									{
										init_copy = op (init_copy, *first_copy);
										++first_copy;
									}
								return init_copy;
							}
					};
					
				template <typename InIter, typename Size, typename T, typename BOp, typename Enable2>
					struct accumulate_n_dispatch <InIter, Size, T, BOp,
					typename enable_if <is_same <typename std::iterator_traits <InIter>::iterator_category, std::random_access_iterator_tag>::value>::type,
					Enable2>
					{
						static T call (const InIter& first, Size n, const T& init, BOp op)
							{
								InIter first_copy = first;
								T init_copy = init;		// param init may be an aligned object, which causes MSC to choke and die...
								for (Size index = 0; index != n; ++index)
									init_copy = op (init_copy, first_copy [index]);
								return init_copy;
							}
					};

				template <typename InIter, typename Size, typename Enable2>
					struct accumulate_n_dispatch <InIter, Size, bool, stdext::minimum <bool>,
					typename enable_if <is_same <typename std::iterator_traits <InIter>::iterator_category, std::random_access_iterator_tag>::value>::type,
					Enable2>
					{
						static bool call (const InIter& first, Size n, bool init, stdext::minimum <bool>)
							{
								if (init)
									{
										InIter first_copy = first;
										for (Size index = 0; index != n; ++index)
											if (!first_copy [index])
												return false;
										return true;
									}
								else
									return false;
							}
					};

				template <typename InIter, typename Size, typename Enable2>
					struct accumulate_n_dispatch <InIter, Size, bool, stdext::maximum <bool>,
					typename enable_if <is_same <typename std::iterator_traits <InIter>::iterator_category, std::random_access_iterator_tag>::value>::type,
					Enable2>
					{
						static bool call (const InIter& first, Size n, bool init, stdext::maximum <bool>)
							{
								if (init)
									return true;
								else
									{
										InIter first_copy = first;
										for (Size index = 0; index != n; ++index)
											if (first_copy [index])
												return true;
										return false;
									}
							}
					};

			}
			
		template <typename InIter, typename Size, typename T, typename BOp>
			inline T accumulate_n (const InIter& first, Size n, const T& init, BOp op)
			{
				return impl::accumulate_n_dispatch <InIter, Size, T, BOp>::call (first, n, init, op);
			}
			
	}

#endif
