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
	dev-java/ant-core
	dev-util/pkgconfig
	dev-vcs/git
	media-libs/lensfun
	media-libs/lcms
	media-libs/libjpeg-turbo
	media-libs/libraw
	media-libs/tiff
	net-misc/rsync
	x11-libs/libX11"

RDEPEND="virtual/jre
	dev-java/javahelp
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
#	export ANT_HOME=/usr/share/ant
}

src_compile() {
	ant -f "${S}"/linux/build.xml jar
}

src_install() {
	cd "${S}"

	_libdir=/usr/$(get_libdir)
	install -dm 0755 "${D}/${_libdir}/${PN}"
	cp -pH linux/products/*.so "${D}/${_libdir}/${PN}"

	_javadir=/usr/share
	install -dm 0755 "${D}/${_javadir}/${PN}/lib"
	cp -pH linux/products/*.jar "${D}/${_javadir}/${PN}/lib"

	_datadir=/usr/share
	install -dm 0755 "${D}/${_datadir}/applications"
	install -m 644 linux/products/lightzone.desktop "${D}/${_datadir}/applications/"
	install -dm 0755 "${D}/${_datadir}/metainfo"
	install -m 644 linux/products/io.github.ktgw0316.LightZone.metainfo.xml "${D}/${_datadir}/metainfo/"
	cp -pHR linux/icons "${D}/${_datadir}/"

	_bindir=/usr/bin
	install -dm 0755 "${D}/${_bindir}"
	install -m 755 lightcrafts/products/dcraw_lz "${D}/${_bindir}"
	install -m 755 "linux/products/${PN}" "${D}/${_bindir}"
}
