#
# $RCSfile: registryFile.jai,v $
#
# Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
#
# Use is subject to license terms.
#
# $Revision: 1.2 $
# $Date: 2005/08/31 22:35:25 $
# $State: Exp $
#
# The master JAI registry initialization file
#
############################################################################
#
# Each line can be in one of the formats described below. Space or tab
# characters separate keywords in each line. The comment character is
# '#'; on each line all characters following the first comment character
# are ignored. The file must be encoded in UTF-8.
#
# 1. To register descriptors :
#
#	descriptor <descriptor-class-name>
#	odesc	   <descriptor-class-name>  <descriptor-name>
#
#    The second version above is deprecated and is retained for backward
#    compatibility with JAI 1.0.2. Descriptors are always registered
#    against <descriptor-class>.getName(). The <descriptor-name> in the
#    second version is always ignored.
#
# 2. To register factory objects under a product against a specific mode :
#
#	<registry-mode-name>	<factory-class-name>	<product-name>	<descriptor-name>   <local-name>
#	<registry-mode-name>	<factory-class-name>	<descriptor-name>
#
#    The first version above is used to register factory objects against
#    modes that support preferences. The second version is used for those
#    that do not support preferences. <local-name> is an arbitrary name that
#    is unique for a given mode. This is (only) used later on in this file
#    to set preferences between factory objects.
#
# 3. To set preferences between products for a descriptor under a
#    specific mode :
#
#	prefProduct <modeName>	<descriptor-name>   <preferred-product-name> <other-product-name>
#	pref	    product	<descriptor-name>   <preferred-product-name> <other-product-name>
#
#    The second version above is deprecated and is retained for backward
#    compatibility with JAI 1.0.2. This version is assumed to set
#    product preferences for the "rendered" mode.
#
# 4. To set preferences between factory objects for descriptor under a
#    a specific product and registry mode :
#
#	pref	<modeName>  <descriptor-name>	<product-name>	<preferred-factory-local-name>	<other-factory-local-name>
#
############################################################################
#
# Image operation descriptors :
#
descriptor	javax.media.jai.operator.AbsoluteDescriptor
descriptor	javax.media.jai.operator.AddCollectionDescriptor
descriptor	javax.media.jai.operator.AddConstDescriptor
descriptor	javax.media.jai.operator.AddConstToCollectionDescriptor
descriptor	javax.media.jai.operator.AddDescriptor
descriptor	javax.media.jai.operator.AffineDescriptor
descriptor	javax.media.jai.operator.AndConstDescriptor
descriptor	javax.media.jai.operator.AndDescriptor
descriptor	javax.media.jai.operator.AWTImageDescriptor
descriptor	javax.media.jai.operator.BandCombineDescriptor
descriptor	javax.media.jai.operator.BandMergeDescriptor
descriptor	javax.media.jai.operator.BandSelectDescriptor
descriptor	javax.media.jai.operator.BinarizeDescriptor
descriptor	javax.media.jai.operator.BMPDescriptor
descriptor	javax.media.jai.operator.BorderDescriptor
descriptor	javax.media.jai.operator.BoxFilterDescriptor
descriptor	javax.media.jai.operator.ClampDescriptor
descriptor	javax.media.jai.operator.ColorConvertDescriptor
descriptor	javax.media.jai.operator.ColorQuantizerDescriptor
descriptor	javax.media.jai.operator.ConstantDescriptor
descriptor	javax.media.jai.operator.CompositeDescriptor
descriptor	javax.media.jai.operator.ConjugateDescriptor
descriptor	javax.media.jai.operator.ConvolveDescriptor
descriptor	javax.media.jai.operator.CropDescriptor
descriptor	javax.media.jai.operator.DCTDescriptor
descriptor	javax.media.jai.operator.DFTDescriptor
descriptor	javax.media.jai.operator.DilateDescriptor
descriptor	javax.media.jai.operator.DivideDescriptor
descriptor	javax.media.jai.operator.DivideComplexDescriptor
descriptor	javax.media.jai.operator.DivideByConstDescriptor
descriptor	javax.media.jai.operator.DivideIntoConstDescriptor
descriptor	javax.media.jai.operator.ErodeDescriptor
descriptor	javax.media.jai.operator.ErrorDiffusionDescriptor
descriptor	javax.media.jai.operator.EncodeDescriptor
descriptor	javax.media.jai.operator.ExpDescriptor
descriptor	javax.media.jai.operator.ExtremaDescriptor
descriptor	javax.media.jai.operator.FileLoadDescriptor
descriptor	javax.media.jai.operator.FileStoreDescriptor
descriptor	javax.media.jai.operator.FilteredSubsampleDescriptor
descriptor	javax.media.jai.operator.FormatDescriptor
descriptor	javax.media.jai.operator.FPXDescriptor
descriptor	javax.media.jai.operator.GIFDescriptor
descriptor	javax.media.jai.operator.GradientMagnitudeDescriptor
descriptor	javax.media.jai.operator.HistogramDescriptor
descriptor	javax.media.jai.operator.IDCTDescriptor
descriptor	javax.media.jai.operator.IDFTDescriptor
descriptor	javax.media.jai.operator.IIPDescriptor
descriptor	javax.media.jai.operator.IIPResolutionDescriptor
descriptor	javax.media.jai.operator.ImageFunctionDescriptor
descriptor	javax.media.jai.operator.InvertDescriptor
descriptor	javax.media.jai.operator.JPEGDescriptor
descriptor	javax.media.jai.operator.LogDescriptor
descriptor	javax.media.jai.operator.LookupDescriptor
descriptor	javax.media.jai.operator.MagnitudeDescriptor
descriptor	javax.media.jai.operator.MagnitudeSquaredDescriptor
descriptor	javax.media.jai.operator.MaxDescriptor
descriptor	javax.media.jai.operator.MaxFilterDescriptor
descriptor	javax.media.jai.operator.MatchCDFDescriptor
descriptor	javax.media.jai.operator.MeanDescriptor
descriptor	javax.media.jai.operator.MedianFilterDescriptor
descriptor	javax.media.jai.operator.MinDescriptor
descriptor	javax.media.jai.operator.MinFilterDescriptor
descriptor	javax.media.jai.operator.MosaicDescriptor
descriptor	javax.media.jai.operator.MultiplyConstDescriptor
descriptor	javax.media.jai.operator.MultiplyComplexDescriptor
descriptor	javax.media.jai.operator.MultiplyDescriptor
descriptor	javax.media.jai.operator.NotDescriptor
descriptor	javax.media.jai.operator.NullDescriptor
descriptor	javax.media.jai.operator.OrConstDescriptor
descriptor	javax.media.jai.operator.OrDescriptor
descriptor	javax.media.jai.operator.OrderedDitherDescriptor
descriptor	javax.media.jai.operator.OverlayDescriptor
descriptor	javax.media.jai.operator.PatternDescriptor
descriptor	javax.media.jai.operator.PeriodicShiftDescriptor
descriptor	javax.media.jai.operator.PhaseDescriptor
descriptor	javax.media.jai.operator.PiecewiseDescriptor
descriptor	javax.media.jai.operator.PNGDescriptor
descriptor	javax.media.jai.operator.PNMDescriptor
descriptor	javax.media.jai.operator.PolarToComplexDescriptor
descriptor	javax.media.jai.operator.RenderableDescriptor
descriptor	javax.media.jai.operator.RescaleDescriptor
descriptor	javax.media.jai.operator.RotateDescriptor
descriptor	javax.media.jai.operator.ScaleDescriptor
descriptor	javax.media.jai.operator.ShearDescriptor
descriptor	javax.media.jai.operator.StreamDescriptor
descriptor	javax.media.jai.operator.SubsampleAverageDescriptor
descriptor	javax.media.jai.operator.SubsampleBinaryToGrayDescriptor
descriptor	javax.media.jai.operator.SubtractDescriptor
descriptor	javax.media.jai.operator.SubtractConstDescriptor
descriptor	javax.media.jai.operator.SubtractFromConstDescriptor
descriptor	javax.media.jai.operator.TIFFDescriptor
descriptor	javax.media.jai.operator.ThresholdDescriptor
descriptor	javax.media.jai.operator.TranslateDescriptor
descriptor	javax.media.jai.operator.TransposeDescriptor
descriptor	javax.media.jai.operator.UnsharpMaskDescriptor
descriptor	javax.media.jai.operator.URLDescriptor
descriptor	javax.media.jai.operator.WarpDescriptor
descriptor	javax.media.jai.operator.XorConstDescriptor
descriptor	javax.media.jai.operator.XorDescriptor

