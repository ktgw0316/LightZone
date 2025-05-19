#
# spec file for package lightzone
#

Name:           lightzone
# Do not use hyphens in Version tag. OBS doesn't handle it properly.
# Use 4.1.0.beta2 for betas and 4.1.0.0 for final, since RPM sorts A-Z before 0-9.
Version:	5.0.0.beta3
Release:	0%{?dist}
License:	BSD-3-Clause
Summary:	Open-source professional-level digital darkroom software
Url:		https://github.com/ktgw0316/LightZone/
Group:		Productivity/Graphics/Convertors
Source:		%{name}-%{version}.tar.xz

%if 0%{?fedora} || 0%{?rhel_version} || 0%{?centos_version}
%define jdk java-21-openjdk-devel
%define lcms2_devel lcms2-devel
%define libjpeg_devel libjpeg-turbo-devel
%define libraw LibRaw
%define libX11_devel libX11-devel
%define pkg_config pkgconfig
%define xmllint libxml2
%define debug_package %{nil}
BuildRequires: ant-openjdk21
%endif

%if 0%{?sles_version}
%define jdk java-21-openjdk-devel
%define lcms2_devel liblcms2-devel
%define libjpeg_devel libjpeg8-devel
%define libraw libraw
%define libX11_devel xorg-x11-libX11-devel
%define pkg_config pkg-config
%define xmllint libxml2
BuildRequires: update-desktop-files
%endif

%if 0%{?suse_version}
%define jdk java-21-openjdk-devel
%define lcms2_devel liblcms2-devel
%define libjpeg_devel libjpeg8-devel
%define libraw libraw
%define libX11_devel libX11-devel
%define pkg_config pkg-config
%define xmllint libxml2-tools
%if 0%{?suse_version} >= 1320
Requires:	xerces-j2-xml-apis
%endif
%endif

%if 0%{?mdkversion} || 0%{?pclinuxos}
%define jdk java-21-openjdk-devel
%define lcms2_devel liblcms2-devel
%define libjpeg_devel libjpeg8-devel
%define libraw libraw
%define libX11_devel libX11-devel
%define pkg_config pkg-config
%define xmllint libxml2-utils
BuildRequires:	libgomp-devel
Requires:	libgomp1
%endif

%if 0%{?mageia}
%define jdk java-latest-openjdk-devel
%define lcms2_devel liblcms2-devel
%define libjpeg_devel libjpeg-devel
%define libraw libraw
%define libX11_devel libx11-devel
%define pkg_config pkg-config
%define xmllint libxml2-utils
BuildRequires:	libgomp-devel
Requires:	libgomp1
%endif

BuildRequires:	javapackages-tools, %{jdk}, %{libX11_devel}, ant, gcc, gcc-c++, make, git, %{lcms2_devel}, lensfun-devel, %{libjpeg_devel}, %{libraw}-devel, libtiff-devel, %{pkg_config}, rsync
Requires:	lcms2, lensfun, %{libraw}, %{xmllint}

BuildRoot:      %{_tmppath}/%{name}-%{version}-build

%description
LightZone is open-source professional-level digital darkroom software for Windows, Mac OS X, and Linux. Rather than using layers as many other photo editors do, LightZone lets the user build up a stack of tools which can be rearranged, turned off and on, and removed from the stack. It's a non-destructive editor, where any of the tools can be re-adjusted or modified later — even in a different editing session. A tool stack can be copied to a batch of photos at one time. LightZone operates in a 16-bit linear color space with the wide gamut of ProPhoto RGB.

%prep
%setup -q

%build
%ant -f linux/build.xml -Dno-ivy=true -Dno-submodule=false jar

%install
%if 0%{?sles_version}
export NO_BRP_CHECK_BYTECODE_VERSION=true
%endif

install -dm 0755 "%buildroot/%{_libexecdir}/%{name}"
cp -pH lightcrafts/products/dcraw_lz "%buildroot/%{_libexecdir}/%{name}"
cp -pH linux/products/*.so "%buildroot/%{_libexecdir}/%{name}"

install -dm 0755 "%buildroot/%{_javadir}/%{name}"
cp -pH linux/products/*.jar "%buildroot/%{_javadir}/%{name}"

# create icons and shortcuts
install -dm 0755 "%buildroot/%{_datadir}/applications"
install -m 644 linux/products/lightzone.desktop "%buildroot/%{_datadir}/applications/"
install -dm 0755 "%buildroot/%{_datadir}/metainfo"
install -m 644 linux/products/io.github.ktgw0316.LightZone.metainfo.xml "%buildroot/%{_datadir}/metainfo/"
cp -pHR linux/icons "%buildroot/%{_datadir}/"

install -dm 755 %{buildroot}/%{_bindir}
install -m 755 linux/products/%{name} %{buildroot}/%{_bindir}

%if 0%{?sles_version}
%suse_update_desktop_file -n %{name}
%endif

%files
%defattr(-,root,root)
%doc COPYING README.md linux/BUILD-Linux.md
%dir %{_libexecdir}/%{name}
%{_libexecdir}/%{name}/*
%dir %{_javadir}/%{name}
%{_javadir}/%{name}/*
%{_bindir}/%{name}
%{_datadir}/applications/%{name}.desktop
%{_datadir}/metainfo/*"
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
%{icondir}/256x256/apps/%{name}.png
%{icondir}/128x128/apps/%{name}.png
%{icondir}/64x64/apps/%{name}.png
%{icondir}/48x48/apps/%{name}.png
%{icondir}/32x32/apps/%{name}.png
%{icondir}/16x16/apps/%{name}.png

%changelog
