package com.github.jeremiehuchet.httpend2endencryption.http

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val ALGORITHM_AES = "AES"
private const val ALGORITHM_ECDH = "ECDH"
private const val HASH_HMACSHA256 = "HmacSHA256"

fun doKeyAgreement(
    privateKey: PrivateKey,
    publicKey: PublicKey,
    salt: ByteArray = "FIXME: randomize".toByteArray()
): SecretKeySpec {
    val sharedSecret = KeyAgreement.getInstance(ALGORITHM_ECDH)
        .apply {
            init(privateKey)
            doPhase(publicKey, true)
        }
        .generateSecret()
        .let { SecretKeySpec(it, HASH_HMACSHA256) }

    return Mac.getInstance(HASH_HMACSHA256)
        .apply { init(sharedSecret) }
        .doFinal(salt)
        .let { SecretKeySpec(it, ALGORITHM_AES) }
}