#
# tile codec descriptor
#
descriptor	javax.media.jai.tilecodec.GZIPTileCodecDescriptor
descriptor	javax.media.jai.tilecodec.JPEGTileCodecDescriptor
descriptor	javax.media.jai.tilecodec.RawTileCodecDescriptor

#
# remote descriptor
#
descriptor    javax.media.jai.remote.JAIRMIDescriptor

#
# "rendered" factory objects
#
rendered    com.lightcrafts.media.jai.opimage.AbsoluteCRIF		com.lightcrafts.media.jai	absolute		sunabsoluterif
rendered    com.lightcrafts.media.jai.mlib.MlibAbsoluteRIF		com.lightcrafts.media.jai	absolute		mlibabsoluterif
rendered    com.lightcrafts.media.jai.opimage.AddCollectionCRIF		com.lightcrafts.media.jai	addcollection		sunaddcollectionrif
rendered    com.lightcrafts.media.jai.opimage.AddConstCRIF		com.lightcrafts.media.jai	addconst		sunaddconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibAddConstRIF		com.lightcrafts.media.jai	addconst		mlibaddconstrif
rendered    com.lightcrafts.media.jai.opimage.AddCRIF			com.lightcrafts.media.jai	add			sunaddrif
rendered    com.lightcrafts.media.jai.mlib.MlibAddRIF			com.lightcrafts.media.jai	add			mlibaddrif
rendered    com.lightcrafts.media.jai.opimage.AffineCRIF		com.lightcrafts.media.jai	affine			sunaffinerif
rendered    com.lightcrafts.media.jai.mlib.MlibAffineRIF		com.lightcrafts.media.jai	affine			mlibaffinerif
rendered    com.lightcrafts.media.jai.opimage.AndConstCRIF		com.lightcrafts.media.jai	andconst		sunandconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibAndConstRIF		com.lightcrafts.media.jai	andconst		mlibandconstrif
rendered    com.lightcrafts.media.jai.opimage.AndCRIF			com.lightcrafts.media.jai	and			sunandrif
rendered    com.lightcrafts.media.jai.mlib.MlibAndRIF			com.lightcrafts.media.jai	and			mlibandrif
rendered    com.lightcrafts.media.jai.opimage.AWTImageRIF		com.lightcrafts.media.jai	awtimage		sunawtimagerif
rendered    com.lightcrafts.media.jai.opimage.BandCombineCRIF		com.lightcrafts.media.jai	bandcombine		sunbandcombinerif
rendered    com.lightcrafts.media.jai.opimage.BandMergeCRIF		com.lightcrafts.media.jai	bandmerge		sunbandmergerif
rendered    com.lightcrafts.media.jai.mlib.MlibBandCombineRIF		com.lightcrafts.media.jai	bandcombine		mlibbandcombinerif
rendered    com.lightcrafts.media.jai.opimage.BandSelectCRIF		com.lightcrafts.media.jai	bandselect		sunbandselectrif
rendered    com.lightcrafts.media.jai.mlib.MlibBandSelectRIF		com.lightcrafts.media.jai	bandselect		mlibbandselectrif
rendered    com.lightcrafts.media.jai.opimage.BinarizeCRIF		com.lightcrafts.media.jai	binarize		sunbinarizerif
rendered    com.lightcrafts.media.jai.mlib.MlibBinarizeRIF		com.lightcrafts.media.jai	binarize		mlibbinarizerif
rendered    com.lightcrafts.media.jai.opimage.BMPRIF			com.lightcrafts.media.jai	bmp			sunbmprif
rendered    com.lightcrafts.media.jai.opimage.BorderRIF			com.lightcrafts.media.jai	border			sunborderrif
rendered    com.lightcrafts.media.jai.opimage.BoxFilterRIF		com.lightcrafts.media.jai	boxfilter		sunboxfilterrif
rendered    com.lightcrafts.media.jai.mlib.MlibBoxFilterRIF		com.lightcrafts.media.jai	boxfilter		mlibboxfilterrif
rendered    com.lightcrafts.media.jai.opimage.ClampCRIF			com.lightcrafts.media.jai	clamp			sunclamprif
rendered    com.lightcrafts.media.jai.mlib.MlibClampRIF			com.lightcrafts.media.jai	clamp			mlibclamprif
rendered    com.lightcrafts.media.jai.opimage.ColorConvertCRIF		com.lightcrafts.media.jai	colorconvert		suncolorconvertrif
rendered    com.lightcrafts.media.jai.opimage.ColorQuantizerRIF		com.lightcrafts.media.jai	colorquantizer		suncolorquantizerrif
rendered    com.lightcrafts.media.jai.opimage.CompositeCRIF		com.lightcrafts.media.jai	composite		suncompositerif
rendered    com.lightcrafts.media.jai.mlib.MlibCompositeRIF		com.lightcrafts.media.jai	composite		mlibcompositerif
rendered    com.lightcrafts.media.jai.opimage.ConstantCRIF		com.lightcrafts.media.jai	constant		sunconstrif1
rendered    com.lightcrafts.media.jai.opimage.ConjugateCRIF		com.lightcrafts.media.jai	conjugate		sunconjugaterif
rendered    com.lightcrafts.media.jai.opimage.ConvolveRIF		com.lightcrafts.media.jai	convolve		sunconvolverif
rendered    com.lightcrafts.media.jai.mlib.MlibConvolveRIF		com.lightcrafts.media.jai	convolve		mlibconvolverif
rendered    com.lightcrafts.media.jai.opimage.CropCRIF			com.lightcrafts.media.jai	crop			suncroprif
rendered    com.lightcrafts.media.jai.opimage.DCTCRIF			com.lightcrafts.media.jai	dct			sundctrif
rendered    com.lightcrafts.media.jai.mlib.MlibDCTRIF			com.lightcrafts.media.jai	dct			mlibdctrif
rendered    com.lightcrafts.media.jai.opimage.DFTCRIF			com.lightcrafts.media.jai	dft			sundftrif
rendered    com.lightcrafts.media.jai.mlib.MlibDFTRIF			com.lightcrafts.media.jai	dft			mlibdftrif
rendered    com.lightcrafts.media.jai.opimage.DilateRIF			com.lightcrafts.media.jai	dilate			sundilaterif
rendered    com.lightcrafts.media.jai.mlib.MlibDilateRIF		com.lightcrafts.media.jai	dilate			mlibdilaterif
rendered    com.lightcrafts.media.jai.opimage.DivideCRIF		com.lightcrafts.media.jai	divide			sundividerif
rendered    com.lightcrafts.media.jai.opimage.DivideComplexCRIF		com.lightcrafts.media.jai	dividecomplex		sundividecomplexrif
rendered    com.lightcrafts.media.jai.mlib.MlibDivideRIF		com.lightcrafts.media.jai	divide			mlibdividerif
rendered    com.lightcrafts.media.jai.opimage.DivideByConstCRIF		com.lightcrafts.media.jai	dividebyconst		sundividebyconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibDivideByConstRIF		com.lightcrafts.media.jai	dividebyconst		mlibdividebyconstrif
rendered    com.lightcrafts.media.jai.opimage.DivideIntoConstCRIF	com.lightcrafts.media.jai	divideintoconst		sundivideintoconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibDivideIntoConstRIF	com.lightcrafts.media.jai	divideintoconst		mlibdivideintoconstrif
rendered    com.lightcrafts.media.jai.opimage.EncodeRIF			com.lightcrafts.media.jai	encode   		sunencoderif
rendered    com.lightcrafts.media.jai.opimage.ErodeRIF			com.lightcrafts.media.jai	erode			suneroderif
rendered    com.lightcrafts.media.jai.mlib.MlibErodeRIF			com.lightcrafts.media.jai	erode			mliberoderif
rendered    com.lightcrafts.media.jai.opimage.ErrorDiffusionRIF		com.lightcrafts.media.jai	errordiffusion		sunerrordiffusionrif
rendered    com.lightcrafts.media.jai.opimage.ExpCRIF		        com.lightcrafts.media.jai	exp			sunexprif
rendered    com.lightcrafts.media.jai.mlib.MlibExpRIF		        com.lightcrafts.media.jai	exp			mlibexprif
rendered    com.lightcrafts.media.jai.opimage.ExtremaRIF		com.lightcrafts.media.jai	extrema			sunextremarif
rendered    com.lightcrafts.media.jai.mlib.MlibExtremaRIF		com.lightcrafts.media.jai	extrema			mlibextremarif
rendered    com.lightcrafts.media.jai.opimage.FileLoadRIF		com.lightcrafts.media.jai	fileload		sunfileloadrif
rendered    com.lightcrafts.media.jai.opimage.FileStoreRIF		com.lightcrafts.media.jai	filestore		sunfilestorerif
rendered    com.lightcrafts.media.jai.opimage.FilteredSubsampleRIF      com.lightcrafts.media.jai       filteredsubsample       sunfilteredsubsamplerif
rendered    com.lightcrafts.media.jai.mlib.MlibFilteredSubsampleRIF     com.lightcrafts.media.jai       filteredsubsample       mlibfilteredsubsamplerif
rendered    com.lightcrafts.media.jai.opimage.FormatCRIF		com.lightcrafts.media.jai	format			sunformatrif
rendered    com.lightcrafts.media.jai.opimage.FPXRIF		        com.lightcrafts.media.jai	fpx			sunfpxrif
rendered    com.lightcrafts.media.jai.opimage.GIFRIF		        com.lightcrafts.media.jai	gif			sungifrif
rendered    com.lightcrafts.media.jai.mlib.MlibGradientRIF		com.lightcrafts.media.jai	gradientmagnitude	mlibgradientrif
rendered    com.lightcrafts.media.jai.opimage.GradientRIF		com.lightcrafts.media.jai	gradientmagnitude	sungradientrif
rendered    com.lightcrafts.media.jai.opimage.HistogramRIF		com.lightcrafts.media.jai	histogram		sunhistogramrif
rendered    com.lightcrafts.media.jai.mlib.MlibHistogramRIF		com.lightcrafts.media.jai	histogram		mlibhistogramrif
rendered    com.lightcrafts.media.jai.opimage.IDCTCRIF			com.lightcrafts.media.jai	idct			sunidctrif
rendered    com.lightcrafts.media.jai.mlib.MlibIDCTRIF			com.lightcrafts.media.jai	idct			mlibidctrif
rendered    com.lightcrafts.media.jai.opimage.IDFTCRIF			com.lightcrafts.media.jai	idft			sunidftrif
rendered    com.lightcrafts.media.jai.mlib.MlibIDFTRIF			com.lightcrafts.media.jai	idft			mlibidftrif
rendered    com.lightcrafts.media.jai.opimage.IIPCRIF			com.lightcrafts.media.jai	iip			suniipcrif
rendered    com.lightcrafts.media.jai.opimage.IIPResolutionRIF		com.lightcrafts.media.jai	iipresolution		suniipresolutionrif
rendered    com.lightcrafts.media.jai.opimage.ImageFunctionRIF		com.lightcrafts.media.jai	imagefunction		sunimagefunctionrif
rendered    com.lightcrafts.media.jai.opimage.InvertCRIF		com.lightcrafts.media.jai	invert			suninvertrif
rendered    com.lightcrafts.media.jai.mlib.MlibInvertRIF		com.lightcrafts.media.jai	invert			mlibinvertrif
rendered    com.lightcrafts.media.jai.opimage.JPEGRIF		        com.lightcrafts.media.jai	jpeg			sunjpegrif
rendered    com.lightcrafts.media.jai.opimage.LogCRIF		        com.lightcrafts.media.jai	log			sunlogrif
rendered    com.lightcrafts.media.jai.mlib.MlibLogRIF		        com.lightcrafts.media.jai	log			mliblogrif
rendered    com.lightcrafts.media.jai.opimage.LookupCRIF		com.lightcrafts.media.jai	lookup			sunlookuprif
rendered    com.lightcrafts.media.jai.mlib.MlibLookupRIF		com.lightcrafts.media.jai	lookup			mliblookuprif
rendered    com.lightcrafts.media.jai.opimage.MagnitudeCRIF		com.lightcrafts.media.jai	magnitude		sunmagnituderif
rendered    com.lightcrafts.media.jai.opimage.MagnitudeSquaredCRIF	com.lightcrafts.media.jai	magnitudesquared	sunmagnitudesquaredrif
rendered    com.lightcrafts.media.jai.opimage.MatchCDFCRIF		com.lightcrafts.media.jai	matchcdf		sunmatchcdfrif
rendered    com.lightcrafts.media.jai.opimage.MaxCRIF		        com.lightcrafts.media.jai	max			sunmaxrif
rendered    com.lightcrafts.media.jai.mlib.MlibMaxRIF		        com.lightcrafts.media.jai	max			mlibmaxrif
rendered    com.lightcrafts.media.jai.opimage.MaxFilterRIF		com.lightcrafts.media.jai	maxfilter		sunmaxfilterrif
rendered    com.lightcrafts.media.jai.mlib.MlibMaxFilterRIF		com.lightcrafts.media.jai	maxfilter		mlibmaxfilterrif
rendered    com.lightcrafts.media.jai.opimage.MeanRIF		        com.lightcrafts.media.jai	mean			sunmeanrif
rendered    com.lightcrafts.media.jai.mlib.MlibMeanRIF		        com.lightcrafts.media.jai	mean			mlibmeanrif
rendered    com.lightcrafts.media.jai.opimage.MedianFilterRIF		com.lightcrafts.media.jai	medianfilter		sunmedianfilterrif
rendered    com.lightcrafts.media.jai.mlib.MlibMedianFilterRIF		com.lightcrafts.media.jai	medianfilter		mlibmedianfilterrif
rendered    com.lightcrafts.media.jai.opimage.MinCRIF		        com.lightcrafts.media.jai	min			sunminrif
rendered    com.lightcrafts.media.jai.mlib.MlibMinRIF		        com.lightcrafts.media.jai	min			mlibminrif
rendered    com.lightcrafts.media.jai.opimage.MinFilterRIF		com.lightcrafts.media.jai	minfilter		sunminfilterrif
rendered    com.lightcrafts.media.jai.mlib.MlibMinFilterRIF		com.lightcrafts.media.jai	minfilter		mlibminfilterrif
rendered    com.lightcrafts.media.jai.opimage.MosaicRIF		com.lightcrafts.media.jai	mosaic		sunmosaicrif
rendered    com.lightcrafts.media.jai.mlib.MlibMosaicRIF		com.lightcrafts.media.jai	mosaic		mlibmosaicrif
rendered    com.lightcrafts.media.jai.opimage.MultiplyCRIF		com.lightcrafts.media.jai	multiply		sunmultiplyrif
rendered    com.lightcrafts.media.jai.opimage.MultiplyComplexCRIF	com.lightcrafts.media.jai	multiplycomplex		sunmultiplycomplexrif
rendered    com.lightcrafts.media.jai.mlib.MlibMultiplyRIF		com.lightcrafts.media.jai	multiply		mlibmultiplyrif
rendered    com.lightcrafts.media.jai.opimage.MultiplyConstCRIF		com.lightcrafts.media.jai	multiplyconst		sunmultiplyconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibMultiplyConstRIF		com.lightcrafts.media.jai	multiplyconst		mlibmultiplyconstrif
rendered    com.lightcrafts.media.jai.opimage.NotCRIF			com.lightcrafts.media.jai	not			sunnotrif
rendered    com.lightcrafts.media.jai.mlib.MlibNotRIF			com.lightcrafts.media.jai	not			mlibnotrif
rendered    javax.media.jai.NullCRIF			   	javax.media.jai		null			sunullrif
rendered    com.lightcrafts.media.jai.opimage.OrConstCRIF		com.lightcrafts.media.jai	orconst			sunorconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibOrConstRIF		com.lightcrafts.media.jai	orconst			mliborconstrif
rendered    com.lightcrafts.media.jai.opimage.OrCRIF			com.lightcrafts.media.jai	or			sunorrif
rendered    com.lightcrafts.media.jai.mlib.MlibOrRIF			com.lightcrafts.media.jai	or			mliborrif
rendered    com.lightcrafts.media.jai.opimage.OrderedDitherRIF		com.lightcrafts.media.jai	ordereddither		sunorderedditherrif
rendered    com.lightcrafts.media.jai.opimage.OverlayCRIF		com.lightcrafts.media.jai	overlay			sunoverlayrif
rendered    com.lightcrafts.media.jai.opimage.PatternRIF		com.lightcrafts.media.jai	pattern			sunpatternrif
rendered    com.lightcrafts.media.jai.opimage.PhaseCRIF			com.lightcrafts.media.jai	phase			sunphaserif
rendered    com.lightcrafts.media.jai.opimage.PeriodicShiftCRIF		com.lightcrafts.media.jai	periodicshift		sunperiodicshiftrif
rendered    com.lightcrafts.media.jai.opimage.PiecewiseCRIF		com.lightcrafts.media.jai	piecewise		sunpiecewiserif
rendered    com.lightcrafts.media.jai.opimage.PNGRIF			com.lightcrafts.media.jai	png			sunpngrif
rendered    com.lightcrafts.media.jai.opimage.PNMRIF			com.lightcrafts.media.jai	pnm			sunpnmrif
rendered    com.lightcrafts.media.jai.opimage.PolarToComplexCRIF	com.lightcrafts.media.jai	polartocomplex		sunpolartocomplexrif
rendered    com.lightcrafts.media.jai.opimage.RescaleCRIF		com.lightcrafts.media.jai	rescale			sunrescalerif
rendered    com.lightcrafts.media.jai.mlib.MlibRescaleRIF		com.lightcrafts.media.jai	rescale			mlibrescalerif
rendered    com.lightcrafts.media.jai.opimage.RotateCRIF		com.lightcrafts.media.jai	rotate			sunrotaterif
rendered    com.lightcrafts.media.jai.mlib.MlibRotateRIF		com.lightcrafts.media.jai	rotate			mlibrotaterif
rendered    com.lightcrafts.media.jai.opimage.ScaleCRIF			com.lightcrafts.media.jai	scale			sunscalerif
rendered    com.lightcrafts.media.jai.mlib.MlibScaleRIF			com.lightcrafts.media.jai	scale			mlibscalerif
rendered    com.lightcrafts.media.jai.opimage.ShearRIF			com.lightcrafts.media.jai	shear			sunshearrif
rendered    com.lightcrafts.media.jai.mlib.MlibShearRIF		        com.lightcrafts.media.jai	shear			mlibshearrif
rendered    com.lightcrafts.media.jai.opimage.StreamRIF			com.lightcrafts.media.jai	stream			sunstreamrif
rendered    com.lightcrafts.media.jai.opimage.SubsampleBinaryToGrayCRIF	com.lightcrafts.media.jai	subsamplebinarytogray	sunsubsamplebinarytograyrif
rendered    com.lightcrafts.media.jai.mlib.MlibSubsampleBinaryToGrayRIF	com.lightcrafts.media.jai	subsamplebinarytogray	mlibsubsamplebinarytograyrif
rendered    com.lightcrafts.media.jai.opimage.SubsampleAverageCRIF	com.lightcrafts.media.jai	subsampleaverage	sunsubsampleaveragerif
rendered    com.lightcrafts.media.jai.mlib.MlibSubsampleAverageRIF	com.lightcrafts.media.jai	subsampleaverage	mlibsubsampleaveragerif
rendered    com.lightcrafts.media.jai.opimage.SubtractCRIF		com.lightcrafts.media.jai	subtract		sunsubtractrif
rendered    com.lightcrafts.media.jai.mlib.MlibSubtractRIF		com.lightcrafts.media.jai	subtract		mlibsubtractrif
rendered    com.lightcrafts.media.jai.opimage.SubtractConstCRIF		com.lightcrafts.media.jai	subtractconst		sunsubtractconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibSubtractConstRIF		com.lightcrafts.media.jai	subtractconst		mlibsubtractconstrif
rendered    com.lightcrafts.media.jai.opimage.SubtractFromConstCRIF	com.lightcrafts.media.jai	subtractfromconst	sunsubtractfromconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibSubtractFromConstRIF	com.lightcrafts.media.jai	subtractfromconst	mlibsubtractfromconstrif
rendered    com.lightcrafts.media.jai.opimage.TIFFRIF			com.lightcrafts.media.jai	tiff			suntiffrif
rendered    com.lightcrafts.media.jai.opimage.ThresholdCRIF		com.lightcrafts.media.jai	threshold		sunthresholdrif
rendered    com.lightcrafts.media.jai.mlib.MlibThresholdRIF		com.lightcrafts.media.jai	threshold		mlibthresholdrif
rendered    com.lightcrafts.media.jai.opimage.TranslateCRIF		com.lightcrafts.media.jai	translate		suntransrif
rendered    com.lightcrafts.media.jai.mlib.MlibTranslateRIF		com.lightcrafts.media.jai	translate		mlibtranslaterif
rendered    com.lightcrafts.media.jai.opimage.TransposeCRIF		com.lightcrafts.media.jai	transpose		suntransposerif
rendered    com.lightcrafts.media.jai.mlib.MlibTransposeRIF		com.lightcrafts.media.jai	transpose		mlibtransposerif
rendered    com.lightcrafts.media.jai.opimage.UnsharpMaskRIF		com.lightcrafts.media.jai	unsharpmask		sununsharpmaskrif
rendered    com.lightcrafts.media.jai.mlib.MlibUnsharpMaskRIF		com.lightcrafts.media.jai	unsharpmask		mlibunsharpmaskrif
rendered    com.lightcrafts.media.jai.opimage.URLRIF			com.lightcrafts.media.jai	url			sunurlrif
rendered    com.lightcrafts.media.jai.opimage.WarpRIF			com.lightcrafts.media.jai	warp			sunwarprif
rendered    com.lightcrafts.media.jai.mlib.MlibWarpRIF			com.lightcrafts.media.jai	warp			mlibwarprif
rendered    com.lightcrafts.media.jai.opimage.XorConstCRIF		com.lightcrafts.media.jai	xorconst		sunxorconstrif
rendered    com.lightcrafts.media.jai.mlib.MlibXorConstRIF		com.lightcrafts.media.jai	xorconst		mlibxorconstrif
rendered    com.lightcrafts.media.jai.opimage.XorCRIF			com.lightcrafts.media.jai	xor			sunxorrif
rendered    com.lightcrafts.media.jai.mlib.MlibXorRIF			com.lightcrafts.media.jai	xor			mlibxorrif


