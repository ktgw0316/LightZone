# Copyright 1999-2016 Gentoo Foundation
# Distributed under the terms of the GNU General Public License v2
# $Header: $

EAPI=5
inherit eutils

if [[ ${PV} == "9999" ]]; then
	EGIT_REPO_URI=${EGIT_REPO_URI:-"git://github.com/ktgw0316/LightZone.git"}
	inherit git-r3
else
	SRC_URI="https://github.com/ktgw0316/LightZone/archive/${PV}.tar.gz"
fi

DESCRIPTION="Open-source professional-level digital darkroom software"
HOMEPAGE="https://github.com/ktgw0316/LightZone/"

LICENSE="BSD"
SLOT="0"
KEYWORDS="~x86 ~amd64"

DEPEND="virtual/jdk
	dev-util/pkgconfig
	dev-vcs/git
	media-libs/lensfun
	media-libs/lcms
	media-libs/libjpeg-turbo
	media-libs/libraw
	media-libs/tiff
	x11-libs/libX11"

RDEPEND="virtual/jre
	dev-libs/libxml2
	media-libs/lensfun
	media-libs/lcms
	media-libs/libjpeg-turbo
	media-libs/libraw
	media-libs/tiff"

pkg_setup() {
    if [[ ${PV} != "9999" ]]; then
		S="${WORKDIR}/LightZone-${PV}"
	fi

#	export JAVA_HOME=$(java-config --jdk-home)
}

src_compile() {
	"${S}"/gradlew jpackageImage -x test
}

src_install() {
	cd "${S}"

	_libdir=/usr/$(get_libdir)
    install -Dt "${D}/${_libdir}/${PN}" lightcrafts/build/resources/main/native/*.so

	_javadir=/usr/share
	install -Dt "${D}/${_javadir}/${PN}/lib" "linux/build/jpackage/${PN}/lib/app/*.jar" -m644

	_datadir=/usr/share
	install -Dt "${D}/${_datadir}/applications" "linux/products/${PN}.desktop" -m644
	cp -a linux/icons "${D}/${_datadir}/"

	_bindir=/usr/bin
	install -Dt "${D}/${_bindir}" lightcrafts/build/resources/main/native/dcraw_lz
	install -t  "${D}/${_bindir}" "linux/products/${PN}"
}
