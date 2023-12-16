package com.lightcrafts.app

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class CheckForUpdateTest : BehaviorSpec({
    Given("a stable current version and a comparison version") {
        When("current version is lower") {
            Then("compareVersions should return true") {
                CheckForUpdate.compareVersions("1.0.0", "1.0.1") shouldBe true
            }
        }

        When("current version is higher") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("1.0.1", "1.0.0") shouldBe false
            }
        }

        When("current major version is higher and minor version is lower") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("2.0.0", "1.0.1") shouldBe false
            }
        }

        When("versions are equal") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("1.0.0", "1.0.0") shouldBe false
            }
        }
    }

    Given("a beta current version and a comparison version") {
        When("current version is lower") {
            Then("compareVersions should return true") {
                CheckForUpdate.compareVersions("1.0.0~beta1", "1.0.0") shouldBe true
            }
        }

        When("current version is higher") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("1.0.1~beta1", "1.0.0") shouldBe false
            }
        }
    }
})