#
# "renderable" factory objects
#
renderable	com.lightcrafts.media.jai.opimage.AbsoluteCRIF		absolute
renderable	com.lightcrafts.media.jai.opimage.AddCollectionCRIF	addcollection
renderable	com.lightcrafts.media.jai.opimage.AddConstCRIF		addconst
renderable	com.lightcrafts.media.jai.opimage.AddCRIF		add
renderable	com.lightcrafts.media.jai.opimage.AffineCRIF		affine
renderable	com.lightcrafts.media.jai.opimage.AndConstCRIF		andconst
renderable	com.lightcrafts.media.jai.opimage.AndCRIF		and
renderable	com.lightcrafts.media.jai.opimage.BandCombineCRIF	bandcombine
renderable	com.lightcrafts.media.jai.opimage.BandMergeCRIF         bandmerge
renderable	com.lightcrafts.media.jai.opimage.BandSelectCRIF	bandselect
renderable	com.lightcrafts.media.jai.opimage.BinarizeCRIF		binarize
renderable	com.lightcrafts.media.jai.opimage.ClampCRIF		clamp
renderable	com.lightcrafts.media.jai.opimage.ColorConvertCRIF	colorconvert
renderable	com.lightcrafts.media.jai.opimage.CompositeCRIF		composite
renderable	com.lightcrafts.media.jai.opimage.ConjugateCRIF		conjugate
renderable	com.lightcrafts.media.jai.opimage.ConstantCRIF		constant
renderable	com.lightcrafts.media.jai.opimage.CropCRIF		crop
renderable	com.lightcrafts.media.jai.opimage.DCTCRIF		dct
renderable	com.lightcrafts.media.jai.opimage.DFTCRIF		dft
renderable	com.lightcrafts.media.jai.opimage.DivideCRIF		divide
renderable	com.lightcrafts.media.jai.opimage.DivideComplexCRIF	dividecomplex
renderable	com.lightcrafts.media.jai.opimage.DivideByConstCRIF	dividebyconst
renderable	com.lightcrafts.media.jai.opimage.DivideIntoConstCRIF	divideintoconst
renderable	com.lightcrafts.media.jai.opimage.ExpCRIF		exp
renderable	com.lightcrafts.media.jai.opimage.FormatCRIF		format
renderable	com.lightcrafts.media.jai.opimage.IDCTCRIF		idct
renderable	com.lightcrafts.media.jai.opimage.IDFTCRIF		idft
renderable	com.lightcrafts.media.jai.opimage.IIPCRIF		iip
renderable	com.lightcrafts.media.jai.opimage.InvertCRIF		invert
renderable	com.lightcrafts.media.jai.opimage.LogCRIF		log
renderable	com.lightcrafts.media.jai.opimage.LookupCRIF		lookup
renderable	com.lightcrafts.media.jai.opimage.MagnitudeCRIF		magnitude
renderable	com.lightcrafts.media.jai.opimage.MagnitudeSquaredCRIF	magnitudesquared
renderable	com.lightcrafts.media.jai.opimage.MatchCDFCRIF		matchcdf
renderable	com.lightcrafts.media.jai.opimage.MaxCRIF		max
renderable	com.lightcrafts.media.jai.opimage.MinCRIF		min
renderable	com.lightcrafts.media.jai.opimage.MultiplyCRIF		multiply
renderable	com.lightcrafts.media.jai.opimage.MultiplyComplexCRIF	multiplycomplex
renderable	com.lightcrafts.media.jai.opimage.MultiplyConstCRIF	multiplyconst
renderable	com.lightcrafts.media.jai.opimage.NotCRIF		not
renderable	javax.media.jai.NullCRIF			null
renderable	com.lightcrafts.media.jai.opimage.OrConstCRIF		orconst
renderable	com.lightcrafts.media.jai.opimage.OrCRIF		or
renderable	com.lightcrafts.media.jai.opimage.OverlayCRIF		overlay
renderable	com.lightcrafts.media.jai.opimage.PeriodicShiftCRIF	periodicshift
renderable	com.lightcrafts.media.jai.opimage.PhaseCRIF		phase
renderable	com.lightcrafts.media.jai.opimage.PiecewiseCRIF		piecewise
renderable	com.lightcrafts.media.jai.opimage.PolarToComplexCRIF	polartocomplex
renderable	com.lightcrafts.media.jai.opimage.RenderableCRIF	renderable
renderable	com.lightcrafts.media.jai.opimage.RescaleCRIF		rescale
renderable	com.lightcrafts.media.jai.opimage.RotateCRIF		rotate
renderable	com.lightcrafts.media.jai.opimage.ScaleCRIF		scale
renderable	com.lightcrafts.media.jai.opimage.SubsampleBinaryToGrayCRIF	subsamplebinarytogray
renderable	com.lightcrafts.media.jai.opimage.SubsampleAverageCRIF	subsampleaverage
renderable	com.lightcrafts.media.jai.opimage.SubtractCRIF		subtract
renderable	com.lightcrafts.media.jai.opimage.SubtractConstCRIF	subtractconst
renderable	com.lightcrafts.media.jai.opimage.SubtractFromConstCRIF	subtractfromconst
renderable	com.lightcrafts.media.jai.opimage.ThresholdCRIF		threshold
renderable	com.lightcrafts.media.jai.opimage.TranslateCRIF		translate
renderable	com.lightcrafts.media.jai.opimage.TransposeCRIF		transpose
renderable	com.lightcrafts.media.jai.opimage.XorConstCRIF		xorconst
renderable	com.lightcrafts.media.jai.opimage.XorCRIF		xor

