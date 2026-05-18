package com.lightcrafts.app

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CheckForUpdateTest : BehaviorSpec({
    Given("a stable current version and a stable comparison version") {
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
                CheckForUpdate.compareVersions("2.0.0", "1.1.0") shouldBe false
            }
        }

        When("current major version is higher and patch version is lower") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("2.0.0", "1.0.1") shouldBe false
            }
        }

        When("minor version crosses 10") {
            Then("10.y.z should greater than 9.y.z") {
                CheckForUpdate.compareVersions("9.0.0", "10.0.0") shouldBe true
            }
        }

        When("minor version crosses 10") {
            Then("x.10.z should greater than x.9.z") {
                CheckForUpdate.compareVersions("1.9.0", "1.10.0") shouldBe true
            }
        }

        When("patch version crosses 10") {
            Then("x.y.10 should greater than x.y.9") {
                CheckForUpdate.compareVersions("1.0.9", "1.0.10") shouldBe true
            }
        }

        When("versions are equal") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("1.0.0", "1.0.0") shouldBe false
            }
        }
    }

    Given("a beta current version and a stable comparison version") {
        When("current version is lower") {
            Then("compareVersions should return true") {
                CheckForUpdate.compareVersions("1.0.0.beta1", "1.0.0") shouldBe true
            }
        }

        When("current version is higher") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("1.0.1.beta1", "1.0.0") shouldBe false
            }
        }
    }

    Given("a stable current version and a beta comparison version") {
        When("current version is higher") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("1.0.0", "1.0.0.beta1") shouldBe false
            }
        }
    }

    Given("a beta current version and a beta comparison version") {
        When("current version is lower") {
            Then("compareVersions should return true") {
                CheckForUpdate.compareVersions("1.0.0.beta9", "1.0.0.beta10") shouldBe true
            }
        }

        When("current version is higher") {
            Then("compareVersions should return false") {
                CheckForUpdate.compareVersions("1.0.0.beta10", "1.0.0.beta9") shouldBe false
            }
        }
    }
})