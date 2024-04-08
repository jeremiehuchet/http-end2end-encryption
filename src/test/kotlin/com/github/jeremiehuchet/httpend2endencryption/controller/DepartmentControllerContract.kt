package com.github.jeremiehuchet.httpend2endencryption.controller

import com.github.jeremiehuchet.httpend2endencryption.test.When
import com.github.jeremiehuchet.httpend2endencryption.test.decodeJsonToList
import com.github.jeremiehuchet.httpend2endencryption.test.decodeJsonToMap
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class DepartmentControllerContract {

    @BeforeEach
    fun configureRestAssured(@LocalServerPort port: Int) {
        RestAssured.port = port
    }

    fun RequestSpecification.specificHeaders() = this
    abstract fun RequestSpecification.encodedRequestBody(body: String): RequestSpecification
    abstract fun ValidatableResponse.hasExpectedResponseHeaders(): ValidatableResponse
    abstract fun ValidatableResponse.bodySatisfies(verifier: (String) -> Unit): ValidatableResponse

    @Test
    fun can_list_departments() {
        given()
                .log().all()
                .specificHeaders()

                .When()
                .get("/departments")

                .then()
                .log().all()
                .statusCode(200)
                .hasExpectedResponseHeaders()
                .bodySatisfies { body ->
                    assertThat(body.decodeJsonToList(), hasItem(
                            allOf(
                                    hasEntry("dep", "35"),
                                    hasEntry("reg", 53),
                                    hasEntry("cheflieu", "35238"),
                                    hasEntry("tncc", 1),
                                    hasEntry("ncc", "ILLE ET VILAINE"),
                                    hasEntry("nccend", "Ille-et-Vilaine"),
                                    hasEntry("libelle", "Ille-et-Vilaine"),
                            )
                    ))
                }
    }

    @Test
    fun can_find_department_by_dep() {
        given()
                .log().all()
                .specificHeaders()

                .When()
                .get("/departments/35")

                .then()
                .log().all()
                .statusCode(200)
                .hasExpectedResponseHeaders()
                .bodySatisfies { body ->
                    assertThat(body.decodeJsonToMap(), allOf(
                            hasEntry("dep", "35"),
                            hasEntry("reg", 53),
                            hasEntry("cheflieu", "35238"),
                            hasEntry("tncc", 1),
                            hasEntry("ncc", "ILLE ET VILAINE"),
                            hasEntry("nccend", "Ille-et-Vilaine"),
                            hasEntry("libelle", "Ille-et-Vilaine"),
                    ))
                }
    }

    @Test
    fun can_update_department() {
        given()
                .log().all()
                .contentType(ContentType.JSON)
                .specificHeaders()
                .encodedRequestBody(
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
                .log().all()
                .statusCode(200)
                .hasExpectedResponseHeaders()
                .bodySatisfies { body ->
                    assertThat(body.decodeJsonToMap(), allOf(
                            hasEntry("dep", "75"),
                            hasEntry("reg", 11),
                            hasEntry("cheflieu", "75056"),
                            hasEntry("tncc", 0),
                            hasEntry("ncc", "PARIS"),
                            hasEntry("nccend", "Paris"),
                            hasEntry("libelle", "Paris \uD83C\uDDEB\uD83C\uDDF7")
                    ))
                }
    }
}