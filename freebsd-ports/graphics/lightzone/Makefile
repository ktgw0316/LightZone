# Created by: ktgw0316 (KITAGAWA Masahiro)
# $FreeBSD$

PORTNAME=	lightzone
DISTVERSION=	4.1.0beta12
CATEGORIES=	graphics java
MASTER_SITES=	https://github.com/${GIT_ACCOUNT}/${GIT_PROJECT}/tarball/${DISTVERSION}/
DISTNAME=	${GIT_PROJECT}-${DISTVERSION}-0-g${GIT_COMMIT}
DIST_SUBDIR=	${PORTNAME}

MAINTAINER=	arctica0316@gmail.com
COMMENT=	Open-source professional-level digital darkroom software

LICENSE=	BSD3CLAUSE

LIB_DEPENDS=	libX11.so:${PORTSDIR}/x11/libX11 \
		libjpeg.so:${PORTSDIR}/graphics/jpeg \
		liblcms2.so:${PORTSDIR}/graphics/lcms2 \
		libtiff.so:${PORTSDIR}/graphics/tiff
BUILD_DEPENDS=	ant:${PORTSDIR}/devel/apache-ant \
		autoconf:${PORTSDIR}/devel/autoconf \
		git:${PORTSDIR}/devel/git \
		javahelp:${PORTSDIR}/java/javahelp \
		pkgconf:${PORTSDIR}/devel/pkgconf \
		tidy:${PORTSDIR}/www/tidy
RUN_DEPENDS=	java:${PORTSDIR}/java/javavmwrapper \
		javahelp:${PORTSDIR}/java/javahelp

GIT_ACCOUNT=	Aries85
GIT_PROJECT=	LightZone
GIT_COMMIT=	53d2e9f

USES=		desktop-file-utils
USE_GCC=	yes
USE_ANT=	yes
USE_JAVA=	yes
JAVA_VENDOR=	openjdk
JAVA_VERSION=	1.6+
JAVA_BUILD=	yes
JAVA_RUN=	yes

DATADIR=	${PREFIX}/libexec/${PORTNAME}
# DOCSDIR=	${PREFIX}/share/${PORTNAME}/doc
WRKSRC=	${WRKDIR}/${GIT_ACCOUNT}-${GIT_PROJECT}-${GIT_COMMIT}/linux

do-install:
	${INSTALL_SCRIPT} ${WRKSRC}/products/${PORTNAME} ${STAGEDIR}${PREFIX}/bin/
	@${MKDIR} ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}
	${INSTALL_DATA} ${WRKSRC}/products/*.jar ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}
	@${MKDIR} ${STAGEDIR}${DATADIR}
	${INSTALL_PROGRAM} ${WRKSRC}/products/LightZone-forkd ${STAGEDIR}${DATADIR}
	${INSTALL_PROGRAM} ${WRKSRC}/products/dcraw_lz ${STAGEDIR}${DATADIR}
	${INSTALL_LIB} ${WRKSRC}/products/*.so ${STAGEDIR}${DATADIR}
	${INSTALL_DATA} ${WRKSRC}/products/${PORTNAME}.desktop ${STAGEDIR}${PREFIX}/share/applications/
	@cd ${WRKSRC} && ${COPYTREE_SHARE} icons ${STAGEDIR}${PREFIX}/share/

post-install:
	${LN} -sf ${DATADIR}/libLCJNI.so ${STAGEDIR}${LIBDIR}

.include <bsd.port.mk>
