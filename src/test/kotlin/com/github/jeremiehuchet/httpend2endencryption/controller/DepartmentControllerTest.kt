package com.github.jeremiehuchet.httpend2endencryption.controller

import com.github.jeremiehuchet.httpend2endencryption.http.doKeyAgreement
import com.github.jeremiehuchet.httpend2endencryption.http.encodeToBase64String
import com.github.jeremiehuchet.httpend2endencryption.test.When
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.RequestLoggingFilter.logRequestTo
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter.logResponseTo
import io.restassured.http.ContentType.JSON
import io.restassured.specification.RequestSpecification
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

@SpringBootTest(webEnvironment = RANDOM_PORT)
class DepartmentControllerTest {

    @BeforeEach
    fun configureRestAssured(@LocalServerPort port: Int) {
        RestAssured.port = port
        RestAssured.filters(logRequestTo(System.out), logResponseTo(System.out))
    }

    @Test
    fun can_list_departments() {
        When()
            .get("/departments")

            .then()
            .statusCode(200)
            .body("$", hasSize<Any>(101))
            .body("[35].dep", equalTo("35"))
            .body("[35].reg", equalTo(53))
            .body("[35].cheflieu", equalTo("35238"))
            .body("[35].tncc", equalTo(1))
            .body("[35].ncc", equalTo("ILLE ET VILAINE"))
            .body("[35].nccend", equalTo("Ille-et-Vilaine"))
            .body("[35].libelle", equalTo("Ille-et-Vilaine"))
    }

    @Test
    fun can_find_department_by_dep() {
        When()
            .get("/departments/35")

            .then()
            .statusCode(200)
            .body("dep", equalTo("35"))
            .body("reg", equalTo(53))
            .body("cheflieu", equalTo("35238"))
            .body("tncc", equalTo(1))
            .body("ncc", equalTo("ILLE ET VILAINE"))
            .body("nccend", equalTo("Ille-et-Vilaine"))
            .body("libelle", equalTo("Ille-et-Vilaine"))
    }

    @Test
    fun can_update_department() {
        given()
            .contentType(JSON)
            .body(
                """
                {
                  "dep": "75",
                  "reg": 11,
                  "cheflieu": "75056",
                  "tncc": 0,
                  "ncc": "PARIS",
                  "nccend": "Paris",
                  "libelle": "Paris 🇫🇷"
                }
                """.trimIndent()
            )

            .When()
            .put("/departments/75")

            .then()
            .statusCode(200)
            .body("dep", equalTo("75"))
            .body("reg", equalTo(11))
            .body("cheflieu", equalTo("75056"))
            .body("tncc", equalTo(0))
            .body("ncc", equalTo("PARIS"))
            .body("nccend", equalTo("Paris"))
            .body("libelle", equalTo("Paris \uD83C\uDDEB\uD83C\uDDF7"))
    }
}
