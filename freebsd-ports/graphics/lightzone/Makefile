# Created by: ktgw0316 (KITAGAWA Masahiro)
# $FreeBSD$

PORTNAME=	lightzone
DISTVERSION=	4.1.0beta7
CATEGORIES=	graphics java
MASTER_SITES=	https://github.com/${GIT_ACCOUNT}/${GIT_PROJECT}/tarball/${DISTVERSION}/
DISTNAME=	${GIT_PROJECT}-${DISTVERSION}-0-g${GIT_COMMIT}
DIST_SUBDIR=	${PORTNAME}

MAINTAINER=	arctica0316@gmail.com
COMMENT=	Open-source professional-level digital darkroom software

LICENSE=	BSD3CLAUSE

LIB_DEPENDS=	libX11.so:${PORTSDIR}/x11/libX11 \
		libjpeg.so:${PORTSDIR}/graphics/jpeg \
		libjpeg.so:${PORTSDIR}/graphics/lcms2 \
		libtiff.so:${PORTSDIR}/graphics/tiff
BUILD_DEPENDS=	ant:${PORTSDIR}/devel/apache-ant \
		git:${PORTSDIR}/devel/git \
		javahelp:${PORTSDIR}/java/javahelp \
		tidy:${PORTSDIR}/www/tidy
RUN_DEPENDS=	java:${PORTSDIR}/java/javavmwrapper

GIT_ACCOUNT=	ktgw0316
GIT_PROJECT=	LightZone
GIT_COMMIT=	3547851

USES=		desktop-file-utils
USE_GCC=	4.2+
USE_ANT=	yes
USE_JAVA=	yes
JAVA_VENDOR=	openjdk
JAVA_VERSION=	1.6+
JAVA_BUILD=	yes
JAVA_RUN=	yes

DATADIR=	${JAVASHAREDIR}/${PORTNAME}
# DOCSDIR=	${PREFIX}/share/${PORTNAME}/doc
WRKSRC=	${WRKDIR}/${GIT_ACCOUNT}-${GIT_PROJECT}-${GIT_COMMIT}/linux

do-install:
	${INSTALL_SCRIPT} ${WRKSRC}/products/${PORTNAME} ${PREFIX}/bin/
	@${MKDIR} ${DATADIR}
	${INSTALL_PROGRAM} ${WRKSRC}/products/LightZone-forkd ${DATADIR}
	${INSTALL_PROGRAM} ${WRKSRC}/products/dcraw ${DATADIR}
	${INSTALL_DATA} ${WRKSRC}/products/*.jar ${DATADIR}
	${INSTALL_LIB} ${WRKSRC}/products/*.so ${DATADIR}
	${INSTALL_DATA} ${WRKSRC}/products/${PORTNAME}.desktop ${PREFIX}/share/applications/
	@cd ${WRKSRC} && ${COPYTREE_SHARE} icons ${PREFIX}/share/

post-install:
	${LN} -sf ${DATADIR}/libLCJNI.so ${LIBDIR}

.include <bsd.port.mk>
