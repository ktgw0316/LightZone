package com.lightcrafts.jai.utils

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class FunctionsTest {

    private lateinit var image: BufferedImage

    @BeforeEach
    fun setUp() {
        image = BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB)
        //        for (x in 0 until image.width) {
        //            for (y in 0 until image.height) {
        //                image.setRGB(x, y, 0xFF0000)
        //            }
        //        }
    }

    @Test
    fun measureFastGaussianBlurExecutionTime() {
        val repeat = 1000
        val startTime = System.currentTimeMillis()

        for(i in 0..repeat) {
            Functions.fastGaussianBlur(image, 2.2)
        }

        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime
        println("Execution time of $repeat fastGaussianBlur is $executionTime milliseconds")
    }

}
