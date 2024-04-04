package com.github.jeremiehuchet.httpend2endencryption.http

import org.springframework.boot.context.properties.ConfigurationProperties
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

@ConfigurationProperties(prefix = "http.secure-transport")
data class HttpEncryptedProperties(
    val privateKey: String
) {
    fun getPrivateKeySpec(): PKCS8EncodedKeySpec {
        val key = privateKey
            .removePrefix("-----BEGIN PRIVATE KEY-----")
            .replace("\n".toRegex(), "")
            .removeSuffix("-----END PRIVATE KEY-----")
        val keyBytes = Base64.getDecoder().decode(key)
        return PKCS8EncodedKeySpec(keyBytes)
    }
}