#
# "collection" factory objects
#
collection	com.lightcrafts.media.jai.opimage.AddConstToCollectionCIF	com.lightcrafts.media.jai	addconsttocollection	sunaddconsttocollectioncif

#
# tile decoder factory objects
#
tileDecoder com.lightcrafts.media.jai.tilecodec.GZIPTileDecoderFactory	com.lightcrafts.media.jai	gzip			sungziptiledecoderfactory
tileDecoder com.lightcrafts.media.jai.tilecodec.JPEGTileDecoderFactory	com.lightcrafts.media.jai	jpeg			suntiledecoderfactory	
tileDecoder com.lightcrafts.media.jai.tilecodec.RawTileDecoderFactory	com.lightcrafts.media.jai	raw			sunrawtiledecoderfactory

#
# tile encoder factory objects
#
tileEncoder com.lightcrafts.media.jai.tilecodec.GZIPTileEncoderFactory	com.lightcrafts.media.jai	gzip			sungziptileencoderfactory
tileEncoder com.lightcrafts.media.jai.tilecodec.JPEGTileEncoderFactory	com.lightcrafts.media.jai	jpeg			sunjpegtileencoderfactory
tileEncoder com.lightcrafts.media.jai.tilecodec.RawTileEncoderFactory	com.lightcrafts.media.jai	raw			sunrawtileencoderfactory

