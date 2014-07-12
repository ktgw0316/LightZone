#
# spec file for package lightzone
#

Name:           lightzone
# Do not use hyphens in Version tag. OBS doesn't handle it properly.
# Use 4.1.0.beta2 for betas and 4.1.0.0 for final, since RPM sorts A-Z before 0-9.
Version:	4.1.0.beta9
Release:	0
License:	BSD-3-Clause
Summary:	Open-source professional-level digital darkroom software
Url:		http://lightzoneproject.org/
Group:		Productivity/Graphics/Convertors 
Source:		%{name}-%{version}.tar.bz2

%if 0%{?fedora}
%define java_version 1.7.0
%define libX11_devel libX11-devel
%define debug_package %{nil}
%endif

%if 0%{?sles_version}
%define java_version 1_7_0
%define libX11_devel xorg-x11-libX11-devel
BuildRequires: update-desktop-files
%endif

%if 0%{?suse_version} == 1210
%define java_version 1_6_0
%define libX11_devel xorg-x11-libX11-devel
%endif

%if 0%{?suse_version} > 1210
%define java_version 1_7_0
%define libX11_devel libX11-devel
%endif

%if 0%{?centos_version}
%define java_version 1.6.0
%define libX11_devel libX11-devel
%define debug_package %{nil}
%endif

%if 0%{?mdkversion}
%define java_version 1.6.0
%define libX11_devel libX11-devel
%endif

BuildRequires:	java-%{java_version}-openjdk-devel, %{libX11_devel}, ant, autoconf, gcc, gcc-c++, make, tidy, git, javahelp2, liblcms2-devel, libjpeg8-devel, libtiff-devel, pkg-config
Requires:	java-%{java_version}-openjdk, javahelp2, liblcms2, libjpeg8, libtiff

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
cp -pH lightcrafts/products/dcraw_lz "%buildroot/%{instdir}"
cp -pH lightcrafts/products/LightZone-forkd "%buildroot/%{instdir}"
cp -pH linux/products/*.so "%buildroot/%{instdir}"
cp -pH linux/products/*.jar "%buildroot/%{instdir}"

# create icons and shortcuts
install -dm 0755 "%buildroot/%{_datadir}/applications"
install -m 644 linux/products/lightzone.desktop "%buildroot/%{_datadir}/applications/"
cp -pHR linux/icons "%buildroot/%{_datadir}/"

install -dm 755 %{buildroot}/%{_bindir}
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
%define icondir %{_datadir}/icons/hicolor
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
%{icondir}/256x256/apps/lightzone.png
%{icondir}/128x128/apps/lightzone.png
%{icondir}/64x64/apps/lightzone.png
%{icondir}/48x48/apps/lightzone.png
%{icondir}/32x32/apps/lightzone.png
%{icondir}/16x16/apps/lightzone.png

%changelog
