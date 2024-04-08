package com.github.jeremiehuchet.httpend2endencryption.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Inherits **exact** same implementation as `DepartmentController`.
 *
 * No need for cryptographic stuff in the controller to handle encryption, everything is done by the `EncryptedContentEncodingFilter`.
 */
@RestController
@RequestMapping("/encrypted/departments")
class EncryptedDepartmentController : DepartmentController()
