/*
 *  valarray_algorithm.h
 *  macstl
 *
 *  Created by Glen Low on Jun 22 2003.
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

#ifndef MACSTL_IMPL_VALARRAY_ALGORITHM_H
#define MACSTL_IMPL_VALARRAY_ALGORITHM_H

namespace stdext
	{
		// forward declared from vec.h -- only used if algorithms instantiated with vec <T, n>
		template <typename F> struct accumulator;

		namespace impl
			{
				template <typename Expr1, typename Expr2, typename Enable = void> struct rechunkable
					{
						enum { value = false };
					};

				template <typename Expr1, typename Expr2> struct rechunkable <Expr1, Expr2,
					typename enable_if <exists <typename Expr1::chunk_iterator>::value != 0 && exists <typename Expr2::const_chunk_iterator>::value != 0>::type>
					{
						enum { value = is_same <
							typename std::iterator_traits <typename Expr1::chunk_iterator>::value_type,
							typename std::iterator_traits <typename Expr1::const_chunk_iterator>::value_type>::value };
					};
										
				// these versions of the std algorithms take valarray expressions (which expose begin (), size () and possibly chunked
				// versions of these members) instead of iterators, and assume that underlying value types are all "valarrayable" i.e.
				// that destruction + copy construction = assignment. This allows dispatching on whether the type has a trivial destructor,
				// which implies that copy construction = assignment, and also on whether the expressions are chunkable.
				
				template <typename T> struct copy_is_assign
					{
						enum { value = has_trivial_copy <T>::value && has_trivial_destructor <T>::value };
					};
				
				// accumulate_array
				
				template <template <typename, typename> class Func, typename Expr>
					typename Expr::value_type accumulate_array (const Expr& expr);

				template <template <typename, typename> class Func, typename Expr, typename Enable1 = void, typename Enable2 = void> struct accumulate_array_dispatch
					{
						static typename Expr::value_type call (const Expr& expr)
							{
								typename Expr::const_iterator iter = expr.begin ();
								typename std::iterator_traits <typename Expr::const_iterator>::value_type init = *iter;
								++iter;
								typedef Func <typename Expr::value_type, typename Expr::value_type> function;
								return stdext::accumulate_n (iter, expr.size () - 1, init, function ());
							}
					};
					
				template <template <typename, typename> class Func, typename Expr, typename Enable2> struct accumulate_array_dispatch <Func, Expr,
					typename enable_if <
						exists <typename stdext::accumulator <Func <
							typename std::iterator_traits <typename Expr::const_chunk_iterator>::value_type,
							typename std::iterator_traits <typename Expr::const_chunk_iterator>::value_type> >::result_type>::value>::type,
					Enable2>
					{
						static typename Expr::value_type tail (const Expr& expr, typename Expr::value_type partial)
							{
								std::size_t size = expr.size ();
								std::size_t tailed = size % std::iterator_traits <typename Expr::const_chunk_iterator>::value_type::length;
				
								// skip over all the chunked parts
								typename Expr::const_iterator iter = expr.begin ();
								std::advance (iter, size - tailed);
								
								typedef Func <typename Expr::value_type, typename Expr::value_type> function;
								return stdext::accumulate_n (iter, tailed, partial, function ());
							}

						static typename Expr::value_type call (const Expr& expr)
							{
								typedef typename std::iterator_traits <typename Expr::const_chunk_iterator>::value_type chunk_type;
								
								typename Expr::const_chunk_iterator iter = expr.chunk_begin ();
								const chunk_type init = *iter;
								++iter;
								
								typedef Func <chunk_type, chunk_type> function;
								return tail (expr,
									macstl::data_of (stdext::accumulator <function> () (stdext::accumulate_n (iter,
										expr.size () / chunk_type::length - 1, init, function ()))));
							}
					};
					
				template <template <typename, typename> class Func, typename Expr>
					inline typename Expr::value_type accumulate_array (const Expr& expr)
					{
						return accumulate_array_dispatch <Func, Expr>::call (expr);
					}
								
				// destroy_array
				
				template <typename Expr, typename Enable = void> struct destroy_array_dispatch
					{
						static void call (Expr& expr)
							{
								stdext::destroy_n (expr.begin (), expr.size ());
							}
					};
				
				template <typename Expr> struct destroy_array_dispatch <Expr,
					typename enable_if <copy_is_assign <typename Expr::value_type>::value>::type>
					{
						static void call (Expr&)
							{
							}
					};
					
				template <typename Expr> struct destroy_array_dispatch <Expr,
					typename enable_if <copy_is_assign <typename Expr::value_type>::value == 0 && exists <typename Expr::chunk_iterator>::value != 0>::type>
					{
						static void tail (Expr& expr)
							{
								std::size_t size = expr.size ();
								std::size_t tailed = size % std::iterator_traits <typename Expr::chunk_iterator>::value_type::length;
						
								// skip over all the chunked parts
								typename Expr::iterator iter = expr.begin ();
								std::advance (iter, size - tailed);
								
								// only copy the part at the tail
								stdext::destroy_n (iter, tailed);
							}
							
						static void call (Expr& expr)
							{
								stdext::destroy_n (expr.chunk_begin (), expr.size () / std::iterator_traits <typename Expr::chunk_iterator>::value_type::length);
								
								tail (expr);
							}
					};
					
				template <typename Expr> inline void destroy_array (Expr& expr)
					{
						destroy_array_dispatch <Expr>::call (expr);
					}

				// copy_array
				// uninitialized_copy_array

				template <typename Expr1, typename Expr2, typename Enable = void> struct copy_array_dispatch
					{
						static void call (Expr1& expr1, const Expr2& expr2)
							{
								stdext::copy_n (expr2.begin (), expr1.size (), expr1.begin ());
							}
					};

				template <typename Expr1, typename Expr2> struct copy_array_dispatch <Expr1, Expr2,
					typename enable_if <rechunkable <Expr1, Expr2>::value>::type>
					{
						static void tail (Expr1& expr1, const Expr2& expr2)
							{
								
								std::size_t size = expr1.size ();
								std::size_t tailed = size % std::iterator_traits <typename Expr1::chunk_iterator>::value_type::length;
				
								// skip over all the chunked parts
								typename Expr1::iterator iter1 = expr1.begin ();
								typename Expr2::const_iterator iter2 = expr2.begin ();
								std::advance (iter1, size - tailed);
								std::advance (iter2, size - tailed);
								
								// only copy the part at the tail
								
								stdext::copy_n (iter2, tailed, iter1);
								
							}
							
						static void call (Expr1& expr1, const Expr2& expr2)
							{
								stdext::copy_n (expr2.chunk_begin (), expr1.size () / std::iterator_traits <typename Expr1::chunk_iterator>::value_type::length, expr1.chunk_begin ());
								tail (expr1, expr2);
							}
					};

				template <typename Expr1, typename Expr2, typename Enable = void> struct uninitialized_copy_array_dispatch
					{
						static void call (Expr1& expr1, const Expr2& expr2)
							{
								stdext::uninitialized_copy_n (expr2.begin (), expr1.size (), expr1.begin ());
							}
					};

				template <typename Expr1, typename Expr2> struct uninitialized_copy_array_dispatch <Expr1, Expr2,
					typename enable_if <copy_is_assign <typename Expr1::value_type>::value>::type>:
					public copy_array_dispatch <Expr1, Expr2, void>
					{
					};
					
				template <typename Expr1, typename Expr2> struct uninitialized_copy_array_dispatch <Expr1, Expr2,
					typename enable_if <copy_is_assign <typename Expr1::value_type>::value == 0 &&
						rechunkable <Expr1, Expr2>::value != 0>::type>
					{
						static void tail (Expr1& expr1, const Expr2& expr2)
							{
								std::size_t size = expr1.size ();
								std::size_t tailed = size % std::iterator_traits <typename Expr1::chunk_iterator>::value_type::length;
				
								// skip over all the chunked parts
								typename Expr1::iterator iter1 = expr1.begin ();
								typename Expr2::const_iterator iter2 = expr2.begin ();
								std::advance (iter1, size - tailed);
								std::advance (iter2, size - tailed);
								
								// only copy the part at the tail
								stdext::uninitialized_copy_n (iter2, tailed, iter1);
							}
							
						static void call (Expr1& expr1, const Expr2& expr2)
							{
								stdext::uninitialized_copy_n (expr2.chunk_begin (), expr1.size () / std::iterator_traits <typename Expr1::chunk_iterator>::value_type::length, expr1.chunk_begin ());
								
								tail (expr1, expr2);
							}
					};

				template <typename Expr1, typename Expr2> inline void copy_array (Expr1& expr1, const Expr2& expr2)
					{
						copy_array_dispatch <Expr1, Expr2>::call (expr1, expr2);
					}

				template <typename Expr1, typename Expr2> inline void uninitialized_copy_array (Expr1& expr1, const Expr2& expr2)
					{
						uninitialized_copy_array_dispatch <Expr1, Expr2>::call (expr1, expr2);
					}

				// other stuf...

				template <typename Expr, typename Enable = void> struct uninitialized_copy_array_ptr_dispatch
					{
						static void call (Expr& expr, const typename Expr::value_type* ptr)
							{
								stdext::uninitialized_copy_n (ptr, expr.size (), expr.begin ());
							}
					};

				template <typename Expr> struct uninitialized_copy_array_ptr_dispatch <Expr,
					typename enable_if <copy_is_assign <typename Expr::value_type>::value>::type>
					{
						static void call (Expr& expr, const typename Expr::value_type* ptr)
							{
								stdext::copy_n (ptr, expr.size (), expr.begin ());
							}
					};

				template <typename Expr> void uninitialized_copy_array_ptr (Expr& expr, const typename Expr::value_type* ptr)
					{
						uninitialized_copy_array_ptr_dispatch <Expr>::call (expr, ptr);
					}

				template <typename Expr> void copy_array_ptr (Expr& expr, const typename Expr::value_type* ptr)
					{
						stdext::copy_n (ptr, expr.size (), expr.begin ());
					}
		
				// fill_array
				// uninitialized_fill_array

				template <typename Expr, typename Enable = void> struct fill_array_dispatch
					{
						static void call (Expr& expr, typename Expr::value_type val)
							{
								stdext::fill_n (expr.begin (), expr.size (), val);
							}
					};

				template <typename Expr> struct fill_array_dispatch <Expr, typename enable_if <exists <typename Expr::chunk_iterator>::value>::type>
					{
						static void tail (Expr& expr, typename Expr::value_type val)
							{
								std::size_t size = expr.size ();
								std::size_t tailed = size % std::iterator_traits <typename Expr::chunk_iterator>::value_type::length;
				
								// skip over all the chunked parts
								typename Expr::iterator iter = expr.begin ();
								std::advance (iter, size - tailed);
								
								// only copy the part at the tail
								stdext::fill_n (iter, tailed, val);
							}
							
						static void call (Expr& expr, typename Expr::value_type val)
							{
								stdext::fill_n (expr.chunk_begin (), expr.size () / std::iterator_traits <typename Expr::chunk_iterator>::value_type::length,
									std::iterator_traits <typename Expr::chunk_iterator>::value_type::fill (val));
									
								tail (expr, val);
							}
					};

				template <typename Expr, typename Enable = void> struct uninitialized_fill_array_dispatch
					{
						static void call (Expr& expr, typename Expr::value_type val)
							{
								stdext::uninitialized_fill_n (expr.begin (), expr.size (), val);
							}
					};
					
				template <typename Expr> struct uninitialized_fill_array_dispatch <Expr,
					typename enable_if <copy_is_assign <Expr>::value>::type>:
					public fill_array_dispatch <Expr, void>
					{
					};

				template <typename Expr> struct uninitialized_fill_array_dispatch <Expr,
					typename enable_if <copy_is_assign <Expr>::value == 0 && exists <typename Expr::chunk_iterator>::value != 0>::type>
					{
						static void tail (Expr& expr, typename Expr::value_type val)
							{
								std::size_t size = expr.size ();
								std::size_t tailed = size % std::iterator_traits <typename Expr::chunk_iterator>::value_type::length;
				
								// skip over all the chunked parts
								typename Expr::iterator iter = expr.begin ();
								std::advance (iter, size - tailed);
								
								// only copy the part at the tail
								stdext::fill_n (iter, tailed, val);
							}
							
						static void call (Expr& expr, typename Expr::value_type val)
							{
								stdext::fill_n (expr.chunk_begin (), expr.size () / std::iterator_traits <typename Expr::chunk_iterator>::value_type::length,
									std::iterator_traits <typename Expr::chunk_iterator>::value_type::fill (val));
								tail (expr, val);
							}
					};

				template <typename Expr> inline void fill_array (Expr& expr, typename Expr::value_type val)
					{
						fill_array_dispatch <Expr>::call (expr, val);
					}

				template <typename Expr> inline void uninitialized_fill_array (Expr& expr, typename Expr::value_type val)
					{
						uninitialized_fill_array_dispatch <Expr>::call (expr, val);
					}		
			}
	}

#endif
