package com.github.jeremiehuchet.httpend2endencryption.controller

import com.github.jeremiehuchet.httpend2endencryption.http.doKeyAgreement
import com.github.jeremiehuchet.httpend2endencryption.http.encodeToBase64String
import com.github.jeremiehuchet.httpend2endencryption.test.When
import com.github.jeremiehuchet.httpend2endencryption.test.decodeJsonToList
import com.github.jeremiehuchet.httpend2endencryption.test.decodeJsonToMap
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.RequestLoggingFilter.logRequestTo
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter.logResponseTo
import io.restassured.http.ContentType.JSON
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

@SpringBootTest(webEnvironment = RANDOM_PORT)
@PropertySource()
class EncryptedDepartmentControllerTest {

    companion object {

        // generate transport keys
        private val SERVER_KEY_PAIR = createECDHKeyPair()
        val CLIENT_KEY_PAIR = createECDHKeyPair()

        private fun createECDHKeyPair(): KeyPair {
            val ecSpec = ECGenParameterSpec("secp256r1")
            val keyPairGenerator = KeyPairGenerator.getInstance("EC")
            keyPairGenerator.initialize(ecSpec, SecureRandom())
            return keyPairGenerator.generateKeyPair()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureServerPrivateKey(registry: DynamicPropertyRegistry) {
            registry.add("http.secure-transport.private-key") {
                """
                    -----BEGIN PRIVATE KEY-----
                    ${SERVER_KEY_PAIR.private.encoded.encodeToBase64String()}
                    -----END PRIVATE KEY-----
                """.trimIndent()
            }
        }

        private fun encrypt(clearData: String): ByteArray {
            val sharedSecret = doKeyAgreement(CLIENT_KEY_PAIR.private, SERVER_KEY_PAIR.public)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, sharedSecret)
            }
            return cipher.iv + cipher.doFinal(clearData.toByteArray())
        }

        private fun decrypt(encryptedData: ByteArray): String {
            val AES_GCM_CIPHER_NO_PADDING = "AES/GCM/NoPadding"
            val AES_GCM_IV_LENGTH = 12
            val AES_GCM_TAG_LENGTH_IN_BITS = 128

            val sharedSecret = doKeyAgreement(CLIENT_KEY_PAIR.private, SERVER_KEY_PAIR.public)

            val cipher = Cipher.getInstance(AES_GCM_CIPHER_NO_PADDING).apply {
                val iv = encryptedData.copyOfRange(0, AES_GCM_IV_LENGTH)
                val ivSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH_IN_BITS, iv)
                init(Cipher.DECRYPT_MODE, sharedSecret, ivSpec)
            }
            return cipher.doFinal(encryptedData, AES_GCM_IV_LENGTH, encryptedData.size - AES_GCM_IV_LENGTH)
                .decodeToString()
        }

    }

    @BeforeEach
    fun configureRestAssured(@LocalServerPort port: Int) {
        RestAssured.port = port
    }

    @Test
    fun can_find_department_by_dep() {
        val encryptedResponse = given()
            .header(
                "Content-Encoding", "aes128gcm; public-key=${CLIENT_KEY_PAIR.public.encoded.encodeToBase64String()}"
            )

            .When()
            .get("/encrypted/departments/35")

            .then()
            .statusCode(200)
            .header("Content-Encoding", equalTo("aes128gcm"))
            .extract().body().asByteArray()

        val decryptedResponse = decrypt(encryptedResponse)
        assertThat(
            decryptedResponse.decodeJsonToMap(), allOf(
                hasEntry("dep", "35"),
                hasEntry("reg", 53),
                hasEntry("cheflieu", "35238"),
                hasEntry("tncc", 1),
                hasEntry("ncc", "ILLE ET VILAINE"),
                hasEntry("nccend", "Ille-et-Vilaine"),
                hasEntry("libelle", "Ille-et-Vilaine")
            )
        )
    }

    @Test
    fun can_list_departments() {
        val encryptedResponse = given()
            .header(
                "Content-Encoding", "aes128gcm; public-key=${CLIENT_KEY_PAIR.public.encoded.encodeToBase64String()}"
            )

            .When()
            .get("/encrypted/departments")

            .then()
            .statusCode(200)
            .header("Content-Encoding", equalTo("aes128gcm"))
            .extract().body().asByteArray()

        val unencryptedListOfDepartments = When()
            .get("/departments")

            .then()
            .extract().body().asString()

        assertThat(encryptedResponse, not(equalTo(unencryptedListOfDepartments)))

        val decryptedResponse = decrypt(encryptedResponse)
        assertThat(
            decryptedResponse.decodeJsonToList(), hasItem(
                allOf(
                    hasEntry("dep", "35"),
                    hasEntry("reg", 53),
                    hasEntry("cheflieu", "35238"),
                    hasEntry("tncc", 1),
                    hasEntry("ncc", "ILLE ET VILAINE"),
                    hasEntry("nccend", "Ille-et-Vilaine"),
                    hasEntry("libelle", "Ille-et-Vilaine")
                )
            )
        )
    }

    @Test
    fun can_update_department() {
        val encryptedResponse = given()
            .contentType(JSON)
            .header(
                "Content-Encoding", "aes128gcm; public-key=${CLIENT_KEY_PAIR.public.encoded.encodeToBase64String()}"
            )
            .body(
                encrypt(
                    """
                        {
                          "dep": "75",
                          "reg": 11,
                          "cheflieu": "75056",
                          "tncc": 0,
                          "ncc": "PARIS",
                          "nccend": "Paris",
                          "libelle": "Paris 🇫🇷 encrypted update"
                        }
                        """.trimIndent()
                )
            )

            .When()
            .put("/encrypted/departments/75")

            .then()
            .statusCode(200)
            .header("Content-Encoding", equalTo("aes128gcm"))
            .extract().body().asByteArray()

        val decryptedResponse = decrypt(encryptedResponse)
        assertThat(
            decryptedResponse.decodeJsonToMap(), allOf(
                hasEntry("dep", "75"),
                hasEntry("reg", 11),
                hasEntry("cheflieu", "75056"),
                hasEntry("tncc", 0),
                hasEntry("ncc", "PARIS"),
                hasEntry("nccend", "Paris"),
                hasEntry("libelle", "Paris \uD83C\uDDEB\uD83C\uDDF7 encrypted update")
            )
        )
    }

    @Test
    fun rejects_unencrypted_request() {
        When()
            .get("/encrypted/departments/35")

            .then()
            .statusCode(415)
    }
}
