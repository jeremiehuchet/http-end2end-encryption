package com.github.jeremiehuchet.httpend2endencryption.http

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.CONTENT_ENCODING
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.PrintWriter
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.DestroyFailedException

private const val AES_128_GCM = "aes128gcm"

private const val ALGORITHM_EC = "EC"
internal const val AES_GCM_CIPHER_NO_PADDING = "AES/GCM/NoPadding"
internal const val AES_GCM_IV_LENGTH = 12
internal const val AES_GCM_TAG_LENGTH_IN_BITS = 128

/**
 * A servlet filter implementing request / response encryption.
 *
 * Partially inspired by RFC 8188, it handles requests having request header <code>Content-Encoding: aes128gcm</code>.
 * But it doesn't structure the request body as specified in RFC 8188 draft, instead:
 *
 * - the request header `Content-Encoding` must contain the client's public key
 * - the request body contains the payload encrypted with the server public key
 * - the response is encrypted with the public key transmitted in the `Content-Encoding` header
 */
class EncryptedContentEncodingFilter(properties: HttpEncryptedProperties) : HttpFilter() {

    val logger = KotlinLogging.logger {}

    private val serverPrivateKey = KeyFactory.getInstance(ALGORITHM_EC)
        .generatePrivate(properties.getPrivateKeySpec())

    override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val contentEncodingHeader = request.getHeader(CONTENT_ENCODING)?.trim() ?: ""
        if (contentEncodingHeader.startsWith(AES_128_GCM)) {
            // 1. read client public key
            val clientPublicKey = contentEncodingHeader.replaceBeforeLast("public-key=", "")
                .removePrefix("public-key=")
                .decodeBase64()
                .let { KeyFactory.getInstance(ALGORITHM_EC).generatePublic(X509EncodedKeySpec(it)) }

            // 2. build shared secret
            val sharedSecret = doKeyAgreement(serverPrivateKey, clientPublicKey)

            try {
                // 3. wrap request/response with decrypt/encrypt streams
                response.setHeader(CONTENT_ENCODING, AES_128_GCM)
                EncryptedHttpServletRequest(sharedSecret, request).use { requestWrapper ->
                    EncryptedHttpServletResponse(sharedSecret, response).use { responseWrapper ->

                        // 4. execute filter chain
                        chain.doFilter(requestWrapper, responseWrapper)
                    }
                }
            } finally {
                // 5. ensure shared is destroyed
                try {
                    sharedSecret.destroy()
                } catch (e: DestroyFailedException) {
                    logger.warn { "Unable to destroy shared secret 😱. It seems ${sharedSecret::class} doesn't implement javax.security.auth.Destroyable" }
                }
            }
        } else {
            // 415 Unsupported Media Type
            // https://developer.mozilla.org/docs/Web/HTTP/Status/415
            response.sendError(415, "Unsupported Media Type")
        }
    }

    private class EncryptedHttpServletRequest(private val key: SecretKeySpec, request: HttpServletRequest) :
        HttpServletRequestWrapper(request), AutoCloseable {

        private var inputStream: ServletInputStream? = null
        private var reader: BufferedReader? = null

        override fun getInputStream(): ServletInputStream {
            if (null == inputStream) {
                inputStream = CipherServletInputStream(key, AES_GCM_CIPHER_NO_PADDING, request.inputStream)
            }
            return inputStream!!
        }

        override fun getReader(): BufferedReader {
            if (null == reader) {
                reader = getInputStream().bufferedReader()
            }
            return reader!!
        }

        override fun close() {
            inputStream?.close()
        }
    }

    private class CipherServletInputStream(
        secretKey: SecretKeySpec,
        cipherTransformation: String,
        private val originalInputStream: ServletInputStream
    ) : ServletInputStream() {

        private val cipher = Cipher.getInstance(cipherTransformation)
            .apply {
                val iv: ByteArray = originalInputStream.readNBytes(AES_GCM_IV_LENGTH)
                val ivSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH_IN_BITS, iv)
                init(DECRYPT_MODE, secretKey, ivSpec)
            }

        private val cipherInputStream = CipherInputStream(originalInputStream, cipher)

        override fun read(b: ByteArray, off: Int, len: Int) = cipherInputStream.read(b, off, len)

        override fun read() = cipherInputStream.read()

        override fun isFinished() = originalInputStream.isFinished

        override fun isReady() = originalInputStream.isReady

        override fun setReadListener(listener: ReadListener?) = originalInputStream.setReadListener(listener)

    }

    internal class EncryptedHttpServletResponse(private val key: SecretKeySpec, response: HttpServletResponse) :
        HttpServletResponseWrapper(response), AutoCloseable {

        private var outputStream: ServletOutputStream? = null
        private var writer: PrintWriter? = null

        override fun getOutputStream(): ServletOutputStream {
            if (null == outputStream) {
                outputStream = CipherServletOutputStream(AES_GCM_CIPHER_NO_PADDING, key, response.outputStream)
            }
            return outputStream!!
        }

        override fun getWriter(): PrintWriter {
            if (null == writer) {
                writer = PrintWriter(getOutputStream())
            }
            return writer!!
        }

        override fun close() {
            outputStream?.close()
        }
    }

    internal class CipherServletOutputStream(
        cipherTransformation: String,
        secretKey: SecretKey,
        private val originalOutputStream: ServletOutputStream
    ) : ServletOutputStream() {

        private val cipherOutputStream: CipherOutputStream

        init {
            val cipher = Cipher.getInstance(cipherTransformation)
                .apply { init(ENCRYPT_MODE, secretKey) }
            originalOutputStream.write(cipher.iv)
            cipherOutputStream = CipherOutputStream(originalOutputStream, cipher)
        }

        override fun write(b: ByteArray) = cipherOutputStream.write(b)

        override fun write(b: ByteArray, off: Int, len: Int) = cipherOutputStream.write(b, off, len)

        override fun write(b: Int) = cipherOutputStream.write(b)

        override fun close() = cipherOutputStream.close()

        override fun flush() = cipherOutputStream.flush()

        override fun isReady() = originalOutputStream.isReady

        override fun setWriteListener(listener: WriteListener?) = originalOutputStream.setWriteListener(listener)
    }
}
