package com.github.jeremiehuchet.httpend2endencryption.controller

import com.github.jeremiehuchet.httpend2endencryption.http.*
import com.github.jeremiehuchet.httpend2endencryption.test.When
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class EncryptedDepartmentControllerTest : DepartmentControllerContract() {

    companion object {

        // generate transport keys
        private val SERVER_KEY_PAIR = createECDHKeyPair()
        private val CLIENT_KEY_PAIR = createECDHKeyPair()

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
    }

    @BeforeEach
    fun prefixAllRequestsPathWithEncryptedEndpoint(@LocalServerPort port: Int) {
        RestAssured.basePath = "/encrypted"
    }

    override fun RequestSpecification.specificHeaders(): RequestSpecification =
            this.header("Content-Encoding", "aes128gcm; public-key=${CLIENT_KEY_PAIR.public.encoded.encodeToBase64String()}")

    override fun RequestSpecification.encodedRequestBody(body: String): RequestSpecification {
        // encrypt request body
        val sharedSecret = doKeyAgreement(CLIENT_KEY_PAIR.private, SERVER_KEY_PAIR.public)
        val cipher = Cipher.getInstance(AES_GCM_CIPHER_NO_PADDING).apply {
            init(Cipher.ENCRYPT_MODE, sharedSecret)
        }
        val decryptedBody = cipher.iv + cipher.doFinal(body.toByteArray())

        return this.body(decryptedBody)
    }

    override fun ValidatableResponse.hasExpectedResponseHeaders(): ValidatableResponse = this.header("Content-Encoding", "aes128gcm")

    override fun ValidatableResponse.bodySatisfies(verifier: (String) -> Unit): ValidatableResponse {
        // decrypt response body
        val encryptedResponseBody = this.extract().body().asByteArray();

        val sharedSecret = doKeyAgreement(CLIENT_KEY_PAIR.private, SERVER_KEY_PAIR.public)
        val cipher = Cipher.getInstance(AES_GCM_CIPHER_NO_PADDING).apply {
            val iv = encryptedResponseBody.copyOfRange(0, AES_GCM_IV_LENGTH)
            val ivSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH_IN_BITS, iv)
            init(Cipher.DECRYPT_MODE, sharedSecret, ivSpec)
        }
        val decryptedResponseBody = cipher.doFinal(encryptedResponseBody, AES_GCM_IV_LENGTH, encryptedResponseBody.size - AES_GCM_IV_LENGTH)
                .decodeToString()

        verifier(decryptedResponseBody)
        return this
    }

    @Test
    fun rejects_unencrypted_request() {
        given()
                .log().all()

                .When()
                .get("/departments/35")

                .then()
                .log().all()
                .statusCode(415)
    }
}
