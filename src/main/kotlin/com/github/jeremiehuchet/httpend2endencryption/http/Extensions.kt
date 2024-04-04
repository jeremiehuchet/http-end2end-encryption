package com.github.jeremiehuchet.httpend2endencryption.http

import java.util.Base64

fun ByteArray.encodeToBase64String() = Base64.getEncoder().encodeToString(this)
fun String.decodeBase64() = Base64.getDecoder().decode(this)
