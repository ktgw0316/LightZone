#
# spec file for package lightzone
#

Name:           lightzone
Version:	4.0.0
Release:	1
License:	BSD-3-Clause
Summary:	Open-source professional-level digital darkroom software
Url:		http://lightzoneproject.org/
Group:		Productivity/Graphics/Convertors 
Source:		%{name}-%{version}.tar.bz2
BuildRequires:	ant, autoconf, automake, nasm, gcc, gcc-c++, libtool, make, tidy, git, javahelp2

%if 0%{?fedora}
BuildRequires: java-1.7.0-openjdk-devel, libX11-devel, xz-libs
%define debug_package %{nil}
%endif
%if 0%{?sles_version}
BuildRequires: java-1_6_0-openjdk-devel, xorg-x11-libX11-devel, liblzma5, update-desktop-files
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

%if 0%{?fedora}
Requires:	java >= 1.6.0, xz-libs
%else
Requires:	java >= 1.6.0, liblzma5
%endif
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
%description
LightZone is open-source professional-level digital darkroom software for Windows, Mac OS X, and Linux. Rather than using layers as many other photo editors do, LightZone lets the user build up a stack of tools which can be rearranged, turned off and on, and removed from the stack. It's a non-destructive editor, where any of the tools can be re-adjusted or modified later — even in a different editing session. A tool stack can be copied to a batch of photos at one time. LightZone operates in a 16-bit linear color space with the wide gamut of ProPhoto RGB.

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
cp -rpH linux/products/lightzone "%buildroot/%{instdir}"

# create icons and shortcuts
%define icondir %{_datadir}/icons/hicolor
install -dm 0755 "%buildroot/%{_datadir}/applications"
install -dm 0755 "%buildroot/%{icondir}/256x256/apps"
install -dm 0755 "%buildroot/%{icondir}/128x128/apps"
install -dm 0755 "%buildroot/%{icondir}/64x64/apps"
install -dm 0755 "%buildroot/%{icondir}/48x48/apps"
install -dm 0755 "%buildroot/%{icondir}/32x32/apps"
install -dm 0755 "%buildroot/%{icondir}/16x16/apps"

cp -rpH linux/products/lightzone.desktop "%buildroot/%{_datadir}/applications/"
cp -rpH linux/icons/LightZone_256x256.png "%buildroot/%{icondir}/256x256/apps/LightZone.png"
cp -rpH linux/icons/LightZone_128x128.png "%buildroot/%{icondir}/128x128/apps/LightZone.png"
cp -rpH linux/icons/LightZone_64x64.png "%buildroot/%{icondir}/64x64/apps/LightZone.png"
cp -rpH linux/icons/LightZone_48x48.png "%buildroot/%{icondir}/48x48/apps/LightZone.png"
cp -rpH linux/icons/LightZone_32x32.png "%buildroot/%{icondir}/32x32/apps/LightZone.png"
cp -rpH linux/icons/LightZone_16x16.png "%buildroot/%{icondir}/16x16/apps/LightZone.png"

install -d -m 755 %{buildroot}/%{_bindir}
install -m 755 linux/products/%{name} %{buildroot}/%{_bindir}

%if 0%{?sles_version}
%suse_update_desktop_file -n %{name}
%endif

%files
%defattr(-,root,root)
%doc COPYING README.md linux/BUILD-Linux.md
%dir %{instdir}
%{instdir}/*
%{_bindir}/%{name}
%{_datadir}/applications/lightzone.desktop
%dir %{icondir}
%dir %{icondir}/256x256
%dir %{icondir}/256x256/apps
%dir %{icondir}/128x128
%dir %{icondir}/128x128/apps
%dir %{icondir}/64x64
%dir %{icondir}/64x64/apps
%dir %{icondir}/48x48
%dir %{icondir}/48x48/apps
%dir %{icondir}/32x32
%dir %{icondir}/32x32/apps
%dir %{icondir}/16x16
%dir %{icondir}/16x16/apps
%{icondir}/256x256/apps/LightZone.png
%{icondir}/128x128/apps/LightZone.png
%{icondir}/64x64/apps/LightZone.png
%{icondir}/48x48/apps/LightZone.png
%{icondir}/32x32/apps/LightZone.png
%{icondir}/16x16/apps/LightZone.png

%changelog
