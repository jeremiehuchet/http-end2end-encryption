package com.github.jeremiehuchet.httpend2endencryption.controller

import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.web.server.LocalServerPort

class DepartmentControllerTest : DepartmentControllerContract() {

    @BeforeEach
    fun resetBasePathToInvokeClearTextEndpoints(@LocalServerPort port: Int) {
        RestAssured.basePath = "/"
    }
    override fun RequestSpecification.encodedRequestBody(body: String): RequestSpecification = this.body(body)

    override fun ValidatableResponse.hasExpectedResponseHeaders() = this

    override fun ValidatableResponse.bodySatisfies(verifier: (String) -> Unit): ValidatableResponse {
        verifier(this.extract().body().asString())
        return this
    }
}
