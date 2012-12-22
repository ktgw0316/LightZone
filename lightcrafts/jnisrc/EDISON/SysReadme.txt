Edge Detection and Image SegmentatiON (EDISON) System ver1.0 README
-------------------------------------------------------------------

System Overview:
===============

The EDISON system is a low-level vision tool that performs confidence based edge detection and synergistic image segmentation. It is packaged under a platform independent graphical user interface. A command prompt version is also provided.

Table of Contents:
-------------------

(A) System Overview
(B) Help Files
(C) Current Version Information
(D) Using wxWindows
(E) References
(F) Contact Information

================================================================================================

(A) System Overview

-------------------------------------------------------------------------------------------------

The low-level image processor system is comprised of four components:

(1) An image segmentation module consisting of a collection of classes that are designed to
    perform mean shift based and synergistic segmentation.
(2) An edge detection module consisting of a collection of classes that are designed to perform
    confidence based edge detection.
(3) A platform independent graphical user interface programmed using wxWindows.
(4) A command prompt version.

The code for each module is available in its own directory. The directory structure is outlined below:

 - segm  : code for image segmentation
 - edge  : code for edge detection
 - GUI   : code for graphical user interface
 - prompt: code for system command prompt

================================================================================================

(B) Help Files

-------------------------------------------------------------------------------------------------

The system help files are located in the 'help' directory. This directory contains the help files needed for both the graphical user interface and command prompt versions of the system. The 'doc' directory contains the help files needed for the graphical user interface. This directory must be placed in the same directory as the program executable to be used with the system. The file 'specification.html' contains the command prompt version specification.

================================================================================================

(C) Current Version Info

-------------------------------------------------------------------------------------------------

Both versions of the system were tested under a Windows environment. The command prompt version was also tested under UNIX. The graphical user interface was compiled using Microsoft Visual C++. The project files used to package the system code are located in the 'project' directory under the project name 'EDISON'. The makefile needed to compile the command prompt is also located in this directory. For any further questions on the compilation of this code, please contact us using the provided contact information below.

Although our system is thoroughly tested bugs may still exist. We would greatly appreciate any bug reports you may provide to assist us in building a more stable system.

================================================================================================

(D) Using wxWindows

-------------------------------------------------------------------------------------------------

In order to compile the graphical user interface version of EDISON wxWindows must be download and installed. It may be obtained from the wxWindows web site at http://www.wxWindows.org.

================================================================================================

(E) References

-------------------------------------------------------------------------------------------------

[1] D. Comanicu, P. Meer: "Mean shift: A robust approach toward feature space analysis".
    IEEE Trans. Pattern Anal. Machine Intell., May 2002.

[2] P. Meer, B. Georgescu: "Edge detection with embedded confidence". IEEE Trans. Pattern Anal.
    Machine Intell., 28, 2001.

[3] C. Christoudias, B. Georgescu, P. Meer: "Synergism in low level vision". 16th International
    Conference of Pattern Recognition, Track 1 - Computer Vision and Robotics, Quebec City,
    Canada, August 2001.

================================================================================================

(F) Contact Information

-------------------------------------------------------------------------------------------------

Personal Contact Information
----------------------------

Email:

	cmch@caip.rutgers.edu		(Chris M. Christoudias)
	georgesc@caip.rutgers.edu	(Bogdan Georgescu)

Laboratory Contact Information
------------------------------

Laboratory Website:
	
	www.caip.rutgers.edu/riul/

================================================================================================
