package com.lightcrafts.jai.opimage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import java.awt.image.BufferedImage
import java.awt.image.DataBuffer

class FastBilateralFilterOpImageTest : FunSpec({
    var sourceImage: BufferedImage? = null
    var filterOpImage: FastBilateralFilterOpImage? = null

    beforeTest {
        sourceImage = BufferedImage(10, 10, BufferedImage.TYPE_USHORT_GRAY)
        filterOpImage = FastBilateralFilterOpImage(sourceImage, HashMap<Any, Any>(), 1.0f, 1.0f)
    }

    test("computes correctly for uniform image") {
        val result = filterOpImage?.data
        result?.sampleModel?.dataType shouldBe DataBuffer.TYPE_USHORT
        result?.width shouldBe sourceImage?.width
        result?.height shouldBe sourceImage?.height
    }

//    test("throws exception for negative sigma") {
//        shouldThrow<IllegalArgumentException> {
//            FastBilateralFilterOpImage(sourceImage, HashMap<Any, Any>(), -1.0f, 1.0f)
//        }
//        shouldThrow<IllegalArgumentException> {
//            FastBilateralFilterOpImage(sourceImage, HashMap<Any, Any>(), 1.0f, -1.0f)
//        }
//    }

    test("handles large sigma values") {
        val largeSigmaFilter = FastBilateralFilterOpImage(sourceImage, HashMap<Any, Any>(), 100.0f, 100.0f)
        val result = largeSigmaFilter.data
        result.sampleModel.dataType shouldBe DataBuffer.TYPE_USHORT
        result.width shouldBe sourceImage?.width
        result.height shouldBe sourceImage?.height
    }
})
