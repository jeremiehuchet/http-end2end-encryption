package com.github.jeremiehuchet.httpend2endencryption.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification

fun When() = RestAssured.`when`()
fun RequestSpecification.When() = this.`when`()

fun String.decodeJsonToMap() : Map<String,Any> = ObjectMapper().readValue(this)
fun String.decodeJsonToList() : List<Map<String,Any>> = ObjectMapper().readValue(this)
