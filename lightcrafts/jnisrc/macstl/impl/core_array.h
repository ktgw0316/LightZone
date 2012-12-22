/*
 *  core_array.h
 *  macstl
 *
 *  Created by Glen Low on Jan 16 2005.
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

#ifndef MACSTL_IMPL_CORE_ARRAY_H
#define MACSTL_IMPL_CORE_ARRAY_H

namespace macstl
	{
		namespace impl
			{
				struct retain_tag;

				template <typename T> class core_array_iterator
					{
						public:
							typedef typename std::random_access_iterator_tag iterator_category;
							typedef T value_type; 
							typedef CFIndex difference_type;
							typedef const value_type* pointer;
							
							class reference
								{
									public:
										operator T () const						{ return reinterpret_cast <T> (CFArrayGetValueAtIndex (array_, index_)); }
										reference& operator= (const T& val)		{ CFArraySetValueAtIndex (array_, index_, reinterpret_cast <const void* const&> (val));  return *this; }

										reference (CFMutableArrayRef array, difference_type index): array_ (array), index_ (index)
											{
											}
									
									private:
										CFMutableArrayRef array_;
										CFIndex index_;
								};

							core_array_iterator (): array_ (NULL), index_ (0)
								{
								}
							
							core_array_iterator (CFMutableArrayRef array, difference_type index): array_ (array), index_ (index)
								{
								}
								
							CFMutableArrayRef array () const	{ return array_; }
							difference_type index () const		{ return index_; }

							reference operator* () const						{ return reference (array_, index_); }
							reference operator[] (difference_type i) const		{ return reference (array_, index_ + i); }

							core_array_iterator& operator++ ()					{ ++index_; return *this; }
							core_array_iterator operator++ (int)				{ return core_array_iterator (array_, index_++); }
							core_array_iterator& operator+= (difference_type n)	{ index_ += n; return *this; }
		
							core_array_iterator& operator-- ()					{ --index_; return *this; }
							core_array_iterator operator-- (int)				{ return core_array_iterator (array_, index_--); }
							core_array_iterator& operator-= (difference_type n)	{ index_ -= n; return *this; }
								
							friend core_array_iterator operator+ (const core_array_iterator& left, difference_type right)
								{
									return core_array_iterator (left.array_, right);
								}
		
							friend core_array_iterator operator+ (difference_type left, const core_array_iterator& right)
								{
									return core_array_iterator (right.array_, left + right.index_);
								}
		
							friend core_array_iterator operator- (const core_array_iterator& left, difference_type right)
								{
									return core_array_iterator (left.array_, left.index_ - right);
								}
							
							friend difference_type operator- (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ - right.index_;
								}
								
							friend bool operator== (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ == right.index_;
								}
								
							friend bool operator!= (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ != right.index_;
								}
		
							friend bool operator< (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ < right.index_;
								}
								
						private:
							CFMutableArrayRef array_;
							difference_type index_;
					};

				template <typename T> class core_array_iterator <const T>
					{
						public:
							typedef typename std::random_access_iterator_tag iterator_category;
							typedef T value_type; 
							typedef CFIndex difference_type;
							typedef const value_type* pointer;
							typedef value_type reference;
							
							core_array_iterator (): array_ (NULL), index_ (0)
								{
								}
							
							core_array_iterator (CFArrayRef array, difference_type index): array_ (array), index_ (index)
								{
								}

							core_array_iterator (const core_array_iterator <T>& other): array_ (other.array_), index_ (other.index_)
								{
								}
								
							CFArrayRef array () const			{ return array_; }
							difference_type index () const		{ return index_; }

							value_type operator* () const					{ const void* val = CFArrayGetValueAtIndex (array_, index_); return reinterpret_cast <const T&> (val); }
							value_type operator[] (difference_type i) const	{ const void* val = CFArrayGetValueAtIndex (array_, index_ + i); return reinterpret_cast <const T&> (val); }
								
							core_array_iterator& operator++ ()					{ ++index_; return *this; }
							core_array_iterator operator++ (int)					{ return core_array_iterator (array_, index_++); }
							core_array_iterator& operator+= (difference_type n)	{ index_ += n; return *this; }
		
							core_array_iterator& operator-- ()					{ --index_; return *this; }
							core_array_iterator operator-- (int)					{ return core_array_iterator (array_, index_--); }
							core_array_iterator& operator-= (difference_type n)	{ index_ -= n; return *this; }
								
							friend core_array_iterator operator+ (const core_array_iterator& left, difference_type right)
								{
									return core_array_iterator (left.array_, right);
								}
		
							friend core_array_iterator operator+ (difference_type left, const core_array_iterator& right)
								{
									return core_array_iterator (right.array_, left + right.index_);
								}
		
							friend core_array_iterator operator- (const core_array_iterator& left, difference_type right)
								{
									return core_array_iterator (left.array_, left.index_ - right);
								}
							
							friend difference_type operator- (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ - right.index_;
								}
								
							friend bool operator== (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ == right.index_;
								}
								
							friend bool operator!= (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ != right.index_;
								}
		
							friend bool operator< (const core_array_iterator& left, const core_array_iterator& right)
								{
									return left.index_ < right.index_;
								}
								
						private:
							CFArrayRef array_;
							difference_type index_;
					};

				template <typename T, typename Fn> void array_applier (const void* val, void* context)
					{
						(*reinterpret_cast <Fn*> (context))
							(reinterpret_cast <const T&> (val));
					}

				template <typename T, typename Fn> CFComparisonResult comparator (const void* val1, const void* val2, void* context)
					{
						Fn& fn = *reinterpret_cast <Fn*> (context);
						const T& t1 = reinterpret_cast <const T&> (val1);
						const T& t2 = reinterpret_cast <const T&> (val2);
						
						if (fn (t1, t2))
							return kCFCompareLessThan;
						else if (fn (t2, t1))
							return kCFCompareGreaterThan;
						else
							return kCFCompareEqualTo;						
					}

				template <typename T> CFComparisonResult comparator_less (const void* val1, const void* val2, void*)
					{
						const T& t1 = reinterpret_cast <const T&> (val1);
						const T& t2 = reinterpret_cast <const T&> (val2);
						
						if (t1 < t2)
							return kCFCompareLessThan;
						else if (t2 < t1)
							return kCFCompareGreaterThan;
						else
							return kCFCompareEqualTo;						
					}
					
				template <typename T> const void* retainer (CFAllocatorRef, const void* val)
					{
						const void* retained;
						new (&retained) T (reinterpret_cast <const T&> (val));
						return retained;
					}
					
				template <typename T> void releaser (CFAllocatorRef, const void* val)
					{
						reinterpret_cast <const T&> (val).~T ();
					}
					
				template <typename T> CFStringRef describer (const void *val)
					{
						std::ostringstream os;
						os << reinterpret_cast <const T&> (val);
						return CFStringCreateWithCString (kCFAllocatorDefault, os.str ().c_str (), kCFStringEncodingUTF8);
					}
					
				template <typename T> Boolean equaller (const void* val1, const void* val2)
					{
						return reinterpret_cast <const T&> (val1) == reinterpret_cast <const T&> (val2);
					}


				template <typename T, typename Iter> typename stdext::impl::enable_if <stdext::impl::is_integral <Iter>::value == 0>::type
					inline array_insert_dispatch (core_array_iterator <T> pos, Iter first, Iter last)
					{
						for (; first != last; ++pos, ++first)
							CFArrayInsertValueAtIndex (pos.array (), pos.index (), reinterpret_cast <const void* const&> (*first));
						return (typename stdext::impl::enable_if <stdext::impl::is_integral <Iter>::value == 0>::type) NULL;
					}

				template <typename T, typename Iter> typename stdext::impl::enable_if <stdext::impl::is_integral <Iter>::value != 0>::type
					inline array_insert_dispatch (core_array_iterator <T> pos, Iter n, Iter val)
					{
						for (; n > 0; --n)
							CFArrayInsertValueAtIndex (pos.array (), pos.index (), reinterpret_cast <const void* const&> (val));
						return (typename stdext::impl::enable_if <stdext::impl::is_integral <Iter>::value != 0>::type) NULL;
					}

			}
			
		template <typename T> class refptr;
		template <typename T> class core_array;
			
		/// Core Foundation constant array.
		
		/// Puts the STL const Back Insertion Sequence interface onto Core Foundation CFArrayRef. Several standard algorithms are also specialized
		/// to call the appropriate CFArrayRef functions. You get better interoperability with C++ objects and STL algorithms,
		/// more intuitive and typesafe indexing syntax and automatic release within scope.
		///
		/// The array maps the required Core Foundation callbacks to standard C++ object behavior:
		/// - retain is copy construct
		/// - release is destruct
		/// - copy description is stream output to a string
		/// - equal is operator==
		/// .
		///
		/// @param	T	The element value type, sizeof (T) == size (void*).
		///
		/// @header	#include <macstl/core.h>

		template <typename T> class const_core_array
			{
				public:
					typedef T value_type;
					typedef impl::core_array_iterator <T> iterator;
					typedef impl::core_array_iterator <const T> const_iterator;
					typedef std::reverse_iterator <iterator> reverse_iterator;
					typedef std::reverse_iterator <const_iterator> const_reverse_iterator;
					typedef typename iterator::reference reference;
					typedef typename const_iterator::reference const_reference;
					typedef typename const_iterator::pointer pointer;
					typedef typename const_iterator::difference_type difference_type;
					typedef CFIndex size_type;
					
					typedef CFArrayRef data_type;
					
					/// @name Constructors and Destructors
					
					//@{
					
					/// Constructs an array with @a n elements from @a vals, using the @a allocator.
					const_core_array (T* vals, CFIndex n, CFAllocatorRef allocator = kCFAllocatorDefault):
						array_ (CFArrayCreate (allocator, reinterpret_cast <void**> (vals), n, &callbacks))
						{
						}
					
					/// Copies the array, using the @a allocator.
					const_core_array (const const_core_array& other):
						array_ (CFArrayCreateCopy (kCFAllocatorDefault, other.array_))
						{
						}

					/// Copies the array, using the @a allocator.
					const_core_array (const core_array <T>& other, CFAllocatorRef allocator = kCFAllocatorDefault):
						array_ (CFArrayCreateCopy (allocator, other.array_))
						{
						}
						
					/// Destructs the array.
					~const_core_array ()
						{
							CFRelease (array_);
						}
						
					//@}
					
					/// @name Data
					
					//@{
						
					/// Gets the raw data.
					CFArrayRef data () const	{ return array_; }

					//@}
					
					/// @name Iterators
					
					//@{
						
					/// Gets an iterator to the first element.
					const_iterator begin () const		{ return const_iterator (array_, 0); }

					/// Gets an iterator to the past-the-last element.
					const_iterator end () const			{ return const_iterator (array_, size ()); }

					/// Gets a reverse iterator to the last element.
					const_reverse_iterator rbegin () const		{ return const_reverse_iterator (end ()); }
					
					/// Gets a reverse iterator to the past-the-first element.
					const_reverse_iterator rend () const		{ return const_reverse_iterator (begin ()); }
					
					//@}
					
					/// @name References
					
					//@{
					
					/// Gets the element at index @a i.
					const_reference operator[] (size_type i) const	{ const void* val = CFArrayGetValueAtIndex (array_, i); return reinterpret_cast <const T&> (val); }

					/// Gets the element at index @a i, if within bounds.
					const_reference at (size_type i) const
						{
							if (i < 0 || i >= size ())
								throw std::out_of_range ("at");
							return operator[] (i);
						}

					/// Gets the first element.
					const_reference front () const					{ return operator[] (0); }
					
					/// Gets the last element.
					const_reference back () const					{ return operator[] (size () - 1); }
					
					//@}
					
					/// @name Sizers
					
					//@{
					
					/// Gets the number of elements.
					size_type size () const			{ return CFArrayGetCount (array_); }
					
					/// Tests whether there are no more elements.
					bool empty () const				{ return size () == 0; }
					
					//@}
					
					/// Gets the maximum number of elements.
					static size_type max_size ()	{ return -1; }
					
				protected:
					union
						{
							CFArrayRef array_;
							CFMutableArrayRef mutable_array_;
						};
						
					const_core_array (CFMutableArrayRef mutable_array): mutable_array_ (mutable_array)
						{
						}
						
					static const CFArrayCallBacks callbacks;
					
				private:
					// constant arrays cannot be assigned to
					const_core_array& operator= (const const_core_array& other);
					
					// refptr interface
					
					const_core_array (data_type array, impl::retain_tag*): array_ ((CFArrayRef) CFRetain (array))
						{
						}

					const_core_array (const core_array <T>& other, impl::retain_tag*): array_ ((CFArrayRef) CFRetain (other.array_))
						{
						}

					friend class refptr <const_core_array>;

			};
			
		template <typename T> const CFArrayCallBacks const_core_array <T>::callbacks = 
			{
				0,	// version
				&impl::retainer <T>,
				&impl::releaser <T>,
				&impl::describer <T>,
				&impl::equaller <T>			
			};

		/// @relates const_core_array
		/// @brief Tests whether @a lhs has equal elements to @a rhs.
		template <typename T> bool operator== (const const_core_array <T>& lhs, const const_core_array <T>& rhs)
			{
				return CFEqual (lhs.data (), rhs.data ());
			}

		/// @relates const_core_array
		/// @brief Tests whether @a lhs is lexicographically less than @a rhs.
		template <typename T> bool operator< (const const_core_array <T>& lhs, const const_core_array <T>& rhs)
			{
				return std::lexicographical_compare (lhs.begin (), lhs.end (), rhs.begin (), rhs.end ());
			}

		/// Core Foundation mutable array.
		
		/// Puts the STL Back Insertion Sequence interface on Core Foundation CFMutableArrayRef. Several standard algorithms are also specialized
		/// to call the appropriate CFMutableArrayRef functions. You get better interoperability with C++ objects and STL algorithms,
		/// more natural indexing syntax and automatic release within scope.
		///
		/// The array maps the required Core Foundation callbacks to standard C++ object behavior:
		/// - retain is copy construct
		/// - release is destruct
		/// - copy description is stream output to a string
		/// - equal is operator==
		/// .
		///
		/// @param	T	The element value type, sizeof (T) == size (void*). 
		///
		/// @header	#include <macstl/core.h>

		template <typename T> class core_array: public const_core_array <T>
			{
				public:
					typedef const_core_array <T> base;
					typedef CFMutableArrayRef data_type;
					
					/// @name Constructors
					
					//@{
					
					/// Constructs an empty array, using the @a allocator.
					core_array (CFAllocatorRef allocator = kCFAllocatorDefault):
						base (CFArrayCreateMutable (allocator, 0, &base::callbacks))
						{
						}
						
					/// Constructs an array with @a n copies of @a val, using the @a allocator.
					core_array (typename base::size_type n, const T& val = T (), CFAllocatorRef allocator = kCFAllocatorDefault):
						base (CFArrayCreateMutable (allocator, 0, &base::callbacks))
						{
							insert (begin (), n, val);
						}

					/// Constructs an array with the range from @a first to @a last, using the @a allocator.
					template <typename Iter> core_array (Iter first, Iter last, CFAllocatorRef allocator = kCFAllocatorDefault):
						base (CFArrayCreateMutable (allocator, 0, &base::callbacks))
						{
							insert (begin (), first, last);
						}
					
					/// Copies the array, using the @a allocator.
					core_array (const core_array& other, CFAllocatorRef allocator = kCFAllocatorDefault):
						base (CFArrayCreateMutableCopy (allocator, 0, other.array_))
						{
						}
						
					//@}
					
					/// @name Data
					
					//@{
					
					/// Gets the raw data.
					CFMutableArrayRef data () const		{ return base::mutable_array_; }
					
					//@}
					
					using base::begin;
					using base::end;
					using base::rbegin;
					using base::rend;
					using base::operator[];
					using base::at;
					using base::front;
					using base::back;

					/// @name Assignments
					
					//@{
					
					/// Assigns the @a other array.
					core_array& operator= (const core_array& other)
						{
							if (&other != this)
								{
									// might consider replacing this with CFArrayReplaceValues and the ilk?
									core_array temp (other);
									swap (temp);
								}
							return *this;
						}
					
					/// Swaps contents with @a other array.
					void swap (core_array <T>& other)	{ std::swap (base::mutable_array_, other.mutable_array_); }
					
					//@}
										
					/// @name Iterators
					
					//@{
					
					/// Gets an iterator to the first element.
					typename base::iterator begin ()	{ return typename base::iterator (base::mutable_array_, 0); }
					
					/// Gets an iterator to the past-the-last element.
					typename base::iterator end ()		{ return typename base::iterator (base::mutable_array_, base::size ()); }

					/// Gets a reverse iterator to the last element.
					typename base::reverse_iterator rbegin ()	{ return typename base::reverse_iterator (end ()); }
					
					/// Gets a reverse iterator to the past-the-first element.
					typename base::reverse_iterator rend ()		{ return typename base::reverse_iterator (begin ()); }

					//@}
					
					/// @name References
					
					//@{
					
					/// Gets the element at index @a i.
					typename base::reference operator[] (typename base::size_type i)	{ return typename base::reference (base::mutable_array_, i); }
					
					/// Gets the element at index @a i, if within bounds.
					typename base::reference at (typename base::size_type i)
						{
							if (i < 0 || i >= base::size ())
								throw std::out_of_range ("at");
							return operator[] (i);
						}
					
					/// Gets the first element.
					typename base::reference front ()									{ return operator[] (0); }
					
					/// Gets the last element.
					typename base::reference back ()									{ return operator[] (base::size () - 1); }
					
					//@}
										
					/// @name Inserters
					
					//@{
					
					/// Inserts at @a pos a copy of @a val.
					typename base::iterator insert (typename base::iterator pos, const T& val)
						{
							CFArrayInsertValueAtIndex (pos.array (), pos.index (), reinterpret_cast <const void* const&> (val));
							return pos;
						}
						
					/// Inserts at @a pos @a n copies of @a val.
					void insert (typename base::iterator pos, typename base::size_type n, const T& val)
						{
							for (; n > 0; --n)
								CFArrayInsertValueAtIndex (pos.array (), pos.index (), reinterpret_cast <const void* const&> (val));
						}

					/// Inserts at @a pos the range from @a first to @a last.
					template <typename Iter> void insert (typename base::iterator pos, Iter first, Iter last)
						{
							impl::array_insert_dispatch (pos, first, last);
						}
						
					void insert (typename base::iterator pos, typename base::const_iterator first, typename base::const_iterator last)
						{
							if (pos.index () == base::size ())
								CFArrayAppendArray (pos.array (), first.array (), CFRangeMake (first.index (), last.index () - first.index ()));
							else
								for (; first != last; ++pos, ++first)
									CFArrayInsertValueAtIndex (pos.array (), pos.index (), reinterpret_cast <const void* const&> (*first));
						}

					/// Inserts the value @a val at the end.
					void push_back (const T& val)
						{
							CFArrayAppendValue (base::mutable_array_, reinterpret_cast <void* const&> (val));
						}

					//@}
					
					/// @name Erasers
					
					//@{
					
					/// Erases the element at @a pos.
					typename base::iterator erase (typename base::iterator first)
						{
							assert (first.array () == base::mutable_array_);
							CFArrayRemoveValueAtIndex (first.array (), first.index ());
							return first;
						}

					/// Erases the range @a first to @a last.
					typename base::iterator erase (typename base::iterator first, typename base::iterator last)
						{
							assert (first.array () == base::mutable_array_);
							assert (last.array () == base::mutable_array_);
							for (CFIndex count = first.index (); count != last.index (); ++count)
								CFArrayRemoveValueAtIndex (first.array (), first.index ());
							return first;
						}

					/// Erases entire array.
					void clear ()
						{
							CFArrayRemoveAllValues (base::mutable_array_);
						}
						
					/// Erases the last element.
					void pop_back ()
						{
							CFArrayRemoveValueAtIndex (base::mutable_array_, base::size () - 1);
						}
					
					//@}
					
					/// @name Resizers
					
					//@{
					
					/// Changes the size of the array to @a sz, filling with @a val elements if necessary.
					void resize (typename base::size_type sz, const T& val = T ())
						{
							if (sz < base::size ())
								erase (begin () + sz, end ());
							else
								insert (end (), sz - base::size (), val);
						}
						
					//@}
					
				private:
					// refptr interface
					
					core_array (data_type array, impl::retain_tag*): base ((CFMutableArrayRef) CFRetain (array))
						{
						}

					core_array (const core_array& other, impl::retain_tag*): base ((CFMutableArrayRef) CFRetain (other.array_))
						{
						}

					friend class refptr <core_array>;

			};
	
	}
	
namespace std
	{
		template <typename T, typename UnaryFunction>
			inline UnaryFunction for_each (macstl::impl::core_array_iterator <T> first, macstl::impl::core_array_iterator <T> last, UnaryFunction f)
			{
				assert (first.array () == last.array ());
				CFRange range = {first.index (), last.index () - first.index ()};			
				CFArrayApplyFunction (first.array (), range,
					&macstl::impl::array_applier <T, UnaryFunction>, &f);
				return f;
			}

		template <typename T>
			inline CFIndex count (macstl::impl::core_array_iterator <T> first, macstl::impl::core_array_iterator <T> last, const T& value)
			{
				assert (first.array () == last.array ());
				CFRange range = {first.index (), last.index () - first.index ()};			
				return CFArrayGetCountOfValue (first.array (), range, reinterpret_cast <const void* const&> (value));
			}
			
		template <typename T>
			inline CFIndex count (macstl::impl::core_array_iterator <const T> first, macstl::impl::core_array_iterator <const T> last, const T& value)
			{
				assert (first.array () == last.array ());
				CFRange range = {first.index (), last.index () - first.index ()};			
				return CFArrayGetCountOfValue (first.array (), range, reinterpret_cast <const void* const&> (value));
			}

		template <typename T>
			inline macstl::impl::core_array_iterator <T> find (macstl::impl::core_array_iterator <T> first, macstl::impl::core_array_iterator <T> last, const T& value)
			{
				assert (first.array () == last.array ());
				CFRange range = {first.index (), last.index () - first.index ()};			
				CFIndex found = CFArrayGetFirstIndexOfValue (first.array (), range, reinterpret_cast <const void* const&> (value));
				return found == -1 ? last : macstl::impl::core_array_iterator <T> (first.array (), found);
			}

		template <typename T>
			inline macstl::impl::core_array_iterator <const T> find (macstl::impl::core_array_iterator <const T> first, macstl::impl::core_array_iterator <const T> last, const T& value)
			{
				assert (first.array () == last.array ());
				CFRange range = {first.index (), last.index () - first.index ()};			
				CFIndex found = CFArrayGetFirstIndexOfValue (first.array (), range, reinterpret_cast <const void* const&> (value));
				return found == -1 ? last : macstl::impl::core_array_iterator <const T> (first.array (), found);
			}

		template <typename T>
			inline std::reverse_iterator <macstl::impl::core_array_iterator <T> > find (
			std::reverse_iterator <macstl::impl::core_array_iterator <T> > first,
			std::reverse_iterator <macstl::impl::core_array_iterator <T> > last, const T& value)
			{
				macstl::impl::core_array_iterator <T> firstbase = first.base ();
				macstl::impl::core_array_iterator <T> lastbase = last.base ();
				assert (firstbase.array () == lastbase.array ());
				CFRange range = {lastbase.index (), firstbase.index () - lastbase.index ()};			
				CFIndex found = CFArrayGetLastIndexOfValue (firstbase.array (), range, reinterpret_cast <const void* const&> (value));
				return found == -1 ? last : std::reverse_iterator <macstl::impl::core_array_iterator <T> >
					(macstl::impl::core_array_iterator <T> (firstbase.array (), found + 1));
			}

		template <typename T>
			inline std::reverse_iterator <macstl::impl::core_array_iterator <const T> > find (
			std::reverse_iterator <macstl::impl::core_array_iterator <const T> > first,
			std::reverse_iterator <macstl::impl::core_array_iterator <const T> > last, const T& value)
			{
				macstl::impl::core_array_iterator <const T> firstbase = first.base ();
				macstl::impl::core_array_iterator <const T> lastbase = last.base ();
				assert (firstbase.array () == lastbase.array ());
				CFRange range = {lastbase.index (), firstbase.index () - lastbase.index ()};			
				CFIndex found = CFArrayGetLastIndexOfValue (firstbase.array (), range, reinterpret_cast <const void* const&> (value));
				return found == -1 ? last : std::reverse_iterator <macstl::impl::core_array_iterator <const T> >
					(macstl::impl::core_array_iterator <const T> (firstbase.array (), found + 1));
			}

		template <typename T, typename StrictWeakOrdering>
			inline void sort (macstl::impl::core_array_iterator <T> first, macstl::impl::core_array_iterator <T> last, StrictWeakOrdering comp)
			{
				assert (first.array () == last.array ());
				CFRange range = {first.index (), last.index () - first.index ()};			
				CFArraySortValues (first.array (), range,
					&macstl::impl::comparator <T, StrictWeakOrdering>, &comp);
			}

		template <typename T>
			inline void sort (macstl::impl::core_array_iterator <T> first, macstl::impl::core_array_iterator <T> last)
			{
				assert (first.array () == last.array ());
				CFRange range = {first.index (), last.index () - first.index ()};			
				CFArraySortValues (first.array (), range,
					&macstl::impl::comparator_less <T>, NULL);
			}

	}
	
#endif
