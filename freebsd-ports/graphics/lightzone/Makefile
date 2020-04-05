# Created by: ktgw0316 (KITAGAWA Masahiro)
# $FreeBSD$

PORTNAME=	lightzone
DISTVERSION=	4.2.1
PORTREVISION=	0
CATEGORIES=	graphics java
DIST_SUBDIR=	${PORTNAME}

MAINTAINER=	arctica0316@gmail.com
COMMENT=	Open-source professional-level digital darkroom software

LICENSE=	BSD3CLAUSE

BUILD_DEPENDS=	ant:${PORTSDIR}/devel/apache-ant \
		autoconf:${PORTSDIR}/devel/autoconf \
		git:${PORTSDIR}/devel/git \
		jhindexer:${PORTSDIR}/java/javahelp \
		rsync:${PORTSDIR}/net/rsync
LIB_DEPENDS=	liblcms2.so:${PORTSDIR}/graphics/lcms2 \
		liblensfun.so:${PORTSDIR}/graphics/lensfun \
		libtiff.so:${PORTSDIR}/graphics/tiff

USE_GITHUB=	yes
GH_ACCOUNT=	ktgw0316
GH_PROJECT=	LightZone

USES=		desktop-file-utils gmake jpeg pkgconfig
USE_ANT=	yes
USE_JAVA=	yes
USE_GCC=	yes
USE_LDCONFIG=	yes
USE_XORG=	x11
JAVA_VENDOR=	openjdk
JAVA_VERSION=	11+
JAVA_BUILD=	yes
JAVA_RUN=	yes
INSTALLS_ICONS=	yes

LIBDIR=		${PREFIX}/lib/${PORTNAME}
PLIST_SUB=	LIBDIR="${LIBDIR}"
WRKSRC_SUBDIR=	linux

post-patch:
		@${REINPLACE_CMD} -e 's|gcc|${CC}|' -e 's|g++|${CXX}|' ${WRKSRC}/../lightcrafts/mk/platform.mk
		@${REINPLACE_CMD} -e 's|%%LOCALBASE%%|${LOCALBASE}|' ${WRKSRC}/products/lightzone

do-install:
	${INSTALL_SCRIPT} ${WRKSRC}/products/${PORTNAME} ${STAGEDIR}${PREFIX}/bin/
	@${MKDIR} ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}
	${INSTALL_DATA} ${WRKSRC}/products/*.jar ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}
	@${MKDIR} ${STAGEDIR}${LIBDIR}
	${INSTALL_PROGRAM} ${WRKSRC}/products/LightZone-forkd ${STAGEDIR}${LIBDIR}
	${INSTALL_PROGRAM} ${WRKSRC}/products/dcraw_lz ${STAGEDIR}${LIBDIR}
	${INSTALL_LIB} ${WRKSRC}/products/*.so ${STAGEDIR}${LIBDIR}
	${INSTALL_DATA} ${WRKSRC}/products/${PORTNAME}.desktop ${STAGEDIR}${PREFIX}/share/applications/
	@cd ${WRKSRC} && ${COPYTREE_SHARE} icons ${STAGEDIR}${PREFIX}/share/

.include <bsd.port.mk>
