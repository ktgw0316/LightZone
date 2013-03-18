#
# spec file for package lightzone
#

Name:           lightzone
Version:	3.9.1
Release:	7
License:	GPLv2+
Summary:	Open-source digital darkroom software
Url:		http://lightzoneproject.org/
Group:		Productivity/Graphics/Convertors 
Source:		LightZone.tar.gz
#Patch:
BuildRequires:	java-1_7_0-openjdk-devel
Requires:	java >= 1.7.0
Provides:	lightzone
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Packager:	Andreas Rother
#Prefix:	/opt
#BuildArch:	noarch
%description
LightZone is professional-level digital darkroom software for Windows, Mac OS X, and Linux. Rather than using layers as many other photo editors do, LightZone lets the user build up a stack of tools which can be rearranged, turned off and on, and removed from the stack. It's a non-destructive editor, where any of the tools can be re-adjusted or modified later — even in a different editing session. A tool stack can be copied to a batch of photos at one time. LightZone operates in a 16-bit linear color space with the wide gamut of ProPhoto RGB.

%prep
%setup -qn LightZone

%build
%ant -f linux/build.xml jar

%install
install -dm 0755 "%buildroot/%_datadir/%name"
cp -rpH lightcrafts/products/dcraw "%buildroot/%_datadir/%name"
cp -rpH lightcrafts/products/LightZone-forkd "%buildroot/%_datadir/%name"
cp -rpH linux/products/*.so "%buildroot/%_datadir/%name"
cp -rpH linux/products/*.jar "%buildroot/%_datadir/%name"

#startscript
cat > %{name} << 'EOF'
#!/bin/sh
#
# LightZone startscript
#
echo Starting %{name} version %{version} ...
echo with options : ${@}

(cd "%_datadir/%name" && LD_LIBRARY_PATH="%_datadir/%name" exec java -Xmx256m -Djava.library.path="%_datadir/%name" -Dcom.lightcrafts.licensetype=ESD -Dlightcrafts.debug=1 -classpath "%_datadir/%name/*" com.lightcrafts.platform.linux.LinuxLauncher ${@} )
EOF
install -d -m 755 %{buildroot}%{_bindir}
install -m 755 %{name} %{buildroot}%{_bindir}/

%post

%postun

%files
%defattr(-,root,root)
%doc COPYING README.md linux/BUILD-Linux.md
%_datadir/%name
%_bindir/%name

%changelog
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