#
# remote rendered factory objects
#
remoterendered      com.lightcrafts.media.jai.rmi.JAIRMICRIF             jairmi

#
# remote renderable factory objects
#
remoterenderable    com.lightcrafts.media.jai.rmi.JAIRMICRIF             jairmi

#
# "rendered" factory object preferences
#
pref	rendered	absolute	com.lightcrafts.media.jai	mlibabsoluterif	sunabsoluterif
pref	rendered	addconst	com.lightcrafts.media.jai	mlibaddconstrif	sunaddconstrif
pref	rendered	add		com.lightcrafts.media.jai	mlibaddrif	sunaddrif
pref	rendered	affine		com.lightcrafts.media.jai	mlibaffinerif	sunaffinerif
pref	rendered	and		com.lightcrafts.media.jai	mlibandrif	sunandrif
pref	rendered	andconst	com.lightcrafts.media.jai	mlibandconstrif	sunandconstrif
pref	rendered	bandcombine	com.lightcrafts.media.jai	mlibbandcombinerif	sunbandcombinerif
pref	rendered	bandselect	com.lightcrafts.media.jai	mlibbandselectrif	sunbandselectrif
pref	rendered	binarize	com.lightcrafts.media.jai	mlibbinarizerif	sunbinarizerif
pref	rendered	boxfilter	com.lightcrafts.media.jai	mlibboxfilterrif	sunboxfilterrif
pref	rendered	clamp		com.lightcrafts.media.jai	mlibclamprif	sunclamprif
pref	rendered	composite	com.lightcrafts.media.jai	mlibcompositerif	suncompositerif
pref	rendered	convolve	com.lightcrafts.media.jai	mlibconvolverif	sunconvolverif
pref	rendered	dct		com.lightcrafts.media.jai	mlibdctrif	sundctrif
pref	rendered	dilate		com.lightcrafts.media.jai	mlibdilaterif	sundilaterif
pref	rendered	idct		com.lightcrafts.media.jai	mlibidctrif	sunidctrif
pref	rendered	dft		com.lightcrafts.media.jai	mlibdftrif	sundftrif
pref	rendered	idft		com.lightcrafts.media.jai	mlibidftrif	sunidftrif
pref	rendered	divide		com.lightcrafts.media.jai	mlibdividerif	sundividerif
pref	rendered	dividebyconst	com.lightcrafts.media.jai	mlibdividebyconstrif	sundividebyconstrif
pref	rendered	divideintoconst	com.lightcrafts.media.jai	mlibdivideintoconstrif	sundivideintoconstrif
pref	rendered	erode		com.lightcrafts.media.jai	mliberoderif	suneroderif
pref	rendered	exp		com.lightcrafts.media.jai	mlibexprif	sunexprif
pref	rendered	extrema		com.lightcrafts.media.jai	mlibextremarif	sunextremarif
pref	rendered	filteredsubsample com.lightcrafts.media.jai	mlibfilteredsubsamplerif	sunfilteredsubsamplerif
pref	rendered	gradientmagnitude	com.lightcrafts.media.jai	mlibgradientrif sungradientrif
pref	rendered	histogram	com.lightcrafts.media.jai	mlibhistogramrif sunhistogramrif
pref	rendered	invert		com.lightcrafts.media.jai	mlibinvertrif	suninvertrif
pref	rendered	log		com.lightcrafts.media.jai	mliblogrif	sunlogrif
pref	rendered	lookup		com.lightcrafts.media.jai	mliblookuprif	sunlookuprif
pref	rendered	max		com.lightcrafts.media.jai	mlibmaxrif	sunmaxrif
pref	rendered	maxfilter	com.lightcrafts.media.jai	mlibmaxfilterrif	sunmaxfilterrif
pref	rendered	mean		com.lightcrafts.media.jai	mlibmeanrif	sunmeanrif
pref	rendered	medianfilter	com.lightcrafts.media.jai	mlibmedianfilterrif	sunmedianfilterrif
pref	rendered	min		com.lightcrafts.media.jai	mlibminrif	sunminrif
pref	rendered	minfilter	com.lightcrafts.media.jai	mlibminfilterrif	sunminfilterrif
pref	rendered	mosaic	com.lightcrafts.media.jai	mlibmosaicrif	sunmosaicrif
pref	rendered	multiply	com.lightcrafts.media.jai	mlibmultiplyrif	sunmultiplyrif
pref	rendered	multiplyconst	com.lightcrafts.media.jai	mlibmultiplyconstrif	sunmultiplyconstrif
pref	rendered	not		com.lightcrafts.media.jai	mlibnotrif	sunnotrif
pref	rendered	or		com.lightcrafts.media.jai	mliborrif	sunorrif
pref	rendered	orconst		com.lightcrafts.media.jai	mliborconstrif	sunorconstrif
pref	rendered	rescale		com.lightcrafts.media.jai	mlibrescalerif	sunrescalerif
pref	rendered	rotate		com.lightcrafts.media.jai	mlibrotaterif	sunrotaterif
pref	rendered	scale		com.lightcrafts.media.jai	mlibscalerif	sunscalerif
pref	rendered	shear		com.lightcrafts.media.jai	mlibshearrif	sunshearrif
pref	rendered	subsampleaverage	com.lightcrafts.media.jai	mlibsubsampleaveragerif	sunsubsampleaveragerif
pref	rendered	subsamplebinarytogray	com.lightcrafts.media.jai	mlibsubsamplebinarytograyrif	sunsubsamplebinarytograyrif
pref	rendered	subtractconst	com.lightcrafts.media.jai	mlibsubtractconstrif	sunsubtractconstrif
pref	rendered	subtractfromconst	com.lightcrafts.media.jai	mlibsubtractfromconstrif	sunsubtractfromconstrif
pref	rendered	subtract	com.lightcrafts.media.jai	mlibsubtractrif	sunsubtractrif
pref	rendered	threshold	com.lightcrafts.media.jai	mlibthresholdrif	sunthresholdrif
pref	rendered	translate	com.lightcrafts.media.jai	mlibtranslaterif	suntransrif
pref	rendered	transpose	com.lightcrafts.media.jai	mlibtransposerif	suntransposerif
pref	rendered	unsharpmask	com.lightcrafts.media.jai	mlibunsharpmaskrif	sununsharpmaskrif
pref	rendered	warp		com.lightcrafts.media.jai	mlibwarprif	sunwarprif
pref	rendered	xor		com.lightcrafts.media.jai	mlibxorrif	sunxorrif
pref	rendered	xorconst	com.lightcrafts.media.jai	mlibxorconstrif	sunxorconstrif

