#
# spec file for package lightzone
#

Name:           lightzone
Version:	3.9.1
Release:	14beta
License:	GPLv2+
Summary:	Open-source digital darkroom software
Url:		http://lightzoneproject.org/
Group:		Productivity/Graphics/Convertors 
Source:		%{name}-%{version}.tar.gz
#Patch:
BuildRequires:	ant, autoconf, automake, nasm, gcc, gcc-c++, libtool, make, tidy, git, javahelp2

%if 0%{?fedora}
BuildRequires: java-1.7.0-openjdk-devel, libX11-devel, xz-libs
%define debug_package %{nil}
%endif
%if 0%{?sles_version}
BuildRequires: java-1_6_0-openjdk-devel, xorg-x11-libX11-devel, liblzma5
%endif
%if 0%{?suse_version} == 1210
BuildRequires: java-1_6_0-openjdk-devel, xorg-x11-libX11-devel, liblzma5
%endif
%if 0%{?suse_version} > 1210
BuildRequires: java-1_7_0-openjdk-devel, libX11-devel, liblzma5
%endif
%if 0%{?centos_version}
BuildRequires: java-1.6.0-openjdk-devel, libX11-devel, liblzma5
%define debug_package %{nil}
%endif
%if 0%{?mdkversion}
BuildRequires: java-1.6.0-openjdk-devel, libX11-devel, liblzma5
%endif

Requires:	java >= 1.6.0
Obsoletes:	lightzone < %{version}
Provides:	lightzone = %{version}
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Packager:	Andreas Rother
#Prefix:	/opt
#BuildArch:	noarch
%description
LightZone is professional-level digital darkroom software for Windows, Mac OS X, and Linux. Rather than using layers as many other photo editors do, LightZone lets the user build up a stack of tools which can be rearranged, turned off and on, and removed from the stack. It's a non-destructive editor, where any of the tools can be re-adjusted or modified later — even in a different editing session. A tool stack can be copied to a batch of photos at one time. LightZone operates in a 16-bit linear color space with the wide gamut of ProPhoto RGB.

%prep
%setup -q

%build
%ant -f linux/build.xml jar

%install
%if 0%{?sles_version}
export NO_BRP_CHECK_BYTECODE_VERSION=true
%endif

%define instdir /opt/%{name}
install -dm 0755 "%buildroot/%{instdir}"
cp -rpH lightcrafts/products/dcraw "%buildroot/%{instdir}"
cp -rpH lightcrafts/products/LightZone-forkd "%buildroot/%{instdir}"
cp -rpH linux/products/*.so "%buildroot/%{instdir}"
cp -rpH linux/products/*.jar "%buildroot/%{instdir}"
cp -rpH linux/products/lightzone "%buildroot/opt/%name"

# create icons and shortcuts
%define icondir usr/share/icons/hicolor/
install -dm 0755 "%buildroot/usr/share/applications"
install -dm 0755 "%buildroot/%icondir/256x256/apps"
install -dm 0755 "%buildroot/%icondir/128x128/apps"
install -dm 0755 "%buildroot/%icondir/64x64/apps"
install -dm 0755 "%buildroot/%icondir/48x48/apps"
install -dm 0755 "%buildroot/%icondir/32x32/apps"
install -dm 0755 "%buildroot/%icondir/16x16/apps"

cp -rpH linux/products/lightzone.desktop "%buildroot/usr/share/applications/"
cp -rpH linux/icons/LightZone_256x256.png "%buildroot/%icondir/256x256/apps/LightZone.png"
cp -rpH linux/icons/LightZone_128x128.png "%buildroot/%icondir/128x128/apps/LightZone.png"
cp -rpH linux/icons/LightZone_64x64.png "%buildroot/%icondir/64x64/apps/LightZone.png"
cp -rpH linux/icons/LightZone_48x48.png "%buildroot/%icondir/48x48/apps/LightZone.png"
cp -rpH linux/icons/LightZone_32x32.png "%buildroot/%icondir/32x32/apps/LightZone.png"
cp -rpH linux/icons/LightZone_16x16.png "%buildroot/%icondir/16x16/apps/LightZone.png"

install -d -m 755 %{buildroot}%{_bindir}
install -m 755 linux/products/%{name} %{buildroot}%{_bindir}/

%post

%postun

%files
%defattr(-,root,root)
%doc COPYING README.md linux/BUILD-Linux.md
/opt/%name
/usr/bin/%name
/usr/share/applications/lightzone.desktop
/usr/share/icons/hicolor/256x256/apps/LightZone.png
/usr/share/icons/hicolor/128x128/apps/LightZone.png
/usr/share/icons/hicolor/64x64/apps/LightZone.png
/usr/share/icons/hicolor/48x48/apps/LightZone.png
/usr/share/icons/hicolor/32x32/apps/LightZone.png
/usr/share/icons/hicolor/16x16/apps/LightZone.png

%changelog
* Sat May 11 2013 Andreas Rother <andreas@rother.org>
- Fixed issue #23: horizontal line artifacts
* Wed May 08 2013 Andreas Rother <andreas@rother.org>
- added menu entry and icons
* Sun May 05 2013 Andreas Rother <andreas@rother.org>
- changes prefix to /opt/lightzone
* Sat May 04 2013 Andreas Rother <andreas@rother.org>
- changes in lightzone startscript
* Mon Mar 18 2013 Andreas Rother <andreas@rother.org>
- updated source from https://github.com/Aries85/LightZone/
- minor changes in lightzone startscript
* Sun Feb 10 2013 Andreas Rother <andreas@rother.org>
- changed license to GPLv2+ due to dcraw.c
- added changes from Pavel Benak:
+ added -H option to cp to follow symbolic links. This allows simpler 
  copying from product directory. There were four JARs missing.
+ script now runs LightZone from /usr/share/lightzone directory, because 
  the native launcher invokes command "./LightZone-forkd". This may be 
  working if moved to bin, might be worth trying.
+ added LD_LIBRARY_PATH to fix problems with native libraries loading
+ removed ant dependency, in OpenSUSE it seems to have weird build results

* Sat Feb 09 2013 Andreas Rother <andreas@rother.org>
- initial version with Lightzone 3.9.1 and OpenJDK 1.7.0
