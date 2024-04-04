package com.github.jeremiehuchet.httpend2endencryption.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/encrypted/departments")
class EncryptedDepartmentController : DepartmentController()
