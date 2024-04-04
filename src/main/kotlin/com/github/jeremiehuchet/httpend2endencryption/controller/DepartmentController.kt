package com.github.jeremiehuchet.httpend2endencryption.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/departments")
class DepartmentController {

    private val departments = ClassPathResource("/departments.csv")
        .inputStream.bufferedReader(Charsets.UTF_8)
        .lines()
        .skip(1)
        .map { it.split(",") }
        .map { Department(it[0], it[1].toInt(), it[2], it[3].toInt(), it[4], it[5], it[6]) }
        .toList()
        .toMutableList()

    @GetMapping
    fun list() = departments

    @GetMapping("/{dep}")
    fun getByDep(@PathVariable dep: String): Department = departments.single { it.dep == dep }

    @PutMapping("/{dep}")
    fun update(@PathVariable dep: String, @RequestBody department: Department): ResponseEntity<Department> {
        dep == department.dep || throw IllegalArgumentException("dep path variable must match the dep request body attribute")

        departments.removeIf { dep == it.dep }
        departments.add(department)
        return ResponseEntity.ok(department)
    }

    data class Department(
        val dep: String,
        val reg: Int,
        val cheflieu: String,
        val tncc: Int,
        val ncc: String,
        val nccend: String,
        val libelle: String
    )
}
