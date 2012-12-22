/*
 *  channel.h
 *  macstl
 *
 *  Created by Glen Low on Dec 12 2004.
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

#ifndef MACSTL_CHANNEL_H
#define MACSTL_CHANNEL_H

#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>

#include <iostream>

#include "impl/config.h"

namespace macstl
	{
		template <typename T> class mmapping;
		
		/// File handle.
		
		/// Wraps the UNIX file handle in RAII fashion. Constructing opens the file and destructing closes it, so using this as an auto variable saves
		/// you from manually tracking all file closures. Errors also cause exceptions rather than returning an error status.
		///
		/// @header	#include <macstl/channel.h>

		class channel
			{
				public:
					/// Constructs without any referenced file.
					channel (): fd_ (-1)
						{
						}
						
					/// Open the file at @a path with open @a flags and permission @a mode.
					explicit channel (const char* path, int flags = O_RDONLY, mode_t mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH):
						fd_ (::open (path, flags, mode))
						{
							if (fd_ == -1)
								throw std::ios::failure ("open");
						}
			
					/// Closes the file.
					~channel ()
						{
							::close (fd_);
						}
						
					/// Changes the working directory to this file.
					void chdir ()
						{
							if (::fchdir (fd_) == -1)
								throw std::ios::failure ("chdir");
						}
						
					/// Changes the file ownership to @a owner and @a group.
					void chown (uid_t owner, gid_t group)
						{
							if (::fchown (fd_, owner, group) == -1)
								throw std::ios::failure ("chown");
						}
						
					/// Gets the flags.
					int flags ()
						{
							int result = ::fcntl (fd_, F_GETFL, 0);
							if (result == -1)
								throw std::ios::failure ("status");
							else
								return result;
						}
						
					/// Sets the @a flags.
					void flags (int flags)
						{
							if (::fcntl (fd_, F_SETFL, flags) == -1)
								throw std::ios::failure ("status");
						}
						
					/// Reads @nbytes bytes into @a buffer.
					std::size_t read (void* buffer, std::size_t nbytes)
						{
							ssize_t result = ::read (fd_, buffer, nbytes);
							if (result == -1)
								throw std::ios::failure ("read");
							else
								return (std::size_t) result;
						}
						
					/// Positions the file at @a offset bytes.
					off_t seek (off_t offset = 0, int whence = SEEK_SET)
						{
							off_t result = ::lseek (fd_, offset, whence);
							if (result == -1)
								throw std::ios::failure ("seek");
							else
								return result;
						}

					/// Synchronizes with the file system. 
					void sync ()
						{
							if (::fsync (fd_) == -1)
								throw std::ios::failure ("sync");
						}

					/// Truncates the file to @a length bytes.
					void truncate (off_t length)
						{
							if (::ftruncate (fd_, length) == -1)
								throw std::ios::failure ("truncate");
						}
						
					/// Writes @a nbytes bytes from @a buffer.
					std::size_t write (const void* buffer, std::size_t nbytes)
						{
							ssize_t result = ::write (fd_, buffer, nbytes);
							if (result == -1)
								throw std::ios::failure ("write");
							else
								return (std::size_t) result;
						}
			
				private:
					int fd_;
					
					channel (const channel&);
					channel& operator= (const channel&);
					
					template <typename T> friend class mmapping;
			};
		}
	
#endif

